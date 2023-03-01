package com.example.android.camerax.video.repositories

import com.example.myapplication.model_class.MLResponse

interface MLModelListener {
    fun onVideoSuccess(authData: String?)

    fun onSuccess(authData: MLResponse?)

    fun onFailure(msg:String)

    fun onError(msg: String)

    fun onNoConnection(msg: String)
}