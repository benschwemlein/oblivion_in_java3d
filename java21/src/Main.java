import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Main {
    public static void main(String[] args) {
        new LandscapeViewer(new File("../landscape")).run();
    }
}

class LandscapeViewer {

    private static final String VERT = """
        #version 330 core
        layout(location = 0) in vec3 pos;
        uniform mat4 viewProj;
        out vec3 worldPos;
        void main() {
            gl_Position = viewProj * vec4(pos, 1.0);
            worldPos = pos;
        }
        """;

    private static final String FRAG = """
        #version 330 core
        in vec3 worldPos;
        out vec4 fragColor;
        void main() {
            vec3 n = normalize(cross(dFdx(worldPos), dFdy(worldPos)));
            float d = clamp(dot(n, normalize(vec3(0.1, 0.3, 0.3))), 0.15, 1.0);
            fragColor = vec4(0.0, d, 0.1 * d, 1.0);
        }
        """;

    private record Mesh(int vao, int count) {}

    private final File landscapeDir;

    private float yaw = 0, pitch = 0.4f, zoom = 500_000f;
    private float panX, panY, panZ;
    private double lastX, lastY;
    private boolean mouseLeft, mouseRight;

    LandscapeViewer(File landscapeDir) {
        this.landscapeDir = landscapeDir;
    }

    void run() {
        GLFWErrorCallback.createPrint(System.err).set();
        glfwInit();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE); // required on macOS

        long window = glfwCreateWindow(1200, 800, "Oblivion Landscape — Java 21/LWJGL", NULL, NULL);
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        GL.createCapabilities();

        glfwSetMouseButtonCallback(window, (w, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT)  mouseLeft  = action == GLFW_PRESS;
            if (button == GLFW_MOUSE_BUTTON_RIGHT) mouseRight = action == GLFW_PRESS;
            if (action == GLFW_PRESS) {
                try (var stack = stackPush()) {
                    var xb = stack.mallocDouble(1); var yb = stack.mallocDouble(1);
                    glfwGetCursorPos(w, xb, yb);
                    lastX = xb.get(0); lastY = yb.get(0);
                }
            }
        });

        glfwSetCursorPosCallback(window, (w, x, y) -> {
            float dx = (float)(x - lastX), dy = (float)(y - lastY);
            if (mouseLeft) {
                yaw   -= dx * 0.005f;
                pitch  = Math.clamp(pitch + dy * 0.005f, -1.4f, 1.4f);
            } else if (mouseRight) {
                float speed = zoom * 0.001f;
                panX -= (float) Math.cos(yaw) * dx * speed;
                panZ += (float) Math.sin(yaw) * dx * speed;
                panY += dy * speed;
            }
            lastX = x; lastY = y;
        });

        glfwSetScrollCallback(window, (w, xOff, yOff) ->
            zoom = Math.clamp(zoom * (1f - (float)yOff * 0.15f), 1f, 1e9f));

        int program  = buildProgram();
        int vpLoc    = glGetUniformLocation(program, "viewProj");
        var meshes   = loadMeshes();
        var vpBuf    = BufferUtils.createFloatBuffer(16);

        glEnable(GL_DEPTH_TEST);
        glClearColor(0.05f, 0.05f, 0.1f, 1f);

        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            try (var stack = stackPush()) {
                var wb = stack.mallocInt(1); var hb = stack.mallocInt(1);
                glfwGetFramebufferSize(window, wb, hb);
                glViewport(0, 0, wb.get(0), hb.get(0));

                float aspect = (float) wb.get(0) / hb.get(0);
                float ex = panX + (float)(Math.sin(yaw) * Math.cos(pitch)) * zoom;
                float ey = panY + (float) Math.sin(pitch) * zoom;
                float ez = panZ + (float)(Math.cos(yaw) * Math.cos(pitch)) * zoom;

                var vp = new Matrix4f()
                    .perspective(0.8f, aspect, 1f, 1e10f)
                    .mul(new Matrix4f().lookAt(ex, ey, ez, panX, panY, panZ, 0, 1, 0));

                glUseProgram(program);
                glUniformMatrix4fv(vpLoc, false, vp.get(vpBuf));
            }

            for (var mesh : meshes) {
                glBindVertexArray(mesh.vao());
                glDrawArrays(GL_TRIANGLE_STRIP, 0, mesh.count());
            }

            glfwSwapBuffers(window);
            glfwPollEvents();
        }

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private List<Mesh> loadMeshes() {
        var files = landscapeDir.listFiles(f -> f.getName().endsWith(".nif"));
        if (files == null) return List.of();
        Arrays.sort(files);

        int cols   = (int) Math.ceil(Math.sqrt(files.length));
        var meshes = new ArrayList<Mesh>();

        for (int i = 0; i < files.length; i++) {
            float xOff = (i % cols) * 131_072f;
            float zOff = (i / cols) * 131_072f;
            var file = files[i];
            try {
                var verts = Nif.parse(file);
                if (verts.isEmpty()) continue;

                // NIF is Z-up; convert to Y-up (swap y and z)
                var buf = BufferUtils.createFloatBuffer(verts.size() * 3);
                for (var v : verts) buf.put(v.x() + xOff).put(v.z()).put(v.y() + zOff);
                buf.flip();

                int vao = glGenVertexArrays(), vbo = glGenBuffers();
                glBindVertexArray(vao);
                glBindBuffer(GL_ARRAY_BUFFER, vbo);
                glBufferData(GL_ARRAY_BUFFER, buf, GL_STATIC_DRAW);
                glVertexAttribPointer(0, 3, GL_FLOAT, false, 12, 0);
                glEnableVertexAttribArray(0);

                meshes.add(new Mesh(vao, verts.size()));
                System.out.printf("loaded %s: %d vertices%n", file.getName(), verts.size());
            } catch (Exception e) {
                System.out.println("skip " + file.getName() + ": " + e.getMessage());
            }
        }

        System.out.printf("%d meshes loaded%n", meshes.size());
        return meshes;
    }

    private int buildProgram() {
        int vert = compileShader(GL_VERTEX_SHADER, VERT);
        int frag = compileShader(GL_FRAGMENT_SHADER, FRAG);
        int prog = glCreateProgram();
        glAttachShader(prog, vert);
        glAttachShader(prog, frag);
        glLinkProgram(prog);
        glDeleteShader(vert);
        glDeleteShader(frag);
        return prog;
    }

    private int compileShader(int type, String src) {
        int shader = glCreateShader(type);
        glShaderSource(shader, src);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE)
            throw new RuntimeException("Shader error: " + glGetShaderInfoLog(shader));
        return shader;
    }
}
