package com.brandstation.larm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
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
        updatePermissionBadge()
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

        binding.btnPermissions.setOnClickListener {
            startActivity(Intent(this, PermissionsActivity::class.java))
        }

        // Testknapp: Totallarm — triggar direkt, testar larm + vibration
        binding.btnTestTotal.setOnClickListener {
            triggerTestAlarm(AlarmType.TOTAL,
                "Larminformation från VRR Ledningscentral\nTOTALLARM - Fri inryckning\nTID : TESTLARM")
        }

        // Testknapp: Vanligt larm
        binding.btnTestRegular.setOnClickListener {
            triggerTestAlarm(AlarmType.REGULAR,
                "Larminformation från VRR Ledningscentral\nLARM Mölnlycke RIB\nTID : TESTLARM")
        }

        // Stäng av pågående larm
        binding.btnStopAlarm.setOnClickListener {
            val intent = Intent(this, AlarmService::class.java).apply {
                action = AlarmService.ACTION_DISMISS
            }
            startService(intent)
            Toast.makeText(this, "Larm stängt av", Toast.LENGTH_SHORT).show()
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
        val testMode = prefs.smsTestMode

        val statusText = when {
            !enabled -> "Inaktiverad"
            testMode && onDuty -> "SMS-TESTLÄGE PÅ – på pass"
            testMode -> "SMS-TESTLÄGE PÅ – alla avsändare accepteras"
            onDuty   -> "På pass – alla larm aktiva"
            else     -> "Utanför schema – endast totallarm"
        }

        val color = when {
            !enabled -> getColor(R.color.status_gray)
            testMode -> getColor(android.R.color.holo_blue_dark)
            onDuty   -> getColor(R.color.status_green)
            else     -> getColor(R.color.status_orange)
        }

        binding.tvStatus.text = statusText
        binding.statusIndicator.setBackgroundColor(color)
    }

    private fun updatePermissionBadge() {
        val hasSms = hasPermission(Manifest.permission.RECEIVE_SMS)
        val hasReadSms = hasPermission(Manifest.permission.READ_SMS)
        val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        } else true
        val hasBattery = run {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(packageName)
        }

        val allOk = hasSms && hasReadSms && hasNotif && hasBattery
        binding.btnPermissions.text = if (allOk) {
            "Behörigheter  ✓"
        } else {
            "Behörigheter  ⚠ KRÄVER ÅTGÄRD"
        }
    }

    private fun ensureServiceRunning() {
        val intent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_MONITOR
        }
        startForegroundService(intent)
    }

    private fun requestRequiredPermissions() {
        val needed = buildList {
            add(Manifest.permission.RECEIVE_SMS)
            add(Manifest.permission.READ_SMS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.filter { !hasPermission(it) }

        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 100)
        }
    }

    private fun hasPermission(perm: String) =
        ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) updatePermissionBadge()
    }
}
