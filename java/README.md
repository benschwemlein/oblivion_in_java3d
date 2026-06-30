# Oblivion Landscape Viewer — Java/Java3D

The original version using Java3D for rendering.

## Requirements

- JDK 8 (Java3D is not compatible with newer JDKs)
- Windows (Java3D natives in `java3d/` are Windows-only)

## Running

```sh
javac java/*.java
java -cp java startgui
```

## Controls

| Input | Action |
|---|---|
| Left-drag | Orbit camera |
| Right-drag | Pan |
| Scroll wheel | Zoom in/out |

## Note

Java3D is abandonware (last release 2016) and only runs on Windows. The `java21/` and `kotlin/` versions use LWJGL instead, which is cross-platform and actively maintained.
