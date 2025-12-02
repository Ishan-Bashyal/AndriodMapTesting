package com.example.ai37.network
import com.example.ai37.model.PlaceAPIResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiCall {
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://api.baato.io/api/v1/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val baatoAPI: BaatoAPI = retrofit.create(BaatoAPI::class.java)

    suspend fun getPlaceDetails(params: Map<String, String>): PlaceAPIResponse {
        return baatoAPI.getPlaceDetailsResponse(params).body()
            ?: throw Exception("Null response")
    }
}
