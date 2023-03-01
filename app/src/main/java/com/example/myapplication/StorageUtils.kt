package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.android.camerax.video.model_class.CameraMetaData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class StorageUtils(private val context: Context) {
    private val STORAGE: String = BuildConfig.APPLICATION_ID + "_pref"
    private var preferences: SharedPreferences? = null
    private val USER_META_INFO_KEY = "current_user_meta_info"
    private val sharedPreferences: SharedPreferences
        get() = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)


    fun storeUserMetaData(userMeta: CameraMetaData?) {
        preferences = sharedPreferences
        val gson = Gson()
        val json: String = gson.toJson(userMeta)
        Log.e("userMeta", json)
        preferences?.edit()?.putString(USER_META_INFO_KEY, json)?.apply()
    }

    val userMetaData: CameraMetaData
        get() {
            preferences = sharedPreferences
            val defaultCameraMetaData =CameraMetaData("android",null,null,null,null,null,null,"Collector",null)
            val defaultJson: String = Gson().toJson(defaultCameraMetaData)
            val gson = Gson()
            val json: String? = preferences?.getString(USER_META_INFO_KEY, defaultJson)
            if (json != null) {
                Log.e("userMeta", json)
            }
            val type: Type = object : TypeToken<CameraMetaData?>() {}.type
            return gson.fromJson(json, type)
        }


}