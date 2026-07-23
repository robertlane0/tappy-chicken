# Tappy Chicken Android: Implementation Plan Roadmap

This document outlines the step-by-step development process to implement the Tappy Chicken Android application. It serves as a tactical engineering blueprint, from scaffolding the empty Gradle project to generating a play-ready release APK.

---

## 1. Project Implementation Timeline Overview

```
Phase 1: Project Bootstrapping & Setup
  ├── Scaffold Gradle multi-module hierarchy
  └── Integrate Kotlin & Vector Drawable configurations
Phase 2: Game Engine Backbone (SurfaceView + Thread)
  ├── Implement SurfaceHolder callbacks
  └── Construct robust Delta-Time Game Loop Thread
Phase 3: Coordinate Projections & Entity Physics
  ├── Map 1080x1920 virtual canvas scaling rules
  └── Build kinematic gravity, flap, and rotation updates
Phase 4: Procedural Environment Scrolling
  ├── Generate infinite background parallax and grass scrolling
  └── Build random pipe pair generator with recycling thresholds
Phase 5: Collision Detection & Scoring Systems
  ├── Develop AABB checking engine with tolerance thresholds
  └── Implement scoring trigger and SaveManager (SharedPreferences)
Phase 6: Multi-State UI/UX Overlay Integration
  ├── Assemble activity_main.xml structure with SurfaceView base
  └── Build animated Ready and Game Over scoreboard overlays
Phase 7: Sound Design Integration & Performance Polish
  ├── Set up SoundPool stream managers for sound effects
  └── Optimize vector drawing cache and finalize garbage collection
Phase 8: Build Verification, Styling Audits & Linting
  └── Run Gradle compilation checks and lint audits
```

---

## 2. Phase-by-Phase Development Requirements

### Phase 1: Project Bootstrapping & Setup
* **Objective:** Scaffold a robust Kotlin-first Android Gradle project targeting modern SDK APIs while ensuring Android 8.0 support.
* **Tasks:**
  1. Initialize standard project structure:
     * `app/src/main/java/com/tappy/chicken/`
     * `app/src/main/res/layout/` | `app/src/main/res/drawable/` | `app/src/main/res/raw/`
  2. Configure root `build.gradle` and app-level `build.gradle` (refer to `compat.md` configurations).
  3. Declare correct configurations in `AndroidManifest.xml` (clamped to Portrait mode, full screen, hardware acceleration enabled).

---

### Phase 2: Game Engine Backbone (SurfaceView + Thread)
* **Objective:** Construct a crash-safe, multi-threaded rendering surface and high-precision game loop.
* **Tasks:**
  1. Implement `GameSurfaceView` extending standard `SurfaceView` and inheriting `SurfaceHolder.Callback`.
  2. Implement `GameThread` running the delta-time tracking loop inside a `synchronized(holder)` lock block (refer to `architecture.md`).
  3. Connect surface callbacks to lifecycle management safely:
     * `surfaceCreated`: Create a new `GameThread`, set `running = true`, and run `thread.start()`.
     * `surfaceDestroyed`: Set `running = false`, loop `thread.join()` with timeout inside `try-catch` until thread successfully exits before surface memory is reclaimed.

---

### Phase 3: Coordinate Projections & Entity Physics
* **Objective:** Build scale-independent physics calculations and rotation systems.
* **Tasks:**
  1. Map $1080 \times 1920$ virtual coordinates to physical pixels during rendering using horizontal and vertical scaling factors (refer to `gameplay_mechanics.md`).
  2. Code the `Chicken` entity class with position, vertical velocity, rotation, and animation frame variables.
  3. Write standard physics tick methods that receive `deltaTime` to safely step position ($y$) and velocity ($v_y$) under constant gravity acceleration ($g$).
  4. Code the flap trigger which overrides $v_y$ with $v_{\text{flap}}$ instantly upon user touch input.

---

### Phase 4: Procedural Environment Scrolling
* **Objective:** Create the endless scrolling environment.
* **Tasks:**
  1. Implement scrolling `Ground` logic moving at $v_x = -350 \text{ units/s}$, looping seamlessly when a tile segment completely scrolls off-screen.
  2. Implement `Background` layer shifting at a slower rate (e.g., $v_x = -70 \text{ units/s}$) to create parallax depth.
  3. Implement the `PipePair` data model containing $x$ position, random vertical $y_{\text{gap\_center}}$, and `passed` boolean.
  4. Write the procedural generator maintaining 3 active pipe pairs spaced $600\text{ units}$ apart, recycling old pipes to the right edge with a freshly randomized gap placement (refer to `gameplay_mechanics.md`).

---

### Phase 5: Collision Detection & Scoring Systems
* **Objective:** Enforce the rules of survival.
* **Tasks:**
  1. Write an Axis-Aligned Bounding Box (AABB) intersection checking engine.
  2. Apply a $12\%$ collision padding reduction factor to the chicken's hitbox to make gameplay feel fair.
  3. Implement boundary collisions: clamping the ceiling to $y = 100$ and triggering game over upon hitting the ground at $y \ge 1600$.
  4. Program the scoring threshold: when the chicken's center point passes the pipe pair's center point, flip the pipe's `passed` flag to `true`, increment the active score, and save to `SharedPreferences` via `SaveManager` if a high score is achieved.

---

### Phase 6: Multi-State UI/UX Overlay Integration
* **Objective:** Assemble a polished UI on top of the drawing canvas.
* **Tasks:**
  1. Compose `activity_main.xml` embedding the custom `GameSurfaceView` inside a parent `FrameLayout`, with XML-based state overlay components layered on top.
  2. Set up the `READY` state overlay: displaying logo assets, high score indicators, and a pulsing `"TAP TO FLAP"` instruction prompt.
  3. Configure the `GAME_OVER` panel: designing the scoreboard panel that transitions in from the bottom of the screen showing current score, best score, retro performance medals, and tactile restart buttons.
  4. Hook click listeners on buttons to reset states safely on the main thread.

---

### Phase 7: Sound Design Integration & Performance Polish
* **Objective:** Integrate retro sonics and performance optimizations.
* **Tasks:**
  1. Code `SoundManager` utilizing `SoundPool` for latency-free playback of the sound catalog (flap, point, hit, fall) with optimal routing.
  2. Set up vector-bitmap caching inside `VectorCache` to pre-rasterize XML vector drawables onto memory-buffered bitmaps once during load time to keep runtime GPU overhead at zero (refer to `compat.md`).
  3. Add the screen-flash impact animation on state transition from `PLAYING` to `DEAD`.

---

### Phase 8: Build Verification, Styling Audits & Linting
* **Objective:** Final quality control checks.
* **Tasks:**
  1. Execute a local Gradle build check to verify source code compiles cleanly:
     ```bash
     ./gradlew assembleDebug
     ```
  2. Run standard static analysis checks to catch syntax discrepancies, unused assets, or API mismatches:
     ```bash
     ./gradlew lintDebug
     ```
  3. Verify memory allocation inside the custom game thread is minimal, eliminating allocations within the core `update` and `render` loop calls to avoid performance-killing GC pauses on low-end Android 8.0 devices.
