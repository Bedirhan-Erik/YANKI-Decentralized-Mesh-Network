package com.bedir.yanki.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import com.bedir.yanki.YankiSyncServiceGrpc
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGrpcChannel(): ManagedChannel {
        // 10.0.2.2 -> Android Emülatörünün bilgisayarındaki 'localhost'a bakma yoludur.
        // Eğer gerçek telefon kullanıyorsan bilgisayarının yerel IP'sini (örn: 192.168.1.x) yazmalısın.
        return ManagedChannelBuilder.forAddress("10.0.2.2", 50051)
            .usePlaintext() // Test aşamasında SSL/TLS sertifikası olmadan bağlanmak için
            .build()
    }

    @Provides
    @Singleton
    fun provideYankiServiceStub(channel: ManagedChannel): YankiSyncServiceGrpc.YankiSyncServiceBlockingStub {
        // server.js'deki servis adıyla aynı olduğundan emin ol (YankiSyncService)
        return YankiSyncServiceGrpc.newBlockingStub(channel)
    }
}