package com.hibiki.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hibiki.ui.archive.ArchiveScreen
import com.hibiki.ui.archive.PhraseAdvancedEditScreen
import com.hibiki.ui.archive.PhraseDetailScreen
import com.hibiki.ui.components.StoneBackground
import com.hibiki.ui.contexts.ContextEditScreen
import com.hibiki.ui.contexts.ContextsScreen
import com.hibiki.ui.contexts.SubjectEditScreen
import com.hibiki.ui.contexts.SubjectsScreen
import com.hibiki.ui.home.HomeScreen
import com.hibiki.ui.settings.SettingsScreen
import com.hibiki.ui.theme.Cyberpunk
import com.hibiki.ui.theme.HibikiMotion

private val NavFadeIn = fadeIn(animationSpec = tween(HibikiMotion.NavEnterMs, easing = HibikiMotion.Easing))
private val NavFadeOut = fadeOut(animationSpec = tween(HibikiMotion.NavExitMs, easing = HibikiMotion.Easing))
private val NavSlideIn = slideInHorizontally(
    animationSpec = tween(HibikiMotion.NavEnterMs, easing = HibikiMotion.Easing),
) { it / 10 }
private val NavSlideOut = slideOutHorizontally(
    animationSpec = tween(HibikiMotion.NavExitMs, easing = HibikiMotion.Easing),
) { -it / 14 }
private val NavScaleIn = scaleIn(
    animationSpec = tween(HibikiMotion.NavEnterMs, easing = HibikiMotion.Easing),
    initialScale = 0.96f,
)
private val NavScaleOut = scaleOut(
    animationSpec = tween(HibikiMotion.NavExitMs, easing = HibikiMotion.Easing),
    targetScale = 1.02f,
)
private val NavEnter = NavFadeIn + NavSlideIn + NavScaleIn
private val NavExit = NavFadeOut + NavSlideOut + NavScaleOut
private val NavPopSlideOut = slideOutHorizontally(
    animationSpec = tween(HibikiMotion.NavExitMs, easing = HibikiMotion.Easing),
) { it / 10 }
private val NavPopSlideIn = slideInHorizontally(
    animationSpec = tween(HibikiMotion.NavEnterMs, easing = HibikiMotion.Easing),
) { -it / 14 }
private val TabFadeIn = fadeIn(animationSpec = tween(HibikiMotion.TabCrossfadeMs, easing = HibikiMotion.Easing))
private val TabFadeOut = fadeOut(animationSpec = tween(HibikiMotion.TabCrossfadeMs, easing = HibikiMotion.Easing))
private val NavPopEnter = NavFadeIn + NavPopSlideIn + NavScaleIn
private val NavPopExit = NavFadeOut + NavPopSlideOut + NavScaleOut

private fun NavDestination.topLevelGraphRoute(): String? =
    hierarchy.mapNotNull { it.route }.firstOrNull { Route.isTopLevelGraph(it) }

@Composable
fun HibikiNavHost(
    onStartOverlay: () -> Unit,
) {
    val navController = rememberNavController()
    val destinations = listOf(
        BottomDest(
            graphRoute = Route.HomeGraph.path,
            startRoute = Route.Home.path,
            label = "Home",
            iconKanji = "基",
        ),
        BottomDest(
            graphRoute = Route.DatabaseGraph.path,
            startRoute = Route.Database.path,
            label = "Database",
            iconKanji = "蔵",
        ),
    )

    fun navigateTopLevel(graphRoute: String) {
        navController.navigate(graphRoute) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    StoneBackground(scrimAlpha = 0.5f) {
        Scaffold(
            containerColor = Cyberpunk.Transparent,
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val selectedIndex = destinations.indexOfFirst { dest ->
                    currentDestination?.hierarchy?.any { entry ->
                        entry.route == dest.graphRoute
                    } == true
                }.coerceAtLeast(0)

                HibikiBottomBar(
                    destinations = destinations,
                    selectedIndex = selectedIndex,
                    onSelect = { index ->
                        val dest = destinations[index]
                        if (selectedIndex == index) {
                            navController.popBackStack(dest.startRoute, inclusive = false)
                        } else {
                            navigateTopLevel(dest.graphRoute)
                        }
                    },
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                )
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Route.HomeGraph.path,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                enterTransition = {
                    val from = initialState.destination.topLevelGraphRoute()
                    val to = targetState.destination.topLevelGraphRoute()
                    if (from != null && to != null && from != to) {
                        TabFadeIn
                    } else {
                        NavEnter
                    }
                },
                exitTransition = {
                    val from = initialState.destination.topLevelGraphRoute()
                    val to = targetState.destination.topLevelGraphRoute()
                    if (from != null && to != null && from != to) {
                        TabFadeOut
                    } else {
                        NavExit
                    }
                },
                popEnterTransition = { NavPopEnter },
                popExitTransition = { NavPopExit },
            ) {
                homeGraph(
                    navController = navController,
                    onStartOverlay = onStartOverlay,
                )
                databaseGraph(navController = navController)
            }
        }
    }
}

private fun NavGraphBuilder.homeGraph(
    navController: NavHostController,
    onStartOverlay: () -> Unit,
) {
    navigation(
        route = Route.HomeGraph.path,
        startDestination = Route.Home.path,
    ) {
        composable(Route.Home.path) {
            HomeScreen(
                onStartOverlay = onStartOverlay,
                onOpenContexts = {
                    navController.navigate(Route.Contexts.path) { launchSingleTop = true }
                },
                onOpenSettings = {
                    navController.navigate(Route.HomeSettings.path) { launchSingleTop = true }
                },
            )
        }
        composable(Route.HomeSettings.path) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Route.Contexts.path) {
            ContextsScreen(
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Route.ContextEdit.create(it)) { launchSingleTop = true } },
                onCreate = { navController.navigate(Route.ContextEdit.create()) { launchSingleTop = true } },
            )
        }
        composable(
            Route.ContextEdit.path,
            arguments = listOf(
                navArgument("contextId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            ContextEditScreen(
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() },
            )
        }
        composable(
            Route.Subjects.path,
            arguments = listOf(navArgument("contextId") { type = NavType.StringType }),
        ) {
            val contextId = it.arguments?.getString("contextId").orEmpty()
            SubjectsScreen(
                onBack = { navController.popBackStack() },
                onEdit = { id ->
                    navController.navigate(Route.SubjectEdit.create(contextId, id)) {
                        launchSingleTop = true
                    }
                },
                onCreate = {
                    navController.navigate(Route.SubjectEdit.create(contextId)) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(
            Route.SubjectEdit.path,
            arguments = listOf(
                navArgument("contextId") { type = NavType.StringType },
                navArgument("subjectId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            SubjectEditScreen(
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() },
            )
        }
    }
}

private fun NavGraphBuilder.databaseGraph(
    navController: NavHostController,
) {
    navigation(
        route = Route.DatabaseGraph.path,
        startDestination = Route.Database.path,
    ) {
        composable(Route.Database.path) {
            ArchiveScreen(
                onOpenPhrase = { phraseId ->
                    navController.navigate(Route.Phrase.create(phraseId)) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(
            Route.Phrase.path,
            arguments = listOf(navArgument("phraseId") { type = NavType.StringType }),
        ) {
            val phraseId = it.arguments?.getString("phraseId").orEmpty()
            PhraseDetailScreen(
                phraseId = phraseId,
                onBack = { navController.popBackStack() },
                onAdvancedEdit = { currentPhraseId ->
                    navController.navigate(Route.PhraseAdvancedEdit.create(currentPhraseId)) {
                        launchSingleTop = true
                    }
                },
                onRandomPhrase = { randomPhraseId ->
                    navController.navigate(Route.Phrase.create(randomPhraseId)) {
                        popUpTo(Route.Phrase.path) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(
            Route.PhraseAdvancedEdit.path,
            arguments = listOf(navArgument("phraseId") { type = NavType.StringType }),
        ) {
            PhraseAdvancedEditScreen(
                onBack = { navController.popBackStack() },
                onDeleted = {
                    navController.popBackStack(Route.Database.path, inclusive = false)
                },
            )
        }
    }
}
