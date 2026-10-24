package io.github.elente.respiro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import io.github.elente.respiro.MainViewModel
import io.github.elente.respiro.R
import io.github.elente.respiro.TvState
import io.github.elente.respiro.data.DebloatPlan

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
                is TvState.Failed -> {
                    Heading(stringResource(R.string.failed_title))
                    Body(stringResource(R.string.failed_help))
                    Body(current.reason)
                }
                is TvState.Ready -> Plan(current)
            }
        }
    }
}

@Composable
private fun Plan(ready: TvState.Ready) {
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
}

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

@Composable
private fun Heading(text: String) = Text(text, color = TextColor, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)

@Composable
private fun Body(text: String) = Text(text, color = Muted, fontSize = 18.sp)
