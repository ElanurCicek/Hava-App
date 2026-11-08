package com.kampplus.hava.ui.city

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonSyntaxException
import com.kampplus.hava.data.CityRepository
import com.kampplus.hava.data.OpenMeteoCityRepository
import com.kampplus.hava.model.City
import com.kampplus.hava.ui.LoadState
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * Şehir listesi ekranının beyni.
 *
 * Görev dağılımı:
 * - Ekran (Composable) yalnızca çizer.
 * - ViewModel state'i tutar ve kararları verir.
 * - Repository veriyi getirir.
 *
 * ViewModel ekran döndüğünde (Activity yeniden kurulurken) yaşamaya devam eder;
 * bu yüzden arama metni ve sonuçlar kaybolmaz.
 *
 * @param repository Varsayılanı gerçek servis. Testlerde sahte bir repository verilir.
 *   Tüm parametrelerin varsayılanı olduğu için Kotlin parametresiz bir kurucu da üretir;
 *   ekranlarda `viewModel()` bunu kullanabilir.
 */
class CityViewModel(
    private val repository: CityRepository = OpenMeteoCityRepository()
) : ViewModel() {

    // State ViewModel'de MutableStateFlow ile tutulur.
    // Değiştirilebilir alan private'tır; dışarıya yalnızca okunabilir hâli açılır.
    private val _query = MutableStateFlow(INITIAL_QUERY)
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<LoadState<List<City>>>(LoadState.Success(emptyList()))
    val uiState: StateFlow<LoadState<List<City>>> = _uiState.asStateFlow()

    /** Kullanıcı arama kutusuna her harf yazdığında çağrılır. */
    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        if (newQuery.isEmpty()) {
            _uiState.value = LoadState.Success(emptyList())
        }
    }

    /** Arama sorgusunu ve öneri listesini tamamen temizler. */
    fun clearQuery() {
        _query.value = ""
        _uiState.value = LoadState.Success(emptyList())
    }

    /**
     * Aramayı çalıştırır ve sonucu dört durumdan birine çevirir.
     *
     * `viewModelScope.launch`: yeni bir coroutine başlatır. Ekran kapanırsa bu kapsam
     * işi otomatik iptal eder, yarıda kalan istek sızıntı yapmaz.
     */
    fun search() {
        if (_query.value.isBlank()) {
            _uiState.value = LoadState.Success(emptyList())
            return
        }
        viewModelScope.launch {
            _uiState.value = LoadState.Loading
            _uiState.value = try {
                val cities = repository.search(_query.value)
                if (cities.isEmpty()) LoadState.Empty else LoadState.Success(cities)
            } catch (e: IOException) {
                Log.e(TAG, "Arama başarısız: bağlantı hatası", e)
                LoadState.Error("İnternet bağlantını kontrol et.")
            } catch (e: HttpException) {
                Log.e(TAG, "Arama başarısız: HTTP ${e.code()}", e)
                LoadState.Error("Sunucu şu an cevap vermiyor.")
            } catch (e: JsonSyntaxException) {
                Log.e(TAG, "Arama başarısız: yanıt çözümlenemedi", e)
                LoadState.Error("Bir şeyler ters gitti. Tekrar dene.")
            }
        }
    }

    private companion object {
        const val TAG = "CityViewModel"

        /** Uygulama ilk açıldığında varsayılan arama boş metindir. */
        const val INITIAL_QUERY = ""
    }
}
