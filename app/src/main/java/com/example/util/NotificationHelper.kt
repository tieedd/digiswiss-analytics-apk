package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity

object NotificationHelper {
    private const val CHANNEL_ID = "digiswiss_tournament_channel"
    private const val CHANNEL_NAME = "DigiSwiss Tournament Alerts"
    private const val CHANNEL_DESC = "Pembaruan jadwal pairing, timer ronde, dan skor turnamen TCG"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendTournamentNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String
    ) {
        // Disabled per user request to avoid clutter
    }
}
