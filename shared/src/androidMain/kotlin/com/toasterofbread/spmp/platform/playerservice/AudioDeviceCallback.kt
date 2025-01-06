package com.toasterofbread.spmp.platform.playerservice

import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.os.Build
import androidx.media3.common.Player
import kotlinx.coroutines.launch

internal class PlayerAudioDeviceCallback(
    private val service: ForegroundPlayerService
): AudioDeviceCallback() {
    private fun isBluetoothAudio(device: AudioDeviceInfo): Boolean {
        if (!device.isSink) {
            return false
        }
        return device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
    }
    private fun isWiredAudio(device: AudioDeviceInfo): Boolean {
        if (!device.isSink) {
            return false
        }
        return (
            device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && device.type == AudioDeviceInfo.TYPE_USB_HEADSET)
        )
    }

    override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
        if (service.player.isPlaying || !service.paused_by_device_disconnect) {
            return
        }

        service.coroutine_scope.launch {
            val resume_on_bt: Boolean = service.context.settings.Player.RESUME_ON_BT_CONNECT.get()
            val resume_on_wired: Boolean = service.context.settings.Player.RESUME_ON_WIRED_CONNECT.get()

            for (device in addedDevices) {
                if ((resume_on_bt && isBluetoothAudio(device)) || (resume_on_wired && isWiredAudio(device))) {
                    service.player.play()
                    break
                }
            }
        }
    }

    override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
        if (!service.player.isPlaying && service.player.playbackState == Player.STATE_READY) {
            return
        }

        service.coroutine_scope.launch {
            val pause_on_bt: Boolean = service.context.settings.Player.PAUSE_ON_BT_DISCONNECT.get()
            val pause_on_wired: Boolean = service.context.settings.Player.PAUSE_ON_WIRED_DISCONNECT.get()

            for (device in removedDevices) {
                if ((pause_on_bt && isBluetoothAudio(device)) || (pause_on_wired && isWiredAudio(device))) {
                    service.device_connection_changed_playing_status = true
                    service.paused_by_device_disconnect = true
                    service.player.pause()
                    break
                }
            }
        }
    }
}
