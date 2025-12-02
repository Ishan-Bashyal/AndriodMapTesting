package com.example.ai37.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Geometry(
    val type: String,
    val coordinates: Double?
) : Parcelable

