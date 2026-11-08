package com.kampplus.hava.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kampplus.hava.model.City
import com.kampplus.hava.ui.city.CityRoute
import com.kampplus.hava.ui.forecast.ForecastViewModel
import com.kampplus.hava.ui.forecast.ForecastRoute

/**
 * Uygulamanın iki ekranı ve aralarındaki geçiş.
 *
 * Navigation Compose, her ekranı bir "rota" (metin adresi) ile tanımlar. Bir şehre
 * dokunulduğunda adresi üretip [NavHostController.navigate] ile o adrese gideriz;
 * geri tuşu yığından bir önceki ekrana döner.
 *
 * Parametreleri yol parçası (`forecast/{name}`) yerine sorgu parametresi olarak
 * taşıyoruz (`forecast?name=...`); çünkü değerler boş olabilir ve boş bir yol
 * parçası rotayı eşleşmez hâle getirir.
 */
@Composable
fun HavaNavHost(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = FORECAST_ROUTE,
        modifier = modifier
    ) {
        composable(CITY_ROUTE) {
            CityRoute(
                onCityClick = { city -> navController.navigate(forecastRoute(city)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = FORECAST_ROUTE,
            arguments = listOf(
                // Hepsi String: koordinatlar ViewModel içinde Double'a çevrilir.
                navArgument("cityName") {
                    type = NavType.StringType
                    defaultValue = "İstanbul"
                },
                navArgument("subtitle") {
                    type = NavType.StringType
                    defaultValue = "İstanbul, Türkiye"
                },
                navArgument("latitude") {
                    type = NavType.StringType
                    defaultValue = "41.0082"
                },
                navArgument("longitude") {
                    type = NavType.StringType
                    defaultValue = "28.9784"
                }
            )
        ) {
            ForecastRoute(
                onBack = { navController.popBackStack() },
                onNavigateToCity = { navController.navigate(CITY_ROUTE) }
            )
        }
    }
}

/** Liste ekranının adresi. */
private const val CITY_ROUTE = "city"

/** Tahmin ekranının adres şablonu; süslü parantezler çalışma anında doldurulur. */
private const val FORECAST_ROUTE =
    "forecast?name={name}&subtitle={subtitle}&latitude={latitude}&longitude={longitude}"

/**
 * Bir şehir için tahmin ekranının gerçek adresini üretir.
 *
 * `Uri.encode`: şehir adındaki boşluk ve Türkçe karakterler adreste sorun
 * çıkarmasın diye kaçışlanır ("Afyonkarahisar, Türkiye" → "Afyonkarahisar%2C%20T%C3%BCrkiye").
 */
private fun forecastRoute(city: City): String = buildString {
    append("forecast")
    append("?name=").append(Uri.encode(city.name))
    append("&subtitle=").append(Uri.encode(city.subtitle))
    append("&latitude=").append(city.latitude)
    append("&longitude=").append(city.longitude)
}
