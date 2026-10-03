package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CityLocation
import com.example.model.CurrentWeather
import com.example.model.DailyForecast
import com.example.model.WeatherUiState
import com.example.ui.components.CitySearchSheet
import com.example.ui.components.DailyForecastCard
import com.example.ui.components.WatermarkView
import com.example.ui.components.WeatherIconView
import com.example.ui.components.WeatherMetricCard
import com.example.ui.theme.RainCyan
import com.example.ui.theme.SunOrange
import com.example.ui.theme.SunYellow
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: WeatherUiState,
    searchQuery: String,
    searchResults: List<CityLocation>,
    recentCities: List<CityLocation>,
    isSearching: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onSelectCity: (CityLocation) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSearchSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isRefreshing = (uiState as? WeatherUiState.Success)?.isRefreshing == true
    val refreshRotation = if (isRefreshing) {
        val transition = rememberInfiniteTransition(label = "refresh_rotation")
        val angle by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "angle"
        )
        angle
    } else 0f

    val locationName = when (uiState) {
        is WeatherUiState.Success -> uiState.location.name
        is WeatherUiState.Error -> uiState.cachedData?.location?.name ?: "WeatherNow"
        is WeatherUiState.Loading -> uiState.previousData?.location?.name ?: "Loading..."
    }

    val locationSubtitle = when (uiState) {
        is WeatherUiState.Success -> listOfNotNull(uiState.location.adminArea, uiState.location.country)
            .filter { it.isNotBlank() }
            .joinToString(", ")
        is WeatherUiState.Error -> uiState.cachedData?.location?.let {
            listOfNotNull(it.adminArea, it.country).filter { s -> s.isNotBlank() }.joinToString(", ")
        } ?: ""
        is WeatherUiState.Loading -> ""
    }

    val isGpsActive = (uiState as? WeatherUiState.Success)?.location?.isCurrentGpsLocation == true

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = locationName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("city_name_title")
                            )
                            if (isGpsActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Current location active",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        if (locationSubtitle.isNotBlank()) {
                            Text(
                                text = locationSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onUseCurrentLocation,
                        modifier = Modifier.testTag("location_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Use current location",
                            tint = if (isGpsActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { showSearchSheet = true },
                        modifier = Modifier.testTag("search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search city",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.testTag("refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh weather",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.rotate(refreshRotation)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            // Unobtrusive, consistent watermark at the bottom of the screen
            WatermarkView()
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState) {
                is WeatherUiState.Loading -> {
                    if (uiState.previousData != null) {
                        WeatherContent(
                            current = uiState.previousData.current,
                            forecast = uiState.previousData.forecast,
                            onRefresh = onRefresh
                        )
                    } else {
                        FullScreenLoading()
                    }
                }

                is WeatherUiState.Success -> {
                    WeatherContent(
                        current = uiState.current,
                        forecast = uiState.forecast,
                        onRefresh = onRefresh
                    )
                }

                is WeatherUiState.Error -> {
                    if (uiState.cachedData != null) {
                        WeatherContent(
                            current = uiState.cachedData.current,
                            forecast = uiState.cachedData.forecast,
                            onRefresh = onRefresh
                        )
                        // Floating error banner
                        AnimatedVisibility(
                            visible = true,
                            enter = slideInVertically() + fadeIn(),
                            exit = slideOutVertically() + fadeOut(),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(16.dp)
                        ) {
                            Snackbar(
                                action = {
                                    TextButton(onClick = onRefresh) {
                                        Text("Retry")
                                    }
                                }
                            ) {
                                Text(uiState.message)
                            }
                        }
                    } else {
                        FullScreenError(
                            errorMessage = uiState.message,
                            onRetry = onRefresh
                        )
                    }
                }
            }
        }
    }

    if (showSearchSheet) {
        CitySearchSheet(
            sheetState = sheetState,
            searchQuery = searchQuery,
            onQueryChanged = onSearchQueryChanged,
            searchResults = searchResults,
            recentCities = recentCities,
            isSearching = isSearching,
            onSelectCity = { city ->
                onSelectCity(city)
                showSearchSheet = false
            },
            onUseCurrentLocation = {
                onUseCurrentLocation()
                showSearchSheet = false
            },
            onDismiss = { showSearchSheet = false }
        )
    }
}

@Composable
private fun WeatherContent(
    current: CurrentWeather,
    forecast: List<DailyForecast>,
    onRefresh: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Central Weather Icon
            WeatherIconView(
                condition = current.condition,
                isDay = current.isDay,
                size = 120.dp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Large Temperature Display in Celsius
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.testTag("temperature_display")
            ) {
                Text(
                    text = "${current.temperatureCelsius.roundToInt()}",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-2).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "°C",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Weather Condition Label Pill
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = current.condition.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("condition_text")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Today's High and Low Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.testTag("high_low_display")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "High temp",
                        tint = SunOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "H: ${current.highTempCelsius.roundToInt()}°C",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "•",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Low temp",
                        tint = RainCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "L: ${current.lowTempCelsius.roundToInt()}°C",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Weather Metrics Grid (Simple Cards)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WeatherMetricCard(
                    title = "Humidity",
                    value = "${current.humidityPercentage}%",
                    icon = Icons.Default.WaterDrop,
                    iconTint = RainCyan,
                    subValue = if (current.humidityPercentage > 70) "Humid" else "Comfortable",
                    testTagId = "metric_humidity",
                    modifier = Modifier.weight(1f)
                )
                WeatherMetricCard(
                    title = "Wind Speed",
                    value = "${current.windSpeedKmh.roundToInt()} km/h",
                    icon = Icons.Default.Air,
                    iconTint = MaterialTheme.colorScheme.primary,
                    subValue = "Direction ${current.windDirectionDegrees}°",
                    testTagId = "metric_wind",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WeatherMetricCard(
                    title = "Feels Like",
                    value = "${current.feelsLikeCelsius.roundToInt()}°C",
                    icon = Icons.Default.Thermostat,
                    iconTint = SunOrange,
                    subValue = when {
                        current.feelsLikeCelsius > current.temperatureCelsius -> "Warmer than actual"
                        current.feelsLikeCelsius < current.temperatureCelsius -> "Cooler than actual"
                        else -> "Similar to actual"
                    },
                    testTagId = "metric_feels_like",
                    modifier = Modifier.weight(1f)
                )
                WeatherMetricCard(
                    title = "Pressure",
                    value = "${current.pressureHpa.roundToInt()} hPa",
                    icon = Icons.Default.Compress,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    subValue = "UV Index: ${current.uvIndex.roundToInt()}",
                    testTagId = "metric_pressure",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5-Day Forecast Section
            if (forecast.isNotEmpty()) {
                DailyForecastCard(forecast = forecast)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FullScreenLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("loading_state"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
            Text(
                text = "Fetching weather updates...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FullScreenError(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("error_state"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )

            Text(
                text = "Weather Unavailable",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("retry_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Try Again")
            }
        }
    }
}
