import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Nif {

    public static List<Vertex> parse(File file) throws IOException {
        var buf = ByteBuffer.wrap(Files.readAllBytes(file.toPath()))
                            .order(ByteOrder.LITTLE_ENDIAN);

        buf.position(48); // skip NIF file header

        var numBlocks     = buf.getInt();
        buf.position(buf.position() + 10);
        var numBlockTypes = Short.toUnsignedInt(buf.getShort());
        buf.getInt(); // unknown

        for (int i = 0; i < numBlockTypes; i++) {
            readString(buf);
            skipZeros(buf);
        }

        for (int i = 0; i < numBlocks; i++) buf.getShort(); // block type indices

        for (int i = 0; i < 3; i++) skipToRef(buf); // skip to NiTriStripsData
        skipZeros(buf);

        var numVertices = Short.toUnsignedInt(buf.getShort());
        buf.getShort(); // unknown
        buf.get();      // has_vertices

        var vertices = new Vertex[numVertices];
        for (int i = 0; i < numVertices; i++)
            vertices[i] = new Vertex(buf.getFloat(), buf.getFloat(), buf.getFloat());

        skipToRef(buf);

        buf.getShort(); // num_triangles
        buf.getShort(); // num_strips
        var stripSize = Short.toUnsignedInt(buf.getShort());
        buf.get();      // unknown byte

        var result = new ArrayList<Vertex>(stripSize);
        for (int i = 0; i < stripSize; i++) {
            var idx = Short.toUnsignedInt(buf.getShort());
            if (idx < vertices.length) result.add(vertices[idx]);
        }

        return result;
    }

    private static String readString(ByteBuffer buf) {
        var sb = new StringBuilder();
        while (buf.hasRemaining() && buf.get(buf.position()) > 32)
            sb.append((char) buf.get());
        if (buf.hasRemaining()) buf.get(); // consume terminator
        return sb.toString();
    }

    private static void skipZeros(ByteBuffer buf) {
        while (buf.hasRemaining() && buf.get(buf.position()) == 0) buf.get();
    }

    private static void skipToRef(ByteBuffer buf) {
        while (buf.remaining() >= 4) {
            if (buf.get(buf.position())   == (byte) -1 && buf.get(buf.position()+1) == (byte) -1 &&
                buf.get(buf.position()+2) == (byte) -1 && buf.get(buf.position()+3) == (byte) -1) {
                buf.position(buf.position() + 4);
                return;
            }
            buf.get();
        }
    }
}
