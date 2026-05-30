package com.grcarmenaty.lifegame.domain.dialogue

import com.grcarmenaty.lifegame.data.dao.DialogueDao
import com.grcarmenaty.lifegame.data.entities.CooldownPlay
import com.grcarmenaty.lifegame.data.entities.DaemonState
import com.grcarmenaty.lifegame.data.entities.LineSeen

/**
 * One batched read per [DialogueEngine.pickFor] call. Architect round 4
 * was clear: the toast click-handler cannot afford 4 sequential DAO
 * round-trips. We do exactly two reads (line_seen + cooldown_play) and
 * fold them into a single [DialogueState] snapshot.
 */
class DialogueStateStore(private val dao: DialogueDao) {

    suspend fun loadState(daemonId: Long): DialogueState {
        val seen = dao.lineSeenForDaemon(daemonId)
        val cooldowns = dao.cooldownsForDaemon(daemonId)

        val played = HashSet<String>(seen.size)
        val playCounts = HashMap<String, Int>(seen.size)
        val lastPlayedAt = HashMap<String, Long>(seen.size)
        for (row in seen) {
            played += row.lineId
            playCounts[row.lineId] = row.playCount
            lastPlayedAt[row.lineId] = row.lastPlayedAt
        }

        val inline = HashSet<String>()
        val screen = HashSet<String>()
        val both = HashSet<String>()
        for (row in cooldowns) {
            when (row.surface) {
                "INLINE" -> inline += row.cooldownGroup
                "SCREEN" -> screen += row.cooldownGroup
                "BOTH" -> both += row.cooldownGroup
            }
        }
        return DialogueState(played, playCounts, lastPlayedAt, inline, screen, both)
    }

    suspend fun markPlayed(daemonId: Long, line: DialogueLine, surface: Surface) {
        val now = System.currentTimeMillis()
        val prior = dao.lineSeen(daemonId, line.id)
        dao.upsertLineSeen(
            LineSeen(
                daemonId = daemonId,
                lineId = line.id,
                lastPlayedAt = now,
                playCount = (prior?.playCount ?: 0) + 1,
            )
        )
        val group = line.cooldownGroup ?: return
        val surfaceTag = when {
            line.crossSurfaceCooldown -> "BOTH"
            surface == Surface.INLINE -> "INLINE"
            else -> "SCREEN"
        }
        dao.upsertCooldown(
            CooldownPlay(
                daemonId = daemonId,
                cooldownGroup = group,
                surface = surfaceTag,
                expiresAtPicks = line.cooldownPicks ?: 8,
            )
        )
    }

    /** Lazy-create the dialogue state row on first dialogue event. */
    suspend fun ensureDaemonState(daemonId: Long): DaemonState {
        val existing = dao.daemonState(daemonId)
        if (existing != null) return existing
        val fresh = DaemonState(daemonId = daemonId)
        dao.upsertDaemonState(fresh)
        return fresh
    }
}
