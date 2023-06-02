package com.example.blueparrot.controllers

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.blueparrot.interfaces.SpeechActivationListener
import com.example.blueparrot.interfaces.SpeechActivator
import java.util.*
import kotlin.math.log


/**
 * https://github.com/gast-lib/gast-lib/blob/master/library/src/root/gast/speech/activation/WordActivator.java
 * Uses direct speech recognition to activate when the user speaks
 * one of the target words
 * @author Greg Milette &#60;[gregorym@gmail.com](mailto:gregorym@gmail.com)&#62;
 */
class WordActivator(
    private val context: Context,
    private val resultListener: SpeechActivationListener
) :
    SpeechActivator, RecognitionListener {
    private var recognizer: SpeechRecognizer? = null

    private val TAG = "WordActivator"

    override fun detectActivation() {
        recognizeSpeechDirectly()
    }

    private fun recognizeSpeechDirectly() {
        Log.d(TAG, "recognizeSpeechDirectly: $languageToRecognizer")
        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        recognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageToRecognizer)
        // accept partial results if they come
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        startRecognize(context, recognizerIntent, this, speechRecognizer)

        //need to have a calling package for it to work
        if (!recognizerIntent.hasExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE)) {
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "com.dummy")
        }
        recognizer!!.setRecognitionListener(this)
        recognizer!!.startListening(recognizerIntent)
    }

    override fun stop() {
        if (speechRecognizer != null) {
            speechRecognizer!!.stopListening()
            speechRecognizer!!.cancel()
            speechRecognizer!!.destroy()
        }
    }

    override fun onResults(results: Bundle) {
        Log.d(TAG, "full results")
        receiveResults(results)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        // not used
    }

    /**
     * common method to process any results bundle from [SpeechRecognizer]
     */
    private fun receiveResults(results: Bundle?) {
        if (results != null
            && results.containsKey(SpeechRecognizer.RESULTS_RECOGNITION)
        ) {
            val heard: List<String>? =
                results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val scores = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
            receiveWhatWasHeard(heard, scores)
        } else {
            Log.d(TAG, "no results")
        }
    }

    private fun receiveWhatWasHeard(heard: List<String>?, scores: FloatArray?) {
        val heardTargetWord = false
        // find the target word
        Log.i(TAG, "Listened " + heard.toString())

        if (heard != null) {
            stop()
            resultListener.onResult(heard)
            resultListener.activated(true)
        }
    }


    override fun onError(errorCode: Int) {
        if (errorCode == SpeechRecognizer.ERROR_NO_MATCH
            || errorCode == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
        ) {
            Log.d(TAG, "didn't recognize anything")
            // keep going
            recognizeSpeechDirectly()
        } else {
            Log.d(TAG, "FAILED errorCode: $errorCode")
        }
    }

    /**
     * lazy initialize the speech recognizer
     */
    private val speechRecognizer: SpeechRecognizer?
        private get() {
            if (recognizer == null) {
                recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            }
            return recognizer
        }

    // other unused methods from RecognitionListener...
    override fun onReadyForSpeech(params: Bundle) {
        Log.d(TAG, "ready for speech $params")
    }

    override fun onEndOfSpeech() {}

    /**
     * @see android.speech.RecognitionListener.onBeginningOfSpeech
     */
    override fun onBeginningOfSpeech() {}
    override fun onBufferReceived(buffer: ByteArray) {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onEvent(eventType: Int, params: Bundle) {}

    companion object {
        private const val TAG = "WordActivator"
        var languageToRecognizer = "es-ES"
        
        fun startRecognize(
            context: Context?,
            recognizerIntent: Intent, listener: RecognitionListener?,
            recognizer: SpeechRecognizer?
        ) {
            //need to have a calling package for it to work
            if (!recognizerIntent.hasExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE)) {
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "com.dummy")
            }
            recognizer!!.setRecognitionListener(listener)
            recognizer.startListening(recognizerIntent)
        }
    }
}