# Oblivion Landscape Viewer — Java 21/LWJGL

A rewrite of the original Java version using modern Java 21 features and
[LWJGL](https://www.lwjgl.org/) for cross-platform OpenGL rendering.

## Requirements

- JDK 21+
- Gradle

Runs on Windows, macOS, and Linux (LWJGL natives for all platforms are bundled).

## Running

```sh
cd java21
gradle run
```

## Controls

| Input | Action |
|---|---|
| Left-drag | Orbit camera |
| Right-drag | Pan |
| Scroll wheel | Zoom in/out |

## Java 21 features used

| Old Java | Java 21 |
|---|---|
| `class NifSVertex` + fields + constructor + getters | `record Vertex(float x, float y, float z) {}` |
| 250-line `NifInputStream` | `ByteBuffer.order(LITTLE_ENDIAN)` + 3 private static methods |
| `try (var stack = ...)` verbose cast | `Math.clamp(value, min, max)` (new in Java 21) |
| Verbose generic loops | Streams, `var`, method references |
| `if (b instanceof MouseZoom) { MouseZoom z = (MouseZoom)b; }` | Pattern matching (not needed with LWJGL) |
| Text blocks for GLSL | `"""..."""` text blocks |
| Local `record Mesh(int vao, int count) {}` | Local records (Java 16+) |

## Why LWJGL over Java3D

- Java3D is abandonware (last release 2016), Windows-only natives
- LWJGL is actively maintained, cross-platform, used by Minecraft
- Same GLSL shader approach as the Rust and Python versions
