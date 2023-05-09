package com.example.blueparrot

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.example.blueparrot.controller.LanguageManager
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

class TranslateFragment : Fragment() {
    private val TAG = "TranslateFragment"

    lateinit var edSource: EditText
    lateinit var edTarget: EditText
    lateinit var btnTranslate: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: chaged the initialization of the transalate obj. It cant be hardcoded


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_translate, container, false)

        edSource = view.findViewById(R.id.ed_source)
        edTarget = view.findViewById(R.id.ed_result)
        btnTranslate = view.findViewById(R.id.btn_translate)

        btnTranslate.setOnClickListener { translate() }

        return view
    }

    private fun createTranslator(source: String, target: String): Translator {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(target)
            .build()
        return Translation.getClient(options)
    }

    private fun translate() {
        if (edSource.text.toString().equals("")) return
        val translator = createTranslator(TranslateLanguage.SPANISH, TranslateLanguage.ENGLISH) // TODO CUIDAO AQUI!
        if (!isAvaliable(translator)) {
            val languageManager = LanguageManager()
            var isDownloadedSourceLanguage = languageManager.download(TranslateLanguage.SPANISH)
            var isDownloadedTargetLanguage = languageManager.download(TranslateLanguage.ENGLISH)
            if (!isDownloadedSourceLanguage && !isDownloadedTargetLanguage) return
        }
        translator.translate(edSource.text.toString())
            .addOnSuccessListener { translatedText ->
                Log.v(
                    TAG,
                    "Texto Source: " + edSource.text.toString() + "Texto Target: " + translatedText
                )
                edTarget.setText(translatedText)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to translate(): $exception")
            }

        translator.close() // Always closed when finish to use
    }

    private fun isAvaliable(languageModel: Translator): Boolean {
        var avaliable = false
        var conditions = DownloadConditions.Builder() // TODO: Conditions can be in another method...
            .requireWifi()
            .build()
        languageModel.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                // Model downloaded successfully. Okay to start translating.
                // (Set a flag, unhide the translation UI, etc.)
                Log.v(TAG, "Languages Downloaded") // TODO toast Required or dialog to confirm the download
                avaliable = true
            }
            .addOnFailureListener { exception ->
                // Model couldn’t be downloaded or other internal error.
                // ...
                Log.e(TAG, "Languages not donwloaded: $exception") // TODO toast Required or dialog to confirm the download
                avaliable = false
            }
        Log.v(TAG, "is avaliable the language model?: $avaliable")
        return avaliable
    }
}