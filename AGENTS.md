# AGENTS.md

## Repo state

Pre-implementation: only `SPECIFICATION.md` and `specs/*.md` exist. No `app/`, `build.gradle`, source, tests, or CI yet.

The specs are the authoritative source of truth, not prose suggestions. Read the relevant spec before implementing anything — they pin the stack, constants, asset XML, and layout structure. When code and a spec disagree, the spec wins unless explicitly retconned.

Spec map:
- `SPECIFICATION.md` — engine-agnostic game design and formal state model.
- `specs/architecture.md` — SurfaceView + GameThread lifecycle, game loop, threading/synchronization rules, state machine.
- `specs/gameplay_mechanics.md` — virtual coordinate system, physics constants, pipe generation/recycling, collision, scoring math.
- `specs/compat.md` — SDK/Gradle config, API desugaring, VectorDrawableCompat + bitmap caching strategy, portrait/fullscreen manifest.
- `specs/assets.md` — color palette, vector drawable XML, audio catalog and SoundPool loader.
- `specs/ui_ux.md` — `activity_main.xml` hierarchy, per-state overlays, game-over panel, medal tiers, polish animations.
- `specs/implementation_plan.md` — 8-phase build roadmap; Phase 8 lists the verify commands.

## Locked target stack (do not deviate without retconning the spec)

- Kotlin 2.0+ (K2 compiler), Android Gradle, single-module app.
- `minSdk 26` (Android 8.0), `targetSdk`/`compileSdk 35` (Android 15).
- Java 8 API desugaring **enabled**; `jvmTarget = "1.8"`; `multiDexEnabled true`.
- Root package `com.tappy.chicken`; `GameSurfaceView` lives at that package.
- Portrait-only, fullscreen theme, `configChanges="orientation|screenSize|keyboardHidden"` in the manifest.

## Architecture decisions that are easy to get wrong

- **SurfaceView + dedicated `GameThread`, not Compose or a single `View`.** The game loop runs on a background thread with delta-time; it must only touch the canvas between `surfaceCreated` and `surfaceDestroyed`.
- **Thread safety rules** (from `architecture.md`): draw inside `synchronized(holder)`; `lockCanvas()` then `unlockCanvasAndPost()` in a `finally`; on `surfaceDestroyed` set `running = false` and `thread.join()` before the surface is reclaimed. Clamp `deltaTime` per frame (`coerceAtMost(0.1f)`) so lag spikes don't tunnel entities through boundaries.
- **Virtual coordinate space is 1080×1920 units.** All physics constants (gravity 3200, flap impulse −900, max fall 1400, max rise −1200, pipe scroll −350, gap height 320, pipe spacing 600, pipe width 180, gap center ∈ [350,1250], ceiling y=100, ground y=1600) are in virtual units. Scale to physical pixels only at render via `Scale_X = ScreenW/1080`, `Scale_Y = ScreenH/1920`. Don't mix virtual and pixel math.
- **Vector drawables must be rasterized once into a `VectorCache` of `Bitmap`s at load time**, then blitted per frame. Parsing/drawing vector XML every frame is explicitly called out as too slow on Android 8. Use `VectorDrawableCompat.create(...)` for consistent rasterization.
- **Game states**: `READY → PLAYING → DEAD → GAME_OVER` (see `architecture.md` §4 for per-state physics/render/input rules). Restart resets everything except high score.
- **Chicken hitbox uses a 12% padding reduction** vs the 70×70 visual bounds (collision box ~55×55) for fair-feeling AABB checks.
- **High score** persisted via `SharedPreferences` ("tappy_chicken_prefs", key `"high_score"`); saved synchronously on game-over.
- **Audio** via `SoundPool` (USAGE_GAME/CONTENT_TYPE_SONIFICATION, max 4 streams); OGG files `sound_flap`, `sound_point`, `sound_hit`, `sound_fall` expected under `res/raw/`.

## Verify commands (once code exists; from implementation_plan Phase 8)

```bash
./gradlew assembleDebug   # build must compile cleanly
./gradlew lintDebug        # static analysis / unused assets / API mismatches
```

Run build before lint. Neither is wired up yet — the Gradle project itself is Phase 1 work.

## Binary assets

OGG sound files and the retro font are **not in the repo and will be provided later**. Do not generate, synthesize, or source replacements. It's fine to wire up `SoundManager` / font references against the documented resource names (`R.raw.sound_flap`, etc., `@font/retro_font`) — just don't commit placeholder binaries.

## Workflow

- Work on `main` directly; no feature branches or PRs expected.
- Preserve the existing commit message style: `<Model> <version>: <action>` (e.g. `Gemini 3.5 Flash: added implementation plan`). Attribute the model that produced the work.