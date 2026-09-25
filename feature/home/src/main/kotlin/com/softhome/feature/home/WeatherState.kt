package com.softhome.feature.home

/** Local weather contract; a repository/API can replace this source later. */
enum class WeatherCondition {
    Clear,
    Cloudy,
    Rain,
    Night,
}

data class WeatherUiState(
    val condition: WeatherCondition = WeatherCondition.Clear,
    val temperatureLabel: String = "Current 8°C",
)
