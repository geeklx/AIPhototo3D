package com.example.data

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class ModelRepository(private val context: Context, private val modelDao: ModelDao) {
    val allModels: Flow<List<ModelEntity>> = modelDao.getAllModels()

    suspend fun insertModel(title: String, bitmap: Bitmap, depthScale: Float = 0.4f): ModelEntity = withContext(Dispatchers.IO) {
        val fileName = "scan_${UUID.randomUUID()}.jpg"
        val file = File(context.filesDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        val entity = ModelEntity(
            title = title,
            imagePath = file.absolutePath,
            depthScale = depthScale
        )
        val id = modelDao.insertModel(entity)
        entity.copy(id = id)
    }

    suspend fun copyUriToLocalAndInsert(title: String, inputStream: InputStream, depthScale: Float = 0.4f): ModelEntity = withContext(Dispatchers.IO) {
        val fileName = "scan_${UUID.randomUUID()}.jpg"
        val file = File(context.filesDir, fileName)
        FileOutputStream(file).use { out ->
            inputStream.copyTo(out)
        }
        val entity = ModelEntity(
            title = title,
            imagePath = file.absolutePath,
            depthScale = depthScale
        )
        val id = modelDao.insertModel(entity)
        entity.copy(id = id)
    }

    suspend fun deleteModel(model: ModelEntity) = withContext(Dispatchers.IO) {
        val file = File(model.imagePath)
        if (file.exists()) {
            file.delete()
        }
        modelDao.deleteModel(model)
    }
}
