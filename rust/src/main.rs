use anyhow::Result;
use bytemuck::{Pod, Zeroable};
use glam::{Mat4, Quat, Vec3};
use std::path::Path;
use wgpu::util::DeviceExt;
use winit::{
    event::*,
    event_loop::{ControlFlow, EventLoop},
    window::WindowBuilder,
};

mod nif;

// NIF vertex coordinates can be large (Oblivion world units).
// Adjust if the scene appears too close or too far.
const DEFAULT_ZOOM: f32 = 500_000.0;
// Spacing between landscape tiles in world units, matching Java's offset value.
const TILE_OFFSET: f32 = 131_072.0;

#[repr(C)]
#[derive(Copy, Clone, Pod, Zeroable)]
struct Vertex {
    pos: [f32; 3],
}

impl Vertex {
    const ATTRIBS: [wgpu::VertexAttribute; 1] = wgpu::vertex_attr_array![0 => Float32x3];

    fn desc() -> wgpu::VertexBufferLayout<'static> {
        wgpu::VertexBufferLayout {
            array_stride: std::mem::size_of::<Self>() as wgpu::BufferAddress,
            step_mode: wgpu::VertexStepMode::Vertex,
            attributes: &Self::ATTRIBS,
        }
    }
}

#[repr(C)]
#[derive(Copy, Clone, Pod, Zeroable)]
struct CameraUniform {
    view_proj: [[f32; 4]; 4],
}

struct Camera {
    yaw: f32,
    pitch: f32,
    zoom: f32,
    pan: Vec3,
}

impl Camera {
    fn new() -> Self {
        Self { yaw: 0.0, pitch: 0.4, zoom: DEFAULT_ZOOM, pan: Vec3::ZERO }
    }

    fn view_proj(&self, aspect: f32) -> Mat4 {
        let proj = Mat4::perspective_rh(0.8, aspect, 1.0, 1e10);
        let rot  = Quat::from_rotation_y(self.yaw) * Quat::from_rotation_x(-self.pitch);
        let eye  = rot * Vec3::new(0.0, 0.0, self.zoom) + self.pan;
        let view = Mat4::look_at_rh(eye, self.pan, Vec3::Y);
        proj * view
    }
}

struct Mesh {
    vertex_buf:   wgpu::Buffer,
    vertex_count: u32,
}

struct State {
    surface:            wgpu::Surface,
    device:             wgpu::Device,
    queue:              wgpu::Queue,
    config:             wgpu::SurfaceConfiguration,
    size:               winit::dpi::PhysicalSize<u32>,
    render_pipeline:    wgpu::RenderPipeline,
    meshes:             Vec<Mesh>,
    camera:             Camera,
    camera_buf:         wgpu::Buffer,
    camera_bind_group:  wgpu::BindGroup,
    depth_view:         wgpu::TextureView,
    mouse_left:         bool,
    mouse_right:        bool,
    last_mouse:         Option<(f64, f64)>,
}

impl State {
    async fn new(window: &winit::window::Window, landscape_dir: &Path) -> Result<Self> {
        let size = window.inner_size();

        let instance = wgpu::Instance::new(wgpu::InstanceDescriptor {
            backends: wgpu::Backends::all(),
            ..Default::default()
        });

        let surface = instance.create_surface(window)?;

        let adapter = instance.request_adapter(&wgpu::RequestAdapterOptions {
            power_preference: wgpu::PowerPreference::HighPerformance,
            compatible_surface: Some(&surface),
            force_fallback_adapter: false,
        }).await.expect("no compatible GPU adapter found");

        let (device, queue) = adapter.request_device(
            &wgpu::DeviceDescriptor {
                label: None,
                required_features: wgpu::Features::empty(),
                required_limits: wgpu::Limits::default(),
            },
            None,
        ).await?;

        let caps   = surface.get_capabilities(&adapter);
        let format = caps.formats.iter().find(|f| f.is_srgb()).copied()
            .unwrap_or(caps.formats[0]);

        let config = wgpu::SurfaceConfiguration {
            usage:                        wgpu::TextureUsages::RENDER_ATTACHMENT,
            format,
            width:                        size.width,
            height:                       size.height,
            present_mode:                 wgpu::PresentMode::Fifo,
            desired_maximum_frame_latency: 2,
            alpha_mode:                   caps.alpha_modes[0],
            view_formats:                 vec![],
        };
        surface.configure(&device, &config);

        let depth_view = make_depth_view(&device, size.width, size.height);

        // Camera uniform buffer
        let camera        = Camera::new();
        let camera_init   = CameraUniform { view_proj: Mat4::IDENTITY.to_cols_array_2d() };
        let camera_buf    = device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
            label:    None,
            contents: bytemuck::cast_slice(&[camera_init]),
            usage:    wgpu::BufferUsages::UNIFORM | wgpu::BufferUsages::COPY_DST,
        });

        let bgl = device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
            label:   None,
            entries: &[wgpu::BindGroupLayoutEntry {
                binding:    0,
                visibility: wgpu::ShaderStages::VERTEX,
                ty: wgpu::BindingType::Buffer {
                    ty:                 wgpu::BufferBindingType::Uniform,
                    has_dynamic_offset: false,
                    min_binding_size:   None,
                },
                count: None,
            }],
        });

        let camera_bind_group = device.create_bind_group(&wgpu::BindGroupDescriptor {
            label:   None,
            layout:  &bgl,
            entries: &[wgpu::BindGroupEntry {
                binding:  0,
                resource: camera_buf.as_entire_binding(),
            }],
        });

        let shader = device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label:  None,
            source: wgpu::ShaderSource::Wgsl(include_str!("shader.wgsl").into()),
        });

        let pipeline_layout = device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
            label:                None,
            bind_group_layouts:   &[&bgl],
            push_constant_ranges: &[],
        });

        let render_pipeline = device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
            label:  None,
            layout: Some(&pipeline_layout),
            vertex: wgpu::VertexState {
                module:      &shader,
                entry_point: "vs_main",
                buffers:     &[Vertex::desc()],
            },
            fragment: Some(wgpu::FragmentState {
                module:      &shader,
                entry_point: "fs_main",
                targets: &[Some(wgpu::ColorTargetState {
                    format,
                    blend:      Some(wgpu::BlendState::REPLACE),
                    write_mask: wgpu::ColorWrites::ALL,
                })],
            }),
            primitive: wgpu::PrimitiveState {
                topology:  wgpu::PrimitiveTopology::TriangleStrip,
                cull_mode: None,
                ..Default::default()
            },
            depth_stencil: Some(wgpu::DepthStencilState {
                format:               wgpu::TextureFormat::Depth32Float,
                depth_write_enabled:  true,
                depth_compare:        wgpu::CompareFunction::Less,
                stencil:              Default::default(),
                bias:                 Default::default(),
            }),
            multisample: Default::default(),
            multiview:   None,
        });

        // Load all .nif files from the landscape directory, tiled in a grid.
        let mut meshes = Vec::new();

        if landscape_dir.exists() {
            let mut paths: Vec<_> = std::fs::read_dir(landscape_dir)?
                .filter_map(|e| e.ok())
                .map(|e| e.path())
                .filter(|p| p.extension().map(|x| x == "nif").unwrap_or(false))
                .collect();
            paths.sort();

            eprintln!("Found {} .nif files", paths.len());

            let cols = ((paths.len() as f32).sqrt().ceil() as usize).max(1);

            for (i, path) in paths.iter().enumerate() {
                match nif::parse_nif(path) {
                    Ok(verts) if !verts.is_empty() => {
                        let col = (i % cols) as f32;
                        let row = (i / cols) as f32;
                        let ox  = col * TILE_OFFSET;
                        let oz  = row * TILE_OFFSET;

                        // NIF uses Z-up; convert to Y-up for wgpu (swap Y and Z).
                        let gpu_verts: Vec<Vertex> = verts.iter()
                            .map(|&[x, y, z]| Vertex { pos: [x + ox, z, y + oz] })
                            .collect();

                        let vertex_buf = device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
                            label:    None,
                            contents: bytemuck::cast_slice(&gpu_verts),
                            usage:    wgpu::BufferUsages::VERTEX,
                        });
                        meshes.push(Mesh { vertex_buf, vertex_count: gpu_verts.len() as u32 });
                    }
                    Ok(_)    => eprintln!("empty: {:?}", path.file_name().unwrap()),
                    Err(e)   => eprintln!("skip {:?}: {}", path.file_name().unwrap(), e),
                }
            }

            eprintln!("Loaded {} meshes", meshes.len());
        } else {
            eprintln!("landscape dir not found: {:?}", landscape_dir);
            eprintln!("Run from the repo root, or pass a path via LANDSCAPE_DIR env var.");
        }

        Ok(Self {
            surface, device, queue, config, size,
            render_pipeline, meshes,
            camera, camera_buf, camera_bind_group,
            depth_view,
            mouse_left: false, mouse_right: false, last_mouse: None,
        })
    }

    fn resize(&mut self, new_size: winit::dpi::PhysicalSize<u32>) {
        if new_size.width > 0 && new_size.height > 0 {
            self.size   = new_size;
            self.config.width  = new_size.width;
            self.config.height = new_size.height;
            self.surface.configure(&self.device, &self.config);
            self.depth_view = make_depth_view(&self.device, new_size.width, new_size.height);
        }
    }

    fn update_camera(&self) {
        let aspect  = self.size.width as f32 / self.size.height as f32;
        let uniform = CameraUniform { view_proj: self.camera.view_proj(aspect).to_cols_array_2d() };
        self.queue.write_buffer(&self.camera_buf, 0, bytemuck::cast_slice(&[uniform]));
    }

    fn render(&mut self) -> Result<(), wgpu::SurfaceError> {
        let output = self.surface.get_current_texture()?;
        let view   = output.texture.create_view(&Default::default());
        let mut enc = self.device.create_command_encoder(&Default::default());

        {
            let mut rp = enc.begin_render_pass(&wgpu::RenderPassDescriptor {
                label: None,
                color_attachments: &[Some(wgpu::RenderPassColorAttachment {
                    view:           &view,
                    resolve_target: None,
                    ops: wgpu::Operations {
                        load:  wgpu::LoadOp::Clear(wgpu::Color { r: 0.05, g: 0.05, b: 0.1, a: 1.0 }),
                        store: wgpu::StoreOp::Store,
                    },
                })],
                depth_stencil_attachment: Some(wgpu::RenderPassDepthStencilAttachment {
                    view: &self.depth_view,
                    depth_ops: Some(wgpu::Operations {
                        load:  wgpu::LoadOp::Clear(1.0),
                        store: wgpu::StoreOp::Store,
                    }),
                    stencil_ops: None,
                }),
                ..Default::default()
            });

            rp.set_pipeline(&self.render_pipeline);
            rp.set_bind_group(0, &self.camera_bind_group, &[]);
            for mesh in &self.meshes {
                rp.set_vertex_buffer(0, mesh.vertex_buf.slice(..));
                rp.draw(0..mesh.vertex_count, 0..1);
            }
        }

        self.queue.submit(std::iter::once(enc.finish()));
        output.present();
        Ok(())
    }
}

fn make_depth_view(device: &wgpu::Device, width: u32, height: u32) -> wgpu::TextureView {
    device.create_texture(&wgpu::TextureDescriptor {
        label:             None,
        size:              wgpu::Extent3d { width, height, depth_or_array_layers: 1 },
        mip_level_count:   1,
        sample_count:      1,
        dimension:         wgpu::TextureDimension::D2,
        format:            wgpu::TextureFormat::Depth32Float,
        usage:             wgpu::TextureUsages::RENDER_ATTACHMENT,
        view_formats:      &[],
    }).create_view(&Default::default())
}

fn main() -> Result<()> {
    env_logger::init();

    let landscape_dir = std::env::var("LANDSCAPE_DIR")
        .map(std::path::PathBuf::from)
        .unwrap_or_else(|_| {
            // Try common locations relative to CWD
            for candidate in &["../landscape", "landscape", "../../landscape"] {
                let p = Path::new(candidate);
                if p.exists() { return p.to_path_buf(); }
            }
            Path::new("../landscape").to_path_buf()
        });

    let event_loop = EventLoop::new();
    let window = WindowBuilder::new()
        .with_title("Oblivion Landscape Viewer — Rust/wgpu")
        .with_inner_size(winit::dpi::LogicalSize::new(1200u32, 800u32))
        .build(&event_loop)?;

    let mut state = pollster::block_on(State::new(&window, &landscape_dir))?;
    state.update_camera();

    event_loop.run(move |event, _, control_flow| {
        *control_flow = ControlFlow::Poll;

        match event {
            Event::WindowEvent { event, .. } => match event {
                WindowEvent::CloseRequested => *control_flow = ControlFlow::Exit,

                WindowEvent::Resized(size) => {
                    state.resize(size);
                    state.update_camera();
                }

                WindowEvent::MouseInput { button, state: btn_state, .. } => {
                    let pressed = btn_state == ElementState::Pressed;
                    match button {
                        MouseButton::Left  => state.mouse_left  = pressed,
                        MouseButton::Right => state.mouse_right = pressed,
                        _ => {}
                    }
                    if !pressed { state.last_mouse = None; }
                }

                WindowEvent::CursorMoved { position, .. } => {
                    let (x, y) = (position.x, position.y);
                    if let Some((lx, ly)) = state.last_mouse {
                        let dx = (x - lx) as f32;
                        let dy = (y - ly) as f32;

                        if state.mouse_left {
                            state.camera.yaw   -= dx * 0.005;
                            state.camera.pitch  = (state.camera.pitch + dy * 0.005).clamp(-1.4, 1.4);
                        } else if state.mouse_right {
                            // Pan in camera-local right/up directions
                            let rot   = Quat::from_rotation_y(state.camera.yaw)
                                      * Quat::from_rotation_x(-state.camera.pitch);
                            let right = rot * Vec3::X;
                            let up    = rot * Vec3::Y;
                            let speed = state.camera.zoom * 0.001;
                            state.camera.pan -= right * dx * speed;
                            state.camera.pan += up    * dy * speed;
                        }
                        state.update_camera();
                    }
                    if state.mouse_left || state.mouse_right {
                        state.last_mouse = Some((x, y));
                    }
                }

                WindowEvent::MouseWheel { delta, .. } => {
                    let scroll = match delta {
                        MouseScrollDelta::LineDelta(_, y)  => y,
                        MouseScrollDelta::PixelDelta(d)    => d.y as f32 * 0.01,
                    };
                    state.camera.zoom = (state.camera.zoom * (1.0 - scroll * 0.15))
                        .clamp(1.0, 1e9);
                    state.update_camera();
                }

                _ => {}
            },

            Event::RedrawRequested(_) => match state.render() {
                Ok(_)                                  => {}
                Err(wgpu::SurfaceError::Lost)          => state.resize(state.size),
                Err(wgpu::SurfaceError::OutOfMemory)   => *control_flow = ControlFlow::Exit,
                Err(e)                                 => eprintln!("{e:?}"),
            },

            Event::MainEventsCleared => window.request_redraw(),

            _ => {}
        }
    });
}
