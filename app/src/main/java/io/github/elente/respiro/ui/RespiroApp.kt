package io.github.elente.respiro.ui

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import io.github.elente.respiro.AppsResult
import io.github.elente.respiro.InstalledApp
import io.github.elente.respiro.MainViewModel
import io.github.elente.respiro.R
import io.github.elente.respiro.TvState
import io.github.elente.respiro.adb.ConnectionProblem
import io.github.elente.respiro.data.DebloatPlan
import io.github.elente.respiro.data.NamedPackage
import io.github.elente.respiro.debloat.SkipReason

private val Background = Color(0xFF0B0F14)
private val TextColor = Color(0xFFF2F4F7)
private val Muted = Color(0xFF9AA4B2)
private val Success = Color(0xFF7FD1AE)
private val Warning = Color(0xFFF2B84B)

@Composable
fun RespiroApp(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    // Back steps out of the uninstall screens and lists; on the plan it leaves Respiro as usual.
    BackHandler(enabled = state is TvState.ConfirmUninstall) { vm.cancelUninstall() }
    BackHandler(
        enabled = state is TvState.Installed || state is TvState.Undo || state is TvState.ReportSaved || state is TvState.SpaceFreed,
    ) { vm.inspect() }
    MaterialTheme(colorScheme = darkColorScheme()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(horizontal = 56.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.app_name), color = TextColor, fontSize = 34.sp, fontWeight = FontWeight.Light)
            when (val current = state) {
                TvState.Connecting -> Body(stringResource(R.string.connecting))
                is TvState.Failed -> Failed(current, onRetry = vm::inspect)
                is TvState.Ready -> Ready(
                    current,
                    onStart = { vm.ask(current.plan, current.userApps) },
                    onUndo = vm::openUndo,
                    onUninstall = vm::openInstalled,
                    onFreeSpace = vm::freeSpace,
                    onReport = { vm.saveReport(current.props) },
                )
                is TvState.Installed -> Installed(current, onPick = vm::askUninstall, onTurnOn = vm::turnOn)
                is TvState.SpaceFreed -> SpaceFreed(current.freedMb, onBack = vm::inspect)
                is TvState.ConfirmUninstall -> ConfirmUninstall(current.app, onUninstall = vm::uninstall, onCancel = vm::cancelUninstall)
                is TvState.ReportSaved -> ReportSaved(current.path, onBack = vm::inspect)
                is TvState.Asking -> Asking(current.plan, current.userApps, onApply = { keep -> vm.apply(current.plan, keep, current.userApps) })
                is TvState.Applying -> Body(stringResource(R.string.applying, current.done, current.total))
                TvState.Working -> Body(stringResource(R.string.working))
                is TvState.Done -> Done(current, onUndo = vm::openUndo)
                is TvState.Undo -> Undo(current.packages, onTurnBackOn = vm::turnBackOn, onBack = vm::inspect)
            }
        }
    }
}

@Composable
private fun Failed(failed: TvState.Failed, onRetry: () -> Unit) {
    Heading(stringResource(R.string.failed_title))
    when (failed.problem) {
        ConnectionProblem.DEBUGGING_OFF -> Body(stringResource(R.string.failed_debugging_off))
        ConnectionProblem.NOT_ALLOWED -> Body(stringResource(R.string.failed_not_allowed))
        ConnectionProblem.OTHER -> Body(stringResource(R.string.failed_help))
        ConnectionProblem.DURING_WORK -> Body(stringResource(R.string.failed_during_work))
    }
    Body(failed.detail)
    FocusedButton(stringResource(R.string.retry), onRetry)
    Body(stringResource(R.string.debugging_reminder))
}

@Composable
private fun Ready(
    ready: TvState.Ready,
    onStart: () -> Unit,
    onUndo: () -> Unit,
    onUninstall: () -> Unit,
    onFreeSpace: () -> Unit,
    onReport: () -> Unit,
) {
    val plan = ready.plan
    val model = plan.model
    if (model != null) {
        Heading(stringResource(R.string.known_tv, model.name))
    } else {
        Heading(stringResource(R.string.unknown_tv, ready.manufacturer, ready.model))
        Body(stringResource(R.string.unknown_tv_help))
    }
    if (plan.disable.isNotEmpty()) {
        Body(pluralStringResource(R.plurals.to_disable, plan.disable.size, plan.disable.size) + "  ·  " + stringResource(R.string.reversible))
    }
    if (plan.ask.isNotEmpty()) Body(pluralStringResource(R.plurals.to_ask, plan.ask.size, plan.ask.size))
    // Buttons above the list, so a long list can never push them off screen.
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        when {
            plan.disable.isNotEmpty() -> FocusedButton(stringResource(R.string.start), onStart)
            plan.ask.isNotEmpty() -> FocusedButton(stringResource(R.string.switch_off_now), onStart)
        }
        if (ready.switchedOff > 0) Button(onClick = onUndo) { Text(stringResource(R.string.see_switched_off, ready.switchedOff)) }
        // Never the default focus: uninstalling can't be undone.
        val apps = ready.removable + ready.switchedOff
        if (apps > 0) Button(onClick = onUninstall) { Text(stringResource(R.string.uninstall_apps, apps)) }
        Button(onClick = onFreeSpace) { Text(stringResource(R.string.free_space)) }
        Button(onClick = onReport) { Text(stringResource(R.string.save_report)) }
    }
    if (plan.disable.isEmpty() && plan.ask.isEmpty()) Body(stringResource(R.string.already_clean))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        items(ready.toSwitchOff, key = { it.pkg }) { named ->
            ListItem(
                selected = false,
                onClick = {},
                headlineContent = { Text(named.label) },
                supportingContent = {
                    val reason = named.reason ?: named.pkg
                    Text(if (named.pkg in ready.userApps) stringResource(R.string.will_uninstall, reason) else reason)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Apps the user installed, biggest first: OK on one asks to confirm its uninstall. */
@Composable
private fun Installed(installed: TvState.Installed, onPick: (InstalledApp) -> Unit, onTurnOn: (InstalledApp) -> Unit) {
    val context = LocalContext.current
    val total = installed.apps.sumOf { it.bytes }
    Heading(stringResource(R.string.installed_title))
    installed.result?.let { AppsResultLine(it) }
    if (installed.apps.isEmpty() && installed.switchedOff.isEmpty()) {
        Body(stringResource(R.string.installed_empty))
        return
    }
    if (installed.apps.isNotEmpty()) {
        Body(pluralStringResource(R.plurals.installed_summary, installed.apps.size, installed.apps.size, Formatter.formatShortFileSize(context, total)))
    }
    val firstRow = remember { FocusRequester() }
    val first = installed.apps.firstOrNull() ?: installed.switchedOff.firstOrNull()
    val focusFirst = { app: InstalledApp -> if (app == first) Modifier.focusRequester(firstRow) else Modifier }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        items(installed.apps, key = { "installed:" + it.named.pkg }) { app ->
            ListItem(
                selected = false,
                onClick = { onPick(app) },
                headlineContent = { Text(app.named.label) },
                leadingContent = { AppIcon(app) },
                trailingContent = { Text(Formatter.formatShortFileSize(context, app.bytes)) },
                modifier = Modifier.fillMaxWidth().then(focusFirst(app)),
            )
        }
        if (installed.switchedOff.isNotEmpty()) {
            item(key = "switched-off-title") { Body(stringResource(R.string.switched_off_section)) }
        }
        items(installed.switchedOff, key = { "off:" + it.named.pkg }) { app ->
            ListItem(
                selected = false,
                onClick = { onTurnOn(app) },
                headlineContent = { Text(app.named.label) },
                supportingContent = { Text(stringResource(R.string.switched_off_row)) },
                leadingContent = { AppIcon(app) },
                trailingContent = { Text(stringResource(R.string.turn_back_on)) },
                modifier = Modifier.fillMaxWidth().then(focusFirst(app)),
            )
        }
    }
    LaunchedEffect(installed) { runCatching { firstRow.requestFocus() } }
}

@Composable
private fun AppIcon(app: InstalledApp) {
    app.icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(40.dp)) }
}

@Composable
private fun AppsResultLine(result: AppsResult) {
    val context = LocalContext.current
    when (result) {
        is AppsResult.Uninstalled -> Text(
            stringResource(R.string.uninstalled_result, result.label, Formatter.formatShortFileSize(context, result.bytes)) +
                "  ·  " + stringResource(R.string.uninstalled_total, Formatter.formatShortFileSize(context, result.totalBytes)),
            color = Success,
            fontSize = 18.sp,
        )
        is AppsResult.UninstallFailed -> Text(stringResource(R.string.uninstall_failed, result.label), color = Warning, fontSize = 18.sp)
        is AppsResult.TurnedOn -> Text(stringResource(R.string.turned_on_result, result.label), color = Success, fontSize = 18.sp)
        is AppsResult.TurnOnFailed -> Text(stringResource(R.string.turn_on_failed, result.label), color = Warning, fontSize = 18.sp)
    }
}

@Composable
private fun SpaceFreed(freedMb: Long?, onBack: () -> Unit) {
    Text(
        if (freedMb != null) stringResource(R.string.space_freed, freedMb) else stringResource(R.string.caches_cleared),
        color = Success,
        fontSize = 32.sp,
        fontWeight = FontWeight.SemiBold,
    )
    Body(stringResource(R.string.caches_help))
    FocusedButton(stringResource(R.string.back), onBack)
}

/** Cancel comes first and takes focus, so pressing OK twice by mistake removes nothing. */
@Composable
private fun ConfirmUninstall(app: InstalledApp, onUninstall: (InstalledApp) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    Heading(stringResource(R.string.uninstall_confirm_title, app.named.label))
    Body(stringResource(R.string.uninstall_confirm_body, Formatter.formatShortFileSize(context, app.bytes)))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        FocusedButton(stringResource(R.string.cancel), onCancel)
        Button(onClick = { onUninstall(app) }) { Text(stringResource(R.string.uninstall), color = Warning) }
    }
}

@Composable
private fun ReportSaved(path: String, onBack: () -> Unit) {
    Heading(stringResource(R.string.report_saved_title))
    Body(stringResource(R.string.report_saved_help, path.removePrefix("/sdcard/")))
    FocusedButton(stringResource(R.string.back), onBack)
}

/** One switch per app Respiro asks about; on means "I use it, leave it". */
@Composable
private fun ColumnScope.Asking(plan: DebloatPlan, userApps: Set<String>, onApply: (Set<String>) -> Unit) {
    var keep by remember { mutableStateOf(plan.ask.map { it.pkg }.toSet()) }
    Heading(stringResource(R.string.ask_title))
    Body(stringResource(R.string.ask_help))
    LazyColumn(modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(plan.ask, key = { it.pkg }) { entry ->
            val used = entry.pkg in keep
            ListItem(
                selected = false,
                onClick = { keep = if (used) keep - entry.pkg else keep + entry.pkg },
                headlineContent = { Text(entry.label) },
                supportingContent = {
                    val off = if (entry.pkg in userApps) R.string.i_dont_use_it_uninstall else R.string.i_dont_use_it
                    Text(stringResource(if (used) R.string.i_use_it else off))
                },
                trailingContent = { Switch(checked = used, onCheckedChange = null) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    FocusedButton(stringResource(R.string.switch_off_now)) { onApply(keep) }
}

@Composable
private fun Done(done: TvState.Done, onUndo: () -> Unit) {
    Text(
        pluralStringResource(R.plurals.switched_off, done.switchedOff, done.switchedOff),
        color = Success,
        fontSize = 32.sp,
        fontWeight = FontWeight.SemiBold,
    )
    if (done.uninstalled > 0) {
        Text(pluralStringResource(R.plurals.apps_uninstalled, done.uninstalled, done.uninstalled), color = Success, fontSize = 22.sp)
    }
    done.freedMb?.let { Text(stringResource(R.string.space_freed, it), color = Success, fontSize = 22.sp) }
    Text(stringResource(R.string.last_step), color = Warning, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    listOf(R.string.debug_step_1, R.string.debug_step_2, R.string.debug_step_3).forEachIndexed { index, step ->
        Body("${index + 1}.  ${stringResource(step)}")
    }
    FocusedButton(stringResource(R.string.see_switched_off_all), onUndo)
    // Skipped packages last and scrolling, so a long list can't push the reminder or the button off screen.
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(done.skipped, key = { it.first.pkg }) { (named, reason) -> Body("  ·  ${named.label}: ${skipReason(reason)}") }
    }
}

@Composable
private fun Undo(packages: List<NamedPackage>, onTurnBackOn: (List<String>) -> Unit, onBack: () -> Unit) {
    Heading(stringResource(R.string.undo_title))
    Body(stringResource(R.string.undo_help))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        FocusedButton(stringResource(R.string.back), onBack)
        if (packages.isNotEmpty()) Button(onClick = { onTurnBackOn(packages.map { it.pkg }) }) { Text(stringResource(R.string.turn_all_back_on)) }
    }
    if (packages.isEmpty()) Body(stringResource(R.string.nothing_switched_off))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(packages, key = { it.pkg }) { named ->
            ListItem(
                selected = false,
                onClick = { onTurnBackOn(listOf(named.pkg)) },
                headlineContent = { Text(named.label) },
                supportingContent = { Text(named.reason ?: named.pkg) },
                trailingContent = { Text(stringResource(R.string.turn_back_on)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun skipReason(reason: SkipReason): String = stringResource(
    when (reason) {
        SkipReason.PROTECTED -> R.string.skip_protected
        SkipReason.OWNS_WIFI -> R.string.skip_owns_wifi
        SkipReason.REFUSED -> R.string.skip_refused
    },
)

/** A button that takes focus when its screen opens, so the remote always lands somewhere. */
@Composable
private fun FocusedButton(text: String, onClick: () -> Unit) {
    val focus = remember { FocusRequester() }
    Button(onClick = onClick, modifier = Modifier.focusRequester(focus)) { Text(text) }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

@Composable
private fun Heading(text: String) = Text(text, color = TextColor, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)

@Composable
private fun Body(text: String) = Text(text, color = Muted, fontSize = 18.sp)
