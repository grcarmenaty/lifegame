package com.grcarmenaty.lifegame.domain

import androidx.annotation.DrawableRes

/**
 * Hand-written companion to the generated [DaemonFaceTable]. Provides:
 *
 *  - a stable two-way map between a face's drawable *entry name*
 *    (e.g. `"face_oracle_exercise_2"`) and its resId, so a user's
 *    chosen face can be persisted as a build-stable string rather than
 *    an R value (which changes between builds and would corrupt
 *    backups), and
 *  - [orderedFor], the picker ordering the summoning / detail UIs use:
 *    the three variants for the daemon's own (archetype, theme) first,
 *    then the rest of that archetype's faces (its other themes), then
 *    every remaining face. This keeps the most relevant faces at the
 *    front of the chooser while still exposing the full set.
 *
 * Keys in [DaemonFaceTable] are `"<archetype>_<theme>"`. Archetype
 * tokens may contain underscores (`drill_sergeant`) but theme tokens
 * never do, so the archetype is everything before the final `_`.
 */
object DaemonFaceCatalog {

    private val nameToRes: Map<String, Int> = buildMap {
        DaemonFaceTable.byKey.forEach { (key, variants) ->
            variants.forEachIndexed { i, res -> put("face_${key}_${i + 1}", res) }
        }
    }

    private val resToName: Map<Int, String> =
        nameToRes.entries.associate { (name, res) -> res to name }

    /** Resolve a stored face name to its drawable, or null if unknown. */
    @DrawableRes
    fun resForName(name: String?): Int? = name?.let { nameToRes[it] }

    /** Reverse lookup: the stable name for a drawable, or null. */
    fun nameForRes(@DrawableRes res: Int): String? = resToName[res]

    /**
     * Faces offered by the chooser, in priority order:
     *  1. the three variants for `(preset, theme)` — or the archetype's
     *     `_other` baseline when the theme has no dedicated art,
     *  2. the archetype's faces for every other theme,
     *  3. everyone else's faces.
     * De-duplicated, order preserved.
     */
    @DrawableRes
    fun orderedFor(preset: VoicePreset, theme: LifeTheme?): List<Int> {
        val presetToken = preset.name.lowercase()
        val themeToken = theme?.key?.lowercase() ?: "other"
        val primaryKey = if (DaemonFaceTable.byKey.containsKey("${presetToken}_$themeToken"))
            "${presetToken}_$themeToken"
        else
            "${presetToken}_other"

        val tier1 = DaemonFaceTable.byKey.getValue(primaryKey).toList()
        val tier2 = DaemonFaceTable.byKey.entries
            .filter { it.key != primaryKey && it.key.substringBeforeLast('_') == presetToken }
            .flatMap { it.value.toList() }
        val tier3 = DaemonFaceTable.byKey.entries
            .filter { it.key.substringBeforeLast('_') != presetToken }
            .flatMap { it.value.toList() }

        val seen = LinkedHashSet<Int>()
        seen += tier1
        seen += tier2
        seen += tier3
        return seen.toList()
    }
}
