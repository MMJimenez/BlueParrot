package com.example.blueparrot.controllers

import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel

class LanguageManager() {
    final val TAG = "LanguageManager"
    val modelManager = RemoteModelManager.getInstance()

    fun getDownloaded(): Boolean {
        TODO("I dont know how to implement yet")
    }

    fun deleteDownloaded(translateLanguage: String): Boolean {
        var isDeleted = false
        val languageModel = TranslateRemoteModel.Builder(translateLanguage).build()
        modelManager.deleteDownloadedModel(languageModel)
            .addOnSuccessListener {
                // Model deleted.
                isDeleted = true // TODO handle with toast
                Log.v(TAG, "Model $translateLanguage deleted")
            }
            .addOnFailureListener {
                // Error.
                Log.e(TAG, it.toString()) // TODO handle with toast
            }
        return isDeleted
    }

    fun download(translateLanguage: String): Boolean {
        var isDownloaded = false
        val languageModel = TranslateRemoteModel.Builder(translateLanguage).build()
        val conditions = DownloadConditions.Builder() // TODO make a way to configure in a menu
            .requireWifi()
            .build()
        modelManager.download(languageModel, conditions)
            .addOnSuccessListener {
                // Model downloaded.
                isDownloaded = true // TODO handle with toast
                Log.d(TAG, "Model $translateLanguage downloaded")
            }
            .addOnFailureListener {
                // Error.
                Log.e(TAG, it.toString()) // TODO handle with toast
            }
        return isDownloaded
    }
}