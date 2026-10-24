package io.github.elente.respiro.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Switch
import androidx.tv.material3.SwitchDefaults
import androidx.tv.material3.Text
import io.github.elente.respiro.MainViewModel
import io.github.elente.respiro.R
import io.github.elente.respiro.TvState
import io.github.elente.respiro.adb.ConnectionProblem
import io.github.elente.respiro.data.DebloatPlan
import io.github.elente.respiro.data.NamedPackage
import io.github.elente.respiro.debloat.SkipReason

@Composable
fun RespiroApp(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    var deviceName by remember { mutableStateOf("") }
    (state as? TvState.Ready)?.let { deviceName = it.plan.model?.name ?: "${it.manufacturer} ${it.model}" }
    // Back steps out of the uninstall screens and lists; on the plan it leaves Respiro as usual.
    BackHandler(enabled = state is TvState.ConfirmUninstall) { vm.cancelUninstall() }
    BackHandler(
        enabled = state is TvState.Installed || state is TvState.Undo || state is TvState.ReportSaved || state is TvState.SpaceFreed,
    ) { vm.inspect() }
    RespiroTheme {
        when (val current = state) {
            TvState.Connecting -> Connecting(deviceName)
            is TvState.Failed -> Failed(deviceName, current, onRetry = vm::inspect)
            is TvState.Ready -> Ready(
                deviceName,
                current,
                onStart = { vm.ask(current.plan, current.userApps) },
                onUndo = vm::openUndo,
                onApps = vm::openInstalled,
                onFreeSpace = vm::freeSpace,
                onReport = { vm.saveReport(current.props) },
            )
            is TvState.Installed -> Installed(deviceName, current, onPick = vm::askUninstall, onTurnOn = vm::turnOn)
            is TvState.ConfirmUninstall -> ConfirmUninstall(current.app, onUninstall = vm::uninstall, onCancel = vm::cancelUninstall)
            is TvState.SpaceFreed -> SpaceFreed(deviceName, current.freedMb, onBack = vm::inspect)
            is TvState.ReportSaved -> ReportSaved(deviceName, current.path, onBack = vm::inspect)
            is TvState.Asking -> Asking(deviceName, current.plan, current.userApps) { keep ->
                vm.apply(current.plan, keep, current.userApps)
            }
            is TvState.Applying -> Busy(deviceName, current.done, current.total)
            TvState.Working -> Busy(deviceName, done = null, total = null)
            is TvState.Done -> Done(deviceName, current, onUndo = vm::openUndo)
            is TvState.Undo -> Undo(deviceName, current.packages, onTurnBackOn = vm::turnBackOn, onBack = vm::inspect)
        }
    }
}

@Composable
private fun Connecting(deviceName: String) {
    RespiroScaffold(deviceName, hero = null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Gutter, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PulsingDot()
            HeroLabel(stringResource(R.string.connecting_title))
            Body(stringResource(R.string.connecting_help))
        }
    }
}

@Composable
private fun Failed(deviceName: String, failed: TvState.Failed, onRetry: () -> Unit) {
    RespiroScaffold(
        deviceName,
        hero = {
            Text("⚠", color = Amber, fontSize = 64.sp)
            HeroLabel(stringResource(R.string.failed_title))
        },
    ) {
        Body(stringResource(failedHelp(failed.problem)), color = TextColor)
        ActionRow(stringResource(R.string.retry), ActionVariant.PRIMARY, onRetry, initialFocus())
        Small(failed.detail, color = Danger)
        Small(stringResource(R.string.debugging_reminder))
    }
}

private fun failedHelp(problem: ConnectionProblem) = when (problem) {
    ConnectionProblem.DEBUGGING_OFF -> R.string.failed_debugging_off
    ConnectionProblem.NOT_ALLOWED -> R.string.failed_not_allowed
    ConnectionProblem.OTHER -> R.string.failed_help
    ConnectionProblem.DURING_WORK -> R.string.failed_during_work
}

@Composable
private fun Ready(
    deviceName: String,
    ready: TvState.Ready,
    onStart: () -> Unit,
    onUndo: () -> Unit,
    onApps: () -> Unit,
    onFreeSpace: () -> Unit,
    onReport: () -> Unit,
) {
    val plan = ready.plan
    val hasWork = plan.disable.isNotEmpty() || plan.ask.isNotEmpty()
    val apps = ready.removable + ready.switchedOff
    val toUninstall = ready.toSwitchOff.filter { it.pkg in ready.userApps }
    // Start runs at once when nothing is asked, so it is permanent, and never focused, when it uninstalls.
    val startVariant = if (toUninstall.isEmpty()) ActionVariant.PRIMARY else ActionVariant.PERMANENT
    val focusStart = hasWork && toUninstall.isEmpty()
    RespiroScaffold(deviceName, hero = { ReadyHero(ready, isReversible = toUninstall.isEmpty()) }) {
        if (hasWork) {
            val start = if (plan.disable.isNotEmpty()) R.string.start else R.string.switch_off_now
            ActionRow(stringResource(start), startVariant, onStart, if (focusStart) initialFocus() else Modifier)
        }
        val firstSecondaryFocus = if (focusStart) Modifier else initialFocus()
        if (ready.switchedOff > 0) {
            ActionRow(stringResource(R.string.see_switched_off, ready.switchedOff), ActionVariant.SECONDARY, onUndo, firstSecondaryFocus)
        }
        // Never the default focus: uninstalling can't be undone.
        if (apps > 0) ActionRow(stringResource(R.string.uninstall_apps, apps), ActionVariant.PERMANENT, onApps)
        val freeSpaceFocus = if (ready.switchedOff > 0) Modifier else firstSecondaryFocus
        ActionRow(stringResource(R.string.free_space), ActionVariant.SECONDARY, onFreeSpace, freeSpaceFocus)
        ActionRow(stringResource(R.string.save_report), ActionVariant.SECONDARY, onReport)
        if (toUninstall.isNotEmpty()) {
            Small(stringResource(R.string.will_uninstall, toUninstall.joinToString(", ") { it.label }), color = Amber)
        }
        val toDisable = ready.toSwitchOff - toUninstall.toSet()
        if (toDisable.isNotEmpty()) {
            Small(stringResource(R.string.what_goes, toDisable.joinToString(", ") { it.label }), maxLines = 1)
        }
    }
}

@Composable
private fun ColumnScope.ReadyHero(ready: TvState.Ready, isReversible: Boolean) {
    val plan = ready.plan
    if (plan.model == null) {
        HeroLabel(stringResource(R.string.hero_safe_basics), color = Amber)
        Small(stringResource(R.string.unknown_tv_help))
    }
    when {
        plan.disable.isNotEmpty() -> {
            HeroNumber("${plan.disable.size}")
            HeroLabel(pluralStringResource(R.plurals.hero_to_disable, plan.disable.size))
            if (isReversible) Small("✓ " + stringResource(R.string.reversible))
        }
        plan.ask.isNotEmpty() -> {
            HeroNumber("${plan.ask.size}")
            HeroLabel(pluralStringResource(R.plurals.hero_to_ask, plan.ask.size))
        }
        else -> {
            HeroNumber("✓")
            HeroLabel(stringResource(R.string.already_clean), color = Air)
        }
    }
}

/** One switch per app Respiro asks about; on means "I use it, leave it". */
@Composable
private fun Asking(deviceName: String, plan: DebloatPlan, userApps: Set<String>, onApply: (Set<String>) -> Unit) {
    var keep by remember { mutableStateOf(plan.ask.map { it.pkg }.toSet()) }
    val going = plan.disable + plan.ask.map { it.pkg }.filter { it !in keep }
    val uninstalling = going.count { it in userApps }
    val switchingOff = going.size - uninstalling
    RespiroScaffold(
        deviceName,
        hero = {
            Title(stringResource(R.string.ask_title))
            Body(stringResource(R.string.ask_help))
        },
    ) {
        LazyColumn(modifier = Modifier.weight(1f, fill = false), contentPadding = ScaledRowPadding, verticalArrangement = Arrangement.spacedBy(RowGap)) {
            itemsIndexed(plan.ask, key = { _, entry -> entry.pkg }) { index, entry ->
                val used = entry.pkg in keep
                RespiroRow(
                    ActionVariant.SECONDARY,
                    onClick = { keep = if (used) keep - entry.pkg else keep + entry.pkg },
                    modifier = if (index == 0) initialFocus() else Modifier,
                ) {
                    Text(entry.label, fontSize = RowLabelSize, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    AskChip(used, isUserApp = entry.pkg in userApps)
                    Switch(checked = used, onCheckedChange = null, colors = switchColors())
                }
            }
        }
        val continueText = if (uninstalling == 0) stringResource(R.string.continue_count, switchingOff)
        else stringResource(R.string.continue_count_uninstall, switchingOff, uninstalling)
        val continueVariant = if (uninstalling == 0) ActionVariant.PRIMARY else ActionVariant.PERMANENT
        ActionRow(continueText, continueVariant, { onApply(keep) })
    }
}

@Composable
private fun AskChip(used: Boolean, isUserApp: Boolean) = when {
    used -> Text(stringResource(R.string.chip_use), color = Air, fontSize = SmallSize)
    isUserApp -> Text(stringResource(R.string.chip_uninstall), color = Amber, fontSize = SmallSize)
    else -> Text(stringResource(R.string.chip_off), color = Muted, fontSize = SmallSize)
}

@Composable
private fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = OnAir,
    checkedTrackColor = Air,
    uncheckedThumbColor = Muted,
    uncheckedTrackColor = Panel,
)

/** Switching off when [done] and [total] are known, turning back on otherwise. Nothing to press. */
@Composable
private fun Busy(deviceName: String, done: Int?, total: Int?) {
    RespiroScaffold(
        deviceName,
        hero = {
            if (done != null && total != null) {
                HeroNumber("$done / $total")
                HeroLabel(stringResource(R.string.hero_applying))
            } else {
                HeroLabel(stringResource(R.string.working))
            }
        },
    ) {
        if (done != null && total != null && total > 0) ProgressBar(done.toFloat() / total)
        Body(stringResource(R.string.dont_turn_off))
    }
}

@Composable
private fun Done(deviceName: String, done: TvState.Done, onUndo: () -> Unit) {
    RespiroScaffold(
        deviceName,
        hero = {
            HeroNumber("✓ ${done.switchedOff}")
            HeroLabel(pluralStringResource(R.plurals.hero_switched_off, done.switchedOff))
            if (done.uninstalled > 0) {
                Body(pluralStringResource(R.plurals.apps_uninstalled, done.uninstalled, done.uninstalled), color = TextColor)
            }
            done.freedMb?.let { Body(stringResource(R.string.space_freed, it), color = TextColor) }
        },
    ) {
        Text(stringResource(R.string.last_step), color = Amber, fontSize = BodySize, fontWeight = FontWeight.SemiBold)
        NumberedSteps(listOf(R.string.debug_step_1, R.string.debug_step_2, R.string.debug_step_3).map { stringResource(it) })
        ActionRow(stringResource(R.string.see_switched_off_all), ActionVariant.PRIMARY, onUndo, initialFocus())
        // Skipped packages last and scrolling, so a long list can't push the steps or the button off screen.
        LazyColumn {
            items(done.skipped, key = { it.first.pkg }) { (named, reason) -> Small("·  ${named.label}: ${skipReason(reason)}") }
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

@Composable
private fun Undo(deviceName: String, packages: List<NamedPackage>, onTurnBackOn: (List<String>) -> Unit, onBack: () -> Unit) {
    RespiroScaffold(
        deviceName,
        hero = {
            HeroNumber("${packages.size}")
            HeroLabel(pluralStringResource(R.plurals.hero_switched_off, packages.size))
            Small(stringResource(R.string.undo_help))
        },
    ) {
        if (packages.isEmpty()) {
            Body(stringResource(R.string.nothing_switched_off))
            ActionRow(stringResource(R.string.back), ActionVariant.PRIMARY, onBack, initialFocus())
            return@RespiroScaffold
        }
        LazyColumn(modifier = Modifier.weight(1f, fill = false), contentPadding = ScaledRowPadding, verticalArrangement = Arrangement.spacedBy(RowGap)) {
            itemsIndexed(packages, key = { _, named -> named.pkg }) { index, named ->
                // Turning back on is reversible, so the first row may take focus.
                UndoRow(named, { onTurnBackOn(listOf(named.pkg)) }, if (index == 0) initialFocus() else Modifier)
            }
        }
        ActionRow(stringResource(R.string.turn_all_back_on), ActionVariant.SECONDARY, { onTurnBackOn(packages.map { it.pkg }) })
    }
}

@Composable
private fun UndoRow(named: NamedPackage, onClick: () -> Unit, modifier: Modifier) {
    RespiroRow(ActionVariant.SECONDARY, onClick, modifier) { focused ->
        Column(Modifier.weight(1f)) {
            Text(named.label, fontSize = RowLabelSize, fontWeight = FontWeight.Medium)
            Small(named.reason ?: named.pkg, maxLines = 1)
        }
        if (focused) Text(stringResource(R.string.turn_back_on) + " ▸", color = Air, fontSize = SmallSize)
    }
}

@Composable
private fun SpaceFreed(deviceName: String, freedMb: Long?, onBack: () -> Unit) {
    RespiroScaffold(
        deviceName,
        hero = {
            HeroNumber(if (freedMb != null) stringResource(R.string.hero_freed_mb, freedMb) else "✓")
            HeroLabel(stringResource(R.string.caches_cleared))
        },
    ) {
        Body(stringResource(R.string.caches_help))
        ActionRow(stringResource(R.string.back), ActionVariant.PRIMARY, onBack, initialFocus())
    }
}

@Composable
private fun ReportSaved(deviceName: String, path: String, onBack: () -> Unit) {
    RespiroScaffold(
        deviceName,
        hero = {
            HeroNumber("✓")
            HeroLabel(stringResource(R.string.report_saved_title))
        },
    ) {
        MonospacePanel(path.removePrefix("/sdcard/"))
        Body(stringResource(R.string.report_saved_help))
        ActionRow(stringResource(R.string.back), ActionVariant.PRIMARY, onBack, initialFocus())
    }
}
