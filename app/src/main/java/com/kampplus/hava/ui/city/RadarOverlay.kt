package com.kampplus.hava.ui.city

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.TilesOverlay

/**
 * RainViewer API'sinden en güncel radyo katmanını (TilesOverlay) indirir.
 */
fun fetchRadarOverlay(context: Context): TilesOverlay? {
    return try {
        val url = URL("https://api.rainviewer.com/public/weather-maps.json")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.requestMethod = "GET"
        conn.connect()

        val responseCode = conn.responseCode
        if (responseCode != 200) return null

        val jsonString = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()

        val rootObj = JSONObject(jsonString)
        val host = rootObj.optString("host", "")
        if (host.isEmpty()) return null

        val radarObj = rootObj.optJSONObject("radar") ?: return null
        val pastArray = radarObj.optJSONArray("past") ?: return null
        if (pastArray.length() == 0) return null

        val lastPast = pastArray.getJSONObject(pastArray.length() - 1)
        val path = lastPast.optString("path", "")
        if (path.isEmpty()) return null

        val tileSource = object : OnlineTileSourceBase(
            "RainViewer",
            0,
            7,
            256,
            ".png",
            arrayOf(host)
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val z = MapTileIndex.getZoom(pMapTileIndex)
                val x = MapTileIndex.getX(pMapTileIndex)
                val y = MapTileIndex.getY(pMapTileIndex)
                return "$host$path/256/$z/$x/$y/2/1_1.png"
            }
        }

        val tileProvider = MapTileProviderBasic(context, tileSource)
        TilesOverlay(tileProvider, context).apply {
            loadingBackgroundColor = Color.TRANSPARENT
            loadingLineColor = Color.TRANSPARENT
        }
    } catch (e: Exception) {
        Log.e("RadarOverlay", "Error fetching radar overlay", e)
        null
    }
}

/**
 * Haritanın sol/sağ alt köşesinde telif / atıf yazısı overlay'i.
 */
class CustomAttributionOverlay(context: Context) : CopyrightOverlay(context) {
    init {
        setTextColor(Color.WHITE)
        setTextSize(10)
    }

    override fun draw(canvas: Canvas, map: MapView, shadow: Boolean) {
        setCopyrightNotice("© OpenStreetMap · Radar: RainViewer")
        super.draw(canvas, map.getProjection())
    }
}
