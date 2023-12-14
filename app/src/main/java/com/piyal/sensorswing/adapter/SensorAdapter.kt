package com.piyal.sensorswing.adapter

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.piyal.sensorswing.R
import com.piyal.sensorswing.data.SensorItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class SensorAdapter(
    private val context: Context,
    private var sensorList: List<SensorItem>,
    private val switchCheckedChangeListener: OnSwitchCheckedChangeListener
) : RecyclerView.Adapter<SensorAdapter.ViewHolder>() {



    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    var onItemClickListener: OnItemClickListener? = null

    interface OnSwitchCheckedChangeListener {
        fun onSwitchCheckedChange(position: Int, isChecked: Boolean)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivSensorIcon: ImageView = itemView.findViewById(R.id.iv_sensor_icon)
        val tvSensorName: TextView = itemView.findViewById(R.id.tv_sensor_name)
        val tvActiveStatus: TextView = itemView.findViewById(R.id.tv_active_stutus)
        val switchCompat: SwitchCompat = itemView.findViewById(R.id.switch_compat)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.sensor_item, parent, false)
        return ViewHolder(view)
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("SwitchStatePrefs", Context.MODE_PRIVATE)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sensorItem = sensorList[position]

        holder.ivSensorIcon.setImageResource(sensorItem.iconResId)
        holder.tvSensorName.text = sensorItem.sensorName
        holder.tvActiveStatus.text = if (sensorItem.isActive) "Running" else "Stopped"
        holder.switchCompat.isChecked = sensorItem.isActive

        holder.switchCompat.setOnCheckedChangeListener(null) // Remove previous listener

        // Set the switch state without triggering the listener
        holder.switchCompat.post {
            holder.switchCompat.isChecked = sensorItem.isActive
            val switchState = sharedPreferences.getBoolean(getSwitchKey(sensorItem.sensorType), false)
        }

        holder.switchCompat.setOnCheckedChangeListener { _, isChecked ->
            switchCheckedChangeListener.onSwitchCheckedChange(position, isChecked)
            sharedPreferences.edit().putBoolean(getSwitchKey(sensorItem.sensorType), isChecked).apply()

        }

        holder.itemView.setOnClickListener {
            onItemClickListener?.onItemClick(position)
        }
    }

    private fun getSwitchKey(sensorType: Int): String {
        return "SwitchState_$sensorType"
    }

    override fun getItemCount(): Int = sensorList.size

    fun updateLightSensorSwitch(isLightSensorActive: Boolean) {
        updateSwitchState(0, isLightSensorActive)
    }

    fun updateAccelerometerSwitch(isAccelerometerSensorActive: Boolean) {
        updateSwitchState(1, isAccelerometerSensorActive)
    }

    fun updateProximitySwitch(isProximitySensorActive: Boolean) {
        updateSwitchState(2, isProximitySensorActive)
    }

    fun updateGyroscopeSwitch(isGyroscopeSensorActive: Boolean) {
        updateSwitchState(3, isGyroscopeSensorActive)
    }


    private fun updateSwitchState(sensorType: Int, isActive: Boolean) {
        CoroutineScope(Dispatchers.Main).launch {
            val position = sensorList.indexOfFirst { it.sensorType == sensorType }
            if (position != -1) {
                sensorList[position].isActive = isActive
                notifyItemChanged(position)

            }
        }
    }

    fun getNumberOfSwitchesTurnedOn(): Int {
        return sensorList.count { it.isActive }
    }
    fun updateData(newData: List<SensorItem>) {
        sensorList = newData
        notifyDataSetChanged()
    }
    fun getSensorType(position: Int): Int {
        return sensorList[position].sensorType
    }



}
