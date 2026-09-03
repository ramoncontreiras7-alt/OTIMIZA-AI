package com.otimiza.delivery.ui.scanner

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.otimiza.delivery.domain.model.Platform
import com.otimiza.delivery.domain.util.PlatformPatterns

class LabelOcrAnalyzer(
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
                    val match = PlatformPatterns.identifyPlatformAndId(block.text) ?: return@forEach
                    val (platform, nativeId) = match
                    if (PlatformPatterns.isDuplicate(nativeId)) return@forEach
                    mainHandler.post { onNativeIdDetected(nativeId, platform) }
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

    fun release() {
        recognizer.close()
    }
}