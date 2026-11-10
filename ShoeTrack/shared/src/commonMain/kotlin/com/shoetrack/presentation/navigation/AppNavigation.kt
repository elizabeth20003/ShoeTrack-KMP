package com.shoetrack.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.shoetrack.domain.model.Money
import com.shoetrack.domain.notebook.NotebookEntry
import com.shoetrack.presentation.create.NewLayawayScreen
import com.shoetrack.presentation.create.NewLayawayViewModel
import com.shoetrack.presentation.detail.LayawayDetailScreen
import com.shoetrack.presentation.detail.LayawayDetailViewModel
import com.shoetrack.presentation.list.LayawayListScreen
import com.shoetrack.presentation.list.LayawayListViewModel
import com.shoetrack.presentation.scan.ScanScreen
import com.shoetrack.presentation.scan.ScanViewModel
import kotlinx.datetime.LocalDate
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

@Composable
fun AppNavigation(layawayToOpen: String? = null, onLayawayOpened: () -> Unit = {}) {
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
            val viewModel = viewModel { koin.get<LayawayListViewModel>() }
            LayawayListScreen(
                viewModel = viewModel,
                onNewLayaway = { navController.navigate(NewLayawayRoute()) },
                onScanNotebook = { navController.navigate(ScanRoute) },
                onOpenLayaway = { id -> navController.navigate(LayawayDetailRoute(id)) },
            )
        }
        composable<NewLayawayRoute> { entry ->
            val prefill = entry.toRoute<NewLayawayRoute>().toNotebookEntry()
            val viewModel = viewModel { koin.get<NewLayawayViewModel> { parametersOf(prefill) } }
            NewLayawayScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable<ScanRoute> {
            val viewModel = viewModel { koin.get<ScanViewModel>() }
            ScanScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onEntryRead = { read -> navController.navigate(read.toRoute()) },
            )
        }
        composable<LayawayDetailRoute> { entry ->
            val layawayId = entry.toRoute<LayawayDetailRoute>().id
            val viewModel = viewModel { koin.get<LayawayDetailViewModel> { parametersOf(layawayId) } }
            LayawayDetailScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}

private fun NotebookEntry.toRoute() = NewLayawayRoute(
    fromNotebook = true,
    customerName = customerName,
    productDescription = productDescription,
    priceUnits = totalPrice?.let { it.cents / 100 },
    paidUnits = paidAmount?.let { it.cents / 100 },
    lastPaymentEpochDay = lastPaymentDate?.toEpochDays()?.toLong(),
)

private fun NewLayawayRoute.toNotebookEntry(): NotebookEntry? = if (!fromNotebook) {
    null
} else {
    NotebookEntry(
        customerName = customerName,
        productDescription = productDescription,
        totalPrice = priceUnits?.let(Money::fromUnits),
        paidAmount = paidUnits?.let(Money::fromUnits),
        lastPaymentDate = lastPaymentEpochDay?.let { LocalDate.fromEpochDays(it) },
    )
}
