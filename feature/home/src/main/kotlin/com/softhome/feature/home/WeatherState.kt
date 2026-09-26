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
    /**
     * Localized temperature text. Blank until a real weather source is wired in
     * (the UI falls back to the placeholder string `home_weather_placeholder`).
     */
    val temperatureLabel: String = "",
)
