package cs276.assignments;

import java.util.ArrayList;
import java.util.List;

public class PostingList {

	private int termId;
	/* A list of docIDs (i.e. postings) */
	private List<Integer> postings;

	public PostingList(int termId, List<Integer> list) {
		this.termId = termId;
		this.postings = list;
	}

	public PostingList(int termId) {
		this.termId = termId;
		this.postings = new ArrayList<Integer>();
	}

	public int getTermId() {
		return this.termId;
	}

	public List<Integer> getList() {
		return this.postings;
	}
	
	public String toString() {
		return "termID: " + this.termId + " " + this.postings;
	}
	
	private static void finish(ArrayList<Integer> postings, int index, List<Integer> ar)
	{
		int length = ar.size();
		for(int i = index; i < length; i++)
		{
			postings.add(ar.get(i));
		}
	}
	
	//combine two posting lists with the same term
	
	public static PostingList combineLists(PostingList left,PostingList right)
	{
		Integer termID=left.getTermId();
		List<Integer> leftAr=left.getList();
		List<Integer> rightAr=right.getList();
		
		int leftSize=leftAr.size();
		int rightSize=rightAr.size();
		
		int combinedSize=leftSize+rightSize;
		
		ArrayList<Integer> finalPostings = new ArrayList<Integer>(combinedSize);
		int leftPtr=0;
		int rightPtr=0;
		
		for(int i=0;i<combinedSize;i++)
		{
			if(leftPtr>=leftSize)
			{
				finish(finalPostings,rightPtr,rightAr);
				break;
			}
			if(rightPtr >= rightSize)
			{
				finish(finalPostings,leftPtr,leftAr);
				break;
			}
			
			int leftDoc =  leftAr.get(leftPtr);
			int rightDoc = rightAr.get(rightPtr);
			
			if(leftDoc < rightDoc)
			{
				finalPostings.add(new Integer(leftDoc));
				leftPtr = leftPtr + 1;
			}
			else
			{
				finalPostings.add(new Integer(rightDoc));
				rightPtr = rightPtr + 1;
			}
		}
		
		PostingList p1=new PostingList(termID,finalPostings);
		return p1;
		
	}
	
	public static PostingList combineMultipleLists(ArrayList<PostingList> p1)
	{
		if(p1==null || p1.size()==0)
			return null;
		while(p1.size()>1)
		{
			PostingList list1=p1.remove(0);
			PostingList list2=p1.remove(0);
			p1.add(combineLists(list1,list2));
		}
		return p1.get(0);
	}
}
