package com.brandstation.larm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.brandstation.larm.databinding.ActivityPermissionsBinding

class PermissionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Behörigheter"

        binding.btnRequestNormal.setOnClickListener { requestNormalPermissions() }
        binding.btnOpenNotifSettings.setOnClickListener { openNotificationSettings() }
        binding.btnOpenOverlay.setOnClickListener { openOverlaySettings() }
        binding.btnOpenBattery.setOnClickListener { openBatterySettings() }
        binding.btnOpenFullscreen.setOnClickListener { openFullscreenIntentSettings() }
    }

    override fun onResume() {
        super.onResume()
        updateStatusDisplay()
    }

    private fun updateStatusDisplay() {
        val smsOk = hasPermission(Manifest.permission.RECEIVE_SMS)
        val notifOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        } else true
        val batteryOk = isBatteryOptimizationIgnored()
        val overlayOk = Settings.canDrawOverlays(this)
        val fullscreenOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+: USE_FULL_SCREEN_INTENT kräver separat godkännande
            packageManager.checkPermission(
                Manifest.permission.USE_FULL_SCREEN_INTENT, packageName
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        binding.statusReceiveSms.text = statusText("Ta emot SMS (RECEIVE_SMS)", smsOk)
        binding.statusNotifications.text = statusText("Notifikationer", notifOk)
        binding.statusBattery.text = statusText("Undantagen från batterioptimering", batteryOk)
        binding.statusOverlay.text = statusText("Visa över andra appar (overlay)", overlayOk)
        binding.statusFullscreen.text = statusText("Helskärmsnotis vid larm", fullscreenOk)

        val allOk = smsOk && notifOk && batteryOk
        binding.overallStatus.text = if (allOk) {
            "Alla kritiska behörigheter är beviljade"
        } else {
            "Vissa behörigheter saknas — appen kanske inte fungerar korrekt"
        }
        binding.overallStatus.setTextColor(
            getColor(if (allOk) R.color.status_green else android.R.color.holo_red_dark)
        )
    }

    private fun statusText(label: String, granted: Boolean): String {
        val icon = if (granted) "✓" else "✗"
        val status = if (granted) "Beviljad" else "SAKNAS"
        return "$icon  $label\n    → $status"
    }

    private fun requestNormalPermissions() {
        val perms = buildList {
            add(Manifest.permission.RECEIVE_SMS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.filter { !hasPermission(it) }

        if (perms.isEmpty()) {
            Toast.makeText(this, "Alla normala behörigheter redan beviljade!", Toast.LENGTH_SHORT).show()
        } else {
            ActivityCompat.requestPermissions(this, perms.toTypedArray(), REQ_PERMS)
        }
    }

    private fun openNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
        }
        startActivity(intent)
    }

    private fun openOverlaySettings() {
        startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
        )
    }

    private fun openBatterySettings() {
        runCatching {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        }.onFailure {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    private fun openFullscreenIntentSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            runCatching {
                startActivity(
                    Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                        data = Uri.parse("package:$packageName")
                    }
                )
            }.onFailure {
                Toast.makeText(this, "Öppna Inställningar → Appar → Brandstation Larm manuellt", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Behövs inte på din Android-version", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun hasPermission(perm: String): Boolean =
        ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_PERMS) updateStatusDisplay()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        private const val REQ_PERMS = 200
    }
}
