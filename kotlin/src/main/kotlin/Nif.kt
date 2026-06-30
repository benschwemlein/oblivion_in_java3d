import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class Vertex(val x: Float, val y: Float, val z: Float)

fun parseNif(file: File): List<Vertex> {
    val buf = ByteBuffer.wrap(file.readBytes()).order(ByteOrder.LITTLE_ENDIAN)

    buf.position(48) // skip NIF file header

    val numBlocks     = buf.int
    buf.position(buf.position() + 10)
    val numBlockTypes = buf.short.toUShort().toInt()
    buf.int // unknown

    repeat(numBlockTypes) {
        buf.readString()
        buf.skipZeros()
    }

    repeat(numBlocks) { buf.short } // block type index per block

    repeat(3) { buf.skipToRef() }  // skip to NiTriStripsData
    buf.skipZeros()

    val numVertices = buf.short.toUShort().toInt()
    buf.short  // unknown
    buf.get()  // has_vertices

    val vertices = Array(numVertices) { Vertex(buf.float, buf.float, buf.float) }

    buf.skipToRef()

    buf.short  // num_triangles
    buf.short  // num_strips
    val stripSize = buf.short.toUShort().toInt()
    buf.get()  // unknown byte

    return (0 until stripSize).mapNotNull { vertices.getOrNull(buf.short.toUShort().toInt()) }
}

private fun ByteBuffer.readString(): String {
    val sb = StringBuilder()
    while (hasRemaining() && get(position()) > 32) sb.append(get().toInt().toChar())
    if (hasRemaining()) get() // consume terminator
    return sb.toString()
}

private fun ByteBuffer.skipZeros() {
    while (hasRemaining() && get(position()) == 0.toByte()) get()
}

private fun ByteBuffer.skipToRef() {
    val ref = byteArrayOf(-1, -1, -1, -1)
    while (remaining() >= 4) {
        if (get(position()) == ref[0] && get(position()+1) == ref[1] &&
            get(position()+2) == ref[2] && get(position()+3) == ref[3]) {
            position(position() + 4)
            return
        }
        get()
    }
}
