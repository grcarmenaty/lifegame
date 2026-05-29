package com.grcarmenaty.lifegame.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.grcarmenaty.lifegame.data.entities.Boon
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
    val boons: List<Boon> = emptyList(),
) {
    fun boonTextFor(id: Long?): String? =
        id?.let { boons.firstOrNull { b -> b.id == it }?.text }
}

data class LeadingMajor(val title: String, val progress: Int, val threshold: Int)

data class DeleteMajorPreview(
    val majorId: Long,
    val title: String,
    val completedMinors: Int,
    val totalMinors: Int,
)

class DaemonDetailViewModel(
    private val repository: PantheonRepository,
    val daemonId: Long,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<DetailState> =
        combine(
            repository.observeDaemon(daemonId),
            repository.observeMajors(daemonId),
            repository.observeBoons(daemonId),
        ) { daemon, majors, boons ->
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
                boons = boons,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState())

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    private val _deletePreview = MutableStateFlow<DeleteMajorPreview?>(null)
    val deletePreview: StateFlow<DeleteMajorPreview?> = _deletePreview

    fun save(name: String, archetype: String, voicePreset: VoicePreset) {
        viewModelScope.launch {
            repository.updateDaemon(daemonId, name.trim(), archetype.trim(), voicePreset)
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

    fun addBoon(text: String, initialCount: Int) {
        viewModelScope.launch {
            repository.addBoon(daemonId, text.trim(), initialCount)
        }
    }

    fun deleteBoon(boonId: Long) {
        viewModelScope.launch { repository.deleteBoon(boonId) }
    }

    fun addMajor(title: String) {
        viewModelScope.launch { repository.addMajor(daemonId, title.trim()) }
    }

    fun addMinor(majorId: Long, title: String, cadence: String, weight: Int) {
        viewModelScope.launch { repository.addMinor(majorId, title.trim(), cadence, weight) }
    }

    fun deleteMinor(minorId: Long) {
        viewModelScope.launch { repository.deleteMinor(minorId) }
    }

    /**
     * Major delete is gated by a preview dialog so the user sees what
     * they're about to destroy. UI calls [requestDeleteMajor] first,
     * gets a populated [deletePreview], then either confirms or cancels.
     */
    fun requestDeleteMajor(majorId: Long, majorTitle: String) {
        viewModelScope.launch {
            val (done, total) = repository.progressLossPreview(majorId)
            _deletePreview.value = DeleteMajorPreview(
                majorId = majorId,
                title = majorTitle,
                completedMinors = done,
                totalMinors = total,
            )
        }
    }

    fun cancelDeleteMajor() { _deletePreview.value = null }

    fun confirmDeleteMajor() {
        val preview = _deletePreview.value ?: return
        _deletePreview.value = null
        viewModelScope.launch { repository.deleteMajor(preview.majorId) }
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
