package com.bedir.yanki.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bedir.yanki.data.local.dao.UserDao
import com.bedir.yanki.data.local.dao.MessageDao
import com.bedir.yanki.data.local.dao.EmergencySignalDao
import com.bedir.yanki.data.local.entity.UserEntity
import com.bedir.yanki.data.local.entity.MessageEntity
import com.bedir.yanki.data.local.entity.EmergencySignalEntity

@Database(
    entities = [UserEntity::class, MessageEntity::class, EmergencySignalEntity::class],
    version = 1,
    exportSchema = false
)
abstract class YankiDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDao
    abstract fun emergencySignalDao(): EmergencySignalDao

}
