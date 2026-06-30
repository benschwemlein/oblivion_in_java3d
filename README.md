# Oblivion Landscape Viewer

![Oblivion landscape](oblivion.jpg)

A personal hack that parses The Elder Scrolls IV: Oblivion landscape mesh files (.nif format) and renders them in 3D. Built for fun.

Reads `.nif` files from the `landscape/` directory, extracts triangle strip vertex data, and displays the terrain with basic directional lighting.

## Implementations

| Directory | Language | 3D Library |
|---|---|---|
| `src/` | Java | Java3D |
| `rust/` | Rust | wgpu |
| `python/` | Python | wgpu-py |

## Running

**Java** — requires Java3D (Windows natives included in `java3d/`):
```sh
javac src/*.java
java -cp src Nif landscape/
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
