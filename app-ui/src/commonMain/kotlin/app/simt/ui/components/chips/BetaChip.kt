package app.simt.ui.components.chips

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
public fun BetaChip(
    modifier: Modifier = Modifier,
    text: String = "BETA",
    tint: Color = MaterialTheme.colorScheme.tertiary,
) {
    val shape = RoundedCornerShape(6.dp)
    Surface(
        modifier =
            modifier
                .clip(shape)
                .background(tint.copy(alpha = 0.18f))
                .border(width = 1.dp, color = tint.copy(alpha = 0.60f), shape = shape),
        color = Color.Transparent,
    ) {
        Text(
            text = text,
            color = tint,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(PaddingValues(horizontal = 6.dp, vertical = 2.dp)),
        )
    }
}
