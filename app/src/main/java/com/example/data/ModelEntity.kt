package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanned_models")
data class ModelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val imagePath: String,            // Path to the saved captured image
    val depthScale: Float = 0.4e-1f,  // Default depth strength
    val timestamp: Long = System.currentTimeMillis()
)
