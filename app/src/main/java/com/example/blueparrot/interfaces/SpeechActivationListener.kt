package com.example.blueparrot.interfaces

interface SpeechActivationListener {
    /**
     * receive results from a {@link SpeechActivator}
     */
    fun activated(success: Boolean)

    fun onResult(heard: List<String>)
}