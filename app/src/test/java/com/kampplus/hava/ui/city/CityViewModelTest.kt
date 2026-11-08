package com.kampplus.hava.ui.city

import com.kampplus.hava.model.City
import com.kampplus.hava.testing.FakeCityRepository
import com.kampplus.hava.testing.MainDispatcherRule
import com.kampplus.hava.testing.sampleCity
import com.kampplus.hava.ui.LoadState
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Şehir listesi ekranının durumlarını doğrular.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CityViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeCityRepository()

    private fun createViewModel() = CityViewModel(repository)

    @Test
    fun `baslangicta arama cubugu bostur`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("", viewModel.query.value)
        assertEquals(LoadState.Success(emptyList<City>()), viewModel.uiState.value)
    }

    @Test
    fun `veri gelince Success durumuna gecilir`() = runTest {
        repository.result = listOf(sampleCity(), sampleCity(id = 2L, name = "İzmir"))

        val viewModel = createViewModel()
        viewModel.onQueryChange("İzmir")
        viewModel.search()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Success bekleniyordu, gelen: $state", state is LoadState.Success)
        assertEquals(2, (state as LoadState.Success).data.size)
    }

    @Test
    fun `bos liste Empty durumuna cevrilir`() = runTest {
        repository.result = emptyList()

        val viewModel = createViewModel()
        viewModel.onQueryChange("BilinmeyenSehir")
        viewModel.search()
        advanceUntilIdle()

        assertEquals(LoadState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `baglanti hatasi anlasilir mesaja cevrilir`() = runTest {
        repository.error = IOException("no network")

        val viewModel = createViewModel()
        viewModel.onQueryChange("Ankara")
        viewModel.search()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Error bekleniyordu, gelen: $state", state is LoadState.Error)
        assertEquals("İnternet bağlantını kontrol et.", (state as LoadState.Error).message)
    }

    @Test
    fun `sunucu hatasi farkli mesaj gosterir`() = runTest {
        repository.error = HttpException(
            Response.error<Unit>(500, "".toResponseBody("application/json".toMediaType()))
        )

        val viewModel = createViewModel()
        viewModel.onQueryChange("Ankara")
        viewModel.search()
        advanceUntilIdle()

        assertEquals(
            "Sunucu şu an cevap vermiyor.",
            (viewModel.uiState.value as LoadState.Error).message
        )
    }

    @Test
    fun `onQueryChange arama metnini gunceller ama istek atmaz`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val callsAfterInit = repository.callCount

        viewModel.onQueryChange("Rize")
        advanceUntilIdle()

        assertEquals("Rize", viewModel.query.value)
        assertEquals(callsAfterInit, repository.callCount)
    }

    @Test
    fun `search guncel metinle istek atar`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onQueryChange("Rize")
        viewModel.search()
        advanceUntilIdle()

        assertEquals("Rize", repository.lastQuery)
    }

    @Test
    fun `clearQuery arama sorgusunu temizler`() = runTest {
        val viewModel = createViewModel()
        viewModel.onQueryChange("Ankara")
        viewModel.clearQuery()
        advanceUntilIdle()

        assertEquals("", viewModel.query.value)
        assertEquals(LoadState.Success(emptyList<City>()), viewModel.uiState.value)
    }
}
