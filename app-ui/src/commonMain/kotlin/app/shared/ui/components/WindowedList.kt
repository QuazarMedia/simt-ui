package app.shared.ui.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.shared.ui.effects.ScrollEdgeEffect
import app.shared.ui.effects.ScrollEdgeEffectDefaults

/** A lazy vertical list with shared scroll state and overflow affordances. */
@Composable
fun WindowedList(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    reverseLayout: Boolean = false,
    userScrollEnabled: Boolean = true,
    fade: Boolean = true,
    fadeSize: Dp = ScrollEdgeEffectDefaults.fadeSize(Orientation.Vertical),
    showArrows: Boolean = true,
    arrowTint: Color = ScrollEdgeEffectDefaults.arrowTint(),
    content: LazyListScope.() -> Unit,
) {
    ScrollEdgeEffect(
        state = state,
        orientation = Orientation.Vertical,
        modifier = modifier,
        fade = fade,
        fadeSize = fadeSize,
        showArrows = showArrows,
        arrowTint = arrowTint,
        reverseDirection = reverseLayout,
    ) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            reverseLayout = reverseLayout,
            userScrollEnabled = userScrollEnabled,
            content = content,
        )
    }
}

/** Typed rows with stable keys, reusable content types, and an optional centered placeholder. */
@Composable
fun <T> WindowedList(
    items: List<T>,
    key: (item: T) -> Any,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    contentType: (item: T) -> Any? = { null },
    fade: Boolean = true,
    fadeSize: Dp = ScrollEdgeEffectDefaults.fadeSize(Orientation.Vertical),
    showArrows: Boolean = true,
    arrowTint: Color = ScrollEdgeEffectDefaults.arrowTint(),
    emptyContent: (@Composable () -> Unit)? = null,
    itemContent: @Composable (index: Int, item: T) -> Unit,
) {
    if (items.isEmpty() && emptyContent != null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            emptyContent()
        }
        return
    }

    WindowedList(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        fade = fade,
        fadeSize = fadeSize,
        showArrows = showArrows,
        arrowTint = arrowTint,
    ) {
        itemsIndexed(
            items = items,
            key = { _, item -> key(item) },
            contentType = { _, item -> contentType(item) },
        ) { index, item ->
            itemContent(index, item)
        }
    }
}
