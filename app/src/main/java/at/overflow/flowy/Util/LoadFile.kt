package at.overflow.flowy.Util

import android.util.Log
import at.overflow.flowy.R
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.StringBuilder

/** 앱 내에 있는 파일을 불러올때 사용할 클래스 */
class LoadFile {

    /** Raw 폴더에서 vertex or fragment shader code를 불러와서 사용한다.
     * return -> shader code (c언어 코드)*/
    fun shaderCodeRead(rawFile : String, type : String): String {
        var string: String? = ""
        val stringBuilder = StringBuilder()

        var inputStream: InputStream? = null

        if (rawFile == "vertex"){
            inputStream = THIS_CONTEXT?.resources!!.openRawResource(R.raw.externer_vertex_shader_code)
        }
        else if (rawFile == "fragment"){
            inputStream = THIS_CONTEXT?.resources!!.openRawResource(R.raw.external_fragment_shader_code)
        }

        val reader = BufferedReader(InputStreamReader(inputStream))
        while (true) {
            try {
                if (reader.readLine().also { string = it } == null) break
            } catch (e: IOException) {
                e.printStackTrace()
            }
            stringBuilder.append(string).append("\n")
        }

        val typeStartString = "[$type]"
        val typeEndString = "[/$type]"

        Log.d("sdf", "$typeStartString : $typeEndString : $stringBuilder")

        val code = stringBuilder.toString().split(typeStartString)[1].split(typeEndString)[0]
        if (inputStream != null) {
            inputStream.close()
        }

        Log.d("shader code", code)

        return code
    }

}