package io.github.elente.respiro.ui

import android.text.format.Formatter
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import io.github.elente.respiro.AppsResult
import io.github.elente.respiro.InstalledApp
import io.github.elente.respiro.R
import io.github.elente.respiro.TvState

private val AppIconSize = 40.dp
private val DialogIconSize = 64.dp
private val DialogWidth = 640.dp

/** Apps the user installed, biggest first, to uninstall; then the apps Respiro switched off, to turn back on. */
@Composable
fun Installed(deviceName: String, installed: TvState.Installed, onPick: (InstalledApp) -> Unit, onTurnOn: (InstalledApp) -> Unit) {
    RespiroScaffold(deviceName, hero = { InstalledHero(installed.apps) }) {
        installed.result?.let { AppsResultBanner(it) }
        // An app row may take focus although it is permanent: OK only opens the confirmation, with Cancel focused.
        val focused = defaultFocusApp(installed)
        val focusModifier = initialFocus(installed)
        val rowModifier = { app: InstalledApp -> if (app == focused) focusModifier else Modifier }
        val biggest = installed.apps.maxOfOrNull { it.bytes }?.coerceAtLeast(1) ?: 1
        LazyColumn(contentPadding = ScaledRowPadding, verticalArrangement = Arrangement.spacedBy(RowGap)) {
            items(installed.apps, key = { "installed:" + it.named.pkg }) { app ->
                InstalledRow(app, app.bytes.toFloat() / biggest, { onPick(app) }, rowModifier(app))
            }
            if (installed.switchedOff.isNotEmpty()) {
                item(key = "switched-off-title") { Body(stringResource(R.string.switched_off_section)) }
            }
            items(installed.switchedOff, key = { "off:" + it.named.pkg }) { app ->
                SwitchedOffRow(app, { onTurnOn(app) }, rowModifier(app))
            }
        }
    }
}

/** After a turn-on, stay in the switched-off section so the next OK can't open an uninstall by surprise. */
private fun defaultFocusApp(installed: TvState.Installed): InstalledApp? {
    val afterTurnOn = installed.result is AppsResult.TurnedOn || installed.result is AppsResult.TurnOnFailed
    return if (afterTurnOn) installed.switchedOff.firstOrNull() ?: installed.apps.firstOrNull()
    else installed.apps.firstOrNull() ?: installed.switchedOff.firstOrNull()
}

@Composable
private fun InstalledHero(apps: List<InstalledApp>) {
    if (apps.isEmpty()) {
        HeroLabel(stringResource(R.string.installed_empty))
        return
    }
    HeroNumber(Formatter.formatShortFileSize(LocalContext.current, apps.sumOf { it.bytes }))
    HeroLabel(pluralStringResource(R.plurals.hero_installed, apps.size, apps.size))
}

@Composable
private fun InstalledRow(app: InstalledApp, sizeFraction: Float, onClick: () -> Unit, modifier: Modifier) {
    RespiroRow(ActionVariant.PERMANENT, onClick, modifier) {
        AppIcon(app, AppIconSize)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(app.named.label, fontSize = RowLabelSize, fontWeight = FontWeight.Medium)
            SizeBar(sizeFraction, Modifier.fillMaxWidth())
        }
        Text(Formatter.formatShortFileSize(LocalContext.current, app.bytes), fontSize = BodySize)
    }
}

@Composable
private fun SwitchedOffRow(app: InstalledApp, onClick: () -> Unit, modifier: Modifier) {
    RespiroRow(ActionVariant.SECONDARY, onClick, modifier) {
        AppIcon(app, AppIconSize)
        Column(Modifier.weight(1f)) {
            Text(app.named.label, fontSize = RowLabelSize, fontWeight = FontWeight.Medium)
            Small(stringResource(R.string.switched_off_row))
        }
        Text(stringResource(R.string.turn_back_on), color = Air, fontSize = SmallSize)
    }
}

@Composable
private fun AppIcon(app: InstalledApp, size: Dp) {
    app.icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(size)) }
}

@Composable
private fun AppsResultBanner(result: AppsResult) {
    val context = LocalContext.current
    when (result) {
        is AppsResult.Uninstalled -> ResultBanner(
            stringResource(R.string.uninstalled_result, result.label, Formatter.formatShortFileSize(context, result.bytes)) +
                "  ·  " + stringResource(R.string.uninstalled_total, Formatter.formatShortFileSize(context, result.totalBytes)),
            failed = false,
        )
        is AppsResult.UninstallFailed -> ResultBanner(stringResource(R.string.uninstall_failed, result.label), failed = true)
        is AppsResult.TurnedOn -> ResultBanner(stringResource(R.string.turned_on_result, result.label), failed = false)
        is AppsResult.TurnOnFailed -> ResultBanner(stringResource(R.string.turn_on_failed, result.label), failed = true)
    }
}

/** Cancel comes first and takes focus, so pressing OK twice by mistake removes nothing. */
@Composable
fun ConfirmUninstall(app: InstalledApp, onUninstall: (InstalledApp) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.width(DialogWidth).clip(RoundedCornerShape(28.dp)).background(Panel).padding(40.dp),
            verticalArrangement = Arrangement.spacedBy(Gutter),
        ) {
            AppIcon(app, DialogIconSize)
            Title(stringResource(R.string.uninstall_confirm_title, app.named.label))
            Body(stringResource(R.string.uninstall_confirm_body, Formatter.formatShortFileSize(context, app.bytes)))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ActionRow(stringResource(R.string.cancel), ActionVariant.SECONDARY, onCancel, Modifier.weight(1f).then(initialFocus()))
                ActionRow(stringResource(R.string.uninstall), ActionVariant.PERMANENT, { onUninstall(app) }, Modifier.weight(1f))
            }
        }
    }
}
