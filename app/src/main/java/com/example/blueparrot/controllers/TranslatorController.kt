package com.example.blueparrot.controllers

import android.content.ContentValues
import android.content.Context
import android.util.Log
import com.example.blueparrot.services.SpeechActivationService
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions


class TranslatorController(private val sourceLanguage: String, private val targetLanguage: String) {
    val translationOptions: TranslatorOptions = TranslatorOptions.Builder()
        .setSourceLanguage(sourceLanguage)
        .setTargetLanguage(targetLanguage)
        .build()

    val translator = Translation.getClient(translationOptions)

    fun translate(text: String, context: Context?): String? {
        translator.translate(text)
            .addOnSuccessListener {
                return@addOnSuccessListener
            }
            .addOnFailureListener {
                Log.e(ContentValues.TAG, "ex.toString()")
            }
        return null
    }
}