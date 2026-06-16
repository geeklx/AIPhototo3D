# Photo to 3D (PolyCraft 3D)

An elegant, modern Android application that converts standard 2D captured or imported photos into interactive 3D height-displacement meshes in real-time. Designed with a **Clean Minimalism** theme, this application offers an engaging, tactile visual experience paired with comprehensive scan history management.

---

## 🎨 Design Philosophy: Clean Minimalism

The interface is engineered around modern Material Design 3 guidelines, leveraging fluid layouts, generous negative space, and a premium interactive aesthetic:

- **Elegance & Flow**: A soft, comforting background (`#FEF7FF`) serves as the foundation.
- **High-contrast Accents**: Rich indigo elements (`#6750A4`) draw focus to key interactions without cluttering the user interface.
- **Visual Feedback**: The 3D render chamber is encased in an eye-safe dark space (`#211F26`) that supports real-time rotation, vertex calculation metrics overlays, and interactive material preview toggles.

---

## 🚀 Key Features

### 1. Interactive 3D Hologram Viewer
*   **Mesh Projection**: Instantly translates image pixel luminance and multi-dimensional normals into a 3D structural vertex mesh.
*   **Intuitive Camera Orbit**: Drag-and-swipe system coordinates to rotate, tilt, or zoom into generated models.
*   **Automatic Spin (Orbit Spin)**: Tap the orbit switch to make the view automatically rotate, with interactive speed/velocity multipliers.

### 2. Multi-Mode Render Engine
*   **Solid**: Complete light-shaded surface structure.
*   **Wireframe**: Transparent grid vectors exposing the raw algorithmic geometry.
*   **Texture**: Full pixel blending mapping the original photograph onto the displaced depth structure.

### 3. Material Presets (Reflection)
Customize reflections across high-fidelity preset indices:
*   **Matte Sand Clay**: For soft, organic, clay-like feedback.
*   **Bright Bronze**: High-contrast, shiny volumetric modeling.
*   **Minimal Purple**: Signature metallic indigo finish.

### 4. Advanced Displacement Slider
*   Fine-control the **Height Displacement Coefficient** dynamically. See immediate recalculations of depth geometry.

### 5. Local Database Navigation History
*   Capture model profiles directly from the camera or pick photo files from your local gallery.
*   Delete unwanted scans, display key stats (number of generated geometric triangles, scale factors, scan timestamp), or quickly restore previous scans for analysis.

---

## 🛠️ Technical Stack

- **Framework**: Jetpack Compose (100% Kotlin UI layout engine)
- **Architecture**: Modular Model-View-ViewModel (MVVM)
- **Asynchronous Execution**: Kotlin Coroutines & StateFlow state propagation
- **Media Transcoder**: Coil Image Pipeline with dynamic edge cropping
- **Design System**: Material Design 3 (M3) styling system

---

## 📦 How to Build and Run

### Prerequisites
- JDK 17
- Android SDK (API Level 34 or above)
- Gradle 8.2+

### Running the Build
To compile the debug APK of the application from the project root:

```bash
gradle assembleDebug
```

To run built unit tests:
```bash
gradle test
```

---

*Enjoy scanning and modeling with PolyCraft 3D!*
