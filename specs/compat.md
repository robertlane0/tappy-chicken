# Tappy Chicken Android: Device Compatibility & Vector Support Specification

This document details the configuration, compatibility layers, and graphic asset systems required to ensure seamless performance going back to Android 8.0 (Oreo) while utilizing modern SDK features and scalable vector drawing.

---

## 1. SDK Target & Tooling Configurations

The project is built using modern Kotlin and the Gradle build system, targeted to balance bleeding-edge OS optimizations with extensive backward compatibility.

| Config Property | Value | Rationale |
|---|---|---|
| **`minSdkVersion`** | `26` | Matches Android 8.0 (Oreo). Covers approximately 95%+ of global active Android hardware. |
| **`targetSdkVersion`** | `35` | Matches Android 15. Standard Google Play Store requirement for target compliance, optimizing performance, memory, and background behaviors. |
| **`compileSdkVersion`** | `35` | Enables compilation using modern platform APIs, permission layers, and UI/UX behaviors. |
| **`kotlinCompilerVersion`** | `2.0.0+` | Uses Kotlin K2 compiler for high performance, smart type-inference, and optimized JVM bytecode generation. |

---

## 2. Java 8+ API Desugaring Support

To write modern Kotlin utilizing newer JVM standard library classes (such as streams, time classes, or concurrent utilities) without compromising compatibility on devices with Android 8.0, Gradle's **API Desugaring** is integrated.

### Gradle Configuration (`build.gradle`)

```groovy
android {
    compileSdkVersion 35

    defaultConfig {
        minSdkVersion 26
        targetSdkVersion 35
        multiDexEnabled true // Crucial for older devices if dependency footprint grows
    }

    compileOptions {
        // Enable support for modern Java/Kotlin API language desugaring
        coreLibraryDesugaringEnabled true
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // API Desugaring support library for API Level < 26 compatibility (backports newer JDK APIs)
    coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:2.0.4'
}
```

---

## 3. Vector Drawable Engine & Drawing Strategy

Using Vector Drawables is ideal for keeping the APK footprint small and guaranteeing high-fidelity rendering across all pixel densities. However, drawing Vector Drawables onto a custom `SurfaceView` Canvas on Android 8.0 requires specialized handling to ensure high performance.

### Loading Vector Drawables on Older Devices

On Android 8 (API 26), standard `VectorDrawable` is native, but bugs and hardware limitations exist. We must utilize `VectorDrawableCompat` from the AndroidX vector library to guarantee consistent rasterization behavior.

### High-Performance Rendering on Canvas

Directly parsing and rendering vector nodes from an XML file during every frame of the game loop is too CPU-intensive and causes major frame drops. Instead, we compile and rasterize Vector Drawables **once** when the game loads, caching them as native high-performance hardware-compatible `Bitmap` objects.

```kotlin
object VectorCache {
    private val bitmapCache = HashMap<Int, Bitmap>()

    /**
     * Loads, scales, and caches a vector drawable into a high-performance Bitmap.
     */
    fun getBitmap(context: Context, resId: Int, targetWidth: Int, targetHeight: Int): Bitmap {
        val cacheKey = resId * 31 + targetWidth * 17 + targetHeight
        if (bitmapCache.containsKey(cacheKey)) {
            return bitmapCache[cacheKey]!!
        }

        // Get the drawable using compat layers
        val drawable = VectorDrawableCompat.create(context.resources, resId, context.theme)
            ?: throw Resources.NotFoundException("Vector asset not found: $resId")

        // Create a Bitmap configuration matching the rendering surface
        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw the vector onto the off-screen cached bitmap canvas
        drawable.setBounds(0, 0, targetWidth, targetHeight)
        drawable.draw(canvas)

        bitmapCache[cacheKey] = bitmap
        return bitmap
    }
    
    fun clear() {
        for (bitmap in bitmapCache.values) {
            bitmap.recycle()
        }
        bitmapCache.clear()
    }
}
```

### Rendering via Hardware Canvas

Within the rendering loop inside our `SurfaceView` thread, drawing these cached bitmaps uses the highly optimized hardware rasterizer:

```kotlin
// In GameView's render loop:
val cachedChickenBitmap = VectorCache.getBitmap(context, R.drawable.ic_chicken, chickenWidth, chickenHeight)

// Hardware accelerated, O(1) blit operation:
canvas.drawBitmap(cachedChickenBitmap, chickenX, chickenY, null)
```

---

## 4. Hardware and Display Adaptations

### 1. Safe Area Insets (Display Cutouts / Notches)
Android 8.0 introduced initial notch support, and Android 9.0 standardized `WindowInsets`. To prevent the UI elements (like current score) from being obscured by system camera cutouts:
* Configure `WindowInsetsCompat` on the root Layout.
* In the game overlay, apply padding matching the top and side safe margins (`insets.displayCutout` or `systemBars`).

### 2. High-Refresh-Rate Displays (90Hz / 120Hz)
While Tappy Chicken targets 60 FPS as a baseline, modern devices running Android 8+ frequently have 90Hz, 120Hz, or dynamic refresh rates.
* Our **Delta Time Game Loop** (detailed in `architecture.md`) automatically ensures gameplay speeds remain physically identical, whether the device renders at 60 FPS, 90 FPS, or 120 FPS.

### 3. Screen Orientation Management
The game is strictly optimized for **Portrait Mode** only. In `AndroidManifest.xml`, we clamp this behavior to avoid lifecycle overhead, device rotation delays, and aspect ratio recreation loops:

```xml
<activity
    android:name=".MainActivity"
    android:screenOrientation="portrait"
    android:configChanges="orientation|screenSize|keyboardHidden"
    android:theme="@style/Theme.TappyChicken.Fullscreen">
</activity>
```
