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

        binding.btnSave.setOnClickListener {
            val sender = binding.etSenderNumber.text.toString().trim()
            val keyword = binding.etTotalAlarmKeyword.text.toString().trim()

            if (sender.isEmpty()) {
                binding.etSenderNumber.error = "Fyll i avsändarnummer"
                return@setOnClickListener
            }
            if (keyword.isEmpty()) {
                binding.etTotalAlarmKeyword.error = "Fyll i nyckelord"
                return@setOnClickListener
            }

            prefs.senderNumber = sender
            prefs.totalAlarmKeyword = keyword
            Toast.makeText(this, "Inställningar sparade", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
