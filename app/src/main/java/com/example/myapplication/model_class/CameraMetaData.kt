package com.example.android.camerax.video.model_class
data class CameraMetaDataObject(val metadata: CameraMetaData)

data class CameraMetaData(
    var device: String = "android",
    var light_intensity: Float?,
    var zoom_level: Float?,
    var os: String?,
    var qr_code_type: String?,
    var model: String?,
    var brand: String?,
    var app_type: String= "Collector",
    var app_version: Float?
)
