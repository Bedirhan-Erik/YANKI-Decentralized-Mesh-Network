package com.bedir.yanki.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import com.bedir.yanki.YankiSyncServiceGrpc
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGrpcChannel(): ManagedChannel {
        return ManagedChannelBuilder.forAddress("10.59.217.212", 50051)
            .usePlaintext()
            // Ağ kopukluğunu tespit için: 30s'de bir ping gönder, 10s içinde cevap gelmezse bağlantıyı kapat
            .keepAliveTime(30, TimeUnit.SECONDS)
            .keepAliveTimeout(10, TimeUnit.SECONDS)
            // Aktif RPC yokken ping gönderme (pil tasarrufu); UNAVAILABLE sonrası resetConnectBackoff() yeterli
            .keepAliveWithoutCalls(false)
            .build()
    }

    @Provides
    @Singleton
    fun provideYankiServiceStub(channel: ManagedChannel): YankiSyncServiceGrpc.YankiSyncServiceBlockingStub {
        // server.js'deki servis adıyla aynı olduğundan emin ol (YankiSyncService)
        return YankiSyncServiceGrpc.newBlockingStub(channel)
    }
}
