package com.example.ai37.model
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Place(
    val license: String?,
    val score: Double?,
    var address: String,
    var centroid: LatLon,
    val placeId: Int,
    val osmId: Long,
    var name: String,
    val geometry: Geometry?,
    var type: String?,
    var tags: List<String>?
) : Parcelable

