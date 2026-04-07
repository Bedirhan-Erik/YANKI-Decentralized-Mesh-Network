package com.bedir.yanki

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.bedir.yanki.ui.navigation.MainScreen
import com.bedir.yanki.ui.permissions.PermissionHandler
import com.bedir.yanki.ui.theme.YankiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YankiTheme {
                PermissionHandler { MainScreen()}
            }
        }
    }
}
