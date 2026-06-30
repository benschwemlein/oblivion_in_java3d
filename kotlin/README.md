# Oblivion Landscape Viewer — Kotlin/Java3D

A Kotlin port of the original Java version. Uses the same Java3D rendering but
with idiomatic Kotlin — data classes, extension functions, `runCatching`, and
no checked exceptions.

## Requirements

- JDK 11+
- Gradle (or use the wrapper once added)
- Windows (Java3D natives are Windows-only)

## Running

```sh
cd kotlin
gradle run
```

Or build a fat jar:

```sh
gradle jar
java -jar build/libs/oblivion-kotlin.jar
```

## What's different from the Java version

| Java | Kotlin |
|---|---|
| `class NifSVertex` with getters | `data class Vertex(val x, val y, val z)` |
| `NifInputStream` (250 lines) | `ByteBuffer` + 3 extension functions (15 lines) |
| Checked exceptions everywhere | `runCatching { }` |
| `new ArrayList<>()`, `files.get(i)` | `listOf()`, index operator, `map`/`filter` |
| `new BranchGroup(); root.addChild(...)` | `BranchGroup().apply { addChild(...) }` |
| ~450 lines total | ~100 lines total |
