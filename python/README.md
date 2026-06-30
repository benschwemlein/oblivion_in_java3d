# Oblivion Landscape Viewer — Python/wgpu

A Python port of the original Java3D NIF viewer using [wgpu-py](https://wgpu-py.readthedocs.io/).
Shares the same WGSL shader and NIF parsing logic as the Rust version.

## Requirements

Python 3.9+, a GPU with Vulkan, Metal, or DX12 support.

## Install

```sh
pip install -r requirements.txt
```

## Running

From the repo root:

```sh
python python/main.py
# or pass the landscape directory explicitly
python python/main.py path/to/landscape
# or via env var
LANDSCAPE_DIR=../landscape python python/main.py
```

## Controls

| Input | Action |
|---|---|
| Left-drag | Orbit camera |
| Right-drag | Pan |
| Scroll wheel | Zoom in/out |

## Files

- **`nif.py`** — binary NIF parser using Python's `struct` module
- **`main.py`** — wgpu-py renderer; same WGSL shader as the Rust version
