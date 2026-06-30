# Oblivion Landscape Viewer — Kotlin/LWJGL

A Kotlin port using [LWJGL](https://www.lwjgl.org/) for cross-platform OpenGL rendering.

## Requirements

- JDK 11+
- Gradle

Runs on Windows, macOS, and Linux (LWJGL natives for all platforms are bundled).

## Running

```sh
cd kotlin
gradle run
```

## Controls

| Input | Action |
|---|---|
| Left-drag | Orbit camera |
| Right-drag | Pan |
| Scroll wheel | Zoom in/out |

## Kotlin vs Java 21

Both versions are roughly the same length. The Kotlin differences are stylistic:

| Java 21 | Kotlin |
|---|---|
| `try (var stack = stackPush()) { ... }` | `MemoryStack.stackPush().use { stack -> ... }` |
| Lambda callbacks with explicit types | Trailing lambdas, type inference |
| `Math.clamp(...)` | `.coerceIn(...)` |
| `for (var mesh : meshes)` | `meshes.forEach { (vao, count) -> }` with destructuring |
| `try { } catch (Exception e) { }` | `runCatching { }.onFailure { }` |
| `glClear(A \| B)` | `glClear(A or B)` |
| Local `record Mesh(int vao, int count)` | `private data class Mesh(val vao: Int, val count: Int)` |
| `buildProgram()` — create, attach, link, delete, return | `buildProgram()` — `.also { }` chaining |

## Why LWJGL over Java3D

- Java3D is abandonware (last release 2016), Windows-only natives
- LWJGL is actively maintained, cross-platform, used by Minecraft
- Same GLSL shader approach as the Rust and Python versions
