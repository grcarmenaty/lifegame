package com.grcarmenaty.lifegame

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.grcarmenaty.lifegame.domain.notify.NudgeWorker
import com.grcarmenaty.lifegame.ui.nav.LifegameNavGraph
import com.grcarmenaty.lifegame.ui.theme.LifegameTheme

class MainActivity : ComponentActivity() {

    /**
     * Holds a daemonId from an incoming notification tap. The nav graph
     * observes this and pops to the daemon detail screen when set.
     * Single-use: cleared by the consumer.
     */
    private val pendingDaemonId = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as LifegameApplication).repository
        consumeDaemonExtra(intent)
        setContent {
            LifegameTheme {
                LifegameNavGraph(
                    repository = repo,
                    pendingDaemonId = pendingDaemonId,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeDaemonExtra(intent)
    }

    private fun consumeDaemonExtra(intent: Intent?) {
        val id = intent?.getLongExtra(NudgeWorker.EXTRA_DAEMON_ID, -1L) ?: -1L
        if (id > 0L) pendingDaemonId.value = id
    }
}
