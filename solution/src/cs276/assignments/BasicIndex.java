package cs276.assignments;

import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.nio.ByteBuffer;
import java.io.IOException;


public class BasicIndex implements BaseIndex 
{
	private static final int INT_SIZE = 4;

	@Override
	public PostingList readPosting(FileChannel fc) 
	{
		
		try {
				if(fc.position() >= fc.size()) return null;
				ByteBuffer buffer = ByteBuffer.allocate(INT_SIZE * 2);
				int numOfBytesRead;
				numOfBytesRead = fc.read(buffer);
				if (numOfBytesRead == -1) return null;
				
				int termID = buffer.getInt(0);
				int numPostings = buffer.getInt(4);
				
				ArrayList<Integer> postings = new ArrayList<Integer>(numPostings);
				ByteBuffer docs = ByteBuffer.allocate(numPostings*4);
				fc.read(docs);
				
				for(int i=0;i<numPostings;i++)
				{
					postings.add(new Integer(docs.getInt(docs.getInt(i*4))));
				}
				
				PostingList p1 = new PostingList(termID,postings);
				return p1;
				
		}
		
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	
	}

	@Override
	public void writePosting(FileChannel fc, PostingList p)
	{
		Integer termID = p.getTermId();
		List<Integer> postings=p.getList();
		int numPostings=postings.size();
		
		try {
				ByteBuffer buffer = ByteBuffer.allocate(INT_SIZE * 2+numPostings*4);
				buffer.clear();
				buffer.putInt(termID);
				buffer.putInt(numPostings);
				
				for(int i=0;i<numPostings;i++)
				{
					buffer.putInt(postings.get(i));
				}
				buffer.flip();
				fc.write(buffer);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
}
