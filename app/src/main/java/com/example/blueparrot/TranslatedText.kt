package com.example.blueparrot

import com.google.android.gms.common.data.DataBufferObserver
import kotlin.properties.Delegates

class TranslatedText : DataBufferObserver.Observable {
    var text: String? = null

    override fun addObserver(callback: DataBufferObserver) {
        TODO("Not yet implemented")
    }

    override fun removeObserver(callback: DataBufferObserver) {
        TODO("Not yet implemented")
    }

}