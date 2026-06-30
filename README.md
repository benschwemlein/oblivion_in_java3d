# Oblivion Landscape Viewer

![Oblivion landscape](oblivion.jpg)

A personal hack that parses The Elder Scrolls IV: Oblivion landscape mesh files (.nif format) and renders them in 3D. Built for fun.

Reads `.nif` files from the `landscape/` directory, extracts triangle strip vertex data, and displays the terrain with basic directional lighting.

## NIF Format

NIF (NetImmerse Format) is the binary mesh format used by Bethesda games. These landscape files are little-endian and don't follow the full NIF spec cleanly, so parsing is done by scanning for known markers rather than walking a block table.

### File layout (as parsed here)

```
[header]
  "Gamebryo File Format, Version 20.2.0.7\n"  — fixed ASCII string
  followed by version bytes and flags
  total: 48 bytes skipped

[block count]        uint32  — number of blocks in the file

[block type count]   uint32  — number of distinct block type strings

[block type strings] null-terminated ASCII strings, one per type
                     e.g. "NiTriStripsData", "NiNode", ...

[block type indices] uint16 per block — which type string each block is

[null refs]          0xFFFFFFFF repeated — block reference sentinels

--- scan from here ---

Skip to the 3rd 0xFFFFFFFF sentinel. The block immediately after is
a NiTriStripsData block containing:

  [vertex count]   uint16
  [vertices]       vertex_count × (float x, float y, float z)  — 12 bytes each
                   coordinate system is Z-up; converted to Y-up at render time

Skip to the next 0xFFFFFFFF sentinel after the vertex array. Then:

  [strip count]    uint16
  [strip lengths]  strip_count × uint16
  [strip indices]  indices into the vertex array, forming triangle strips
```

### Why scan instead of parse properly?

The block table approach would require implementing the full NIF spec across many block types. Since we only need one block (NiTriStripsData) per file and its position relative to the sentinel refs is consistent across all Oblivion landscape tiles, scanning is simpler and more robust for this specific use case.

### Coordinate system

NIF uses Z-up. The renderers swap Y and Z on load so the terrain sits upright in a Y-up world (standard for OpenGL/wgpu).

### Tile layout

Each `.nif` file is one landscape tile. Tiles are arranged in a grid with 131,072 units of spacing (matching Oblivion's cell size in NIF coordinate units).

## Implementations

| Directory | Language | 3D Library |
|---|---|---|
| `java/` | Java | Java3D |
| `java21/` | Java 21 | LWJGL |
| `kotlin/` | Kotlin | LWJGL |
| `rust/` | Rust | wgpu |
| `python/` | Python | wgpu-py |

## Running

**Java** — requires Java3D (Windows natives included in `java3d/`):
```sh
javac java/*.java
java -cp java startgui
```

**Java 21:**
```sh
cd java21 && gradle run
```

**Kotlin:**
```sh
cd kotlin && gradle run
```

**Rust:**
```sh
cd rust && cargo run --release
```

**Python:**
```sh
pip install -r python/requirements.txt
python python/main.py
```
