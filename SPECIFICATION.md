# Tappy Chicken: High-Level Game Specification
 
This specification describes the core mechanics and behavior of the original Tappy Chicken at a gameplay and systems level, independent of any particular implementation language or engine.
 
---
 
# 1. Game Overview
 
Tappy Chicken is a real-time, single-player, endless arcade game centered around maintaining controlled flight through a procedurally generated sequence of obstacles.
 
The player controls exactly one action:
 
* Apply an instantaneous upward impulse ("flap").
 
Everything else is deterministic or automatic.
 
The challenge emerges from repeatedly compensating for constant gravity while navigating through moving gaps between obstacles.
 
---
 
# 2. Objective
 
The player attempts to survive for as long as possible.
 
Success is measured by:
 
* Number of obstacle pairs successfully passed.
* Corresponding score.
 
The game has no ending.
 
Difficulty comes from sustained precision rather than increasing game complexity.
 
---
 
# 3. Game World
 
The world consists of:
 
* A scrolling background
* A scrolling ground
* A flying chicken
* Infinite obstacle pairs ("pipes")
 
The camera remains effectively fixed.
 
Instead of the chicken moving forward through the world:
 
* The chicken stays approximately fixed horizontally.
* The environment scrolls leftward.
 
---
 
# 4. Coordinate System
 
The playfield is a 2D rectangle.
 
Important properties:
 
* X increases rightward.
* Y increases downward.
* Gravity therefore increases positive Y velocity.
 
The visible screen completely defines the gameplay area.
 
Objects leaving the left edge are removed.
 
New objects enter from the right edge.
 
---
 
# 5. Chicken
 
The chicken is the only controllable entity.
 
It has:
 
* Position
* Vertical velocity
* Collision bounds
* Orientation (visual only)
* Animation state
 
Horizontal position is effectively constant throughout gameplay.
 
Only vertical motion changes.
 
---
 
# 6. Chicken Motion
 
Motion is governed by two influences:
 
## Gravity
 
Every update:
 
* Gravity increases downward velocity.
 
Gravity is constant.
 
---
 
## Flap
 
Whenever input occurs:
 
Current vertical velocity is immediately replaced (or effectively overridden) by a fixed upward impulse.
 
The flap:
 
* Does not accumulate.
* Does not depend on current speed.
* Produces immediate upward acceleration.
 
No sustained lift exists.
 
Each flap is an isolated impulse.
 
---
 
# 7. Rotation
 
Rotation is cosmetic.
 
Typical behavior:
 
* While ascending, nose tilts upward.
* While descending, nose gradually rotates downward.
* Maximum upward/downward angles are clamped.
 
Rotation does not affect collision.
 
---
 
# 8. Animation
 
The chicken cycles through wing frames.
 
Animation is periodic and independent of player input.
 
It continues while alive.
 
After death:
 
* Animation may stop or continue depending on implementation.
 
---
 
# 9. Obstacles
 
Obstacles consist of vertical pipe pairs.
 
Each pair contains:
 
* Top pipe
* Bottom pipe
 
Between them is a gap.
 
The gap is always traversable.
 
---
 
# 10. Pipe Generation
 
Pipes are generated indefinitely.
 
Each new pair has:
 
* Fixed horizontal spacing from previous pair.
* Randomized vertical gap position.
 
Gap size remains constant.
 
Only vertical placement changes.
 
---
 
# 11. Pipe Motion
 
Pipes move horizontally:
 
* Constant speed
* Right to left
 
No acceleration.
 
No vertical movement.
 
No rotation.
 
---
 
# 12. Pipe Recycling
 
When a pipe pair exits the screen:
 
* Remove or recycle it.
* Generate a new pair beyond the right edge.
 
The world therefore appears infinite.
 
---
 
# 13. Ground
 
Ground scrolls horizontally.
 
Its purposes:
 
* Visual reference
* Collision surface
 
Ground loops seamlessly.
 
---
 
# 14. Background
 
Background scrolls slower than pipes to create parallax.
 
It has no gameplay effect.
 
---
 
# 15. Player Input
 
Exactly one gameplay input exists:
 
Flap.
 
Input sources may include:
 
* Mouse click
* Screen tap
* Keyboard key
* Controller button
 
All map to identical behavior.
 
No distinction exists between devices.
 
---
 
# 16. Timing
 
Gameplay proceeds continuously.
 
Each update performs:
 
1. Read input.
2. Apply flap if requested.
3. Apply gravity.
4. Update chicken position.
5. Move pipes.
6. Detect collisions.
7. Award score.
8. Render frame.
 
---
 
# 17. Collision
 
Collisions are tested between:
 
Chicken and:
 
* Top pipe
* Bottom pipe
* Ground
 
Collision immediately ends gameplay.
 
No health system exists.
 
No lives exist.
 
No damage accumulation exists.
 
---
 
# 18. Pipe Collision
 
The playable region consists only of the gap.
 
Touching any pipe surface causes immediate death.
 
No tolerance beyond collision geometry exists.
 
---
 
# 19. Ceiling
 
Behavior differs slightly among implementations.
 
Common behaviors:
 
* Chicken cannot leave the visible top.
* Chicken may clip slightly before descending.
* Contact may clamp position.
 
Touching the top generally does not immediately end the game.
 
---
 
# 20. Ground Collision
 
Touching the ground always causes game over.
 
Ground is a solid collision boundary.
 
---
 
# 21. Death State
 
Upon death:
 
Chicken loses player control.
 
Gameplay stops scoring.
 
Pipes stop or shortly stop moving depending on implementation.
 
Chicken falls toward the ground under gravity.
 
The run becomes final.
 
---
 
# 22. Scoring
 
One point is awarded for each obstacle pair successfully passed.
 
Passing means:
 
The chicken's horizontal position crosses completely beyond the pipe pair without collision.
 
Each pair awards exactly one point.
 
---
 
# 23. High Score
 
The highest score achieved is stored persistently.
 
If current score exceeds stored score:
 
High score is updated.
 
---
 
# 24. Difficulty
 
Difficulty remains nearly constant.
 
Primary parameters remain fixed:
 
* Gravity
* Flap strength
* Pipe speed
* Pipe spacing
* Gap size
 
Challenge increases naturally through player fatigue and precision requirements.
 
Some versions introduce slight random variation, but no progressive difficulty scaling is fundamental to the game.
 
---
 
# 25. Randomness
 
Randomness affects only obstacle placement.
 
Each pipe gap's vertical center is sampled within a bounded range such that:
 
* Gap remains entirely on-screen.
* Pipes remain traversable.
 
Everything else behaves deterministically.
 
---
 
# 26. User Interface
 
During play:
 
Display:
 
* Current score
 
After death:
 
Display:
 
* Final score
* Best score
* Restart option
 
---
 
# 27. Game States
 
The game can be modeled with these states:
 
## Ready
 
* Chicken idle animation
* No gameplay
* Waiting for first flap
 
---
 
## Playing
 
* Physics active
* Pipes moving
* Score increasing
 
---
 
## Dead
 
* Collision occurred
* Chicken falling
* Score frozen
 
---
 
## Game Over
 
* Final score shown
* Await restart input
 
---
 
# 28. Restart
 
Restart completely resets:
 
* Chicken position
* Velocity
* Score
* Pipes
* Random sequence (new layout)
* State
 
High score remains.
 
---
 
# 29. Winning
 
There is no victory condition.
 
The only terminal condition is failure.
 
---
 
# 30. Core Gameplay Loop
 
The game repeatedly executes:
 
1. Wait for flap.
2. Chicken rises.
3. Gravity slows ascent.
4. Chicken begins falling.
5. Player flaps again.
6. Pipes approach.
7. Player aligns with gap.
8. Pass pipe.
9. Increment score.
10. Repeat indefinitely until collision.
 
---
 
# 31. Essential Design Properties
 
The game's design is characterized by:
 
* Single-button input
* Continuous physics
* Constant forward progression
* Endless procedural obstacle generation
* Immediate failure upon collision
* One-point reward per obstacle pair
* No enemies
* No power-ups
* No upgrades
* No levels
* No resource management
* No narrative
* Skill-based progression only
 
---
 
# 32. Minimal Formal Model
 
At the highest level, the game state can be represented as:
 
```
Game
├── State
│   ├── Ready
│   ├── Playing
│   ├── Dead
│   └── GameOver
│
├── Chicken
│   ├── Position
│   ├── Vertical Velocity
│   ├── Rotation
│   └── Collision Bounds
│
├── Pipes[]
│   ├── X Position
│   ├── Gap Center
│   ├── Gap Height
│   └── Passed Flag
│
├── Physics
│   ├── Gravity
│   └── Flap Impulse
│
├── World
│   ├── Scroll Speed
│   ├── Ground
│   └── Background
│
├── Score
└── High Score
```
 
In essence, Tappy Chicken can be viewed as a deterministic physics simulation with one discrete player action (flap), a constant gravitational force, a horizontally scrolling stream of randomly positioned obstacle gaps, binary collision outcomes, and an endless survival objective measured solely by the number of obstacles successfully passed.
