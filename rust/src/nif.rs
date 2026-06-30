use std::fs;
use std::path::Path;

pub fn parse_nif(path: &Path) -> anyhow::Result<Vec<[f32; 3]>> {
    let data = fs::read(path)?;
    parse_bytes(&data)
}

fn u8(data: &[u8], pos: &mut usize) -> u8 {
    let v = data[*pos];
    *pos += 1;
    v
}

fn u16(data: &[u8], pos: &mut usize) -> u16 {
    let v = u16::from_le_bytes([data[*pos], data[*pos + 1]]);
    *pos += 2;
    v
}

fn u32(data: &[u8], pos: &mut usize) -> u32 {
    let v = u32::from_le_bytes([data[*pos], data[*pos+1], data[*pos+2], data[*pos+3]]);
    *pos += 4;
    v
}

fn f32(data: &[u8], pos: &mut usize) -> f32 {
    let v = std::primitive::f32::from_le_bytes([data[*pos], data[*pos+1], data[*pos+2], data[*pos+3]]);
    *pos += 4;
    v
}

// Read printable ASCII string — stops at any byte <= 32, then consumes the terminator.
fn read_string(data: &[u8], pos: &mut usize) -> String {
    let mut s = String::new();
    while *pos < data.len() && data[*pos] > 32 {
        s.push(data[*pos] as char);
        *pos += 1;
    }
    if *pos < data.len() {
        *pos += 1; // consume terminator byte
    }
    s
}

fn skip_zeros(data: &[u8], pos: &mut usize) {
    while *pos < data.len() && data[*pos] == 0 {
        *pos += 1;
    }
}

// Scan forward for 4 consecutive 0xFF bytes (NIF null reference), leave pos just after them.
fn skip_to_ref(data: &[u8], pos: &mut usize) -> bool {
    while *pos + 4 <= data.len() {
        if data[*pos] == 0xFF && data[*pos+1] == 0xFF
            && data[*pos+2] == 0xFF && data[*pos+3] == 0xFF
        {
            *pos += 4;
            return true;
        }
        *pos += 1;
    }
    false
}

fn parse_bytes(data: &[u8]) -> anyhow::Result<Vec<[f32; 3]>> {
    let mut pos = 0;

    // Skip NIF file header (version string + metadata fields).
    // Java code empirically skips 48 bytes before num_blocks.
    pos += 48;

    let num_blocks = u32(data, &mut pos) as usize;
    pos += 10; // unknown header bytes
    let num_block_types = u16(data, &mut pos) as usize;
    pos += 4;  // unknown int

    for _ in 0..num_block_types {
        let _name = read_string(data, &mut pos);
        skip_zeros(data, &mut pos);
    }

    // Block type index per block
    for _ in 0..num_blocks {
        u16(data, &mut pos);
    }

    // Skip to 3rd null reference to reach NiTriStripsData header
    for _ in 0..3 {
        if !skip_to_ref(data, &mut pos) {
            anyhow::bail!("unexpected EOF scanning for NIF ref");
        }
    }
    skip_zeros(data, &mut pos);

    let num_vertices = u16(data, &mut pos) as usize;
    pos += 2; // unknown short
    pos += 1; // has_vertices byte

    let mut vertices: Vec<[f32; 3]> = Vec::with_capacity(num_vertices);
    let mut largest = 0.0_f32;

    for _ in 0..num_vertices {
        let x = f32(data, &mut pos);
        let y = f32(data, &mut pos);
        let z = f32(data, &mut pos);
        largest = largest.max(x.abs()).max(y.abs()).max(z.abs());
        vertices.push([x, y, z]);
    }

    skip_to_ref(data, &mut pos);

    let _num_triangles = u16(data, &mut pos);
    let _num_strips   = u16(data, &mut pos);
    let strip_size    = u16(data, &mut pos) as usize;
    pos += 1; // unknown byte

    let mut output = Vec::with_capacity(strip_size);
    for _ in 0..strip_size {
        let idx = u16(data, &mut pos) as usize;
        if idx < vertices.len() {
            output.push(vertices[idx]);
        }
    }

    Ok(output)
}
