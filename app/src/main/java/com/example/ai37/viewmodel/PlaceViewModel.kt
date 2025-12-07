package com.example.ai37.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai37.model.Place
import com.example.ai37.repository.ApiCallRepo
import com.example.ai37.util.ApiTokenUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaceViewModel : ViewModel() {

    private val _place = MutableStateFlow<Place?>(null)
    val place = _place.asStateFlow()

    fun loadPlace(placeId: String) {
        viewModelScope.launch {
            val params = mutableMapOf<String, String>()
            params["key"] = ApiTokenUtil.BAATO_API_KEY
            params["placeId"] = placeId

            val response = ApiCallRepo.getPlaceDetails(params)
            _place.value = response.data.firstOrNull()
        }
    }
}
