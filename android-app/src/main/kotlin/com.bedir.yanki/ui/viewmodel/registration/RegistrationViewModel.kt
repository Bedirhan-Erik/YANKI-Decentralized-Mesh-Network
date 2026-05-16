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

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val repository: YankiRepository
) : ViewModel() {
    var fullName by mutableStateOf("")
    var username by mutableStateOf("")
    var bloodType by mutableStateOf("")
    var allergies by mutableStateOf("")
    var medications by mutableStateOf("")
    var emergencyContact by mutableStateOf("")

    suspend fun completeRegistration() {
        val newUser = UserEntity(
            user_id = UUID.randomUUID().toString(),
            username = username,
            full_name = fullName,
            public_key = byteArrayOf(), // Will be handled by repository key generation if needed or updated later
            last_seen = System.currentTimeMillis(),
            is_trusted = true,
            blood_type = bloodType,
            allergies = allergies,
            medications = medications,
            emergency_contact = emergencyContact
        )
        repository.saveUser(newUser)
    }
}
