package com.hibiki.ui.archive

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState

internal fun isHeroScrollAtTop(scrollState: ScrollState?): Boolean =
    scrollState == null || !scrollState.canScrollBackward

internal fun isHeroScrollAtTop(lazyListState: LazyListState?): Boolean =
    lazyListState == null ||
        (lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0)

internal fun isHeroScrollAtTop(
    scrollState: ScrollState?,
    lazyListState: LazyListState?,
): Boolean = when {
    lazyListState != null -> isHeroScrollAtTop(lazyListState)
    else -> isHeroScrollAtTop(scrollState)
}
