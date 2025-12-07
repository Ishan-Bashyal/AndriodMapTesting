package com.example.ai37.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai37.model.PlaceModel
import com.example.ai37.repository.ApiCallRepo
import com.example.ai37.util.ApiTokenUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaceViewModel : ViewModel() {

    private val _placeModel = MutableStateFlow<PlaceModel?>(null)
    val place = _placeModel.asStateFlow()

    fun loadPlace(placeId: String) {
        viewModelScope.launch {
            val params = mutableMapOf<String, String>()
            params["key"] = ApiTokenUtil.BAATO_API_KEY
            params["placeId"] = placeId

            val response = ApiCallRepo.getPlaceDetails(params)
            _placeModel.value = response.data.firstOrNull()
        }
    }
}
