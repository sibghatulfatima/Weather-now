package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.WeatherDatabase
import com.example.data.location.LocationHelper
import com.example.data.repository.WeatherRepository
import com.example.model.CityLocation
import com.example.model.WeatherUiState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class WeatherViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: WeatherRepository = WeatherRepository(
        cachedWeatherDao = WeatherDatabase.getDatabase(application).cachedWeatherDao(),
        recentCityDao = WeatherDatabase.getDatabase(application).recentCityDao()
    ),
    private val locationHelper: LocationHelper = LocationHelper(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<CityLocation>>(emptyList())
    val searchResults: StateFlow<List<CityLocation>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    val recentCities: StateFlow<List<CityLocation>> = repository.getRecentCities()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var currentCityLocation: CityLocation? = null
    private var weatherFetchJob: Job? = null

    // Default fallback city if no GPS & no cache yet
    private val defaultCity = CityLocation(
        name = "London",
        country = "United Kingdom",
        adminArea = "England",
        latitude = 51.5074,
        longitude = -0.1278
    )

    init {
        // Observe search query with debounce
        viewModelScope.launch {
            _searchQuery
                .debounce(350)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.trim().length >= 2) {
                        performSearch(query.trim())
                    } else {
                        _searchResults.value = emptyList()
                        _isSearching.value = false
                    }
                }
        }

        // Initialize weather
        loadInitialWeather()
    }

    private fun loadInitialWeather() {
        viewModelScope.launch {
            // First check if cached weather is present in Room
            val cached = repository.getCachedWeather()
            if (cached != null) {
                currentCityLocation = cached.location
                _uiState.value = cached
                // Refresh silently in background
                fetchWeather(cached.location, isRefresh = true)
            } else {
                // If permission is already granted, try location; otherwise load default
                if (locationHelper.hasLocationPermission()) {
                    loadWeatherForCurrentLocation()
                } else {
                    loadWeatherForCity(defaultCity)
                }
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
        if (newQuery.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            val result = repository.searchCities(query)
            result.onSuccess { cities ->
                _searchResults.value = cities
            }.onFailure {
                _searchResults.value = emptyList()
            }
            _isSearching.value = false
        }
    }

    fun loadWeatherForCity(city: CityLocation) {
        currentCityLocation = city
        fetchWeather(city, isRefresh = false)
    }

    fun loadWeatherForCurrentLocation() {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading(previousData = (_uiState.value as? WeatherUiState.Success))
            val gpsLocation = locationHelper.getCurrentCityLocation()
            if (gpsLocation != null) {
                currentCityLocation = gpsLocation
                fetchWeather(gpsLocation, isRefresh = false)
            } else {
                // Graceful fallback: If current location failed or was denied, keep previous or use default
                val fallback = currentCityLocation ?: defaultCity
                _uiState.value = WeatherUiState.Error(
                    message = "Could not detect GPS location. Showing ${fallback.name}.",
                    cachedData = (_uiState.value as? WeatherUiState.Success)
                )
                fetchWeather(fallback, isRefresh = false)
            }
        }
    }

    fun refresh() {
        val target = currentCityLocation ?: defaultCity
        fetchWeather(target, isRefresh = true)
    }

    private fun fetchWeather(location: CityLocation, isRefresh: Boolean) {
        weatherFetchJob?.cancel()
        weatherFetchJob = viewModelScope.launch {
            val prevSuccess = when (val s = _uiState.value) {
                is WeatherUiState.Success -> s
                is WeatherUiState.Loading -> s.previousData
                is WeatherUiState.Error -> s.cachedData
            }

            if (!isRefresh && prevSuccess == null) {
                _uiState.value = WeatherUiState.Loading()
            } else if (prevSuccess != null) {
                _uiState.value = prevSuccess.copy(isRefreshing = true)
            }

            val result = repository.fetchWeather(location)
            result.onSuccess { (currentWeather, forecast) ->
                _uiState.value = WeatherUiState.Success(
                    location = location,
                    current = currentWeather,
                    forecast = forecast,
                    isRefreshing = false,
                    lastUpdatedTimestamp = System.currentTimeMillis()
                )
            }.onFailure { error ->
                _uiState.value = WeatherUiState.Error(
                    message = error.localizedMessage ?: "Unable to load weather. Please check your internet connection.",
                    cachedData = prevSuccess?.copy(isRefreshing = false)
                )
            }
        }
    }
}
