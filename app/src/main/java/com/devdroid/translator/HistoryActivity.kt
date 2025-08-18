package com.devdroid.translator

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var historyAdapter: HistoryAdapter
    private var historyList = mutableListOf<HistoryListItem>()
    private lateinit var recyclerView: RecyclerView

    // ✅ NEW: Views for the new button and text
    private lateinit var clearHistoryButton: ExtendedFloatingActionButton
    private lateinit var swipeInstructionText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        db = AppDatabase.getDatabase(this)

        val toolbar: MaterialToolbar = findViewById(R.id.historyToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // ✅ Find the new views by their ID
        recyclerView = findViewById(R.id.historyRecyclerView)
        clearHistoryButton = findViewById(R.id.clearHistoryButton)
        swipeInstructionText = findViewById(R.id.swipeInstructionText)

        setupRecyclerView()

        // ✅ Set the click listener for the new button
        clearHistoryButton.setOnClickListener {
            showClearHistoryConfirmationDialog()
        }

        loadHistory()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(historyList) { selectedItem ->
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Translated Text", selectedItem.translatedText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this@HistoryActivity, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = historyAdapter
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        // Swipe-to-delete functionality remains the same
        val swipeToDeleteCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }
            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                if (viewHolder is HistoryAdapter.HeaderViewHolder) return 0
                return super.getMovementFlags(recyclerView, viewHolder)
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = historyAdapter.listItems[position] as HistoryListItem.TranslationHistoryItem

                lifecycleScope.launch { db.translationDao().deleteById(item.translation.id) }

                historyList.removeAt(position)
                historyAdapter.notifyItemRemoved(position)

                Snackbar.make(recyclerView, "Translation deleted", Snackbar.LENGTH_LONG)
                    .setAction("UNDO") {
                        lifecycleScope.launch {
                            db.translationDao().insert(item.translation)
                            loadHistory()
                        }
                    }.show()
            }
        }
        ItemTouchHelper(swipeToDeleteCallback).attachToRecyclerView(recyclerView)
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            val history = db.translationDao().getAll()

            // ✅ REMOVED the automatic dialog call from here

            // ✅ Manage visibility of the new views
            if (history.isEmpty()) {
                clearHistoryButton.visibility = View.GONE
                swipeInstructionText.visibility = View.GONE
            } else {
                clearHistoryButton.visibility = View.VISIBLE
                swipeInstructionText.visibility = View.VISIBLE
            }

            val groupedList = groupTranslationsByDate(history)
            historyList.clear()
            historyList.addAll(groupedList)
            historyAdapter.notifyDataSetChanged()
        }
    }

    private fun showClearHistoryConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear History?")
            .setMessage("Are you sure you want to delete all translations? This action cannot be undone.")
            .setPositiveButton("Clear") { _, _ ->
                clearHistory()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearHistory() {
        lifecycleScope.launch {
            db.translationDao().clearAll()
            runOnUiThread {
                historyList.clear()
                historyAdapter.notifyDataSetChanged()

                // ✅ Hide the button and text after clearing
                clearHistoryButton.visibility = View.GONE
                swipeInstructionText.visibility = View.GONE

                Toast.makeText(this@HistoryActivity, "History cleared", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // groupTranslationsByDate function remains exactly the same...
    private fun groupTranslationsByDate(translations: List<TranslationItem>): List<HistoryListItem> {

        if (translations.isEmpty()) return emptyList()

        val groupedList = mutableListOf<HistoryListItem>()

        var lastHeader = ""

        val todayCal = Calendar.getInstance()

        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(todayCal.time)

        val yesterdayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(yesterdayCal.time)



        for (translation in translations) {

            val itemCal = Calendar.getInstance().apply { timeInMillis = translation.timestamp }

            val itemDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(itemCal.time)



            val header = when (itemDate) {

                todayDate -> "TODAY"

                yesterdayDate -> "YESTERDAY"

                else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(itemCal.time).uppercase()

            }



            if (header != lastHeader) {

                groupedList.add(HistoryListItem.HeaderItem(header))

                lastHeader = header

            }

            groupedList.add(HistoryListItem.TranslationHistoryItem(translation))

        }

        return groupedList

    }
}