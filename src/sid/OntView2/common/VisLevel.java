package sid.OntView2.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

//import org.apache.log4j.Level;

public class VisLevel {


   /***************************/
	final static int MIN_WIDTH=30;
	int id;
	int width = MIN_WIDTH;
	int shapeNo = 0;
	int posx = 0;
	VisGraph graph;
    HashSet<Shape> levelShapes;
    ArrayList<Shape> shapeAsList;
    boolean changed;
   /***************************/
	
	public VisLevel(VisGraph pgraph, int pid, int pposx){
		posx= pposx;
		id = pid;
		graph = pgraph;
		levelShapes = new HashSet<Shape>();
	}
 
	public ArrayList<Shape> orderedList(){
		ArrayList<Shape> list = new ArrayList<Shape>();
		for (Shape shape : levelShapes){
			list.add(shape);
		}
		Collections.sort(list, Shape.POSY_ORDER);
		return list;
	}

	public int getShapeNo(){
		int c=0;
		for (Shape s : levelShapes){
			c++;
		}	
		return c;		
	}	
	
	public void put(Shape s){levelShapes.add(s);}
	public HashSet<Shape> getShapeSet(){return levelShapes;}
	public void setWidth(int x){width=x;}
	public int getWidth() {return width;}
	public int getXpos(){return posx;}

	public int getID(){return id;}
	public void setID(int pid){
		for (Shape s :levelShapes){
			s.depthlevel = pid;
		}
		id = pid;
	}
		
	
	/**
	 * Updates position of shapes that are in the level
	 * @param x
	 */
	public void setXpos(int x){
		posx=x;
		//it requires the shape's in the level to be updated
		for (Shape s : getShapeSet()){
			s.setPosX(getXpos()+s.getWidth()/2);
		}

    }
	
	
	/**
	 * Updates posx due to level width expansion 
	 */
	public void updateWidth(int newWidth) {
		
	    final int DININCREM = 5;
	    width = (levelShapes.size()>20 ? getWidth() + DININCREM *(levelShapes.size()-20) : getWidth());
		int dwidth = newWidth -getWidth();
			if (dwidth > 0){
				for (VisLevel level : graph.levelSet){
					if (level.getID() > id){
						level.setXpos(level.getXpos()+dwidth);
					}
				}
			}	
	}
	
	public static VisLevel getLevelFromID(Set<VisLevel>set, int id){
		for (VisLevel v : set){
			if (v.getID()==id)
				return v;
		}
		return null;
	}
	
	public void addShape(Shape shape){
		VisLevel oldl= shape.getVisLevel();
		if (oldl !=null) {
			oldl.levelShapes.remove(shape);
		}
		levelShapes.add(shape);
	    shape.vdepthlevel=this;
	    shape.depthlevel = this.getID();
		shape.setPosX(posx+shape.getWidth()/2);
		
	}
	

	/**
	 * folds levelset and removes empty levels
	 * @param set
	 */
	public static void shrinkLevelSet(Set<VisLevel> set){
		
	//shrink
		for (int i=firstLevel(set) ; i<lastLevel(set); i++){
			VisLevel currentLevel = getLevelFromID(set, i);
			if (!currentLevel.isConstraintLevel()){
				currentLevel.fold(set);
			}
		}
		// remove empty levels
		HashSet<VisLevel> emptyLevels = new HashSet<VisLevel>();
		for (VisLevel lvl : set) {
			if (lvl.levelShapes.isEmpty()){			
				emptyLevels.add(lvl);
			}	
		}
		for (VisLevel emptylvl : emptyLevels) {
			for (VisLevel lvl : set){
				if (lvl.getID()> emptylvl.getID()) {
					lvl.setID(lvl.getID()-1);
				}
			}
			set.remove(emptylvl);
			emptylvl = null;
		}
		
	}
	
	
	/**
	 * When expanding, adding constraints results in adding to many nearly empty
	 * levels. By calling fold, we merge those levels as much as possible.
	 * From level i+1 to the last one moves shapes to level i if possible
	 * @param set
	 */
	
	private void fold(Set<VisLevel>set){
		int j = id+1;
		boolean posible = true;
		Set<Shape> movableSet = new HashSet<Shape>();
		while ( (getLevelFromID(set, j).isConstraintLevel())  ||  (j!=lastLevel(set))) {
			VisLevel lvl = getLevelFromID(set, j);
			for (Shape shape : lvl.levelShapes){
			    posible = true;
				for (VisConnector c : shape.inConnectors){
					if (c.from.getVisLevel().getID() >= id)
						posible=false;
				}
				if (posible)
					movableSet.add(shape);
			}
			for (Shape movable : movableSet){
				movable.setVisLevel(this);
			}
			movableSet.clear();
			j++;
		}
	}
	
	/**
	 * Adjusts level width and position 
	 * After changes made by shrinklevelSet position and size
	 * information could be outdated. Hence this method
	 * @param set
	 */
	
	public static void adjustWidthAndPos(Set<VisLevel> set){
		int maxLevel=0;
		for (VisLevel lvl : set) {
			int maxShapeWidthInLevel = 0;
			if (lvl.getID()> maxLevel)
				maxLevel = lvl.getID();
			if (lvl.allLevelShapesHidden()){
				lvl.setWidth(0);
			}
			else {
				for (Shape shape : lvl.levelShapes){
					if (shape.getWidth() > maxShapeWidthInLevel){
						maxShapeWidthInLevel = shape.getWidth();
					}	
					// enlarge
				}
			}
			lvl.setWidth(maxShapeWidthInLevel+MIN_WIDTH);
			lvl.updateWidth(maxShapeWidthInLevel+MIN_WIDTH);
			
			
		}
		for (int i=firstLevel(set)+1;i<=maxLevel;i++){
			VisLevel lvl = VisLevel.getLevelFromID(set, i);
			VisLevel prevlvl = VisLevel.getLevelFromID(set, i-1);
			lvl.setXpos(prevlvl.getXpos()+prevlvl.getWidth());
		}
		VisLevel lvl = VisLevel.getLevelFromID(set, 0);
		lvl.setXpos(lvl.getXpos());
		
	
		
	}
	
	/**
	 * creates a new level with the specified id
	 * Looks for the previous level to get its data
	 * and pushes levels with id greater or equal than specified
	 * @param set
	 * @param id
	 * @param graph
	 */
	public static void insertLevel(Set<VisLevel> set, int id, VisGraph graph){
	//creates a new Level
		VisLevel newlvl = null;
		if (id > 0){
			VisLevel prevLevel = VisLevel.getLevelFromID(set, id-1);
			if (prevLevel!= null) {
				newlvl = new VisLevel(graph, id, prevLevel.posx+prevLevel.width);
			}
			else { 
				//shouldn't enter here though
				newlvl = new VisLevel(graph, id, VisClass.FIRST_X_SEPARATION);
			}
			for (VisLevel lvl: set){
				if (lvl.getID() >= id){
					lvl.setID(lvl.getID()+1);
				}
			}
			set.add(newlvl);
		}
	}
	
	/**
	 * Returns last level if from the specified set
	 * @param set
	 * @return
	 */
	public static int lastLevel(Set<VisLevel> set){
	//creates a new Level
		int i = 0;
		for (VisLevel lvl : set) 
			i = (  lvl.getID()>i ? lvl.getID() : i );
		return i;
	}
	
	/**
	 * Returns first level if from the specified set
	 * @param set
	 * @return
	 */
	public static int firstLevel(Set<VisLevel> set){
	//creates a new Level
		int i = 0;
		for (VisLevel lvl : set) 
			i = (  lvl.getID()<=i ? lvl.getID() : i );
		return i;
	}
	
	
	
	/**
	 * Sees if all shapes in the level are constraints 
	 */
	public boolean isConstraintLevel(){
		//false unless all shapes are constraints and it's not an empty level
		boolean is= false;
		
		for (Shape s : levelShapes) {
			if (s instanceof VisClass){
				return false;
			}	
			if (s instanceof VisConstraint){
				is = true;
			}
		}
		return is;
	}
	
	public boolean isBottomLevel(){
		for (Shape s : levelShapes) {
			if ((s instanceof VisClass) && (s.asVisClass().isBottom)) {
				return true;
			}	
		}	
		return false;
	}
	
	
	public VisLevel copy(){
		VisLevel newLevel = new VisLevel(graph, id, getXpos());
		newLevel.width = getWidth();
		return newLevel;
		
	}
	
	/**
	 * If all shapes in level are hidden return true. False otherwise
	 * @return
	 */
	public boolean allLevelShapesHidden(){
		for (Shape s : this.getShapeSet()){
			if (s.isVisible())
				return false;
		}
		return true;
	}
		
}
