package com.example.android.camerax.video.remote_db

import com.example.myapplication.model_class.MLResponse
import com.example.android.camerax.video.model_class.MLResponseVideo
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface RemoteApiInterface {


    @Multipart
    @POST("/upload/image")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part
    ): Response<MLResponse>

    @Multipart
    @POST("/analysis/image/rgb")
    suspend fun uploadImageForRGBAnalysis(
        @Part file: MultipartBody.Part
    ): Response<MLResponse>

    @Multipart
    @POST("/upload/video/yellow")
    suspend fun uploadVideo(
        @Part file: MultipartBody.Part,
        @Part("metadata") userMetaData: RequestBody
    ): Response<MLResponseVideo>

   @Multipart
    @POST("/collect/video/yellow")
    suspend fun collectVideo(
       @Part file: MultipartBody.Part,
       @Part("device") userMetaData: RequestBody,
       @Part("light_intensity")light_intensity: RequestBody,
       @Part("zoom_level")zoom_level: RequestBody,
       @Part("os")os: RequestBody?,
       @Part("qr_code_type")qr_code_type: RequestBody?,
       @Part("model")model: RequestBody?,
       @Part("brand")brand: RequestBody?,
       @Part("app_type")app_type: RequestBody?,
       @Part("app_version")app_version: RequestBody?
   ): Response<MLResponseVideo>

    @Multipart
    @POST("/collect/image/yellow")
    suspend fun collectFrame(
       @Part file: MultipartBody.Part,
       @Part("device") userMetaData: RequestBody,
       @Part("light_intensity")light_intensity: RequestBody,
       @Part("zoom_level")zoom_level: RequestBody,
       @Part("os")os: RequestBody?,
       @Part("qr_code_type")qr_code_type: RequestBody?,
       @Part("model")model: RequestBody?,
       @Part("brand")brand: RequestBody?,
       @Part("app_type")app_type: RequestBody?,
       @Part("app_version")app_version: RequestBody?
   ): Response<MLResponse>

    @Multipart
    @POST("/predict/image/yellow")
    suspend fun predictFrame(
        @Part file: MultipartBody.Part,
        @Part("device") userMetaData: RequestBody,
        @Part("light_intensity")light_intensity: RequestBody,
        @Part("zoom_level")zoom_level: RequestBody,
        @Part("os")os: RequestBody?,
        @Part("qr_code_type")qr_code_type: RequestBody?,
        @Part("model")model: RequestBody?,
        @Part("brand")brand: RequestBody?,
        @Part("app_type")app_type: RequestBody?,
        @Part("app_version")app_version: RequestBody?
    ): Response<MLResponse>

    @GET("/result")
    suspend fun uploadVideoFirebaseData(
        @Query("unique_code") code: String
    ): Response<MLResponse>

    companion object {
        //API end points..
        const val PRODUCTION = "https://test3.acviss.co"

        const val SUCCESS = 200

        //        const val BASEURL = TESTING
        const val BASEURL = PRODUCTION

    }

}