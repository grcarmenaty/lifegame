package com.grcarmenaty.lifegame.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.grcarmenaty.lifegame.domain.PantheonRepository
import com.grcarmenaty.lifegame.ui.daily.DailyScreen
import com.grcarmenaty.lifegame.ui.summoning.SummoningScreen

private object Routes {
    const val LOADING = "loading"
    const val SUMMONING = "summoning"
    const val DAILY = "daily"
}

@Composable
fun LifegameNavGraph(repository: PantheonRepository) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.LOADING) {
        composable(Routes.LOADING) {
            var decided by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                if (!decided) {
                    val target = if (repository.daemonCount() == 0) Routes.SUMMONING else Routes.DAILY
                    nav.navigate(target) {
                        popUpTo(Routes.LOADING) { inclusive = true }
                    }
                    decided = true
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        }
        composable(Routes.SUMMONING) {
            SummoningScreen(
                repository = repository,
                onSummoned = {
                    nav.navigate(Routes.DAILY) {
                        popUpTo(Routes.SUMMONING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.DAILY) {
            DailyScreen(repository = repository)
        }
    }
}
