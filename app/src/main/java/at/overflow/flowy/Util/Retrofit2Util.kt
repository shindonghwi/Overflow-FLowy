package at.overflow.flowy.Util

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Retrofit2Util {

    fun getRetrofit2Builder(baseURL : String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseURL)
//            .addConverterFactory(NullOnEmptyConverterFactory())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

}