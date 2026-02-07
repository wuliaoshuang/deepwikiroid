package com.moxiang.deepwiki

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import java.net.URLEncoder
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import com.moxiang.deepwiki.core.ui.components.TabBar
import com.moxiang.deepwiki.feature.detail.RepoDetailScreen
import com.moxiang.deepwiki.feature.browser.BrowserTestScreen
import com.moxiang.deepwiki.feature.favorites.NewFavoritesScreen
import com.moxiang.deepwiki.feature.history.NewHistoryScreen
import com.moxiang.deepwiki.feature.home.HomeScreen
import com.moxiang.deepwiki.feature.search.SearchScreen
import com.moxiang.deepwiki.feature.settings.NewSettingsScreen
import com.moxiang.deepwiki.feature.settings.ScriptManagerScreen

/**
 * Main Navigation Container - Based on Pencil Design
 * Manages bottom tab navigation and screen transitions
 */
@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    var selectedTab by remember { mutableStateOf(0) }
    var currentRoute by remember { mutableStateOf("home") }

    val navigateToDetail: (String, String?, Int) -> Unit = { repoName, description, stars ->
        val encodedRepoName = URLEncoder.encode(repoName, StandardCharsets.UTF_8.toString())
        val encodedDescription = URLEncoder.encode(description ?: "", StandardCharsets.UTF_8.toString())
        navController.navigate("detail/$encodedRepoName?description=$encodedDescription&stars=$stars")
    }

    // Update selected tab when route changes
    LaunchedEffect(currentRoute) {
        selectedTab = when (currentRoute) {
            "home" -> 0
            "favorites" -> 1
            "history" -> 2
            "settings" -> 3
            else -> selectedTab
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Content Area
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.fillMaxSize()
        ) {
                // Home Screen
                composable("home") {
                    currentRoute = "home"
                    HomeScreen(
                        onNavigateToDetail = navigateToDetail,
                        onNavigateToSearch = {
                            navController.navigate("search")
                        },
                        onNavigateToBrowserTest = {
                            navController.navigate("browser-test")
                        }
                    )
                }

                // Search Screen
                composable("search") {
                    currentRoute = "search"
                    SearchScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onRepositoryClick = navigateToDetail
                    )
                }

                // Favorites Screen
                composable("favorites") {
                    currentRoute = "favorites"
                    NewFavoritesScreen(
                        onRepositoryClick = navigateToDetail
                    )
                }

                // History Screen
                composable("history") {
                    currentRoute = "history"
                    NewHistoryScreen(
                        onRepositoryClick = { history ->
                            navigateToDetail(history.repoName, history.description, history.stars)
                        }
                    )
                }

                // Settings Screen
                composable("settings") {
                    currentRoute = "settings"
                    NewSettingsScreen(
                        onNavigateToScripts = { navController.navigate("scripts") }
                    )
                }

                // Script Manager Screen
                composable("scripts") {
                    currentRoute = "scripts"
                    ScriptManagerScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // Repository Detail Screen
                composable(
                    route = "detail/{repoName}?description={description}&stars={stars}",
                    arguments = listOf(
                        navArgument("repoName") {
                            type = NavType.StringType
                            nullable = false
                        },
                        navArgument("description") {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                        navArgument("stars") {
                            type = NavType.IntType
                            defaultValue = 0
                        }
                    )
                ) { backStackEntry ->
                    currentRoute = "detail"
                    val encodedRepoName = backStackEntry.arguments?.getString("repoName") ?: ""
                    val encodedDescription = backStackEntry.arguments?.getString("description") ?: ""
                    val stars = backStackEntry.arguments?.getInt("stars") ?: 0

                    val repoName = try {
                        URLDecoder.decode(encodedRepoName, StandardCharsets.UTF_8.toString())
                    } catch (e: Exception) {
                        encodedRepoName
                    }
                    val description = try {
                        URLDecoder.decode(encodedDescription, StandardCharsets.UTF_8.toString())
                    } catch (e: Exception) {
                        encodedDescription
                    }

                    RepoDetailScreen(
                        repoName = repoName,
                        repoDescription = description,
                        repoStars = stars,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                // Browser Test Screen
                composable("browser-test") {
                    currentRoute = "browser-test"
                    BrowserTestScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }

        // Bottom Tab Bar (floating above content, only show on main screens)
        if (currentRoute in listOf("home", "favorites", "history", "settings")) {
            TabBar(
                selectedTab = selectedTab,
                onTabSelected = { index ->
                    selectedTab = index
                    val route = when (index) {
                        0 -> "home"
                        1 -> "favorites"
                        2 -> "history"
                        3 -> "settings"
                        else -> "home"
                    }
                    navController.navigate(route) {
                        // Pop up to start destination to avoid building up a large back stack
                        popUpTo("home") {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
        }
    }
}
