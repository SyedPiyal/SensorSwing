package com.piyal.sensorswing.repository

import com.github.mikephil.charting.data.Entry
import android.database.Cursor
import android.util.Log
import com.piyal.sensorswing.database.SensorDatabaseHelper
import java.text.SimpleDateFormat
import java.util.*


class ChartRepository {

    companion object {
        private  var sensorDatabaseHelper: SensorDatabaseHelper? = null

        fun init(sensorDatabaseHelper: SensorDatabaseHelper) {
            this.sensorDatabaseHelper = sensorDatabaseHelper
        }
    }

     fun getSensorData(sensorType: Int): List<Entry> {
        val entries = mutableListOf<Entry>()

        val db = sensorDatabaseHelper!!.writableDatabase
        val cursor: Cursor?

        try {
            cursor = db.rawQuery(
                "SELECT ${SensorDatabaseHelper.COLUMN_TIMESTAMP}, ${SensorDatabaseHelper.COLUMN_SENSOR_VALUE}" +
                        " FROM ${SensorDatabaseHelper.TABLE_NAME}" +
                        " WHERE ${SensorDatabaseHelper.COLUMN_SENSOR_TYPE} = ?" +
                        " ORDER BY ${SensorDatabaseHelper.COLUMN_TIMESTAMP} ASC",
                arrayOf(sensorType.toString())
            )

            if (cursor != null) {
                val timestampIndex = cursor.getColumnIndex(SensorDatabaseHelper.COLUMN_TIMESTAMP)
                val valueIndex = cursor.getColumnIndex(SensorDatabaseHelper.COLUMN_SENSOR_VALUE)

                while (cursor.moveToNext()) {
                    if (timestampIndex != -1 && valueIndex != -1) {
                        val timestamp = cursor.getString(timestampIndex)
                        val value = cursor.getFloat(valueIndex)

                        // Convert timestamp to milliseconds
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val date = dateFormat.parse(timestamp)
                        val milliseconds = date?.time ?: 0

                        entries.add(Entry(milliseconds.toFloat(), value))
                    } else {

                    }
                }

                cursor.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }
        return entries
    }

}

