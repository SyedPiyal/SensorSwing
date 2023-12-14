SensorSwing is a sensor app created to get the values from light sensor, proximity sensor, accelerometer, and gyroscope .In the App Screen There is different cardview for each sensor with switch. When user turn on a switch from the Home Screen specific sensor will start working even if user closes it from the task manager it will work in the background. It will be only Off when user turn off the Switch. When a User Click Sensor name card view it goes into a screen with the time series chart corresponding to that sensor. 
The challenges that I have faced:
1.	Record the sensor values in the android sqlite db every 5 minutes, and make time series charts of each of the 4 sensors
2.	showing a notification to let the user know of all the 4 values.

Things that I have learned:
1.	how to work with different sensor
2.	how to use record data to make time series charts
3.	Most Important and difficult part that I have learned was to Run the service in the background and showing a notification 

Tasks that I completed:
1.	Getting the values from light sensor, proximity sensor, accelerometer, and gyroscope, and shows the latest value in 4 different cards in the app screen
2.	Record the sensor values in the android sqlite db every 5 minutes, and make time series charts of each of the 4 sensors. If you click on the sensor value cards from the home screen, it goes into a screen with the time series chart corresponding to that sensor.
3.	Run the service in the background, so even if the user closes it from the task manager, the app keeps running in the background, it should be showing a notification to let the user know . I was only able to show all the sensor name in the notification but only light sensor Live Value in the notification.
