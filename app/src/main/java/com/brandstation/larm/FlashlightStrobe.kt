package com.brandstation.larm

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.util.Log

class FlashlightStrobe(private val context: Context) {

    private var active = false
    private val handler = Handler(Looper.getMainLooper())
    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null
    private var torchOn = false

    fun start() {
        try {
            cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
                val chars = cameraManager?.getCameraCharacteristics(id)
                chars?.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK &&
                    chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId == null) {
                Log.w(TAG, "Ingen bakre kamera med ficklampa hittades")
                return
            }
            active = true
            blink()
        } catch (e: Exception) {
            Log.e(TAG, "Kunde inte starta ficklampa: ${e.message}")
        }
    }

    fun stop() {
        active = false
        handler.removeCallbacksAndMessages(null)
        try {
            val id = cameraId ?: return
            cameraManager?.setTorchMode(id, false)
        } catch (e: Exception) {
            Log.w(TAG, "Fel vid stängning av ficklampa: ${e.message}")
        }
        torchOn = false
    }

    private fun blink() {
        if (!active) return
        try {
            val id = cameraId ?: return
            torchOn = !torchOn
            cameraManager?.setTorchMode(id, torchOn)
        } catch (e: Exception) {
            Log.w(TAG, "Fel vid blink: ${e.message}")
        }
        if (active) {
            handler.postDelayed(::blink, 300)
        }
    }

    companion object {
        private const val TAG = "FlashlightStrobe"
    }
}
