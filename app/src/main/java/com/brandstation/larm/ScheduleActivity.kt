package com.brandstation.larm

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.brandstation.larm.databinding.ActivityScheduleBinding
import java.util.Locale

class ScheduleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScheduleBinding
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Joursschema"

        prefs = Prefs(this)

        loadSchedule()
        setupTimePickers()

        binding.btnSave.setOnClickListener { saveAndFinish() }
    }

    // bit 0=mån, 1=tis, 2=ons, 3=tor, 4=fre, 5=lör, 6=sön
    private val dayCheckboxes: List<Pair<CheckBox, Int>> by lazy {
        listOf(
            binding.checkMon to 0,
            binding.checkTue to 1,
            binding.checkWed to 2,
            binding.checkThu to 3,
            binding.checkFri to 4,
            binding.checkSat to 5,
            binding.checkSun to 6,
        )
    }

    private fun loadSchedule() {
        val mask = prefs.dutyDaysMask
        dayCheckboxes.forEach { (cb, bit) ->
            cb.isChecked = (mask shr bit) and 1 == 1
        }
        updateTimeText(binding.tvStartTime, prefs.dutyStartMinutes)
        updateTimeText(binding.tvEndTime, prefs.dutyEndMinutes)
    }

    private fun setupTimePickers() {
        binding.tvStartTime.setOnClickListener {
            showTimePicker(prefs.dutyStartMinutes) { mins ->
                prefs.dutyStartMinutes = mins
                updateTimeText(binding.tvStartTime, mins)
            }
        }
        binding.tvEndTime.setOnClickListener {
            showTimePicker(prefs.dutyEndMinutes) { mins ->
                prefs.dutyEndMinutes = mins
                updateTimeText(binding.tvEndTime, mins)
            }
        }
    }

    private fun showTimePicker(currentMinutes: Int, onSet: (Int) -> Unit) {
        TimePickerDialog(
            this,
            { _, h, m -> onSet(h * 60 + m) },
            currentMinutes / 60,
            currentMinutes % 60,
            true   // 24-timmarsformat
        ).show()
    }

    private fun updateTimeText(view: TextView, totalMinutes: Int) {
        view.text = String.format(Locale.getDefault(), "%02d:%02d",
            totalMinutes / 60, totalMinutes % 60)
    }

    private fun saveAndFinish() {
        var mask = 0
        dayCheckboxes.forEach { (cb, bit) ->
            if (cb.isChecked) mask = mask or (1 shl bit)
        }
        prefs.dutyDaysMask = mask
        // Uppdatera notisens statustext
        val intent = android.content.Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_MONITOR
        }
        startService(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
