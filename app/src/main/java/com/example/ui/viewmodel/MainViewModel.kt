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
import com.example.math.Vertex3D
import com.example.math.Triangle3D
import com.example.export.ModelExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.cos
import kotlin.math.sin

data class HotspotConfig(
    val id: String,
    val name: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val description: String
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = ModelRepository(application, database.modelDao())

    // Historical Models in Room db (maps to saved wallpapers/art details)
    val historicalModels: StateFlow<List<ModelEntity>> = repository.allModels
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedModel = MutableStateFlow<ModelEntity?>(null)
    val selectedModel: StateFlow<ModelEntity?> = _selectedModel.asStateFlow()

    // --- First-Person Camera Coordinates ---
    private val _cameraX = MutableStateFlow(0f)
    val cameraX: StateFlow<Float> = _cameraX.asStateFlow()

    private val _cameraY = MutableStateFlow(0f) // Head height level
    val cameraY: StateFlow<Float> = _cameraY.asStateFlow()

    private val _cameraZ = MutableStateFlow(0f)
    val cameraZ: StateFlow<Float> = _cameraZ.asStateFlow()

    private val _cameraYaw = MutableStateFlow(0f) // Horizontal camera angle (facing south)
    val cameraYaw: StateFlow<Float> = _cameraYaw.asStateFlow()

    private val _cameraPitch = MutableStateFlow(0f) // Vertical camera tilt
    val cameraPitch: StateFlow<Float> = _cameraPitch.asStateFlow()

    private val _zoomFactor = MutableStateFlow(1.0f)
    val zoomFactor: StateFlow<Float> = _zoomFactor.asStateFlow()

    // --- Interactive Photo-to-3D Depth Space parameters ---
    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    private val _depthScale = MutableStateFlow(0.4f)
    val depthScale: StateFlow<Float> = _depthScale.asStateFlow()

    private val _renderMode = MutableStateFlow(RenderMode.TEXTURE)
    val renderMode: StateFlow<RenderMode> = _renderMode.asStateFlow()

    private val _materialIndex = MutableStateFlow(0)
    val materialIndex: StateFlow<Int> = _materialIndex.asStateFlow()

    private val _meshVertices = MutableStateFlow<List<Vertex3D>>(emptyList())
    val meshVertices: StateFlow<List<Vertex3D>> = _meshVertices.asStateFlow()

    private val _meshTriangles = MutableStateFlow<List<Triangle3D>>(emptyList())
    val meshTriangles: StateFlow<List<Triangle3D>> = _meshTriangles.asStateFlow()

    // --- House Tour Preferences ---
    private val _currentRoomIndex = MutableStateFlow(4) // 4=Photo 3D mode only
    val currentRoomIndex: StateFlow<Int> = _currentRoomIndex.asStateFlow()

    private val _wallColorIndex = MutableStateFlow(0) // 0=Ivory, 1=Mint, 2=Charcoal, 3=Lavender
    val wallColorIndex: StateFlow<Int> = _wallColorIndex.asStateFlow()

    private val _floorPatternIndex = MutableStateFlow(0) // 0=Wood Parquet, 1=Polished Marble, 2=Grid Terminal
    val floorPatternIndex: StateFlow<Int> = _floorPatternIndex.asStateFlow()

    private val _sceneBrightness = MutableStateFlow(1.0f) // Ambient lighting scale
    val sceneBrightness: StateFlow<Float> = _sceneBrightness.asStateFlow()

    private val _isTvOn = MutableStateFlow(true)
    val isTvOn: StateFlow<Boolean> = _isTvOn.asStateFlow()

    private val _isLampOn = MutableStateFlow(true)
    val isLampOn: StateFlow<Boolean> = _isLampOn.asStateFlow()

    private val _activeHotspotId = MutableStateFlow<String?>(null)
    val activeHotspotId: StateFlow<String?> = _activeHotspotId.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // Active custom wallpaper/artwork loaded in the study frame
    private val _artworkBitmap = MutableStateFlow<Bitmap?>(null)
    val artworkBitmap: StateFlow<Bitmap?> = _artworkBitmap.asStateFlow()

    private val _isTransitioning = MutableStateFlow(false)
    val isTransitioning: StateFlow<Boolean> = _isTransitioning.asStateFlow()

    private var transitionJob: Job? = null

    // Room Hotspots List
    val roomHotspots = mapOf(
        0 to listOf(
            HotspotConfig("living_sofa", "时尚真皮沙发", -2.0f, -0.6f, 2.0f, "【奢华真皮沙发】\n意式极简美学设计，饱满回弹填充。配上定制亮色靠枕，兼顾空间平衡感与极致坐感体验。"),
            HotspotConfig("living_tv", "4K巨幕电视墙", 2.2f, -0.2f, 2.0f, "【75寸超视网膜电视】\n集成高动态HDR显示，内置杜比环绕音响，支持一键随手开关电视画面。")
        ),
        1 to listOf(
            HotspotConfig("bedroom_bed", "羽绒双人床", 13.5f, -0.7f, -1.0f, "【智能亲肤大床】\n选用天然低敏乳胶垫，搭配80支秘鲁棉高品质床品，放松高压下的身心，奢享极致好梦。"),
            HotspotConfig("bedroom_lamp", "智能触控台灯", 17.5f, -0.4f, -1.8f, "【触控式无极台灯】\n支持声控和微光色温调节。在睡前阅读时开启，给居室蒙上温馨而慵懒的人文暖调烛光。")
        ),
        2 to listOf(
            HotspotConfig("balcony_chair", "编织休闲摇椅", -1.5f, -0.7f, 16.5f, "【藤制户外休闲椅】\n优质PE环保编织材质，耐热防雨。夏夜伴着清凉晚风，手捧咖啡眺望远处的群山日落，生活在这里慢了下来。"),
            HotspotConfig("balcony_plants", "空气净化绿植", 1.5f, -0.1f, 14.5f, "【垂直绿植微景观】\n整合金葛、波士顿肾蕨和吊兰吸附甲醛与粉尘，让天然健康的清新负氧离子充盈阳台每一处空气。")
        ),
        3 to listOf(
            HotspotConfig("study_bookshelf", "北欧原木书架", -17.0f, 0.2f, -2.0f, "【全木质榫卯书架】\n精选高硬度红橡木全木件拼装，多宝格结构错落有致，收纳书籍与旅行珍藏，展现艺术底蕴。"),
            HotspotConfig("study_frame", "数码自绘相册", -13.5f, 0.4f, 1.3f, "【壁置高像素画壁】\n用户可通过点击上方 [相册导入] 或 [拍照建模]，将自己拍摄的精彩照片一键投放至此处数码画框，定制独享书房！")
        )
    )

    init {
        // Generate pre-loaded beautiful futuristic gradient photo on startup
        val defaultBmp = createDefaultGradientBitmap()
        _artworkBitmap.value = defaultBmp
        reconstructMesh(defaultBmp)

        // Collect saved DB artwork entries on start
        viewModelScope.launch {
            historicalModels.collect { models ->
                if (models.isNotEmpty() && _selectedModel.value == null) {
                    loadModel(models.first())
                }
            }
        }
    }

    // Creates a gorgeous high-contrast programmatic radial wave to show as fallback
    fun createDefaultGradientBitmap(): Bitmap {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val paint = android.graphics.Paint()
        for (y in 0 until size) {
            val yRatio = y.toFloat() / size
            for (x in 0 until size) {
                val xRatio = x.toFloat() / size
                // Radial peak formula
                val dx = x - size / 2f
                val dy = y - size / 2f
                val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                val factor = kotlin.math.cos(distance * 0.12f) * 0.5f + 0.5f
                
                val r = (yRatio * 150f * factor + 105f).toInt().coerceIn(0, 255)
                val g = (factor * 180f + 75f).toInt().coerceIn(0, 255)
                val b = (xRatio * 180f * factor + 75f).toInt().coerceIn(0, 255)
                
                paint.color = android.graphics.Color.rgb(r, g, b)
                canvas.drawPoint(x.toFloat(), y.toFloat(), paint)
            }
        }
        return bmp
    }

    // High performance background coroutine for photogrammetric 3D depth extraction
    fun reconstructMesh(bitmap: Bitmap) {
        viewModelScope.launch {
            _isGenerating.value = true
            withContext(Dispatchers.Default) {
                try {
                    val size = 50 // 50x50 dynamic grid
                    val w = bitmap.width
                    val h = bitmap.height
                    val verts = ArrayList<Vertex3D>(size * size)
                    
                    for (r in 0 until size) {
                        val yRatio = r.toFloat() / (size - 1)
                        val pixelY = (yRatio * (h - 1)).toInt().coerceIn(0, h - 1)
                        for (c in 0 until size) {
                            val xRatio = c.toFloat() / (size - 1)
                            val pixelX = (xRatio * (w - 1)).toInt().coerceIn(0, w - 1)
                            val pixel = bitmap.getPixel(pixelX, pixelY)
                            
                            val red = (pixel shr 16) and 0xFF
                            val green = (pixel shr 8) and 0xFF
                            val blue = pixel and 0xFF
                            
                            // Grayscale brightness
                            val brightness = (0.299f * red + 0.587f * green + 0.114f * blue) / 255f
                            
                            // Model center projection mapping
                            val x3D = (xRatio - 0.5f) * 4.0f
                            val y3D = (0.5f - yRatio) * 4.0f
                            // Depth based on pixel brightness
                            val z3D = (brightness - 0.5f) * -2.5f // Inverted to project brighter spots forward
                            
                            verts.add(Vertex3D(x3D, y3D, z3D, red, green, blue))
                        }
                    }

                    val tris = ArrayList<Triangle3D>()
                    for (r in 0 until size - 1) {
                        for (c in 0 until size - 1) {
                            val v1 = r * size + c
                            val v2 = r * size + (c + 1)
                            val v3 = (r + 1) * size + c
                            val v4 = (r + 1) * size + (c + 1)
                            
                            tris.add(Triangle3D(v1, v2, v3))
                            tris.add(Triangle3D(v2, v4, v3))
                        }
                    }
                    _meshVertices.value = verts
                    _meshTriangles.value = tris
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Reconstruct mesh failed", e)
                }
            }
            _isGenerating.value = false
        }
    }

    // Set configuration values manually
    fun setWallColor(colorIdx: Int) {
        _wallColorIndex.value = colorIdx
    }

    fun setFloorPattern(patternIdx: Int) {
        _floorPatternIndex.value = patternIdx
    }

    fun setSceneBrightness(brightness: Float) {
        _sceneBrightness.value = maxOf(0.1f, minOf(2.0f, brightness))
    }

    fun setTvState(on: Boolean) {
        _isTvOn.value = on
    }

    fun setLampState(on: Boolean) {
        _isLampOn.value = on
    }

    fun setHotspotId(id: String?) {
        _activeHotspotId.value = id
    }

    fun updateCameraAngles(yawDelta: Float, pitchDelta: Float) {
        if (_isTransitioning.value) return
        _cameraYaw.value = (_cameraYaw.value + yawDelta) % (2f * Math.PI.toFloat())
        // Restrict pitch to prevent upside-down camera flipping
        _cameraPitch.value = maxOf(-1.1f, minOf(1.1f, _cameraPitch.value + pitchDelta))
    }

    fun setZoom(zoom: Float) {
        _zoomFactor.value = maxOf(0.4f, minOf(2.5f, zoom))
    }

    fun setFullscreen(active: Boolean) {
        _isFullscreen.value = active
    }

    fun setDepthScale(scale: Float) {
        _depthScale.value = maxOf(0.01f, minOf(2.0f, scale))
    }

    fun setRenderMode(mode: RenderMode) {
        _renderMode.value = mode
    }

    // First person player movement: translate camera positions relative to direction look angle (yaw)
    fun movePlayer(forward: Float, strafe: Float) {
        if (_isTransitioning.value) return
        val yaw = _cameraYaw.value
        val speed = 0.45f

        // Calculate direction vectors based on camera yaw
        val fx = sin(yaw)
        val fz = cos(yaw)
        val sx = cos(yaw)
        val sz = -sin(yaw)

        val dx = (forward * fx + strafe * sx) * speed
        val dz = (forward * fz + strafe * sz) * speed

        // Keep player strictly bounded near the current room center to avoid walking out into space
        val bounds = 5.2f
        val startX = when (_currentRoomIndex.value) {
            1 -> 15f
            3 -> -15f
            else -> 0f
        }
        val startZ = when (_currentRoomIndex.value) {
            2 -> 15f
            else -> 0f
        }

        val nextX = _cameraX.value + dx
        val nextZ = _cameraZ.value + dz

        if (nextX in (startX - bounds)..(startX + bounds)) {
            _cameraX.value = nextX
        }
        if (nextZ in (startZ - bounds)..(startZ + bounds)) {
            _cameraZ.value = nextZ
        }
    }

    // Linear ease-out glide animation transition between different spaces (Simulated 3-player controller fly-through)
    fun selectRoom(roomIndex: Int) {
        if (roomIndex == _currentRoomIndex.value) return
        _currentRoomIndex.value = roomIndex

        val targetX = when (roomIndex) {
            1 -> 15f
            3 -> -15f
            else -> 0f
        }
        val targetZ = when (roomIndex) {
            2 -> 15f
            else -> 0f
        }
        val targetY = 0f

        // Custom looking angle looking inward for each room
        val targetYaw = when (roomIndex) {
            1 -> Math.PI.toFloat() * 1.5f // Look at Bed
            2 -> 0f // Look at Balcony nature view
            3 -> Math.PI.toFloat() * 1.6f // Look at Book shelf & Frame
            else -> 0f // Look at Sofa
        }

        transitionJob?.cancel()
        transitionJob = viewModelScope.launch {
            _isTransitioning.value = true
            _activeHotspotId.value = null

            val steps = 24
            val startX = _cameraX.value
            val startZ = _cameraZ.value
            val startY = _cameraY.value
            val startYaw = _cameraYaw.value
            val startPitch = _cameraPitch.value

            for (i in 1..steps) {
                val t = i.toFloat() / steps
                // Ease out curve
                val tEase = 1f - (1f - t) * (1f - t)

                _cameraX.value = startX + (targetX - startX) * tEase
                _cameraZ.value = startZ + (targetZ - startZ) * tEase
                _cameraY.value = startY + (targetY - startY) * tEase

                _cameraYaw.value = startYaw + (targetYaw - startYaw) * tEase
                _cameraPitch.value = startPitch + (0f - startPitch) * tEase

                delay(18)
            }

            _cameraX.value = targetX
            _cameraZ.value = targetZ
            _cameraY.value = targetY
            _cameraYaw.value = targetYaw
            _cameraPitch.value = 0f

            _isTransitioning.value = false
        }
    }

    // Loads a historical customized decoration card
    fun loadModel(model: ModelEntity) {
        _selectedModel.value = model
        _depthScale.value = model.depthScale
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    BitmapFactory.decodeFile(model.imagePath)
                }
                if (bitmap != null) {
                    _artworkBitmap.value = bitmap
                    reconstructMesh(bitmap)
                    // Instantly shift to 3D Photo depth space view (Tab Index 4)
                    _currentRoomIndex.value = 4
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading artwork image", e)
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

    // Deletes an artwork model from database
    fun deleteModel(model: ModelEntity) {
        viewModelScope.launch {
            repository.deleteModel(model)
            if (_selectedModel.value?.id == model.id) {
                _selectedModel.value = null
                _artworkBitmap.value = null
                // Load default programmatic wave
                val defaultBmp = createDefaultGradientBitmap()
                _artworkBitmap.value = defaultBmp
                reconstructMesh(defaultBmp)
            }
        }
    }

    // Saves the photo to local directory and registers it as a decoration wallpaper card
    private suspend fun saveAndGenerate(bitmap: Bitmap) {
        val title = "拍摄三维空间 #${System.currentTimeMillis() % 1000}"
        val savedEntity = repository.insertModel(title, bitmap, _depthScale.value)
        _selectedModel.value = savedEntity
        _artworkBitmap.value = bitmap
        reconstructMesh(bitmap)
        // Instantly transition to 3D Photo depth space view (Tab Index 4) so users immediately view their 3D reconstructed photo!
        _currentRoomIndex.value = 4
    }

    // Exports the custom home configurations or photo 3D meshes as metadata script
    fun getExportedString(format: String): String {
        // If in Photo-to-3D mode, export the actual reconstructed 3D mesh points & polygons!
        if (_currentRoomIndex.value == 4) {
            val verts = _meshVertices.value
            val tris = _meshTriangles.value
            return when (format.uppercase()) {
                "OBJ" -> ModelExporter.exportToOBJ(verts, tris)
                "PLY" -> ModelExporter.exportToPLY(verts, tris)
                else -> ModelExporter.exportToSTL(verts, tris)
            }
        }

        val roomNames = listOf("Living Room", "Bedroom", "Balcony", "Study", "Photo-to-3D Space")
        val floorNames = listOf("Teak Wood", "Calacatta Marble", "Virtual Grid")
        val wallColors = listOf("Ivory White", "Mint Green", "Minimalist Gray", "Lavender Clay")
        
        return """
            =========================================
            POLYCRAFT 3D : FIRST-PERSON VIRTUAL HOUSE
            CUSTOM DESIGN EXPORT SCHEMATICS ($format)
            =========================================
            Generated on: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}
            
            [Room Configuration]
            Active Spaces Visited: ${roomNames[_currentRoomIndex.value]}
            Floor Finishing Style: ${floorNames[_floorPatternIndex.value]}
            Wall Coat Paint Color: ${wallColors[_wallColorIndex.value]}
            Ambient Lux Brightness: ${_sceneBrightness.value}x
            
            [Interative Device States]
            Living Room TV active : ${_isTvOn.value}
            Bedroom Desk Lamp lit : ${_isLampOn.value}
            
            [Spatial Head Coordinates]
            Position X : ${_cameraX.value}
            Position Y : ${_cameraY.value}
            Position Z : ${_cameraZ.value}
            Rotation Yaw (rad)   : ${_cameraYaw.value}
            Rotation Pitch (rad) : ${_cameraPitch.value}
            
            * Designed flawlessly using PolyCraft 3D Engine *
        """.trimIndent()
    }
}
