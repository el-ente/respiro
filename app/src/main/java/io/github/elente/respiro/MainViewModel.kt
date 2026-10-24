package io.github.elente.respiro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.elente.respiro.adb.SelfAdb
import io.github.elente.respiro.data.AssetDevices
import io.github.elente.respiro.data.DebloatPlan
import io.github.elente.respiro.data.Getprop
import io.github.elente.respiro.debloat.Backup
import io.github.elente.respiro.debloat.ChangeLog
import io.github.elente.respiro.debloat.Debloater
import io.github.elente.respiro.debloat.Outcome
import io.github.elente.respiro.debloat.TvPackages
import io.github.elente.respiro.debloat.forTv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/** Where Respiro stands with this TV. */
sealed interface TvState {
    data object Connecting : TvState

    data class Failed(val reason: String) : TvState

    /** The plan for this TV, already narrowed to what is installed and still on. A null model means an unknown TV. */
    data class Ready(val manufacturer: String, val model: String, val plan: DebloatPlan, val switchedOff: Int) : TvState

    data class Asking(val plan: DebloatPlan) : TvState

    data class Applying(val done: Int, val total: Int) : TvState

    /** Turning things back on. */
    data object Working : TvState

    data class Done(val outcomes: List<Outcome>) : TvState

    /** Everything Respiro switched off, to turn back on. */
    data class Undo(val packages: List<String>) : TvState
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow<TvState>(TvState.Connecting)
    val state: StateFlow<TvState> = _state.asStateFlow()

    private val log = ChangeLog(File(app.filesDir, "switched-off.txt"))
    private val backups = File(app.filesDir, "backups")

    /** One job on the TV at a time: a second tap while one runs is ignored. */
    private var job: Job? = null

    init {
        inspect()
    }

    fun inspect() = onTv(TvState.Connecting) { shell ->
        val props = Getprop.parse(shell.run("getprop"))
        val database = AssetDevices.load(getApplication<Application>().assets)
        val plan = database.plan(database.match(props)).forTv(TvPackages.read(shell))
        TvState.Ready(props[MANUFACTURER].orEmpty(), props[MODEL].orEmpty(), plan, log.all().size)
    }

    fun ask(plan: DebloatPlan) {
        if (plan.ask.isEmpty()) apply(plan, keepUsing = emptySet()) else _state.value = TvState.Asking(plan)
    }

    /** Switches off the plan's list plus every asked-about app the user didn't tick, after a backup. */
    fun apply(plan: DebloatPlan, keepUsing: Set<String>) {
        val packages = plan.disable.map { it.pkg } + plan.ask.map { it.pkg }.filter { it !in keepUsing }
        onTv(TvState.Applying(0, packages.size)) { shell ->
            Backup.save(shell, backups, System.currentTimeMillis())
            val outcomes = Debloater(shell, log).disable(packages) { done, total -> _state.value = TvState.Applying(done, total) }
            TvState.Done(outcomes)
        }
    }

    fun openUndo() {
        _state.value = TvState.Undo(log.all())
    }

    fun turnBackOn(packages: List<String>) = onTv(TvState.Working) { shell ->
        val debloater = Debloater(shell, log)
        packages.forEach(debloater::enable)
        TvState.Undo(log.all())
    }

    private fun onTv(busy: TvState, work: (io.github.elente.respiro.adb.Shell) -> TvState) {
        if (job?.isActive == true) return
        _state.value = busy
        job = viewModelScope.launch(Dispatchers.IO) {
            _state.value = runCatching { SelfAdb.session(getApplication(), work) }
                .getOrElse { TvState.Failed(it.message ?: it.javaClass.simpleName) }
        }
    }

    private companion object {
        const val MANUFACTURER = "ro.product.manufacturer"
        const val MODEL = "ro.product.model"
    }
}
