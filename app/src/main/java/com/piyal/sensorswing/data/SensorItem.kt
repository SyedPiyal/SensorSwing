package com.piyal.sensorswing.data


data class SensorItem(
    val iconResId: Int,
    val sensorName: String,
    var isActive: Boolean,
    val sensorType: Int
)
