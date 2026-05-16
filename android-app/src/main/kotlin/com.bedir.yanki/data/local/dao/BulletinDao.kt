package com.bedir.yanki.data.local.dao

import androidx.room.*
import com.bedir.yanki.data.local.entity.BulletinEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BulletinDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: BulletinEntity)

    @Query("SELECT * FROM bulletins ORDER BY timestamp DESC")
    fun getAllPostsFlow(): Flow<List<BulletinEntity>>

    @Query("SELECT * FROM bulletins WHERE is_synced = 0")
    suspend fun getUnsyncedPosts(): List<BulletinEntity>

    @Query("UPDATE bulletins SET is_synced = 1 WHERE post_id = :postId")
    suspend fun markAsSynced(postId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM bulletins WHERE post_id = :postId)")
    suspend fun isPostExists(postId: String): Boolean

    @Query("DELETE FROM bulletins WHERE timestamp < :threshold")
    suspend fun deleteOldPosts(threshold: Long)
}
