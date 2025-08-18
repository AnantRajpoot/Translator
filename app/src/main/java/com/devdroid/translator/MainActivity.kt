package com.devdroid.translator

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputLayout
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private val TAG = "TranslatorDebug"

    // Views
    private lateinit var targetLangAutoComplete: AutoCompleteTextView
    private lateinit var editText: EditText
    private lateinit var textView: TextView
    private lateinit var button: Button
    private lateinit var copyButton: ImageButton
    private lateinit var speakOutputButton: ImageButton

    // ML Kit & TTS
    private lateinit var translator: Translator
    private lateinit var languageIdentifier: LanguageIdentifier
    private lateinit var tts: TextToSpeech

    // Activity Result Launchers
    private lateinit var speechResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    // State variables
    private var currentSourceLangCode: String = ""
    private var currentTargetLangCode: String = ""

    // History Database-----
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initializing the database----
        db = AppDatabase.getDatabase(this)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // --- Initialize everything ---
        tts = TextToSpeech(this, this)
        languageIdentifier = LanguageIdentification.getClient()

        targetLangAutoComplete = findViewById(R.id.targetLangAutoComplete)
        editText = findViewById(R.id.editText)
        textView = findViewById(R.id.textView)
        button = findViewById(R.id.button)
        copyButton = findViewById(R.id.copyButton)
        speakOutputButton = findViewById(R.id.speakOutputButton)
        val textInputLayout: TextInputLayout = findViewById(R.id.textInputLayout)

        // --- Setup Dropdown Adapter ---
        val languages = resources.getStringArray(R.array.languages)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, languages)
        targetLangAutoComplete.setAdapter(adapter)

        // --- Setup Microphone Input ---
        setupMicrophoneInput()

        // --- Setup Button Click Listeners ---
        setupClickListeners(textInputLayout)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            Log.d(TAG, "onInit: TTS Engine Initialized SUCCESSFULLY.")
            speakOutputButton.isEnabled = true
        } else {
            Log.e(TAG, "onInit: TTS Engine Initialization FAILED! Status: $status")
            Toast.makeText(this, "TTS initialization failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupMicrophoneInput() {
        speechResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val speechResult = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!speechResult.isNullOrEmpty()) {
                    editText.setText(speechResult[0])
                }
            }
        }

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                launchSpeechRecognizer()
            } else {
                Toast.makeText(this, "Microphone permission is required for voice input.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners(textInputLayout: TextInputLayout) {
        textInputLayout.setEndIconOnClickListener {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> {
                    launchSpeechRecognizer()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }

        button.setOnClickListener {
            val stringToTranslate = editText.text.toString()
            val targetLanguage = targetLangAutoComplete.text.toString()

            if (stringToTranslate.isEmpty() || targetLanguage.isEmpty()) {
                Toast.makeText(this, "Please enter text and select a target language.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            speakOutputButton.visibility = View.GONE
            copyButton.visibility = View.GONE

            identifyLanguageAndTranslate(stringToTranslate, targetLanguage)
        }

        copyButton.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("label", textView.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        speakOutputButton.setOnClickListener {
            val text = textView.text.toString()
            if (text.isNotEmpty() && text != "Translated text will appear here" && currentTargetLangCode.isNotEmpty()) {
                speak(text, currentTargetLangCode)
            }
        }
    }

    private fun launchSpeechRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
        }
        try {
            speechResultLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Speech recognition is not available on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun speak(text: String, langCode: String) {
        try {
            val locale = Locale(langCode)
            if (tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) {
                tts.language = locale
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            } else {
                Toast.makeText(this, "TTS for this language is not installed.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "TTS for this language is not supported.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun identifyLanguageAndTranslate(text: String, targetLanguage: String) {
        // Toast.makeText(this, "Detecting language...", Toast.LENGTH_SHORT).show() // <-- UNNECESSARY TOAST REMOVED
        languageIdentifier.identifyLanguage(text)
            .addOnSuccessListener { languageCode ->
                if (languageCode != "und") {
                    val targetLangCode = getLanguageCode(targetLanguage)
                    translateText(languageCode, targetLangCode, text)
                } else {
                    Toast.makeText(this, "Could not detect the language of the text.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Language identification failed.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun translateText(sourceLangCode: String, targetLangCode: String, text: String) {
        this.currentSourceLangCode = sourceLangCode
        this.currentTargetLangCode = targetLangCode

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLangCode)
            .setTargetLanguage(targetLangCode)
            .build()
        translator = Translation.getClient(options)
        downloadModal(text)
    }

    private fun downloadModal(input: String) {
        val conditions = DownloadConditions.Builder().requireWifi().build()
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                // Toast.makeText(this@MainActivity, "Model ready, translating...", Toast.LENGTH_SHORT).show() // <-- UNNECESSARY TOAST REMOVED
                translateLanguage(input)
            }.addOnFailureListener { exception ->
                Toast.makeText(this@MainActivity, "Model download failed: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun translateLanguage(input: String) {
        translator.translate(input)
            .addOnSuccessListener { translatedText ->
                textView.text = translatedText

                // SAVE TO HISTORY DATABASE--------
                lifecycleScope.launch {
                    val item = TranslationItem(
                        sourceText = input,
                        translatedText = translatedText,
                        sourceLangCode = currentSourceLangCode,
                        targetLangCode = currentTargetLangCode
                    )
                    db.translationDao().insert(item)
                }
            }

            .addOnFailureListener { exception ->
                Toast.makeText(this@MainActivity, "Failed to translate: ${exception.message}", Toast.LENGTH_SHORT).show()
            }

        // SHOW BUTTONS WHEN TRANSLATION IS SUCCESSFUL
        speakOutputButton.visibility = View.VISIBLE
        copyButton.visibility = View.VISIBLE
    }

    private fun getLanguageCode(language: String): String {
        return when (language) {
            "English" -> TranslateLanguage.ENGLISH
            "French" -> TranslateLanguage.FRENCH
            "German" -> TranslateLanguage.GERMAN
            "Spanish" -> TranslateLanguage.SPANISH
            "Hindi" -> TranslateLanguage.HINDI
            "Japanese" -> TranslateLanguage.JAPANESE
            "Russian" -> TranslateLanguage.RUSSIAN
            else -> TranslateLanguage.ENGLISH
        }
    }

    // --- Menu and Model Download Functions ---
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_history -> {
                val intent = Intent(this, HistoryActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_download_language -> {
                showLanguageDownloadDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLanguageDownloadDialog() {
        val languages = resources.getStringArray(R.array.languages)
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select a language to download")
        builder.setItems(languages) { _, which ->
            val selectedLanguage = languages[which]
            downloadLanguageModel(selectedLanguage)
        }
        builder.create().show()
    }

    private fun downloadLanguageModel(language: String) {
        val languageCode = getLanguageCode(language)
        val model = TranslateRemoteModel.Builder(languageCode).build()
        val conditions = DownloadConditions.Builder().requireWifi().build()
        Toast.makeText(this, "Downloading $language model...", Toast.LENGTH_SHORT).show()
        RemoteModelManager.getInstance().download(model, conditions)
            .addOnSuccessListener {
                Toast.makeText(this, "$language model downloaded successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Download failed: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }
}