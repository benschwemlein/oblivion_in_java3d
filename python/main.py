"""
Oblivion Landscape Viewer — Python/wgpu

Usage:
    python main.py [landscape_dir]

Controls:
    Left-drag   orbit
    Right-drag  pan
    Scroll      zoom
"""

import math
import os
import sys
from pathlib import Path

import numpy as np
import wgpu
from rendercanvas.auto import RenderCanvas, loop

import nif as nif_parser

TILE_OFFSET  = 131_072.0
DEFAULT_ZOOM = 500_000.0

SHADER = """
struct Camera { view_proj: mat4x4<f32> }
@group(0) @binding(0) var<uniform> camera: Camera;

struct VIn  { @location(0) pos: vec3<f32> }
struct VOut {
    @builtin(position) clip: vec4<f32>,
    @location(0) wpos: vec3<f32>,
}

@vertex fn vs(in: VIn) -> VOut {
    var o: VOut;
    o.clip = camera.view_proj * vec4<f32>(in.pos, 1.0);
    o.wpos = in.pos;
    return o;
}

@fragment fn fs(in: VOut) -> @location(0) vec4<f32> {
    let n = normalize(cross(dpdx(in.wpos), dpdy(in.wpos)));
    let d = clamp(dot(n, normalize(vec3<f32>(0.1, 0.3, 0.3))), 0.15, 1.0);
    return vec4<f32>(0.0, d, 0.1 * d, 1.0);
}
"""


# --- math helpers -----------------------------------------------------------

def _perspective(fov_y, aspect, near, far):
    f = 1.0 / math.tan(fov_y / 2.0)
    return np.array([
        [f / aspect, 0,  0,                         0],
        [0,          f,  0,                         0],
        [0,          0,  far / (near - far),       (far * near) / (near - far)],
        [0,          0, -1,                         0],
    ], dtype=np.float32)


def _look_at(eye, target, up):
    f = target - eye;  f /= np.linalg.norm(f)
    r = np.cross(f, up); r /= np.linalg.norm(r)
    u = np.cross(r, f)
    T = np.eye(4, dtype=np.float32)
    T[:3, 3] = -eye
    R = np.array([
        [r[0], r[1], r[2], 0],
        [u[0], u[1], u[2], 0],
        [-f[0], -f[1], -f[2], 0],
        [0, 0, 0, 1],
    ], dtype=np.float32)
    return R @ T


def _view_proj(yaw, pitch, zoom, pan, aspect):
    cy, sy = math.cos(yaw),   math.sin(yaw)
    cp, sp = math.cos(pitch), math.sin(pitch)
    # camera direction in world space
    dx = sy * cp
    dy = sp
    dz = cy * cp
    eye = pan + np.array([dx, dy, dz], dtype=np.float32) * zoom
    proj = _perspective(0.8, aspect, 1.0, 1e10)
    view = _look_at(eye, pan, np.array([0, 1, 0], dtype=np.float32))
    m = proj @ view
    # numpy is row-major; WGSL mat4x4 is column-major → transpose before upload
    return np.ascontiguousarray(m.T, dtype=np.float32)


# --- main -------------------------------------------------------------------

def main(landscape_dir):
    canvas = RenderCanvas(
        title="Oblivion Landscape Viewer — Python/wgpu",
        size=(1200, 800),
        update_mode="continuous",
    )

    adapter = wgpu.gpu.request_adapter_sync(power_preference="high-performance")
    device  = adapter.request_device_sync()

    context = canvas.get_context("wgpu")
    render_fmt = context.get_preferred_format(adapter)
    context.configure(device=device, format=render_fmt)

    shader = device.create_shader_module(code=SHADER)

    bgl = device.create_bind_group_layout(entries=[{
        "binding":    0,
        "visibility": wgpu.ShaderStage.VERTEX,
        "buffer":     {"type": wgpu.BufferBindingType.uniform},
    }])

    camera_buf = device.create_buffer(
        size=64,  # 4x4 float32
        usage=wgpu.BufferUsage.UNIFORM | wgpu.BufferUsage.COPY_DST,
    )

    bind_group = device.create_bind_group(
        layout=bgl,
        entries=[{"binding": 0, "resource": {"buffer": camera_buf, "offset": 0, "size": 64}}],
    )

    pipeline_layout = device.create_pipeline_layout(bind_group_layouts=[bgl])

    pipeline = device.create_render_pipeline(
        layout=pipeline_layout,
        vertex={
            "module":      shader,
            "entry_point": "vs",
            "buffers": [{
                "array_stride": 12,
                "step_mode":    wgpu.VertexStepMode.vertex,
                "attributes":   [{"format": wgpu.VertexFormat.float32x3, "offset": 0, "shader_location": 0}],
            }],
        },
        primitive={
            "topology":  wgpu.PrimitiveTopology.triangle_strip,
            "cull_mode": wgpu.CullMode.none,
        },
        depth_stencil={
            "format":               wgpu.TextureFormat.depth32float,
            "depth_write_enabled":  True,
            "depth_compare":        wgpu.CompareFunction.less,
        },
        fragment={
            "module":      shader,
            "entry_point": "fs",
            "targets":     [{"format": render_fmt}],
        },
    )

    # Load meshes
    meshes = []
    nif_files = sorted(Path(landscape_dir).glob("*.nif")) if Path(landscape_dir).exists() else []
    cols = max(1, math.ceil(math.sqrt(len(nif_files))))

    for i, path in enumerate(nif_files):
        try:
            verts = nif_parser.parse_nif(path)
        except Exception as e:
            print(f"skip {path.name}: {e}")
            continue
        if not verts:
            continue

        ox = (i % cols) * TILE_OFFSET
        oz = (i // cols) * TILE_OFFSET
        # NIF is Z-up → convert to Y-up (swap Y and Z)
        arr = np.array([[x + ox, z, y + oz] for x, y, z in verts], dtype=np.float32)

        vbuf = device.create_buffer_with_data(
            data=arr.tobytes(),
            usage=wgpu.BufferUsage.VERTEX,
        )
        meshes.append((vbuf, len(verts)))
        print(f"loaded {path.name}: {len(verts)} vertices")

    print(f"{len(meshes)} meshes loaded")

    # Depth texture (recreated on resize)
    depth_state = {"texture": None, "view": None, "size": (0, 0)}

    def ensure_depth(w, h):
        if depth_state["size"] != (w, h):
            depth_state["texture"] = device.create_texture(
                size=(w, h, 1),
                format=wgpu.TextureFormat.depth32float,
                usage=wgpu.TextureUsage.RENDER_ATTACHMENT,
            )
            depth_state["view"]  = depth_state["texture"].create_view()
            depth_state["size"]  = (w, h)

    # Camera state
    cam = {"yaw": 0.0, "pitch": 0.4, "zoom": DEFAULT_ZOOM, "pan": np.zeros(3, dtype=np.float32)}
    mouse = {"left": False, "right": False, "x": 0.0, "y": 0.0}

    def update_camera():
        w, h = canvas.get_physical_size()
        aspect = max(w, 1) / max(h, 1)
        m = _view_proj(cam["yaw"], cam["pitch"], cam["zoom"], cam["pan"], aspect)
        device.queue.write_buffer(camera_buf, 0, m.tobytes())

    update_camera()

    @canvas.add_event_handler("pointer_down", "pointer_up", "pointer_move", "wheel")
    def on_event(event):
        t = event["event_type"]

        if t == "pointer_down":
            btn = event.get("button", 0)
            if btn == 1: mouse["left"]  = True
            if btn == 2: mouse["right"] = True
            mouse["x"] = event["x"]
            mouse["y"] = event["y"]

        elif t == "pointer_up":
            btn = event.get("button", 0)
            if btn == 1: mouse["left"]  = False
            if btn == 2: mouse["right"] = False

        elif t == "pointer_move":
            dx = event["x"] - mouse["x"]
            dy = event["y"] - mouse["y"]
            mouse["x"] = event["x"]
            mouse["y"] = event["y"]

            if mouse["left"]:
                cam["yaw"]   -= dx * 0.005
                cam["pitch"]  = max(-1.4, min(1.4, cam["pitch"] + dy * 0.005))
                update_camera()

            elif mouse["right"]:
                yaw = cam["yaw"]
                right = np.array([ math.cos(yaw), 0, -math.sin(yaw)], dtype=np.float32)
                up    = np.array([0, 1, 0], dtype=np.float32)
                speed = cam["zoom"] * 0.001
                cam["pan"] -= right * dx * speed
                cam["pan"] += up    * dy * speed
                update_camera()

        elif t == "wheel":
            dy = event.get("dy", 0)
            cam["zoom"] = max(1.0, min(1e9, cam["zoom"] * (1.0 + dy * 0.0015)))
            update_camera()

    def draw():
        w, h = canvas.get_physical_size()
        ensure_depth(w, h)

        target = context.get_current_texture()
        view   = target.create_view()
        enc    = device.create_command_encoder()

        rp = enc.begin_render_pass(
            color_attachments=[{
                "view":           view,
                "resolve_target": None,
                "clear_value":    (0.05, 0.05, 0.1, 1.0),
                "load_op":        wgpu.LoadOp.clear,
                "store_op":       wgpu.StoreOp.store,
            }],
            depth_stencil_attachment={
                "view":             depth_state["view"],
                "depth_clear_value": 1.0,
                "depth_load_op":    wgpu.LoadOp.clear,
                "depth_store_op":   wgpu.StoreOp.store,
            },
        )
        rp.set_pipeline(pipeline)
        rp.set_bind_group(0, bind_group, [], 0, 99)
        for vbuf, count in meshes:
            rp.set_vertex_buffer(0, vbuf)
            rp.draw(count)
        rp.end()

        device.queue.submit([enc.finish()])

    canvas.request_draw(draw)
    loop.run()


if __name__ == "__main__":
    landscape_dir = sys.argv[1] if len(sys.argv) > 1 else os.environ.get("LANDSCAPE_DIR", "../landscape")
    main(landscape_dir)
