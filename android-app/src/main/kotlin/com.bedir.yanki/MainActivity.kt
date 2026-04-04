package com.bedir.yanki

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.bedir.yanki.ui.navigation.MainScreen
import com.bedir.yanki.ui.theme.YankiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YankiTheme {
                // Uygulamanın ana giriş noktası olan MainScreen'i çağırıyoruz.
                // Bu ekran alt menü ve sayfa geçişlerini (NavHost) yönetir.
                MainScreen()
            }
        }
    }
}
