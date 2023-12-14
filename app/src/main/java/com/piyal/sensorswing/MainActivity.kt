package com.piyal.sensorswing


import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.piyal.sensorswing.adapter.SensorAdapter
import com.piyal.sensorswing.database.SensorDatabaseHelper
import com.piyal.sensorswing.databinding.ActivityMainBinding
import com.piyal.sensorswing.repository.ChartRepository
import com.piyal.sensorswing.viewmodel.SensorViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity(), SensorAdapter.OnSwitchCheckedChangeListener {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: SensorViewModel by viewModels()
    private lateinit var sensorAdapter: SensorAdapter
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val CHANNEL_ID = "SensorNotificationChannel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        createNotificationChannel()


        binding.rvSensor.layoutManager = GridLayoutManager(this, 2)

        sensorAdapter = SensorAdapter(this, emptyList(), this)
        binding.rvSensor.adapter = sensorAdapter



        // Observe LiveData in MainActivity
        viewModel.sensorList.observe(this) { updatedSensorList ->
            sensorAdapter.updateData(updatedSensorList)
        }

        // Observe LiveData for light sensor value in MainActivity
        viewModel.lightSensorValue.observe(this) { lightSensorValue ->
            binding.tvLightSensorValue.text = "Light Sensor Value: $lightSensorValue"




        }

        // Observe LiveData for Accelerometer sensor value in MainActivity
        viewModel.accelerometerSensorValue.observe(this) { accelerometerValue ->
            binding.tvAccelerometerSensorValue.text = "Accelerometer Value: $accelerometerValue"

        }

        // Observe LiveData for Proximity sensor value in MainActivity
        viewModel.proximitySensorValue.observe(this) { proximityValue ->
            binding.tvProximitySensorValue.text = "Proximity Value: $proximityValue"
        }

        // Observe LiveData for Gyroscope sensor value in MainActivity
        viewModel.gyroscopeSensorValue.observe(this) { gyroscopeValue ->
            binding.tvGyroscopeValue.text = "Gyroscope Value: $gyroscopeValue"
        }

        // Observe LiveData for Light sensor active status in MainActivity
        viewModel.isLightSensorActive.observe(this) { isLightSensorActive ->
            // Update the Light sensor switch
            sensorAdapter.updateLightSensorSwitch(isLightSensorActive)
        }

        // Observe LiveData for Accelerometer sensor active status in MainActivity
        viewModel.isAccelerometerSensorActive.observe(this) { isAccelerometerSensorActive ->
            // Update the Accelerometer sensor switch
            sensorAdapter.updateAccelerometerSwitch(isAccelerometerSensorActive)
        }

        // Observe LiveData for Proximity sensor active status in MainActivity
        viewModel.isProximitySensorActive.observe(this) { isProximitySensorActive ->
            // Update the Proximity sensor switch
            sensorAdapter.updateProximitySwitch(isProximitySensorActive)
        }

        // Observe LiveData for Gyroscope sensor active status in MainActivity
        viewModel.isGyroscopeSensorActive.observe(this) { isGyroscopeSensorActive ->
            // Update the Gyroscope sensor switch
            sensorAdapter.updateGyroscopeSwitch(isGyroscopeSensorActive)
        }

        // Set item click listener
        sensorAdapter.onItemClickListener = object : SensorAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                // Open ChartActivity with the selected sensor type
                val sensorType = sensorAdapter.getSensorType(position)
                val intent = Intent(this@MainActivity, ChartActivity::class.java)
                intent.putExtra(ChartActivity.EXTRA_SENSOR_TYPE, sensorType)
                startActivity(intent)
            }
        }
        // Initialize  SensorDatabaseHelper
        val sensorDatabaseHelper = SensorDatabaseHelper(applicationContext)

        // Initialize ChartRepository
        ChartRepository.init(sensorDatabaseHelper)
    }



    @RequiresApi(Build.VERSION_CODES.O)
    override fun onSwitchCheckedChange(position: Int, isChecked: Boolean) {
        coroutineScope.launch {
            // Replace viewModel with your actual reference to SensorViewModel
            viewModel.updateActiveStatus(sensorAdapter.getSensorType(position), isChecked)

            if (isChecked) {
                viewModel.startSensorService(this@MainActivity, sensorAdapter.getSensorType(position))
            } else {
                viewModel.stopSensorService(this@MainActivity, sensorAdapter.getSensorType(position))
            }

            updateSwitchCountTextView()
        }
    }

    private fun updateSwitchCountTextView() {
        val numberOfSwitchesTurnedOn = sensorAdapter.getNumberOfSwitchesTurnedOn()
        binding.tvActive.text = "Sensors Turned On: $numberOfSwitchesTurnedOn"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sensor Values",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                lightColor = Color.GREEN
                lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
            }

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.createNotificationChannel(channel)
        }
    }



}
