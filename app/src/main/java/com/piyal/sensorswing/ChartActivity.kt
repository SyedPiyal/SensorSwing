package com.piyal.sensorswing

import android.graphics.Color
import android.hardware.Sensor
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.piyal.sensorswing.viewmodel.ChartViewModel



class ChartActivity : AppCompatActivity() {

    private lateinit var lineChart: LineChart
    private lateinit var chartViewModel: ChartViewModel
    private var sensorType: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chart)

        lineChart = findViewById(R.id.lineChart)
        chartViewModel = ViewModelProvider(this)[ChartViewModel::class.java]

        // Retrieve the sensor type from the intent
        sensorType = intent.getIntExtra(EXTRA_SENSOR_TYPE, -1)

        if (sensorType != -1) {
            // Set up the chart
            setupChart()

            // Load and display sensor data
            chartViewModel.getSensorData(sensorType) { data ->
                updateChart(data)
            }
        } else {
            // Handle the case where sensorType is not provided
            showToast("Invalid sensor type")
            finish()
        }
    }

    private fun setupChart() {
        // Customize chart settings as needed
        lineChart.description.isEnabled = false
        lineChart.setTouchEnabled(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(true)
        lineChart.setPinchZoom(true)

        // Customize the legend
        val legend = lineChart.legend
        legend.isEnabled = true
        legend.textSize = 12f
        legend.formSize = 10f
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT

        // Customize the X-axis
        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textSize = 12f
        xAxis.textColor = Color.BLACK
        xAxis.setDrawAxisLine(true)
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f // Set granularity as needed
        // Rotate the X-axis labels by a specified angle
        xAxis.labelRotationAngle = 45f // Adjust the angle as needed

        // Customize the left Y-axis
        val yAxisLeft = lineChart.axisLeft
        yAxisLeft.textSize = 12f
        yAxisLeft.textColor = Color.BLACK
        yAxisLeft.setDrawAxisLine(true)
        yAxisLeft.setDrawGridLines(true)

        //new
        // Customize the right Y-axis and hide labels
        //val yAxisRight = lineChart.axisRight
        //yAxisRight.isEnabled = false  // Disable the right Y-axis



        // Example customization for LineDataSet
        val dataSet = LineDataSet(null, "Sensor Values")
        dataSet.color = Color.BLUE
        dataSet.lineWidth = 2f
        dataSet.setCircleColor(Color.RED)
        dataSet.setDrawValues(true)
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 10f

        // Example animation
        lineChart.animateX(1000, Easing.EaseInOutExpo)

        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.invalidate()

        // Customize the X-axis and Y-axis as needed

        // Set a label for the chart based on the sensor type
        val label = when (sensorType) {
            Sensor.TYPE_LIGHT -> "Light Sensor Data"
            Sensor.TYPE_ACCELEROMETER -> "Accelerometer Data"
            Sensor.TYPE_PROXIMITY -> "Proximity Sensor Data"
            Sensor.TYPE_GYROSCOPE -> "Gyroscope Data"
            else -> "Sensor Data"
        }
        lineChart.description.text = label
    }

    private fun updateChart(data: List<Entry>) {
        if (data.isNotEmpty()) {
            val dataSet = LineDataSet(data, "Sensor Values")
            // Customize the dataset as needed

            val lineData = LineData(dataSet)
            lineChart.data = lineData
            lineChart.invalidate()
        } else {
            showToast("No chart data available")
            Log.d("ChartActivity", "No chart data available for sensorType: $sensorType")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_SENSOR_TYPE = "extra_sensor_type"
    }
}
