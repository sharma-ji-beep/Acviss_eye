package com.example.myapplication

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.database.Cursor
import android.graphics.*
import android.media.Image
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.myapplication.ml.Black
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.*
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.atan2
import kotlin.math.sqrt


object Utils {
    val TAG: String = Utils::class.java.simpleName

    fun getAbsolutePathFromUri(contentUri: Uri, context: Context): String? {
        var cursor: Cursor? = null
        return try {
            cursor = context
                .contentResolver
                .query(contentUri, arrayOf(MediaStore.Images.Media.DATA), null, null, null)
            if (cursor == null) {
                return null
            }
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            cursor.getString(columnIndex)
        } catch (e: RuntimeException) {
            Log.e(
                "VideoViewerFragment", String.format(
                    "Failed in getting absolute path for Uri %s with Exception %s",
                    contentUri.toString(), e.toString()
                )
            )
            null
        } finally {
            cursor?.close()
        }
    }

    fun calculateAngle(
        topLeftX: Float,
        topLeftY: Float,
        topRightX: Float,
        topRightY: Float,
        bottomLeftX: Float,
        bottomLeftY: Float
    ): Double? {
        val dx1 = topRightX - topLeftX
        val dy1 = topRightY - topLeftY
        val dx2 = bottomLeftX - topLeftX
        val dy2 = bottomLeftY - topLeftY

        return try {
            Math.toDegrees(
                atan2(dy1.toDouble(), dx1.toDouble()) - atan2(
                    dy2.toDouble(),
                    dx2.toDouble()
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    }

    fun calculateAngleSC(
        topLeftX: Float,
        topLeftY: Float,
        topRightX: Float,
        topRightY: Float,
        bottomLeftX: Float,
        bottomLeftY: Float
    ): Double? {
        return try {
            val angle = atan2(topRightY - topLeftY, topRightX - topLeftX)
            val v = Math.toDegrees(angle - Math.PI / 2)
            v
        } catch (e: Exception) {
            null
        }

    }

    fun rotateImage(bitmap: Bitmap, degree: Double): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun addBlurInImage(sourceBitmap: Bitmap, context: Context): Bitmap {

        val rs = RenderScript.create(context)
        val input = Allocation.createFromBitmap(rs, sourceBitmap)
        val output = Allocation.createTyped(rs, input.type)
        val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        script.setRadius(2f)
        script.setInput(input)
        script.forEach(output)
        output.copyTo(sourceBitmap)
        rs.destroy()
        return sourceBitmap

    }

    fun cropCircle(bitmap: Bitmap, polygon: Array<ResultPoint>): Bitmap? {

        val centerX = getCenterX(polygon)
        val centerY = getCenterY(polygon)

        val width = getWidthFromPolygon(polygon, centerX, centerY)
        val radius = getRadiusFromWidth(width)


        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)


        val paint = Paint()
        paint.isAntiAlias = true

        canvas.drawColor(Color.WHITE)

        // Crop the circle from the original bitmap
        val circleBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val circleCanvas = Canvas(circleBitmap)
        circleCanvas.drawCircle(centerX, centerY, radius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        circleCanvas.drawBitmap(bitmap, 0f, 0f, paint)
        paint.xfermode = null


        // Calculate the position to draw the cropped circle on the white canvas
        val drawX = (output.width - circleBitmap.width) / 2
        val drawY = (output.height - circleBitmap.height) / 2

        // Draw the cropped circle onto the white canvas
        canvas.drawBitmap(circleBitmap, drawX.toFloat(), drawY.toFloat(), null)

        return output


    }

    private fun getRadiusFromWidth(width: Float): Float {
        val radius = ((487F.div(277F)) * width)
        return radius
    }

    fun drawSquare(bitmap: Bitmap, polygon: Array<ResultPoint>): Bitmap {
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height
        val newBitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val centerX = getCenterX(polygon)
        val centerY = getCenterY(polygon)
        val width = getWidthFromPolygon(polygon, centerX, centerY)
//        val sideLength = ((1F.div(2F)) * width) + width
        val sideLength = width.times(2)
        Log.d(TAG, "sideLength :$sideLength : $width")


        val canvas = Canvas(newBitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        val paint = Paint()
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        val halfSideLength = sideLength / 2
        canvas.drawRect(
            centerX - halfSideLength,
            centerY - halfSideLength,
            centerX + halfSideLength,
            centerY + halfSideLength,
            paint
        )
        return newBitmap
    }

    private fun getWidthFromPolygon(
        polygon: Array<ResultPoint>,
        centerX: Float,
        centerY: Float
    ) = sqrt(
        (polygon[0].x - centerX).times((polygon[0].x - centerX)) +
                (polygon[0].y - centerY).times((polygon[0].y - centerY))
    )

    private fun getCenterY(polygon: Array<ResultPoint>) =
        (polygon[0].y + polygon[2].y) / 2

    private fun getCenterX(polygon: Array<ResultPoint>) =
        (polygon[0].x + polygon[2].x) / 2

    fun drawSquareWithFourCoordinates(
        bitmap: Bitmap,
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newBitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        val paint = Paint()
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL

        val squarePath = Path()
        squarePath.moveTo(x0, y0)
        squarePath.lineTo(x1, y1)
        squarePath.lineTo(x2, y2)
        squarePath.lineTo(x3, y3)
        squarePath.lineTo(x0, y0)
        squarePath.close()

        canvas.drawPath(squarePath, paint)

        return newBitmap
    }

    fun findFourthPoint(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float
    ): Pair<Float, Float> {
        val sideLength = findDistance(x0, y0, x1, y1)
        val orientation = determineOrientation(x0, y0, x1, y1, x2, y2)
        return calculateFourthCoordinate(x2, y2, sideLength, orientation)
    }

    private fun findDistance(x0: Float, y0: Float, x1: Float, y1: Float): Float {
        return sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0))
    }

    private fun determineOrientation(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float
    ): Int {
        val slope = (y1 - y0) / (x1 - x0)
        val perpendicularSlope = -1 / slope
        val yIntercept = y2 - x2 * perpendicularSlope
        return when {
            y2 > (perpendicularSlope * x2 + yIntercept) -> 1
            y2 < (perpendicularSlope * x2 + yIntercept) -> -1
            else -> 0
        }
    }

    private fun calculateFourthCoordinate(
        x2: Float,
        y2: Float,
        sideLength: Float,
        orientation: Int
    ): Pair<Float, Float> {
        return when (orientation) {
            1 -> Pair(x2 + sideLength, y2 + sideLength)
            -1 -> Pair(x2 - sideLength, y2 - sideLength)
            else -> Pair(x2, y2 + sideLength)
        }
    }

    fun saveImageIntoFile(context: Context, bitmap: Bitmap, albumName: String) {
        val filename = "${System.currentTimeMillis()}.jpg"
        val write: (OutputStream) -> Boolean = {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DCIM}/$albumName"
                )
            }

            context.contentResolver.let {
                it.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.let { uri ->
                    it.openOutputStream(uri)?.let(write)
                }
            }
        } else {
            val imagesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                    .toString() + File.separator + albumName
            val file = File(imagesDir)
            if (!file.exists()) {
                file.mkdir()
            }
            val image = File(imagesDir, filename)
            write(FileOutputStream(image))
        }

    }

    fun bitMapToFile(
        bitmap: Bitmap,
        qr_code_type: String,
        albumName: String,
        context: Context
    ): File? {
        val filename = "$qr_code_type${System.currentTimeMillis()}.jpg"
        val write: (OutputStream) -> Boolean = {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DCIM}/$albumName"
                )
            }

            var imageUri: Uri? = null
            context.contentResolver.let {
                imageUri = it.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                imageUri?.let { uri ->
                    it.openOutputStream(uri)?.let(write)
                }
            }

            val file = imageUri?.let { uri ->
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    it.moveToFirst()
                    val columnIndex = it.getColumnIndex(MediaStore.Images.Media.DATA)
                    val filePath = it.getString(columnIndex)
                    File(filePath)
                }
            }

            return file
        } else {


            val imagesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                    .toString() + File.separator + albumName + File.separator + filename
            val file = File(imagesDir)
            if (!file.exists()) {
                file.mkdir()
            }
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            write(outputStream)
            return file
        }
    }

    fun qrCodeResultFromZxing(bitmap: Bitmap): Result? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val source = RGBLuminanceSource(width, height, pixels)
        val bBitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = MultiFormatReader()

        return try {
            reader.decode(bBitmap)
        } catch (e: Exception) {
            Log.e(TAG, "decode exception", e)
            reader.reset()
            null
        }

    }

    fun convertColorImgToGray(originalImage: Bitmap): Bitmap? {
        val width: Int = originalImage.width
        val height: Int = originalImage.height
        val bwImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val color: Int = originalImage.getPixel(x, y)
                val red: Int = Color.red(color)
                val green: Int = Color.green(color)
                val blue: Int = Color.blue(color)
                val gray = (red + green + blue) / 3
                val newPixel: Int = Color.argb(255, gray, gray, gray)
                bwImage.setPixel(x, y, newPixel)
            }
        }
        return bwImage
    }

    fun circleArea(polygon: Array<ResultPoint>): Double {
        val centerX = getCenterX(polygon)
        val centerY = getCenterY(polygon)

        val width = getWidthFromPolygon(polygon, centerX, centerY)
        val radius = getRadiusFromWidth(width)

        return Math.PI * radius * radius
    }

    fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val bytes: Int = bitmap.byteCount
        val buffer = ByteBuffer.allocate(bytes) //Create a new buffer

        bitmap.copyPixelsToBuffer(buffer)
        buffer.rewind()
        return buffer
    }

    fun loadModelFile(context: Context): ByteBuffer {
        val fileDescriptor = context.assets.openFd("blackWhiteCodeCheck.h5")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun runModel(context: Context, bitmap: Bitmap): IntArray? {
        try {
            val image = Bitmap.createScaledBitmap(bitmap, 225, 225, true)

            val tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(image)
            val byteBuffer: ByteBuffer = tensorImage.buffer
            // Creates inputs for reference.
            val inputFeature0: TensorBuffer =
                TensorBuffer.createFixedSize(intArrayOf(1, 225, 225, 3), DataType.FLOAT32)
            inputFeature0.loadBuffer(byteBuffer)

            val model4: Black = Black.newInstance(context)

            val outputs: Black.Outputs = model4.process(inputFeature0)
            val outputFeature0: TensorBuffer = outputs.outputFeature0AsTensorBuffer
            val data1: FloatArray = outputFeature0.floatArray
            val ret = IntArray(data1.size)
            for (i in data1.indices) {
                ret[i] = data1[i].toInt()
            }
            model4.close()
            return ret
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

    }

    fun dynamicModelRun(context: Context, bitmap: Bitmap, count: Int): Int? {
        return if (count == 0) {
            val modelValue = runModel(context, bitmap)?.get(0)
            Log.d(TAG, "dynamicModelRun $modelValue")
            modelValue
        } else count
    }

    fun takeScreenshotOfView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return bitmap
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun usePixelCopy(videoView: SurfaceView, callback: (Bitmap?) -> Unit) {
        val bitmap: Bitmap = Bitmap.createBitmap(
            videoView.width,
            videoView.height,
            Bitmap.Config.ARGB_8888
        );
        try {
            // Create a handler thread to offload the processing of the image.
            val handlerThread = HandlerThread("PixelCopier");
            handlerThread.start();
            PixelCopy.request(
                videoView, bitmap,
                { copyResult ->
                    if (copyResult == PixelCopy.SUCCESS) {
                        callback(bitmap)
                    }
                    handlerThread.quitSafely();
                },
                Handler(handlerThread.looper)
            )
        } catch (e: IllegalArgumentException) {
            callback(null)
            // PixelCopy may throw IllegalArgumentException, make sure to handle it
            e.printStackTrace()
        }
    }

    fun showBitmapInAlertDialog(context: Context, bitmap: Bitmap?) {
        val imageView = ImageView(context)
        imageView.setImageBitmap(bitmap)
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Select an option")
        builder.setView(imageView)
        builder.setCancelable(false)
        builder.setPositiveButton("OK") { dialog, which ->


        }
        val dialog = builder.create()
        dialog.show()
    }

    fun showAlert(s: String, context: Context) {
        val builder =
            AlertDialog.Builder(context)
        builder.setCancelable(false)
        builder.setTitle("Code status")
        builder.setMessage(s)
        //if the flag is false, just dismiss the dialog
        builder.setPositiveButton("ok") { dialog: DialogInterface, _: Int ->
            dialog.dismiss()

        }
        builder.show()

    }

    fun showRadioAlert(context: Context): String? {
        var selectedOptionName: String? = null

        val radioGroup = RadioGroup(context)
        val option1 = RadioButton(context)
        option1.text = "Original"
        val option2 = RadioButton(context)
        option2.text = "Digital"
        val option3 = RadioButton(context)
        option3.text = "Color Xerox"
        val option4 = RadioButton(context)
        option4.text = "HQ Color Xerox"

        radioGroup.addView(option1)
        radioGroup.addView(option2)
        radioGroup.addView(option3)
        radioGroup.addView(option4)
        radioGroup.check(option1.id)
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Select an option")
        builder.setView(radioGroup)
        builder.setCancelable(false)
        builder.setPositiveButton("OK") { dialog, which ->
            val selectedOption =
                radioGroup.indexOfChild(radioGroup.findViewById(radioGroup.checkedRadioButtonId))
            val result = radioGroup.getChildAt(selectedOption) as RadioButton
            selectedOptionName = result.text.toString()

        }
        val dialog = builder.create()
        dialog.show()
        return selectedOptionName
    }

    fun showToast(s: String, context: Context) {
        Toast.makeText(context, s, Toast.LENGTH_LONG).show()
    }


    fun Image.toBitmap(): Bitmap {
        val buffer: ByteBuffer = this.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer[bytes]
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
    }
}