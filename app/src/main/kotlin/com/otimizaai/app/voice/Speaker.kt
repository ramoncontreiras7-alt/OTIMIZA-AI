package com.otimizaai.app.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lê textos em voz alta (como a leitura da corrida do Gigu), usando a voz
 * do próprio Android em português. Funciona sem internet se a voz estiver instalada.
 */
@Singleton
class Speaker @Inject constructor(
    @ApplicationContext context: Context,
) {
    private var ready = false
    private var pending: String? = null

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            ready = true
            pending?.let { speakNow(it) }
            pending = null
        }
    }

    fun speak(text: String) {
        if (ready) speakNow(text) else pending = text
    }

    private fun speakNow(text: String) {
        tts.language = Locale("pt", "BR")
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "otimiza-ai")
    }
}
