# Oblivion Landscape Viewer — Java 21/Java3D

A rewrite of the original Java version using modern Java 21 features.
Same Java3D rendering, significantly less code.

## Requirements

- JDK 21+
- Gradle
- Windows (Java3D natives are Windows-only)

## Running

```sh
cd java21
gradle run
```

## What's new vs the original Java

| Old Java | Java 21 |
|---|---|
| `class NifSVertex` + fields + constructor + getters | `record Vertex(float x, float y, float z) {}` |
| Manual byte-swap in `NifInputStream` (250 lines) | `ByteBuffer.order(LITTLE_ENDIAN)` + `getFloat()`/`getShort()` |
| `Short.toUnsignedInt()` workaround | Built-in `Short.toUnsignedInt()` (Java 8+, just unused before) |
| Verbose `for` loops over lists | Streams with `flatMapToDouble`, `map`, `toArray` |
| `new ArrayList(); list.add(x)` repeated | `var` + cleaner initialization |
| `if (behavior instanceof MouseZoom) { MouseZoom z = (MouseZoom) behavior; }` | `if (behavior instanceof MouseZoom z)` pattern matching |

## What Java 21 still can't do (vs Kotlin)

- No extension functions — `skipZeros`, `readString`, `skipToRef` are private static methods rather than feeling like they belong on `ByteBuffer`
- Checked exceptions still require `throws` declarations or try/catch everywhere
- No `.apply { }` — object setup is still create-then-configure, not inline
