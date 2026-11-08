package com.kampplus.hava.ui.city

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable

object MarkerIcons {

    fun createCustomPinBitmap(context: Context, colorHex: Int, sizeDp: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt()
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radius = sizePx / 2f

        val haloPaint = Paint().apply {
            color = colorHex
            alpha = (255 * 0.25f).toInt()
            isAntiAlias = true
        }
        canvas.drawCircle(radius, radius, radius, haloPaint)

        val innerRadius = radius * 0.7f
        val circlePaint = Paint().apply {
            color = colorHex
            isAntiAlias = true
        }
        canvas.drawCircle(radius, radius, innerRadius, circlePaint)

        val borderWidth = 3f * density
        val borderPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = borderWidth
            isAntiAlias = true
        }
        canvas.drawCircle(radius, radius, innerRadius - borderWidth / 2, borderPaint)

        return bitmap
    }

    /**
     * Şehir marker'ı için nokta + yanında beyaz kalın sıcaklık etiketi içeren Bitmap oluşturur.
     * Bitmap yanında (anchorX, anchorY) Pair'ini de döndürür.
     */
    fun createTemperaturePinBitmap(
        context: Context,
        colorHex: Int,
        dotSizeDp: Int,
        temperature: Int
    ): Pair<Bitmap, Pair<Float, Float>> {
        val density = context.resources.displayMetrics.density
        val dotSizePx = (dotSizeDp * density).toInt()
        val dotRadius = dotSizePx / 2f

        val text = "$temperature°"
        val textSizePx = 12f * density

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = textSizePx
            isFakeBoldText = true
            isAntiAlias = true
        }

        val textWidth = textPaint.measureText(text)
        val fontMetrics = textPaint.fontMetrics
        val textHeight = fontMetrics.bottom - fontMetrics.top

        val horizontalPadding = 6f * density
        val verticalPadding = 3f * density

        val pillWidth = textWidth + horizontalPadding * 2
        val pillHeight = textHeight + verticalPadding * 2

        val gap = 4f * density
        val totalWidth = (dotSizePx + gap + pillWidth).toInt()
        val totalHeight = maxOf(dotSizePx.toFloat(), pillHeight).toInt()

        val bitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val dotCenterY = totalHeight / 2f
        val dotCenterX = dotRadius

        // 1. Noktayı çiz
        val haloPaint = Paint().apply {
            color = colorHex
            alpha = (255 * 0.25f).toInt()
            isAntiAlias = true
        }
        canvas.drawCircle(dotCenterX, dotCenterY, dotRadius, haloPaint)

        val innerRadius = dotRadius * 0.7f
        val circlePaint = Paint().apply {
            color = colorHex
            isAntiAlias = true
        }
        canvas.drawCircle(dotCenterX, dotCenterY, innerRadius, circlePaint)

        val borderWidth = 3f * density
        val borderPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = borderWidth
            isAntiAlias = true
        }
        canvas.drawCircle(dotCenterX, dotCenterY, innerRadius - borderWidth / 2, borderPaint)

        // 2. Sıcaklık Pil Arka Planını Çiz
        val pillLeft = dotSizePx + gap
        val pillTop = dotCenterY - pillHeight / 2f
        val pillRight = pillLeft + pillWidth
        val pillBottom = pillTop + pillHeight

        val pillRect = RectF(pillLeft, pillTop, pillRight, pillBottom)
        val pillCorner = 8f * density

        val bgPaint = Paint().apply {
            color = Color.parseColor("#CC1E293B")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(pillRect, pillCorner, pillCorner, bgPaint)

        val pillBorderPaint = Paint().apply {
            color = Color.parseColor("#40FFFFFF")
            style = Paint.Style.STROKE
            strokeWidth = 1f * density
            isAntiAlias = true
        }
        canvas.drawRoundRect(pillRect, pillCorner, pillCorner, pillBorderPaint)

        // 3. Metni Çiz
        val textX = pillLeft + horizontalPadding
        val textY = pillTop + verticalPadding - fontMetrics.top
        canvas.drawText(text, textX, textY, textPaint)

        val anchorX = dotCenterX / totalWidth
        val anchorY = dotCenterY / totalHeight

        return bitmap to (anchorX to anchorY)
    }
}
