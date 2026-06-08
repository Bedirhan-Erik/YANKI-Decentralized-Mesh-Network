package com.bedir.yanki.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bedir.yanki.data.local.dao.UserDao
import com.bedir.yanki.data.local.dao.MessageDao
import com.bedir.yanki.data.local.dao.EmergencySignalDao
import com.bedir.yanki.data.local.dao.BulletinDao
import com.bedir.yanki.data.local.entity.UserEntity
import com.bedir.yanki.data.local.entity.MessageEntity
import com.bedir.yanki.data.local.entity.EmergencySignalEntity
import com.bedir.yanki.data.local.entity.BulletinEntity

@Database(
    entities = [UserEntity::class, MessageEntity::class, EmergencySignalEntity::class, BulletinEntity::class],
    version = 6,
    exportSchema = true
)
abstract class YankiDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDao
    abstract fun emergencySignalDao(): EmergencySignalDao
    abstract fun bulletinDao(): BulletinDao

}
