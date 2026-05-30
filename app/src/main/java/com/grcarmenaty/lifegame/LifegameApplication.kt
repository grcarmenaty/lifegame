package com.grcarmenaty.lifegame

import android.app.Application
import com.grcarmenaty.lifegame.data.LifegameDatabase
import com.grcarmenaty.lifegame.domain.PantheonRepository
import com.grcarmenaty.lifegame.domain.dialogue.DialogueEngine
import com.grcarmenaty.lifegame.domain.dialogue.DialogueStateStore
import com.grcarmenaty.lifegame.domain.dialogue.lines.DialogueCorpus

class LifegameApplication : Application() {

    private val database by lazy { LifegameDatabase.get(this) }

    val dialogueEngine: DialogueEngine by lazy { DialogueEngine(DialogueCorpus.all) }

    private val dialogueStateStore: DialogueStateStore by lazy {
        DialogueStateStore(database.dialogueDao())
    }

    val repository: PantheonRepository by lazy {
        PantheonRepository(
            database.daemonDao(),
            database.questDao(),
            database.boonDao(),
            database.dialogueDao(),
            dialogueEngine,
            dialogueStateStore,
        )
    }
}
