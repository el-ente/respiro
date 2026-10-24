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
import io.github.elente.respiro.debloat.Uninstaller
import androidx.compose.ui.graphics.ImageBitmap
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

/** An app the user installed, as the uninstall list shows it. */
data class InstalledApp(val named: NamedPackage, val icon: ImageBitmap?, val bytes: Long)

/** What the last action on the apps screen did. */
sealed interface AppsResult {
    val label: String

    /** [totalBytes] adds up everything uninstalled this session. */
    data class Uninstalled(override val label: String, val bytes: Long, val totalBytes: Long) : AppsResult

    data class UninstallFailed(override val label: String) : AppsResult

    data class TurnedOn(override val label: String) : AppsResult

    data class TurnOnFailed(override val label: String) : AppsResult
}

/** Where Respiro stands with this TV. */
sealed interface TvState {
    data object Connecting : TvState

    data class Failed(val problem: ConnectionProblem, val detail: String) : TvState

    /** The plan for this TV, already narrowed to what is installed and still on. A null model means an unknown TV. */
    /** [toSwitchOff] names each package of the plan's disable list, in the same order. */
    /** [userApps] are the plan's packages the user installed: uninstalled, not switched off. */
    data class Ready(
        val props: Map<String, String>,
        val plan: DebloatPlan,
        val toSwitchOff: List<NamedPackage>,
        val switchedOff: Int,
        val userApps: Set<String>,
        val removable: Int,
    ) : TvState {
        val manufacturer: String get() = props[Getprop.MANUFACTURER].orEmpty()
        val model: String get() = props[Getprop.MODEL].orEmpty()
    }

    /**
     * The apps screen: apps the user installed, biggest first, to uninstall; and the apps Respiro switched off, to
     * turn back on. Never apps switched off by anyone else. [result] says what the last action did.
     */
    data class Installed(val apps: List<InstalledApp>, val switchedOff: List<InstalledApp>, val result: AppsResult?) : TvState

    data class ConfirmUninstall(val app: InstalledApp) : TvState

    /** Caches cleared; [freedMb] is null when the TV didn't report its free space. */
    data class SpaceFreed(val freedMb: Long?) : TvState

    /** The device report was saved to [path] on the TV. */
    data class ReportSaved(val path: String) : TvState

    data class Asking(val plan: DebloatPlan, val userApps: Set<String>) : TvState

    data class Applying(val done: Int, val total: Int) : TvState

    /** Turning things back on. */
    data object Working : TvState

    /** [freedMb] is null when the TV didn't report its free space. */
    data class Done(
        val switchedOff: Int,
        val uninstalled: Int,
        val skipped: List<Pair<NamedPackage, SkipReason>>,
        val freedMb: Long?,
    ) : TvState

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

    private val selfPackage = app.packageName
    private var lastInstalled: TvState.Installed? = null
    private var freedByUninstall = 0L

    init {
        inspect()
    }

    fun inspect() = onTv(TvState.Connecting) { shell ->
        val props = Getprop.parse(shell.run("getprop"))
        val tv = TvPackages.read(shell)
        val plan = database.plan(database.match(props)).forTv(tv)
        val userApps = (plan.disable.map { it.pkg } + plan.ask.map { it.pkg }).filter(tv::isUserApp).toSet()
        TvState.Ready(
            props = props,
            plan = plan,
            toSwitchOff = plan.disable.map { names.describe(it.pkg) },
            switchedOff = log.all().size,
            userApps = userApps,
            removable = Uninstaller(shell, selfPackage).removable().size,
        )
    }

    fun ask(plan: DebloatPlan, userApps: Set<String>) {
        if (plan.ask.isEmpty()) apply(plan, keepUsing = emptySet(), userApps) else _state.value = TvState.Asking(plan, userApps)
    }

    /** Switches off the plan's list plus every asked-about app the user didn't tick, after a backup. */
    fun apply(plan: DebloatPlan, keepUsing: Set<String>, userApps: Set<String>) {
        val packages = plan.disable.map { it.pkg } + plan.ask.map { it.pkg }.filter { it !in keepUsing }
        onTv(TvState.Applying(0, packages.size)) { shell ->
            Backup.save(shell, backups, System.currentTimeMillis())
            val freeBefore = Storage.freeKb(shell)
            val debloater = Debloater(shell, log, Uninstaller(shell, selfPackage))
            val outcomes = debloater.disable(packages, userApps) { done, total -> _state.value = TvState.Applying(done, total) }
            TvState.Done(
                switchedOff = outcomes.count { it is Outcome.Disabled },
                uninstalled = outcomes.count { it is Outcome.Uninstalled },
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

    fun openInstalled() = onTv(TvState.Working) { shell -> appsScreen(shell, result = null) }

    fun askUninstall(app: InstalledApp) {
        _state.value = TvState.ConfirmUninstall(app)
    }

    /** Back from the confirmation: the list as it was, without asking the TV again. */
    fun cancelUninstall() {
        _state.value = lastInstalled ?: TvState.Connecting.also { inspect() }
    }

    fun uninstall(app: InstalledApp) = onTv(TvState.Working) { shell ->
        val removed = Uninstaller(shell, selfPackage).uninstall(app.named.pkg)
        if (removed) freedByUninstall += app.bytes
        val label = app.named.label
        appsScreen(shell, if (removed) AppsResult.Uninstalled(label, app.bytes, freedByUninstall) else AppsResult.UninstallFailed(label))
    }

    /** Only for packages Respiro switched off: the change log is the whole list. */
    fun turnOn(app: InstalledApp) = onTv(TvState.Working) { shell ->
        val label = app.named.label
        val on = app.named.pkg in log.all() && Debloater(shell, log).enable(app.named.pkg)
        appsScreen(shell, if (on) AppsResult.TurnedOn(label) else AppsResult.TurnOnFailed(label))
    }

    private fun appsScreen(shell: io.github.elente.respiro.adb.Shell, result: AppsResult?): TvState.Installed {
        val sizes = Storage.appSizes(shell)
        val describe = { pkg: String -> InstalledApp(names.describe(pkg), names.icon(pkg, ICON_PX), sizes[pkg] ?: 0) }
        return TvState.Installed(
            apps = Uninstaller(shell, selfPackage).removable().map(describe).sortedByDescending { it.bytes },
            switchedOff = log.all().map(describe),
            result = result,
        )
    }

    fun freeSpace() = onTv(TvState.Working) { shell ->
        val before = Storage.freeKb(shell)
        Storage.clearCaches(shell)
        TvState.SpaceFreed(freedMb(before, Storage.freeKb(shell)))
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
            val next = runCatching { SelfAdb.session(getApplication(), work) }
                .getOrElse { TvState.Failed(ConnectionProblem.of(it), it.message ?: it.javaClass.simpleName) }
            if (next is TvState.Installed) lastInstalled = next
            _state.value = next
        }
    }

    private companion object {
        const val KB_PER_MB = 1024
        const val ICON_PX = 96
    }
}
