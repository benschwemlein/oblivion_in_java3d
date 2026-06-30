# Oblivion Landscape Viewer — Rust/wgpu

A port of the original Java3D NIF viewer to Rust using [wgpu](https://wgpu.rs/).

Parses Oblivion `.nif` landscape mesh files and renders them in a 3D window with mouse controls.

## Requirements

- Rust 1.70+
- A GPU with Vulkan, Metal, or DX12 support

## Running

From the repo root:

```sh
cd rust
cargo run --release
```

The viewer looks for `.nif` files in `../landscape/` by default. Override with:

```sh
LANDSCAPE_DIR=/path/to/landscape cargo run --release
```

## Controls

| Input | Action |
|---|---|
| Left-drag | Orbit camera |
| Right-drag | Pan |
| Scroll wheel | Zoom in/out |

## How it works

**NIF parsing (`src/nif.rs`)** — reads the binary NIF 20.0.0.5 format:
skips the file header, scans for null reference markers (`0xFFFFFFFF`),
reads the vertex array and triangle strip indices from the `NiTriStripsData` block,
then reorders vertices by the strip index list.

**Rendering (`src/main.rs`, `src/shader.wgsl`)** — each `.nif` file becomes
a triangle-strip draw call. The WGSL fragment shader reconstructs per-face normals
from screen-space derivatives (`dpdx`/`dpdy`) and applies a directional light,
producing a green shaded look that matches the original Java3D version.
Tiles are laid out in a square grid with 131,072-unit spacing.

## Differences from the Java version

- Uses `f32::from_le_bytes` for correct little-endian float parsing (the Java code had a byte-swap bug in its float reader).
- No Swing UI — just a native window via `winit`.
- All `.nif` files in the landscape directory are loaded automatically rather than a hardcoded 6×6 grid.
- NIF Z-up coordinates are converted to Y-up for wgpu.
