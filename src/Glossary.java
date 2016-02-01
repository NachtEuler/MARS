package MARS;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;
import java.util.AbstractCollection;

/* Glossary pairs (integer valued) symbols with (M valued) meanings,
 * conceptually creating a bijective map that is quick to convert
 * between symbol and meaning.
 *
 * The goal is to reduce file size in cases of redundancy.
 *
 * Glossary is implemented as a Collection of meanings to simplify
 * read/write operations. Because symbol-meaning pairs may define
 * recorded data they should generally not support removal.
 *
 * Moreover integer valued symbols can be assigned consecutively
 * and infered from an ordered list of meaning.
 */

public class Glossary<M> extends AbstractCollection<M>{

	/*** PARAMETERS ***/
	//Limits to 16-bit symbols to save space in files
   private static final int LIMIT = 0xFFFF0000;
   //error codes for int methods
   public static final int CANNOT_ASSIGN = -1;
   public static final int NOT_FOUND = -1;
   //default sizes
   private static final int DEFAULT_INITIAL_CAPACITY = 16;
   private static final float DEFAULT_LOAD_FACTOR = 0.75f;



	/*** VARIABLES ***/
	private ArrayList<M> list;
	private HashMap<M,Integer> map;



	/*** CONSTRUCTORS ***/

	//Collections Recommended Constructors
	public Glossary(){
		this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
	}

	public Glossary(Collection<? extends M> c){
		this(c.size(), DEFAULT_LOAD_FACTOR);
		for(M element:c)
			add(element);
	}

	//intial capacity only
	public Glossary(int initialCapacity){
		this(initialCapacity, DEFAULT_LOAD_FACTOR);
	}

	//Constructs backing HashMap and ArrayList
	public Glossary(int initialCapacity, float loadFactor){
		//keep sizes for arralist and hashmap consistent.
		if(DEFAULT_INITIAL_CAPACITY < initialCapacity){
			initialCapacity = DEFAULT_INITIAL_CAPACITY;
		}
		if((initialCapacity & LIMIT)!=0){
			//TODO: warn about limit?
		}
		// TODO: determine relationship between sizes
		map = new HashMap<M,Integer>((int)(initialCapacity/loadFactor),loadFactor);
		list = new ArrayList<M>(initialCapacity);
	}



	/*** MODIFIERS ***/

	//Collections Methods - adds a meaning to the glossary
	public boolean add(M meaning){
		return assign(meaning)!=CANNOT_ASSIGN;
	}

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

	//Collection Methods - same as size of list
	public int size(){
		return list.size();
	}

	//Collection Method - more efficent that default iteration
	public boolean contains(Object o){
		return map.containsKey(o);
	}

	//Collection Method - iterates over meanings
	//TODO: should not support remove, arraylist iterator does
	public Iterator<M> iterator(){
		return list.iterator();
	}

}