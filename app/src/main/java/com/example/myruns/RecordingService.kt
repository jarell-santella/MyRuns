package com.example.myruns

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.lang.reflect.Type
import java.util.concurrent.ArrayBlockingQueue
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.properties.Delegates

class RecordingService : Service(), LocationListener, SensorEventListener {
    companion object {
        val TRACKING_MSG_INT_VALUE = 0
        val CLASSIFYING_MSG_INT_VALUE = 1
        val AUTOMATIC_ACTIVITY_KEY = "automatic_activity_key"

        val DURATION_KEY = "duration_key"
        val DISTANCE_KEY = "distance_key"
        val AVG_SPEED_KEY = "avg_speed_key"
        val CUR_SPEED_KEY = "cur_speed_key"
        val CLIMB_KEY = "climb_key"
        val CALORIES_KEY = "calories_key"
        val LOCATION_LIST_KEY = "location_list_key"
    }

    private lateinit var  binder: ServiceBinder
    private var msgHandler: Handler? = null

    private lateinit var notificationManager: NotificationManager
    private var NOTIFICATION_ID by Delegates.notNull<Int>()
    private lateinit var CHANNEL_ID: String
    private lateinit var CHANNEL_NAME: String

    private lateinit var locationManager: LocationManager
    private lateinit var locations: ArrayList<LatLng>
    private lateinit var lastLocation: Location
    private var mapInit = false

    private var startTime = 0L
    private var currentTime = 0L
    private var lastTime = 0L

    private var duration: Double = 0.0
    private var distance: Double = 0.0
    private var avgSpeed: Double = 0.0
    private var curSpeed: Double = 0.0
    private var climb: Double = 0.0
    private var calories: Double = 0.0

    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private var ACCELEROMETER_BUFFER_CAPACITY by Delegates.notNull<Int>()
    private var ACCELEROMETER_BLOCK_CAPACITY by Delegates.notNull<Int>()
    private var classifierJob: Job? = null
    private lateinit var accelerationData: ArrayBlockingQueue<Double>
    private var runClassifier = false
    private var sensorInit = false

    private var activityType: Int = -1

    override fun onCreate() {
        super.onCreate()

        msgHandler = null

        binder = ServiceBinder()
        locations = ArrayList()

        NOTIFICATION_ID = resources.getInteger(R.integer.notification_id)
        CHANNEL_ID = resources.getString(R.string.channel_id_key)
        CHANNEL_NAME = resources.getString(R.string.channel_name)
    }

    inner class ServiceBinder : Binder() {
        fun setMsgHandler(msgHandler: Handler) {
            // Once message handler is set, start finding locations
            this@RecordingService.msgHandler = msgHandler
            initLocationManager()
            initSensorManager()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        var notificationText = getString(R.string.tracking_notification_text)

        if (intent != null) {
            // To be performed if the user chooses "Automatic" as input type in StartFragment
            // Otherwise, saves resources and the classification of the activity type won't be performed (i.e. only track user's location, don't classify their activity)
            if (intent.getIntExtra(getString(R.string.input_key), -1) == 2) {
                ACCELEROMETER_BUFFER_CAPACITY = resources.getInteger(R.integer.accelerometer_buffer_capacity)
                ACCELEROMETER_BLOCK_CAPACITY = resources.getInteger(R.integer.accelerometer_block_capacity)

                accelerationData = ArrayBlockingQueue<Double>(ACCELEROMETER_BLOCK_CAPACITY)
                notificationText = getString(R.string.tracking_and_classifying_notification_text)
                runClassifier = true

                var finishedClassifying = true
                classifierJob = CoroutineScope(Default).launch {
                    while (runClassifier && finishedClassifying) {
                        finishedClassifying = false
                        finishedClassifying = classifyActivityType()
                    }
                }
            }
            else {
                runClassifier = false
            }
        }

        // Create notification on start for the foreground service
        val pendingIntent = Intent(this, MapActivity::class.java).let {
                notificationIntent -> PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(notificationText)
            .setSmallIcon(R.drawable.ic_myruns)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            val notificationChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        // Start foreground service with this notification
        startForeground(NOTIFICATION_ID, notification)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        msgHandler = null
        return true
    }

    // Initialize LocationManager
    @SuppressLint("MissingPermission")
    private fun initLocationManager() {
        try {
            locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            val criteria = Criteria()
            criteria.accuracy = Criteria.ACCURACY_FINE
            val provider : String? = locationManager.getBestProvider(criteria, true)
            if(provider != null) {
                val location = locationManager.getLastKnownLocation(provider)
                locationManager.requestLocationUpdates(provider, 10, 0f, this)
                if (location != null) {
                    onLocationChanged(location)
                }
            }
        }
        catch (_: SecurityException) {
        }
    }

    override fun onLocationChanged(location: Location) {
        // Skip first reading (first reading is not always accurate)
        if (mapInit) {
            if (runClassifier && !sensorInit) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST)
                sensorInit = true
            }
            val lat = location.latitude
            val lng = location.longitude
            val latLng = LatLng(lat, lng)

            // Add latest location to locations list
            locations.add(latLng)

            if (::lastLocation.isInitialized) {
                currentTime = System.currentTimeMillis()
                duration = ((currentTime - startTime) / 1000).toDouble()

                distance += lastLocation.distanceTo(location)
                avgSpeed = distance
                if (duration != 0.0) {
                    avgSpeed = distance / duration
                }

                curSpeed = lastLocation.distanceTo(location).toDouble()
                if (((currentTime - lastTime) / 1000).toDouble() != 0.0) {
                    curSpeed = lastLocation.distanceTo(location).toDouble() / ((currentTime - lastTime) / 1000).toDouble()
                }

                climb = lastLocation.altitude - location.altitude

                // Assume 0.06 calories burned every 1 m (60 calories burned every 1 km)
                calories = distance * 0.06
            }

            // Record starting time once there is exactly one location in the locations ArrayList
            if (locations.size == 1) {
                startTime = System.currentTimeMillis()
            }

            lastLocation = location
            lastTime = System.currentTimeMillis()

            // Send tracking message to MapActivityViewModel when location has changed, therefore updating user's location as well as several other statistics of the user's exercise
            sendTrackingMessage()
        }
        mapInit = true
    }

    // Prevent crashes when location services permissions turned on and off while service is running
    override fun onProviderEnabled(provider: String) {
    }

    override fun onProviderDisabled(provider: String) {
    }

    // Send tracking message to MapActivityViewModel updating the user's location as well as several other statistics of the user's exercise
    private fun sendTrackingMessage() {
        try {
            if(msgHandler != null) {
                val type: Type = object : TypeToken<ArrayList<LatLng>>() {}.type

                val bundle = Bundle()
                bundle.putDouble(DURATION_KEY, duration)
                bundle.putDouble(DISTANCE_KEY, distance)
                bundle.putDouble(AVG_SPEED_KEY, avgSpeed)
                bundle.putDouble(CUR_SPEED_KEY, curSpeed)
                bundle.putDouble(CLIMB_KEY, climb)
                bundle.putDouble(CALORIES_KEY, calories)
                bundle.putString(LOCATION_LIST_KEY, Gson().toJson(locations, type))

                val message = msgHandler!!.obtainMessage()
                message.data = bundle
                message.what = TRACKING_MSG_INT_VALUE
                msgHandler!!.sendMessage(message)
            }
        }
        catch (_: Throwable) {
        }
    }

    // Initialize SensorManager
    private fun initSensorManager() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        // Get acceleration data from accelerometer and calculate the magnitude of acceleration
        // Put magnitude of acceleration into the acceleration data
        if (sensorEvent != null && sensorEvent.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            val x = sensorEvent.values[0].toDouble()
            val y = sensorEvent.values[1].toDouble()
            val z = sensorEvent.values[2].toDouble()

            val magnitude = sqrt(x.pow(2) + y.pow(2) + z.pow(2))

            try {
                accelerationData.add(magnitude)
            }
            catch (_: IllegalStateException) {
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    private fun classifyActivityType(): Boolean {
        val featureVector = ArrayList<Double>(ACCELEROMETER_BLOCK_CAPACITY + 1)
        val x = DoubleArray(ACCELEROMETER_BLOCK_CAPACITY)
        val y = DoubleArray(ACCELEROMETER_BLOCK_CAPACITY)

        for (i in 0 until ACCELEROMETER_BLOCK_CAPACITY) {
            if (!runClassifier) {
                return false
            }

            // Put acceleration data into x
            x[i] = accelerationData.take().toDouble()
        }

        accelerationData.clear()

        // Find maximum of the 64 data points
        val max = x.maxOrNull() ?: 0.0
        // Manipulate x and y
        FFT(ACCELEROMETER_BLOCK_CAPACITY).fft(x, y)

        for (i in 0 until ACCELEROMETER_BLOCK_CAPACITY) {
            if (!runClassifier) {
                return false
            }

            // Add manipulated x and y into feature vector
            val magnitude = sqrt(x[i].pow(2) + y[i].pow(2))
            featureVector.add(magnitude)
        }

        // Add max into feature vector
        featureVector.add(max)

        // Classify activity by feeding the feature vector into the classifier
        // Classifier is 0 when standing, 1 when walking, 2 when running, and 3 when it's something else
        val classifiedActivityType = WekaClassifier.classify(featureVector.toArray()).toInt()
        activityType = when (classifiedActivityType) {
            0 -> 2
            1 -> 1
            2 -> 0
            else -> -1
        }

        sendClassifyingMessage()

        return true
    }

    // Send classifying message to MapActivityViewModel updating the user's activity type
    private fun sendClassifyingMessage() {
        try {
            if (msgHandler != null) {
                val bundle = Bundle()
                bundle.putInt(AUTOMATIC_ACTIVITY_KEY, activityType)

                val message = msgHandler!!.obtainMessage()
                message.data = bundle
                message.what = CLASSIFYING_MSG_INT_VALUE
                msgHandler!!.sendMessage(message)
            }
        }
        catch (_: Throwable) {
        }
    }

    // Called when user swipes up to kill app
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        onUnbind(null)
        onDestroy()
    }

    // Clean up service when service needs to be killed
    override fun onDestroy() {
        super.onDestroy()

        msgHandler = null

        locations.clear()
        if (runClassifier) {
            classifierJob!!.cancel()
            classifierJob = null
            accelerationData.clear()
        }

        mapInit = false
        runClassifier = false
        sensorInit = false

        notificationManager.cancel(NOTIFICATION_ID)
        if (locationManager != null) {
            locationManager.removeUpdates(this)
        }
        if (sensorManager != null) {
            sensorManager.unregisterListener(this)
        }

        stopForeground(true)
        stopSelf()
    }
}