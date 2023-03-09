package com.example.myapplication

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.hardware.*
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.provider.Settings
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.startForegroundService
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.android.camerax.video.model_class.CameraMetaData
import com.example.android.camerax.video.repositories.MLModelListener
import com.example.android.camerax.video.repositories.MyViewModelFactory
import com.example.myapplication.Utils.bitMapToFile
import com.example.myapplication.Utils.calculateAngleSC
import com.example.myapplication.Utils.circleArea
import com.example.myapplication.Utils.cropCircle
import com.example.myapplication.Utils.drawSquare
import com.example.myapplication.Utils.dynamicModelRun
import com.example.myapplication.Utils.qrCodeResultFromZxing
import com.example.myapplication.Utils.rotateImage
import com.example.myapplication.Utils.runModel
import com.example.myapplication.Utils.showBitmapInAlertDialog
import com.example.myapplication.Utils.takeScreenshotOfView
import com.example.myapplication.Utils.toBitmap
import com.example.myapplication.databinding.FragmentScannerBinding
import com.example.myapplication.model_class.MLResponse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.google.zxing.client.android.BeepManager
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.journeyapps.barcodescanner.camera.CameraParametersCallback
import com.tbruyelle.rxpermissions2.RxPermissions
import java.io.FileNotFoundException
import java.lang.Math.abs


class ScannerFragment : Fragment(), SensorEventListener, MLModelListener {

    private val TAG = ScannerFragment::class.java.simpleName
    private lateinit var barcodeView: DecoratedBarcodeView
    private var beepManager: BeepManager? = null
    private lateinit var model: MLViewModel

    private lateinit var binding: FragmentScannerBinding
    private lateinit var rxPermission: RxPermissions
    private var run_count: Int = 0
    private var image_count: Int = 0
    private var qr_code_type: String? = null
    var sensorManager: SensorManager? = null
    private var light_sensor: Sensor? = null
    private var zoomRatio: Float? = null

    private var light_value: Float? = null
    private var radioAlert: AlertDialog? = null
    private var simpleAlert: AlertDialog? = null

    private val REQUEST_CODE_SCREEN_CAPTURE = 1

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var mediaProjection: MediaProjection
    private lateinit var virtualDisplay: VirtualDisplay
    private lateinit var imageReader: ImageReader
    private val SCREEN_WIDTH = 720
    private val SCREEN_HEIGHT = 1280

    companion object {
        fun newInstance() =
            ScannerFragment()

    }

    private val callbackCamera: CameraParametersCallback =
        CameraParametersCallback { parameters ->
            val savedZoomLevel = StorageUtils(requireContext()).userMetaData.zoom_level
            parameters?.maxZoom?.let { getZoomRatio(it) }

            if (savedZoomLevel != null) {
                parameters?.zoom = savedZoomLevel.toInt()
            } else {
                if (zoomRatio != null) {
                    parameters?.zoom = zoomRatio?.toInt()!!
                }
            }
            parameters!!
        }

    private fun getZoomRatio(maxZoomRatio: Int) {

        zoomRatio = maxZoomRatio.times(1F.div(3F))
        val android_details = getSystemDetail()
        val userMeta = CameraMetaData(
            light_intensity = null,
            qr_code_type = qr_code_type,
            model = Build.MODEL,
            brand = Build.MANUFACTURER,
            os = android_details,
            zoom_level = zoomRatio,
            app_version = BuildConfig.VERSION_CODE.toFloat()
        )
        StorageUtils(requireContext()).storeUserMetaData(userMeta = userMeta)
    }

    @SuppressLint("HardwareIds")
    private fun getSystemDetail(): String {
        return "Brand: ${Build.BRAND} \n" +
                "DeviceID: ${
                    Settings.Secure.getString(
                        requireContext().contentResolver,
                        Settings.Secure.ANDROID_ID
                    )
                } \n" +
                "Model: ${Build.MODEL} \n" +
                "ID: ${Build.ID} \n" +
                "SDK: ${Build.VERSION.SDK_INT} \n" +
                "Manufacture: ${Build.MANUFACTURER} \n" +
                "Brand: ${Build.BRAND} \n" +
                "User: ${Build.USER} \n" +
                "Type: ${Build.TYPE} \n" +
                "Base: ${Build.VERSION_CODES.BASE} \n" +
                "Incremental: ${Build.VERSION.INCREMENTAL} \n" +
                "Board: ${Build.BOARD} \n" +
                "Host: ${Build.HOST} \n" +
                "FingerPrint: ${Build.FINGERPRINT} \n" +
                "Version Code: ${Build.VERSION.RELEASE}"
    }

    private val callback: BarcodeCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {

            try {
                val barBitmap = result.bitmap
                val scaleFactor = result.bitmapScaleFactor
                val originalBitmap = Bitmap.createScaledBitmap(
                    barBitmap,
                    barBitmap.width * scaleFactor,
                    barBitmap.height * scaleFactor,
                    true
                )

                if (originalBitmap != null) {
                    try {
                        requestScreenCapture()
//                        val screenShot = takeScreenshot()
                        val polygon =
                            originalBitmap.let { qrCodeResultFromZxing(it)?.resultPoints }


                        if (polygon != null) {
                            val area = circleArea(polygon)
                            Log.d(Utils.TAG, "circleArea : $area")
                            if (area>=50000){
                                getResultIfQRCodeIsNotXerox(originalBitmap, result,polygon)
                                binding.zoomQrText.text=""
                            }else{
                                binding.zoomQrText.text="Please bring your phone closer to QR Code"
                            }

                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    image_count--
                    Log.e(TAG, "decode result.resultPoints  ${result.resultPoints.size}")
                    val output = runModel(requireContext(), originalBitmap)

                    Log.e(TAG, "outputData : ${output?.get(0)}")
                    val count = dynamicModelRun(requireContext(), originalBitmap, run_count)
                    if (count != null) {
                        run_count = count

                        if (run_count == 0) {
//                            showBitmapInAlertDialog(originalBitmap)


                            return
                        } else {
//                            getResultIfQRCodeIsNotXerox(originalBitmap, result)
                        }
                    }
                }


            } catch (e: FileNotFoundException) {
                Log.e(TAG, e.stackTrace.toString())
            }

            barcodeView.statusView?.text = ""


        }

        override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {
//            Log.e(TAG, "resultPoints $resultPoints")
        }

    }
    private fun takeScreenshot(): Bitmap? {
        var screenBitmap:Bitmap?=null
        try {
            val v1: View = requireActivity().window.decorView.rootView
            v1.isDrawingCacheEnabled = true
            val bitmap = Bitmap.createBitmap(v1.drawingCache)
            v1.isDrawingCacheEnabled = false
            screenBitmap=bitmap
        } catch (e: Throwable) {
            // Several error may come out with file handling or DOM
            e.printStackTrace()
        }
        return screenBitmap
    }

    private fun requestScreenCapture() {
        val serviceIntent = Intent(requireContext(), MyForegroundService::class.java)
        startForegroundService(requireContext(),serviceIntent)
        Handler(Looper.getMainLooper()).postDelayed({
            mediaProjectionManager = requireActivity().getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_CAPTURE)
        }, 1000)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE && resultCode == Activity.RESULT_OK) {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data!!)
            startScreenCapture()
        }
    }
    private fun startScreenCapture() {
        imageReader = ImageReader.newInstance(SCREEN_WIDTH, SCREEN_HEIGHT, ImageFormat.JPEG, 1)
        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture",
            SCREEN_WIDTH, SCREEN_HEIGHT, resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, null)
        imageReader.setOnImageAvailableListener({ reader ->
            // Handle the captured image
            val image = reader.acquireLatestImage()
            val screenShot = image.toBitmap()
            showBitmapInAlertDialog(requireContext(),screenShot)
            image.close()
        }, null)
    }

    private fun getResultIfQRCodeIsNotXerox(
        originalBitmap: Bitmap,
        result: BarcodeResult,
        polygon: Array< ResultPoint>?
    ) {
//                                    withContext(Dispatchers.Main) {
//
////                                        controlDismissSimpleAlert("original code", requireContext())
////                                        progressVisibility(false)
//                                        run_count=0
//                                        image_count=0
//
//                                    }
//                                    return





        val croppedImg = polygon?.let { cropCircle(originalBitmap, it) }

//                        val fourthCoordinate = findFourthPoint(
//                            polygon[0].x,
//                            polygon[0].y,
//                            polygon[1].x,
//                            polygon[1].y,
//                            polygon[2].x,
//                            polygon[2].y
//                        )
//                        val croppedSquareImg = croppedImg?.let {
//                            drawSquareWithFourCoordinates(
//                                it,
//                                polygon[0].x,
//                                polygon[0].y,
//                                polygon[1].x,
//                                polygon[1].y,
//                                polygon[2].x,
//                                polygon[2].y,
//                                fourthCoordinate.first,
//                                fourthCoordinate.second
//                            )
//                        }

        val angle = calculateAngleSC(
            result.resultPoints[0].x,
            result.resultPoints[0].y,
            result.resultPoints[1].x,
            result.resultPoints[1].y,
            result.resultPoints[2].x,
            result.resultPoints[2].y
        )

//                        Log.e(TAG, "decode result.resultPoints ${polygon[0].x} ->${polygon[0].x}->${polygon[0].x}->${polygon[0].x}->${polygon[0].x}  ->${polygon[0].x}  ->${fourthCoordinate.first}  ->${fourthCoordinate.second}
//                            polygon[0].x,
//                            polygon[1].x,
//                            polygon[1].x,
//                            polygon[2].x,
//                            polygon[2].x,
//
//                            }")

        val userMetaData = StorageUtils(requireContext()).userMetaData
        userMetaData.qr_code_type = qr_code_type
//                              userMetaData.app_type=app_type
        userMetaData.light_intensity = light_value

        if (angle != null && abs(angle) == 0.0) {
            // cv2.imwrite(path, img)
            val croppedSquareImg =
                croppedImg?.let { drawSquare(it, polygon) }
            if (croppedSquareImg != null) {
                val file = qr_code_type?.let {
                    bitMapToFile(
                        croppedSquareImg,
                        it, "FMC_FILE", requireContext()
                    )
                }

                if (file != null) {
                    model.collectMLResponseByFrame(file, userMetaData)
                    beepManager?.playBeepSoundAndVibrate()
                    controlDismissRadioAlert(requireContext(), 1)
                    return
                }
            }

        } else if (angle != null) {
            val img = croppedImg?.let { rotateImage(it, 90 - abs(angle)) }
            val croppedSquareImg = img?.let {
                qrCodeResultFromZxing(it)?.resultPoints?.let { it1 ->
                    drawSquare(
                        it,
                        it1
                    )
                }
            }
            if (croppedSquareImg != null) {

                val file = qr_code_type?.let {
                    bitMapToFile(
                        croppedSquareImg,
                        it, "FMC_FILE", requireContext()
                    )
                }
//                                    showBitmapInAlertDialog(croppedSquareImg)

                if (file != null) {
                    model.collectMLResponseByFrame(file, userMetaData)
                    beepManager?.playBeepSoundAndVibrate()
                    controlDismissRadioAlert(requireContext(), 1)
                    return
                }
            }
        }
    }



    //todo: request the camera & other permissions before starting functions
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentScannerBinding.inflate(inflater, container, false)
        controlDismissRadioAlert(requireContext(), 1)
        rxPermission = RxPermissions(this)
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager


        if (sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT) != null) {
            light_sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT);
        }
        val userMeta = StorageUtils(requireContext()).userMetaData
        userMeta.qr_code_type = qr_code_type
//        userMeta.app_type = app_type
        StorageUtils(requireContext()).storeUserMetaData(
            userMeta = userMeta
        )
        requireActivity().let { activity ->
            model = ViewModelProvider(
                activity,
                MyViewModelFactory(
                    this
                )
            ).get(MLViewModel::class.java)
        }
        setUI()
        return binding.root
    }

    private fun pauseBarCodeReader(isPause: Boolean) {
        if (isPause)
            barcodeView.pause()
        else barcodeView.resume()

    }

    override fun onResume() {
        super.onResume()

        isSufficientPremissionsGranted()
        pauseBarCodeReader(false)

    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this);
        pauseBarCodeReader(true)
    }

    private fun setUI() {
        barcodeView = binding.barcodeScanner
        barcodeView.resume()

    }


    @SuppressLint("CheckResult")
    private fun isSufficientPremissionsGranted() {
//        var permission = false
        rxPermission.requestEach(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
            .subscribe { permission ->
                if (permission.granted) {
                    initScanner()
//                    permission = true
                } else if (permission.shouldShowRequestPermissionRationale) {
                    val alertDialog = MaterialAlertDialogBuilder(requireContext())
                    alertDialog.setTitle(getString(R.string.camera_location_access_needed))
                    alertDialog.setMessage(getString(R.string.grant_permission_camera_location))
                    alertDialog.setPositiveButton("Settings") { dialog, _ ->
                        dialog.dismiss()
                        AppUtils(requireActivity()).openApplicationSetting()
                    }
                    alertDialog.setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                        requireActivity().onBackPressed()
                    }
                    alertDialog.show()

                } else {
                    val alertDialog = MaterialAlertDialogBuilder(requireContext())
                    alertDialog.setTitle(getString(R.string.camera_location_access_needed))
                    alertDialog.setMessage(getString(R.string.grant_permission_camera_location))
                    alertDialog.setPositiveButton("OK") { dialog, _ ->
                        //                        isSufficientPremissionsGranted()

                    }
                    alertDialog.setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()

                    }
                    alertDialog.show()
                }


            }
    }

    private fun controlDismissSimpleAlert(s: String, context: Context) {
        if (simpleAlert == null) {

            showSimpleAlert(context, s)
        } else {
            simpleAlert?.dismiss()
            showSimpleAlert(context, s)
        }

    }

    private fun showSimpleAlert(context: Context, s: String) {
        val builder = AlertDialog.Builder(context)
        builder.setCancelable(false)
        builder.setTitle("Code status")
        builder.setMessage(s)
        //if the flag is false, just dismiss the dialog
        builder.setPositiveButton("ok") { dialog: DialogInterface, _: Int ->
            dialog.dismiss()
        }
        simpleAlert = builder.create()
        simpleAlert?.show()
    }

    private fun controlDismissRadioAlert(context: Context, id: Int) {
        val userMeta = StorageUtils(requireContext()).userMetaData
        userMeta.qr_code_type = qr_code_type
//            userMeta.app_type = app_type
        StorageUtils(requireContext()).storeUserMetaData(
            userMeta = userMeta
        )
        if (radioAlert == null) {
            showRadioAlert(context, id)
        } else {
            radioAlert?.dismiss()
            showRadioAlert(context, id)
        }
    }

    private fun showRadioAlert(context: Context, id: Int) {
        val builder = AlertDialog.Builder(context)
        val radioGroupQrCodeType = RadioGroup(context)
        val option1 = RadioButton(context)
        option1.text = "Original"
        val option2 = RadioButton(context)
        option2.text = "Digital"
        val option3 = RadioButton(context)
        option3.text = "Color Xerox"
        val option4 = RadioButton(context)
        option4.text = "HQ Color Xerox"

        radioGroupQrCodeType.addView(option1)
        radioGroupQrCodeType.addView(option2)
        radioGroupQrCodeType.addView(option3)
        radioGroupQrCodeType.addView(option4)
        radioGroupQrCodeType.check(id)

        //        val radioGroupAppType = RadioGroup(context)
        //        val option5 = RadioButton(context)
        //        option5.text = "Collector"
        //        val option6 = RadioButton(context)
        //        option6.text = "Verification"
        //        val option7 = RadioButton(context)
        //        option7.text = "Testing"
        //        radioGroupAppType.addView(option5)
        //        radioGroupAppType.addView(option6)
        //        radioGroupAppType.addView(option7)
        //        radioGroupAppType.check(5)

        //        val layout = LinearLayout(requireContext())
        //        val view = View(requireContext())
        //        view.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
        //        view.setBackgroundColor(R.color.black_overlay)
        //        layout.orientation = LinearLayout.VERTICAL
        //        layout.addView(radioGroupQrCodeType)
        //        layout.addView(view)
        //        layout.addView(radioGroupAppType)

        builder.setTitle("Select an option")
        builder.setView(radioGroupQrCodeType)
        builder.setCancelable(false)
        builder.setPositiveButton("OK") { dialog, which ->
            val selectedOption = radioGroupQrCodeType.indexOfChild(
                radioGroupQrCodeType.findViewById(radioGroupQrCodeType.checkedRadioButtonId)
            )
            val result = radioGroupQrCodeType.getChildAt(selectedOption) as RadioButton
            qr_code_type = result.text.toString()
            //            val radioGroupAppTypeSelectedOption =
            //                radioGroupAppType.indexOfChild(radioGroupAppType.findViewById(radioGroupAppType.checkedRadioButtonId))
            //            val radioGroupAppTypeResult =
            //                radioGroupAppType.getChildAt(radioGroupAppTypeSelectedOption) as RadioButton
            ////            app_type = radioGroupAppTypeResult.text.toString()

        }
        radioAlert = builder.create()
        radioAlert?.show()
    }

    private fun initScanner() {
        val formats: Collection<BarcodeFormat> =
            listOf(BarcodeFormat.QR_CODE, BarcodeFormat.CODE_39)
        barcodeView.barcodeView.decoderFactory = DefaultDecoderFactory(formats)
        barcodeView.initializeFromIntent(requireActivity().intent)
        barcodeView.decodeContinuous(callback)
        barcodeView.changeCameraParameters(callbackCamera)
        beepManager = BeepManager(requireActivity())
    }

    override fun onVideoSuccess(authData: String?) {
        if (authData != null) {
//            controlDismissSimpleAlert("original code", requireContext())

        }
//        progressVisibility(false)
    }


    override fun onSuccess(authData: MLResponse?) {
//        if (authData != null) {
//            controlDismissSimpleAlert(authData.data, requireContext())
//
//        }
//        progressVisibility(false)

    }

    override fun onFailure(msg: String) {
//        progressVisibility(false)

    }

    override fun onError(msg: String) {
        Utils.showToast(msg, requireContext())
//        progressVisibility(false)

    }

    override fun onNoConnection(msg: String) {
//        progressVisibility(false)

    }

    override fun onSensorChanged(event: SensorEvent?) {

        if (event != null) {
            if (event.sensor.type == Sensor.TYPE_LIGHT) {
                light_value = event.values[0]
                Log.d(TAG, "onSensorChanged light_value:$light_value  event:${event.values}")

            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

}


