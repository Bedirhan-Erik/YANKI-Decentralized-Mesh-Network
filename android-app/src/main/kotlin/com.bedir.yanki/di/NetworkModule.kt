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
        // Gerçek cihaz bağlantısı için PC IP adresi kullanılıyor: 172.20.10.4
        return ManagedChannelBuilder.forAddress("172.20.10.4", 50051)
            .usePlaintext()
            .build()
    }

    @Provides
    @Singleton
    fun provideYankiServiceStub(channel: ManagedChannel): YankiSyncServiceGrpc.YankiSyncServiceBlockingStub {
        // server.js'deki servis adıyla aynı olduğundan emin ol (YankiSyncService)
        return YankiSyncServiceGrpc.newBlockingStub(channel)
    }
}