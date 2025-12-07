package com.example.ai37.model

data class PlaceAPIResponseModel(
    val timestamp: String,
    val status: Int,
    val message: String,
    val data: List<Place>
)

