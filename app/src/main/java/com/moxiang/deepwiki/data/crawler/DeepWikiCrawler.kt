package com.moxiang.deepwiki.data.crawler

import com.moxiang.deepwiki.data.model.DocumentDto
import com.moxiang.deepwiki.data.model.RepositoryDto
import com.moxiang.deepwiki.data.model.WikiMenuDto
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException
import java.net.URLEncoder

/**
 * DeepWiki 数据抓取器
 * 已由 HTML 爬虫升级为官方 API 调用
 */
class DeepWikiCrawler {
    companion object {
        private const val BASE_URL = "https://deepwiki.com"
        private const val API_BASE_URL = "https://api.devin.ai/ada"

        // 使用单例 OkHttpClient 以复用连接池，显著提升后续请求速度
        private val sharedClient = OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        private val htmlConverter: FlexmarkHtmlConverter = FlexmarkHtmlConverter.builder().apply {
            set(FlexmarkHtmlConverter.SETEXT_HEADINGS, false)
            set(FlexmarkHtmlConverter.OUTPUT_ATTRIBUTES_ID, false)
            set(FlexmarkHtmlConverter.SKIP_ATTRIBUTES, true)
        }.build()
    }

    /**
     * 获取热门仓库
     */
    suspend fun fetchPopularRepositories(): List<RepositoryDto> = withContext(Dispatchers.IO) {
        searchRepositories("")
    }

    /**
     * 搜索仓库
     */
    suspend fun searchRepositories(query: String): List<RepositoryDto> = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "$API_BASE_URL/list_public_indexes?search_repo=$encodedQuery"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "DeepWiki-Android-App/1.0")
            .header("Accept", "application/json")
            .build()

        // 使用共享的 sharedClient
        val response = sharedClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IOException("HTTP Error ${response.code}")
        }

        val jsonString = response.body?.string() ?: ""
        parseJsonResponse(jsonString)
    }

    /**
     * 获取仓库文档
     * @param path 仓库名(owner/repo) 或 完整路径(/owner/repo/page)
     */
    suspend fun fetchDocument(path: String): DocumentDto = withContext(Dispatchers.IO) {
        // 放弃正文抓取：仅爬取菜单信息，正文交给 WebView 渲染
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        val url = "$BASE_URL/$cleanPath"
        val parts = cleanPath.split("/")
        val repoFullName = if (parts.size >= 2) "${parts[0]}/${parts[1]}" else cleanPath
        val title = parts.lastOrNull()?.ifBlank { repoFullName } ?: repoFullName

        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            )
            .build()

        val response = sharedClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IOException("HTTP Error ${response.code}")
        }

        val html = response.body?.string() ?: ""
        val menuItems = try {
            parseMenu(Jsoup.parse(html), repoFullName, cleanPath)
        } catch (_: Exception) {
            emptyList()
        }

        DocumentDto(
            repoFullName = repoFullName,
            title = title,
            content = "",
            htmlUrl = url,
            menuItems = menuItems
        )
    }

    private fun parseDocumentPage(html: String, fullName: String, url: String, currentPath: String): DocumentDto {
        val document = Jsoup.parse(html)

        // 尝试提取标题
        val title = document.select("h1").first()?.text()
            ?: document.title()
            ?: fullName

        // 尝试提取主要内容（稳定策略：候选评分 + 清理 + 启发式过滤）
        val contentElement = selectMainContent(document)
        val contentClone = contentElement.clone().also { cleanContent(it) }
        var content = htmlToMarkdown(contentClone.html())

        val nextDataRoot = parseNextData(document)
        val fallback = nextDataRoot?.let { extractFromNextDataRoot(it, url) }
        if (!fallback.isNullOrBlank()) {
            val useFallback = when {
                isLowQualityContent(content) && !isLowQualityContent(fallback) -> true
                !isLowQualityContent(fallback) && fallback.length >= (content.length * 0.6) -> true
                else -> false
            }
            if (useFallback) {
                content = fallback
            }
        }

        if (isLowQualityContent(content) && nextDataRoot != null) {
            val extraData = fetchNextDataJson(nextDataRoot, currentPath)
            if (extraData != null) {
                val jsonFallback = extractFromNextDataRoot(extraData, url)
                if (!jsonFallback.isNullOrBlank() && !isLowQualityContent(jsonFallback)) {
                    content = jsonFallback
                }
            }
        }

        // 解析菜单
        val menuItems = parseMenu(document, fullName, currentPath)

        return DocumentDto(
            repoFullName = fullName,
            title = title,
            content = content,
            htmlUrl = url,
            menuItems = menuItems
        )
    }

    private data class TempMenuItem(
        val title: String,
        val path: String,
        val isSelected: Boolean,
        val level: Int,
        val children: MutableList<TempMenuItem> = mutableListOf()
    )

    private fun parseMenu(document: org.jsoup.nodes.Document, repoFullName: String, currentPath: String): List<WikiMenuDto> {
        // 查找侧边栏列表
        // 根据用户反馈，菜单是一个 flat list，通过 padding-left 控制层级
        // 查找包含 href 指向当前 repo 的 ul
        val sidebarUl = document.select("ul").firstOrNull { ul ->
            ul.select("a[href^=/$repoFullName]").isNotEmpty()
        } ?: return emptyList()

        val tempItems = mutableListOf<TempMenuItem>()

        // 1. 解析所有 li 为中间对象，提取层级
        sidebarUl.select("li").forEach { li ->
            val link = li.selectFirst("a")
            if (link != null) {
                val href = link.attr("href")
                val text = link.text()

                // 解析 padding-left 获取层级
                // style="padding-left:12px"
                val style = li.attr("style")
                val paddingPattern = Regex("padding-left:\\s*(\\d+)(?:px)?")
                val match = paddingPattern.find(style)
                val padding = match?.groupValues?.get(1)?.toIntOrNull() ?: 0

                // 假设每级缩进 12px (根据用户提供的样例 0 -> 12)
                // level 0: 0px, level 1: 12px, level 2: 24px...
                val level = if (padding > 0) padding / 12 else 0

                val isSelected = href == "/$currentPath" || href == currentPath
                // 确保 path 格式正确
                val finalPath = if (href.startsWith("/")) href else "/$href"

                tempItems.add(TempMenuItem(text, finalPath, isSelected, level))
            }
        }

        // 2. 构建树结构
        val roots = mutableListOf<TempMenuItem>()
        // 记录每一级最后一个添加的节点，作为下一级的父节点
        val lastNodeAtLevel = mutableMapOf<Int, TempMenuItem>()

        tempItems.forEach { item ->
            if (item.level == 0) {
                roots.add(item)
                lastNodeAtLevel[0] = item
            } else {
                // 寻找父节点 (level - 1)
                // 如果数据不规范（比如直接从 0 跳到 2），则尝试找最近的上一级
                var parentLevel = item.level - 1
                while (parentLevel >= 0 && !lastNodeAtLevel.containsKey(parentLevel)) {
                    parentLevel--
                }

                if (parentLevel >= 0) {
                    val parent = lastNodeAtLevel[parentLevel]
                    parent?.children?.add(item)
                    lastNodeAtLevel[item.level] = item
                } else {
                    // 如果找不到父节点，作为根节点处理 (容错)
                    roots.add(item)
                    lastNodeAtLevel[item.level] = item
                }
            }
            // 清除比当前 level 更深的记录，避免错误的跨支树连接 (虽然 logic 上可能不需要，但为了严谨)
            val levels = lastNodeAtLevel.keys.toList() // Copy keys
            levels.forEach { l ->
                if (l > item.level) lastNodeAtLevel.remove(l)
            }
        }

        // 3. 转换为最终 DTO
        return roots.map { convertToDto(it) }
    }

    private fun convertToDto(temp: TempMenuItem): WikiMenuDto {
        return WikiMenuDto(
            title = temp.title,
            path = temp.path,
            isSelected = temp.isSelected,
            children = temp.children.map { convertToDto(it) }
        )
    }

    private fun parseListItems(ulElement: Element, repoFullName: String, currentPath: String): List<WikiMenuDto> {
        // 保留旧方法作为备用，或者直接移除
        return emptyList()
    }

    private fun htmlToMarkdown(html: String): String {
        val trimmed = html.trim()
        if (trimmed.isEmpty()) return ""
        val markdown = htmlConverter.convert(trimmed)
        return postProcessMarkdown(markdown)
    }

    private fun postProcessMarkdown(markdown: String): String {
        var out = markdown
        // Normalize horizontal rules
        out = out.replace(Regex("(?m)^\\s*\\*\\s*(\\*\\s*){2,}$"), "---")
        // Drop empty code fences
        out = out.replace(Regex("(?s)```\\s*\\n\\s*```\\s*\\n"), "")
        // Trim trailing whitespace-only lines
        out = out.replace(Regex("(?m)^[ \\t]+$"), "")
        // Collapse excessive blank lines
        out = out.replace(Regex("\\n{3,}"), "\n\n")
        return out.trim()
    }

    private fun isLowQualityContent(markdown: String): Boolean {
        val trimmed = markdown.trim()
        if (trimmed.isEmpty()) return true
        if (trimmed.length < 200) return true
        val lower = trimmed.lowercase()
        val badMarkers = listOf(
            "loading",
            "index your code with devin",
            "edit wiki",
            "share",
            "menu"
        )
        if (badMarkers.any { lower.contains(it) }) return true

        val lines = trimmed.lines()
        val listLines = lines.count { it.trim().startsWith("- ") }
        val textLines = lines.count {
            val t = it.trim()
            t.isNotEmpty() && !t.startsWith("- ") && !t.startsWith("#")
        }
        if (listLines >= 20 && textLines == 0) return true
        return false
    }

    /**
     * 选择最可能的正文节点：候选列表 + 文本分数 + 链接密度惩罚
     */
    private fun selectMainContent(document: Document): Element {
        val candidates = mutableListOf<Element>().apply {
            addAll(document.select("article"))
            addAll(document.select(".markdown-body"))
            addAll(document.select(".prose"))
            addAll(document.select("main"))
            if (isEmpty()) add(document.body())
        }

        var best = candidates.first()
        var bestScore = Int.MIN_VALUE

        candidates.forEach { candidate ->
            val clone = candidate.clone().also { cleanContent(it) }
            val score = computeTextScore(clone)
            if (score > bestScore) {
                bestScore = score
                best = candidate
            }
        }

        return best
    }

    private data class Candidate(val text: String, val score: Int, val looksHtml: Boolean)

    private fun extractFromNextDataRoot(root: JSONObject, baseUrl: String): String? {
        val candidates = mutableListOf<Candidate>()
        collectCandidates(root, "\$", candidates)
        val best = candidates.maxByOrNull { it.score } ?: return null
        val raw = best.text.trim()

        return if (best.looksHtml) {
            val doc = Jsoup.parse(raw)
            val body = doc.body()
            val cleaned = body.clone().also { cleanContent(it) }
            htmlToMarkdown(cleaned.html())
        } else {
            raw
        }
    }

    private fun collectCandidates(node: Any?, path: String, out: MutableList<Candidate>) {
        when (node) {
            is JSONObject -> {
                val keys = node.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    collectCandidates(node.opt(key), "$path.$key", out)
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    collectCandidates(node.opt(i), "$path[$i]", out)
                }
            }
            is String -> {
                val score = scoreCandidate(node, path)
                if (score > 0) {
                    out.add(Candidate(node, score, looksLikeHtml(node)))
                }
            }
        }
    }

    private fun scoreCandidate(text: String, path: String): Int {
        val trimmed = text.trim()
        if (trimmed.length < 120) return 0
        var score = minOf(trimmed.length, 8000) / 10
        val lower = trimmed.lowercase()

        if (looksLikeHtml(trimmed)) score += 250
        if (lower.contains("```") || lower.contains("\n# ")) score += 300
        if (lower.contains("overview") || lower.contains("architecture")) score += 80

        val keyBoosts = listOf(
            "markdown",
            "md",
            "content",
            "body",
            "article",
            "document",
            "doc",
            "html",
            "source"
        )
        val lowerPath = path.lowercase()
        if (keyBoosts.any { lowerPath.contains(it) }) score += 300

        val penalties = listOf(
            "loading",
            "index your code with devin",
            "edit wiki",
            "share",
            "menu"
        )
        if (penalties.any { lower.contains(it) }) score -= 400

        return score
    }

    private fun looksLikeHtml(text: String): Boolean {
        if (!text.contains("<") || !text.contains(">")) return false
        return text.contains("</") || text.contains("<p") || text.contains("<div") || text.contains("<h1")
    }

    private fun parseNextData(document: Document): JSONObject? {
        val script = document.selectFirst("script#__NEXT_DATA__")
            ?: document.selectFirst("script[id=__NEXT_DATA__]")
            ?: document.selectFirst("script[data-next-data]")
            ?: return null

        val rawJson = script.data().ifBlank { script.html() }.trim()
        if (rawJson.isBlank()) return null

        return try {
            JSONObject(rawJson)
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchNextDataJson(nextData: JSONObject, cleanPath: String): JSONObject? {
        val buildId = nextData.optString("buildId")
        if (buildId.isBlank()) return null

        val query = nextData.optJSONObject("query")
        val queryString = buildQueryString(query)
        val pathPart = cleanPath.trimStart('/')
        if (pathPart.isBlank()) return null

        val dataUrl = buildString {
            append(BASE_URL)
            append("/_next/data/")
            append(buildId)
            append("/")
            append(pathPart)
            append(".json")
            if (queryString.isNotBlank()) {
                append("?")
                append(queryString)
            }
        }

        val request = Request.Builder()
            .url(dataUrl)
            .header("User-Agent", "DeepWiki-Android-App/1.0")
            .header("Accept", "application/json")
            .build()

        val response = sharedClient.newCall(request).execute()
        if (!response.isSuccessful) return null
        val jsonString = response.body?.string().orEmpty()
        if (jsonString.isBlank()) return null
        return try {
            JSONObject(jsonString)
        } catch (_: Exception) {
            null
        }
    }

    private fun buildQueryString(query: JSONObject?): String {
        if (query == null) return ""
        val pairs = mutableListOf<String>()
        val keys = query.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = query.opt(key)
            when (value) {
                is JSONArray -> {
                    for (i in 0 until value.length()) {
                        val item = value.opt(i)?.toString() ?: continue
                        pairs.add("${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(item, "UTF-8")}")
                    }
                }
                null -> Unit
                else -> {
                    pairs.add("${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value.toString(), "UTF-8")}")
                }
            }
        }
        return pairs.joinToString("&")
    }

    private fun computeTextScore(element: Element): Int {
        val textLen = element.text().length
        if (textLen == 0) return 0

        val linkTextLen = element.select("a").text().length
        val linkPenalty = linkTextLen * 2

        val listPenalty = element.select("ul,ol").fold(0) { acc, list ->
            val liCount = list.select("li").size
            if (liCount < 8) return@fold acc
            val linkCount = list.select("a").size
            val linkRatio = if (liCount == 0) 0.0 else linkCount.toDouble() / liCount
            if (linkRatio >= 0.6) acc + 200 else acc
        }

        return textLen - linkPenalty - listPenalty
    }

    /**
     * 清理正文中的导航/菜单/装饰性节点
     */
    private fun cleanContent(element: Element) {
        element.select(
            "nav,header,footer,aside,form,button,script,style,noscript,svg"
        ).remove()

        element.select(
            "[role=navigation],[aria-label*=Menu],[aria-label*=menu]," +
                "[class*=sidebar],[class*=Sidebar],[class*=menu],[class*=Menu]," +
                "[class*=breadcrumb],[class*=Breadcrumb],[class*=toc],[class*=Toc]," +
                "[class*=loading],[class*=Loading],[class*=spinner],[class*=Spinner],[class*=skeleton]"
        ).remove()

        removeLikelyTocLists(element)
    }

    /**
     * 删除“像目录”的列表（大量链接且项数较多）
     */
    private fun removeLikelyTocLists(root: Element) {
        root.select("ul,ol").forEach { list ->
            val liCount = list.select("li").size
            if (liCount < 8) return@forEach
            val linkCount = list.select("a").size
            val linkRatio = if (liCount == 0) 0.0 else linkCount.toDouble() / liCount
            if (linkRatio >= 0.6) {
                list.remove()
            }
        }
    }


    /**
     * 解析 JSON 响应 (已移除耗时的循环打印)
     */
    private fun parseJsonResponse(jsonString: String): List<RepositoryDto> {
        if (jsonString.isEmpty()) return emptyList()

        val repositories = mutableListOf<RepositoryDto>()
        val root = JSONObject(jsonString)
        val indices = root.optJSONArray("indices") ?: return emptyList()

        for (i in 0 until indices.length()) {
            val item = indices.getJSONObject(i)
            val repoName = item.optString("repo_name")

            if (repoName.isNotEmpty()) {
                repositories.add(
                    RepositoryDto(
                        fullName = repoName,
                        description = item.optString("description", ""),
                        stars = item.optInt("stargazers_count", 0),
                        language = item.optString("language", "Unknown"),
                        url = "$BASE_URL/$repoName"
                    )
                )
            }
        }

        return repositories
    }
}
