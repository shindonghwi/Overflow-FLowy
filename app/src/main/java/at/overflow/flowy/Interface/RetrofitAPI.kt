package at.overflow.flowy.Interface

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/** 서버와 네트워크 통신을 할때 사용하는 인터페이스 이다.
 *
 * postFlowyZoomLogData -> 사용자의 Flowy Zoom 사용 횟수를 측정하기 위해 서버와 통신하는 메서드
 *
 * uploadImage -> 버스 번호 인식 프로젝트에서 사용한건데, 카메라에서 캡처한 사진을 서버에 보내서 결과값을 받기위한 메서드
 *
 * */

interface RetrofitAPI {

    /** 플로위 줌 로그데이터를 서버로 보낸다. */
    @POST("putActLog")
    fun postFlowyZoomLogData(@Body param:HashMap<String, Any>)

    /** 버스 번호 인식 - lightVision */
    @POST("detect")
    fun uploadImage(@Body param:HashMap<String, Any>):Call<Any>

}