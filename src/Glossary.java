package MARS;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

//Glossary pairs (integer valued) symbols with (M valued) meanings.
//The goal is to reduce file size in cases of redundancy.

public class Glossary<M>{

	/*** PARAMETERS ***/
	//Limits to 16-bit symbols to save space in files
   private static final int LIMIT = 0xFFFF0000;
   //error codes for int methods
   public static final int CANNOT_ASSIGN = -1;
   public static final int NOT_FOUND = -1;


	/*** VARIABLES ***/
	private ArrayList<M> list;
	private HashMap<M,Integer> map;


	/*** CONSTRUCTORS ***/
	//Constructs backing HashMap and ArrayList
	public Glossary(int initialCapacity, float loadFactor){
		if((initialCapacity & LIMIT)!=0){
			//TODO: warn about limit?
		}
		// TODO: determine relationship between sizes
		map = new HashMap<M,Integer>(initialCapacity,loadFactor);
		list = new ArrayList<M>(initialCapacity);
	}


	/*** MODIFIERS ***/
	//returns symbol assigned to meaning (adding new symbol if needed
	public int assign(M meaning){
		Integer I = map.get(meaning);
		if(I==null){
			if((list.size() & LIMIT)!=0){
				//TODO: error size limit
				return CANNOT_ASSIGN;
			}
			I = new Integer(list.size());
			map.put(meaning,I);
			list.add(meaning);
		}
		return I.intValue();
	}


	/*** ACCESSORS ***/
	//returns symbol or NOT_FOUND if not found
	public int getSymbol(M meaning){
		Integer I = map.get(meaning);
		if(I==null)
			return NOT_FOUND;
		return I.intValue();
	}

	//returns meaning or null if not found
	public M getMeaning(int symbol){
		return list.get(symbol);
	}

	/*** SPECIAL ***/
	//get details from ArrayList
	public int size(){
		return list.size();
	}
	public Iterator<M> iterator(){
		return list.iterator();
	}

}