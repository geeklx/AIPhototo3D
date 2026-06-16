package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ModelEntity
import com.example.data.ModelRepository
import com.example.export.ModelExporter
import com.example.math.Triangle3D
import com.example.math.Vertex3D
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

enum class RenderMode {
    TEXTURE,
    WIREFRAME,
    POINT_CLOUD,
    SHADED
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = ModelRepository(application, database.modelDao())

    // List of scanned models in Room
    val historicalModels: StateFlow<List<ModelEntity>> = repository.allModels
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current selected model
    private val _selectedModel = MutableStateFlow<ModelEntity?>(null)
    val selectedModel: StateFlow<ModelEntity?> = _selectedModel.asStateFlow()

    // 3D Mesh States
    private val _vertices = MutableStateFlow<List<Vertex3D>>(emptyList())
    val vertices: StateFlow<List<Vertex3D>> = _vertices.asStateFlow()

    private val _triangles = MutableStateFlow<List<Triangle3D>>(emptyList())
    val triangles: StateFlow<List<Triangle3D>> = _triangles.asStateFlow()

    // Mesh Resolution (Width/Height of grid)
    private val meshResolution = 64

    // UI Configuration States
    private val _renderMode = MutableStateFlow(RenderMode.TEXTURE)
    val renderMode: StateFlow<RenderMode> = _renderMode.asStateFlow()

    private val _depthStrength = MutableStateFlow(0.35f)
    val depthStrength: StateFlow<Float> = _depthStrength.asStateFlow()

    private val _autoRotate = MutableStateFlow(true)
    val autoRotate: StateFlow<Boolean> = _autoRotate.asStateFlow()

    private val _rotationSpeed = MutableStateFlow(1.5f)
    val rotationSpeed: StateFlow<Float> = _rotationSpeed.asStateFlow()

    private val _cameraPitch = MutableStateFlow(-0.35f) // Altitude angle in radians
    val cameraPitch: StateFlow<Float> = _cameraPitch.asStateFlow()

    private val _cameraYaw = MutableStateFlow(0f) // Current orbit angle around model Y
    val cameraYaw: StateFlow<Float> = _cameraYaw.asStateFlow()

    private val _zoomFactor = MutableStateFlow(1.1f)
    val zoomFactor: StateFlow<Float> = _zoomFactor.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // Active loaded photo Bitmap
    private val _currentBitmap = MutableStateFlow<Bitmap?>(null)
    val currentBitmap: StateFlow<Bitmap?> = _currentBitmap.asStateFlow()

    init {
        // Automatically convert the fallback resources or initial state if any are in history
        viewModelScope.launch {
            historicalModels.collect { models ->
                if (models.isNotEmpty() && _selectedModel.value == null) {
                    loadModel(models.first())
                }
            }
        }
    }

    // Set configuration values manually
    fun setRenderMode(mode: RenderMode) {
        _renderMode.value = mode
    }

    fun setDepthStrength(strength: Float) {
        _depthStrength.value = strength
        _currentBitmap.value?.let { regenerateMesh(it) }
    }

    fun setAutoRotate(enabled: Boolean) {
        _autoRotate.value = enabled
    }

    fun setRotationSpeed(speed: Float) {
        _rotationSpeed.value = speed
    }

    fun updateCameraAngles(yawDelta: Float, pitchDelta: Float) {
        _cameraYaw.value = (_cameraYaw.value + yawDelta) % (2f * Math.PI.toFloat())
        // Restrict pitch to prevent upside-down camera flipping
        _cameraPitch.value = maxOf(-1.3f, minOf(1.3f, _cameraPitch.value + pitchDelta))
    }

    fun setZoom(zoom: Float) {
        _zoomFactor.value = maxOf(0.4f, minOf(3.0f, zoom))
    }

    fun incrementRotation() {
        if (_autoRotate.value) {
            val speedFactor = 0.005f * _rotationSpeed.value
            _cameraYaw.value = (_cameraYaw.value + speedFactor) % (2f * Math.PI.toFloat())
        }
    }

    // Loads a historical model
    fun loadModel(model: ModelEntity) {
        _selectedModel.value = model
        _depthStrength.value = model.depthScale
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    BitmapFactory.decodeFile(model.imagePath)
                }
                if (bitmap != null) {
                    _currentBitmap.value = bitmap
                    regenerateMesh(bitmap)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading model bitmap", e)
            }
        }
    }

    // Imports a photo from the system URI picker
    fun importPhoto(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val tempFile = withContext(Dispatchers.IO) {
                        val bytes = inputStream.readBytes()
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                    if (tempFile != null) {
                        saveAndGenerate(tempFile)
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error importing photo", e)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    // Capture photo from Camera directly
    fun onPhotoCaptured(bitmap: Bitmap) {
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                saveAndGenerate(bitmap)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error saving captured photo", e)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    // Deletes a model from database and memory
    fun deleteModel(model: ModelEntity) {
        viewModelScope.launch {
            repository.deleteModel(model)
            if (_selectedModel.value?.id == model.id) {
                _selectedModel.value = null
                _currentBitmap.value = null
                _vertices.value = emptyList()
                _triangles.value = emptyList()
            }
        }
    }

    // Saves the photo to local directory and registers it in database
    private suspend fun saveAndGenerate(bitmap: Bitmap) {
        val title = "3D Scan #${System.currentTimeMillis() % 10000}"
        val savedEntity = repository.insertModel(title, bitmap, _depthStrength.value)
        _selectedModel.value = savedEntity
        _currentBitmap.value = bitmap
        regenerateMesh(bitmap)
    }

    // Core height-map 3D processing algorithm
    private fun regenerateMesh(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.Default) {
            val res = meshResolution
            val scaledBmp = Bitmap.createScaledBitmap(bitmap, res, res, true)
            val tempVertices = mutableListOf<Vertex3D>()
            val tempTriangles = mutableListOf<Triangle3D>()

            // 1. Calculate Vertices from Depth (Luminance)
            for (row in 0 until res) {
                for (col in 0 until res) {
                    // Normalize horizontal grid values to range [-1.0f..1.0f]
                    val x = -1.0f + 2.0f * (col.toFloat() / (res - 1))
                    val y = -1.0f + 2.0f * (row.toFloat() / (res - 1))

                    // Extract colors and calculate light luminance
                    val pixel = scaledBmp.getPixel(col, row)
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF
                    
                    // Standard visual luminance weighting
                    val luminance = 0.299f * r + 0.587f * g + 0.114f * b
                    
                    // Displacement z increases for lighter areas
                    val z = (luminance / 255.0f - 0.5f) * _depthStrength.value

                    tempVertices.add(Vertex3D(x, y, z, r, g, b))
                }
            }

            // 2. BuildWatertight Face Triangular Indices Connecting Vertices
            for (row in 0 until res - 1) {
                for (col in 0 until res - 1) {
                    val tl = row * res + col
                    val tr = row * res + col + 1
                    val bl = (row + 1) * res + col
                    val br = (row + 1) * res + col + 1

                    // Triangle 1
                    tempTriangles.add(Triangle3D(tl, tr, bl))
                    // Triangle 2
                    tempTriangles.add(Triangle3D(tr, br, bl))
                }
            }

            _vertices.value = tempVertices
            _triangles.value = tempTriangles
        }
    }

    // Handles files exporting directly
    fun getExportedString(format: String): String {
        return when (format.uppercase()) {
            "OBJ" -> ModelExporter.exportToOBJ(_vertices.value, _triangles.value)
            "STL" -> ModelExporter.exportToSTL(_vertices.value, _triangles.value)
            "PLY" -> ModelExporter.exportToPLY(_vertices.value, _triangles.value)
            else -> ""
        }
    }
}
