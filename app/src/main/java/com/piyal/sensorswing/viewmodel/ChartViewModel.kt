package com.piyal.sensorswing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.mikephil.charting.data.Entry
import com.piyal.sensorswing.repository.ChartRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChartViewModel : ViewModel() {

    private val repository = ChartRepository()

    fun getSensorData(sensorType: Int, callback: (List<Entry>) -> Unit) {
        viewModelScope.launch {
            val data = withContext(Dispatchers.IO) {
                repository.getSensorData(sensorType)
            }
            callback(data)
        }
    }
}
