package br.com.doma.assistente

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import java.util.Locale

class SpeechManager(context: Context) {

    private var pronto = false
    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val r = tts.setLanguage(Locale("pt", "BR"))
                pronto = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }

    fun falar(texto: String) {
        if (pronto) {
            tts.speak(texto, TextToSpeech.QUEUE_ADD, null, "doma")
        }
    }

    fun alerta(texto: String) {
        val tom = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        tom.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1000)
        if (pronto) {
            tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "doma")
        }
    }

    fun parar() {
        tts.stop()
    }

    fun fechar() {
        tts.stop()
        tts.shutdown()
    }
}
