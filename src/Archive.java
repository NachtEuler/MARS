package MARS;

import java.net.URI;
import java.util.HashMap;

//Archive provides the basic structure to navigate the archive relative
//to the client program for access or modification. When making changes
//such as updating catalogues or adding files, this needs to be wrapped
//in a working archive to maintain and modify structures in memory.
public class Archive{

	//locations of Archive contents relative to client
	URI root;
	URI master_table;
	HashMap<String,URI> Catalogues;
	HashMap<String,URI> Networks;

	//loads a an archive from an archive file
	public Archive(URI archive_file){
		//TODO
	}

	public Index loadIndex(){
		return null; //TODO
	}

	public Catalogue loadCatalogue(int id){
		return null; //TODO
	}

	public Network loadNetwork(int id){
		return null; //TODO
	}
}