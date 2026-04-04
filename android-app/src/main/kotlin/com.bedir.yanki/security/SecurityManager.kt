package com.bedir.yanki.security

import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.interfaces.Sign
import com.goterl.lazysodium.utils.KeyPair
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityManager @Inject constructor() {

    private val sodium = LazySodiumAndroid(SodiumAndroid())

    /**
     * Kullanıcı için yeni bir Ed25519 anahtar çifti oluşturur.
     * Uygulama ilk kez çalıştırıldığında bir kez çağrılmalıdır.
     */
    fun generateUserKeyPair(): KeyPair {
        return sodium.cryptoSignKeypair()
    }

    /**
     * Verilen veriyi kullanıcının özel anahtarı (private key) ile imzalar.
     * @return 64 bytelık imza (signature)
     */
    fun signData(data: ByteArray, privateKey: ByteArray): ByteArray {
        val signature = ByteArray(Sign.ED25519_BYTES)
        sodium.cryptoSignDetached(signature, data, data.size.toLong(), privateKey)
        return signature
    }

    /**
     * İmzalanmış verinin doğruluğunu gönderenin genel anahtarı (public key) ile kontrol eder.
     */
    fun verifySignature(data: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean {
        return sodium.cryptoSignVerifyDetached(signature, data, data.size, publicKey)
    }
}
