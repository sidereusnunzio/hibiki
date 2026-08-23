package com.hibiki.ui.navigation

sealed class Route(val path: String) {
    data object HomeGraph : Route("home_graph")
    data object DatabaseGraph : Route("database_graph")

    data object Home : Route("home")
    data object HomeSettings : Route("home/settings")
    data object Contexts : Route("home/contexts")
    data object ContextEdit : Route("home/contextEdit?contextId={contextId}") {
        fun create(contextId: String? = null): String =
            if (contextId.isNullOrBlank()) "home/contextEdit" else "home/contextEdit?contextId=$contextId"
    }
    data object Subjects : Route("home/subjects/{contextId}") {
        fun create(contextId: String): String = "home/subjects/$contextId"
    }
    data object SubjectEdit : Route("home/subjectEdit/{contextId}?subjectId={subjectId}") {
        fun create(contextId: String, subjectId: String? = null): String =
            if (subjectId.isNullOrBlank()) {
                "home/subjectEdit/$contextId"
            } else {
                "home/subjectEdit/$contextId?subjectId=$subjectId"
            }
    }

    data object Database : Route("database")
    data object Phrase : Route("database/phrase/{phraseId}") {
        fun create(phraseId: String): String = "database/phrase/$phraseId"
    }
    data object PhraseAdvancedEdit : Route("database/phrase/{phraseId}/advanced") {
        fun create(phraseId: String): String = "database/phrase/$phraseId/advanced"
    }

    companion object {
        fun isTopLevelGraph(route: String?): Boolean =
            route == HomeGraph.path || route == DatabaseGraph.path
    }
}
