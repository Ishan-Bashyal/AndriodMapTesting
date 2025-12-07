package com.example.ai37.repository

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.QueryMap
import com.example.ai37.model.PlaceAPIResponseModel


interface ApiImplRepo {
    @GET("places/")
    suspend fun getPlaceDetailsResponse(@QueryMap parameters: Map<String, String>): Response<PlaceAPIResponseModel>
}
