package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object TimerManager {
    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    fun setTimerSeconds(seconds: Int) { _timerSeconds.value = seconds.coerceAtLeast(0) }
    fun setTimerRunning(isRunning: Boolean) { _isTimerRunning.value = isRunning }
}

class RoundTimerService : Service() {

    private val channelId = "timer_channel"
    private val notificationId = 1001

    // Scope milik service, dibatalkan di onDestroy. Versi lama membuat
    // CoroutineScope(Dispatchers.Default) baru setiap kali startTimer() dipanggil
    // tanpa pernah membatalkan scope-nya.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null
    private var lastShownSecond = -1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopTimerAndSelf()
            return START_NOT_STICKY
        }

        val seconds = (intent?.getIntExtra(EXTRA_SECONDS, 0) ?: 0).coerceAtLeast(0)
        if (seconds == 0) {
            stopTimerAndSelf()
            return START_NOT_STICKY
        }

        if (!TimerManager.isTimerRunning.value) {
            TimerManager.setTimerSeconds(seconds)
            TimerManager.setTimerRunning(true)

            // Wajib sejak API 34 kalau targetSdk >= 34: tipe foreground service
            // harus disebutkan saat memanggil startForeground.
            ServiceCompat.startForeground(
                this, notificationId, buildNotification(seconds),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
            )
            startTimer(seconds)
        }
        // START_REDELIVER_INTENT: kalau proses dibunuh, intent dengan durasi
        // dikirim ulang. START_STICKY mengirim intent null sehingga timer
        // restart dengan 0 detik.
        return START_REDELIVER_INTENT
    }

    /**
     * Hitung mundur berbasis deadline, bukan akumulasi delay(1000).
     * Versi lama kehilangan waktu setiap kali coroutine terlambat dijadwalkan —
     * pada ronde 45 menit selisihnya bisa lebih dari satu menit, dan ini
     * timer resmi pertandingan.
     */
    private fun startTimer(initialSeconds: Int) {
        timerJob?.cancel()
        val deadline = SystemClock.elapsedRealtime() + initialSeconds * 1000L

        timerJob = scope.launch {
            while (TimerManager.isTimerRunning.value) {
                val remaining = ((deadline - SystemClock.elapsedRealtime()) / 1000.0)
                    .toInt().coerceAtLeast(0)
                TimerManager.setTimerSeconds(remaining)

                // Notifikasi hanya diperbarui saat detiknya benar-benar berubah.
                if (remaining != lastShownSecond) {
                    lastShownSecond = remaining
                    notificationManager().notify(notificationId, buildNotification(remaining))
                }
                if (remaining <= 0) break
                delay(250)
            }

            if (TimerManager.timerSeconds.value <= 0) {
                TimerManager.setTimerRunning(false)
                notificationManager().notify(notificationId, buildNotification(0, timeUp = true))
                ServiceCompat.stopForeground(this@RoundTimerService, ServiceCompat.STOP_FOREGROUND_DETACH)
                // Versi lama berhenti di sini: service tetap hidup selamanya
                // setelah waktu habis.
                stopSelf()
            }
        }
    }

    private fun stopTimerAndSelf() {
        timerJob?.cancel()
        TimerManager.setTimerRunning(false)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun notificationManager() =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun buildNotification(seconds: Int, timeUp: Boolean = false): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, RoundTimerService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = if (timeUp) {
            "Waktu ronde telah habis. Jalankan aturan 3 giliran tambahan."
        } else {
            "Waktu tersisa: %02d:%02d".format(seconds / 60, seconds % 60)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(if (timeUp) "WAKTU HABIS" else "Ronde Sedang Berjalan")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(!timeUp)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .apply { if (!timeUp) addAction(0, "Hentikan", stopIntent) }
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Timer Ronde", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Waktu yang tersisa untuk ronde turnamen"
                setSound(null, null)
            }
            notificationManager().createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        timerJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "STOP_TIMER"
        const val EXTRA_SECONDS = "SECONDS"
    }
}
