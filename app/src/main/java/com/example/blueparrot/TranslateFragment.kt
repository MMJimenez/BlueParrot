package com.example.blueparrot

import android.Manifest
import android.content.*
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.PermissionChecker
import androidx.core.content.PermissionChecker.checkCallingOrSelfPermission
import androidx.fragment.app.Fragment
import com.example.blueparrot.controllers.LanguageManager
import com.example.blueparrot.services.SpeechActivationService
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.HashMap

class TranslateFragment : Fragment(), TextToSpeech.OnInitListener {
    private val TAG = "TranslateFragment"

    private lateinit var edSource: EditText
    private lateinit var edTarget: EditText
    private lateinit var btnTranslate: Button
    private lateinit var btnRecognize: Button

    private var tts: TextToSpeech? = null
    var ttsEngine: HashMap<String, String>? = null

    private val serviceBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val number = intent.getIntExtra("Number_key", 0)
            val text = intent.getStringExtra("RECOGNIZE_SPEECH")
            if (text != null) {
                translateRecognition(text, edSource, edTarget)
            }
            Log.d("MyService", "onReceive text = $text")
        }
    }

    override fun onInit(status: Int) {
        TODO("Not yet implemented")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // register the receiver
        val intentFilter = IntentFilter("my_service_action")
        requireActivity().registerReceiver(serviceBroadcastReceiver, intentFilter)

        tts = TextToSpeech(context, this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_translate, container, false)

        edSource = view.findViewById(R.id.ed_source)
        edTarget = view.findViewById(R.id.ed_result)
        btnTranslate = view.findViewById(R.id.btn_translate)
        btnRecognize = view.findViewById(R.id.btn_recognize)

        btnTranslate.setOnClickListener {
            translateText(edSource, edTarget)
        }

        btnRecognize.setOnClickListener {
            startSpeechActivator()
        }

        requestRecordAudioPermission()

        return view
    }

    override fun onDestroy() {
        requireActivity().unregisterReceiver(serviceBroadcastReceiver)
        super.onDestroy()
    }

    private fun requestRecordAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val requiredPermission: String = Manifest.permission.RECORD_AUDIO

            // If the user previously denied this permission then show a message explaining why
            // this permission is needed
            if (checkCallingOrSelfPermission(requireContext(), requiredPermission) == PermissionChecker.PERMISSION_DENIED) {
                requestPermissions(arrayOf(requiredPermission), 101)
            }
        }
    }


    private fun createTranslator(source: String, target: String): Translator {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(target)
            .build()
        return Translation.getClient(options)
    }

    private fun translateText(sourceView: EditText, targetView: EditText) {
        if (TextUtils.isEmpty(sourceView.text.toString())) {
            Toast.makeText(context, "Ed Origin cant be empty", Toast.LENGTH_SHORT).show()
            return
        }

        var translationOptions = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.SPANISH)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()
        var translator = Translation.getClient(translationOptions)



        var sourceText = sourceView.text.toString()

//        var progressDialog = ProgressDialog(context)
//        progressDialog.setMessage("Downloading the translation model...")
//        progressDialog.setCancelable(false)
//        progressDialog.show()

        translator.downloadModelIfNeeded()
            .addOnSuccessListener {
                // progressDialog.dismiss()
            }
            .addOnFailureListener {
                Log.e(ContentValues.TAG, "ex.toString()")
            }

        Log.v(ContentValues.TAG, "Continuando...")
        var result: Task<String> = translator.translate(sourceText)
            .addOnSuccessListener {
                targetView.setText(it)
            }
            .addOnFailureListener {
                Log.e(ContentValues.TAG, "ex.toString()")
            }
    }

    private fun translateRecognition(heardText: String, sourceView: EditText, targetView: EditText) {
        sourceView.setText(heardText)
        translateText(sourceView, targetView)
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

    fun startSpeechActivator() {
        val i = SpeechActivationService.makeStartServiceIntent(context)
        requireContext().startService(i)
    }
}