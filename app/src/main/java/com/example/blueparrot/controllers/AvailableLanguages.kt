package com.example.blueparrot.controllers

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import com.example.blueparrot.R

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
        fun defaultLanguagesList(resources: Resources): ArrayList<String> {
            return arrayListOf(
                resources.getString(R.string.spanish),
                resources.getString(R.string.english),
                resources.getString(R.string.french),
                resources.getString(R.string.german)
            )
        }
    }
}