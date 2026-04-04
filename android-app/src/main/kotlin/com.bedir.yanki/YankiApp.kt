package com.bedir.yanki

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.bedir.yanki.data.remote.sync.SyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class YankiApp : Application(), Configuration.Provider {

    // Hilt'in WorkManager içindeki sınıflara (SyncWorker gibi)
    // nesne enjekte edebilmesi için bu fabrika sınıfı şart.
    @Inject lateinit var workerFactory: HiltWorkerFactory

    // Az önce yazdığımız zamanlayıcı şefimiz
    @Inject lateinit var syncManager: SyncManager

    override fun onCreate() {
        super.onCreate()

        // --- KRİTİK ADIM ---
        // Uygulama açıldığı an senkronizasyon takvimini kuruyoruz.
        // Cihaz kapansa bile Android bu görevi hatırlayacak.
        syncManager.scheduleSync()
    }

    // WorkManager'ın Hilt ile konuşmasını sağlayan yapılandırma
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}