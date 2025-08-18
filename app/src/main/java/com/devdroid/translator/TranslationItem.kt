package com.devdroid.translator

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "translation_history")
data class TranslationItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceText: String,
    val translatedText: String,
    val sourceLangCode: String,
    val targetLangCode: String,
    val timestamp: Long = System.currentTimeMillis()
)