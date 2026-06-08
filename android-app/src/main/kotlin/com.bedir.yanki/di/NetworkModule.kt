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
        return ManagedChannelBuilder.forAddress("34.10.209.207", 50051)
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
