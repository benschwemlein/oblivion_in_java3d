
public class NifSVertex {
	
	private float x, y, z;
	
	NifSVertex(float X, float Y, float Z){
		x = X;
		y = Y;
		z = Z;
		
	};
	
	public float getX(){
		return x;
	}
	
	public float getY(){
		return y;
	}
	
	public float getZ(){
		return z;
	}
	
	public void print(){
		System.out.println("NifSVertex: x=" + this.getX() + ", y = " + this.getY() + ", z = " + this.getZ());
	}
	
	public String getString(){
		return this.getX() + "~" +  this.getY() + "~" + this.getZ() + "\n";
	}
	


}
