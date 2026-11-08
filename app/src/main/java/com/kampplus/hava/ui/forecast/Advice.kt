package com.kampplus.hava.ui.forecast

fun generateAdvice(
    temp: Double,
    weatherCode: Int,
    windSpeed: Double?,
    uvIndex: Double?,
    aqi: Int?
): List<String> {
    val advices = mutableListOf<String>()

    if (weatherCode in listOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99)) {
        advices.add("☂️ Şemsiyeni yanına al, yağmur bekleniyor.")
    } else if (weatherCode in listOf(71, 73, 75, 77, 85, 86)) {
        advices.add("⛄ Kalın giyin, yollar kayganlaşabilir.")
    }

    when {
        temp < 5 -> advices.add("🧥 Hava çok soğuk, kalın mont ve bere giyin.")
        temp in 5.0..12.0 -> advices.add("🧥 Hava serin, mont veya kalın palto tercih et.")
        temp in 12.0..18.0 -> advices.add("🧥 Ceket veya hırka giymek için uygun bir hava.")
        temp in 18.0..25.0 -> advices.add("👕 İnce kıyafetler ve tişört giyebilirsin.")
        temp > 25.0 && temp <= 32.0 -> advices.add("☀️ Hava sıcak, hafif ve açık renkli giysiler seç.")
        temp > 32.0 -> advices.add("💧 Hava oldukça sıcak, bol su içmeyi unutma.")
    }

    val conditionIsClear = weatherCode in listOf(0, 1)
    val aqiIsGood = aqi != null && aqi < 40
    val windOk = windSpeed == null || windSpeed < 25.0
    if (conditionIsClear && temp in 12.0..26.0 && windOk && aqiIsGood) {
        advices.add("🏃 Dışarıda koşu ve yürüyüş için harika bir hava!")
    }

    if (uvIndex != null && uvIndex >= 6.0) {
        advices.add("🕶️ UV indeksi yüksek, güneş kremi ve şapka kullan.")
    }

    if (aqi != null && aqi >= 60) {
        advices.add("😷 Hava kalitesi düşük, dışarıda uzun süre kalma.")
    }

    if (windSpeed != null && windSpeed > 40.0) {
        advices.add("💨 Rüzgarlı hava, hafif eşyalara dikkat et.")
    }

    return advices.take(3)
}
