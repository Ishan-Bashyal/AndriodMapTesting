package com.example.ai37.repository
import com.example.ai37.model.PlaceAPIResponseModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiCallRepo {
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://api.baato.io/api/v1/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val APIImplRepo: ApiImplRepo = retrofit.create(ApiImplRepo::class.java)

    suspend fun getPlaceDetails(params: Map<String, String>): PlaceAPIResponseModel {
        return APIImplRepo.getPlaceDetailsResponse(params).body()
            ?: throw Exception("Null response")
    }
}
