package br.com.doma.assistente

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.provider.Settings

class AudioHelper(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var callback: AudioDeviceCallback? = null

    fun audioOutputAvailable(type: Int): Boolean {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT)) {
            return false
        }
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type == type }
    }

    fun temAltoFalante() = audioOutputAvailable(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)

    fun temFone() = audioOutputAvailable(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)

    fun temSaida() = temAltoFalante() || temFone()

    fun monitorar(aoMudar: () -> Unit) {
        callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                aoMudar()
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                aoMudar()
            }
        }
        audioManager.registerAudioDeviceCallback(callback, null)
    }

    fun pararMonitoramento() {
        callback?.let { audioManager.unregisterAudioDeviceCallback(it) }
        callback = null
    }

    fun abrirConfigBluetooth() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
