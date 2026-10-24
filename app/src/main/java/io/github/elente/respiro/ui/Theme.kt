package io.github.elente.respiro.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

val Background = Color(0xFF0C1719)
val Panel = Color(0xFF13242A)
val SurfaceFocus = Color(0xFF1D3A40)
val TextColor = Color(0xFFF1F5F4)
val Muted = Color(0xFF93A8A6)
val Air = Color(0xFF6FE3C1)
val OnAir = Color(0xFF06231B)

/** Amber always means "this can't be undone" or "careful", so risk reads from the couch. */
val Amber = Color(0xFFF3B44C)
val Danger = Color(0xFFFF8A7A)

val HeroNumberSize = 96.sp
val HeroLabelSize = 28.sp
val TitleSize = 32.sp
val BodySize = 20.sp
val RowLabelSize = 22.sp
val SmallSize = 16.sp

val SafeAreaHorizontal = 48.dp
val SafeAreaVertical = 32.dp
val Gutter = 24.dp
val RowGap = 8.dp

@Composable
fun RespiroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Air,
            onPrimary = OnAir,
            background = Background,
            onBackground = TextColor,
            surface = Panel,
            onSurface = TextColor,
            surfaceVariant = Panel,
            onSurfaceVariant = Muted,
            border = Air,
            error = Danger,
        ),
        content = content,
    )
}
