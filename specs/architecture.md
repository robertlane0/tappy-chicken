# Tappy Chicken Android: Architectural Specification (SurfaceView & Thread)

This document details the software architecture, state machine, and threading models for the Tappy Chicken Android application. 

---

## 1. Core Architecture Overview

Tappy Chicken utilizes a classic mobile game architecture based on a dedicated background game-loop thread and a hardware-accelerated **`SurfaceView`**. This decouples the physics and rendering rates from the main Android UI thread, ensuring a stutter-free 60 FPS gameplay experience on devices going back to Android 8.0.

```
+-------------------------------------------------------------+
|                     Android UI Thread                       |
|  - Activity Lifecycle Management (OnPause, OnResume)        |
|  - System & State Overlays (XML-based Buttons, Scoreboards) |
|  - Input Event Handling (Taps, Clicks)                      |
+------------------------------+------------------------------+
                               |
                   Triggers Lifecycle & Inputs
                               v
+------------------------------+------------------------------+
|                     Game Loop Thread                        |
|  - Controlled by a high-precision rendering loop            |
|  - Updates scale-independent physics positions             |
|  - Queries touch impulses via atomic state                 |
|  - Standardizes frame updates with Delta Time               |
|  - Locks Canvas on SurfaceHolder & renders Vector objects  |
+-------------------------------------------------------------+
```

---

## 2. SurfaceView & Thread Lifecycle Integration

The background thread is tightly coupled with both the **Activity Lifecycle** and the **`SurfaceHolder.Callback`** interface. This prevents crashes due to rendering on an uninitialized/destroyed surface.

### Thread Lifecycle States

```
                 [Activity onCreate / onCreateView]
                                 │
                                 ▼
                     SurfaceHolder.Callback
                     ┌───────────────────┐
                     │  surfaceCreated   ├────────┐
                     └─────────┬─────────┘        │
                               │                  │ Creates & Starts
                               ▼                  ▼
                     ┌───────────────────┐  ┌─────────────┐
           ┌────────►│  surfaceChanged   │  │ GameThread  │
           │         └─────────┬─────────┘  └──────┬──────┘
           │                   │                   │
  Re-locks │                   ▼                   │ Sets running = true
  Canvas   │         ┌───────────────────┐         │ Updates & Draws
           └─────────┤   Active Loop     │◄────────┘
                     └─────────┬─────────┘
                               │
                               ▼ [Activity onPause / surfaceDestroyed]
                     ┌───────────────────┐
                     │ surfaceDestroyed  │
                     └─────────┬─────────┘
                               │ Sets running = false
                               ▼
                        [thread.join()]
                               │
                               ▼
                        Thread Terminated
```

### Safety & Synchronization Rules

1. **Surface Ownership:** The game thread must *only* write to the `Canvas` between `surfaceCreated` and `surfaceDestroyed`.
2. **Double-Buffering & Locking:** To draw to the `SurfaceView`, the thread must:
   * Call `canvas = holder.lockCanvas()` (handles hardware-accelerated drawing buffer allocation).
   * Perform vector draw operations on the acquired `canvas` within a `synchronized(holder)` block to prevent concurrent access.
   * Call `holder.unlockCanvasAndPost(canvas)` inside a `finally` block to guarantee release.
3. **Graceful Shutdown:** In `surfaceDestroyed`, the background thread's run flag must be set to `false`, and the UI thread must block with `thread.join()` inside a `try-catch` block until the game loop exits completely. This prevents orphaned drawing calls.

---

## 3. High-Precision Game Loop

The loop uses a standard Delta Time calculation to ensure consistent gameplay speeds across variable hardware configurations.

```kotlin
class GameThread(private val surfaceHolder: SurfaceHolder, private val gameView: GameView) : Thread() {
    @Volatile var running: Boolean = false
    
    override fun run() {
        var lastTime = System.nanoTime()
        
        while (running) {
            val canvas = surfaceHolder.lockCanvas()
            if (canvas != null) {
                try {
                    synchronized(surfaceHolder) {
                        val currentTime = System.nanoTime()
                        // Calculate elapsed time in seconds
                        val deltaTime = (currentTime - lastTime) / 1_000_000_000f
                        lastTime = currentTime
                        
                        // Limit deltaTime to prevent game objects from clipping through boundaries
                        // in case of a temporary thread pause/lag spike
                        val clampedDeltaTime = deltaTime.coerceAtMost(0.1f)
                        
                        // Step 1: Update Physics & Positions
                        gameView.update(clampedDeltaTime)
                        
                        // Step 2: Render Vector Assets
                        gameView.render(canvas)
                    }
                } finally {
                    surfaceHolder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }
}
```

---

## 4. Game State Machine

The core mechanics are modeled using four mutually exclusive game states. Transitions are handled by atomic status checks.

```
       ┌───────────┐
       │   READY   │  <────────────────────────────────────────┐
       └─────┬─────┘                                           │
             │                                                 │
             │  Player Flaps (First Tap)                       │
             ▼                                                 │
       ┌───────────┐                                           │
       │  PLAYING  │                                           │
       └─────┬─────┘                                           │
             │                                                 │
             │  Collision Detected (with pipe or ground)       │
             ▼                                                 │ Restart Button
       ┌───────────┐                                           │ Pressed
       │   DEAD    │                                           │
       └─────┬─────┘                                           │
             │                                                 │
             │  Chicken completes descent and hits ground      │
             ▼                                                 │
       ┌───────────┐                                           │
       │ GAME_OVER ├───────────────────────────────────────────┘
       └───────────┘
```

### Game States Definition

| State | Physics Engine Behavior | Rendering Elements | Input Handler Action |
|---|---|---|---|
| **`READY`** | Chicken is stationary vertically, executing an idle hover. Pipes are static. Background parallax is inactive. | Chicken, Ground, Background, "Tap to Flap" overlay instructions. | First tap applies upward impulse and changes state to `PLAYING`. |
| **`PLAYING`** | Full 2D physics active. Pipes scroll left. Parallax background shifts left. Collision detection active. Scoring triggers on pipe crossing. | Chicken, Active Pipes, Scrolling Ground, Scrolling Background, Live Score HUD overlay. | Each tap registers a discrete flap impulse. |
| **`DEAD`** | All scrolling stops instantly. Chicken's control input is severed. Gravity continues pulling the chicken downward. Collision checks disabled. | Static Pipes, Ground, Background, falling Chicken. | Taps are ignored completely. |
| **`GAME_OVER`**| All physics and movement are fully halted. Score is finalized, comparing to the stored best score. | Final static game frame, high-contrast Scoreboard overlay, "Restart" and "Share" options. | Clicking "Restart" calls reset logic and resets state to `READY`. |

---

## 5. UI Thread Inter-Process Communication

Since Android UI updates must happen on the main UI thread:
* **Score updates & State Transitions** occurring inside the `GameThread` will trigger events via standard listeners.
* High-frequency UI (Live Score) is drawn directly on the `SurfaceView` Canvas inside the loop for performance.
* Screen-space transition overlays (such as the Game Over Scorecard) are normal XML-configured Android Views nested over the `SurfaceView` inside a `RelativeLayout` or `FrameLayout`, toggled visible/gone using handler posts:

```kotlin
activity.runOnUiThread {
    gameOverLayout.visibility = View.VISIBLE
    finalScoreTextView.text = currentScore.toString()
}
```
