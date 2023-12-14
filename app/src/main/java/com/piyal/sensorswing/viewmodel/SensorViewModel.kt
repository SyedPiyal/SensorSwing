package com.piyal.sensorswing.viewmodel



import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.piyal.sensorswing.R
import com.piyal.sensorswing.data.SensorItem
import com.piyal.sensorswing.database.SensorDatabaseHelper
import com.piyal.sensorswing.services.SensorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale


class SensorViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val _sensorList = MutableLiveData<List<SensorItem>>()
    val sensorList: LiveData<List<SensorItem>> get() = _sensorList

    private val _isLightSensorActive = MutableLiveData<Boolean>()
    val isLightSensorActive: LiveData<Boolean> get() = _isLightSensorActive

    private val _isAccelerometerSensorActive = MutableLiveData<Boolean>()
    val isAccelerometerSensorActive: LiveData<Boolean> get() = _isAccelerometerSensorActive

    private val _isProximitySensorActive = MutableLiveData<Boolean>()
    val isProximitySensorActive: LiveData<Boolean> get() = _isProximitySensorActive

    private val _isGyroscopeSensorActive = MutableLiveData<Boolean>()
    val isGyroscopeSensorActive: LiveData<Boolean> get() = _isGyroscopeSensorActive

    private val _lightSensorValue = MutableLiveData<Float>()
    val lightSensorValue: LiveData<Float> get() = _lightSensorValue

    private val _accelerometerSensorValue = MutableLiveData<Float>()
    val accelerometerSensorValue: LiveData<Float> get() = _accelerometerSensorValue

    private val _proximitySensorValue = MutableLiveData<Float>()
    val proximitySensorValue: LiveData<Float> get() = _proximitySensorValue

    private val _gyroscopeSensorValue = MutableLiveData<Float>()
    val gyroscopeSensorValue: LiveData<Float> get() = _gyroscopeSensorValue

    private val sensorManager: SensorManager by lazy {
        application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private val lightSensor: Sensor by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    }

    private val accelerometerSensor: Sensor by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    private val proximitySensor: Sensor by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    }

    private val gyroscopeSensor: Sensor by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val sensorDatabaseHelper = SensorDatabaseHelper(application)




    private val insertDataInterval = 5 * 60 * 1000L

    private suspend fun insertDataPeriodically() {
        while (true) {
            insertSensorData()
            delay(insertDataInterval)
        }
    }

    private val sharedPreferences: SharedPreferences =
        application.getSharedPreferences("SwitchStatePrefs", Context.MODE_PRIVATE)


    init {
        // Start inserting data immediately
        coroutineScope.launch {
            insertDataPeriodically()
        }

        _sensorList.value = listOf(
            SensorItem(R.drawable.ic_sunny, "Light", false, Sensor.TYPE_LIGHT),
            SensorItem(R.drawable.ic_sensor_gyroscope, "Accelerometer", false, Sensor.TYPE_ACCELEROMETER),
            SensorItem(R.drawable.ic_sensor_linear_acceleration, "Proximity", false, Sensor.TYPE_PROXIMITY),
            SensorItem(R.drawable.ic_sensor_proximity, "Gyroscope", false, Sensor.TYPE_GYROSCOPE),

        )
        // Retrieve switch states from shared preferences
        _sensorList.value?.forEach { sensorItem ->
            val switchState = sharedPreferences.getBoolean(getSwitchKey(sensorItem.sensorType), false)
            sensorItem.isActive = switchState
            updateSensorActiveStatus(sensorItem.sensorType, switchState)
        }

        _isLightSensorActive.value = _sensorList.value?.find { it.sensorType == Sensor.TYPE_LIGHT }?.isActive ?: false
        _isAccelerometerSensorActive.value = _sensorList.value?.find { it.sensorType == Sensor.TYPE_ACCELEROMETER }?.isActive ?: false
        _isProximitySensorActive.value = _sensorList.value?.find { it.sensorType == Sensor.TYPE_PROXIMITY }?.isActive ?: false
        _isGyroscopeSensorActive.value = _sensorList.value?.find { it.sensorType == Sensor.TYPE_GYROSCOPE }?.isActive ?: false




    }

    private fun getSwitchKey(sensorType: Int): String {
        return "SwitchState_$sensorType"
    }



    private suspend fun insertSensorData() {
        val currentTimeMillis = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedTimestamp = dateFormat.format(currentTimeMillis)

        _sensorList.value?.forEach { sensorItem ->
            if (sensorItem.isActive) {
                val sensorType = sensorItem.sensorType
                val sensorValue = getSensorValue(sensorType)
                val contentValues = ContentValues().apply {
                    put(SensorDatabaseHelper.COLUMN_SENSOR_TYPE, sensorType)
                    put(SensorDatabaseHelper.COLUMN_SENSOR_VALUE, sensorValue)
                    put(SensorDatabaseHelper.COLUMN_TIMESTAMP, formattedTimestamp)
                }

                withContext(Dispatchers.IO) {
                    val db = sensorDatabaseHelper.writableDatabase
                    db.insert(SensorDatabaseHelper.TABLE_NAME, null, contentValues)
                    db.close()
                }
            }
        }
    }


    private fun getSensorValue(sensorType: Int): Float {
        // Modify this method to get the latest sensor value based on the sensorType
        // You can use the LiveData values or the latest values stored in your ViewModel
        return when (sensorType) {
            Sensor.TYPE_LIGHT -> _lightSensorValue.value ?: 0f
            Sensor.TYPE_ACCELEROMETER -> _accelerometerSensorValue.value ?: 0f
            Sensor.TYPE_PROXIMITY -> _proximitySensorValue.value ?: 0f
            Sensor.TYPE_GYROSCOPE -> _gyroscopeSensorValue.value ?: 0f
            else -> 0f
        }
    }

    private fun updateSensorActiveStatus(sensorType: Int, isActive: Boolean) {
        when (sensorType) {
            Sensor.TYPE_LIGHT -> _isLightSensorActive.value = isActive
            Sensor.TYPE_ACCELEROMETER -> _isAccelerometerSensorActive.value = isActive
            Sensor.TYPE_PROXIMITY -> _isProximitySensorActive.value = isActive
            Sensor.TYPE_GYROSCOPE -> _isGyroscopeSensorActive.value = isActive
            // Add more cases for other sensor types as needed
        }

        coroutineScope.launch {
            if (isActive) {
                startSensor(sensorType)
            } else {
                stopSensor(sensorType)
            }
        }
    }

    private suspend fun startSensor(sensorType: Int) {
        when (sensorType) {
            Sensor.TYPE_LIGHT -> {
                withContext(Dispatchers.IO) {
                    sensorManager.registerListener(
                        this@SensorViewModel,
                        lightSensor,
                        SensorManager.SENSOR_DELAY_NORMAL
                    )
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                withContext(Dispatchers.IO) {
                    sensorManager.registerListener(
                        this@SensorViewModel,
                        accelerometerSensor,
                        SensorManager.SENSOR_DELAY_NORMAL
                    )
                }
            }
            Sensor.TYPE_PROXIMITY -> {
                withContext(Dispatchers.IO) {
                    sensorManager.registerListener(
                        this@SensorViewModel,
                        proximitySensor,
                        SensorManager.SENSOR_DELAY_NORMAL
                    )
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                withContext(Dispatchers.IO) {
                    sensorManager.registerListener(
                        this@SensorViewModel,
                        gyroscopeSensor,
                        SensorManager.SENSOR_DELAY_NORMAL
                    )
                }
            }
        }
    }

    private suspend fun stopSensor(sensorType: Int) {
        when (sensorType) {
            Sensor.TYPE_LIGHT -> {
                withContext(Dispatchers.IO) {
                    sensorManager.unregisterListener(this@SensorViewModel, lightSensor)
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                withContext(Dispatchers.IO) {
                    sensorManager.unregisterListener(this@SensorViewModel, accelerometerSensor)
                }
            }
            Sensor.TYPE_PROXIMITY -> {
                withContext(Dispatchers.IO) {
                    sensorManager.unregisterListener(this@SensorViewModel, proximitySensor)
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                withContext(Dispatchers.IO) {
                    sensorManager.unregisterListener(this@SensorViewModel, gyroscopeSensor)
                }
            }
        }
    }

    fun updateSensorValue(sensorType: Int, value: FloatArray) {
        when (sensorType) {
            Sensor.TYPE_LIGHT -> _lightSensorValue.value = value[0]
            Sensor.TYPE_ACCELEROMETER -> _accelerometerSensorValue.value = value[0]
            Sensor.TYPE_PROXIMITY -> _proximitySensorValue.value = value[0]
            Sensor.TYPE_GYROSCOPE -> _gyroscopeSensorValue.value = value[0]
            // Add more cases for other sensor types as needed
        }
    }

    fun updateActiveStatus(sensorType: Int, isActive: Boolean) {
        val updatedList = _sensorList.value?.toMutableList()
        updatedList?.find { it.sensorType == sensorType }?.let { updatedItem ->
            val updatedItemIndex = updatedList.indexOf(updatedItem)
            val updatedItemCopy = updatedItem.copy(isActive = isActive)
            updatedList[updatedItemIndex] = updatedItemCopy
            _sensorList.value = updatedList!!

            updateSensorActiveStatus(sensorType, isActive)
            Log.d("SensorViewModel", "Switch state updated. SensorType: $sensorType, IsActive: $isActive")


            // Save switch state to shared preferences
            sharedPreferences.edit().putBoolean(getSwitchKey(sensorType), isActive).apply()

            // Immediately insert sensor data when the sensor is activated
            if (isActive) {
                coroutineScope.launch {
                    insertSensorData()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(this)

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for light sensor
    }

    override fun onSensorChanged(event: SensorEvent) {
        val sensorType = event.sensor.type
        when (sensorType) {
            Sensor.TYPE_LIGHT -> {
                if (_isLightSensorActive.value == true) {
                    updateSensorValue(sensorType, event.values)
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                if (_isAccelerometerSensorActive.value == true) {
                    updateSensorValue(sensorType, event.values)
                }
            }
            Sensor.TYPE_PROXIMITY -> {
                if (_isProximitySensorActive.value == true) {
                    updateSensorValue(sensorType, event.values)
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                if (_isGyroscopeSensorActive.value == true) {
                    updateSensorValue(sensorType, event.values)
                }
            }
            // Add more cases for other sensor types as needed
        }
    }

    /*@RequiresApi(Build.VERSION_CODES.O)
    fun startLightSensorService(context: Context) {
        if (_isLightSensorActive.value == true) {
            context.startForegroundService(SensorService.newIntent(context, this))
        }
    }*/

    @RequiresApi(Build.VERSION_CODES.O)
    fun startSensorService(context: Context, sensorType: Int) {
        if (isSensorActive(sensorType)) {
            context.startForegroundService(SensorService.newIntent(context, this, sensorType,lightSensorValue))
        }
    }

    fun stopSensorService(context: Context, sensorType: Int) {
        if (!isSensorActive(sensorType)) {
            val stopServiceIntent = Intent(context, SensorService::class.java)
            context.stopService(stopServiceIntent)
        }
    }

    private fun isSensorActive(sensorType: Int): Boolean {
        return when (sensorType) {
            Sensor.TYPE_LIGHT -> _isLightSensorActive.value ?: false
            Sensor.TYPE_ACCELEROMETER -> _isAccelerometerSensorActive.value ?: false
            Sensor.TYPE_PROXIMITY -> _isProximitySensorActive.value ?: false
            Sensor.TYPE_GYROSCOPE -> _isGyroscopeSensorActive.value ?: false
            else -> false
        }
    }



}
