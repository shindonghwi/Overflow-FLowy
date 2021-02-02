package at.overflow.flowy.Util

import android.R.attr
import android.graphics.Bitmap
import android.os.Environment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer


class BitmapUtil {

    val filePath = Environment.getExternalStorageDirectory().toString()
    val folderName = "Flowy"

    fun saveImage(bitmap: Bitmap?, saveFileName : String, code : Int): File? {

        var classification : String = ""

        when (code){
            0 -> classification = "Success"
            1 -> classification = "SourceEmpty"
            2 -> classification = "ProcessorBusy"
            3 -> classification = "DecodeError"
            4 -> classification = "NotFoundBus"
            5 -> classification = "NotFoundBusNumber"
        }

        val fileName = "$saveFileName.jpg"
        val dirs = File(filePath, "$folderName/$classification")

        // Flowy 폴더가 없으면 만든다.
        if (!dirs.exists()) {
            dirs.mkdirs()
        }

        var file: File? = null
        return try {
            file = File("$filePath/$folderName/$classification/$fileName")
            file.createNewFile()

            //Convert bitmap to byte array
            val bos = ByteArrayOutputStream()
            bitmap!!.compress(Bitmap.CompressFormat.JPEG, 50, bos) // YOU can also save it in JPEG
            val bitmapData = bos.toByteArray()

            //write the bytes in file
            val fos = FileOutputStream(file)
            fos.write(bitmapData)
            fos.flush()
            fos.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            file // it will return null
        }
    }

    fun bitmapToByteArray(bitmap : Bitmap?):ByteArray {

//        val size: Int = bitmap!!.width * bitmap!!.height
//        val byteBuffer: ByteBuffer = ByteBuffer.allocate(bitmap.byteCount)
//        bitmap.copyPixelsToBuffer(byteBuffer)
//        return byteBuffer.array()

        val stream = ByteArrayOutputStream()
//        CoroutineScope(Dispatchers.Default).launch {
        bitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, stream)
//        }
        return stream.toByteArray()
    }
}