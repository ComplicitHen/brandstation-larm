package com.brandstation.larm

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.brandstation.larm.databinding.ActivitySettingsBinding
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: Prefs

    companion object {
        private const val REQ_IMPORT_FILE = 55
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Inställningar"

        prefs = Prefs(this)

        binding.etSenderNumber.setText(prefs.senderNumber)
        binding.etTotalAlarmKeyword.setText(prefs.totalAlarmKeyword)
        binding.etAlarmKeyword.setText(prefs.alarmKeyword)
        binding.switchTotalAlarm.isChecked = prefs.totalAlarmEnabled
        binding.switchSmsTestMode.isChecked = prefs.smsTestMode
        binding.switchVibrationOnly.isChecked = prefs.vibrationOnly
        binding.switchUseTts.isChecked = prefs.useTts

        // Feature: Auto-dismiss
        binding.etAutoDismissMinutes.setText(prefs.autoDismissMinutes.toString())

        // Feature: Ficklampa strobe
        binding.switchFlashlightStrobe.isChecked = prefs.flashlightStrobe

        // Feature: Quiet hours
        binding.switchQuietHours.isChecked = prefs.quietHoursEnabled
        binding.layoutQuietHoursTimes.visibility =
            if (prefs.quietHoursEnabled) View.VISIBLE else View.GONE
        updateQuietTimeText(binding.tvQuietStart, prefs.quietStartMinutes)
        updateQuietTimeText(binding.tvQuietEnd, prefs.quietEndMinutes)

        binding.switchQuietHours.setOnCheckedChangeListener { _, checked ->
            prefs.quietHoursEnabled = checked
            binding.layoutQuietHoursTimes.visibility = if (checked) View.VISIBLE else View.GONE
        }

        binding.tvQuietStart.setOnClickListener {
            showTimePicker(prefs.quietStartMinutes) { mins ->
                prefs.quietStartMinutes = mins
                updateQuietTimeText(binding.tvQuietStart, mins)
            }
        }

        binding.tvQuietEnd.setOnClickListener {
            showTimePicker(prefs.quietEndMinutes) { mins ->
                prefs.quietEndMinutes = mins
                updateQuietTimeText(binding.tvQuietEnd, mins)
            }
        }

        // Geo-filter
        binding.switchGeoFilter.isChecked = prefs.geoFilterEnabled
        binding.layoutGeoSettings.visibility = if (prefs.geoFilterEnabled) View.VISIBLE else View.GONE
        binding.etStationLat.setText(prefs.stationLat.toString())
        binding.etStationLng.setText(prefs.stationLng.toString())
        binding.etGeoRadius.setText(prefs.geoRadiusKm.toString())

        binding.switchGeoFilter.setOnCheckedChangeListener { _, checked ->
            prefs.geoFilterEnabled = checked
            binding.layoutGeoSettings.visibility = if (checked) View.VISIBLE else View.GONE
            if (checked) {
                LocationWorker.schedule(this)
                Toast.makeText(this, "Platsfilter aktiverat — uppdaterar position var 30:e min", Toast.LENGTH_SHORT).show()
            } else {
                LocationWorker.cancel(this)
            }
        }

        binding.switchSmsTestMode.setOnCheckedChangeListener { _, checked ->
            prefs.smsTestMode = checked
            if (checked) {
                Toast.makeText(
                    this,
                    "SMS-testläge PÅ — appen triggar nu på SMS från alla avsändare!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        binding.switchVibrationOnly.setOnCheckedChangeListener { _, checked ->
            prefs.vibrationOnly = checked
            if (checked && prefs.useTts) {
                prefs.useTts = false
                binding.switchUseTts.isChecked = false
                Toast.makeText(this, "Vibration-only aktiverat, TTS inaktiverat", Toast.LENGTH_SHORT).show()
            }
        }

        binding.switchUseTts.setOnCheckedChangeListener { _, checked ->
            prefs.useTts = checked
            if (checked && prefs.vibrationOnly) {
                prefs.vibrationOnly = false
                binding.switchVibrationOnly.isChecked = false
                Toast.makeText(this, "TTS aktiverat, vibration-only inaktiverat", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSave.setOnClickListener {
            val sender = binding.etSenderNumber.text.toString().trim()
            val totalKw = binding.etTotalAlarmKeyword.text.toString().trim()
            val alarmKw = binding.etAlarmKeyword.text.toString().trim()

            if (sender.isEmpty()) { binding.etSenderNumber.error = "Fyll i avsändarnummer"; return@setOnClickListener }
            if (totalKw.isEmpty()) { binding.etTotalAlarmKeyword.error = "Fyll i nyckelord"; return@setOnClickListener }
            if (alarmKw.isEmpty()) { binding.etAlarmKeyword.error = "Fyll i nyckelord"; return@setOnClickListener }

            prefs.senderNumber = sender
            prefs.totalAlarmKeyword = totalKw
            prefs.alarmKeyword = alarmKw
            prefs.totalAlarmEnabled = binding.switchTotalAlarm.isChecked

            // Feature: Auto-dismiss
            val autoDismissText = binding.etAutoDismissMinutes.text.toString().trim()
            prefs.autoDismissMinutes = autoDismissText.toIntOrNull() ?: 10

            // Feature: Ficklampa strobe
            prefs.flashlightStrobe = binding.switchFlashlightStrobe.isChecked

            // Geo-filter
            prefs.geoFilterEnabled = binding.switchGeoFilter.isChecked
            binding.etStationLat.text.toString().toDoubleOrNull()?.let { prefs.stationLat = it }
            binding.etStationLng.text.toString().toDoubleOrNull()?.let { prefs.stationLng = it }
            binding.etGeoRadius.text.toString().toIntOrNull()?.let { prefs.geoRadiusKm = it }

            Toast.makeText(this, "Inställningar sparade", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Feature 7: Exportera inställningar
        binding.btnExportSettings.setOnClickListener {
            val json = SettingsIO.export(this)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Brandstation Larm – inställningar")
                putExtra(Intent.EXTRA_TEXT, json)
            }
            startActivity(Intent.createChooser(shareIntent, "Exportera inställningar"))
        }

        // Feature 7: Importera inställningar
        binding.btnImportSettings.setOnClickListener {
            showImportDialog()
        }
    }

    private fun showTimePicker(currentMinutes: Int, onSet: (Int) -> Unit) {
        TimePickerDialog(
            this,
            { _, h, m -> onSet(h * 60 + m) },
            currentMinutes / 60,
            currentMinutes % 60,
            true
        ).show()
    }

    private fun updateQuietTimeText(view: TextView, totalMinutes: Int) {
        view.text = String.format(Locale.getDefault(), "%02d:%02d",
            totalMinutes / 60, totalMinutes % 60)
    }

    // Feature 7: Visa dialog med EditText för inklistrad JSON
    private fun showImportDialog() {
        val et = EditText(this).apply {
            hint = "Klistra in JSON här"
            minLines = 5
            setPadding(32, 16, 32, 16)
        }
        AlertDialog.Builder(this)
            .setTitle("Importera inställningar")
            .setMessage("Klistra in exporterad JSON nedan:")
            .setView(et)
            .setPositiveButton("Importera") { _, _ ->
                val text = et.text.toString().trim()
                if (text.isEmpty()) {
                    Toast.makeText(this, "Ingen JSON angiven", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                try {
                    SettingsIO.import(this, text)
                    // Uppdatera fälten med nya värden
                    binding.etSenderNumber.setText(prefs.senderNumber)
                    binding.etTotalAlarmKeyword.setText(prefs.totalAlarmKeyword)
                    binding.etAlarmKeyword.setText(prefs.alarmKeyword)
                    binding.switchTotalAlarm.isChecked = prefs.totalAlarmEnabled
                    binding.switchSmsTestMode.isChecked = prefs.smsTestMode
                    binding.switchVibrationOnly.isChecked = prefs.vibrationOnly
                    binding.switchUseTts.isChecked = prefs.useTts
                    binding.etAutoDismissMinutes.setText(prefs.autoDismissMinutes.toString())
                    binding.switchFlashlightStrobe.isChecked = prefs.flashlightStrobe
                    binding.switchQuietHours.isChecked = prefs.quietHoursEnabled
                    binding.layoutQuietHoursTimes.visibility =
                        if (prefs.quietHoursEnabled) View.VISIBLE else View.GONE
                    updateQuietTimeText(binding.tvQuietStart, prefs.quietStartMinutes)
                    updateQuietTimeText(binding.tvQuietEnd, prefs.quietEndMinutes)
                    Toast.makeText(this, "Inställningar importerade", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Ogiltig JSON: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Avbryt", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
