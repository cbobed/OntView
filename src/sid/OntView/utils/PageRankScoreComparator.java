package sid.OntView.utils;

import java.util.AbstractMap.SimpleEntry;
import java.util.Comparator;


public class PageRankScoreComparator implements Comparator<SimpleEntry<String, Float>>{
	public int compare(SimpleEntry<String, Float> o1, SimpleEntry<String, Float> o2) {
		// TODO Auto-generated method stub
		if (o1.getValue() < o2.getValue()) 
			return  -1; 
		else if (o1.getValue() > o2.getValue()) 
			return 1; 
		else 
			return 0;
	}
}
