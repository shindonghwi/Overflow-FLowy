package at.overflow.flowy.Interface

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*


interface RetrofitAPI {


    // @GET( EndPoint-자원위치(URI) )
//    @GET("posts/{post}")
//    fun getPosts(@Path("post") post: String?): Call<PostResult?>?

    /** 플로위 줌 로그데이터를 서버로 보낸다. */
    @POST("putActLog")
    fun postFlowyZoomLogData(@Body param:HashMap<String, Any>)

    @Multipart
    @POST("info")
    fun uploadFile(@Part image : MultipartBody.Part):Call<ResponseBody>

}