package com.example.blueparrot.interfaces

interface SpeechActivator {
    /**
    * listen for speech activation, when heard, call a {@link SpeechActivationListener}
    * and stop listening
    */
    fun detectActivation();

    /**
     * stop waiting for activation.
     */
    fun stop();
}