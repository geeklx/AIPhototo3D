package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelDao {
    @Query("SELECT * FROM scanned_models ORDER BY timestamp DESC")
    fun getAllModels(): Flow<List<ModelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: ModelEntity): Long

    @Delete
    suspend fun deleteModel(model: ModelEntity)

    @Query("DELETE FROM scanned_models WHERE id = :id")
    suspend fun deleteModelById(id: Long)
}
