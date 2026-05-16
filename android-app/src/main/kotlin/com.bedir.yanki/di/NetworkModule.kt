package com.bedir.yanki.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import com.bedir.yanki.YankiSyncServiceGrpc
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGrpcChannel(): ManagedChannel {
        // EMÜLATÖR İÇİN: "10.0.2.2"
        // GERÇEK CİHAZ İÇİN: Bilgisayarınızın güncel yerel IP'si (örn: "10.59.217.212")
        val host = "10.59.217.212"
        val port = 50051
        
        android.util.Log.i("YANKI_NET", "gRPC Sunucusuna Bağlanılıyor: $host:$port")

        return ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .keepAliveTime(30, TimeUnit.SECONDS)
            .keepAliveTimeout(10, TimeUnit.SECONDS)
            .keepAliveWithoutCalls(true)
            .idleTimeout(24, TimeUnit.HOURS)
            .build()
    }

    @Provides
    @Singleton
    fun provideYankiServiceStub(channel: ManagedChannel): YankiSyncServiceGrpc.YankiSyncServiceBlockingStub {
        return YankiSyncServiceGrpc.newBlockingStub(channel)
    }
}
