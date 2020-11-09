package at.overflow.flowy.Interface

import retrofit2.Call
import retrofit2.http.*

interface RetrofitAPI {
    @GET("/posts") // 여기는 주소가 들어감 , 아래 userId는 쿼리
    fun getData(@Query("userId") id:String): Call<List<POST>>

    /** 플로위 줌 로그데이터를 서버로 보낸다. */
    @POST("putActLog")
    fun postFlowyZoomLogData(@Body param:HashMap<String, Any>):Call<POST>


}