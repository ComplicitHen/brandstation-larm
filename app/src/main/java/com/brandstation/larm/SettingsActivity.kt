package com.brandstation.larm

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.brandstation.larm.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: Prefs

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
            Toast.makeText(this, "Inställningar sparade", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
