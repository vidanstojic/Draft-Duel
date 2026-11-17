package com.vidanstojic.draftduel.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.vidanstojic.draftduel.di.LocalAppContainer
import com.vidanstojic.draftduel.domain.model.GameMode
import com.vidanstojic.draftduel.feedback.MusicTrack
import com.vidanstojic.draftduel.ui.screens.bot.BotSelectRoute
import com.vidanstojic.draftduel.ui.screens.category.CategoryRoute
import com.vidanstojic.draftduel.ui.screens.home.HomeRoute
import com.vidanstojic.draftduel.ui.screens.howto.HowToPlayRoute
import com.vidanstojic.draftduel.ui.screens.settings.SettingsRoute
import com.vidanstojic.draftduel.ui.screens.invite.InviteRoute
import com.vidanstojic.draftduel.ui.screens.local.LocalMatchRoute
import com.vidanstojic.draftduel.ui.screens.local.LocalSetupRoute
import com.vidanstojic.draftduel.ui.screens.match.MatchRoute
import com.vidanstojic.draftduel.ui.screens.result.ResultRoute
import com.vidanstojic.draftduel.ui.screens.matchmaking.FindOpponentRoute

@Composable
fun DraftDuelNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    MatchMusic(navController)
    NavHost(
        navController = navController,
        startDestination = Route.Home,
        modifier = modifier,
    ) {
        composable<Route.Home> {
            HomeRoute(
                onModeSelected = { mode -> navController.navigate(Route.ChooseCategory(mode.name)) },
                onHowToPlay = { navController.navigate(Route.HowToPlay) { launchSingleTop = true } },
                onSettings = { navController.navigate(Route.Settings) { launchSingleTop = true } },
            )
        }
        composable<Route.Settings> {
            SettingsRoute(
                onBack = { navController.popBackStack() },
                onHowToPlay = { navController.navigate(Route.HowToPlay) { launchSingleTop = true } },
            )
        }
        composable<Route.HowToPlay> {
            HowToPlayRoute(onClose = { navController.popBackStack() })
        }
        composable<Route.ChooseCategory> { entry ->
            val route = entry.toRoute<Route.ChooseCategory>()
            CategoryRoute(
                mode = GameMode.valueOf(route.mode),
                onBack = { navController.popBackStack() },
                onContinue = { mode -> navController.navigate(destinationAfterSetup(mode)) },
            )
        }
        composable<Route.FindOpponent> {
            FindOpponentRoute(
                onCancel = { navController.popBackStack() },
                onMatchReady = { navController.startMatch() },
            )
        }
        composable<Route.InviteFriend> {
            InviteRoute(
                onBack = { navController.popBackStack() },
                onMatchReady = { navController.startMatch() },
            )
        }
        composable<Route.ChooseBot> {
            BotSelectRoute(
                onBack = { navController.popBackStack() },
                onStart = { navController.startMatch() },
            )
        }
        composable<Route.Match> {
            MatchRoute(
                onFinished = { navController.showResult() },
                onLeave = { navController.backToHome() },
            )
        }
        composable<Route.Result> {
            ResultRoute(
                onClose = { navController.backToHome() },
                onRematch = { mode ->
                    if (mode == GameMode.SAME_DEVICE) navController.startLocalMatch() else navController.startMatch()
                },
            )
        }
        composable<Route.LocalSetup> {
            LocalSetupRoute(
                onBack = { navController.popBackStack() },
                onStart = { navController.startLocalMatch() },
            )
        }
        composable<Route.LocalMatch> {
            LocalMatchRoute(
                onFinished = { navController.showResult() },
                onLeave = { navController.backToHome() },
            )
        }
    }
}

/** The match music during a match, the menu loop everywhere else. */
@Composable
private fun MatchMusic(navController: NavHostController) {
    val container = LocalAppContainer.current
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    val inMatch = destination != null && (destination.hasRoute<Route.Match>() || destination.hasRoute<Route.LocalMatch>())
    val track = if (inMatch) MusicTrack.MATCH else MusicTrack.MENU
    LaunchedEffect(track) { container.feedback.music(track) }
}

/** Where the category screen leads for each mode. */
private fun destinationAfterSetup(mode: GameMode): Route = when (mode) {
    GameMode.QUICK_MATCH -> Route.FindOpponent
    GameMode.FRIEND -> Route.InviteFriend
    GameMode.SAME_DEVICE -> Route.LocalSetup
    GameMode.VS_BOT -> Route.ChooseBot
}

/** Opens the match and drops the waiting screen, so Back from the match does not return to it. */
private fun NavHostController.startMatch() {
    navigate(Route.Match) {
        popUpTo<Route.Home>()
    }
}

private fun NavHostController.startLocalMatch() {
    navigate(Route.LocalMatch) {
        popUpTo<Route.Home>()
    }
}

private fun NavHostController.showResult() {
    navigate(Route.Result) {
        popUpTo<Route.Home>()
    }
}

private fun NavHostController.backToHome() {
    popBackStack<Route.Home>(inclusive = false)
}
