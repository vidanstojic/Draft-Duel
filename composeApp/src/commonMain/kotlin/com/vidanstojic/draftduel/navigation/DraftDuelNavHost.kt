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
                onContinue = { },
            )
        }
    }
}
