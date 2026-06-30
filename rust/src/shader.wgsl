struct Camera {
    view_proj: mat4x4<f32>,
}

@group(0) @binding(0)
var<uniform> camera: Camera;

struct VertIn {
    @location(0) pos: vec3<f32>,
}

struct VertOut {
    @builtin(position) clip_pos: vec4<f32>,
    @location(0) world_pos: vec3<f32>,
}

@vertex
fn vs_main(in: VertIn) -> VertOut {
    var out: VertOut;
    out.clip_pos  = camera.view_proj * vec4<f32>(in.pos, 1.0);
    out.world_pos = in.pos;
    return out;
}

@fragment
fn fs_main(in: VertOut) -> @location(0) vec4<f32> {
    // Reconstruct face normal from screen-space derivatives of world position.
    let normal    = normalize(cross(dpdx(in.world_pos), dpdy(in.world_pos)));
    let light_dir = normalize(vec3<f32>(0.1, 0.3, 0.3));
    let diffuse   = clamp(dot(normal, light_dir), 0.15, 1.0);
    // Match Java's green (0.0, 1.0, 0.1) flat appearance with basic shading.
    return vec4<f32>(0.0, diffuse, 0.1 * diffuse, 1.0);
}
