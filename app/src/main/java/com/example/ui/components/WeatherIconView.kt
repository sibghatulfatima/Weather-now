package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.WeatherCondition
import com.example.ui.theme.CloudGray
import com.example.ui.theme.RainCyan
import com.example.ui.theme.SnowWhite
import com.example.ui.theme.StormPurple
import com.example.ui.theme.SunOrange
import com.example.ui.theme.SunYellow

@Composable
fun WeatherIconView(
    condition: WeatherCondition,
    isDay: Boolean = true,
    size: Dp = 110.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "weather_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val (iconColor, glowColors) = when (condition) {
        WeatherCondition.SUNNY, WeatherCondition.MAINLY_CLEAR -> {
            if (isDay) {
                Pair(
                    SunOrange,
                    listOf(SunYellow.copy(alpha = 0.35f), SunOrange.copy(alpha = 0.15f), Color.Transparent)
                )
            } else {
                Pair(
                    SunYellow,
                    listOf(SunYellow.copy(alpha = 0.25f), Color.Transparent)
                )
            }
        }
        WeatherCondition.PARTLY_CLOUDY -> Pair(
            SunYellow,
            listOf(SunYellow.copy(alpha = 0.2f), CloudGray.copy(alpha = 0.2f), Color.Transparent)
        )
        WeatherCondition.OVERCAST -> Pair(
            CloudGray,
            listOf(CloudGray.copy(alpha = 0.3f), Color.Transparent)
        )
        WeatherCondition.DRIZZLE, WeatherCondition.RAINY, WeatherCondition.HEAVY_RAIN -> Pair(
            RainCyan,
            listOf(RainCyan.copy(alpha = 0.3f), Color.Transparent)
        )
        WeatherCondition.THUNDERSTORM -> Pair(
            StormPurple,
            listOf(StormPurple.copy(alpha = 0.35f), SunYellow.copy(alpha = 0.15f), Color.Transparent)
        )
        WeatherCondition.SNOWY -> Pair(
            SnowWhite,
            listOf(RainCyan.copy(alpha = 0.25f), SnowWhite.copy(alpha = 0.2f), Color.Transparent)
        )
        WeatherCondition.FOGGY -> Pair(
            CloudGray,
            listOf(CloudGray.copy(alpha = 0.25f), Color.Transparent)
        )
        WeatherCondition.UNKNOWN -> Pair(
            MaterialTheme.colorScheme.primary,
            listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), Color.Transparent)
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("weather_central_icon"),
        contentAlignment = Alignment.Center
    ) {
        // Glowing animated aura
        Box(
            modifier = Modifier
                .size(size)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(Brush.radialGradient(glowColors))
        )

        // Main Weather Icon
        Icon(
            imageVector = condition.icon,
            contentDescription = condition.displayName,
            tint = iconColor,
            modifier = Modifier
                .size(size * 0.65f)
                .scale(pulseScale)
        )
    }
}
