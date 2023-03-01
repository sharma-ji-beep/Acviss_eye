package com.example.myapplication

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model_class.MLResponse
import com.example.android.camerax.video.model_class.MLResponseVideo
import com.example.android.camerax.video.model_class.CameraMetaData
import com.example.android.camerax.video.model_class.CameraMetaDataObject
import com.example.android.camerax.video.repositories.MLModelListener
import com.example.android.camerax.video.repositories.MLModelRepository
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File


/***
 *need to do proper error handling
 */

class MLViewModel(private val mlModelListener: MLModelListener) : ViewModel() {
    val TAG = MLViewModel::class.java.simpleName
    private val mlModelRepository: MLModelRepository = MLModelRepository()


    fun getMLResponse(
        file: File
    ) {

        val requestBody = file.asRequestBody("image/jpg".toMediaTypeOrNull())
        val body: MultipartBody.Part =
            MultipartBody.Part.Companion.createFormData("file", file.name, requestBody)
        Log.d(TAG, "file ${file.name}: ${requestBody.contentType()}  : $body")
//        val name: RequestBody = "file".toRequestBody("text/plain".toMediaTypeOrNull())


        viewModelScope.launch {
            val response = mlModelRepository.uploadImage(body)
            val uploadImageForRGBAnalysisResponse =
                mlModelRepository.uploadImageForRGBAnalysis(body)
            if (response.isSuccessful) {
                val json = Gson().toJson(response.body())
                val data = Gson().fromJson(json, MLResponse::class.java)
                mlModelListener.onSuccess(data)
                Log.d(TAG, "json:: tag:${data} json:${json} response.body():${response.body()}")
            } else {
                response.errorBody(); // do something with that
                Log.d(TAG, "errorBody:: tag:${response.errorBody()}")
                mlModelListener.onError(response.message())
            }

            if (uploadImageForRGBAnalysisResponse.isSuccessful) {
                val json = Gson().toJson(uploadImageForRGBAnalysisResponse.body())
                val data = Gson().fromJson(json, MLResponse::class.java)
//                mlModelListener.onSuccess(data)
                Log.d(
                    TAG,
                    "uploadImageForRGBAnalysisResponse json:: tag:${data} json:${json} response.body():${uploadImageForRGBAnalysisResponse.body()}"
                )
            } else {
                uploadImageForRGBAnalysisResponse.errorBody(); // do something with that
                Log.d(
                    TAG,
                    "uploadImageForRGBAnalysisResponse errorBody:: tag:${uploadImageForRGBAnalysisResponse.errorBody()}"
                )
//                mlModelListener.onError(uploadImageForRGBAnalysisResponse.message())
            }

//            response.enqueue( object : Callback<ResponseBody>{
//                override fun onResponse(
//                    call: Call<ResponseBody>,
//                    response: Response<ResponseBody>
//                ) {
//
//                }
//
//                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
//                    Log.d(TAG, "onFailure:: tag:${t.printStackTrace()}")
//
//                    t.message?.let { mlModelListener.onFailure(it) }
//                }
//
//            })

        }
    }

    fun getMLResponseByVideo(
        file: File,
        userMetaData: CameraMetaData
    ) {
//        val testUserMetaData= CameraMetaData(device = "android",null,null,null)
        val gson = Gson()
        val json = gson.toJson(CameraMetaDataObject(userMetaData))
        Log.d(
            TAG,
            "userMetaData json::$json "
        )
        val metadataBody = json.toRequestBody("application/json".toMediaTypeOrNull())

        val requestBody = file.asRequestBody("video/mp4".toMediaTypeOrNull())
        val body: MultipartBody.Part =
            MultipartBody.Part.Companion.createFormData("file", file.name, requestBody)
        Log.d(TAG, "file ${file.name}: ${requestBody.contentType()}  : $body")
//        val uniqueCode = UUID.randomUUID().toString()



        viewModelScope.launch {

            val uploadVideoResponse = mlModelRepository.uploadVideo(body,metadataBody)

            if (uploadVideoResponse.isSuccessful) {
                val uploadVideoResponseJson = Gson().toJson(uploadVideoResponse.body())
                val data = Gson().fromJson(uploadVideoResponseJson, MLResponseVideo::class.java)
//                mlModelListener.onVideoSuccess(data.code)
                Log.d(
                    TAG,
                    "uploadVideoResponse json:: tag:${data} json:${uploadVideoResponseJson} response.body():${uploadVideoResponse.body()}"
                )

                callFirebaseProcessedData(data)

            } else {
                uploadVideoResponse.errorBody(); // do something with that
                Log.d(
                    TAG, "uploadVideoResponse errorBody:: tag:${
                        uploadVideoResponse.errorBody()
                            ?.string()
                    }"
                )
//                mlModelListener.onError(uploadImageForRGBAnalysisResponse.message())
            }


        }
    }
    fun collectMLResponseByVideo(
        file: File,
        userMetaData: CameraMetaData
    ) {
//        val testUserMetaData= CameraMetaData(device = "android",null,null,null)
        val gson = Gson()
        val json = gson.toJson(CameraMetaDataObject(userMetaData))
        Log.d(
            TAG,
            "userMetaData json::$json "
        )
        val metadataBody = json.toRequestBody("application/json".toMediaTypeOrNull())


        val device = userMetaData.device.toRequestBody("text/plain".toMediaTypeOrNull())
        val light_intensity = userMetaData.light_intensity?.let { requestBodyFloat(it) }
        val zoom_level = userMetaData.zoom_level?.let { requestBodyFloat(it) }
        val os = userMetaData.os?.toRequestBody("text/plain".toMediaTypeOrNull())
        val qr_code_type = userMetaData.qr_code_type?.toRequestBody("text/plain".toMediaTypeOrNull())
        val model = userMetaData.model?.toRequestBody("text/plain".toMediaTypeOrNull())
        val brand = userMetaData.brand?.toRequestBody("text/plain".toMediaTypeOrNull())
        val app_type = userMetaData.app_type.toRequestBody("text/plain".toMediaTypeOrNull())
        val app_version = userMetaData.app_version?.let { requestBodyFloat(it) }


        val requestBody = file.asRequestBody("video/mp4".toMediaTypeOrNull())
        val body: MultipartBody.Part =
            MultipartBody.Part.Companion.createFormData("file", file.name, requestBody)
        Log.d(TAG, "file ${file.name}: ${requestBody.contentType()}  : $body")
//        val uniqueCode = UUID.randomUUID().toString()



        viewModelScope.launch {
            if (zoom_level != null && light_intensity!=null){

                try{
                    val uploadVideoResponse = mlModelRepository.collectVideo(
                        body,
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

                    if (uploadVideoResponse.isSuccessful) {
                        val uploadVideoResponseJson = Gson().toJson(uploadVideoResponse.body())
                        val data =
                            Gson().fromJson(uploadVideoResponseJson, MLResponseVideo::class.java)
//                mlModelListener.onVideoSuccess(data.code)
                        Log.d(
                            TAG,
                            "collectVideoResponse json:: tag:${data} json:${uploadVideoResponseJson} response.body():${uploadVideoResponse.body()}"
                        )
                        mlModelListener.onVideoSuccess("Upload Complete")

//                    callFirebaseProcessedData(data)

                    } else {
                        uploadVideoResponse.errorBody(); // do something with that
                        Log.d(
                            TAG, "collectVideoResponse errorBody:: tag:${
                                uploadVideoResponse.errorBody()
                                    ?.string()
                            }"
                        )
                        mlModelListener.onError(uploadVideoResponse.message())

//                mlModelListener.onError(uploadImageForRGBAnalysisResponse.message())
                    }
                } catch (t:Throwable){
                    mlModelListener.onError(t.message.toString())
                }
            }
        }
    }

    fun collectMLResponseByFrame(
        file: File,
        userMetaData: CameraMetaData
    ) {
//        val testUserMetaData= CameraMetaData(device = "android",null,null,null)
        val gson = Gson()
        val json = gson.toJson(CameraMetaDataObject(userMetaData))
        Log.d(
            TAG,
            "userMetaData json::$json "
        )
        val metadataBody = json.toRequestBody("application/json".toMediaTypeOrNull())


        val device = userMetaData.device.toRequestBody("text/plain".toMediaTypeOrNull())
        val light_intensity = userMetaData.light_intensity?.let { requestBodyFloat(it) }
        val zoom_level = userMetaData.zoom_level?.let { requestBodyFloat(it) }
        val os = userMetaData.os?.toRequestBody("text/plain".toMediaTypeOrNull())
        val qr_code_type = userMetaData.qr_code_type?.toRequestBody("text/plain".toMediaTypeOrNull())
        val model = userMetaData.model?.toRequestBody("text/plain".toMediaTypeOrNull())
        val brand = userMetaData.brand?.toRequestBody("text/plain".toMediaTypeOrNull())
        val app_type = userMetaData.app_type.toRequestBody("text/plain".toMediaTypeOrNull())
        val app_version = userMetaData.app_version?.let { requestBodyFloat(it) }


        val requestBody = file.asRequestBody("image/jpg".toMediaTypeOrNull())
        val body: MultipartBody.Part = MultipartBody.Part.Companion.createFormData("file", file.name, requestBody)
        Log.d(TAG, "file ${file.name}: ${requestBody.contentType()}  : $body")
//        val uniqueCode = UUID.randomUUID().toString()



        viewModelScope.launch {
            if (zoom_level != null && light_intensity!=null){

                val uploadVideoResponse = mlModelRepository.predictFrame(body,device,light_intensity,zoom_level,os,qr_code_type,model,brand,app_type,app_version)

                if (uploadVideoResponse.isSuccessful) {
                    val uploadVideoResponseJson = Gson().toJson(uploadVideoResponse.body())
                    val data = Gson().fromJson(uploadVideoResponseJson, MLResponse::class.java)
                mlModelListener.onVideoSuccess(data.data)
                    Log.d(
                        TAG,
                        "collectVideoResponse json:: tag:${data} json:${uploadVideoResponseJson} response.body():${uploadVideoResponse.body()}"
                    )


                } else {
                    uploadVideoResponse.errorBody(); // do something with that
                    Log.d(
                        TAG, "collectVideoResponse errorBody:: tag:${
                            uploadVideoResponse.errorBody()
                                ?.string()
                        }"
                    )
//                mlModelListener.onError(uploadImageForRGBAnalysisResponse.message())
                }
            }


        }
    }

    private fun requestBodyFloat(doubleValue: Float): RequestBody {
        return doubleValue.toString().toByteArray()
            .toRequestBody("application/octet-stream".toMediaTypeOrNull())

    }

    private suspend fun callFirebaseProcessedData(data: MLResponseVideo) {
        val firebaseDataResponse = mlModelRepository.uploadVideoFirebaseData(data.code)
        val firebaseDataJson = Gson().toJson(firebaseDataResponse.body())
        val firebaseDataData = Gson().fromJson(firebaseDataJson, MLResponse::class.java)
        if (firebaseDataResponse.isSuccessful) {
            mlModelListener.onSuccess(firebaseDataData)
            Log.d(
                TAG,
                "firebaseDataResponse json:: tag:${
                    firebaseDataData
                } json:${firebaseDataJson} response.body():${firebaseDataResponse.body()}"
            )
        } else {
            callFirebaseProcessedData(data)
            mlModelListener.onError(firebaseDataResponse.message())
            Log.d(
                TAG, "firebaseDataResponse errorBody:: tag:${
                    firebaseDataResponse.errorBody()
                        ?.string()
                }"
            )
        }
    }

}