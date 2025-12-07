package com.example.ai37.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LatLonModel(
    val lat: Double,
    val lon: Double
) : Parcelable


