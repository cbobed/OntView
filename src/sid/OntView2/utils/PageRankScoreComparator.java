package sid.OntView2.utils;

import java.util.Comparator;
import java.util.Map.Entry;

public class PageRankScoreComparator implements Comparator<Entry<String, Double>>{
	public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
		// TODO Auto-generated method stub
        return o1.getValue().compareTo(o2.getValue());
	}
}
