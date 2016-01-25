package MARS;

import java.nio.ByteBuffer;
import java.util.ArrayList;

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
	//Writes a catalogue to a specified file
	public void write(){
		//TODO: write glossary
		//TODO: write entries
	}

	//Reads a catalogue from a specified file
	public void read(){
		//TODO: read glossary
		//TODO: read entries
	}


	/*** SINGLE ENTRY ***/
	//all the data in one entry of a catalogue
	public class Entry{
		int archive_id;
		short path_symbol;
		String file_name;
		int file_size;
		byte[] check_sum;
		byte[] extra_data;

		/*** STORAGE ***/
		//reads data into blank Entry
		public void read(ByteBuffer in){
			//TODO
		}
		//writes data
		public void write(ByteBuffer out){
			//TODO
		}

	}
}