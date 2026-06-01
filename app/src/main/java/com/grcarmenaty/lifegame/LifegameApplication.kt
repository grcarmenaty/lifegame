package com.grcarmenaty.lifegame

import android.app.Application
import com.grcarmenaty.lifegame.data.LifegameDatabase
import com.grcarmenaty.lifegame.domain.PantheonRepository
import com.grcarmenaty.lifegame.domain.UserPrefs
import com.grcarmenaty.lifegame.domain.attention.AttentionDecay
import com.grcarmenaty.lifegame.domain.attention.AttentionDecayScheduler
import com.grcarmenaty.lifegame.domain.dialogue.DialogueEngine
import com.grcarmenaty.lifegame.domain.dialogue.DialogueStateStore
import com.grcarmenaty.lifegame.domain.dialogue.lines.DialogueCorpus
import com.grcarmenaty.lifegame.domain.notify.NotificationChannels
import com.grcarmenaty.lifegame.domain.notify.NotificationPrefs
import com.grcarmenaty.lifegame.domain.notify.NudgeScheduler

class LifegameApplication : Application() {

    private val database by lazy { LifegameDatabase.get(this) }

    val dialogueEngine: DialogueEngine by lazy { DialogueEngine(DialogueCorpus.all) }

    private val dialogueStateStore: DialogueStateStore by lazy {
        DialogueStateStore(database.dialogueDao())
    }

    private val notificationPrefs: NotificationPrefs by lazy { NotificationPrefs(this) }

    private val userPrefs: UserPrefs by lazy { UserPrefs(this) }

    private val attentionDecay: AttentionDecay by lazy {
        AttentionDecay(database.daemonDao(), database.dialogueDao(), notificationPrefs)
    }

    val repository: PantheonRepository by lazy {
        PantheonRepository(
            database.daemonDao(),
            database.questDao(),
            database.boonDao(),
            database.dialogueDao(),
            dialogueEngine,
            dialogueStateStore,
            database.epicChapterDao(),
            attentionDecay,
            database.personalDateDao(),
            userPrefs,
        )
    }

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureCreated(this)
        NudgeScheduler.schedule(this)
        AttentionDecayScheduler.schedule(this)
    }
}
