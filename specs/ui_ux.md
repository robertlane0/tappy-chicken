# Tappy Chicken Android: UI & UX Layout Specification

This document details the layout structure, screen transitions, visual overlays, typography, and interactive components for the Tappy Chicken user interface.

---

## 1. Window Layout Structure

The app uses a single Activity architecture with a fullscreen, high-performance canvas setup. The **`SurfaceView`** is positioned at the base of a **`FrameLayout`**, while state-specific XML layout views are overlaid on top. This splits high-frame-rate rendering from standard static UI menus (maximizing performance on Android 8.0+ devices).

```xml
<!-- activity_main.xml Layout Hierarchy -->
<FrameLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:keepScreenOn="true">

    <!-- 1. Base Layer: High-Performance Surface View -->
    <com.tappy.chicken.GameSurfaceView
        android:id="@+id/game_surface_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

    <!-- 2. Overlay Layer: State Menus Container -->
    <RelativeLayout
        android:id="@+id/ui_overlay_container"
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <!-- Ready State Instruction Set -->
        <LinearLayout
            android:id="@+id/layout_ready"
            android:visibility="visible" ... />

        <!-- Live HUD Score Indicator (Top-Center) -->
        <TextView
            android:id="@+id/text_live_score"
            android:visibility="gone" ... />

        <!-- GameOver Dialog Card Panel -->
        <include layout="@layout/panel_game_over" />

    </RelativeLayout>
</FrameLayout>
```

---

## 2. HUD State UI Elements

### 1. `READY` State UI (Hovering Idle)
* **Goal:** Intuitively guide the user to tap without overwhelming them.
* **Componentry:**
  * **Title Logo:** Centered vector asset representation of "Tappy Chicken" with a subtle bobbing animation.
  * **Tap Cue:** Alternating blinking text: `"TAP TO FLAP"`.
  * **Instruction Icon:** A vector hand gesture moving up and down between two dotted lines.
* **Layout Rule:** Automatically hidden when the state shifts from `READY` to `PLAYING`.

```xml
<LinearLayout
    android:id="@+id/layout_ready"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_centerInParent="true"
    android:orientation="vertical"
    android:gravity="center">
    
    <ImageView
        android:layout_width="280dp"
        android:layout_height="120dp"
        android:src="@drawable/ic_logo_tappy" />
        
    <TextView
        android:id="@+id/txt_tap_prompt"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="TAP TO FLAP"
        android:textColor="#FFFFFF"
        android:textSize="26sp"
        android:fontFamily="@font/retro_font"
        android:shadowColor="#000000"
        android:shadowRadius="10" />
</LinearLayout>
```

### 2. `PLAYING` State UI (Active Flight)
* **Goal:** Maximum screen visibility, zero clutter.
* **Componentry:**
  * **Live Score:** Positioned at the absolute top-center (`android:layout_alignParentTop="true"`, `android:layout_centerHorizontal="true"`).
  * **Styling:** Large numbers using a heavy, high-contrast, black-stroked retro font to remain readable over dynamic background elements.
  * **Safe Area Handling:** Enforces a top margin of `50dp` (safely below notches and the system status bar).

```xml
<TextView
    android:id="@+id/text_live_score"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_alignParentTop="true"
    android:layout_centerHorizontal="true"
    android:layout_marginTop="50dp"
    android:text="0"
    android:textSize="54sp"
    android:textColor="#FFFFFF"
    android:shadowColor="#000000"
    android:shadowDx="4"
    android:shadowDy="4"
    android:shadowRadius="8"
    android:fontFamily="@font/retro_font" />
```

---

## 3. `GAME_OVER` State Panel (`panel_game_over.xml`)

When the chicken falls after colliding, a central card transitions into view using an Android sliding animation (interpolated bottom-to-center).

```
   +───────────────────────────────────+
   │             GAME OVER             │  <-- Header (Red, Bobbing)
   +───────────────────────────────────+
   │                                   │
   │   +───────────────────────────+   │
   │   │         SCOREBOARD        │   │
   │   │                           │   │
   │   │  MEDAL       SCORE        │   │
   │   │  +---+        23          │   │  <-- Current Run
   │   │  | O |                    │   │
   │   │  +---+       BEST         │   │
   │   │               84          │   │  <-- Loaded from SharedPreferences
   │   │                           │   │
   │   +───────────────────────────+   │
   │                                   │
   │       [RESTART]     [SHARE]       │  <-- Large, tactile buttons
   +───────────────────────────────────+
```

### 1. Retro Medal Awards
To encourage replayability, a medal is awarded based on performance:

| Points Scored | Medal | Vector Drawable Reference |
|---|---|---|
| **$0 \dots 9$** | None | Blank Silhouette |
| **$10 \dots 19$** | Bronze Medal | `ic_medal_bronze.xml` |
| **$20 \dots 39$** | Silver Medal | `ic_medal_silver.xml` |
| **$40 \ge$** | Gold Medal | `ic_medal_gold.xml` |

### 2. High-Contrast Interactive Buttons
* **Tactile Restart:** Orange blocky retro button with a heavy 3D shadow that depresses slightly on click (`onTouchListener` shifts background color and downward translation slightly).
* **Immediate Reset:** Clicking trigger releases active memory states, calls `GameSurfaceView.resetGame()`, sets UI containers back to `Ggone`, and sets the state to `READY`.

---

## 4. Animation & Polish Specifications

1. **Tap Splash Feedback:** Every time the screen is tapped during `PLAYING` state, a small, high-transparency ripple ring is drawn briefly at the touch coordinates on the `SurfaceView` Canvas, fading out in $0.2\text{ seconds}$.
2. **Flash Effect on Hit:** When changing from `PLAYING` to `DEAD`, a white rectangular overlay is briefly rendered on-screen with alpha dropping from $1.0$ to $0.0$ over a duration of $0.15\text{ seconds}$ to emphasize impact.
3. **Pulsing Prompt:** In the `READY` state, the `"TAP TO FLAP"` prompt pulses dynamically in opacity:

   $$\text{Alpha} = 0.4 + 0.6 \cdot \left| \sin\left( \pi \cdot \text{currentTimeMillis} / 600 \right) \right|$$
