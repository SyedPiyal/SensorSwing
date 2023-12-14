package com.piyal.sensorswing.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.provider.Settings
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.piyal.sensorswing.MainActivity
import com.piyal.sensorswing.R
import com.piyal.sensorswing.viewmodel.SensorViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext



class SensorService : LifecycleService() {

    private lateinit var sensorManager: SensorManager
    private lateinit var lightSensor: Sensor
    private lateinit var sensorEventListener: SensorEventListener

    private val handler = Handler(Looper.getMainLooper())
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val NOTIFICATION_CHANNEL_ID = "SensorNotificationChannel"
    private val NOTIFICATION_ID = 1
    private var isLightSensorActive: Boolean = false
    private var isProximitySensorActive: Boolean = false
    private var isGyroscopeSensorActive: Boolean = false
    private var isAccelerometerSensorActive: Boolean = false
    private var sensorViewModel: SensorViewModel? = null

    private var startTimeMillis: Long = 0
    private var lastSensorValue: Float = 0f

    private var activeSensorName: String = ""

    private lateinit var lightSensorLiveData: LiveData<Float>
    private var latestLightSensorData: Float = 0f

    fun observeLightSensorLiveData(lightSensorLiveData: LiveData<Float>) {
        this.lightSensorLiveData = lightSensorLiveData
        this.lightSensorLiveData.observeForever { lightSensorValue ->

            updateNotification("Foreground Service", activeSensorName, lightSensorValue)
        }
    }

    companion object {
        private var sensorViewModel: SensorViewModel? = null

        private fun setSensorViewModel(viewModel: SensorViewModel) {
            sensorViewModel = viewModel
        }

        fun newIntent(
            context: Context,
            viewModel: SensorViewModel,
            sensorType: Int,
            lightSensorLiveData: LiveData<Float>
        ): Intent {
            setSensorViewModel(viewModel)
            return Intent(context, SensorService::class.java).apply {
                putExtra(EXTRA_SENSOR_TYPE, sensorType)
                putExtra(EXTRA_LIGHT_SENSOR_DATA, lightSensorLiveData.value ?: 0f)
            }
        }

        const val EXTRA_SENSOR_TYPE = "EXTRA_SENSOR_TYPE"
        const val EXTRA_LIGHT_SENSOR_DATA = "EXTRA_LIGHT_SENSOR_DATA"
    }

    private fun getSensorName(sensorType: Int): String {
        return when (sensorType) {
            Sensor.TYPE_LIGHT -> "Light"
            Sensor.TYPE_ACCELEROMETER -> "Accelerometer"
            Sensor.TYPE_PROXIMITY -> "Proximity"
            Sensor.TYPE_GYROSCOPE -> "Gyroscope"
            // Add more cases for other sensor types as needed
            else -> "Unknown"
        }
    }

    // Update the active sensor name when the sensor changes
    private fun updateActiveSensor(sensorName: String) {
            activeSensorName = sensorName
            updateNotification("Foreground Service", activeSensorName, latestLightSensorData)

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // Observe the LiveData directly
        sensorViewModel?.lightSensorValue?.observe(this) { lightSensorValue ->
            // Update the notification when the LiveData changes
            updateNotification("Foreground Service", activeSensorName, lightSensorValue)

            // Store the latest light sensor data
            latestLightSensorData = lightSensorValue
        }

        // Handle the intent here
        val sensorType = intent?.getIntExtra(EXTRA_SENSOR_TYPE, -1) ?: -1
        val lightSensorData = intent?.getFloatExtra(EXTRA_LIGHT_SENSOR_DATA, 0f) ?: 0f

        // Store the latest light sensor data
        latestLightSensorData = lightSensorData

        if (sensorType == -1) {
            // Handle the case when EXTRA_SENSOR_TYPE is not provided
            stopSelf()
            return START_NOT_STICKY
        }

        // Get the correct sensor name based on the sensor type
        val sensorName = getSensorName(sensorType)

        // ... Existing code ...

        // Start the service in the foreground with the correct sensor name
        startForeground(NOTIFICATION_ID, buildNotification("Foreground Service", sensorName, lightSensorData))

        // Update the active sensor name after starting the service in the foreground
        updateActiveSensor(sensorName)

        // ... Existing code ...

        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        // Observe the LiveData
        observeLightSensorLiveData(sensorViewModel?.lightSensorValue ?: MutableLiveData())

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        sensorEventListener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Not needed for light sensor
            }

            override fun onSensorChanged(event: SensorEvent) {
                Log.d("onSensorChanged", "onSensorChanged called")
                val sensorType = event.sensor.type
                val sensorName = getSensorName(sensorType) // Get the correct sensor name
                when (sensorType) {
                    Sensor.TYPE_LIGHT -> {
                        if (isLightSensorActive) {
                            coroutineScope.launch {
                                sensorViewModel?.updateSensorValue(Sensor.TYPE_LIGHT, event.values)

                                // Update the active sensor name
                                updateActiveSensor(sensorName)

                                checkAndRequestNotificationPermission()

                                // Update the notification with the active sensor name and its current value
                                updateNotification("Light Sensor", sensorName, event.values[0])



                                lastSensorValue = event.values[0]
                            }
                        }
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        if (isAccelerometerSensorActive) {
                            coroutineScope.launch {
                                sensorViewModel?.updateSensorValue(Sensor.TYPE_ACCELEROMETER, event.values)

                                // Update the active sensor name
                                updateActiveSensor(sensorName)

                                checkAndRequestNotificationPermission()

                                // Update the notification with the active sensor name and its current value
                                updateNotification("Accelerometer Sensor", sensorName, event.values[0])

                                lastSensorValue = event.values[0]
                            }
                        }
                    }
                    Sensor.TYPE_PROXIMITY -> {
                        if (isProximitySensorActive) {
                            coroutineScope.launch {
                                sensorViewModel?.updateSensorValue(Sensor.TYPE_PROXIMITY, event.values)

                                // Update the active sensor name
                                updateActiveSensor(sensorName)

                                checkAndRequestNotificationPermission()

                                // Update the notification with the active sensor name and its current value
                                updateNotification("Proximity Sensor", sensorName, event.values[0])

                                lastSensorValue = event.values[0]
                            }
                        }
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        if (isGyroscopeSensorActive) {
                            coroutineScope.launch {
                                sensorViewModel?.updateSensorValue(Sensor.TYPE_GYROSCOPE, event.values)

                                // Update the active sensor name
                                updateActiveSensor(sensorName)

                                checkAndRequestNotificationPermission()

                                // Update the notification with the active sensor name and its current value
                                updateNotification("Gyroscope Sensor", sensorName, event.values[0])

                                lastSensorValue = event.values[0]
                            }
                        }
                    }


                    // Add similar handling for other sensor types
                }
            }

        }

        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                sensorManager.registerListener(
                    sensorEventListener,
                    lightSensor,
                    SensorManager.SENSOR_DELAY_NORMAL,
                )

            }
        }

        // Explicitly update the active sensor after starting the service in the foreground
        //updateActiveSensor("InitialSensor")
        updateActiveSensor(activeSensorName)

        startTimeMillis = System.currentTimeMillis()

        handler.postDelayed(notificationRunnable, 30000)


    }


    private fun buildNotification(title: String, sensorName: String, sensorValue: Float): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentText = "Active Sensor: $sensorName, Value: $sensorValue"

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }


    private fun updateNotification(title: String, sensorName: String, sensorValue: Float) {
        val notification = buildNotification(title, sensorName, sensorValue)
        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.d("updateNotification", "updateNotification called")
    }



    override fun onDestroy() {
        super.onDestroy()

        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                sensorManager.unregisterListener(sensorEventListener)
            }
        }

        handler.removeCallbacks(notificationRunnable)
    }





    private val notificationRunnable = Runnable {
        coroutineScope.launch {
            if (isLightSensorActive) {
                val currentTimeMillis = System.currentTimeMillis()
                val elapsedTimeMillis = currentTimeMillis - startTimeMillis
                val elapsedTimeSeconds = elapsedTimeMillis / 1000

                // Use the latest light sensor data
                val lightSensorData = latestLightSensorData

                sensorViewModel?.updateSensorValue(Sensor.TYPE_LIGHT, floatArrayOf(lightSensorData))

                val notificationContent = "Time: ${elapsedTimeSeconds}s, Value: $lightSensorData lux"
                updateNotification("Foreground Service", activeSensorName, lightSensorData)

                startTimeMillis = currentTimeMillis
            }
        }
    }



    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Light Sensor Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
    }


    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (NotificationManagerCompat.getEnabledListenerPackages(this)
                    .contains(packageName)
            ) {
                // Your app has the POST_NOTIFICATIONS permission
                // Proceed to post notifications
            } else {
                // Your app does not have the POST_NOTIFICATIONS permission
                // Request the permission
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                startActivity(intent)
            }
        }
    }
}

