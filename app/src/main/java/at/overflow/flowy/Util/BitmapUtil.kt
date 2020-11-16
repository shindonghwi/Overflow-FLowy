package at.overflow.flowy.Util

import android.graphics.Bitmap
import android.os.Environment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.*

class BitmapUtil {
    fun textureBitmapToFile(bitmap: Bitmap?): File? {

        val filePath = Environment.getExternalStorageDirectory().toString()
        val folderName = "Flowy"
        val fileName = "uploadImage.jpeg"
        val dirs = File(filePath, folderName)

        // Flowy 폴더가 없으면 만든다.
        if (!dirs.exists()) {
            dirs.mkdirs()
        }

        var file: File? = null
        return try {
            file = File("$filePath/$folderName/$fileName")
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
}