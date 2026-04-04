package com.bedir.yanki.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = YankiGreen,
    secondary = YankiCardBg,
    background = YankiDarkBg,
    surface = YankiCardBg
)

@Composable
fun YankiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
