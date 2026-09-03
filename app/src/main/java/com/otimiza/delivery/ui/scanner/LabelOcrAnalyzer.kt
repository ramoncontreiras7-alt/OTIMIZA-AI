package com.otimiza.delivery.ui.scanner

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.otimiza.delivery.data.local.model.Platform

class LabelOcrAnalyzer(
    private val platformPattern: Regex,
    private val onNativeIdDetected: (nativeStopId: String, platform: Platform) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient()
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile
    private var processing = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (processing) {
            imageProxy.close()
            return
        }
        processing = true

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            processing = false
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                visionText.textBlocks.forEach { block ->
                    platformPattern.find(block.text)?.value?.let { rawNativeId ->
                        val platform = detectPlatform(rawNativeId)
                        mainHandler.post { onNativeIdDetected(rawNativeId, platform) }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w("LabelOcrAnalyzer", "OCR falhou: ${e.message}")
            }
            .addOnCompleteListener {
                imageProxy.close()
                processing = false
            }
    }

    private fun detectPlatform(nativeId: String): Platform = when {
        nativeId.startsWith("IFOOD", ignoreCase = true) -> Platform.IFOOD
        nativeId.startsWith("ML", ignoreCase = true) -> Platform.MERCADO_LIVRE
        nativeId.startsWith("LALA", ignoreCase = true) -> Platform.LALAMOVE
        else -> Platform.IFOOD
    }

    fun release() {
        recognizer.close()
    }
}
