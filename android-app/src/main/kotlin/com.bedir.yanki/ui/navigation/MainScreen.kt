package com.bedir.yanki.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bedir.yanki.ui.emergency.EmergencySOSScreen
import com.bedir.yanki.ui.main.MainDashboard
import com.bedir.yanki.ui.messages.ChatDetailScreen
import com.bedir.yanki.ui.messages.MessagesListScreen
import com.bedir.yanki.ui.profile.EditHealthScreen
import com.bedir.yanki.ui.profile.HealthDetailScreen
import com.bedir.yanki.ui.profile.ProfileScreen
import com.bedir.yanki.ui.profile.SettingsScreen
import com.bedir.yanki.ui.registration.*
import com.bedir.yanki.ui.viewmodel.MeshViewModel
import com.bedir.yanki.ui.viewmodel.registration.RegistrationViewModel

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val meshViewModel: MeshViewModel = hiltViewModel()
    
    // Kayıt durumunu anlık takip ediyoruz (Flow -> State)
    val isUserRegistered by meshViewModel.isUserRegistered.collectAsState()

    // Navigasyon durumunu takip ederek alt barın görünürlüğünü belirliyoruz
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Alt barın gizli kalması gereken sayfalar (Kayıt akışı ve Giriş)
    val authRoutes = remember {
        listOf(
            Screen.Welcome.route,
            Screen.RegisterStep1.route,
            Screen.RegisterStep2.route,
            Screen.RegisterStep3.route,
            Screen.RegisterSuccess.route,
            Screen.Login.route
        )
    }

    // Kullanıcı kayıtlıysa ve bir auth sayfasında değilse alt barı göster
    // Bu sayede Ayarlar veya Sohbet Detay gibi yerlerde de sekmeler arası geçiş yapılabilir.
    val shouldShowBottomBar = isUserRegistered && currentRoute != null && currentRoute !in authRoutes

    Scaffold(
        bottomBar = { 
            if (shouldShowBottomBar) {
                YankiBottomBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            // Uygulama ilk açıldığında nereden başlayacağını belirler (sabit tutulur)
            startDestination = remember { if (isUserRegistered) Screen.Home.route else Screen.Welcome.route },
            modifier = Modifier.padding(innerPadding)
        ) {
            // --- KAYIT AKIŞI ---
            composable(Screen.Welcome.route) {
                WelcomeScreen(navController = navController)
            }
            
            // Kayıt adımları için ortak bir ViewModel kapsamı oluşturuyoruz
            composable(Screen.RegisterStep1.route) {
                val regViewModel: RegistrationViewModel = hiltViewModel(it)
                RegisterStep1Screen(navController = navController, viewModel = regViewModel)
            }
            composable(Screen.RegisterStep2.route) {
                val entry = remember(it) { navController.getBackStackEntry(Screen.RegisterStep1.route) }
                val regViewModel: RegistrationViewModel = hiltViewModel(entry)
                RegisterStep2Screen(navController = navController, viewModel = regViewModel)
            }
            composable(Screen.RegisterStep3.route) {
                val entry = remember(it) { navController.getBackStackEntry(Screen.RegisterStep1.route) }
                val regViewModel: RegistrationViewModel = hiltViewModel(entry)
                RegisterStep3Screen(navController = navController, viewModel = regViewModel)
            }

            composable(Screen.RegisterSuccess.route) {
                RegisterSuccessScreen(navController = navController)
            }
            composable(Screen.Login.route) {
                LoginScreen(navController = navController)
            }

            // --- ANA UYGULAMA ---
            composable(Screen.Home.route) {
                MainDashboard(navController = navController, viewModel = meshViewModel)
            }

            // 2. AĞ (SOS EKRANI)
            composable(Screen.Network.route) {
                EmergencySOSScreen(navController = navController, viewModel = meshViewModel)
            }

            // DUYURU PANOSU
            composable(Screen.BulletinBoard.route) {
                com.bedir.yanki.ui.bulletin.BulletinBoardScreen(viewModel = meshViewModel)
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
                ProfileScreen(navController = navController, viewModel = meshViewModel)
            }

            // 5. AYARLAR VE DETAYLAR
            composable(Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }
            composable(Screen.HealthDetail.route) {
                HealthDetailScreen(navController = navController, viewModel = meshViewModel)
            }
            composable(Screen.EditHealth.route) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type") ?: ""
                EditHealthScreen(navController = navController, viewModel = meshViewModel, type = type)
            }
        }
    }
}