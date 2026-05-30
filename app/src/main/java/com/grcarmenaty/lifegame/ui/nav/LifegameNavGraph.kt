package com.grcarmenaty.lifegame.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.grcarmenaty.lifegame.domain.PantheonRepository
import com.grcarmenaty.lifegame.ui.daily.DailyScreen
import com.grcarmenaty.lifegame.ui.detail.DaemonDetailScreen
import com.grcarmenaty.lifegame.ui.settings.SettingsScreen
import com.grcarmenaty.lifegame.ui.summoning.SummoningScreen

private object Routes {
    const val LOADING = "loading"
    const val SUMMONING = "summoning"
    const val DAILY = "daily"
    const val DETAIL = "daemon/{id}"
    const val SETTINGS = "settings"

    fun detail(id: Long) = "daemon/$id"
}

@Composable
fun LifegameNavGraph(
    repository: PantheonRepository,
    pendingDaemonId: MutableState<Long?> = remember { mutableStateOf(null) },
) {
    val nav = rememberNavController()
    // When the app is launched via a nudge notification tap, navigate to
    // the daemon detail screen. Single-use: clear the value after dispatch.
    LaunchedEffect(pendingDaemonId.value) {
        val id = pendingDaemonId.value ?: return@LaunchedEffect
        nav.navigate(Routes.detail(id)) {
            launchSingleTop = true
        }
        pendingDaemonId.value = null
    }
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
                    // Works for both entry points: from Loading (no Daily on
                    // the stack yet) we push Daily; from Daily (+) we pop
                    // Summoning and reuse the existing Daily underneath.
                    nav.navigate(Routes.DAILY) {
                        popUpTo(Routes.SUMMONING) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onCancel = {
                    // If there's nothing behind summoning (first-run cancel),
                    // popBackStack returns false and the app exits — acceptable.
                    if (!nav.popBackStack()) {
                        // First-run cancel — nothing to fall back to; nav stays put.
                    }
                },
            )
        }
        composable(Routes.DAILY) {
            DailyScreen(
                repository = repository,
                onAddDaemon = { nav.navigate(Routes.SUMMONING) },
                onOpenDetail = { id -> nav.navigate(Routes.detail(id)) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            DaemonDetailScreen(
                repository = repository,
                daemonId = id,
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                repository = repository,
                onBack = { nav.popBackStack() },
            )
        }
    }
}
