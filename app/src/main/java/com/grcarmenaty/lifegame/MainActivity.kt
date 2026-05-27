package com.grcarmenaty.lifegame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.grcarmenaty.lifegame.ui.nav.LifegameNavGraph
import com.grcarmenaty.lifegame.ui.theme.LifegameTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as LifegameApplication).repository
        setContent {
            LifegameTheme {
                LifegameNavGraph(repository = repo)
            }
        }
    }
}
