package MARS;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.BufferUnderflowException;
import java.nio.BufferOverflowException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;

public class Catalogue{

	/*** VARIABLES ***/
	//reference to containing archive for absolute root, etc.
	Archive archive;
	//relative glossary and list of records
	Glossary<String> paths;
	ArrayList<Entry> list;



	/*** CONSTRUCTOR ***/
	public Catalogue(int glossary_size, int list_size){
		paths = new Glossary<String>(glossary_size);
		list = new ArrayList<Entry>(list_size);
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
	}






	/*** TESTING ***/
	public static void main(String[] args){
		Entry X = new Entry();
		X.archive_id=1;
		X.path="Some\\Path\\";
		X.file_name="Some.File";
		X.file_size=2;
		byte[] x_hash = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};
		X.hash = x_hash;
		byte[] x_extra = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23};
		X.extra = x_extra;

		Catalogue C = new Catalogue(16,16);
		C.list.add(X);
		C.paths.add(X.path);

		try{
			C.write(args[0]);
		}catch(IOException e){
			e.printStackTrace(System.out);
		}
	}

}