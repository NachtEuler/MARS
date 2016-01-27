package MARS;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.BufferUnderflowException;
import java.nio.BufferOverflowException;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.IOException;

public class Catalogue{

	/*** VARIABLES ***/
	//reference to containing archive for absolute root, etc.
	Archive archive;
	//relative glossary and list of records
	Glossary<String> paths;
	ArrayList<Entry> list;


	/*** MANAGEMENT ***/
	//replaces catalogue data
	private void update(){
		//TODO: update definition
		//TODO: rebuild glossary
	}


	/*** STORAGE ***/
	//Reads a catalogue from a specified file
	public void read(ReadableByteChannel in) throws IOException{
		//TODO: Size to allocate ?
		ByteBuffer buffer = ByteBuffer.allocate(0x1000);
		int bytes_read;

		//READ GLOSSARY
		while((bytes_read = in.read(buffer))!=-1){
			//TODO: read String from buffer
		}

		//READ ENTRIES
		while((bytes_read = in.read(buffer))!=-1){
			//TODO: read Entry from buffer
		}
	}

	//Writes a catalogue to a specified file
	public void write(WritableByteChannel out) throws IOException{
		//TODO: Size to allocate ?
		ByteBuffer buffer = ByteBuffer.allocate(0x1000);
		boolean already_failed = false;//tracks if write has already failed, double fails are huge problem

		// WRITE GLOSSARY
		{	//write number of entries
			SIO.write32(buffer,paths.size());
			// iterate over entries
			Iterator<String> iterator = paths.iterator();
			String current;
			current = iterator.next();
			while(current!=null){

				//write this entry to buffer
				buffer.mark();                    //mark buffer in case of failure
				try{ SIO.writeString(buffer,current); }      //try to write current
				catch(BufferOverflowException e){ //if full
					buffer.reset();                //reset and write out
					buffer.flip();
					out.write(buffer);
					buffer.clear();

					if(!already_failed){           //first time, try again
						already_failed = true;
						continue;
					}else{                         //second time, note the error
						//TODO: ERROR HANDLING
						System.out.println("\n\n Writing Glossary Failed \n");
					}
				}

				//move to new entry
				current = iterator.next();
				already_failed=false;
			}
			//NOTE: loop always ends with something in the buffer
		}

		//start fresh
		buffer.flip();
		out.write(buffer);
		buffer.clear();

		// WRITE ENTRIES
		{	//write number of entries
			SIO.write32(buffer,list.size());
			// iterate over entries
			Iterator<Entry> iterator = list.iterator();
			Entry current;
			current = iterator.next();
			while(current!=null){

				//write this entry to buffer
				buffer.mark();                        //mark buffer in case of failure
				try{ current.writeTo(buffer,paths); } //try to write current
				catch(BufferOverflowException e){     //if full
					buffer.reset();                    //reset and write out
					buffer.flip();
					out.write(buffer);
					buffer.clear();

					if(!already_failed){               //first time, try again
						already_failed = true;
						continue;
					}else{                             //second time, note the error
						//TODO: ERROR HANDLING
						System.out.println("\n\n Writing Entries Failed \n");
					}
				}

				//move to new entry
				current = iterator.next();
				already_failed=false;
			}
			//NOTE: loop always ends with something in the buffer
		}

		//finish up
		buffer.flip();
		out.write(buffer);
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
		//reads dat
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

}