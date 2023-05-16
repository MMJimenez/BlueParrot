package com.example.blueparrot.services

import android.app.*
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.blueparrot.R
import com.example.blueparrot.controllers.WordActivator
import com.example.blueparrot.interfaces.SpeechActivationListener
import com.example.blueparrot.interfaces.SpeechActivator
import java.util.*


class SpeechActivationService : Service(),
    SpeechActivationListener, OnInitListener {
    private var isStarted = false
    private var activator: SpeechActivator? = null
    private var startIntent: Intent? = null
    private var tts: TextToSpeech? = null
    private var isInit = false
    private var handler: Handler? = null
    var ttsEngine: HashMap<String, String>? = null
    override fun onCreate() {
        super.onCreate()
        isStarted = false
        tts = TextToSpeech(applicationContext, this)
        handler = Handler()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts!!.setLanguage(Locale("es", "ES"))
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsEngine = HashMap()
                ttsEngine!![TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = UTTERANCE_ID
                tts!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String) {}
                    override fun onDone(utteranceId: String) {
                        if (utteranceId == UTTERANCE_ID) {
                            Log.d(TAG, "entra en onDone")
                            startDetecting(startIntent, true)
                        }
                    }

                    override fun onError(utteranceId: String) {}
                })
                isInit = true
            }
        }
    }

    /**
     * stop or start an activator based on the activator type and if an
     * activator is currently running
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        handler!!.removeCallbacksAndMessages(null)
        if (intent != null) {
            startIntent = intent
            if (intent.hasExtra(ACTIVATION_STOP_INTENT_KEY)) {
                Log.d(TAG, "stop service intent")
                activated(false)
            } else {
                if (isStarted) {
                    // the activator is currently started
                    // if the intent is requesting a new activator
                    // stop the current activator and start
                    // the new one
                    if (isDifferentType(intent)) {
                        Log.d(TAG, "is different type")
                        stopActivator()
                        startDetecting(intent)
                    } else {
                        Log.d(TAG, "already started this type")
                    }
                } else {
                    // activator not started, start it
                    startDetecting(intent)
                }
            }
        }

        // restart in case the Service gets canceled
        return START_REDELIVER_INTENT
    }

    private fun startDetecting(intent: Intent?) {
        var extras = "null extras"
        if (intent!!.extras != null) {
            extras = intent.extras.toString()
        }
        if (activator == null) {
            Log.d(TAG, "null activator")
            activator = requestedActivator
        }
        isStarted = true
        activator.let{ it?.detectActivation() }
        startForeground(NOTIFICATION_ID, getNotification(intent))
    }

    // execute startDetecting in Main thread from other threads
    // startDetecting only can launch from Main thread
    private fun startDetecting(intent: Intent?, fromOtherThread: Boolean) {
        if (fromOtherThread) {
            val mainHandler = Handler(Looper.getMainLooper())
            val myRunnable = Runnable { startDetecting(startIntent) }
            mainHandler.post(myRunnable)
        }
    }

    private val requestedActivator: SpeechActivator
        private get() = WordActivator(this, this, "hello")

    /**
     * determine if the intent contains an activator type
     * that is different than the currently running type
     */
//    private fun isDifferentType(intent: Intent): Boolean {
//        var different = false
//        different = if (activator == null) {
//            return true
//        } else {
//            val possibleOther: SpeechActivator = requestedActivator
//            !possibleOther.getClass().getName().equals(activator.getClass().getName())
//        }
//        return different
//    }

    private fun isDifferentType(intent: Intent): Boolean {
        var different = false
        if (activator == null) {
            return true
        } else {
            val possibleOther: SpeechActivator = requestedActivator
            different = possibleOther.javaClass.name != activator!!.javaClass.name
        }
        return different
    }

    private fun speak(word: String, withConfirmation: Boolean = false) {
        val speakConfirmationParam = if (withConfirmation) ttsEngine else null
        if (tts != null && isInit) {
            tts!!.speak(word, TextToSpeech.QUEUE_FLUSH, speakConfirmationParam)
        }
    }

    override fun activated(success: Boolean) {
        // make sure the activator is stopped before doing anything else
        stopActivator()

        // broadcast result
        val intent = Intent(ACTIVATION_RESULT_BROADCAST_NAME)
        intent.putExtra(ACTIVATION_RESULT_INTENT_KEY, success)
        sendBroadcast(intent)

        // always stop after receive an activation
        stopSelf()
    }

    fun activatedAndTtsConfirmation(text: String) {
        // make sure the activator is stopped before doing anything else
        stopActivator()
        if (isInit) {
            speak(text, true)
        }

        // broadcast result
        val intent = Intent(ACTIVATION_RESULT_BROADCAST_NAME)
        intent.putExtra(ACTIVATION_RESULT_INTENT_KEY, true)
        sendBroadcast(intent)
        stopActivator()

//        if (startIntent != null) {
//            handler.postDelayed(new Runnable() {
//
//                @Override
//                public void run() {
//                    startDetecting(startIntent);
//                }
//            }, 3 * 1000);
//
//        }
    }

    fun activatedAndTts(text: String) {
        // make sure the activator is stopped before doing anything else
        stopActivator()
        if (isInit) {
            speak(text)
        }

        // broadcast result
        val intent = Intent(ACTIVATION_RESULT_BROADCAST_NAME)
        intent.putExtra(ACTIVATION_RESULT_INTENT_KEY, true)
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        Log.d(TAG, "On destroy")
        super.onDestroy()
        stopActivator()
        stopForeground(true)
    }

    private fun stopActivator() {
        if (activator != null) {
            Log.d(TAG, "stopped: " + activator!!.javaClass.name)
            activator!!.stop()
            isStarted = false
        }
    }

    private fun getNotification(intent: Intent?): Notification {
        val name = "Di el número y \"ordinarias\" o \"parar\""
        val title = "Reconocimiento Voz Activo"
        var channelIDBuildVersion = ""
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelIDBuildVersion =
                createNotificationChannel("my_service", "My Background Service")
        }
        val pi = PendingIntent.getService(
            this,
            0,
            makeServiceStopIntent(this),
            FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(
            this,
            channelIDBuildVersion
        )
            .setWhen(System.currentTimeMillis()).setTicker(title)
            .setSmallIcon(R.drawable.ic_mic_white)
            .setContentIntent(pi)
            .setAutoCancel(false)
            .setContentTitle(title)
            .setContentText(name)
            .setOngoing(true)
            .setNumber(100)
            .build()
    }

    private fun createNotificationChannel(channelID: String, channelName: String): String {
        val notificationChannel: NotificationChannel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel =
                NotificationChannel(channelID, channelName, NotificationManager.IMPORTANCE_NONE)
            notificationChannel.lightColor = Color.BLUE
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val service = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            service.createNotificationChannel(notificationChannel)
        }
        return channelID
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        private const val TAG = "SpeechActivationService"
        const val ACTIVATION_RESULT_INTENT_KEY = "ACTIVATION_RESULT_INTENT_KEY"
        const val ACTIVATION_RESULT_BROADCAST_NAME =
            "com.asesoreslocales.rdpost.functions.ACTIVATION"
        const val ACTIVATION_STOP_INTENT_KEY = "ACTIVATION_STOP_INTENT_KEY"
        const val NOTIFICATION_ID = 10298
        const val UTTERANCE_ID = "End of speak"
        const val EXTRA_WORD = "word"
        fun makeStartServiceIntent(context: Context?): Intent {
            return Intent(context, SpeechActivationService::class.java)
        }

        fun makeServiceStopIntent(context: Context?): Intent {
            val i = Intent(context, SpeechActivationService::class.java)
            i.putExtra(ACTIVATION_STOP_INTENT_KEY, true)
            return i
        }
    }
}