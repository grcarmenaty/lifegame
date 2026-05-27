package com.grcarmenaty.lifegame

import android.app.Application
import com.grcarmenaty.lifegame.data.LifegameDatabase
import com.grcarmenaty.lifegame.domain.PantheonRepository

class LifegameApplication : Application() {
    val repository: PantheonRepository by lazy {
        val db = LifegameDatabase.get(this)
        PantheonRepository(db.daemonDao(), db.questDao())
    }
}
