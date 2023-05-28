package com.example.blueparrot

import android.Manifest
import android.content.*
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.PermissionChecker
import androidx.core.content.PermissionChecker.checkCallingOrSelfPermission
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.blueparrot.controllers.AvailableLanguages
import com.example.blueparrot.services.SpeechActivationService
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.*


class TranslateFragment : Fragment(), TextToSpeech.OnInitListener {
    private val TAG = "TranslateFragment"

    private var isConversationOn = false

    private lateinit var edSource: EditText
    private lateinit var edTarget: EditText
    private lateinit var btnTranslate: Button
    private lateinit var btnRecognize: ImageButton
    private lateinit var btnConversation: ImageButton

    private lateinit var edSourceLanguage: AutoCompleteTextView
    private lateinit var edTargetLanguage: AutoCompleteTextView

    private var sourceLocale = Locale("es", "ES") // TODO no hardcode
    private var targetLocale = Locale.ENGLISH // TODO no hardcode

    // This variables are the Strings of the Locales
//    private var sourceLanguage = "SPANISH" // TODO no hardcode,
//    private var targetLanguage = "ENGLISH"

    private var tts: TextToSpeech? = null
    var ttsEngine: HashMap<String, String>? = null
    private var isInit = false

    private val serviceBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val number = intent.getIntExtra("Number_key", 0)
            val text = intent.getStringExtra("RECOGNIZE_SPEECH")
            if (text != null) {
                tts!!.language = targetLocale
                translateRecognition(text, edSource, edTarget)
            }
            Log.d("MyService", "onReceive text = $text")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Set language
            val result = tts!!.setLanguage(targetLocale)
            Log.d(TAG, "onInit: $targetLocale")
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsEngine = HashMap()
                ttsEngine!![TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] =
                    SpeechActivationService.UTTERANCE_ID
                tts!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {

                    override fun onStart(utteranceId: String) {}

                    override fun onDone(utteranceId: String) {
                        if (utteranceId == SpeechActivationService.UTTERANCE_ID) {
                            if (isConversationOn) {
                                switchLanguages()
                                tts!!.language = targetLocale
                                startSpeechRecognition()
                                Log.d(TAG, "onDone: $targetLocale")
                            }
                        }
                    }

                    override fun onError(utteranceId: String) {}
                })
                isInit = true
            }
        }
    }

    private fun speak(word: String) {
        val speakConfirmationParam = ttsEngine
        if (tts != null && isInit) {
            tts!!.speak(word, TextToSpeech.QUEUE_FLUSH, speakConfirmationParam)
        }
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
        btnConversation = view.findViewById(R.id.btn_conversation)

        btnTranslate.setOnClickListener {
            translateText(edSource, edTarget)
        }

        btnRecognize.setOnClickListener {
            startSpeechRecognition()
        }

        btnConversation.setOnClickListener {
            if (!isConversationOn) {
                btnConversation.setImageResource(R.drawable.ic_cancel_conversation_)
                isConversationOn = true
                startSpeechRecognition()
                enableButtons(false, false)
            } else {
                btnConversation.setImageResource(R.drawable.ic_conversation_white_)
                isConversationOn = false
                enableButtons()
            }
            Log.d(TAG, "Estado es: $isConversationOn")
        }

        requestRecordAudioPermission()

        val languageList = AvailableLanguages.defaultLanguagesList(resources)

        val adapterLanguage: ArrayAdapter<String> =
            ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                languageList
            )

        edSourceLanguage = view.findViewById(R.id.ed_source_language)
        edSourceLanguage.setAdapter(adapterLanguage)
        edSourceLanguage.setText(languageList[0])

        edTargetLanguage = view.findViewById(R.id.ed_target_language)
        edTargetLanguage.setAdapter(adapterLanguage)
        edTargetLanguage.setText(languageList[1])

        return view
    }

    override fun onDestroy() {
        requireActivity().unregisterReceiver(serviceBroadcastReceiver)
        super.onDestroy()
    }

    private fun enableButtons(
        btnTranslateEnable: Boolean = true,
        btnRecognizeEnable: Boolean = true,
        btnConversationEnable: Boolean = true
    ) {
        btnTranslate.isEnabled = btnTranslateEnable
        btnTranslate.isVisible = btnTranslateEnable

        btnRecognize.isEnabled = btnRecognizeEnable
        btnRecognize.isVisible = btnRecognizeEnable

        btnConversation.isEnabled = btnConversationEnable
        btnConversation.isVisible = btnConversationEnable
    }

    private fun requestRecordAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val requiredPermission: String = Manifest.permission.RECORD_AUDIO

            // If the user previously denied this permission then show a message explaining why
            // this permission is needed
            if (checkCallingOrSelfPermission(
                    requireContext(),
                    requiredPermission
                ) == PermissionChecker.PERMISSION_DENIED
            ) {
                requestPermissions(arrayOf(requiredPermission), 101)
            }
        }
    }

    private fun setLocalesFromAutoComplete() {
        sourceLocale = stringLanguageToLocale(edSourceLanguage.text.toString())!!
        targetLocale = stringLanguageToLocale(edTargetLanguage.text.toString())!!
    }

    private fun switchLanguages() {
        val tempLanguage = sourceLocale
        sourceLocale = targetLocale
        targetLocale = tempLanguage
        Log.d(TAG, "sourceLocale: $sourceLocale")
        Log.d(TAG, "targetLocale: $targetLocale")

//        sourceLanguage = localeToLanguageModel(sourceLocale)!! // TODO No dejar estos unwrappers
//        targetLanguage = localeToLanguageModel(targetLocale)!! // TODO No dejar estos unwrappers
    }

    private fun localeToLanguageModel(locale: Locale): String? {
        when (locale.language) {
            Locale("es", "ES").language -> return resources.getString(R.string.spanish)
            Locale.ENGLISH.language -> return resources.getString(R.string.english)
            Locale.FRANCE.language -> return resources.getString(R.string.french)
            Locale.GERMAN.language -> return resources.getString(R.string.german)
        }
        return null
    }

    private fun stringLanguageToLocale(language: String): Locale? {
        when (language) {
            resources.getString(R.string.spanish) -> return Locale("es", "ES")
            resources.getString(R.string.english) -> return Locale.ENGLISH
            resources.getString(R.string.french) -> return Locale.FRANCE
            resources.getString(R.string.german) -> return Locale.GERMAN
        }
        return null
    }


//    private fun createTranslator(source: String, target: String): Translator {
//        val options = TranslatorOptions.Builder()
//            .setSourceLanguage(source)
//            .setTargetLanguage(target)
//            .build()
//        return Translation.getClient(options)
//    }

    private fun translateText(sourceView: EditText, targetView: EditText, withTTS: Boolean = true) {
        setLocalesFromAutoComplete()

        if (TextUtils.isEmpty(sourceView.text.toString())) {
            Toast.makeText(context, "Ed Origin cant be empty", Toast.LENGTH_SHORT).show()
            return
        }

        var translationOptions = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLocale.language)
            .setTargetLanguage(targetLocale.language)
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
                if (withTTS) speak(it)
            }
            .addOnFailureListener {
                Log.e(ContentValues.TAG, "ex.toString()")
            }
    }

    private fun translateRecognition(
        heardText: String,
        sourceView: EditText,
        targetView: EditText
    ) {
        sourceView.setText(heardText)
        translateText(sourceView, targetView)
    }

    private fun isAvaliable(languageModel: Translator): Boolean {
        var avaliable = false
        var conditions =
            DownloadConditions.Builder() // TODO: Conditions can be in another method...
                .requireWifi()
                .build()
        languageModel.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                // Model downloaded successfully. Okay to start translating.
                // (Set a flag, unhide the translation UI, etc.)
                Log.v(
                    TAG,
                    "Languages Downloaded"
                ) // TODO toast Required or dialog to confirm the download
                avaliable = true
            }
            .addOnFailureListener { exception ->
                // Model couldn’t be downloaded or other internal error.
                // ...
                Log.e(
                    TAG,
                    "Languages not donwloaded: $exception"
                ) // TODO toast Required or dialog to confirm the download
                avaliable = false
            }
        Log.v(TAG, "is avaliable the language model?: $avaliable")
        return avaliable
    }

    fun startSpeechRecognition() {
        setLocalesFromAutoComplete()
        val i = SpeechActivationService.makeStartServiceIntent(context)
        requireContext().startService(i)
    }
}