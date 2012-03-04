import java.io.IOException;
import java.io.BufferedReader;
import java.io.File;
import java.io.*;
import java.util.*;

public class NifInputStream extends BufferedInputStream{
		LinkedList test_buffer;
		boolean debug_mode;
		DataInputStream dis;
		long byte_count;
		long mark;
		boolean end_of_file;
		boolean little_endian;
				
		
		
		NifInputStream(InputStream is, boolean littleendian){
			super(is);
			debug_mode = false;
			test_buffer = new LinkedList();
			byte_count = 0;
			mark = 0;
			end_of_file = false;
			little_endian = littleendian;
			
			
		}
		
		NifInputStream(InputStream is, boolean littleendian, boolean debug){
			super(is);
			byte_count = 0;
			mark = 0;
			debug_mode = debug;
			end_of_file = false;
			little_endian = littleendian;
			
			try {
			if(debug){
				test_buffer = new LinkedList();
				int tempvalue = 0;
				
				while( tempvalue != -1){
					tempvalue = super.read();
					test_buffer.add(new Integer(tempvalue));
				
				}
			}
			
			} catch (IOException i){
				System.out.print(i.getMessage());
				i.printStackTrace();
				debug_mode = false;
			
			}
			
			
					
		}
		
		public NifSVertex readFVertex() throws IOException{
			float x = this.readFloat();
			float y = this.readFloat();
			float z = this.readFloat();
			return new NifSVertex(x, y, z);
		}
		
		public String readString() throws IOException{
			StringBuffer sb = new StringBuffer();
			int value = 0; 
			
			while((value = this.read()) > 32){
					sb.append((char)value);
			}
			
			return sb.toString();
				  
		}
		
		public float readFloat() throws IOException{
			int bytes[] = new int[4];
			int temp;
			
			for(int i = 0; i < 4; i++){
				temp = this.read();
				bytes[i] = temp;
				//	System.out.println("byte[" + i + "] = " + temp + " pos = " + super.pos + " " +  bytes[0] + " " + (byte)196);
			}
			
			
			
			
			return intArraytoFloat(bytes);
			
		}
		
		public float readFloat(int debug) throws IOException{
			int bytes[] = new int[4];
			int temp;
			
			for(int i = 0; i < 4; i++){
				temp = this.read();
				bytes[i] = temp;
					System.out.println("byte[" + i + "] = " + temp + " pos = " + this.getByteCount() + " " +  bytes[0] + " " + (byte)196);
			}
			
			
			 for(int i = 0; i < 4; i++){
            	 System.out.println("bytes = " + bytes[i]);
             }
			
			return intArraytoFloat(bytes);
			
		}
		
		public int readInt() throws  IOException{
			int bytes[] = new int[4];
			bytes = this.readByteArray(4);
						
			int tempInt = ((bytes[0]<<24&0xFF000000) | 
							((bytes[1]<<16)&0xFF0000) |
							((bytes[2]<<8)&0xFF00) |
							(bytes[3]&0xFF));
			return tempInt;
		}
		
				
		public int readShort() throws  IOException{
			int bytes[] = new int[2];
			bytes = this.readByteArray(2);
			
												
			int tempShort = (((bytes[0]<<8)&0xFF00) | (bytes[1]&0xFF));
			return tempShort;
		}
		
						
		public byte readByte() throws  IOException{
			return (byte)this.read();
		}
		
		
		
		public void reset() throws IOException{
			byte_count = mark;
			super.reset();
		}
		
		public void mark(){
			this.mark(2);
		}
		
		public void mark(int readlimit){
			mark = byte_count;
			super.mark(readlimit);
		}
		
		
		
		public long skip(long n) throws IOException{
			this.incrementByteCount(n);
			return super.skip(n);
			
		}
		
		public long skipTo(long n) throws IOException{
			long count = 0;
			if(n >= this.getByteCount()){
				int value;
				while((value = this.read()) != -1 && this.getByteCount() < n){
					count++;
				}
				
			}	
			//System.out.println("skipTo = "  + this.getByteCount());
			return count;
		}
		
		public long skipZeroValueBytes() throws IOException{
			byte value = 0;
			long count = 0;
			//if(!this.isEndOfFile()){
				while((value = (byte)this.read()) == 0 ){
					count++;
					//System.out.println(value);
					this.mark(1);
				};
				this.reset();
				//System.out.println(this.getByteCount());
			//}
			return count;
			
		}
		
			
		public long skipToInt(int value) throws IOException {
			
			int tempInt = 0;
			
			int bytes[] = this.readByteArray(4);
		               
            int count = 0;
            
          
                                
            while (tempInt != value && bytes[3] != -1){
            	
            	 this.mark();
            	
            	tempInt = ((bytes[3]<<24&0xFF000000) | 
						((bytes[2]<<16)&0xFF0000) |
						((bytes[1]<<8)&0xFF00) |
						(bytes[0]&0xFF));
            	
            	bytes[0] = bytes[1];
            	bytes[1] = bytes[2];
            	bytes[2] = bytes[3];
            	bytes[3] = this.read();
            	          	
            	
            	//count++;
            	
            }
            
            this.reset();
            
           return this.getByteCount();
		}
		
		public long skipToRef() throws IOException {
			//Apparently NIF uses 4 bytes 255 255 255 255 as a reference.   This equals -1 as an int.	
			return (this.skipToInt(-1));
		}
		
		public long skipToRef(int n) throws IOException {
			//Apparently NIF uses 4 bytes 255 255 255 255 as a reference.   This equals -1 as an int.	
			
			for(int i = 0; i < n - 1; i++){
				System.out.println(this.skipToRef());
			}
			return (this.skipToRef());
		}
		
			
		
		
		public void printBytes(int n) throws IOException{
			this.printBytes(this.getByteCount(), n);
		}
		
		public void printBytes(long start, long len) throws IOException{
			long end  = start + len;
			
			
			
			if(start < this.getByteCount()){
				throw new IOException("Start can't be less than current position.");
			} 
			
			this.mark((int)(end - start));
			
			this.skipTo(start - 1);
			
			long count_temp = start;
			int value;
			
			String flag = "<<<<<";
			
			while((value = this.read()) != -1 && count_temp <= end){
				if(count_temp == this.getByteCount()){
					flag = "<<<<<";
				} else {
					flag = "";
				}
				
				System.out.println((char)value + "  " + (int)value + "  " + (count_temp + 1) + " " + flag);
				count_temp++;
			}
			this.reset();
			
			//super.skip(byte_count);
					
		}
		
		public boolean isLittleEndian(){
			return little_endian;
		}
		
		public long getByteCount(){
			return byte_count;
		}
		
		private boolean incrementByteCount(){
			if(!this.isEndOfFile()){
				byte_count++;
			}	
			return  !this.isEndOfFile();
			
		}
		
		private long incrementByteCount(long n){
			long i;
			for(i = 0; i < n; i++ ){
				if(!incrementByteCount()){
					break;
				}
			}
			
			return i;
		}
		
		// Thanks to http://www.artima.com/forums/flat.jsp?forum=1&thread=145058 for this function.
		private float intArraytoFloat( int[] aBytes){
			int tempbits; 
			if(this.isLittleEndian()){
				tempbits = ((0xff & aBytes[0]) | ((0xff & aBytes[1]) << 8 ) | ((0xff & aBytes[2]) << 16)|((0xff & aBytes[3]) <<24));
			} else {
				tempbits = ((0xff & aBytes[3]) | ((0xff & aBytes[2]) << 8 ) | ((0xff & aBytes[1]) << 16)|((0xff & aBytes[0]) <<24));
			}
			
		    
			float ff = Float.intBitsToFloat(tempbits);
		 
			return ff;
		}
		
		public boolean isEndOfFile(){
			return end_of_file;
		}
		
		
		
		//This is the core byte reading function.
		public int read() throws IOException{
			
			int temp;
			
			if(!debug_mode){
				temp = super.read();
							
			} else {
				Integer tempInt = (Integer)test_buffer.removeFirst();
				temp = tempInt.intValue();
			}
			
			if(temp == -1){
				end_of_file = true;
			}			
			
			this.incrementByteCount();
			
			return temp;
		}
		
//		This is a core byte array reading method.
		public int[] readByteArray(int size) throws IOException{
			int[] temp = new int[size];
			
			if(this.isLittleEndian()){
				for(int i = (size - 1); i >= 0; i--){
					temp[i] = this.read();
				}
			} else {
				for(int i = 0; i < size; i++){
					temp[i] = this.read();
				}
			}
			
			return temp;
		}
		
		public int read(byte[] b, int off, int len) throws IOException{
			byte_count = byte_count + len;
			return super.read(b, off,len);
		}
		
		public void close() throws IOException{
			super.close();
		}
		
		
		/*public void printShortAsBytes(int s){
				byte[] b = shortToByteArray(s); 
				
				System.out.println("short = " + s + " byte[0] = " + b[0] + " byte[1] = " + b[1]);
			
		}
		
		//The following two functions are from the following...
		//  Copyright 1999-2004 The Apache Software Foundation.
		//package org.apache.fop.render.afp.tools;
		public static void shortToByteArray(
				int value,
				byte[] array,
				int offset) {
				array[offset + 1] = (byte) (value >>> 8);
				array[offset] = (byte) value;
	    }
		
		public static byte[] shortToByteArray(int value) {
			byte[] serverValue = new byte[2];
			shortToByteArray(value, serverValue, 0);
			return serverValue;
		}*/
		

		
}
