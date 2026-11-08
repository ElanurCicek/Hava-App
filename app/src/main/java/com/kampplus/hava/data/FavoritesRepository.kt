package com.kampplus.hava.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kampplus.hava.ui.forecast.ForecastViewModel.FavoriteCity

class FavoritesRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getFavorites(): List<FavoriteCity> {
        val json = prefs.getString(KEY_FAVORITES, null) ?: return DEFAULT_FAVORITES
        return try {
            val type = object : TypeToken<List<FavoriteCity>>() {}.type
            val list: List<FavoriteCity>? = gson.fromJson(json, type)
            if (!list.isNullOrEmpty()) list else DEFAULT_FAVORITES
        } catch (_: Exception) {
            DEFAULT_FAVORITES
        }
    }

    fun saveFavorites(favorites: List<FavoriteCity>) {
        try {
            val json = gson.toJson(favorites)
            prefs.edit().putString(KEY_FAVORITES, json).apply()
        } catch (_: Exception) {}
    }

    companion object {
        private const val PREFS_NAME = "hava_favorites_prefs"
        private const val KEY_FAVORITES = "favorite_cities"

        val DEFAULT_FAVORITES = listOf(
            FavoriteCity("İstanbul", "İstanbul, Türkiye", 41.0082, 28.9784),
            FavoriteCity("Ankara", "Ankara, Türkiye", 39.9334, 32.8597),
            FavoriteCity("İzmir", "İzmir, Türkiye", 38.4237, 27.1428),
            FavoriteCity("Bursa", "Bursa, Türkiye", 40.1885, 29.0610),
            FavoriteCity("Antalya", "Antalya, Türkiye", 36.8969, 30.7133),
            FavoriteCity("Trabzon", "Trabzon, Türkiye", 41.0027, 39.7168),
            FavoriteCity("Gaziantep", "Gaziantep, Türkiye", 37.0662, 37.3833)
        )
    }
}
