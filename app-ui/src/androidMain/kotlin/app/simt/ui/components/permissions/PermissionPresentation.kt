package app.simt.ui.components.permissions

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Everything the generic permission UI needs to present a runtime permission consistently: icon,
 * theme color, dialog copy and the short label used in checklists/banners.
 *
 * The application owns the mapping from its own permission model onto this class (and the
 * matching string resources); nothing else needs to change for the generic dialog/banner to
 * support a new permission.
 */
public data class PermissionPresentation(
    public val icon: ImageVector,
    public val tint: Color,
    public val buttonContainerColor: Color,
    public val buttonContentColor: Color,
    public val backgroundStyle: AnimatedDialogBackgroundStyle,
    @StringRes public val titleRes: Int,
    @StringRes public val explanationRes: Int,
    @StringRes public val taskLabelRes: Int,
)
