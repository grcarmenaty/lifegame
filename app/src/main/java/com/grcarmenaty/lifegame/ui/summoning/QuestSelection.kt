package com.grcarmenaty.lifegame.ui.summoning

import androidx.compose.runtime.saveable.Saver
import com.grcarmenaty.lifegame.data.entities.MinorQuest
import com.grcarmenaty.lifegame.domain.PantheonRepository
import com.grcarmenaty.lifegame.domain.catalog.CatalogMajor
import com.grcarmenaty.lifegame.domain.catalog.QuestCatalog
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The user's in-progress quest selection during summoning. Modelled as
 * immutable data + pure transforms so it can be held in a single
 * `rememberSaveable` (serialized via [SummonSelectionSaver]) and survive
 * rotation / process death like the rest of the ritual.
 */
@Serializable
data class SelMinor(
    val title: String,
    val cadence: String,
    val cadenceCount: Int = 1,
    val cadenceDaysCsv: String = "",
    val weight: Int = 1,
)

@Serializable
data class SelMajor(
    /** Stable selection key: the catalog templateId, or "custom:…". */
    val key: String,
    val templateId: String? = null,
    val title: String,
    /** Selected catalog-minor templateIds under this major. */
    val minorTemplateIds: List<String> = emptyList(),
    val customMinors: List<SelMinor> = emptyList(),
) {
    val minorCount: Int get() = minorTemplateIds.size + customMinors.size
}

@Serializable
data class SummonSelection(
    val majors: List<SelMajor> = emptyList(),
) {
    fun has(key: String): Boolean = majors.any { it.key == key }
}

private val selectionJson = Json { ignoreUnknownKeys = true }

val SummonSelectionSaver: Saver<SummonSelection, String> = Saver(
    save = { selectionJson.encodeToString(SummonSelection.serializer(), it) },
    restore = { selectionJson.decodeFromString(SummonSelection.serializer(), it) },
)

/** Council recommendation: pre-tick the first 3 repeating + first 1 one-off. */
fun defaultSelectedMinors(major: CatalogMajor): List<String> =
    major.repeating.take(3).map { it.templateId } + major.oneOff.take(1).map { it.templateId }

fun SummonSelection.toggleCatalogMajor(major: CatalogMajor): SummonSelection =
    if (has(major.templateId)) copy(majors = majors.filterNot { it.key == major.templateId })
    else copy(
        majors = majors + SelMajor(
            key = major.templateId,
            templateId = major.templateId,
            title = major.title,
            minorTemplateIds = defaultSelectedMinors(major),
        )
    )

fun SummonSelection.addCustomMajor(title: String): SummonSelection {
    val key = "custom:${majors.count { it.templateId == null }}:${title.hashCode()}"
    return copy(majors = majors + SelMajor(key = key, templateId = null, title = title))
}

fun SummonSelection.removeMajor(key: String): SummonSelection =
    copy(majors = majors.filterNot { it.key == key })

fun SummonSelection.toggleMinor(majorKey: String, minorTemplateId: String): SummonSelection =
    copy(majors = majors.map { m ->
        when {
            m.key != majorKey -> m
            minorTemplateId in m.minorTemplateIds ->
                m.copy(minorTemplateIds = m.minorTemplateIds - minorTemplateId)
            else -> m.copy(minorTemplateIds = m.minorTemplateIds + minorTemplateId)
        }
    })

fun SummonSelection.addCustomMinor(majorKey: String, minor: SelMinor): SummonSelection =
    copy(majors = majors.map { m ->
        if (m.key != majorKey) m else m.copy(customMinors = m.customMinors + minor)
    })

fun SummonSelection.removeCustomMinor(majorKey: String, index: Int): SummonSelection =
    copy(majors = majors.map { m ->
        if (m.key != majorKey) m
        else m.copy(customMinors = m.customMinors.filterIndexed { i, _ -> i != index })
    })

/**
 * Materialise the selection into repository specs: catalog minors
 * resolved from their templates, plus any custom acts the user authored.
 * Shared by the summoning ritual and the detail screen's library add.
 */
fun SummonSelection.toMajorSpecs(): List<PantheonRepository.NewMajorSpec> =
    majors.map { sm ->
        val catalogMinors = sm.minorTemplateIds.mapNotNull { tid ->
            QuestCatalog.minor(tid)?.let { cm ->
                PantheonRepository.NewMinorSpec(
                    title = cm.title,
                    cadence = cm.cadence,
                    cadenceCount = cm.cadenceCount,
                    cadenceDays = cm.cadenceDays,
                    weight = cm.weight,
                    templateId = cm.templateId,
                )
            }
        }
        val customMinors = sm.customMinors.map { cmn ->
            PantheonRepository.NewMinorSpec(
                title = cmn.title,
                cadence = cmn.cadence,
                cadenceCount = cmn.cadenceCount,
                cadenceDays = MinorQuest.parseDaysCsv(cmn.cadenceDaysCsv),
                weight = cmn.weight,
            )
        }
        PantheonRepository.NewMajorSpec(
            title = sm.title,
            templateId = sm.templateId,
            minors = catalogMinors + customMinors,
        )
    }
