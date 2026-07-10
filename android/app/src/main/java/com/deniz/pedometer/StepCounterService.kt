package com.deniz.pedometer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Runs in the foreground and listens to the device's hardware step counter
 * (TYPE_STEP_COUNTER), which reports a cumulative count since last reboot.
 * We convert that into "steps today" by remembering the sensor's value at
 * the start of the day, then periodically write the running total to the
 * local Room DB and push unsynced days to the server.
 */
class StepCounterService : Service(), SensorEventListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var sensorManager: SensorManager
    private lateinit var db: AppDatabase
    private var baselineForToday: Float? = null
    private var currentDay: String = today()

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getInstance(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        startForeground(NOTIFICATION_ID, buildNotification(0))

        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            stopSelf() // Device has no step counter hardware
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        val total = event.values[0]
        val day = today()

        if (day != currentDay) {
            // Rolled over to a new day; reset baseline
            currentDay = day
            baselineForToday = total
        }
        if (baselineForToday == null) {
            baselineForToday = total
        }

        val stepsToday = (total - (baselineForToday ?: total)).toInt().coerceAtLeast(0)
        updateNotification(stepsToday)

        scope.launch {
            db.stepDao().upsert(
                StepRecordEntity(deviceId = deviceId(), day = day, steps = stepsToday, synced = false)
            )
            syncUnsyncedRecords()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private suspend fun syncUnsyncedRecords() {
        val token = TokenStore(this).getToken() ?: return
        val unsynced = db.stepDao().getUnsynced()
        if (unsynced.isEmpty()) return

        val entries = unsynced.map { StepEntryDto(it.deviceId, it.day, it.steps) }
        try {
            val response = ApiClient.service.syncSteps("Bearer $token", StepSyncRequest(entries))
            if (response.isSuccessful) {
                unsynced.forEach { db.stepDao().markSynced(it.deviceId, it.day) }
            }
        } catch (_: Exception) {
            // No connectivity; will retry on next sensor event or WorkManager tick
        }
    }

    private fun buildNotification(steps: Int): Notification {
        val channelId = "pedometer_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Adım Sayar", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val openAppIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Adım Sayar çalışıyor")
            .setContentText("Bugün: $steps adım")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(steps: Int) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(steps))
    }

    private fun deviceId(): String =
        Build.MODEL + "-" + (Build.FINGERPRINT.hashCode().toString())

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
