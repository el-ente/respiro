package io.github.elente.respiro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

@Composable
fun RespiroApp(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
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
                is TvState.Ready -> Ready(current, onStart = { vm.ask(current.plan) }, onUndo = vm::openUndo)
                is TvState.Asking -> Asking(current.plan, onApply = { keep -> vm.apply(current.plan, keep) })
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
private fun Ready(ready: TvState.Ready, onStart: () -> Unit, onUndo: () -> Unit) {
    val plan = ready.plan
    val model = plan.model
    if (model != null) {
        Heading(stringResource(R.string.known_tv, model.name))
    } else {
        Heading(stringResource(R.string.unknown_tv, ready.manufacturer, ready.model))
        Body(stringResource(R.string.unknown_tv_help))
    }
    Body(pluralStringResource(R.plurals.to_disable, plan.disable.size, plan.disable.size))
    groupCounts(plan).forEach { (group, count) -> Body("  ·  $group: $count") }
    Body(pluralStringResource(R.plurals.to_ask, plan.ask.size, plan.ask.size))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        when {
            plan.disable.isNotEmpty() -> FocusedButton(stringResource(R.string.start), onStart)
            plan.ask.isNotEmpty() -> FocusedButton(stringResource(R.string.switch_off_now), onStart)
        }
        if (ready.switchedOff > 0) Button(onClick = onUndo) { Text(stringResource(R.string.see_switched_off, ready.switchedOff)) }
    }
}

/** One switch per app Respiro asks about; on means "I use it, leave it". */
@Composable
private fun Asking(plan: DebloatPlan, onApply: (Set<String>) -> Unit) {
    var keep by remember { mutableStateOf(plan.ask.map { it.pkg }.toSet()) }
    Heading(stringResource(R.string.ask_title))
    Body(stringResource(R.string.ask_help))
    FocusedButton(stringResource(R.string.switch_off_now)) { onApply(keep) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(plan.ask, key = { it.pkg }) { entry ->
            val used = entry.pkg in keep
            ListItem(
                selected = false,
                onClick = { keep = if (used) keep - entry.pkg else keep + entry.pkg },
                headlineContent = { Text(entry.label) },
                trailingContent = { Switch(checked = used, onCheckedChange = null) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Done(done: TvState.Done, onUndo: () -> Unit) {
    Heading(pluralStringResource(R.plurals.switched_off, done.switchedOff, done.switchedOff))
    done.freedMb?.let { Body(stringResource(R.string.space_freed, it)) }
    Body(stringResource(R.string.turn_debugging_off))
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

@Composable
private fun groupCounts(plan: DebloatPlan): List<Pair<String, Int>> {
    val labels = mapOf(
        "telemetry" to stringResource(R.string.group_telemetry),
        "ads" to stringResource(R.string.group_ads),
        "services" to stringResource(R.string.group_services),
        "preloaded" to stringResource(R.string.group_preloaded),
    )
    return plan.disable.groupingBy { it.group }.eachCount().map { (group, count) -> (labels[group] ?: group) to count }
}

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
