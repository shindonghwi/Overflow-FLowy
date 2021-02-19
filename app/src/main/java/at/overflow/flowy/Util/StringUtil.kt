package at.overflow.flowy.Util

import android.util.Base64
import java.nio.charset.Charset

class StringUtil {

    fun base64Encoding(str: String): String? {
        val base64 = str.toByteArray(Charset.forName("UTF-8"))
        return Base64.encodeToString(base64, Base64.DEFAULT)
    }

    fun base64Decoding(str: String?): String {
        return String(Base64.decode(str, Base64.DEFAULT), Charset.forName("UTF-8"))
    }

}