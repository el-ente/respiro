package io.github.elente.respiro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.elente.respiro.adb.ConnectionProblem
import io.github.elente.respiro.adb.SelfAdb
import io.github.elente.respiro.data.AppNames
import io.github.elente.respiro.data.AssetDevices
import io.github.elente.respiro.data.DebloatPlan
import io.github.elente.respiro.data.NamedPackage
import io.github.elente.respiro.data.Getprop
import io.github.elente.respiro.debloat.Backup
import io.github.elente.respiro.debloat.ChangeLog
import io.github.elente.respiro.debloat.Debloater
import io.github.elente.respiro.debloat.Outcome
import io.github.elente.respiro.debloat.SkipReason
import io.github.elente.respiro.debloat.Storage
import io.github.elente.respiro.debloat.TvPackages
import io.github.elente.respiro.debloat.forTv
import io.github.elente.respiro.report.DeviceReport
import io.github.elente.respiro.report.ReportFile
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

    data class Failed(val problem: ConnectionProblem, val detail: String) : TvState

    /** The plan for this TV, already narrowed to what is installed and still on. A null model means an unknown TV. */
    data class Ready(val props: Map<String, String>, val plan: DebloatPlan, val switchedOff: Int) : TvState {
        val manufacturer: String get() = props[Getprop.MANUFACTURER].orEmpty()
        val model: String get() = props[Getprop.MODEL].orEmpty()
    }

    /** The device report was saved to [path] on the TV. */
    data class ReportSaved(val path: String) : TvState

    data class Asking(val plan: DebloatPlan) : TvState

    data class Applying(val done: Int, val total: Int) : TvState

    /** Turning things back on. */
    data object Working : TvState

    /** [freedMb] is null when the TV didn't report its free space. */
    data class Done(val switchedOff: Int, val skipped: List<Pair<NamedPackage, SkipReason>>, val freedMb: Long?) : TvState

    /** Everything Respiro switched off, to turn back on. */
    data class Undo(val packages: List<NamedPackage>) : TvState
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow<TvState>(TvState.Connecting)
    val state: StateFlow<TvState> = _state.asStateFlow()

    private val log = ChangeLog(File(app.filesDir, "switched-off.txt"))
    private val backups = File(app.filesDir, "backups")
    private val database by lazy { AssetDevices.load(app.assets) }
    private val names by lazy { AppNames(app.packageManager, database) }

    /** One job on the TV at a time: a second tap while one runs is ignored. */
    private var job: Job? = null

    init {
        inspect()
    }

    fun inspect() = onTv(TvState.Connecting) { shell ->
        val props = Getprop.parse(shell.run("getprop"))
        val plan = database.plan(database.match(props)).forTv(TvPackages.read(shell))
        TvState.Ready(props, plan, log.all().size)
    }

    fun ask(plan: DebloatPlan) {
        if (plan.ask.isEmpty()) apply(plan, keepUsing = emptySet()) else _state.value = TvState.Asking(plan)
    }

    /** Switches off the plan's list plus every asked-about app the user didn't tick, after a backup. */
    fun apply(plan: DebloatPlan, keepUsing: Set<String>) {
        val packages = plan.disable.map { it.pkg } + plan.ask.map { it.pkg }.filter { it !in keepUsing }
        onTv(TvState.Applying(0, packages.size)) { shell ->
            Backup.save(shell, backups, System.currentTimeMillis())
            val freeBefore = Storage.freeKb(shell)
            val outcomes = Debloater(shell, log).disable(packages) { done, total -> _state.value = TvState.Applying(done, total) }
            TvState.Done(
                switchedOff = outcomes.count { it is Outcome.Disabled },
                skipped = outcomes.filterIsInstance<Outcome.Skipped>().map { names.describe(it.pkg) to it.reason },
                freedMb = freedMb(freeBefore, Storage.freeKb(shell)),
            )
        }
    }

    /** Saves what a contributor needs to add this TV to the database, to Download/ on the TV. */
    fun saveReport(props: Map<String, String>) = onTv(TvState.Working) { shell ->
        val report = DeviceReport.build(
            props = props,
            preinstalled = TvPackages.parse(shell.run("pm list packages -s")),
            disabled = TvPackages.parse(shell.run("pm list packages -d")),
            switchedOff = log.all(),
        )
        val path = DeviceReport.path(props)
        ReportFile.write(shell, path, report)
        TvState.ReportSaved(path)
    }

    fun openUndo() {
        viewModelScope.launch(Dispatchers.IO) { _state.value = undoList() }
    }

    fun turnBackOn(packages: List<String>) = onTv(TvState.Working) { shell ->
        val debloater = Debloater(shell, log)
        packages.forEach(debloater::enable)
        undoList()
    }

    private fun freedMb(beforeKb: Long?, afterKb: Long?): Long? =
        if (beforeKb == null || afterKb == null) null else ((afterKb - beforeKb) / KB_PER_MB).coerceAtLeast(0)

    private fun undoList() = TvState.Undo(log.all().map(names::describe))

    private fun onTv(busy: TvState, work: (io.github.elente.respiro.adb.Shell) -> TvState) {
        if (job?.isActive == true) return
        _state.value = busy
        job = viewModelScope.launch(Dispatchers.IO) {
            _state.value = runCatching { SelfAdb.session(getApplication(), work) }
                .getOrElse { TvState.Failed(ConnectionProblem.of(it), it.message ?: it.javaClass.simpleName) }
        }
    }

    private companion object {
        const val KB_PER_MB = 1024
    }
}
