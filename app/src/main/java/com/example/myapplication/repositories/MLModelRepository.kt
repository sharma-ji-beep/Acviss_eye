package com.example.android.camerax.video.repositories

import com.example.myapplication.model_class.MLResponse
import com.example.android.camerax.video.model_class.MLResponseVideo
import com.example.myapplication.remote_db.RemoteApiClient
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response

class MLModelRepository {

    suspend fun uploadImage(
        file: MultipartBody.Part
    ): Response<MLResponse> {
        return RemoteApiClient.getClient().uploadImage(file = file)

    }

    suspend fun uploadVideo(
        file: MultipartBody.Part,
        userMetaData: RequestBody
    ): Response<MLResponseVideo> {
        return RemoteApiClient.getClient().uploadVideo(file = file, userMetaData)

    }

    suspend fun collectVideo(
        file: MultipartBody.Part,
        device: RequestBody,
        light_intensity: RequestBody,
        zoom_level: RequestBody,
        os: RequestBody?,
        qr_code_type: RequestBody?,
        model: RequestBody?,
        brand: RequestBody?,
        app_type: RequestBody?,
        app_version: RequestBody?
    ): Response<MLResponseVideo> {
        return RemoteApiClient.getClient().collectVideo(
            file = file,
            device,
            light_intensity,
            zoom_level,
            os,
            qr_code_type,
            model,
            brand,
            app_type,
            app_version
        )

    }
    suspend fun predictFrame(
        file: MultipartBody.Part,
        device: RequestBody,
        light_intensity: RequestBody,
        zoom_level: RequestBody,
        os: RequestBody?,
        qr_code_type: RequestBody?,
        model: RequestBody?,
        brand: RequestBody?,
        app_type: RequestBody?,
        app_version: RequestBody?
    ): Response<MLResponse> {
        return RemoteApiClient.getClient().predictFrame(
            file = file,
            device,
            light_intensity,
            zoom_level,
            os,
            qr_code_type,
            model,
            brand,
            app_type,
            app_version
        )

    }

    suspend fun uploadVideoFirebaseData(
        uniqueCode: String
    ): Response<MLResponse> {
        return RemoteApiClient.getClient().uploadVideoFirebaseData(code = uniqueCode)

    }

    suspend fun uploadImageForRGBAnalysis(
        file: MultipartBody.Part
    ): Response<MLResponse> {
        return RemoteApiClient.getClient().uploadImageForRGBAnalysis(file = file)

    }


}
