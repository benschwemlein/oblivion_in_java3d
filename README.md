# Oblivion Landscape Viewer

![Oblivion landscape](oblivion.jpg)

A personal hack that parses The Elder Scrolls IV: Oblivion landscape mesh files (.nif format) and renders them in 3D. Built for fun.

Reads `.nif` files from the `landscape/` directory, extracts triangle strip vertex data, and displays the terrain with basic directional lighting.

## Implementations

| Directory | Language | 3D Library |
|---|---|---|
| `java/` | Java | Java3D |
| `java21/` | Java 21 | Java3D |
| `kotlin/` | Kotlin | Java3D |
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
