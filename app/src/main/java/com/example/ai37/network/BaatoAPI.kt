package com.example.ai37.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.QueryMap
import com.example.ai37.model.PlaceAPIResponse


interface BaatoAPI {
    @GET("places/")
    suspend fun getPlaceDetailsResponse(@QueryMap parameters: Map<String, String>): Response<PlaceAPIResponse>
}
