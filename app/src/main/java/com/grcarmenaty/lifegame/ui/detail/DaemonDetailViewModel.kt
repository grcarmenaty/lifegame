package com.grcarmenaty.lifegame.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.grcarmenaty.lifegame.data.entities.Boon
import com.grcarmenaty.lifegame.data.entities.Daemon
import com.grcarmenaty.lifegame.data.entities.DaemonState
import com.grcarmenaty.lifegame.data.entities.EpicChapter
import com.grcarmenaty.lifegame.data.entities.MajorQuest
import com.grcarmenaty.lifegame.data.entities.MinorQuest
import com.grcarmenaty.lifegame.domain.ApotheosisEvent
import com.grcarmenaty.lifegame.domain.PantheonRepository
import com.grcarmenaty.lifegame.domain.VoicePreset
import com.grcarmenaty.lifegame.domain.attention.AttentionConfig
import com.grcarmenaty.lifegame.domain.attention.AttentionMath
import com.grcarmenaty.lifegame.domain.attention.ResolvedAttentionConfig
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
    val attentionPoints: Int = 0,
    val levelProgress: Float = 0f,
    val resolvedConfig: ResolvedAttentionConfig? = null,
    val daemonState: DaemonState? = null,
    val majors: List<MajorQuest> = emptyList(),
    val boons: List<Boon> = emptyList(),
    val minorsByMajor: Map<Long, List<MinorQuest>> = emptyMap(),
    val epicChapters: List<EpicChapter> = emptyList(),
) {
    fun boonTextFor(id: Long?): String? =
        id?.let { boons.firstOrNull { b -> b.id == it }?.text }

    fun minorsFor(majorId: Long): List<MinorQuest> =
        minorsByMajor[majorId] ?: emptyList()
}

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
        val daemonStateFlow = repository.observeDaemonState(daemonId)
        val epicChaptersFlow = repository.observeEpicChapters(daemonId)
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

        // 6 input flows — combine in two phases to stay within
        // kotlinx.coroutines's 5-arg combine overload.
        combine(daemonFlow, majorsFlow, boonsFlow, minorsMapFlow, daemonStateFlow) {
                daemon, majors, boons, minorsMap, ds ->
            Phase1(daemon, majors, boons, minorsMap, ds)
        }.combine(epicChaptersFlow) { phase1, chapters ->
            val daemon = phase1.daemon
            val ds = phase1.ds
            val resolved = if (daemon != null && ds != null) {
                AttentionConfig.resolve(ds, VoicePreset.fromKey(daemon.voicePreset))
            } else null
            val attention = ds?.attentionPoints ?: 0
            DetailState(
                daemon = daemon,
                level = AttentionMath.levelFor(attention),
                attentionPoints = attention,
                levelProgress = AttentionMath.levelProgress(attention),
                resolvedConfig = resolved,
                daemonState = ds,
                majors = phase1.majors,
                boons = phase1.boons,
                minorsByMajor = phase1.minorsMap,
                epicChapters = chapters,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState())
    }

    private data class Phase1(
        val daemon: Daemon?,
        val majors: List<MajorQuest>,
        val boons: List<Boon>,
        val minorsMap: Map<Long, List<MinorQuest>>,
        val ds: DaemonState?,
    )

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    private val _deletePreview = MutableStateFlow<DeleteMajorPreview?>(null)
    val deletePreview: StateFlow<DeleteMajorPreview?> = _deletePreview

    private val _apotheosis = MutableStateFlow<ApotheosisEvent?>(null)
    val apotheosis: StateFlow<ApotheosisEvent?> = _apotheosis

    fun save(name: String, archetype: String, voicePreset: VoicePreset, face: String?) {
        viewModelScope.launch {
            repository.updateDaemon(daemonId, name.trim(), archetype.trim(), voicePreset, face)
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

    fun addMajorsFromLibrary(specs: List<PantheonRepository.NewMajorSpec>) {
        viewModelScope.launch { specs.forEach { repository.addMajorFromCatalog(daemonId, it) } }
    }

    fun editMajor(majorId: Long, title: String, fragmentOverride: String?) {
        viewModelScope.launch { repository.editMajor(majorId, title, fragmentOverride) }
    }

    fun editMinor(
        minorId: Long,
        title: String,
        cadence: String,
        cadenceCount: Int,
        cadenceDays: Set<Int>,
        weight: Int,
        fragmentOverride: String?,
    ) {
        viewModelScope.launch {
            repository.editMinor(minorId, title, cadence, cadenceCount, cadenceDays, weight, fragmentOverride)
        }
    }

    fun addMajor(title: String) {
        viewModelScope.launch { repository.addMajor(daemonId, title.trim()) }
    }

    fun addMinor(
        majorId: Long,
        title: String,
        cadence: String,
        cadenceCount: Int,
        cadenceDays: Set<Int>,
        weight: Int,
    ) {
        viewModelScope.launch {
            repository.addMinor(
                majorId = majorId,
                title = title.trim(),
                cadence = cadence,
                weight = weight,
                cadenceCount = cadenceCount,
                cadenceDays = cadenceDays,
            )
        }
    }

    fun deleteMinor(minorId: Long) {
        viewModelScope.launch { repository.deleteMinor(minorId) }
    }

    fun completeMajor(majorId: Long) {
        viewModelScope.launch {
            val event = repository.completeMajor(majorId)
            if (event != null) _apotheosis.value = event
        }
    }

    fun dismissApotheosis() { _apotheosis.value = null }

    fun reopenMajor(majorId: Long) {
        viewModelScope.launch { repository.reopenMajor(majorId) }
    }

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

    // ---- v0.0.10 attention/decay/boon-accrual overrides ----

    fun setDecayOverride(perDay: Int?, graceDays: Int?) {
        viewModelScope.launch { repository.setDecayOverride(daemonId, perDay, graceDays) }
    }

    fun setDecayDisabled(disabled: Boolean) {
        viewModelScope.launch { repository.setDecayDisabled(daemonId, disabled) }
    }

    fun setMinorsPerBoonOverride(value: Int?) {
        viewModelScope.launch { repository.setMinorsPerBoonOverride(daemonId, value) }
    }

    // ---- Epic chapters ----

    fun addEpicChapter(text: String) {
        viewModelScope.launch { repository.addEpicChapter(daemonId, text) }
    }

    fun deleteEpicChapter(chapterId: Long) {
        viewModelScope.launch { repository.deleteEpicChapter(chapterId) }
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
