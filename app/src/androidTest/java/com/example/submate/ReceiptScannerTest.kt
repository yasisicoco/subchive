package com.example.submate

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.submate.domain.ReceiptParser
import com.example.submate.integration.ReceiptScanner
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ReceiptScannerTest {
    @Test fun bundledModelsReadReceiptWithoutUserMedia() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<SubMateApplication>()
        val bitmap = Bitmap.createBitmap(1000, 500, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 64f }
        listOf("NETFLIX", "KRW 17,000", "2026-11-15").forEachIndexed { index, text -> canvas.drawText(text, 50f, 100f + index * 110, paint) }
        val image = File(context.cacheDir, "ocr-instrumentation.png")
        try {
            image.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val result = ReceiptParser.parse(ReceiptScanner.scan(context, Uri.fromFile(image)))
            assertEquals("netflix", result.service?.key)
            assertEquals("17000", result.amount)
            assertEquals(java.time.LocalDate.of(2026, 11, 15), result.date)
        } finally { bitmap.recycle(); image.delete() }
    }
}
