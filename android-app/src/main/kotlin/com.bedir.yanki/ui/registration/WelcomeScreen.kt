package com.bedir.yanki.ui.registration

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bedir.yanki.R
import com.bedir.yanki.ui.navigation.Screen
import com.bedir.yanki.ui.radar.RadarView
import com.bedir.yanki.ui.theme.YankiDarkBg
import com.bedir.yanki.ui.theme.YankiGreen

@Composable
fun WelcomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(YankiDarkBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.yanki_logo),
            contentDescription = "YANKI Logo",
            modifier = Modifier
                .size(180.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Fit
        )

        Text(
            text = "Merkeziyetsiz Haberleşme Ağı",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(64.dp))

        RadarView(
            modifier = Modifier.size(300.dp),
            isMeshActive = true,
            neighborCount = 3
        )

        Spacer(modifier = Modifier.height(64.dp))

        Text(
            text = "Afet anında kapsama alanı dışında kalsanız bile\nçevrenizdekilerle iletişimde kalın.",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { navController.navigate(Screen.RegisterStep1.route) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = YankiGreen),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = "Hemen Başla",
                color = YankiDarkBg,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
