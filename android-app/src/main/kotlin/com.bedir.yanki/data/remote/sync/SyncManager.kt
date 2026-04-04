package com.bedir.yanki.data.remote.sync

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun scheduleSync() {
        // 1. Kısıtlamaları Belirle: Sadece internet varken çalış!
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // İnternet şart
            .setRequiresBatteryNotLow(true) // Pil bitmek üzereyken zorlama (Afet anında kritik!)
            .build()

        // 2. Periyodik İş İsteği Oluştur (Örn: Her 15 dakikada bir kontrol et)
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES, // En az 15 dk olabilir (Android kısıtlaması)
            5, TimeUnit.MINUTES   // Esneklik payı
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL, // Hata alırsan süreyi katlayarak bekle (2, 4, 8 dk...)
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        // 3. İşi Sıraya Koy (Benzersiz isim veriyoruz ki mükerrer kayıt olmasın)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "YANKI_SYNC_JOB",
            ExistingPeriodicWorkPolicy.KEEP, // Eğer zaten varsa eskisiyle devam et (Sıfırlama)
            syncRequest
        )
    }
}