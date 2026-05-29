package com.grcarmenaty.lifegame.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.grcarmenaty.lifegame.domain.ImportResult
import com.grcarmenaty.lifegame.domain.PantheonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Orchestrates export / import / reset. The actual file I/O lives in the
 * UI layer because it needs `ContentResolver` (Context) — the VM stays
 * Context-free by taking suspend lambdas for reading and writing.
 */
class SettingsViewModel(
    private val repository: PantheonRepository,
    private val appVersion: String,
) : ViewModel() {

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    fun export(write: suspend (String) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val json = repository.exportToJson(appVersion)
                write(json)
                _status.value = "Pantheon exported."
            } catch (e: Exception) {
                _status.value = "Export failed: ${e.localizedMessage ?: e.javaClass.simpleName}"
            } finally {
                _busy.value = false
            }
        }
    }

    fun import(read: suspend () -> String) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val json = read()
                _status.value = when (val result = repository.importFromJson(json)) {
                    is ImportResult.Success ->
                        if (result.daemonCount == 1) "Imported 1 daemon."
                        else "Imported ${result.daemonCount} daemons."
                    is ImportResult.Error -> result.message
                }
            } catch (e: Exception) {
                _status.value = "Import failed: ${e.localizedMessage ?: e.javaClass.simpleName}"
            } finally {
                _busy.value = false
            }
        }
    }

    fun reset() {
        viewModelScope.launch {
            _busy.value = true
            try {
                repository.reset()
                _status.value = "Pantheon reset."
            } catch (e: Exception) {
                _status.value = "Reset failed: ${e.localizedMessage ?: e.javaClass.simpleName}"
            } finally {
                _busy.value = false
            }
        }
    }

    fun acknowledgeStatus() { _status.value = null }

    companion object {
        fun factory(repository: PantheonRepository, appVersion: String) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(repository, appVersion) as T
            }
    }
}
