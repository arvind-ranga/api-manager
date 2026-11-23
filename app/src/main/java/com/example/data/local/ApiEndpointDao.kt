package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiEndpointDao {
    @Query("SELECT * FROM api_endpoints ORDER BY updatedAt DESC, id DESC")
    fun observeAll(): Flow<List<ApiEndpointEntity>>

    @Query("SELECT * FROM api_endpoints ORDER BY updatedAt DESC, id DESC")
    suspend fun getAllOnce(): List<ApiEndpointEntity>

    @Query("SELECT * FROM api_endpoints WHERE id = :id")
    suspend fun getById(id: Long): ApiEndpointEntity?

    @Query("SELECT * FROM api_endpoints WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): ApiEndpointEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ApiEndpointEntity): Long

    @Upsert
    suspend fun upsertAll(entities: List<ApiEndpointEntity>)

    @Update
    suspend fun update(entity: ApiEndpointEntity)

    @Query("DELETE FROM api_endpoints WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM api_endpoints WHERE remoteId = :remoteId")
    suspend fun deleteByRemoteId(remoteId: String)
}
