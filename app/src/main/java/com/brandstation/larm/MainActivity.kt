package com.brandstation.larm

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.brandstation.larm.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Prefs
    private lateinit var scheduleManager: ScheduleManager

    companion object {
        private const val REQ_PICK_SOUND = 42
        private const val REQ_PICK_RINGTONE = 43
    }

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
        updateSoundButtonLabel()
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

        binding.btnLog.setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }

        binding.btnPickSound.setOnClickListener { pickSoundFile() }

        // Testknapp: Totallarm
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
            val intent = Intent(this, AlarmService::class.java)
            intent.setAction(AlarmService.ACTION_DISMISS)
            startService(intent)
            Toast.makeText(this, "Larm stängt av", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickSoundFile() {
        val items = arrayOf(
            "Systemlarm/ringtone",
            "Välj ljudfil",
            "Återställ standard (inbyggd signal)"
        )
        AlertDialog.Builder(this)
            .setTitle("Välj larmsignal")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        val existingUri = prefs.customSoundUri?.let { Uri.parse(it) }
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            if (existingUri != null) {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
                            }
                        }
                        startActivityForResult(intent, REQ_PICK_RINGTONE)
                    }
                    1 -> {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "audio/*"
                        }
                        startActivityForResult(intent, REQ_PICK_SOUND)
                    }
                    2 -> {
                        prefs.customSoundUri = null
                        updateSoundButtonLabel()
                        Toast.makeText(this, "Återställd till inbyggd signal", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when {
            requestCode == REQ_PICK_SOUND && resultCode == Activity.RESULT_OK -> {
                val uri: Uri = data?.data ?: return
                // Begär persistent läsrättighet så appen kommer åt filen även efter omstart
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                prefs.customSoundUri = uri.toString()
                updateSoundButtonLabel()
                Toast.makeText(this, "Larmsignal sparad", Toast.LENGTH_SHORT).show()
            }
            requestCode == REQ_PICK_RINGTONE && resultCode == Activity.RESULT_OK -> {
                val uri: Uri? = data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                prefs.customSoundUri = uri?.toString()
                updateSoundButtonLabel()
                Toast.makeText(this, "Larmsignal sparad", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSoundButtonLabel() {
        val custom = prefs.customSoundUri
        binding.btnPickSound.text = if (custom != null) {
            val name = Uri.parse(custom).lastPathSegment ?: "eget ljud"
            "Larmsignal: $name"
        } else {
            "Välj larmsignal  (standard: inbyggd)"
        }
    }

    private fun triggerTestAlarm(alarmType: AlarmType, message: String) {
        val intent = Intent(this, AlarmService::class.java)
        intent.setAction(AlarmService.ACTION_TRIGGER)
        intent.putExtra(AlarmService.EXTRA_ALARM_TYPE, alarmType.name)
        intent.putExtra(AlarmService.EXTRA_MESSAGE, message)
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

        // Feature 4: Nästa passvisning
        val nextDuty = scheduleManager.nextDutyStart()
        binding.tvNextDuty.text = if (nextDuty != null) "Nästa pass: $nextDuty" else ""
    }

    private fun updatePermissionBadge() {
        val hasSms = hasPermission(Manifest.permission.RECEIVE_SMS)
        val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        } else true
        val hasBattery = run {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(packageName)
        }

        val allOk = hasSms && hasNotif && hasBattery
        binding.btnPermissions.text = if (allOk) {
            "Behörigheter  ✓"
        } else {
            "Behörigheter  ⚠ KRÄVER ÅTGÄRD"
        }
    }

    private fun ensureServiceRunning() {
        val intent = Intent(this, AlarmService::class.java)
        intent.setAction(AlarmService.ACTION_START_MONITOR)
        startForegroundService(intent)
    }

    private fun requestRequiredPermissions() {
        val needed = buildList {
            add(Manifest.permission.RECEIVE_SMS)
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
