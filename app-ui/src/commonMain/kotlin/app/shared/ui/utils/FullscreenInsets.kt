package app.shared.ui.utils

import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.shared.ui.internal.platform.fullScreenBottomWindowInsets
import app.shared.ui.internal.platform.fullScreenTopWindowInsets

@Composable
public fun Modifier.fullScreenTopInsets(): Modifier = windowInsetsPadding(fullScreenTopWindowInsets())

@Composable
public fun Modifier.fullScreenBottomInsets(): Modifier = windowInsetsPadding(fullScreenBottomWindowInsets())
