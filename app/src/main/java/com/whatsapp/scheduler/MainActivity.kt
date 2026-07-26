package com.whatsapp.scheduler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.whatsapp.scheduler.ui.screens.CreateEditScheduleScreen
import com.whatsapp.scheduler.ui.screens.MainScreen
import com.whatsapp.scheduler.ui.screens.PermissionGuideScreen
import com.whatsapp.scheduler.ui.theme.WhatsAppSchedulerTheme
import com.whatsapp.scheduler.ui.viewmodel.MainViewModel
import com.whatsapp.scheduler.ui.viewmodel.ScheduleFormViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate()

        val app = application as WhatsAppSchedulerApp
        val repository = app.repository

        val mainViewModel: MainViewModel by viewModels {
            MainViewModel.Factory(repository)
        }

        val formViewModel: ScheduleFormViewModel by viewModels {
            ScheduleFormViewModel.Factory(repository)
        }

        setContent {
            WhatsAppSchedulerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WhatsAppSchedulerAppNav(
                        mainViewModel = mainViewModel,
                        formViewModel = formViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun WhatsAppSchedulerAppNav(
    mainViewModel: MainViewModel,
    formViewModel: ScheduleFormViewModel
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(
                viewModel = mainViewModel,
                onNavigateToCreate = {
                    navController.navigate("create")
                },
                onNavigateToEdit = { scheduleId ->
                    navController.navigate("edit/$scheduleId")
                },
                onNavigateToPermissions = {
                    navController.navigate("permissions")
                }
            )
        }

        composable("create") {
            CreateEditScheduleScreen(
                viewModel = formViewModel,
                scheduleId = 0,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "edit/{scheduleId}",
            arguments = listOf(navArgument("scheduleId") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("scheduleId") ?: 0L
            CreateEditScheduleScreen(
                viewModel = formViewModel,
                scheduleId = id,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("permissions") {
            PermissionGuideScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
