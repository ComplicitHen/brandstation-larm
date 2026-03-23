package com.brandstation.larm

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.brandstation.larm.databinding.ActivitySettingsBinding

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
