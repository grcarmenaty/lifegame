package com.grcarmenaty.lifegame.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.grcarmenaty.lifegame.data.entities.Daemon
import com.grcarmenaty.lifegame.data.entities.MajorQuest
import com.grcarmenaty.lifegame.domain.PantheonRepository
import com.grcarmenaty.lifegame.domain.VoicePreset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailState(
    val daemon: Daemon? = null,
    val level: Int = 1,
    val levelProgress: Float = 0f,
    val leadingMajor: LeadingMajor? = null,
    val majors: List<MajorQuest> = emptyList(),
)

data class LeadingMajor(val title: String, val progress: Int, val threshold: Int)

class DaemonDetailViewModel(
    private val repository: PantheonRepository,
    val daemonId: Long,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<DetailState> =
        combine(
            repository.observeDaemon(daemonId),
            repository.observeMajors(daemonId),
        ) { daemon, majors ->
            val completedCount = majors.count { it.completed }
            val open = majors.filter { !it.completed }
            val leading = open.maxByOrNull {
                it.progressCount.toFloat() / it.thresholdCount.coerceAtLeast(1)
            }
            DetailState(
                daemon = daemon,
                level = 1 + completedCount,
                levelProgress = leading?.let {
                    (it.progressCount.toFloat() / it.thresholdCount.coerceAtLeast(1))
                        .coerceIn(0f, 1f)
                } ?: 0f,
                leadingMajor = leading?.let {
                    LeadingMajor(it.title, it.progressCount, it.thresholdCount)
                },
                majors = majors,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState())

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    fun save(
        name: String,
        archetype: String,
        voicePreset: VoicePreset,
        boon: String,
    ) {
        viewModelScope.launch {
            repository.updateDaemon(daemonId, name.trim(), archetype.trim(), voicePreset, boon.trim())
            _saved.value = true
        }
    }

    fun acknowledgeSaved() { _saved.value = false }

    fun vanish(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.vanishDaemon(daemonId)
            onDone()
        }
    }

    companion object {
        fun factory(repository: PantheonRepository, daemonId: Long) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    DaemonDetailViewModel(repository, daemonId) as T
            }
    }
}
