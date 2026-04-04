package com.bedir.yanki.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bedir.yanki.data.local.entity.EmergencySignalEntity

@Dao
interface EmergencySignalDao {
    @Query("SELECT EXISTS(SELECT 1 FROM emergency_signals WHERE signal_id = :signalId)")
    suspend fun isSignalExists(signalId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignal(signal: EmergencySignalEntity)

    @Query("SELECT * FROM emergency_signals ORDER BY timestamp DESC")
    suspend fun getAllSignals(): List<EmergencySignalEntity>

    @Query("SELECT * FROM emergency_signals WHERE is_synced = 0 ORDER BY timestamp DESC LIMIT 10")
    suspend fun getPendingSignals(): List<EmergencySignalEntity>
}
