package com.bedir.yanki.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bedir.yanki.ui.components.YankiBottomBar
import com.bedir.yanki.ui.emergency.EmergencySOSScreen
import com.bedir.yanki.ui.main.MainDashboard
import com.bedir.yanki.ui.messages.ChatDetailScreen
import com.bedir.yanki.ui.messages.MessagesListScreen
import com.bedir.yanki.ui.viewmodel.MeshViewModel

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val meshViewModel: MeshViewModel = hiltViewModel()

    Scaffold(
        bottomBar = { YankiBottomBar(navController = navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. ANA SAYFA (RADAR)
            composable(Screen.Home.route) {
                MainDashboard(navController = navController, viewModel = meshViewModel)
            }

            // 2. AĞ (SOS EKRANI)
            composable(Screen.Network.route) {
                EmergencySOSScreen(navController = navController, viewModel = meshViewModel)
            }

            // 3. MESAJLAR
            composable(Screen.Messages.route) {
                MessagesListScreen(viewModel = meshViewModel, navController = navController)
            }
            composable(
                route = Screen.ChatDetail.route
            ) { backStackEntry ->
                // URL'den userId ve userName parametrelerini ayıklıyoruz
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                val userName = backStackEntry.arguments?.getString("userName") ?: "Bilinmeyen"
                ChatDetailScreen(
                    userId = userId,
                    userName = userName,
                    viewModel = meshViewModel,
                    navController = navController
                )
            }

            // 4. PROFİL
            composable(Screen.Profile.route) {
                // TODO: ProfileScreen()
            }
        }
    }
}