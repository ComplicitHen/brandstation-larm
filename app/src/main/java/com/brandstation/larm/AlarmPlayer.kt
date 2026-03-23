package com.brandstation.larm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class AlarmPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var savedVolume = -1

    fun play(alarmType: AlarmType) {
        stop()

        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        savedVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
        am.setStreamVolume(
            AudioManager.STREAM_ALARM,
            am.getStreamMaxVolume(AudioManager.STREAM_ALARM),
            0
        )

        val prefs = Prefs(context)
        val customUri = prefs.customSoundUri

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )

                // Prioritet: eget ljud → inbyggd larmsignal → systemlarm
                val rawRes = if (alarmType == AlarmType.TOTAL) R.raw.alarm_total else R.raw.alarm_regular
                when {
                    customUri != null -> {
                        setDataSource(context, android.net.Uri.parse(customUri))
                        Log.d("AlarmPlayer", "Spelar eget ljud: $customUri")
                    }
                    else -> {
                        val fd = context.resources.openRawResourceFd(rawRes)
                        setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                        fd.close()
                        Log.d("AlarmPlayer", "Spelar inbyggd larmsignal ($alarmType)")
                    }
                }
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("AlarmPlayer", "Kunde inte spela bundlad signal, fallback till systemlarm", e)
            // Fallback: systemets larmsignal
            runCatching {
                mediaPlayer?.release()
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(context, fallbackUri)
                    isLooping = true
                    prepare()
                    start()
                }
            }
        }

        vibrate(alarmType)
    }

    fun stop() {
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null

        if (savedVolume >= 0) {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.setStreamVolume(AudioManager.STREAM_ALARM, savedVolume, 0)
            savedVolume = -1
        }

        stopVibration()
    }

    private fun vibrate(alarmType: AlarmType) {
        // Totallarm: intensivare vibrationsmönster
        val pattern = if (alarmType == AlarmType.TOTAL) {
            longArrayOf(0, 800, 200, 800, 200, 800, 500)
        } else {
            longArrayOf(0, 600, 300, 600, 300, 1000, 300)
        }

        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(pattern, 0)
            }
        }
    }

    private fun stopVibration() {
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator.cancel()
        } else {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            v.cancel()
        }
    }
}
