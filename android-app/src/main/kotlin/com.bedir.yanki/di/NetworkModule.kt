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
        // EMÜLATÖR İÇİN KESİN ÇÖZÜM: 10.0.2.2 (Bilgisayarın localhost'una tüneldir)
        // Eğer GERÇEK CİHAZ kullanıyorsanız buraya bilgisayarınızın yerel IP'sini yazın (örn: "192.168.1.50")
        val host = "10.0.2.2"
        
        android.util.Log.i("YANKI_NET", "gRPC Sunucusuna Bağlanılıyor: $host:50051")

        return ManagedChannelBuilder.forAddress(host, 50051)
            .usePlaintext()
            .keepAliveTime(30, java.util.concurrent.TimeUnit.SECONDS)
            .keepAliveWithoutCalls(true)
            .maxInboundMessageSize(10 * 1024 * 1024)
            .build()
    }

    @Provides
    @Singleton
    fun provideYankiServiceStub(channel: ManagedChannel): YankiSyncServiceGrpc.YankiSyncServiceBlockingStub {
        // server.js'deki servis adıyla aynı olduğundan emin ol (YankiSyncService)
        return YankiSyncServiceGrpc.newBlockingStub(channel)
    }
}
