package io.github.elente.respiro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.elente.respiro.adb.SelfAdb
import io.github.elente.respiro.data.AssetDevices
import io.github.elente.respiro.data.DebloatPlan
import io.github.elente.respiro.data.Getprop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Where Respiro stands with this TV. */
sealed interface TvState {
    data object Connecting : TvState

    data class Failed(val reason: String) : TvState

    /** [plan]'s model is null when the TV isn't in the database. */
    data class Ready(val manufacturer: String, val model: String, val plan: DebloatPlan) : TvState
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow<TvState>(TvState.Connecting)
    val state: StateFlow<TvState> = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) { _state.value = inspect() }
    }

    private fun inspect(): TvState = runCatching {
        val app = getApplication<Application>()
        val props = Getprop.parse(SelfAdb.shell(app, "getprop"))
        val database = AssetDevices.load(app.assets)
        TvState.Ready(
            manufacturer = props[MANUFACTURER].orEmpty(),
            model = props[MODEL].orEmpty(),
            plan = database.plan(database.match(props)),
        )
    }.getOrElse { TvState.Failed(it.message ?: it.javaClass.simpleName) }

    private companion object {
        const val MANUFACTURER = "ro.product.manufacturer"
        const val MODEL = "ro.product.model"
    }
}
