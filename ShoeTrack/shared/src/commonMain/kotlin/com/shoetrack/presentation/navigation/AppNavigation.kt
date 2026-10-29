package com.shoetrack.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.shoetrack.presentation.create.NewLayawayScreen
import com.shoetrack.presentation.create.NewLayawayViewModel
import com.shoetrack.presentation.detail.LayawayDetailScreen
import com.shoetrack.presentation.detail.LayawayDetailViewModel
import com.shoetrack.presentation.list.LayawayListScreen
import com.shoetrack.presentation.list.LayawayListViewModel
import org.koin.mp.KoinPlatform

@Composable
fun AppNavigation(
    layawayToOpen: String? = null,
    onLayawayOpened: () -> Unit = {},
) {
    val navController = rememberNavController()
    val koin = KoinPlatform.getKoin()

    LaunchedEffect(layawayToOpen) {
        if (layawayToOpen != null) {
            navController.navigate(LayawayDetailRoute(layawayToOpen))
            onLayawayOpened()
        }
    }

    NavHost(navController = navController, startDestination = LayawayListRoute) {
        composable<LayawayListRoute> {
            val viewModel = viewModel { LayawayListViewModel(koin.get(), koin.get(), koin.get()) }
            LayawayListScreen(
                viewModel = viewModel,
                onNewLayaway = { navController.navigate(NewLayawayRoute) },
                onOpenLayaway = { id -> navController.navigate(LayawayDetailRoute(id)) },
            )
        }
        composable<NewLayawayRoute> {
            val viewModel = viewModel { NewLayawayViewModel(koin.get(), koin.get(), koin.get()) }
            NewLayawayScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable<LayawayDetailRoute> { entry ->
            val layawayId = entry.toRoute<LayawayDetailRoute>().id
            val viewModel = viewModel { LayawayDetailViewModel(layawayId, koin.get(), koin.get(), koin.get(), koin.get()) }
            LayawayDetailScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}
