package io.github.elente.respiro.ui

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

private const val FOCUSED_SCALE = 1.04f
private const val GLOW_ALPHA = 0.35f
private const val HERO_WEIGHT = 0.38f
private const val CONTENT_WEIGHT = 0.62f
private val FocusBorderWidth = 3.dp
private val GlowElevation = 12.dp
private val RowShape = RoundedCornerShape(16.dp)
private val PanelShape = RoundedCornerShape(28.dp)

/** Room for a focused row's scale and glow, so a list doesn't clip them. */
val ScaledRowPadding = PaddingValues(vertical = 8.dp)

/** Primary is the obvious next step; Permanent marks what can't be undone, and never takes default focus. */
enum class ActionVariant { PRIMARY, SECONDARY, PERMANENT }

/** Top bar, then a hero panel with the one fact of the screen beside the actions; no hero gives the content the width. */
@Composable
fun RespiroScaffold(
    deviceName: String,
    hero: (@Composable ColumnScope.() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = SafeAreaHorizontal, vertical = SafeAreaVertical),
        verticalArrangement = Arrangement.spacedBy(Gutter),
    ) {
        TopBar(deviceName)
        Row(horizontalArrangement = Arrangement.spacedBy(Gutter), modifier = Modifier.fillMaxSize()) {
            if (hero != null) HeroPanel(Modifier.weight(HERO_WEIGHT), hero)
            Column(
                modifier = Modifier.weight(CONTENT_WEIGHT).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(RowGap),
                content = content,
            )
        }
    }
}

@Composable
private fun TopBar(deviceName: String) {
    Row(modifier = Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("RESPIRO", color = Muted, fontSize = 14.sp, letterSpacing = 3.sp, modifier = Modifier.weight(1f))
        Text(deviceName, color = Muted, fontSize = SmallSize)
    }
}

@Composable
private fun HeroPanel(modifier: Modifier, hero: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxHeight().clip(PanelShape).background(Panel).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        content = hero,
    )
}

@Composable
fun HeroNumber(text: String, color: Color = Air) =
    Text(text, color = color, fontSize = HeroNumberSize, fontWeight = FontWeight.Light, maxLines = 1)

@Composable
fun HeroLabel(text: String, color: Color = TextColor) =
    Text(text, color = color, fontSize = HeroLabelSize, lineHeight = 34.sp, fontWeight = FontWeight.Medium)

@Composable
fun Title(text: String) = Text(text, color = TextColor, fontSize = TitleSize, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold)

@Composable
fun Body(text: String, color: Color = Muted) = Text(text, color = color, fontSize = BodySize)

@Composable
fun Small(text: String, color: Color = Muted, maxLines: Int = Int.MAX_VALUE) =
    Text(text, color = color, fontSize = SmallSize, maxLines = maxLines, overflow = TextOverflow.Ellipsis)

/**
 * A focusable row: scales up, glows and gets a border on focus. [content] learns whether the row has focus, so a
 * row can show its action only when the remote is on it.
 */
@Composable
fun RespiroRow(
    variant: ActionVariant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.(focused: Boolean) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val style = rowStyle(variant)
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        interactionSource = interaction,
        shape = ClickableSurfaceDefaults.shape(shape = RowShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Panel,
            contentColor = style.restLabel,
            focusedContainerColor = style.focusedFill,
            focusedContentColor = style.focusedLabel,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = FOCUSED_SCALE),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(FocusBorderWidth, style.border), shape = RowShape),
        ),
        glow = ClickableSurfaceDefaults.glow(focusedGlow = Glow(style.border.copy(alpha = GLOW_ALPHA), GlowElevation)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) { content(focused) }
    }
}

private data class RowStyle(val restLabel: Color, val focusedFill: Color, val focusedLabel: Color, val border: Color)

private fun rowStyle(variant: ActionVariant) = when (variant) {
    ActionVariant.PRIMARY -> RowStyle(restLabel = Air, focusedFill = Air, focusedLabel = OnAir, border = TextColor)
    ActionVariant.SECONDARY -> RowStyle(restLabel = TextColor, focusedFill = SurfaceFocus, focusedLabel = TextColor, border = Air)
    ActionVariant.PERMANENT -> RowStyle(restLabel = Amber, focusedFill = SurfaceFocus, focusedLabel = Amber, border = Amber)
}

/** One action as a row; a permanent one carries an amber dot. */
@Composable
fun ActionRow(text: String, variant: ActionVariant, onClick: () -> Unit, modifier: Modifier = Modifier) {
    RespiroRow(variant, onClick, modifier) {
        if (variant == ActionVariant.PERMANENT) Box(Modifier.size(8.dp).clip(CircleShape).background(Amber))
        Text(text, fontSize = BodySize, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (variant == ActionVariant.PRIMARY) Text("▶", fontSize = BodySize)
    }
}

/** Puts the remote on this element when the screen opens, or again whenever [key] changes. */
@Composable
fun initialFocus(key: Any = Unit): Modifier {
    val requester = remember { FocusRequester() }
    LaunchedEffect(key) { runCatching { requester.requestFocus() } }
    return Modifier.focusRequester(requester)
}

@Composable
fun NumberedSteps(steps: List<String>) {
    steps.forEachIndexed { index, step ->
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${index + 1}", color = Air, fontSize = 28.sp, fontWeight = FontWeight.Medium)
            Body(step, color = TextColor)
        }
    }
}

/** How big an app is next to the biggest one. */
@Composable
fun SizeBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(modifier.height(6.dp).clip(CircleShape)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).background(Air.copy(alpha = 0.4f)))
    }
}

@Composable
fun ProgressBar(fraction: Float) {
    val shown by animateFloatAsState(fraction.coerceIn(0f, 1f), label = "progress")
    Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Panel)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(shown).background(Air))
    }
}

/** A line saying what the last action did; it never takes focus. */
@Composable
fun ResultBanner(text: String, failed: Boolean) {
    val color = if (failed) Amber else Air
    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(CircleShape).background(color.copy(alpha = 0.15f))
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart,
    ) { Text(text, color = color, fontSize = BodySize) }
}

@Composable
fun PulsingDot() {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val alpha by pulse.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 800), RepeatMode.Reverse),
        label = "alpha",
    )
    Box(Modifier.size(120.dp).alpha(alpha).clip(CircleShape).background(Air))
}

@Composable
fun MonospacePanel(text: String) {
    Box(Modifier.fillMaxWidth().clip(RowShape).background(Panel).padding(20.dp)) {
        Text(text, color = TextColor, fontSize = BodySize, fontFamily = FontFamily.Monospace)
    }
}
