package com.bedir.yanki.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.bedir.yanki.data.local.entity.UserEntity

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE user_id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE user_id = :userId")
    fun getUserFlow(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users ORDER BY last_seen DESC")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>

    @Query("UPDATE users SET last_seen = :timestamp, last_mac = :macAddress, last_rssi = :rssi WHERE user_id = :userId")
    suspend fun updateLastSeen(userId: String, timestamp: Long, macAddress: String, rssi: Int)

    @Query("SELECT * FROM users WHERE last_mac = :mac LIMIT 1")
    suspend fun getUserByMac(mac: String): UserEntity?

    @Query("DELETE FROM users WHERE user_id = :userId")
    suspend fun deleteUserById(userId: String)

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}
