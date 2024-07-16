package sid.OntView.utils;

import java.util.Comparator;

import eu.wdaqua.pagerank.PageRankScore;

public class PageRankScoreComparator implements Comparator<PageRankScore>{
	public int compare(PageRankScore o1, PageRankScore o2) {
		// TODO Auto-generated method stub
		if (o1.pageRank < o2.pageRank) 
			return  -1; 
		else if (o1.pageRank > o2.pageRank) 
			return 1; 
		else 
			return 0;
	}
}
