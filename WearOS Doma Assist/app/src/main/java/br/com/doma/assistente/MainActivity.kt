package br.com.doma.assistente

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var audio: AudioHelper
    private lateinit var fala: SpeechManager
    private lateinit var status: TextView
    private lateinit var btnFone: Button

    private val mensagens = listOf(
        "Reunião de equipe às 14 horas na sala 3.",
        "Lembrete: almoço começa às 12 e 30.",
        "Novo treinamento de segurança disponível."
    )

    private val treino = listOf(
        "Treinamento de evacuação. Passo 1: mantenha a calma.",
        "Passo 2: siga as luzes verdes até a saída.",
        "Passo 3: vá até o ponto de encontro."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audio = AudioHelper(this)
        fala = SpeechManager(this)
        montarTela()
    }

    override fun onStart() {
        super.onStart()
        atualizarStatus()
        audio.monitorar { atualizarStatus() }
    }

    override fun onStop() {
        super.onStop()
        audio.pararMonitoramento()
        fala.parar()
    }

    override fun onDestroy() {
        super.onDestroy()
        fala.fechar()
    }

    private fun atualizarStatus() {
        val alto = if (audio.temAltoFalante()) "sim" else "não"
        val fone = if (audio.temFone()) "conectado" else "desconectado"
        status.text = "Alto-falante: $alto\nFone: $fone"
        btnFone.visibility = if (audio.temSaida()) View.GONE else View.VISIBLE
    }

    private fun comSaida(acao: () -> Unit) {
        if (audio.temSaida()) {
            acao()
        } else {
            Toast.makeText(this, "Conecte um fone", Toast.LENGTH_SHORT).show()
            audio.abrirConfigBluetooth()
        }
    }

    private fun lerMensagens() = comSaida {
        mensagens.forEach { fala.falar(it) }
    }

    private fun alertaSeguranca() = comSaida {
        fala.alerta("Atenção! Emergência. Dirija-se à saída mais próxima.")
    }

    private fun treinamento() = comSaida {
        treino.forEach { fala.falar(it) }
    }

    private fun comandoDeVoz() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
        try {
            startActivityForResult(intent, 2)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Sem reconhecimento de voz", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            comandoDeVoz()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 2 || resultCode != RESULT_OK) return

        val resultados = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val texto = resultados?.firstOrNull()?.lowercase() ?: return

        if (texto.contains("mensagem") || texto.contains("mensagens") || texto.contains("ler")) {
            lerMensagens()
        } else if (texto.contains("alerta") || texto.contains("emergência")) {
            alertaSeguranca()
        } else if (texto.contains("treinamento")) {
            treinamento()
        } else if (texto.contains("parar")) {
            fala.parar()
        } else {
            comSaida { fala.falar("Não entendi: $texto") }
        }
    }

    private fun montarTela() {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 60, 40, 80)

        val titulo = TextView(this)
        titulo.text = "Doma Assist"
        titulo.textSize = 16f
        titulo.setTextColor(Color.WHITE)
        titulo.gravity = Gravity.CENTER
        layout.addView(titulo)

        status = TextView(this)
        status.textSize = 12f
        status.setTextColor(Color.LTGRAY)
        status.gravity = Gravity.CENTER
        layout.addView(status)

        layout.addView(botao("Ler mensagens") { lerMensagens() })
        layout.addView(botao("Comando de voz") { comandoDeVoz() })
        layout.addView(botao("Alerta de segurança") { alertaSeguranca() })
        layout.addView(botao("Treinamento") { treinamento() })
        btnFone = botao("Conectar fone") { audio.abrirConfigBluetooth() }
        layout.addView(btnFone)
        layout.addView(botao("Parar áudio") { fala.parar() })

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Color.BLACK)
        scroll.addView(layout)
        setContentView(scroll)
    }

    private fun botao(texto: String, aoClicar: () -> Unit): Button {
        val b = Button(this)
        b.text = texto
        b.isAllCaps = false
        b.setOnClickListener { aoClicar() }
        return b
    }
}
