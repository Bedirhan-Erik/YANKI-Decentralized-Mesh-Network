package com.bedir.yanki.security

import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.interfaces.Sign
import com.goterl.lazysodium.utils.KeyPair
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityManager @Inject constructor() {

    private var sodiumInstance: LazySodiumAndroid? = null

    private fun getSodium(): LazySodiumAndroid? {
        if (sodiumInstance == null) {
            try {
                sodiumInstance = LazySodiumAndroid(SodiumAndroid())
                android.util.Log.d("YANKI_SECURITY", "Sodium başarıyla başlatıldı.")
            } catch (e: Throwable) {
                android.util.Log.e("YANKI_SECURITY", "Sodium başlatılamadı (Kritik): ${e.message}")
                if (e is UnsatisfiedLinkError) e.printStackTrace()
            }
        }
        return sodiumInstance
    }

    /**
     * Kullanıcı için yeni bir Ed25519 anahtar çifti oluşturur.
     */
    fun generateUserKeyPair(): com.goterl.lazysodium.utils.KeyPair? {
        return try {
            getSodium()?.cryptoSignKeypair()
        } catch (e: Exception) {
            android.util.Log.e("YANKI_SECURITY", "KeyPair oluşturma hatası: ${e.message}")
            null
        }
    }

    fun signData(data: ByteArray, privateKey: ByteArray): ByteArray {
        return try {
            val s = getSodium()
            if (s == null || privateKey.size < 32) return ByteArray(64)
            
            val signature = ByteArray(64)
            val success = s.cryptoSignDetached(signature, data, data.size.toLong(), privateKey)
            if (success) signature else ByteArray(64)
        } catch (e: Throwable) {
            android.util.Log.e("YANKI_SECURITY", "İmzalama hatası (Fatal): ${e.message}")
            ByteArray(64)
        }
    }

    /**
     * İmzayı doğrular.
     */
    fun verifySignature(data: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean {
        return try {
            val s = getSodium()
            if (s == null || signature.size != 64 || publicKey.size < 32) return false
            s.cryptoSignVerifyDetached(signature, data, data.size, publicKey)
        } catch (e: Throwable) {
            android.util.Log.e("YANKI_SECURITY", "Doğrulama hatası: ${e.message}")
            false
        }
    }
}
