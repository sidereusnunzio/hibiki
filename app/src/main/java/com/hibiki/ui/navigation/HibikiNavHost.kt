package com.hibiki.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hibiki.ui.archive.ArchiveScreen
import com.hibiki.ui.archive.PhraseDetailScreen
import com.hibiki.ui.contexts.ContextEditScreen
import com.hibiki.ui.contexts.ContextsScreen
import com.hibiki.ui.contexts.SubjectEditScreen
import com.hibiki.ui.contexts.SubjectsScreen
import com.hibiki.ui.home.HomeScreen
import com.hibiki.ui.settings.SettingsScreen

object Routes {
    const val Home = "home"
    const val Archive = "archive"
    const val Phrase = "phrase/{phraseId}"
    const val Contexts = "contexts"
    const val ContextEdit = "contextEdit?contextId={contextId}"
    const val Subjects = "subjects/{contextId}"
    const val SubjectEdit = "subjectEdit/{contextId}?subjectId={subjectId}"
    const val Settings = "settings"
}

@Composable
fun HibikiNavHost(
    onStartOverlay: () -> Unit,
) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.Home) {
        composable(Routes.Home) {
            HomeScreen(
                onStartOverlay = onStartOverlay,
                onOpenArchive = { nav.navigate(Routes.Archive) },
                onOpenContexts = { nav.navigate(Routes.Contexts) },
                onOpenSettings = { nav.navigate(Routes.Settings) },
            )
        }
        composable(Routes.Archive) {
            ArchiveScreen(
                onBack = { nav.popBackStack() },
                onOpenPhrase = { nav.navigate("phrase/$it") },
            )
        }
        composable(
            Routes.Phrase,
            arguments = listOf(navArgument("phraseId") { type = NavType.StringType }),
        ) {
            PhraseDetailScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.Contexts) {
            ContextsScreen(
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate("contextEdit?contextId=$it") },
                onCreate = { nav.navigate("contextEdit") },
            )
        }
        composable(
            Routes.ContextEdit,
            arguments = listOf(
                navArgument("contextId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            ContextEditScreen(
                onBack = { nav.popBackStack() },
                onDone = { nav.popBackStack() },
            )
        }
        composable(
            Routes.Subjects,
            arguments = listOf(navArgument("contextId") { type = NavType.StringType }),
        ) {
            val contextId = it.arguments?.getString("contextId").orEmpty()
            SubjectsScreen(
                onBack = { nav.popBackStack() },
                onEdit = { id -> nav.navigate("subjectEdit/$contextId?subjectId=$id") },
                onCreate = { nav.navigate("subjectEdit/$contextId") },
            )
        }
        composable(
            Routes.SubjectEdit,
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
                onBack = { nav.popBackStack() },
                onDone = { nav.popBackStack() },
            )
        }
        composable(Routes.Settings) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}
