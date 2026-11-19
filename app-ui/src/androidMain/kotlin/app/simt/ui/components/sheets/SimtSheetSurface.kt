package app.simt.ui.components.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.simt.ui.components.TopOverlayHeader
import app.simt.ui.components.TopOverlayToolToggle
import app.simt.ui.components.backgrounds.GradientBackground
import app.simt.ui.components.buttons.DialogActionButtons
import app.simt.ui.components.canopyGlass
import app.simt.ui.components.dialogs.ModalDefaults
import app.simt.ui.components.loader.BlockingLoaderHandle
import app.simt.ui.components.loader.LocalBlockingLoaderState
import app.simt.ui.components.loader.showHandle
import app.simt.ui.effects.ScrollEdgeEffect

/** Which screen edge a [SimtSheetSurface] anchors to and slides in from. */
public enum class SheetEdge {
    Bottom,
    Top,
}

/**
 * Canonical edge-sheet surface (Layer 1). Renders a glass sheet anchored to the top or bottom
 * edge with a [TopOverlayHeader], a scrollable body, and an optional action row — the single
 * source of truth for both bottom and top sheets.
 *
 * The sheet slides in from [edge]; dismiss is animated and [onDismissRequest] is called after the
 * exit transition completes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun SimtSheetSurface(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    edge: SheetEdge = SheetEdge.Bottom,
    subtitle: String? = null,
    headerIcon: (@Composable () -> Unit)? = null,
    headerTools: List<TopOverlayToolToggle> = emptyList(),
    onSave: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    isSaveEnabled: Boolean = true,
    saveLabel: String? = null,
    cancelLabel: String? = null,
    /**
     * When true, a full-screen blocking overlay is shown above the dialog content.
     * This is used for operations that must not be interrupted (e.g. GPS reset).
     */
    isBlocking: Boolean = false,
    /** Optional message displayed in the blocking overlay. */
    blockingMessage: String? = null,
    customActions: (@Composable RowScope.() -> Unit)? = null,
    isScrollable: Boolean = true,
    headerToolsModifier: Modifier = Modifier,
    overlayContent: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val configuration = LocalConfiguration.current
    val transitionState = remember { MutableTransitionState(false) }
    var pendingDismiss by remember { mutableStateOf(false) }
    val contentScrollState = rememberScrollState()

    val blockingLoaderController = LocalBlockingLoaderState.current
    var blockingHandle: BlockingLoaderHandle? by remember { mutableStateOf(null) }

    LaunchedEffect(isBlocking) {
        if (blockingLoaderController == null) return@LaunchedEffect

        if (isBlocking && blockingHandle == null) {
            blockingHandle = blockingLoaderController.showHandle(blockingMessage)
        } else if (!isBlocking && blockingHandle != null) {
            blockingHandle?.close()
            blockingHandle = null
        }
    }

    LaunchedEffect(blockingMessage) {
        blockingHandle?.update(blockingMessage)
    }

    DisposableEffect(Unit) {
        onDispose {
            blockingHandle?.close()
            blockingHandle = null
        }
    }

    fun requestDismiss() {
        // Prevent accidental dismissal (tap outside / close) while a blocking
        // operation is running.
        if (isBlocking) return
        if (!pendingDismiss) {
            pendingDismiss = true
            transitionState.targetState = false
        }
    }

    LaunchedEffect(Unit) {
        transitionState.targetState = true
    }

    if (pendingDismiss && transitionState.isIdle && !transitionState.currentState) {
        LaunchedEffect(Unit) {
            onDismissRequest()
        }
        return
    }

    // Per-edge geometry: anchor alignment, slide offset sign, safe-inset side, and corner rounding.
    val alignment = if (edge == SheetEdge.Bottom) Alignment.BottomCenter else Alignment.TopCenter
    val insetSide = if (edge == SheetEdge.Bottom) WindowInsetsSides.Bottom else WindowInsetsSides.Top
    val sheetShape =
        if (edge == SheetEdge.Bottom) {
            RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
        } else {
            RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
        }

    Dialog(
        onDismissRequest = { requestDismiss() },
        properties = ModalDefaults.FullScreenDialogProperties,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Scrim
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.32f))
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { requestDismiss() })
                        },
            )

            AnimatedVisibility(
                visibleState = transitionState,
                enter =
                    fadeIn(animationSpec = tween(durationMillis = 140)) +
                        slideInVertically(animationSpec = tween(durationMillis = 180)) { fullHeight ->
                            if (edge == SheetEdge.Bottom) fullHeight else -fullHeight
                        },
                exit =
                    fadeOut(animationSpec = tween(durationMillis = 110)) +
                        slideOutVertically(animationSpec = tween(durationMillis = 140)) { fullHeight ->
                            if (edge == SheetEdge.Bottom) fullHeight else -fullHeight
                        },
                modifier = Modifier.align(alignment),
            ) {
                val safeSheetPadding =
                    WindowInsets.safeDrawing
                        .only(insetSide + WindowInsetsSides.Horizontal)
                        .asPaddingValues()
                Surface(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier =
                        Modifier
                            .widthIn(max = 640.dp)
                            .fillMaxWidth()
                            .padding(safeSheetPadding)
                            .heightIn(max = configuration.screenHeightDp.dp * 0.9f)
                            .canopyGlass(shape = sheetShape, surfaceAlpha = 0.97f),
                    color = Color.Transparent,
                    shape = sheetShape,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TopOverlayHeader(
                            applyTopInsets = false,
                            title = title,
                            subtitle = subtitle,
                            onClose = { requestDismiss() },
                            icon = headerIcon,
                            tools = headerTools,
                            toolsModifier = headerToolsModifier,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        )

                        // Content area (scrollable)
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    // When scrollable, the content must be height-constrained for
                                    // verticalScroll() to engage. For non-scrollable sheets we keep
                                    // the previous "wrap content" behavior.
                                    .weight(1f, fill = isScrollable),
                        ) {
                            GradientBackground(
                                modifier = Modifier.fillMaxSize(),
                            )

                            if (isScrollable) {
                                ScrollEdgeEffect(
                                    state = contentScrollState,
                                    orientation = Orientation.Vertical,
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    Column(
                                        modifier =
                                            modifier
                                                .fillMaxWidth()
                                                .verticalScroll(contentScrollState)
                                                .padding(horizontal = 16.dp)
                                                .padding(top = 16.dp, bottom = 8.dp),
                                    ) {
                                        content()
                                    }
                                }
                            } else {
                                Column(
                                    modifier =
                                        modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .padding(top = 16.dp, bottom = 8.dp),
                                ) {
                                    content()
                                }
                            }
                        }

                        // Divider and Actions
                        if (onSave != null || onClose != null || customActions != null) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )

                            DialogActionButtons(
                                onCancel = onCancel?.let { { requestDismiss() } },
                                onSave =
                                    onSave?.let {
                                        {
                                            it()
                                            requestDismiss()
                                        }
                                    },
                                onClose =
                                    onClose?.let {
                                        {
                                            it()
                                            requestDismiss()
                                        }
                                    },
                                isSaveEnabled = isSaveEnabled,
                                cancelLabel = cancelLabel,
                                saveLabel = saveLabel,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                customActions = customActions,
                            )
                        }
                    }
                }
            }

            overlayContent?.invoke()
        }
    }
}
