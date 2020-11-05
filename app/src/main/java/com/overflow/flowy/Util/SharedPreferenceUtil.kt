package com.overflow.flowy.Util

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.overflow.flowy.DTO.ContrastData

class SharedPreferenceUtil {

    fun saveArrayListData(preferences: SharedPreferences, key : String, arrayList: ArrayList<ContrastData>) {
        val editor = preferences.edit()
        val gson = Gson()
        val json = gson.toJson(arrayList)
        editor.putString(key, json)
        editor.apply()
    }

    fun loadArrayListData(preferences: SharedPreferences, key : String) : ArrayList<ContrastData> {
        val gson = Gson()
        val json = preferences.getString(key, "")
        val type = object: TypeToken<ArrayList<ContrastData>>() {
        }.type

        return gson.fromJson(json, type)
    }

}