package com.example.blueparrot.controllers

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings.Global.getString
import com.example.blueparrot.R
import java.util.Locale

class AvailableLanguages(var context: Context) {
    private fun getSharedPref(): SharedPreferences {
        return context.getSharedPreferences("pref", Context.MODE_PRIVATE)
    }

    fun load() {

    }

    fun save(context: Context, localeList: ArrayList<String>) {
        val sharedPref =  getSharedPref();

    }

    companion object {
        fun defaultLocaleList(): ArrayList<String> {
            return arrayListOf(
                R.string.spanish.toString(),
                R.string.english.toString(),
                R.string.french.toString(),
                R.string.german.toString()
            )
        }
    }
}