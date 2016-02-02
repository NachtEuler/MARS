package MARS;

// Static I/O features for this application
// global encoding and size issue addressed

import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.io.IOException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.BiConsumer;

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



	/*** Buffer Handling ***/
	//recycle data currently in a ByteBuffer
	public static void recycle(ByteBuffer buffer){
		ByteBuffer old = buffer.slice(); //keep position of old data
		buffer.clear();                  //reset position of buffer
		buffer.put(old);                 //move old data to start
	}



	/*** Simplify Read/Write Operations on Collections ***/
	/* Perform multiple iterations of read-then-get or put-then-write
	 * from on a single channel and put/read ready buffer.
	 *
	 * Each iteration is a function involving a buffer and type and
	 * performing solely get (for read) or put (for write) operations
	 * on the buffer. Modifing mark, flipping, reseting, etc. are not
	 * safe.
	 *
	 * For readAll, the buffer should begin and end ready for get and
	 * full, as if read(buffer) has just been called.
	 *
	 * For writeAll, the buffer should begin and end ready for put and
	 * empty, as if clear() has just been called.
	 *
	 * A collection is needed that supports add(), iterator(), size().
	 */

	//works with full buffer, takes data from buffer and reads in
	public static <T> void readAll(Collection<T> col, Function<ByteBuffer,T> read,
								ReadableByteChannel in, ByteBuffer buffer) throws IOException{

		//intialize (buffer starts with data)
		ByteBuffer buffer_saver;
		T read_item = null;
		//determine size to read
		int items_remaing = read32(buffer);

		//iterate bulk of data
		while(items_remaing!=0){
			//mark position and get from buffer
			buffer.mark();
			try{
				read_item = read.apply(buffer);
			}catch(BufferUnderflowException e){
				//keep partial read, read in new data, try again
				buffer.reset();
				recycle(buffer);
				in.read(buffer);
				buffer.flip();
				continue;
			}
			//store entry
			col.add(read_item);
			items_remaing--;
		}//NOTE: loop may end with data for the next items data in buffer

		//refill buffer (buffer ends full)
		recycle(buffer);
		in.read(buffer);
		buffer.flip();
	}

	//works with empty buffer, fills buffer with data and writes out
	public static <T> void writeAll(Collection<T> col, BiConsumer<ByteBuffer,T> write,
								WritableByteChannel out, ByteBuffer buffer) throws IOException{

		//intialize (buffer begins empty)
		int items_remaining = col.size();
		Iterator<T> itr = col.iterator();
		T item;
		if(!itr.hasNext()) return; //nothing to put/write
		item = itr.next();
		//write total size
		write32(buffer, items_remaining);

		//iterate to write bulk of data
		while(items_remaining!=0){
			//mark position and put to buffer
			buffer.mark();
			try{
				write.accept(buffer,item);
			}catch(BufferOverflowException e){
				//truncate partial write, ouptut whole data, try again
				buffer.reset();
				buffer.flip();
				out.write(buffer);
				buffer.clear();
				continue;
			}
			//move to next entry
			items_remaining--;
			if(!itr.hasNext()) break; //nothing left to put
			item = itr.next();
		}//NOTE: loop always ends with something in the buffer

		//finish up (buffer ends empty)
		buffer.flip();
		out.write(buffer);
		buffer.clear();
	}



	/*** FORMATING ***/
	//may not belong in SIO
	private static final char[] hex =
		{'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	public static String hexString(byte[] a){
		StringBuilder build = new StringBuilder(2*a.length);
		for(int i=0; i<a.length; i++){
			build.append(hex[(a[i]&0xF0)>>>4]);
			build.append(hex[a[i]&0x0F]);
		}
		return build.toString();
	}

	/*** TESTING ***/
	//No libraries are imported above for this code making it easy to remove
	public static void main(String[] args){
		String file_name = args[0];
		int seed = Integer.parseInt(args[1]);
		int n=5;
		java.util.Random rand = new java.util.Random(seed);

		//NOW TEST COLLECTIONS
		//allocate
		java.util.List<Integer> data32 = new java.util.ArrayList<Integer>(n);
		java.util.List<Integer> data16 = new java.util.LinkedList<Integer>();
		java.util.Set<String> dataString = new java.util.HashSet<String>(2*n);

		java.util.List<Integer> test32 = new java.util.ArrayList<Integer>(n);
		java.util.List<Integer> test16 = new java.util.LinkedList<Integer>();
		java.util.Set<String> testString = new java.util.HashSet<String>(2*n);

		//randomize
		for(int i=n; i--!=0;)
			data32.add(rand.nextInt());
		for(int i=n; i--!=0;)
			data16.add(rand.nextInt()&0xFFFF);
		for(int i=n; i--!=0;)
			dataString.add(randomString(rand));

		//write
		java.io.RandomAccessFile file = null;
		java.nio.channels.FileChannel f = null;
		ByteBuffer buffer = ByteBuffer.allocate(256);
		try{
			file = new java.io.RandomAccessFile(file_name, "rw");
			f = file.getChannel();
			buffer.clear();
			writeAll(data32,(ByteBuffer b, Integer i)->{write32(b,i.intValue());},f,buffer);
			writeAll(data16,(ByteBuffer b, Integer i)->{write16(b,i.intValue());},f,buffer);
			writeAll(dataString,(ByteBuffer b, String s)->{writeString(b,s);},f,buffer);
			file.close();
		}
		catch(Exception e){
			e.printStackTrace(System.out);
			System.exit(1);
		}

		//read
		try{
			file = new java.io.RandomAccessFile(file_name, "rw");
			f = file.getChannel();
			buffer.clear();
			f.read(buffer);
			buffer.flip();
			readAll(test32,(ByteBuffer b)->{return read32(b);},f,buffer);
			readAll(test16,(ByteBuffer b)->{return read16(b);},f,buffer);
			readAll(testString,(ByteBuffer b)->{return readString(b);},f,buffer);
			file.close();
		}
		catch(Exception e){
			e.printStackTrace(System.out);
			System.exit(1);
		}

		//compare
		System.out.println(" --- INT 32 ARRAY LIST --- ");
		for(Integer i : data32) System.out.printf("%11d ",i);
		System.out.println();
		for(Integer i : test32) System.out.printf("%11d ",i);
		System.out.println("\n");
		System.out.println(" --- INT 16 LINKED LIST --- ");
		for(Integer i : data16) System.out.printf("%6d ",i);
		System.out.println();
		for(Integer i : test16) System.out.printf("%6d ",i);
		System.out.println("\n");
		System.out.println(" --- STRING HASH SET --- ");
		for(String s : dataString) System.out.printf("%s ",s);
		System.out.println();
		for(String s : testString) System.out.printf("%s ",s);
		System.out.println("\n");

	}

	static String randomString(java.util.Random rand){
		String ABC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890._-";
		int length = rand.nextInt(30);
		StringBuilder building = new StringBuilder(30);
		while(length--!=0)
			building.append(ABC.charAt(rand.nextInt(ABC.length())));
		return building.toString();
	}
}