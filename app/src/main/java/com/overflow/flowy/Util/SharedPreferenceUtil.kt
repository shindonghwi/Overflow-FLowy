package com.overflow.flowy.Util

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.overflow.flowy.DTO.ContrastData

class SharedPreferenceUtil {

    /** 정수 데이터 저장 */
    fun saveIntData(preferences: SharedPreferences, key : String, value: Int){
        val editor = preferences.edit()
        editor.putInt(key, value)
        editor.apply()
    }

    /** 불린 데이터 저장 */
    fun saveBooleanData(preferences: SharedPreferences, key : String, value: Boolean){
        val editor = preferences.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    /** 문자열 데이터 저장 */
    fun saveStringData(preferences: SharedPreferences, key : String, value: String){
        val editor = preferences.edit()
        editor.putString(key, value)
        editor.apply()
    }

    /** 문자열 데이터 불러오기 */
    fun loadStringData(preferences: SharedPreferences, key : String) : String? {
        return preferences.getString(key, "")
    }

    /** ArrayList 데이터 저장 */
    fun saveArrayListData(preferences: SharedPreferences, key : String, arrayList: ArrayList<ContrastData>) {
        val editor = preferences.edit()
        val gson = Gson()
        val json = gson.toJson(arrayList)
        editor.putString(key, json)
        editor.apply()
    }

    /** ArrayList 데이터 불러오기 */
    fun loadArrayListData(preferences: SharedPreferences, key : String) : ArrayList<ContrastData> {
        val gson = Gson()
        val json = preferences.getString(key, "")
        val type = object: TypeToken<ArrayList<ContrastData>>() {
        }.type

        return gson.fromJson(json, type)
    }

    /** xml 파일 데이터 지우기 */
    fun removeKey(preferences: SharedPreferences, key : String){
        val editor = preferences.edit()
        editor.remove(key).apply()

    }

}