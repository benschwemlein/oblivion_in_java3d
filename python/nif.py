import struct
from pathlib import Path


def parse_nif(path):
    return _parse(Path(path).read_bytes())


def _u8(data, pos):
    return data[pos], pos + 1

def _u16(data, pos):
    return struct.unpack_from('<H', data, pos)[0], pos + 2

def _u32(data, pos):
    return struct.unpack_from('<I', data, pos)[0], pos + 4

def _f32(data, pos):
    return struct.unpack_from('<f', data, pos)[0], pos + 4

def _string(data, pos):
    end = pos
    while end < len(data) and data[end] > 32:
        end += 1
    return data[pos:end].decode('ascii', errors='replace'), end + 1

def _skip_zeros(data, pos):
    while pos < len(data) and data[pos] == 0:
        pos += 1
    return pos

def _skip_to_ref(data, pos):
    while pos + 4 <= len(data):
        if data[pos:pos+4] == b'\xff\xff\xff\xff':
            return pos + 4, True
        pos += 1
    return pos, False


def _parse(data):
    pos = 48  # skip NIF file header

    num_blocks, pos = _u32(data, pos)
    pos += 10
    num_block_types, pos = _u16(data, pos)
    pos += 4

    for _ in range(num_block_types):
        _, pos = _string(data, pos)
        pos = _skip_zeros(data, pos)

    pos += num_blocks * 2  # block type index per block

    # Skip to 3rd null reference to reach NiTriStripsData
    for _ in range(3):
        pos, ok = _skip_to_ref(data, pos)
        if not ok:
            return []
    pos = _skip_zeros(data, pos)

    num_vertices, pos = _u16(data, pos)
    pos += 3  # unknown short + has_vertices byte

    vertices = []
    for _ in range(num_vertices):
        x, pos = _f32(data, pos)
        y, pos = _f32(data, pos)
        z, pos = _f32(data, pos)
        vertices.append((x, y, z))

    pos, _ = _skip_to_ref(data, pos)

    _, pos = _u16(data, pos)  # num_triangles
    _, pos = _u16(data, pos)  # num_strips
    strip_size, pos = _u16(data, pos)
    pos += 1

    result = []
    for _ in range(strip_size):
        idx, pos = _u16(data, pos)
        if idx < len(vertices):
            result.append(vertices[idx])

    return result
