package com.bedir.yanki.ui.viewmodel.registration

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.bedir.yanki.data.local.entity.UserEntity
import com.bedir.yanki.repository.YankiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.*
import javax.inject.Inject
import android.content.Intent
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val repository: YankiRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    var fullName by mutableStateOf("")
    var bloodType by mutableStateOf("")
    var allergies by mutableStateOf("")
    var medications by mutableStateOf("")
    var emergencyContact by mutableStateOf("")
    
    var generatedPublicKey by mutableStateOf("")
    var registrationTimestamp by mutableStateOf(0L)

    suspend fun completeRegistration() {
        // Anahtarların oluşturulduğundan emin olalım
        repository.ensureUserKeys()
        
        val pubKeyHex = repository.sharedPreferences.getString("public_key", "") ?: ""
        generatedPublicKey = pubKeyHex
        registrationTimestamp = System.currentTimeMillis()

        val newUser = UserEntity(
            user_id = UUID.randomUUID().toString(),
            username = fullName, // Kullanıcı adı yerine direkt tam ismi kullanıyoruz
            full_name = fullName,
            public_key = if (pubKeyHex.isNotEmpty()) java.util.Base64.getDecoder().decode(pubKeyHex) else byteArrayOf(),
            last_seen = registrationTimestamp,
            is_trusted = true,
            blood_type = bloodType,
            allergies = allergies,
            medications = medications,
            emergency_contact = emergencyContact
        )
        repository.saveUser(newUser)
        
        // Mesh servisini başlat
        val intent = Intent(context, com.bedir.yanki.services.MeshService::class.java)
        context.startForegroundService(intent)
    }
}
