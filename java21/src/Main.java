import com.sun.j3d.utils.behaviors.mouse.*;
import com.sun.j3d.utils.geometry.*;
import com.sun.j3d.utils.universe.SimpleUniverse;
import java.awt.BorderLayout;
import java.io.File;
import java.util.Arrays;
import java.util.stream.DoubleStream;
import javax.media.j3d.*;
import javax.swing.JFrame;
import javax.vecmath.*;

public class Main {
    public static void main(String[] args) {
        new LandscapeViewer(new File("../landscape")).show();
    }
}

class LandscapeViewer {

    private final File landscapeDir;

    LandscapeViewer(File landscapeDir) {
        this.landscapeDir = landscapeDir;
    }

    void show() {
        var canvas   = new Canvas3D(SimpleUniverse.getPreferredConfiguration());
        var universe = new SimpleUniverse(canvas);
        universe.getViewingPlatform().setNominalViewingTransform();
        universe.addBranchGraph(buildScene());

        var frame = new JFrame("Oblivion Landscape — Java 21/Java3D");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(canvas, BorderLayout.CENTER);
        frame.setSize(1000, 800);
        frame.setVisible(true);
    }

    private BranchGroup buildScene() {
        var bounds      = new BoundingSphere(new Point3d(), Double.MAX_VALUE);
        var orbitGroup  = orbitGroup(bounds);

        var t = new Transform3D();
        t.setScale(0.00002);
        var scaleGroup = new TransformGroup(t);
        scaleGroup.addChild(orbitGroup);

        var root = new BranchGroup();
        root.addChild(scaleGroup);
        root.compile();
        return root;
    }

    private TransformGroup orbitGroup(BoundingSphere bounds) {
        var group = new TransformGroup();
        group.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        group.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);

        group.addChild(directionalLight(bounds));
        group.addChild(landscape());

        for (var behavior : new MouseBehavior[]{ new MouseRotate(), new MouseZoom(), new MouseTranslate() }) {
            if (behavior instanceof MouseZoom z) z.setFactor(1000); // pattern matching
            behavior.setTransformGroup(group);
            behavior.setSchedulingBounds(bounds);
            group.addChild(behavior);
        }

        return group;
    }

    private Group landscape() {
        var group = new Group();
        var files = landscapeDir.listFiles(f -> f.getName().endsWith(".nif"));
        if (files == null) return group;

        Arrays.sort(files);
        int cols = (int) Math.ceil(Math.sqrt(files.length));

        for (int i = 0; i < files.length; i++) {
            float xOffset = (i % cols) * 131_072f;
            float yOffset = (i / cols) * 131_072f;
            var file = files[i];
            try {
                group.addChild(meshFor(file, xOffset, yOffset));
            } catch (Exception e) {
                System.out.println("skip " + file.getName() + ": " + e.getMessage());
            }
        }

        return group;
    }

    private Shape3D meshFor(File file, float xOffset, float yOffset) throws Exception {
        var vertices = Nif.parse(file);

        float largest = (float) vertices.stream()
            .flatMapToDouble(v -> DoubleStream.of(v.x(), v.y(), v.z()))
            .max()
            .orElse(0);

        var coords = vertices.stream()
            .map(v -> new Point3f(v.x() - largest / 2 + xOffset,
                                  v.y() - largest / 2 + yOffset,
                                  v.z() - largest))
            .toArray(Point3f[]::new);

        var gi = new GeometryInfo(GeometryInfo.TRIANGLE_STRIP_ARRAY);
        gi.setCoordinates(coords);
        gi.setStripCounts(new int[]{ vertices.size() });

        new NormalGenerator().generateNormals(gi);
        new Stripifier().stripify(gi);
        gi.indexify();
        gi.recomputeIndices();

        return new Shape3D(gi.getGeometryArray(), appearance());
    }

    private Appearance appearance() {
        var app = new Appearance();
        app.setColoringAttributes(new ColoringAttributes(0f, 1f, 0.1f, ColoringAttributes.SHADE_FLAT));
        var mat = new Material();
        mat.setLightingEnable(true);
        app.setMaterial(mat);
        return app;
    }

    private DirectionalLight directionalLight(BoundingSphere bounds) {
        var light = new DirectionalLight(new Color3f(1f, 1f, 1f), new Vector3f(-0.1f, -0.3f, -0.3f));
        light.setInfluencingBounds(bounds);
        light.setCapability(Light.ALLOW_STATE_WRITE);
        return light;
    }
}
