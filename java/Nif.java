import java.io.BufferedReader;
import java.io.File;
import java.io.*;
import java.util.*;

public class Nif{
	
    FileInputStream fis ;
    //DataInputStream dis;
    String file_name;
    FileReader fr;
    BufferedReader br;
	NifInputStream nis;
	DataInputStream dis;
	
	float largest_vector;
	
	int number_of_vertices;
   
	
	Nif(String fn){
		file_name = fn;
		number_of_vertices = 0;
		int largest_vector = 0;
		
		try { 
			
				//--Open File--//
				
			//	fis = new FileInputStream(fileName);
				//DataInputStream dis = new DataInputStream(fis);
				fr = new FileReader(file_name);
				br = new BufferedReader(fr);
				
				fis = new FileInputStream(file_name);
				nis = new NifInputStream(fis, true);
				
				
			
				
				
			
				
		} catch (FileNotFoundException nf){
					System.out.println("Data file was NOT read.");
					System.out.print(nf.getMessage());
					nf.printStackTrace();} 
		
	}  
	
	
public float getLargestVector(){
	return largest_vector;
}
	

//	Reads the given file by file name and returns a  ___________ of block references
public	NifSVertex[] ReadVertexArray() {
		//ArrayList vertices = new ArrayList();
		NifSVertex[] vertices;
		NifSVertex[] vertices_output;
		short[] points;
		
		
		try{	
				//--Read Header--//
			 
			// nis.printBytes(170);
			
			 nis.skip(48);
			  
			 int number_of_blocks = nis.readInt();
			 		   
			 nis.skip(10);
			   	  
			 int number_of_blocks_types = nis.readShort();
			  	
			  		 
			 System.out.println("number_of_blocks = " + number_of_blocks + "\n");
			 System.out.println("number_of_blocks_types = " + number_of_blocks_types + "\n");
			  
			  		
			  nis.readInt();
			 
			  
			 ArrayList array_block_names = new ArrayList();
			  for(int i = 0; i < number_of_blocks_types; i++){
				 
				  array_block_names.add(nis.readString().trim());
				   nis.skipZeroValueBytes();
				   System.out.println(array_block_names.get(i));
				
			  }
			  
			 			  
			  for(int i = 0; i < number_of_blocks; i++){
				  nis.readShort();
			  }
			  
			  //Skip data we don't need by skipping 3 references.
			  nis.skipToRef(3);
			  nis.skipZeroValueBytes();
			  //nis.printBytes(20);
			  
			  			 
			  //Number of Vetices
			  number_of_vertices = nis.readShort();
						 
			 // int number_of_vertices = nis.readShortDIS();
			  System.out.println("number_of_vertices = " + number_of_vertices);
			  //Unknown short.
			  nis.readShort();
			  //Has Vertices
			  nis.readByte(); //boolean has_vertices = (boolean)nis.readByte();
			 
			  NifSVertex vertex1;
			  
			  //FileWriter fw = new FileWriter("output.txt");
			  
			  //Build Vertices
			  vertices = new NifSVertex[number_of_vertices];
			  
			  for(int i = 0; i < number_of_vertices; i++){
				  vertex1 = nis.readFVertex();
				  vertices[i] = vertex1;
				  
				  this.increaseLargestVector(vertex1);
					 
				  //fw.write(vertex1.getString());;
					
					
				  
			  }
			  
			
			  
			 
			
			 System.out.println("Skip to Ref = " + nis.skipToRef()); 
			  //Get Number of Triangles.
			  int number_of_triangles = nis.readShort();
			  int number_of_strips = nis.readShort();
			  
			  
			  int strip_size; 
			// for(int i = 0; i < number_of_strips; i++){
				  strip_size = nis.readShort();
		// }
			  
			  System.out.println("number_of_triangles = " + number_of_triangles);
			  System.out.println("number_of_strips = " + number_of_strips);
			  System.out.println("stripe_size = " + strip_size);
			  
			  //Unneeded byte.
			  nis.read();
			 
				 
			
			 System.out.println(nis.getByteCount());
		
			 
			 //Build Points
			// FileWriter fw2 = new FileWriter("output2.txt");
			 
			 int number_of_points = strip_size;
			 number_of_vertices = number_of_points;
			 
			 //System.out.println(number_of_points);
			  points = new short[number_of_points];
			  short point;
			  
			
			  for(int i = 0; i < number_of_points; i++){
				  point = (short)nis.readShort();
				  points[i] = point;
				  
			  }
			
			 vertices_output = new NifSVertex[number_of_points];
			 for(int i = 0; i < number_of_points; i++){
				 vertices_output[i] = vertices[points[i]];
				 // fw2.write(vertices[points[i]].getString() + "\n");
							  
			  }
			 
			// fw2.close();
			  
			  //System.out.println(nis.skipToShort((short)35668));
			 
			
			    
		} catch (IOException i){
			System.out.print(i.getMessage());
			i.printStackTrace();
			vertices = new NifSVertex[0];
			vertices_output = new NifSVertex[0];
		}
		
		//Convert Array of vertices in StripArray
		 
		
		
				
		return vertices_output;	
		
	}

	public long getNumberOfVetices(){
		return number_of_vertices ;
	}
	
	private void increaseLargestVector(NifSVertex v){
		if(v.getX() > largest_vector){
			largest_vector = v.getX();
		}
		
		if(v.getY() > largest_vector){
			largest_vector = v.getY();
		}
		
		if(v.getZ() > largest_vector){
			largest_vector = v.getZ();
		}
		
	}


}
