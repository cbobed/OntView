package sid.OntView2.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

//import org.apache.log4j.Level;

public class VisLevel {

   /***************************/
	final static int MIN_WIDTH=20;
    final static int SPACE_BETWEEN_LEVELS= 50;
	int id;
	int width = MIN_WIDTH;
	int posx;
	VisGraph graph;
    HashSet<Shape> levelShapes;
   /***************************/
	
	public VisLevel(VisGraph pGraph, int pid, int pPosx){
		posx= pPosx;
		id = pid;
		graph = pGraph;
		levelShapes = new HashSet<>();
	}
 
	public ArrayList<Shape> orderedList(){
        ArrayList<Shape> list = new ArrayList<>(levelShapes);
		list.sort(Shape.POSY_ORDER);
		return list;
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
	 */
	public void setXpos(int x){
		posx=x;
		//it requires the shape's in the level to be updated
		for (Shape s : getShapeSet()){
			s.setPosX(getXpos() + s.getWidth() / 2);
		}
    }
	
	
	/**
	 * Updates posx due to level width expansion 
	 */
	public void updateWidth(int newWidth) {
		
	    final int DININCREM = 5;
	    width = (levelShapes.size()>20 ? getWidth() + DININCREM *(levelShapes.size()-20) : getWidth());
		int dWidth = newWidth - getWidth();
        if (dWidth > 0){
            for (VisLevel level : graph.levelSet){
                if (level.getID() > id){
                    level.setXpos(level.getXpos()+dWidth);
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
		VisLevel oldL= shape.getVisLevel();
		if (oldL !=null) {
			oldL.levelShapes.remove(shape);
		}
		levelShapes.add(shape);
	    shape.vdepthlevel=this;
	    shape.depthlevel = this.getID();
		shape.setPosX(posx + shape.getWidth()/2);

	}
	

	/**
	 * folds levelSet and removes empty levels
	 */
	public static void shrinkLevelSet(Set<VisLevel> set){
		
	//shrink
		for (int i=firstLevel(set) ; i<lastLevel(set); i++){
			VisLevel currentLevel = getLevelFromID(set, i);
            assert currentLevel != null;
            if (!currentLevel.isConstraintLevel()){
				currentLevel.fold(set);
			}
		}
		// remove empty levels
		HashSet<VisLevel> emptyLevels = new HashSet<>();
		for (VisLevel lvl : set) {
			if (lvl.levelShapes.isEmpty()){			
				emptyLevels.add(lvl);
			}	
		}
		for (VisLevel emptyLvl : emptyLevels) {
			for (VisLevel lvl : set){
				if (lvl.getID()> emptyLvl.getID()) {
					lvl.setID(lvl.getID()-1);
				}
			}
			set.remove(emptyLvl);
		}
		
	}
	
	
	/**
	 * When expanding, adding constraints results in adding to many nearly empty
	 * levels. By calling fold, we merge those levels as much as possible.
	 * From level i+1 to the last one moves shapes to level i if possible
	 */
	
	private void fold(Set<VisLevel>set){
		int j = id+1;
		boolean possible = true;
		Set<Shape> movableSet = new HashSet<>();
		while ( (Objects.requireNonNull(getLevelFromID(set, j)).isConstraintLevel())  ||  (j!=lastLevel(set))) {
			VisLevel lvl = getLevelFromID(set, j);
            assert lvl != null;
            for (Shape shape : lvl.levelShapes){
			    possible = true;
				for (VisConnector c : shape.inConnectors){
                    if (c.from.getVisLevel().getID() >= id) {
                        possible = false;
                        break;
                    }
				}
				if (possible)
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
	 * After changes made by shrinkLevelSet position and size
	 * information could be outdated. Hence, this method
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
                    int shapeWidth = Math.max(shape.getWidth(), (int) shape.getIndicatorSize());
                    if (shapeWidth > maxShapeWidthInLevel) {
                        maxShapeWidthInLevel = shapeWidth;
                    }
                }
                maxShapeWidthInLevel += SPACE_BETWEEN_LEVELS;
			}
			lvl.setWidth(maxShapeWidthInLevel+MIN_WIDTH);
			lvl.updateWidth(maxShapeWidthInLevel+MIN_WIDTH);
			
		}
		for (int i=firstLevel(set)+1 ; i<=maxLevel; i++){
			VisLevel lvl = VisLevel.getLevelFromID(set, i);
			VisLevel prevLvl = VisLevel.getLevelFromID(set, i-1);
            assert prevLvl != null;
            assert lvl != null;
            lvl.setXpos(prevLvl.getXpos()+prevLvl.getWidth());
		}
		VisLevel lvl = VisLevel.getLevelFromID(set, 0);
        assert lvl != null;
        lvl.setXpos(lvl.getXpos());
	}
	
	/**
	 * creates a new level with the specified id
	 * Looks for the previous level to get its data
	 * and pushes levels with id greater or equal than specified
	 */
	public static void insertLevel(Set<VisLevel> set, int id, VisGraph graph){
	    //creates a new Level
		VisLevel newLvl = null;
		if (id > 0){
			VisLevel prevLevel = VisLevel.getLevelFromID(set, id-1);
			if (prevLevel!= null) {
				newLvl = new VisLevel(graph, id, prevLevel.posx+prevLevel.width);
			}
			else { 
				//shouldn't enter here though
				newLvl = new VisLevel(graph, id, VisClass.FIRST_X_SEPARATION);
			}
			for (VisLevel lvl: set){
				if (lvl.getID() >= id){
					lvl.setID(lvl.getID()+1);
				}
			}
			set.add(newLvl);
		}
	}
	
	/**
	 * Returns last level if from the specified set
	 */
	public static int lastLevel(Set<VisLevel> set){
	//creates a new Level
		int i = 0;
		for (VisLevel lvl : set) 
			i = (Math.max(lvl.getID(), i));
		return i;
	}
	
	/**
	 * Returns first level if from the specified set
	 */
	public static int firstLevel(Set<VisLevel> set){
	//creates a new Level
		int i = 0;
		for (VisLevel lvl : set) 
			i = (Math.min(lvl.getID(), i));
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
	 */
	public boolean allLevelShapesHidden(){
		for (Shape s : this.getShapeSet()){
			if (s.isVisible()) {
                return false;
            }
		}
		return true;
	}
		
}
