# Tappy Chicken Android: Gameplay Mechanics & Physics Specification

This document details the mathematical model, scale-independent physics formulas, procedural obstacle generation systems, and collision algorithms required to build a deterministic and pixel-perfect clone of Tappy Chicken on Android.

---

## 1. Scale-Independent Coordinate System

To ensure that the physics simulation behaves identically across devices with differing pixel resolutions and aspect ratios, the entire game engine operates inside a fixed **Virtual Coordinate Space** of **$1080 \times 1920$** units.

```
(0,0) ──────────────────────────────────► +X (1080)
  │
  │     +──────────────────────────+
  │     |                          |
  │     |         Ceiling          |
  │     |  - - - - - - - - - - - - |  Y = 100
  │     |                          |
  │     |         Chicken          |
  │     |          [O]             |
  │     |                          |
  │     |       Gap (Height: 320)  |
  │     |                          |
  │     |                          |
  │     | - - - - - - - - - - - - -|  Y = 1600 (Ground Boundary)
  │     |          Ground          |
  │     +──────────────────────────+
  ▼
 +Y (1920)
```

### Conversion Scale Factors

Before rendering elements to the real device screen, coordinates are dynamically projected to actual physical pixels:

$$\text{Scale}_X = \frac{\text{Screen Width}}{1080}, \quad \text{Scale}_Y = \frac{\text{Screen Height}}{1920}$$

$$\text{Physical}_X = \text{Virtual}_X \cdot \text{Scale}_X$$
$$\text{Physical}_Y = \text{Virtual}_Y \cdot \text{Scale}_Y$$

$$\text{Physical Width} = \text{Virtual Width} \cdot \text{Scale}_X$$
$$\text{Physical Height} = \text{Virtual Height} \cdot \text{Scale}_Y$$

---

## 2. Chicken Physics Engine

The chicken operates as a point-mass with vertical-only freedom of motion. All physics updates are scaled by `deltaTime` to ensure constant-time behavior independent of rendering frame-rates.

### Physics Constants (Virtual Units)

| Constant | Symbol | Value | Unit | Description |
|---|---|---|---|---|
| **Gravity** | $g$ | $3200$ | $\text{units/s}^2$ | Constant downward acceleration pull. |
| **Flap Impulse** | $v_{\text{flap}}$ | $-900$ | $\text{units/s}$ | Instantaneous upward velocity replacement. |
| **Max Fall Speed** | $v_{\text{max\_fall}}$| $1400$ | $\text{units/s}$ | Terminal vertical downward velocity limit. |
| **Max Rise Speed** | $v_{\text{max\_rise}}$| $-1200$ | $\text{units/s}$ | Maximum upward velocity limit. |

### Movement Update Equations

During every frame in state `PLAYING` or `DEAD`, the physics engine updates the chicken’s kinematic properties:

$$\Delta v_y = g \cdot \Delta t$$
$$v_y \leftarrow \text{clamp}(v_y + \Delta v_y, \ v_{\text{max\_rise}}, \ v_{\text{max\_fall}})$$
$$y \leftarrow y + v_y \cdot \Delta t$$

### Flap Action

When the player taps the screen during `READY` or `PLAYING`:

$$v_y \leftarrow v_{\text{flap}}$$

*Note: The flap is a discrete velocity replacement, not an accumulation. No lift forces are simulated.*

---

## 3. Cosmetic Rotation Engine

To provide visual feedback on speed, the chicken's drawing sprite rotates based on its current vertical velocity ($v_y$).

* **Going Up ($v_y < 0$):** The nose tilts upward.
* **Going Down ($v_y > 0$):** The nose gradually rotates downwards.

### Mathematical Mapping

* **Ascent Boundary:** Velocity of $v_y \le -400$ maps to a pitch of $-25^\circ$.
* **Descent Boundary:** Velocity of $v_y \ge 1000$ maps to a pitch of $+70^\circ$.
* **Linear Interpolation Formula:**
  
  $$\theta = \text{clamp}\left( -25 + \frac{v_y - (-400)}{1000 - (-400)} \cdot (70 - (-25)), \ -25, \ 70 \right) \text{ degrees}$$

---

## 4. Procedural Obstacle (Pipe) Generation

Pipes move right-to-left across the screen. When a pipe scrolls off the left screen edge, it is recycled to spawn past the right screen edge.

### Pipe Spawn Metrics (Virtual Units)

* **Scroll Speed ($v_x$):** $-350 \text{ units/s}$ (constant leftward scroll).
* **Gap Height ($h_{\text{gap}}$):** $320 \text{ units}$ (constant opening vertical spacing).
* **Horizontal Pipe Spacing ($w_{\text{spacing}}$):** $600 \text{ units}$ (distance between consecutive pipe pairs).
* **Pipe Width ($w_{\text{pipe}}$):** $180 \text{ units}$.
* **Vertical Gap Placement Range ($y_{\text{gap\_center}}$):** Sampled uniformly between $y = 350$ and $y = 1250$.

### Spawning & Recycling Logic

* Initial state spawns **3 pipe pairs** positioned sequentially at:
  * Pipe 1: $x = 1200$
  * Pipe 2: $x = 1800$
  * Pipe 3: $x = 2400$
* On every update, for each active pipe:

  $$x \leftarrow x + v_x \cdot \Delta t$$

* If a pipe's trailing edge exits the boundary ($x + w_{\text{pipe}} < 0$):
  * Find the current furthest pipe $x_{\text{max}}$.
  * Reset current pipe's position to $x \leftarrow x_{\text{max}} + w_{\text{spacing}}$.
  * Generate a new random vertical center $y_{\text{gap\_center}} \in [350, 1250]$.
  * Reset the `passed` flag to `false`.

---

## 5. Collision Detection & Boundaries

The game enforces strict binary collision parameters. Touching any obstacle or the ground ends the game instantly.

### Bounding Boxes (Virtual Coordinate Space)

To make gameplay feel fair and prevent visual clipping frustum bugs:
* **Pipes:** Represented as standard Axis-Aligned Bounding Boxes (AABB).
* **Ground:** Solid block spanning $y = [1600 \dots 1920]$.
* **Ceiling Boundary:** $y = 100$. The chicken cannot fly past this point. If $y < 100$, clamp $y \leftarrow 100$ and set $v_y \leftarrow 0$.
* **Chicken Collision Radius:** Although visual assets are vector shapes, the chicken is modeled as a smaller inner bounding circle (or tight bounding box with $12\%$ padding tolerance) to avoid frustrating near-collisions.

```
   Visual Bounds (70x70)
   ┌───────────────────┐
   │    ┌─────────┐    │
   │  ┌─┘         └─┐  │
   │  │   Light     │  │
   │  │   AABB      │  │  <-- Actual Collision Boundary (55x55)
   │  └─┐         ┌─┘  │
   │    └─────────┘    │
   └───────────────────┘
```

### Collision Evaluation Algorithm

During the physics update cycle, collision flags are evaluated:

```kotlin
fun checkCollision(chicken: Chicken, pipes: List<PipePair>, groundY: Float): Boolean {
    // 1. Check Ground Collision (Instant death)
    if (chicken.y + chicken.collisionHeight / 2f >= groundY) {
        return true
    }

    // 2. Check Pipe Collisions
    val chickenBox = chicken.getBoundingBox() // Applies 12% collision tolerance padding
    for (pipe in pipes) {
        if (chickenBox.intersects(pipe.topPipeBox) || chickenBox.intersects(pipe.bottomPipeBox)) {
            return true
        }
    }
    return false
}
```

---

## 6. Scoring & Persistence

### Scoring Trigger
A point is awarded when a pipe pair's horizontal center point is successfully crossed by the chicken's horizontal center point:

$$\text{If } \left(x_{\text{chicken}} > x_{\text{pipe}} + \frac{w_{\text{pipe}}}{2}\right) \text{ and } \left(\text{passed} == \text{false}\right):$$
$$\text{Score} \leftarrow \text{Score} + 1$$
$$\text{passed} \leftarrow \text{true}$$

### Persistent High Score Storage
To guarantee persistent records across device restarts, high scores are stored locally using Android's lightweight **`SharedPreferences`**.

```kotlin
class SaveManager(context: Context) {
    private val prefs = context.getSharedPreferences("tappy_chicken_prefs", Context.MODE_PRIVATE)

    fun getHighScore(): Int {
        return prefs.getInt("high_score", 0)
    }

    fun saveScore(currentScore: Int): Boolean {
        val currentHigh = getHighScore()
        if (currentScore > currentHigh) {
            prefs.edit().putInt("high_score", currentScore).apply()
            return true // New high score saved!
        }
        return false
    }
}
```
Validation operations run inside the game-over transition thread synchronously to prevent data loss.
