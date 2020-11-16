package at.overflow.flowy.Interface

import at.overflow.flowy.DTO.ContrastData
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.POST
import retrofit2.http.Query


interface RetrofitAPI {


    // @GET( EndPoint-자원위치(URI) )
//    @GET("posts/{post}")
//    fun getPosts(@Path("post") post: String?): Call<PostResult?>?

    /** 플로위 줌 로그데이터를 서버로 보낸다. */
    @POST("putActLog")
    fun postFlowyZoomLogData(@Body param:HashMap<String, Any>)

    @POST("detect")
    fun uploadImage(@Body param:HashMap<String, Any>):Call<Any>

}