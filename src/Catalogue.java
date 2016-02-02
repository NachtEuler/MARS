package MARS;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.BufferUnderflowException;
import java.nio.BufferOverflowException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;
import java.util.AbstractCollection;

public class Catalogue extends AbstractCollection<Catalogue.Entry>{

	/*** PARAMETERS ***/
   //default sizes
   private static final int DEFAULT_INITIAL_LIST_CAPACITY = 256;
   private static final int DEFAULT_INITIAL_PATH_CAPACITY = 256;



	/*** VARIABLES ***/
	//reference to containing archive for absolute root, etc.
	Archive archive;
	//relative glossary and list of records
	Glossary<String> paths;
	ArrayList<Entry> list;



	/*** CONSTRUCTOR ***/

	//Collections Recommended Constructors
	public Catalogue(){
		this(DEFAULT_INITIAL_LIST_CAPACITY, DEFAULT_INITIAL_PATH_CAPACITY);
	}
	public Catalogue(Collection<? extends Catalogue.Entry> c){
		//optimal relationship?
		this(c.size(), DEFAULT_INITIAL_PATH_CAPACITY);
		for(Entry element:c)
			add(element);
	}

	//Constructs backing Glossary and ArrayList
	public Catalogue(int list_size, int glossary_size){
		paths = new Glossary<String>(glossary_size);
		list = new ArrayList<Entry>(list_size);
	}



	/*** COLLECTIONS METHODS ***/

	public boolean add(Entry e){
		//make sure we can add to the glossary first
		if( paths.assign(e.path) == Glossary.CANNOT_ASSIGN)
			return false;
		//add to the list
		if(list.add(e))
			return true;
		else
			return false;//TODO: What if path accepts but list fails?
	}
	public int size(){
		return list.size();
	}
	public Iterator<Entry> iterator(){
		//TODO: Again, bad remove method
		return list.iterator();
	}



	/*** MANAGEMENT ***/
	//replaces catalogue data
	private void update(){
		//TODO: update definition
		//TODO: rebuild glossary
	}



	/*** STORAGE ***/
	//Reads a catalogue from a specified file
	public void read(String file_name) throws IOException{
		//Does file type matter
		FileInputStream file = new FileInputStream(file_name);
		ReadableByteChannel in = file.getChannel();

		//TODO: Size to allocate ?
		ByteBuffer buffer = ByteBuffer.allocate(0x1000);
		in.read(buffer);
		buffer.flip();
		SIO.readAll(paths,  (b)->SIO.readString(b), in, buffer);
		SIO.readAll(list, (b)->Entry.readEntry(b,paths), in, buffer);

		file.close();
	}

	//Writes a catalogue to a specified file
	public void write(String file_name) throws IOException{
		//Does file object choice matter?
		//RandomAccessFile file = new RandomAccessFile(file_name, "rw");
		FileOutputStream file = new FileOutputStream(file_name);
		WritableByteChannel out = file.getChannel();

		//TODO: Size to allocate ?
		ByteBuffer buffer = ByteBuffer.allocate(0x1000);
		buffer.clear();
		SIO.writeAll(paths, (b,m)->SIO.writeString(b,m), out, buffer);
		SIO.writeAll(list, (b,e)->e.writeTo(b,paths), out, buffer);

		file.close();
	}



	/*** FORMATING ***/
	public void print(PrintStream out, boolean print_glossary){
		out.println(" --- CATALOGUE PRINT ---");
		out.println();
		if(print_glossary){
			out.println("   PATH GLOSSARY:");
			paths.printTable(out);
		}
		out.println("   CATALOGUE LIST:");
		out.println();
		for(Entry e : list){
			out.printf(" %10d : %10d : %s",
					e.archive_id, e.file_size, SIO.hexString(e.hash));
			out.println();
			//TODO: choose the right seperator
			out.printf(" %s\\%s", e.path, e.file_name);
			out.println();
			out.printf("%s",SIO.hexString(e.extra));
			out.println();
			out.println();
		}
		out.println();
		out.println(" --- CATALOGUE PRINT ---");
	}



	/*** SINGLE ENTRY ***/
	//reflects the low-level data stored in a an entry
	public static class Entry{
		int archive_id;
		String path;
		String file_name;
		int file_size;
		byte[] hash;
		byte[] extra;

		/*** STORAGE ***/
		//wrapper for reading data to new entry
		public static Entry readEntry(ByteBuffer in, Glossary<String> path_glossary){
			Entry e = new Entry();
			e.readFrom(in,path_glossary);
			return e;
		}
		//reads data
		public void readFrom(ByteBuffer in, Glossary<String> path_glossary){
			archive_id = SIO.read32(in);
			path = path_glossary.getMeaning(SIO.read16(in));
			file_name = SIO.readString(in);
			file_size = SIO.read32(in);
			hash = SIO.readHash(in);
			extra = SIO.readArray(in);
		}
		//writes data
		public void writeTo(ByteBuffer out, Glossary<String> path_glossary){
			SIO.write32(out,archive_id);
			SIO.write16(out,path_glossary.getSymbol(path));
			SIO.writeString(out,file_name);
			SIO.write32(out,file_size);
			SIO.writeHash(out,hash);
			SIO.writeArray(out,extra);
		}

		/*** FORMATING ***/
		public String toString(){
			return file_name;//TODO
		}
	}






	/*** TESTING ***/
	// [Directory] [seed] [catalogue_file1] [catalogue_file1] [print1] [print2]
	public static void main(String[] args){
		Catalogue C = new Catalogue();
		int seed = Integer.parseInt(args[1]);
		java.util.Random r = new java.util.Random(seed);

		System.out.println("Building First Catalogue");
		System.out.println("Searching "+args[0]);
		//path walking stream borrowed from StackOverflow
		try{ java.nio.file.Files.walk(java.nio.file.Paths.get(args[0]))
        	 .filter(java.nio.file.Files::isRegularFile)
        	 .forEach((p)->C.add(makeEntry(p,r)));
		} catch(Exception e){ System.out.println(e); }

		System.out.println("Writing "+args[2]);
		try{
			C.write(args[2]);
		}catch(IOException e){
			e.printStackTrace(System.out);
			System.out.println("WRITE FAILED");
			System.exit(1);
		}

		System.out.println("Building Second Catalogue");
		Catalogue X = new Catalogue();

		System.out.println("Reading "+args[2]);
		try{
			X.read(args[2]);
		}catch(IOException e){
			e.printStackTrace(System.out);
			System.out.println("READ FAILED");
			System.exit(1);
		}

		System.out.println("Print Readable Recods");
		try{
			FileOutputStream print1 = new FileOutputStream(args[4]);
			FileOutputStream print2 = new FileOutputStream(args[5]);
			C.print(new PrintStream(print1),true);
			X.print(new PrintStream(print2),true);
			print1.close();
			print2.close();
		}catch(Exception e){
			e.printStackTrace(System.out);
			System.out.println("PRINT FAILED");
			System.exit(1);
		}

		System.out.println("Writing "+args[3]);
		try{
			X.write(args[3]);
		}catch(IOException e){
			e.printStackTrace(System.out);
			System.out.println("WRITE FAILED");
			System.exit(1);
		}

		System.out.println("DONE!\n");
	}

	//FOLLOWING CODE MAKES A FAIRLY REALISTIC (WE ANTICIPATE) ENTRY
	//Only srchive's id and extra data does not come from actural files
	static int md5_buf_size = 0x1000;
	static ByteBuffer md5_buffer = ByteBuffer.allocate(md5_buf_size);
   static java.security.MessageDigest md5;
   static{
   	try{
   		md5 = java.security.MessageDigest.getInstance("MD5");
   	}catch(Exception e){}
   }
	public static Entry makeEntry(java.nio.file.Path p, java.util.Random r){
		Entry e = new Entry();
		e.archive_id = r.nextInt(0x0FFFFFFF); //depends on archive
		e.path = p.getParent().toString();
		e.file_name = p.getFileName().toString();
		java.io.File f = p.toFile();
		e.file_size = (int)f.length();
		try{
			FileInputStream file = new FileInputStream(f);
			ReadableByteChannel in = file.getChannel();

			md5.reset();
			int bytes_read = 0;

			do{
				md5_buffer.clear();
				bytes_read = in.read(md5_buffer);
				md5_buffer.flip();
				md5.update(md5_buffer);
			}while(bytes_read == md5_buf_size);

			e.hash = md5.digest();
	   } catch(Exception x){
	   	x.printStackTrace(System.out);
	   	System.exit(1);
	   }
	   e.extra = new byte[r.nextInt(0x100)];
	   r.nextBytes(e.extra); //may depend on archive, we don't know yet
	   return e;
	}

}