package com.brandstation.larm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.brandstation.larm.databinding.ActivityLogBinding

class LogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogBinding
    private lateinit var adapter: LogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Larmlogg"

        adapter = LogAdapter()
        binding.recyclerLog.layoutManager = LinearLayoutManager(this)
        binding.recyclerLog.adapter = adapter

        binding.btnClearLog.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Rensa logg")
                .setMessage("Ta bort alla larmhändelser i loggen?")
                .setPositiveButton("Rensa") { _, _ ->
                    AlarmLog.clear(this)
                    loadLog()
                }
                .setNegativeButton("Avbryt", null)
                .show()
        }

        loadLog()
    }

    override fun onResume() {
        super.onResume()
        loadLog()
    }

    private fun loadLog() {
        val entries = AlarmLog.getAll(this)
        adapter.setItems(entries)
        binding.tvEmptyLog.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerLog.visibility = if (entries.isEmpty()) View.GONE else View.VISIBLE
        binding.tvLogCount.text = "${entries.size} larm"

        val stats = AlarmLog.getStats(this)
        binding.tvStatsTotal.text = "Totalt: ${stats.total}"
        binding.tvStatsWeek.text = "Denna vecka: ${stats.thisWeek}"
        binding.tvStatsMonth.text = "Denna månad: ${stats.thisMonth}"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    inner class LogAdapter : RecyclerView.Adapter<LogAdapter.VH>() {

        private var items: List<AlarmEntry> = emptyList()

        fun setItems(list: List<AlarmEntry>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_log, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val entry = items[position]
            val typeLabel = when (entry.alarmType) {
                AlarmType.TOTAL -> "TOTALLARM"
                AlarmType.REGULAR -> "LARM"
            }
            val testTag = if (entry.wasTestMode) " [TESTLÄGE]" else ""
            holder.text1.text = "${AlarmLog.formatTime(entry.timestamp)}  •  $typeLabel$testTag"
            holder.text2.text = entry.message.take(120)

            // Färgad vänsterbåge beroende på larmtyp
            val barColor = when (entry.alarmType) {
                AlarmType.TOTAL -> 0xFFB71C1C.toInt()  // djupröd — TOTALLARM
                AlarmType.REGULAR -> 0xFFE65100.toInt() // orange — vanligt larm
            }
            holder.colorBar.setBackgroundColor(barColor)
        }

        override fun getItemCount() = items.size

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val text1: TextView = itemView.findViewById(R.id.text1)
            val text2: TextView = itemView.findViewById(R.id.text2)
            val colorBar: View = itemView.findViewById(R.id.logColorBar)
        }
    }
}
