# Tappy Chicken Android: Assets & Vector Definition Specification

This document details the visual style, color palette, vector path structures, and audio resources required to build the visual assets for the Tappy Chicken Android application.

---

## 1. Visual Style & Color Palette

The game uses a crisp, retro-inspired flat cartoon art style. Since all graphics are composed of scalable vector equations rather than bitmaps, the asset footprint is extremely small ($<100\text{ KB}$ for all visuals combined).

### Standard Color Palette

The following hex values compose the entire visual styling of the game:

| Layer / Element | Hex Color | Color Sample Description | Usage |
|---|---|---|---|
| **Sky / Background** | `#87CEEB` | Sky Blue | Background clearing fill |
| **Cloud White** | `#F5F5F5` | Off-White / Soft Gray | Scrolling background clouds |
| **Cloud Highlight**| `#FFFFFF` | Pure White | Inner cloud layers |
| **Pipe Surface** | `#228B22` | Forest Green | Outer pipe outline and body paint |
| **Pipe Highlight**| `#32CD32` | Lime Green | Internal vertical pipe specular gradient |
| **Pipe Shadow** | `#006400` | Dark Green | Pipe inner joints and shaded regions |
| **Ground Base** | `#D2B48C` | Tan / Light Brown | Deep soil block |
| **Ground Grass** | `#2E8B57` | Sea Green | Top grass edge of the scrolling ground |
| **Chicken Body** | `#FFD700` | Gold / Yellow | Primary body fill |
| **Chicken Wing** | `#FFA500` | Orange | Wing flap shape |
| **Chicken Beak** | `#FF4500` | Orange-Red | Mouth beak highlights |
| **Chicken Eye** | `#FFFFFF` / `#000000` | White / Black | Eye details |

---

## 2. Vector Drawable Asset XML Specifications

Using standard Android XML drawables ensures hardware-accelerated rendering on modern canvases. The XMLs are placed inside the project's `res/drawable/` directory.

### 1. The Chicken (`res/drawable/ic_chicken.xml`)
The chicken is composed of several vector paths representing the body, beak, eyes, and wings.

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="70dp"
    android:height="70dp"
    android:viewportWidth="70"
    android:viewportHeight="70">
    <!-- Chicken Body: Yellow Circle/Oval -->
    <path
        android:fillColor="#FFD700"
        android:strokeColor="#332200"
        android:strokeWidth="2.5"
        android:pathData="M35,15 C48,15 58,25 58,35 C58,45 48,55 35,55 C22,55 12,45 12,35 C12,25 22,15 35,15 Z" />
    <!-- Wing: Orange Oval on the side -->
    <path
        android:fillColor="#FFA500"
        android:strokeColor="#332200"
        android:strokeWidth="2"
        android:pathData="M22,32 C28,32 32,36 32,41 C32,46 28,50 22,50 C16,50 12,46 12,41 C12,36 16,32 22,32 Z" />
    <!-- Eye: Large White Circle with Black Pupil -->
    <path
        android:fillColor="#FFFFFF"
        android:strokeColor="#332200"
        android:strokeWidth="1.5"
        android:pathData="M45,22 C49,22 52,25 52,29 C52,33 49,36 45,36 C41,36 38,33 38,29 C38,25 41,22 45,22 Z" />
    <path
        android:fillColor="#000000"
        android:pathData="M47,27 C49,27 50,28 50,29 C50,30 49,31 47,31 C45,31 44,30 44,29 C44,28 45,27 47,27 Z" />
    <!-- Beak: Reddish-Orange Triangle pointing right -->
    <path
        android:fillColor="#FF4500"
        android:strokeColor="#332200"
        android:strokeWidth="2"
        android:pathData="M55,31 L65,36 L54,41 Z" />
</vector>
```

### 2. The Pipe Structure (`res/drawable/ic_pipe.xml` & `res/drawable/ic_pipe_cap.xml`)
The pipe structure consists of two components: the main cylinder body (pattern-tiled or stretched) and the raised lip cap on the end.

#### Pipe Body Component (`res/drawable/ic_pipe_body.xml`)
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="180dp"
    android:height="100dp"
    android:viewportWidth="180"
    android:viewportHeight="100">
    <!-- Main Cylinder Gradient (Dark Green to Lime highlight to Dark Green) -->
    <path
        android:pathData="M0,0 L180,0 L180,100 L0,100 Z">
        <aapt:attr name="android:fillColor" xmlns:aapt="http://schemas.android.com/tools">
            <gradient
                android:startX="0"
                android:startY="50"
                android:endX="180"
                android:endY="50"
                android:type="linear">
                <item android:offset="0.0" android:color="#006400" /> <!-- Shadow -->
                <item android:offset="0.3" android:color="#32CD32" /> <!-- Highlight -->
                <item android:offset="1.0" android:color="#004d00" /> <!-- Deep Shadow -->
            </gradient>
        </aapt:attr>
    </path>
    <!-- Left and Right Outlines -->
    <path
        android:strokeColor="#113311"
        android:strokeWidth="4"
        android:pathData="M2,0 L2,100 M178,0 L178,100" />
</vector>
```

#### Pipe End Cap Component (`res/drawable/ic_pipe_cap.xml`)
The end cap is slightly wider ($200\text{ units}$ wide, hanging $10\text{ units}$ over both edges of the $180\text{ unit}$ pipe body) and is $60\text{ units}$ high.
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="200dp"
    android:height="60dp"
    android:viewportWidth="200"
    android:viewportHeight="60">
    <path
        android:pathData="M0,0 L200,0 L200,60 L0,60 Z">
        <aapt:attr name="android:fillColor" xmlns:aapt="http://schemas.android.com/tools">
            <gradient
                android:startX="0"
                android:startY="30"
                android:endX="200"
                android:endY="30"
                android:type="linear">
                <item android:offset="0.0" android:color="#006400" />
                <item android:offset="0.3" android:color="#32CD32" />
                <item android:offset="1.0" android:color="#004d00" />
            </gradient>
        </aapt:attr>
    </path>
    <path
        android:strokeColor="#113311"
        android:strokeWidth="4"
        android:pathData="M2,0 L2,60 M198,0 L198,60 M0,2 L200,2 M0,58 L200,58" />
</vector>
```

### 3. The Ground Layer
The ground consists of a scrolling horizontal vector tile:
* Top $30\text{ units}$: `#2E8B57` grass belt with alternating dark green vertical detail lines.
* Bottom $290\text{ units}$: `#D2B48C` tan soil block with scattered brown pixelated pebbles.

---

## 3. Audio & Sound Effect Assets

To match retro game aesthetics, the app handles lightweight sound feedback using Android's **`SoundPool`** class (recommended for low-latency, short sound clips since Android 8.0).

### Sound Asset Catalog (Ogg Vorbis format, 44.1kHz, Mono)

| Filename | Purpose | Playback Logic |
|---|---|---|
| `sound_flap.ogg` | Player inputs a flap command | Played instantly at $1.0\times$ speed inside `MainActivity` input listener. |
| `sound_point.ogg`| Chicken passes a pipe pair | Triggered inside `GameThread` on score increment. |
| `sound_hit.ogg` | Chicken collides with a pipe | Played instantly upon state changing to `DEAD`. |
| `sound_fall.ogg` | Chicken hits the ground | Played inside state `DEAD` when vertical fall completes. |

### SoundPool Loader Implementation

Using `AudioAttributes` ensures standard playback routing and volume scaling on Oreo and newer devices.

```kotlin
class SoundManager(context: Context) {
    private val soundPool: SoundPool
    private val flapId: Int
    private val pointId: Int
    private val hitId: Int
    private val fallId: Int

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attrs)
            .build()

        // Loads OGG files placed under app/src/main/assets/ or res/raw/
        flapId = soundPool.load(context, R.raw.sound_flap, 1)
        pointId = soundPool.load(context, R.raw.sound_point, 1)
        hitId = soundPool.load(context, R.raw.sound_hit, 1)
        fallId = soundPool.load(context, R.raw.sound_fall, 1)
    }

    fun playFlap() = soundPool.play(flapId, 1f, 1f, 1, 0, 1f)
    fun playPoint() = soundPool.play(pointId, 1f, 1f, 1, 0, 1f)
    fun playHit() = soundPool.play(hitId, 1f, 1f, 2, 0, 1f)
    fun playFall() = soundPool.play(fallId, 1f, 1f, 2, 0, 1f)
    
    fun release() {
        soundPool.release()
    }
}
```
All assets are managed asynchronously to prevent blocking during initial game boot.
