package com.kampplus.hava.ui.forecast

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kampplus.hava.model.DailyForecast
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SunArcCard(
    dailyForecast: DailyForecast?,
    modifier: Modifier = Modifier,
    utcOffsetSeconds: Int? = null
) {
    if (dailyForecast?.sunrise == null || dailyForecast.sunset == null) return

    val sunrise = dailyForecast.sunrise
    val sunset = dailyForecast.sunset

    val offset = utcOffsetSeconds ?: 0
    val zoneOffset = ZoneOffset.ofTotalSeconds(offset)
    val now = LocalDateTime.ofInstant(Instant.now(), zoneOffset)

    val sunriseMinutes = sunrise.hour * 60 + sunrise.minute
    val sunsetMinutes = sunset.hour * 60 + sunset.minute
    val nowMinutes = now.hour * 60 + now.minute

    val dayLengthMinutes = (sunsetMinutes - sunriseMinutes).coerceAtLeast(1)
    val hours = dayLengthMinutes / 60
    val minutes = dayLengthMinutes % 60

    val progress = when {
        nowMinutes < sunriseMinutes -> 0f
        nowMinutes > sunsetMinutes -> 1f
        else -> (nowMinutes - sunriseMinutes).toFloat() / dayLengthMinutes
    }

    var animProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = animProgress,
        animationSpec = tween(durationMillis = 1000),
        label = "SunArcAnimation"
    )

    LaunchedEffect(progress) {
        animProgress = progress
    }

    val isDaytime = nowMinutes in sunriseMinutes..sunsetMinutes

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🌅 GÜN DOĞUMU & BATIMI",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                )
                Text(
                    text = "Gün uzunluğu: ${hours} sa ${minutes} dk",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.7f)
                    )
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .padding(horizontal = 16.dp)
            ) {
                val width = size.width
                val height = size.height
                val radius = height.coerceAtMost(width / 2)

                val center = Offset(width / 2f, height)

                drawArc(
                    color = Color.White.copy(alpha = 0.3f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 3.dp.toPx())
                )

                if (isDaytime) {
                    val angleRadians = Math.PI * (1.0 - animatedProgress.toDouble())
                    val sunX = center.x + (radius * cos(angleRadians)).toFloat()
                    val sunY = center.y - (radius * sin(angleRadians)).toFloat()

                    drawCircle(
                        color = Color(0xFFFFD166),
                        radius = 8.dp.toPx(),
                        center = Offset(sunX, sunY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(sunX, sunY)
                    )
                } else {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.4f),
                        radius = 6.dp.toPx(),
                        center = Offset(center.x, height + 10f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Doğum: ${sunrise.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f))
                )
                Text(
                    text = if (isDaytime) "Güneş gökyüzünde" else "Güneş battı",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFFD166), fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Batım: ${sunset.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f))
                )
            }
        }
    }
}
