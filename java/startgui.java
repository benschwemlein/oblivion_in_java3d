

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.StringTokenizer;

import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.ColorCube;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.geometry.Stripifier;
import com.sun.j3d.utils.behaviors.mouse.*;
import com.sun.j3d.utils.behaviors.mouse.MouseBehavior;
//import com.sun.j3d.utils.behaviors.mouse.MouseWheelZoom;
import com.sun.j3d.utils.behaviors.mouse.MouseZoom;
import com.sun.j3d.utils.compression.*;
//com.sun.j3d.utils.compression.CompressionStream





import com.sun.j3d.utils.universe.*;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Shape3D;
import javax.media.j3d.*;
import javax.vecmath.*;
import java.util.Hashtable;
import javax.media.j3d.Text3D;
import javax.media.j3d.Transform3D;
import com.sun.j3d.utils.geometry.Sphere;







public class startgui {

  	JFrame frame;
  	JLabel label; 	
	JPanel panel;
	Canvas3D canvas3D;
	BranchGroup objRoot;

  	public static void main(String[] args) {
   		startgui gui = new startgui();
		gui.go(); 
  	} 
  	
  	
  	public GeometryInfo createLandScapeMesh(String filename, long xOffset, long yOffset, long zOffset){
		//GeometryInfo gi = new GeometryInfo(GeometryInfo.TRIANGLE_ARRAY);
		GeometryInfo gi = new GeometryInfo(GeometryInfo.TRIANGLE_STRIP_ARRAY);
		
		int n = 0;
			
		Nif nl = new Nif(filename);
		
		NifSVertex[] vertices = nl.ReadVertexArray();
		int size = (int)nl.getNumberOfVetices(); 
		
		//size = 3;
		
		Point3f[] coordinates = new Point3f[size];
	
		
		float y;
		float z;
		float x;
		
		
		
		for(int i = 0; i < size; i++){
		//for(int i = size - 1; i >= 0; i--){
			
			x = vertices[i].getX() - (nl.getLargestVector()/2) + xOffset;
			y = vertices[i].getY() - (nl.getLargestVector()/2) + yOffset;
			z = vertices[i].getZ() -  (nl.getLargestVector()/1) + zOffset;
			
			coordinates[i] = new Point3f(x, y, z);
		}

		 System.out.println("size = " + size);
		
		int numStrips = 1;
		int[] stripCounts = new int[numStrips];
		for(int strip = 0; strip < numStrips; strip++){
			stripCounts[strip] = size;
			
		}
		
		System.out.println(" getLargestVector = " + nl.getLargestVector());
		
		gi.setStripCounts(stripCounts);
		gi.setCoordinates(coordinates);
		
		
		NormalGenerator ng = new NormalGenerator();
		ng.generateNormals(gi);

		Stripifier st = new Stripifier();
		st.stripify(gi);

		gi.indexify();
		gi.recomputeIndices();
		
		return gi;
			
	}
  	
	public Group createOblivionLandScape(){
		Group g = new Group();
		
		int xSize = 6;
		int ySize = 6;
		
		long offset = 131072;
		long xOffset = 0;
		long yOffset = 0;
		long zOffset = 0;
		
		String[][] file_name = new String[xSize][ySize];
		
		
		file_name[3][0] = "00.-96";
		file_name[3][1] = "00.-64";
		file_name[3][2] = "00.-32";
		file_name[3][3] = "00.00";
		file_name[3][4] = "00.32";
		file_name[3][5] = "00.64";
		
		file_name[1][0] = "-64.-96";
		file_name[1][1] = "-64.-64";
		file_name[1][2] = "-64.-32";
		file_name[1][3] = "-64.00";
		file_name[1][4] = "-64.32";
		file_name[1][5] = "-64.64";
		
		file_name[2][0] = "-32.-96";
		file_name[2][1] = "-32.-64";
		file_name[2][2] = "-32.-32";
		file_name[2][3] = "-32.00";
		file_name[2][4] = "-32.32";
		file_name[2][5] = "-32.64";
		
		file_name[0][0] = "-96.-96";
		file_name[0][1] = "-96.-64";
		file_name[0][2] = "-96.-32";
		file_name[0][3] = "-96.00";
		file_name[0][4] = "-96.32";
		file_name[0][5] = "-96.64";
	
			
		file_name[4][0] = "32.-96";
		file_name[4][1] = "32.-64";
		file_name[4][2] = "32.-32";
		file_name[4][3] = "32.00";
		file_name[4][4] = "32.32";
		file_name[4][5] = "32.64";
	
		
		file_name[5][0] = "64.-96";
		file_name[5][1] = "64.-64";
		file_name[5][2] = "64.-32";
		file_name[5][3] = "64.00";
		file_name[5][4] = "64.32";
		file_name[5][5] = "64.64";
				
	
				
	
		
		for(int i = 0; i < 6; i++){
			for(int j = 4; j < 6; j++){
				file_name[i][j] = "";
			}
		}
			
		/*for(int i = 0; i < 6; i++){
			file_name[4][i] = "";
		}
		
		for(int i = 0; i < 6; i++){
			file_name[3][i] = "";
		}*/
		
		String filename;
		int n = 0;
		
		for(int x = 0; x < xSize; x++){
			yOffset = 0;
			for(int y = 0; y < ySize; y++){
				if(file_name[x][y].length() > 0){
					filename = "landscape/60." + file_name[x][y] + ".32.nif";
					try{
											
						//gi[n] = createLandScapeMesh(filename, xOffset, yOffset, zOffset);
						GeometryInfo gi = createLandScapeMesh(filename, xOffset, yOffset, zOffset);
						GeometryArray ga = gi.getGeometryArray();
						Shape3D thisland = new Shape3D(ga, CreateAppearance());
						g.addChild(thisland);
						//n++;
				 	} catch(Exception e){
				 		System.out.println("error" +  filename);
				 	}
				}
			 	yOffset = yOffset +  offset;
			}
			xOffset =  xOffset + offset;
		}
		
		
		//GeometryCompressor gc = new GeometryCompressor();
		//CompressionStream cs = new CompressionStream(gi);
		//CompressedGeometry cdg = gc.compress(cs); 
		
		//Shape3D thisland = new Shape3D(cdg, CreateAppearance());
		
		
		//return thisland;
		return g;
		
	}	

	
	private int count_meshes = 1;
	private Appearance app = new Appearance();
	
	
	private  Group generateText(String text, long xOffset, long yOffset, long zOffset){
		Group g = new Group();
		
		TransformGroup objOffset = new TransformGroup(); 
		Transform3D tr3d	= new Transform3D();
		Vector3d v3d = new Vector3d(xOffset, yOffset, (zOffset + 1000)); 
		System.out.println("text  xOffset = " + xOffset + " yOffset = " + yOffset + " length = " + v3d.length());
		tr3d.set(v3d);
		objOffset.setTransform(tr3d);
		g.addChild(objOffset); 
		
				
		
		
		Font3D f3d = new Font3D(new Font(null, Font.PLAIN, 50000), new FontExtrusion()); 
		//Integer temp = new Integer(count_meshes);
		Text3D t3d = new Text3D(f3d, text);
		Point3f p = new Point3f(xOffset, yOffset, (zOffset + 3000));
		OrientedShape3D os3d = new OrientedShape3D(t3d, app, 0, p);
		
		objOffset.addChild(os3d);
		
		
		
		
		
		return g;
	}
	
	

	
	public BranchGroup createSceneGraph() {
		BranchGroup objRoot = new BranchGroup(); 
		  TransformGroup objScale = new TransformGroup(); 
		  Transform3D t3d = new Transform3D(); 
		  t3d.setScale(0.00002); 
		 // t3d.setScale(0.2); 
		  objScale.setTransform(t3d); 
		  objRoot.addChild(objScale); 
		  TransformGroup objTrans = new TransformGroup(); 
		  objTrans.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE); 
		  objTrans.setCapability(TransformGroup.ALLOW_TRANSFORM_READ); 
		  objScale.addChild(objTrans);
		
		
		 objTrans.addChild(createLight());
		 objTrans.addChild(createOblivionLandScape());
		
		
		  //BoundingSphere bounds = new BoundingSphere(new Point3d(0.0,0.0,0.0), 100.0); 
		BoundingSphere bounds = new BoundingSphere(new Point3d(), Double.MAX_VALUE);	  
			  
			  // Create the rotate behavior node 
			  MouseRotate behavior = new MouseRotate(); 
			  behavior.setTransformGroup(objTrans); 
			  objTrans.addChild(behavior); 
			  behavior.setSchedulingBounds(bounds); 
			  
			  // Create the zoom behavior node 
			  MouseZoom behavior2 = new MouseZoom(); 
			  behavior2.setFactor(1000);
			  behavior2.setTransformGroup(objTrans); 
			  objTrans.addChild(behavior2); 
			  behavior2.setSchedulingBounds(bounds); 
			  
			  // Create the translate behavior node 
			  MouseTranslate behavior3 = new MouseTranslate(); 
			  behavior3.setTransformGroup(objTrans); 
			  objTrans.addChild(behavior3); 
			  behavior3.setSchedulingBounds(bounds); 
		
		
        	// Have Java 3D perform optimizations on this scene graph.
       		objRoot.compile();

		return objRoot;
    }
	
	
	public String[] getDirectory(String directoryName){
		String[] dir = new java.io.File(directoryName).list();
		java.util.Arrays.sort(dir);
		
		//java.util.Arrays.sort(dir);
		for(int i = 0; i < dir.length; i++){
			
		}
		
		return dir;
	}
	
	
	public Appearance CreateAppearance(){
		Appearance appearance = new Appearance();
		//ColoringAttributes ca = new ColoringAttributes(1.0f, 1.0f, 1.0f, ColoringAttributes.SHADE_GOURAUD);
		ColoringAttributes ca = new ColoringAttributes(0.0f, 1.0f, 0.1f, ColoringAttributes.SHADE_FLAT);
		appearance.setColoringAttributes(ca);
		Material material = new Material();
		material.setLightingEnable(true);
		//material.setEmissiveColor(0.0f, 0.0f, 0.0f);
		//material.setDiffuseColor(0.30f, 0.1f, 0.0f);
		//material.setSpecularColor(0.75f, 0.3f, 0.0f);
		//material.setSpecularColor(0.75f, 0.3f, 0.0f);

		//ambient 
		//material.setShininess(0);
		appearance.setMaterial(material);
		
		return appearance;
	}


	
	public DirectionalLight createLight(){
		Vector3f lightDirection = new Vector3f(-0.1f, -0.3f, -0.3f);
		Color3f white = new Color3f(1f,1f,1f);

		DirectionalLight lightDirectional = new DirectionalLight(white, lightDirection);
		BoundingSphere infiniteBounds = new BoundingSphere(new Point3d(), Double.MAX_VALUE);
		lightDirectional.setInfluencingBounds(infiniteBounds);

		lightDirectional.setCapability(Light.ALLOW_STATE_WRITE);

		return lightDirectional;
	}

 	class RightListener implements ActionListener{
		public void actionPerformed(ActionEvent event){
			label.setText("Right Clicked");

		}
		
	}


	class LeftListener implements ActionListener{
		public void actionPerformed(ActionEvent event){
			label.setText("Left Clicked");

		}
		
	}


	class StopListener implements ActionListener{
		public void actionPerformed(ActionEvent event){
			label.setText("Stop Clicked");

		}
		
	}	

	public void go(){
		frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);



		JButton rightButton = new JButton("Rotate Right");
		rightButton.addActionListener(new RightListener());


		JButton leftButton = new JButton("Rotate Left");
		leftButton.addActionListener(new LeftListener());

		JButton stopButton = new JButton("Stop");
		stopButton.addActionListener(new StopListener());

		label = new JLabel("Label");

		panel = new JPanel();

		//Let's try the Java 3d stuff.

			GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
        		canvas3D = new Canvas3D(config);
        
       		 	BranchGroup scene = createSceneGraph();
      		 	       			
        		SimpleUniverse simpleU = new SimpleUniverse(canvas3D);
        		simpleU.getViewingPlatform().setNominalViewingTransform();
        
        		simpleU.addBranchGraph(scene);
                	
		//Done

		//frame.getContentPane().add(BorderLayout.EAST, rightButton);
		//frame.getContentPane().add(BorderLayout.WEST, leftButton);
		//frame.getContentPane().add(BorderLayout.SOUTH, stopButton);
		//frame.getContentPane().add(BorderLayout.NORTH, label);
		frame.getContentPane().add(BorderLayout.CENTER, canvas3D);

		frame.setSize(1000, 800);
		frame.setVisible(true);


	

	}		
					


}




