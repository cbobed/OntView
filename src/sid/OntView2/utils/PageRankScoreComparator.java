package sid.OntView2.utils;

import java.util.Comparator;
import java.util.Map.Entry;

public class PageRankScoreComparator implements Comparator<Entry<String, Double>>{
	public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
		// TODO Auto-generated method stub
		if (o1.getValue() < o2.getValue()) 
			return  -1; 
		else if (o1.getValue() > o2.getValue()) 
			return 1; 
		else 
			return 0;
	}
}
