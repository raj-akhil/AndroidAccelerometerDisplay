package com.example.accelerometerdisplayapp // This must exactly match your package name!

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

class MainActivity : AppCompatActivity(), SensorEventListener {

    // Sensor related variables
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null // Declare gyroscope sensor

    // UI elements for Accelerometer
    private lateinit var xValueTextView: TextView
    private lateinit var yValueTextView: TextView
    private lateinit var zValueTextView: TextView

    // UI elements for Gyroscope
    private lateinit var gyroXValueTextView: TextView // Declare gyroscope TextViews
    private lateinit var gyroYValueTextView: TextView
    private lateinit var gyroZValueTextView: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI elements for Accelerometer
        xValueTextView = findViewById(R.id.xValueTextView)
        yValueTextView = findViewById(R.id.yValueTextView)
        zValueTextView = findViewById(R.id.zValueTextView)

        // Initialize UI elements for Gyroscope
        gyroXValueTextView = findViewById(R.id.gyroXValueTextView) // Initialize gyroscope TextViews
        gyroYValueTextView = findViewById(R.id.gyroYValueTextView)
        gyroZValueTextView = findViewById(R.id.gyroZValueTextView)


        // Get the SensorManager system service
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // Get the default accelerometer sensor
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) {
            xValueTextView.text = "Accelerometer not available."
            yValueTextView.text = ""
            zValueTextView.text = ""
        }

        // Get the default gyroscope sensor
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) // Get gyroscope sensor
        if (gyroscope == null) {
            gyroXValueTextView.text = "Gyroscope not available."
            gyroYValueTextView.text = ""
            gyroZValueTextView.text = ""
        }
    }

    override fun onResume() {
        super.onResume()
        // Register the sensor listener for the accelerometer
        accelerometer?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        // Register the sensor listener for the gyroscope
        gyroscope?.also { // Register gyroscope listener
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister all sensor listeners when the activity is not in the foreground
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                xValueTextView.text = String.format("X: %.2f", x)
                yValueTextView.text = String.format("Y: %.2f", y)
                zValueTextView.text = String.format("Z: %.2f", z)
            }
            Sensor.TYPE_GYROSCOPE -> { // Handle gyroscope events
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                gyroXValueTextView.text = String.format("GX: %.2f", x)
                gyroYValueTextView.text = String.format("GY: %.2f", y)
                gyroZValueTextView.text = String.format("GZ: %.2f", z)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used for this simple display app
    }
}
