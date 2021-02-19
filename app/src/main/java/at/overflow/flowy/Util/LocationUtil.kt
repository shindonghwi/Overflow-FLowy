package at.overflow.flowy.Util

import android.location.Address
import android.location.Geocoder
import android.widget.Toast
import java.io.IOException
import java.util.*
import kotlin.math.cos

class LocationUtil {

    fun getCurrentAddress(latitude: Double, longitude: Double): String? {
        //지오코더... GPS를 주소로 변환
        val geocoder = Geocoder(THIS_CONTEXT, Locale.getDefault())
        val addresses: List<Address>
        addresses = try {
            geocoder.getFromLocation(
                latitude,
                longitude,
                7
            )
        } catch (ioException: IOException) {
            //네트워크 문제
            Toast.makeText(THIS_CONTEXT, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show()
            return "지오코더 서비스 사용불가"
        } catch (illegalArgumentException: IllegalArgumentException) {
            Toast.makeText(THIS_CONTEXT, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show()
            return "잘못된 GPS 좌표"
        }
        if (addresses == null || addresses.isEmpty()) {
            Toast.makeText(THIS_CONTEXT, "주소 미발견", Toast.LENGTH_LONG).show()
            return "주소 미발견"
        }
        val address: Address = addresses[0]
        return address.getAddressLine(0).toString().toString() + "\n"
    }

    //반경 m이내의 위도차(degree)
    fun latitudeInDifference(diff:Int):Double {
        //지구반지름
        val earth = 6371000 //단위m
        return (diff * 360.0) / (2.0 * Math.PI * earth.toDouble())
    }

    //반경 m이내의 경도차(degree)
    fun longitudeInDifference(_latitude:Double, diff:Int):Double {
        //지구반지름
        val earth = 6371000 //단위m
        val ddd = Math.cos(0.0)
        val ddf = Math.cos(Math.toRadians(_latitude))
        return (diff * 360.0) / (2.0 * Math.PI * earth.toDouble() * cos(Math.toRadians(_latitude)))
    }

}