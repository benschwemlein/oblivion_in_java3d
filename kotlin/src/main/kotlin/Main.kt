import org.joml.Matrix4f
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.NULL
import java.io.File
import kotlin.math.*

fun main() {
    LandscapeViewer(File("../landscape")).run()
}

private val VERT = """
    #version 330 core
    layout(location = 0) in vec3 pos;
    uniform mat4 viewProj;
    out vec3 worldPos;
    void main() {
        gl_Position = viewProj * vec4(pos, 1.0);
        worldPos = pos;
    }
""".trimIndent()

private val FRAG = """
    #version 330 core
    in vec3 worldPos;
    out vec4 fragColor;
    void main() {
        vec3 n = normalize(cross(dFdx(worldPos), dFdy(worldPos)));
        float d = clamp(dot(n, normalize(vec3(0.1, 0.3, 0.3))), 0.15, 1.0);
        fragColor = vec4(0.0, d, 0.1 * d, 1.0);
    }
""".trimIndent()

class LandscapeViewer(private val landscapeDir: File) {

    private data class Mesh(val vao: Int, val count: Int)

    private var yaw = 0f;  private var pitch = 0.4f; private var zoom = 500_000f
    private var panX = 0f; private var panY = 0f;    private var panZ = 0f
    private var lastX = 0.0; private var lastY = 0.0
    private var mouseLeft = false; private var mouseRight = false

    fun run() {
        GLFWErrorCallback.createPrint(System.err).set()
        glfwInit()
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE)

        val window = glfwCreateWindow(1200, 800, "Oblivion Landscape — Kotlin/LWJGL", NULL, NULL)
        glfwMakeContextCurrent(window)
        glfwSwapInterval(1)
        glfwShowWindow(window)
        GL.createCapabilities()

        glfwSetMouseButtonCallback(window) { w, button, action, _ ->
            if (button == GLFW_MOUSE_BUTTON_LEFT)  mouseLeft  = action == GLFW_PRESS
            if (button == GLFW_MOUSE_BUTTON_RIGHT) mouseRight = action == GLFW_PRESS
            if (action == GLFW_PRESS) {
                MemoryStack.stackPush().use { stack ->
                    val xb = stack.mallocDouble(1); val yb = stack.mallocDouble(1)
                    glfwGetCursorPos(w, xb, yb)
                    lastX = xb.get(0); lastY = yb.get(0)
                }
            }
        }

        glfwSetCursorPosCallback(window) { _, x, y ->
            val dx = (x - lastX).toFloat()
            val dy = (y - lastY).toFloat()
            when {
                mouseLeft  -> {
                    yaw   -= dx * 0.005f
                    pitch  = (pitch + dy * 0.005f).coerceIn(-1.4f, 1.4f)
                }
                mouseRight -> {
                    val speed = zoom * 0.001f
                    panX -= cos(yaw) * dx * speed
                    panZ += sin(yaw) * dx * speed
                    panY += dy * speed
                }
            }
            lastX = x; lastY = y
        }

        glfwSetScrollCallback(window) { _, _, dy ->
            zoom = (zoom * (1f - dy.toFloat() * 0.15f)).coerceIn(1f, 1e9f)
        }

        val program = buildProgram()
        val vpLoc   = glGetUniformLocation(program, "viewProj")
        val meshes  = loadMeshes()
        val vpBuf   = BufferUtils.createFloatBuffer(16)

        glEnable(GL_DEPTH_TEST)
        glClearColor(0.05f, 0.05f, 0.1f, 1f)

        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            MemoryStack.stackPush().use { stack ->
                val wb = stack.mallocInt(1); val hb = stack.mallocInt(1)
                glfwGetFramebufferSize(window, wb, hb)
                glViewport(0, 0, wb.get(0), hb.get(0))

                val aspect = wb.get(0).toFloat() / hb.get(0)
                val ex = panX + sin(yaw) * cos(pitch) * zoom
                val ey = panY + sin(pitch) * zoom
                val ez = panZ + cos(yaw) * cos(pitch) * zoom

                val vp = Matrix4f()
                    .perspective(0.8f, aspect, 1f, 1e10f)
                    .mul(Matrix4f().lookAt(ex, ey, ez, panX, panY, panZ, 0f, 1f, 0f))

                glUseProgram(program)
                glUniformMatrix4fv(vpLoc, false, vp.get(vpBuf))
            }

            meshes.forEach { (vao, count) ->
                glBindVertexArray(vao)
                glDrawArrays(GL_TRIANGLE_STRIP, 0, count)
            }

            glfwSwapBuffers(window)
            glfwPollEvents()
        }

        glfwFreeCallbacks(window)
        glfwDestroyWindow(window)
        glfwTerminate()
    }

    private fun loadMeshes(): List<Mesh> {
        val files = landscapeDir.listFiles { f -> f.extension == "nif" }?.sorted() ?: return emptyList()
        val cols  = ceil(sqrt(files.size.toDouble())).toInt()

        return files.mapIndexedNotNull { i, file ->
            val xOff = (i % cols) * 131_072f
            val zOff = (i / cols) * 131_072f
            runCatching {
                val verts = parseNif(file)
                if (verts.isEmpty()) return@runCatching null

                // NIF is Z-up; convert to Y-up (swap y and z)
                val buf = BufferUtils.createFloatBuffer(verts.size * 3).also { buf ->
                    verts.forEach { v -> buf.put(v.x + xOff).put(v.z).put(v.y + zOff) }
                    buf.flip()
                }

                val vao = glGenVertexArrays(); val vbo = glGenBuffers()
                glBindVertexArray(vao)
                glBindBuffer(GL_ARRAY_BUFFER, vbo)
                glBufferData(GL_ARRAY_BUFFER, buf, GL_STATIC_DRAW)
                glVertexAttribPointer(0, 3, GL_FLOAT, false, 12, 0)
                glEnableVertexAttribArray(0)

                println("loaded ${file.name}: ${verts.size} vertices")
                Mesh(vao, verts.size)
            }.onFailure { println("skip ${file.name}: ${it.message}") }.getOrNull()
        }
    }

    private fun buildProgram() = glCreateProgram().also { prog ->
        val shaders = listOf(GL_VERTEX_SHADER to VERT, GL_FRAGMENT_SHADER to FRAG)
            .map { (type, src) -> compileShader(type, src) }
        shaders.forEach { glAttachShader(prog, it) }
        glLinkProgram(prog)
        shaders.forEach { glDeleteShader(it) }
    }

    private fun compileShader(type: Int, src: String) = glCreateShader(type).also { shader ->
        glShaderSource(shader, src)
        glCompileShader(shader)
        check(glGetShaderi(shader, GL_COMPILE_STATUS) != GL_FALSE) {
            "Shader error: ${glGetShaderInfoLog(shader)}"
        }
    }
}
