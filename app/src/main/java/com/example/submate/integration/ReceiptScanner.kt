package com.example.submate.integration

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object ReceiptScanner {
    suspend fun scan(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val image = InputImage.fromFilePath(context, uri)
        val korean = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
        val latin = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val first = korean.process(image).await().text
            val second = latin.process(image).await().text
            (first.lineSequence() + second.lineSequence()).map(String::trim).filter(String::isNotEmpty).distinct().joinToString("\n").also { check(it.isNotBlank()) { "이미지에서 글자를 찾지 못했습니다." } }
        } finally { korean.close(); latin.close() }
    }
}
