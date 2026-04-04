package com.bedir.yanki.ui.radar

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bedir.yanki.ui.theme.YankiGreen
import com.bedir.yanki.ui.theme.YankiGreyDot
import com.bedir.yanki.ui.theme.YankiRadarRings

data class NeighborPoint(val xRatio: Float, val yRatio: Float, val isOnline: Boolean)

@Composable
fun RadarView(
    modifier: Modifier = Modifier,
    isMeshActive: Boolean,
    neighborCount: Int,
    neighbors: List<NeighborPoint> = emptyList()
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = this.center
            val maxRadius = size.minDimension / 2.1f

            for (i in 1..3) {
                drawCircle(
                    color = YankiRadarRings,
                    radius = maxRadius * (i / 3f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            if (isMeshActive) {
                drawCircle(
                    color = YankiGreen.copy(alpha = pulseAlpha),
                    radius = maxRadius * pulseScale,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            drawCircle(
                color = YankiGreen,
                radius = 10.dp.toPx(),
                center = center
            )

            if (isMeshActive && (neighborCount > 0 || neighbors.isNotEmpty())) {
                val displayPoints = neighbors.ifEmpty {
                    listOf(
                        NeighborPoint(-0.6f, -0.6f, true),
                        NeighborPoint(0.7f, -0.4f, true),
                        NeighborPoint(-0.5f, 0.7f, true),
                        NeighborPoint(0.8f, 0.5f, false)
                    ).take(neighborCount)
                }

                displayPoints.forEach { point ->
                    val dotPositionX = center.x + (point.xRatio * maxRadius)
                    val dotPositionY = center.y + (point.yRatio * maxRadius)
                    val dotCenter = Offset(dotPositionX, dotPositionY)

                    drawLine(
                        color = if (point.isOnline) YankiGreen.copy(alpha = 0.5f) else YankiGreyDot.copy(alpha = 0.3f),
                        start = center,
                        end = dotCenter,
                        strokeWidth = 1.dp.toPx()
                    )

                    drawCircle(
                        color = if (point.isOnline) YankiGreen else YankiGreyDot,
                        radius = 8.dp.toPx(),
                        center = dotCenter
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "SEN",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
