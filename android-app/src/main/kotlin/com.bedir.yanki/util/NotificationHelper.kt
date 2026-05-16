package com.bedir.yanki.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.bedir.yanki.MainActivity
import com.bedir.yanki.data.local.entity.BulletinEntity
import com.bedir.yanki.data.local.entity.EmergencySignalEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannels()
    }

    private fun createChannels() {
        // SOS Kanalı
        val sosChannel = NotificationChannel(
            CHANNEL_SOS,
            "Acil Durum Sinyalleri (SOS)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Çevredeki acil durum sinyallerini bildirir"
            enableLights(true)
            enableVibration(true)
        }

        // Duyuru Kanalı
        val bulletinChannel = NotificationChannel(
            CHANNEL_BULLETIN,
            "Duyuru Panosu",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Yeni eklenen ilan ve duyuruları bildirir"
        }

        notificationManager.createNotificationChannel(sosChannel)
        notificationManager.createNotificationChannel(bulletinChannel)
    }

    fun showBulletinNotification(bulletin: BulletinEntity) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Opsiyonel: Direkt duyuru sayfasına yönlendirme için extra eklenebilir
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val title = when (bulletin.type) {
            "ALERT" -> "⚠️ ÖNEMLİ UYARI: ${bulletin.sender_name}"
            "NEED" -> "🆘 İhtiyaç Bildirimi: ${bulletin.sender_name}"
            else -> "📢 Yeni Duyuru: ${bulletin.sender_name}"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_BULLETIN)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(bulletin.content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(bulletin.post_id.hashCode(), notification)
    }

    fun showSOSNotification(signal: EmergencySignalEntity) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_SOS)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("🚨 YAKINDA SOS SİNYALİ!")
            .setContentText("${signal.emergency_type} yardımı gerekiyor!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(signal.signal_id.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_SOS = "yanki_sos_channel"
        const val CHANNEL_BULLETIN = "yanki_bulletin_channel"
    }
}
