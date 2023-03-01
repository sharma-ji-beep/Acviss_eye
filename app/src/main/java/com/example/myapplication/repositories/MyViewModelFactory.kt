package com.example.android.camerax.video.repositories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MyViewModelFactory(val mlModelListener: MLModelListener) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        modelClass.getConstructor(MLModelListener::class.java)
            .newInstance(mlModelListener)
}