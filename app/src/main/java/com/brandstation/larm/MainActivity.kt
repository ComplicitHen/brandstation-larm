package com.brandstation.larm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.brandstation.larm.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Prefs
    private lateinit var scheduleManager: ScheduleManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs(this)
        scheduleManager = ScheduleManager(this)

        setupUI()
        requestRequiredPermissions()
        ensureServiceRunning()
    }

    override fun onResume() {
        super.onResume()
        updateStatusDisplay()
        binding.enableSwitch.isChecked = prefs.isEnabled
    }

    private fun setupUI() {
        binding.enableSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.isEnabled = isChecked
            updateStatusDisplay()
            val intent = Intent(this, AlarmService::class.java).apply {
                action = AlarmService.ACTION_START_MONITOR
            }
            startForegroundService(intent)
        }

        binding.btnSchedule.setOnClickListener {
            startActivity(Intent(this, ScheduleActivity::class.java))
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Testknapp: Totallarm — triggar direkt utan att skicka SMS
        binding.btnTestTotal.setOnClickListener {
            triggerTestAlarm(AlarmType.TOTAL, "TOTALLARM Testlarm från appen")
        }

        // Testknapp: Vanligt larm
        binding.btnTestRegular.setOnClickListener {
            triggerTestAlarm(AlarmType.REGULAR, "Larm 2:42 Testlarm från appen")
        }

        // Stäng av pågående larm
        binding.btnStopAlarm.setOnClickListener {
            val intent = Intent(this, AlarmService::class.java).apply {
                action = AlarmService.ACTION_DISMISS
            }
            startService(intent)
            Toast.makeText(this, "Larm stängt av", Toast.LENGTH_SHORT).show()
        }

        // Batterioptimering: visa systeminställning
        binding.btnBatteryOpt.setOnClickListener {
            openBatteryOptimizationSettings()
        }
    }

    private fun triggerTestAlarm(alarmType: AlarmType, message: String) {
        val intent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_TRIGGER
            putExtra(AlarmService.EXTRA_ALARM_TYPE, alarmType.name)
            putExtra(AlarmService.EXTRA_MESSAGE, message)
        }
        startForegroundService(intent)
    }

    private fun updateStatusDisplay() {
        val enabled = prefs.isEnabled
        val onDuty = enabled && scheduleManager.isOnDuty()

        val (statusText, color) = when {
            !enabled -> Pair("Inaktiverad", getColor(android.R.color.darker_gray))
            onDuty   -> Pair("På pass – övervakar alla larm", getColor(R.color.status_green))
            else     -> Pair("Utanför schema – endast totallarm", getColor(R.color.status_orange))
        }

        binding.tvStatus.text = statusText
        binding.statusIndicator.setBackgroundColor(color)
    }

    private fun ensureServiceRunning() {
        val intent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_MONITOR
        }
        startForegroundService(intent)
    }

    private fun openBatteryOptimizationSettings() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        runCatching { startActivity(intent) }
            ?: run {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
    }

    private fun requestRequiredPermissions() {
        val needed = buildList {
            add(Manifest.permission.RECEIVE_SMS)
            add(Manifest.permission.READ_SMS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 100)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            val denied = permissions.zip(grantResults.toList())
                .filter { it.second != PackageManager.PERMISSION_GRANTED }
                .map { it.first }
            if (denied.isNotEmpty()) {
                Toast.makeText(
                    this,
                    "Saknade rättigheter: ${denied.joinToString()}\nAppen fungerar inte utan dessa.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
