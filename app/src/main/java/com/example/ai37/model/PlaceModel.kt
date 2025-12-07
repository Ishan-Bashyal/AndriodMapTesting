package com.example.ai37.model
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlaceModel(
    val license: String?,
    val score: Double?,
    var address: String,
    var centroid: LatLonModel,
    val placeId: Int,
    val osmId: Long,
    var name: String,
    val geometryModel: GeometryModel?,
    var type: String?,
    var tags: List<String>?
) : Parcelable

