package com.vidanstojic.draftduel.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.vidanstojic.draftduel.domain.model.GameMode
import com.vidanstojic.draftduel.ui.screens.category.CategoryRoute
import com.vidanstojic.draftduel.ui.screens.home.HomeRoute
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
    NavHost(
        navController = navController,
        startDestination = Route.Home,
        modifier = modifier,
    ) {
        composable<Route.Home> {
            HomeRoute(onModeSelected = { mode -> navController.navigate(Route.ChooseCategory(mode.name)) })
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

/** Where the category screen leads for each mode. */
private fun destinationAfterSetup(mode: GameMode): Route = when (mode) {
    GameMode.QUICK_MATCH -> Route.FindOpponent
    GameMode.FRIEND -> Route.InviteFriend
    GameMode.SAME_DEVICE -> Route.LocalSetup
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
