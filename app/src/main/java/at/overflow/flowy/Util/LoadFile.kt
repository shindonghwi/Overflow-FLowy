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
     * return -> shader code ( glsl 언어 코드 )
     *
     * raw 폴더에 shader code 파일을 보면 아래와 같은 구조로 되어있다.
     *
     * [default]
     * ~~ 내용 ~~
     * [/default]
     *
     * [luminance]
     * ~~ 내용 ~~
     * [/luminance]
     *
     * shaderCodeRead() 함수를 호출할때
     *  - rawFile 에는 vertex or fragment 값을 받는다. ( vertex , fragment shader code 에 대해 지식이 없다면 공부해야함. )
     *  - type 에는 default 인지 luminance 값을 선택할것인지 를 정한다.
     *
     * ~~ 내용 ~~ 을 불러오기 위해
     * ~~ 내용 ~~ 앞 뒤로 [type] 값 기준으로 스플릿을 하여 내용을 가져오는 함수이다.
     *
     * */
    fun shaderCodeRead(rawFile : String, type : String): String {
        var string: String? = ""
        val stringBuilder = StringBuilder()

        var inputStream: InputStream? = null

        if (rawFile == "vertex"){
            inputStream = THIS_CONTEXT?.resources!!.openRawResource(R.raw.external_vertex_shader_code)
        }
        else if (rawFile == "fragment"){
            inputStream = THIS_CONTEXT?.resources!!.openRawResource(R.raw.external_fragment_shader_code)
        }

        /** shader code 내용 불러오기 */
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

        Log.d("shader code", "$typeStartString : $typeEndString : $stringBuilder")

        /** shader code 내용 불러오기  */
        val code = stringBuilder.toString().split(typeStartString)[1].split(typeEndString)[0]
        inputStream?.close()

        Log.d("shader code", code)

        return code
    }

}