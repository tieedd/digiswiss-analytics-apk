package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TimerManager {
    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    fun setTimerSeconds(seconds: Int) {
        _timerSeconds.value = seconds
    }
    
    fun setTimerRunning(isRunning: Boolean) {
        _isTimerRunning.value = isRunning
    }
}

class RoundTimerService : Service() {

    private val CHANNEL_ID = "timer_channel"
    private val NOTIFICATION_ID = 1001
    private var timerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "STOP_TIMER") {
            TimerManager.setTimerRunning(false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val seconds = intent?.getIntExtra("SECONDS", 0) ?: 0
        if (!TimerManager.isTimerRunning.value) {
            TimerManager.setTimerSeconds(seconds)
            TimerManager.setTimerRunning(true)
            startForeground(NOTIFICATION_ID, buildNotification(seconds))
            startTimer()
        }

        return START_STICKY
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = CoroutineScope(Dispatchers.Default).launch {
            while (TimerManager.isTimerRunning.value && TimerManager.timerSeconds.value > 0) {
                delay(1000)
                val currentSeconds = TimerManager.timerSeconds.value - 1
                TimerManager.setTimerSeconds(currentSeconds)
                
                // Update notification
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, buildNotification(currentSeconds))
            }
            
            if (TimerManager.timerSeconds.value <= 0) {
                TimerManager.setTimerRunning(false)
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, buildNotification(0, true))
                stopForeground(STOP_FOREGROUND_DETACH)
            }
        }
    }

    private fun buildNotification(seconds: Int, timeUp: Boolean = false): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = if (timeUp) {
            "Waktu ronde telah habis! Jalankan aturan 3 giliran tambahan."
        } else {
            val mins = seconds / 60
            val secs = seconds % 60
            "Waktu tersisa: %02d:%02d".format(mins, secs)
        }

        val title = if (timeUp) "WAKTU HABIS!" else "Ronde Sedang Berjalan"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(!timeUp)
            .setSilent(true) // Keep it silent so it doesn't beep every second
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer Ronde",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Waktu yang tersisa untuk ronde turnamen"
                setSound(null, null)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
