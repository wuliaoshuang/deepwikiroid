package com.moxiang.deepwiki.core.ui.locale

import java.util.Locale

enum class AppLanguage(val id: String, val localeTag: String) {
    ENGLISH("en", "en"),
    CHINESE("zh", "zh-CN");

    companion object {
        fun fromId(id: String?): AppLanguage {
            return entries.firstOrNull { it.id == id } ?: defaultForLocale()
        }

        fun defaultForLocale(): AppLanguage {
            return if (Locale.getDefault().language.startsWith("zh")) {
                CHINESE
            } else {
                ENGLISH
            }
        }
    }
}
