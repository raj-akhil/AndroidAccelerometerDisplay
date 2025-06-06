package com.example.accelerometerdisplayapp

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.Switch
import android.widget.TextView
import androidx.activity.ComponentActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject // To create JSON data
import android.widget.Spinner


class MainActivity : ComponentActivity(), SensorEventListener {

    // Sensor-related variables
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var proximitySensor: Sensor? = null
    private var lightSensor: Sensor? = null
    private lateinit var labelSpinner: Spinner

    // UI elements
    private lateinit var tvAccelerometerX: TextView
    private lateinit var tvAccelerometerY: TextView
    private lateinit var tvAccelerometerZ: TextView
    private lateinit var tvGyroscopeGx: TextView
    private lateinit var tvGyroscopeGy: TextView
    private lateinit var tvGyroscopeGz: TextView
    private lateinit var tvProximity: TextView
    private lateinit var tvLight: TextView
    private lateinit var websocketToggleSwitch: Switch

    private var lastSentTime = System.currentTimeMillis()
    private val sendIntervalMillis = 5L // 20 Hz = 50 ms interval

    // WebSocket-related variables
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    // Current sensor readings (to be sent)
    private var currentAccelX = 0f
    private var currentAccelY = 0f
    private var currentAccelZ = 0f
    private var currentGyroGx = 0f
    private var currentGyroGy = 0f
    private var currentGyroGz = 0f

    private var currentProximity = -1f
    private var currentLight = -1f
    // WebSocket Server Configuration
    // IMPORTANT:
    // For Emulator: use "ws://10.0.2.2:8080" (10.0.2.2 is your host machine's localhost)
    // For Physical Device: use "ws://YOUR_COMPUTER_IP_ADDRESS:8080" (e.g., "ws://192.168.1.100:8080")
    private val WEBSOCKET_URL = "ws://192.168.1.110:8080"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Assuming your layout file is activity_main.xml

        // Initialize UI elements
        tvAccelerometerX = findViewById(R.id.xValueTextView)
        tvAccelerometerY = findViewById(R.id.yValueTextView)
        tvAccelerometerZ = findViewById(R.id.zValueTextView)
        tvGyroscopeGx = findViewById(R.id.gyroXValueTextView)
        tvGyroscopeGy = findViewById(R.id.gyroYValueTextView)
        tvGyroscopeGz = findViewById(R.id.gyroZValueTextView)
        tvProximity = findViewById(R.id.proximityTextView)
        tvLight = findViewById(R.id.lightTextView)
        websocketToggleSwitch = findViewById(R.id.websocket_toggle_switch)
        labelSpinner = findViewById(R.id.activitySpinner)

        // Initialize Sensor Manager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        // Set up the Switch listener
        websocketToggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                connectWebSocket()
                // Register listeners only when switch is ON and we want to send data
                accelerometer?.let { sensorManager.registerListener(this, it, 20_000) }
                gyroscope?.let { sensorManager.registerListener(this, it, 20_000) }
                proximitySensor?.let { sensorManager.registerListener(this, it, 20_000) }
                lightSensor?.let { sensorManager.registerListener(this, it, 20_000) }

                Log.d("WebSocket", "WebSocket sending ON")
            } else {
                disconnectWebSocket()
                // Unregister listeners when switch is OFF
                sensorManager.unregisterListener(this)
                Log.d("WebSocket", "WebSocket sending OFF")
            }
        }
    }

    private fun connectWebSocket() {
        if (webSocket != null) {
            Log.d("WebSocket", "WebSocket already connected or connecting.")
            return
        }

        val request = Request.Builder().url(WEBSOCKET_URL).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                Log.d("WebSocket", "Connection opened: ${response.message}")
                runOnUiThread {
                    // Optional: Update UI to show connection status
                    // tvStatus.text = "Connected!"
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocket", "Receiving: $text")
                // If your Python server sends messages back, you'd handle them here
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Closing: $code / $reason")
                webSocket.close(1000, null) // Explicitly close to ensure proper state
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                Log.e("WebSocket", "Error: ${t.message}", t)
                runOnUiThread {
                    // Optional: Update UI to show error
                    // tvStatus.text = "Error: ${t.message}"
                    websocketToggleSwitch.isChecked = false // Turn off switch if error
                }
                this@MainActivity.webSocket = null // Clear WebSocket reference on failure
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Connection closed: $code / $reason")
                this@MainActivity.webSocket = null // Clear WebSocket reference on close
            }
        })
    }

    private fun disconnectWebSocket() {
        if (webSocket != null) {
            webSocket?.close(1000, "App disconnected")
            webSocket = null
            Log.d("WebSocket", "WebSocket disconnected.")
        } else {
            Log.d("WebSocket", "WebSocket is null, not disconnecting.")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {

        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                currentAccelX = event.values[0]
                currentAccelY = event.values[1]
                currentAccelZ = event.values[2]
                tvAccelerometerX.text = String.format("X: %.2f", currentAccelX)
                tvAccelerometerY.text = String.format("Y: %.2f", currentAccelY)
                tvAccelerometerZ.text = String.format("Z: %.2f", currentAccelZ)
            }
            Sensor.TYPE_GYROSCOPE -> {
                currentGyroGx = event.values[0]
                currentGyroGy = event.values[1]
                currentGyroGz = event.values[2]
                tvGyroscopeGx.text = String.format("Gx: %.2f", currentGyroGx)
                tvGyroscopeGy.text = String.format("Gy: %.2f", currentGyroGy)
                tvGyroscopeGz.text = String.format("Gz: %.2f", currentGyroGz)
            }
            Sensor.TYPE_PROXIMITY -> {
                currentProximity = event.values[0]
                tvProximity.text = String.format("Proximity: %.2f", currentProximity)
            }
            Sensor.TYPE_LIGHT -> {
                currentLight = event.values[0]
                tvLight.text = String.format("Ambient Light: %.2f", currentLight)
            }
        }

        val currentTime = System.currentTimeMillis()
        val deltaTime = currentTime - lastSentTime
        if (deltaTime >= sendIntervalMillis) {
            lastSentTime = currentTime
            val frequency = 1000.0 / deltaTime  // Hz
            // Build your JSON object and send via WebSocket here
            val label = labelSpinner.selectedItem.toString()

            val jsonObject = JSONObject().apply {
                put("timestamp", currentTime)
                put("accelerometer", JSONObject().apply {
                    put("x", currentAccelX)
                    put("y", currentAccelY)
                    put("z", currentAccelZ)
                })
                put("gyroscope", JSONObject().apply {
                    put("gx", currentGyroGx)
                    put("gy", currentGyroGy)
                    put("gz", currentGyroGz)
                })
                put("proximity", currentProximity)
                put("light", currentLight)
                put("label", label)
            }

            if (websocketToggleSwitch.isChecked && webSocket != null) {
                webSocket?.send(jsonObject.toString())
            }
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not typically used for displaying basic sensor data
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure WebSocket is disconnected when the activity is destroyed
        disconnectWebSocket()
        // Unregister sensor listener to prevent memory leaks if activity is destroyed while sending
        sensorManager.unregisterListener(this)
    }
}