package MARS;

// Static I/O features for this application
// global encoding and size issue addressed

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class SIO{

	/*** INTEGERS ***/
	//read to int a various number of bits
	final static int read32(ByteBuffer buffer){
		return (buffer.get()&0xFF)<<24
				|(buffer.get()&0xFF)<<16
				|(buffer.get()&0xFF)<<8
				|(buffer.get()&0xFF);
	}
	final static int read24(ByteBuffer buffer){
		return (buffer.get()&0xFF)<<16
				|(buffer.get()&0xFF)<<8
				|(buffer.get()&0xFF);
	}
	final static int read16(ByteBuffer buffer){
		return (buffer.get()&0xFF)<<8
				|(buffer.get()&0xFF);
	}
	final static int read8(ByteBuffer buffer){
		return (buffer.get()&0xFF);
	}
	//write int to various number of bits
	final static void write32(ByteBuffer buffer, int i){
		buffer.put((byte)(i>>24&0xFF))
				.put((byte)(i>>16&0xFF))
				.put((byte)(i>>8 &0xFF))
				.put((byte)(i    &0xFF));
	}
	final static void write24(ByteBuffer buffer, int i){
		buffer.put((byte)(i>>16&0xFF))
				.put((byte)(i>>8 &0xFF))
				.put((byte)(i    &0xFF));
	}
	final static void write16(ByteBuffer buffer, int i){
		buffer.put((byte)(i>>8 &0xFF))
				.put((byte)(i    &0xFF));
	}
	final static void write8(ByteBuffer buffer, int i){
		buffer.put((byte)(i    &0xFF));
	}



	/*** ARRAYS ***/
	//Hashes fixed length byte[] and can be write/read differnetly that general byte[]
	final static byte[] readHash(ByteBuffer buffer){
		byte[] hash = new byte[16];
		buffer.get(hash);
		return hash;
	}
	final static void writeHash(ByteBuffer buffer, byte[] hash){
		buffer.put(hash);
	}

	//Variable length byte array need to read in the same length as they write out
	final static byte[] readArray(ByteBuffer buffer){
		byte[] array = new byte[read16(buffer)];
		buffer.get(array);
		return array;
	}
	final static void writeArray(ByteBuffer buffer, byte[] array){
		write16(buffer, array.length);
		buffer.put(array);
	}



	/*** STRINGS ***/
	//controls the encoding of Strings.
	final static Charset ENCODING = StandardCharsets.UTF_8;

	//Strings are stored as variable length arrays of their encoding
	public static String readString(ByteBuffer buffer){
		return new String(readArray(buffer),ENCODING);
	}
	public static void writeString(ByteBuffer buffer, String str){
		writeArray(buffer, str.getBytes(ENCODING));
	}



	/*** TESTING ***/
	//No libraries are imported above for this code making it easy to remove
	public static void main(String[] args){
		String file_name = args[0];
		int seed = Integer.parseInt(args[1]);
		int n=10;

		//allocate space for data and test input
		int[] data32 = new int[n];
		int[] data24 = new int[n];
		int[] data16 = new int[n];
		int[] data8 = new int[n];
		byte[][] dataHash = new byte[n][16];
		byte[][] dataArray = new byte[n][];
		String[] dataString = new String[n];

		int[] test32 = new int[n];
		int[] test24 = new int[n];
		int[] test16 = new int[n];
		int[] test8 = new int[n];
		byte[][] testHash = new byte[n][];
		byte[][] testArray = new byte[n][];
		String[] testString = new String[n];

		//randomize and write data
		java.util.Random r = new java.util.Random(seed);
		java.io.RandomAccessFile file = null;
		java.nio.channels.FileChannel f = null;
		try{
			file = new java.io.RandomAccessFile(file_name, "rw");
			f = file.getChannel();
		}catch(Exception e){
			System.out.println(e);
			System.exit(1);
		}
		ByteBuffer buffer = ByteBuffer.allocate(200);
		for(int i=0; i<n; i++){
			write32(buffer, data32[i] = r.nextInt());
			write24(buffer, data24[i] = r.nextInt()&0xFFFFFF);
			write16(buffer, data16[i] = r.nextInt()&0xFFFF);
			write8(buffer, data8[i] = r.nextInt()&0xFF);

			r.nextBytes(dataHash[i]);
			writeHash(buffer, dataHash[i]);
			dataArray[i] = new byte[r.nextInt(20)];
			r.nextBytes(dataArray[i]);
			writeArray(buffer, dataArray[i]);

			byte[] temp = new byte[r.nextInt(20)];
			r.nextBytes(temp);
			dataString[i] = new String(temp,StandardCharsets.UTF_8);
			writeString(buffer, dataString[i]);

			buffer.flip();
			try{f.write(buffer);}
			catch(Exception e){System.out.println(e);}
			buffer.clear();
		}
		try{ file.close();}
		catch(Exception e){
			System.out.println(e);
		}

		try{Thread.sleep(1000);}
		catch(Exception e){}

		//read in
		try{
			file = new java.io.RandomAccessFile(file_name, "r");
			f = file.getChannel();
		}catch(Exception e){
			System.out.println(e);
			System.exit(1);
		}
		int bytes_read = 0;
		buffer = ByteBuffer.allocate(200);
		int c=0;
		try{ bytes_read = f.read(buffer); }
		catch(Exception e){System.out.println(e);}
		buffer.flip();
		while(c<n && bytes_read!=-1){
			buffer.mark();
			try{
				test32[c] = read32(buffer);
				test24[c] = read24(buffer);
				test16[c] = read16(buffer);
				test8[c] = read8(buffer);

				testHash[c] = readHash(buffer);
				testArray[c] = readArray(buffer);

				testString[c] = readString(buffer);
				c++; //update when wholre record is read
			}catch(java.nio.BufferUnderflowException e){
				buffer.reset();
				//TODO: revisit this!
				//probably a very lazy way but I'm not sure how to do it with one buffer.
				ByteBuffer temp = ByteBuffer.allocate(4096);
				temp.put(buffer);
				buffer = temp;
				try{ bytes_read = f.read(buffer); }
				catch(Exception x){System.out.println(x);}
				buffer.flip();
				System.out.println(buffer);
				System.out.println(bytes_read);
			}
		}
		try{ file.close();}
		catch(Exception e){
			System.out.println(e);
		}
		if(c!=n)
			System.out.println("size error");

		//compare
		System.out.println("\n --- INT 32 --- ");
		for(int i=n; i--!=0;)
			System.out.printf(" %d %d\n",data32[i],test32[i]);

		System.out.println("\n --- INT 24 --- ");
		for(int i=n; i--!=0;)
			System.out.printf(" %d %d\n",data24[i],test24[i]);

		System.out.println("\n --- INT 16 --- ");
		for(int i=n; i--!=0;)
			System.out.printf(" %d %d\n",data16[i],test16[i]);

		System.out.println("\n --- INT 8 --- ");
		for(int i=n; i--!=0;)
			System.out.printf(" %d %d\n",data8[i],test8[i]);

		System.out.println("\n --- HASH --- ");
		for(int i=n; i--!=0;){
			for(int j=0; j<dataHash[i].length; j++)
				System.out.printf("%d ",dataHash[i][j]);
			System.out.println();
			for(int j=0; j<testHash[i].length; j++)
				System.out.printf("%d ",testHash[i][j]);
			System.out.println();
			System.out.println();
		}

		System.out.println(" --- ARRAY --- ");
		for(int i=n; i--!=0;){
			for(int j=0; j<dataArray[i].length; j++)
				System.out.printf("%d ",dataArray[i][j]);
			System.out.println();
			for(int j=0; j<testArray[i].length; j++)
				System.out.printf("%d ",testArray[i][j]);
			System.out.println();
			System.out.println();
		}


		System.out.println(" --- STRING --- ");
		for(int i=n; i--!=0;){
			System.out.println(dataString[i]);
			System.out.println(testString[i]);
			System.out.println();
		}
	}
}