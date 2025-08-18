package com.devdroid.translator


// This sealed class will represent any possible item in our RecyclerView list.
sealed class HistoryListItem {
    // A data class for our date headers (e.g., "TODAY")
    data class HeaderItem(val title: String) : HistoryListItem()

    // A data class for our actual translation items
    data class TranslationHistoryItem(val translation: TranslationItem) : HistoryListItem()
}