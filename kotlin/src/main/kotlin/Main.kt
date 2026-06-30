import com.sun.j3d.utils.behaviors.mouse.MouseRotate
import com.sun.j3d.utils.behaviors.mouse.MouseTranslate
import com.sun.j3d.utils.behaviors.mouse.MouseZoom
import com.sun.j3d.utils.geometry.NormalGenerator
import com.sun.j3d.utils.geometry.Stripifier
import com.sun.j3d.utils.geometry.GeometryInfo
import com.sun.j3d.utils.universe.SimpleUniverse
import java.awt.BorderLayout
import java.io.File
import javax.media.j3d.*
import javax.swing.JFrame
import javax.vecmath.*
import kotlin.math.sqrt

fun main() {
    val landscapeDir = File("../landscape")
    LandscapeViewer(landscapeDir).show()
}

class LandscapeViewer(private val landscapeDir: File) {

    fun show() {
        val canvas = Canvas3D(SimpleUniverse.getPreferredConfiguration())

        SimpleUniverse(canvas).apply {
            viewingPlatform.setNominalViewingTransform()
            addBranchGraph(buildScene())
        }

        JFrame("Oblivion Landscape — Kotlin/Java3D").apply {
            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            contentPane.add(canvas, BorderLayout.CENTER)
            setSize(1000, 800)
            isVisible = true
        }
    }

    private fun buildScene(): BranchGroup {
        val bounds = BoundingSphere(Point3d(), Double.MAX_VALUE)

        val orbitGroup = TransformGroup().apply {
            setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE)
            setCapability(TransformGroup.ALLOW_TRANSFORM_READ)
            addChild(directionalLight(bounds))
            addChild(landscape())
            addChild(MouseRotate().also    { it.transformGroup = this; it.schedulingBounds = bounds })
            addChild(MouseZoom().also      { it.setFactor(1000.0); it.transformGroup = this; it.schedulingBounds = bounds })
            addChild(MouseTranslate().also { it.transformGroup = this; it.schedulingBounds = bounds })
        }

        val scaleGroup = TransformGroup(Transform3D().apply { setScale(0.00002) }).apply {
            addChild(orbitGroup)
        }

        return BranchGroup().apply {
            addChild(scaleGroup)
            compile()
        }
    }

    private fun landscape(): Group {
        val nifFiles = landscapeDir.listFiles { f -> f.extension == "nif" }
            ?.sorted() ?: return Group()

        val cols = sqrt(nifFiles.size.toDouble()).toInt().coerceAtLeast(1)

        return Group().apply {
            nifFiles.forEachIndexed { i, file ->
                val xOffset = (i % cols) * 131_072f
                val yOffset = (i / cols) * 131_072f
                runCatching { addChild(meshFor(file, xOffset, yOffset)) }
                    .onFailure { println("skip ${file.name}: ${it.message}") }
            }
        }
    }

    private fun meshFor(file: File, xOffset: Float, yOffset: Float): Shape3D {
        val vertices = parseNif(file)
        val largest  = vertices.flatMap { listOf(it.x, it.y, it.z) }.maxOrNull() ?: 0f

        val gi = GeometryInfo(GeometryInfo.TRIANGLE_STRIP_ARRAY).apply {
            coordinates = vertices.map { v ->
                Point3f(v.x - largest / 2 + xOffset, v.y - largest / 2 + yOffset, v.z - largest)
            }.toTypedArray()
            stripCounts = intArrayOf(vertices.size)
        }

        NormalGenerator().generateNormals(gi)
        Stripifier().stripify(gi)
        gi.indexify()
        gi.recomputeIndices()

        return Shape3D(gi.geometryArray, appearance())
    }

    private fun appearance() = Appearance().apply {
        coloringAttributes = ColoringAttributes(0f, 1f, 0.1f, ColoringAttributes.SHADE_FLAT)
        material = Material().apply { setLightingEnable(true) }
    }

    private fun directionalLight(bounds: BoundingSphere) = DirectionalLight(
        Color3f(1f, 1f, 1f),
        Vector3f(-0.1f, -0.3f, -0.3f)
    ).apply {
        influencingBounds = bounds
        setCapability(Light.ALLOW_STATE_WRITE)
    }
}
