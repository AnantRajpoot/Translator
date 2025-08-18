package com.devdroid.translator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(
    val listItems: MutableList<HistoryListItem>,
    private val onItemClick: (TranslationItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Define integer constants for our two view types
    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    // ViewHolder for the date headers
    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val headerTextView: TextView = view.findViewById(R.id.headerTextView)
    }

    // ViewHolder for the translation items
    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val sourceTextView: TextView = view.findViewById(R.id.sourceTextView)
        val translatedTextView: TextView = view.findViewById(R.id.translatedTextView)
        val languagePairTextView: TextView = view.findViewById(R.id.languagePairTextView)
    }

    // This function tells the adapter which type of view to create
    override fun getItemViewType(position: Int): Int {
        return when (listItems[position]) {
            is HistoryListItem.HeaderItem -> TYPE_HEADER
            is HistoryListItem.TranslationHistoryItem -> TYPE_ITEM
        }
    }

    // This function creates the correct ViewHolder based on the view type
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history, parent, false)
            ItemViewHolder(view)
        }
    }

    // This function binds the data to the correct ViewHolder
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = listItems[position]) {
            is HistoryListItem.HeaderItem -> {
                (holder as HeaderViewHolder).headerTextView.text = item.title
            }
            is HistoryListItem.TranslationHistoryItem -> {
                val itemViewHolder = holder as ItemViewHolder
                val translation = item.translation

                itemViewHolder.sourceTextView.text = translation.sourceText
                itemViewHolder.translatedTextView.text = translation.translatedText

                val langPair = "Detected (${translation.sourceLangCode.uppercase()}) → ${getLanguageName(translation.targetLangCode)} (${translation.targetLangCode.uppercase()})"
                itemViewHolder.languagePairTextView.text = langPair

                itemViewHolder.itemView.setOnClickListener {
                    onItemClick(translation)
                }
            }
        }
    }

    override fun getItemCount() = listItems.size

    // Helper function to get the full language name from its code
    private fun getLanguageName(langCode: String): String {
        return when (langCode) {
            "en" -> "English"
            "fr" -> "French"
            "de" -> "German"
            "es" -> "Spanish"
            "hi" -> "Hindi"
            "ja" -> "Japanese"
            "ru" -> "Russian"
            else -> "Unknown"
        }
    }
}