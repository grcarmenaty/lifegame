package com.grcarmenaty.lifegame.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.grcarmenaty.lifegame.data.entities.Boon
import com.grcarmenaty.lifegame.data.entities.Daemon
import com.grcarmenaty.lifegame.data.entities.MajorQuest
import com.grcarmenaty.lifegame.data.entities.MinorQuest
import com.grcarmenaty.lifegame.domain.ApotheosisEvent
import com.grcarmenaty.lifegame.domain.PantheonRepository
import com.grcarmenaty.lifegame.domain.VoicePreset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailState(
    val daemon: Daemon? = null,
    val level: Int = 1,
    val levelProgress: Float = 0f,
    val leadingMajor: LeadingMajor? = null,
    val majors: List<MajorQuest> = emptyList(),
    val boons: List<Boon> = emptyList(),
    // Hoisted out of MajorCard so the screen never opens per-item Flow
    // subscriptions during scroll. Empty list for a major with no
    // minors. Missing key = "loading" (treat as empty).
    val minorsByMajor: Map<Long, List<MinorQuest>> = emptyMap(),
) {
    fun boonTextFor(id: Long?): String? =
        id?.let { boons.firstOrNull { b -> b.id == it }?.text }

    fun minorsFor(majorId: Long): List<MinorQuest> =
        minorsByMajor[majorId] ?: emptyList()
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
    val state: StateFlow<DetailState> = run {
        val daemonFlow = repository.observeDaemon(daemonId)
        val majorsFlow = repository.observeMajors(daemonId)
        val boonsFlow = repository.observeBoons(daemonId)
        // Build a Map<majorId, minors> by flatMapping over the current
        // majors list — every major's minors flow is consumed exactly
        // once, at the VM level, so the UI never opens a per-item
        // subscription during scroll.
        val minorsMapFlow: Flow<Map<Long, List<MinorQuest>>> =
            majorsFlow.flatMapLatest { majors ->
                if (majors.isEmpty()) flowOf(emptyMap())
                else {
                    val perMajor: List<Flow<Pair<Long, List<MinorQuest>>>> =
                        majors.map { m ->
                            repository.observeMinors(m.id).map { m.id to it }
                        }
                    combine(perMajor) { pairs -> pairs.toMap() }
                }
            }

        combine(daemonFlow, majorsFlow, boonsFlow, minorsMapFlow) { daemon, majors, boons, minorsMap ->
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
                minorsByMajor = minorsMap,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState())
    }

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    private val _deletePreview = MutableStateFlow<DeleteMajorPreview?>(null)
    val deletePreview: StateFlow<DeleteMajorPreview?> = _deletePreview

    private val _apotheosis = MutableStateFlow<ApotheosisEvent?>(null)
    val apotheosis: StateFlow<ApotheosisEvent?> = _apotheosis

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

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setNotificationsEnabled(daemonId, enabled) }
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
     * User-driven close of a major quest. The only path that fires
     * apotheosis — minor completions only track progress now.
     */
    fun completeMajor(majorId: Long) {
        viewModelScope.launch {
            val event = repository.completeMajor(majorId)
            if (event != null) _apotheosis.value = event
        }
    }

    fun dismissApotheosis() { _apotheosis.value = null }

    /** Reopen a previously-closed major. No wish refund. */
    fun reopenMajor(majorId: Long) {
        viewModelScope.launch { repository.reopenMajor(majorId) }
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
