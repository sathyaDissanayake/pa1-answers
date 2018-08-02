package cs276.assignments;

import cs276.util.Pair;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class Index {
	

	// Term id -> (position in index file, doc frequency) dictionary
	private static Map<Integer, Pair<Long, Integer>> postingDict 
		= new HashMap<Integer, Pair<Long, Integer>>();
	// Doc name -> doc id dictionary
	private static Map<String, Integer> docDict
		= new HashMap<String, Integer>();
	// Term -> term id dictionary
	private static Map<String, Integer> termDict
		= new HashMap<String, Integer>();
	// Block queue
	private static LinkedList<File> blockQueue
		= new LinkedList<File>();

	// Total file counter
	private static int totalFileCount = 0;
	// Document counter
	private static int docIdCounter = 0;
	// Term counter
	private static int wordIdCounter = 0;
	// Index
	private static BaseIndex index = null;


	private static void sanityCheck(File indexFile) throws Throwable 
	{
		
		RandomAccessFile rf = new RandomAccessFile(indexFile, "r");
		FileChannel fc = rf.getChannel();
		PostingList pl = index.readPosting(fc);
		
		while(pl != null)
		{
			System.out.println(pl);
			pl = index.readPosting(fc);
		}
	}

	private static File compressVB(File indexFile, File outputFile) throws Throwable 
	{
		RandomAccessFile rf = new RandomAccessFile(indexFile, "r");
		RandomAccessFile raOut = new RandomAccessFile(outputFile, "rw");
		
		FileChannel fc = rf.getChannel();
		FileChannel fcOut = raOut.getChannel();
		BaseIndex vbIndex = new VBIndex();
		
		while(true)
		{
			PostingList posting = index.readPosting(fc);
			
			if(posting == null)
			{
				rf.close();
				raOut.close();
				indexFile.delete();
				return outputFile;
			}
			
			List<Integer> list = posting.getList();
			Pair<Long,Integer> posFreq = new Pair<Long,Integer>(new Long(fcOut.position()), new Integer(list.size()));
			
			postingDict.put(new Integer(posting.getTermId()),posFreq);
			vbIndex.writePosting(fcOut,posting);
		}
	}
	
	/* 
	 * Write a posting list to the file 
	 * You should record the file position of this posting list
	 * so that you can read it back during retrieval
	 * 
	 * 
	 */
	private static void writePosting(FileChannel fc, PostingList posting) throws Throwable 
	{
		
		List<Integer> list = posting.getList();
		Pair<Long,Integer> posFreq = new Pair<Long,Integer>(new Long(fc.position()), new Integer(list.size()));
		
		postingDict.put(new Integer(posting.getTermId()),posFreq);
		
		index.writePosting(fc,posting);
	}

	private static void merge(ArrayList<FileChannel> kFileChannels, FileChannel comb) throws IOException
	{
		//Read the first postings list from each FileChannel
		int kSize = kFileChannels.size();
		ArrayList<PostingList> kPostings = new ArrayList<PostingList>(kSize);
		
		for(int i = 0; i < kSize; i++)
		{
			PostingList pl = index.readPosting(kFileChannels.get(i));
			kPostings.add(pl);
		}
		
		ArrayList<Integer> shortestIds = new ArrayList<Integer>(kSize);
		ArrayList<Pair<Integer,Integer>> validIds = new ArrayList<Pair<Integer,Integer>>(kSize);
		
		while(true)
		{
			//All null, all lists are exhausted.
			boolean done = true;
			
			for(int i = 0; i < kSize; i++)
			{
				PostingList pl = kPostings.get(i);
				
				if(pl != null)
				{
					done = false;
					validIds.add(new Pair<Integer,Integer> (new Integer(pl.getTermId()),new Integer(i)));
				}
			}
			
			if(done) return;
			
			//Order the terms from least to greatest
			Collections.sort(validIds);
			
			//Take the shorter termID, if equal then combine
			Pair<Integer,Integer> sh = validIds.get(0);
			int shortestTerm = (Integer)sh.getFirst();
			shortestIds.add((Integer)sh.getSecond());
			int validSize = validIds.size();
			
			for(int i = 1; i < validSize; i++)
			{
				Pair<Integer,Integer> next = validIds.get(i);
				int nextTerm = (Integer)next.getFirst();
				
				if(nextTerm == shortestTerm)
				{
					//The terms are equal, merge 
					shortestIds.add((Integer)next.getSecond());
				}
				else
				{
					break;
				}
			}
			
			int shortSize = shortestIds.size();
			
			if(shortSize == 1)
			{
				//No terms are equal, write and advance the shortest term
				int shortId = shortestIds.get(0);
				writePosting(comb,kPostings.get(shortId));
				PostingList advance = index.readPosting(kFileChannels.get(shortId));
				kPostings.set(shortId,advance);
			}
			else
			{
				//Multiple terms are equal, merge them
				ArrayList<PostingList> shortList = new ArrayList<PostingList>(shortSize);
				
				for(int i = 0; i < shortSize; i++)
				{
					int shortestIndex = shortestIds.get(i);
					shortList.add(kPostings.get(shortestIndex));
					PostingList advance = index.readPosting(kFileChannels.get(shortestIndex));
					kPostings.set(shortestIndex,advance);
				}
				
				PostingList combinedPosting = PostingList.combineMultipleLists(shortList);
				writePosting(comb,combinedPosting);
			}
			shortestIds.clear();
			validIds.clear();
		}
	}

	private static void mapper(TreeSet<Pair<Integer,Integer>> pairs, String term, int docID)
	{
		if(!termDict.containsKey(term))
		{
			wordIdCounter = wordIdCounter + 1;
			termDict.put(term,new Integer(wordIdCounter));
		}
		
		Pair<Integer,Integer> pair = new Pair<Integer,Integer>(termDict.get(term),new Integer(docID));
		pairs.add(pair);
		
	}

	private static void reducer(TreeSet<Pair<Integer,Integer>> pairs,FileChannel fc) throws Throwable
	{
		ArrayList<Pair<Integer,Integer>> equalTerms = new ArrayList<Pair<Integer,Integer>>();
		
		for (Pair<Integer,Integer> pr : pairs) 
		{
			if(equalTerms.size() == 0)
			{
				equalTerms.add(pr);
			}else
			{
				Integer etTerm = (equalTerms.get(0)).getFirst();
				Integer prTerm = pr.getFirst();
				if(!etTerm.equals(prTerm))
				{
					//Create a list
					createPosting(equalTerms,fc);
					equalTerms.clear();
					equalTerms.add(pr);
				}
				else
				{
					//Continue the postings list
					equalTerms.add(pr);
				}
			}
		}
		if(equalTerms.size() != 0)
		{
			//Create a list
			createPosting(equalTerms,fc);
		}
	}

	private static void createPosting(ArrayList<Pair<Integer,Integer>> pairs,FileChannel fc) throws Throwable
	{
		ArrayList<Integer> intPostings = new ArrayList<Integer>(pairs.size());
		
		for(int i = 0; i < pairs.size(); i++)
		{
			Integer doc = (Integer)(pairs.get(i)).getSecond();
			intPostings.add(doc);
		}
		
		int termID = (Integer)(pairs.get(0)).getFirst();
		PostingList pl = new PostingList(termID,intPostings);
		index.writePosting(fc,pl);
		
	}

	public static void main(String[] args) throws Throwable 
	{
		/* Show timing or not */
		boolean verbose = false;//true;
		boolean sanity = false;//true;
		
		/* Parse command line */
		if (args.length != 3)
		{
			System.err
					.println("Usage: java Index [Basic|VB|Gamma] data_dir output_dir");
			return;
		}

		/* Get index */
		//String className = "cs276.assignments." + args[0] + "Index";
		String className = "cs276.assignments.BasicIndex";
		try 
		{
			Class<?> indexClass = Class.forName(className);
			index = (BaseIndex) indexClass.newInstance();
		} 
		catch (Exception e) 
		{
			System.err
					.println("Index method must be \"Basic\", \"VB\", or \"Gamma\"");
			throw new RuntimeException(e);
		}

		/* Get root directory */
		String root = args[1];
		File rootdir = new File(root);
		if (!rootdir.exists() || !rootdir.isDirectory()) 
		{
			System.err.println("Invalid data directory: " + root);
			return;
		}

		/* Get output directory */
		String output = args[2];
		File outdir = new File(output);
		if (outdir.exists() && !outdir.isDirectory()) 
		{
			System.err.println("Invalid output directory: " + output);
			return;
		}

		if (!outdir.exists()) 
		{
			if (!outdir.mkdirs()) 
			{
				System.err.println("Create output directory failure");
				return;
			}
		}

		/* BSBI indexing algorithm */
		File[] dirlist = rootdir.listFiles();

		/* For each block */
		for (File block : dirlist) 
		{
			File blockFile = new File(output, block.getName());
			blockQueue.add(blockFile);

			File blockDir = new File(root, block.getName());
			File[] filelist = blockDir.listFiles();
			
			//int avgTokensPerDoc = 100;
			//int avgDocsPerBlock = 500;
			//int avgPairs = avgTokensPerDoc*avgDocsPerBlock;
			int avgUniquePerBlock = 700;
			int avgPairs = avgUniquePerBlock;
			TreeSet<Pair<Integer,Integer>> pairs = new TreeSet<Pair<Integer,Integer>>();

			/* For each file */
			long begin = System.currentTimeMillis();
			
			for (File file : filelist) 
			{
				++totalFileCount;
				String fileName = block.getName() + "/" + file.getName();
				docIdCounter = docIdCounter + 1;
				docDict.put(fileName, docIdCounter);
				
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line;
				while ((line = reader.readLine()) != null) {
					String[] tokens = line.trim().split("\\s+");
					for (String token : tokens)
					{
						mapper(pairs,token,docIdCounter);
					}
				}
				reader.close();
			}
			
			long end = System.currentTimeMillis();
			if(verbose)
			{
				System.out.println("Mapper phase took " + ((end - begin)/1000) + " seconds");
			}

			/* Sort and output */
			if (!blockFile.createNewFile()) 
			{
				System.err.println("Create new block failure.");
				return;
			}
			
			RandomAccessFile bfc = new RandomAccessFile(blockFile, "rw");
			/* My code here.
			 * This is where we sort the termID docID pairs, create the postings
			 * and block index and write the index to file. The method will call
			 * writePosting above for sure.
			 */
			
			FileChannel fc = bfc.getChannel();
			begin = System.currentTimeMillis();
			reducer(pairs,fc);
			end = System.currentTimeMillis();
			
			if(verbose)
			{
				System.out.println("Reducer phase took " + ((end - begin)/1000) + " seconds");
			}
			bfc.close();
		}

		/* Required: output total number of files. */
		System.out.println(totalFileCount);

		/* Merge blocks */
		long begin = System.currentTimeMillis();
		while (true) 
		{
			if (blockQueue.size() <= 1)
				break;
			int size = blockQueue.size();
			ArrayList<File> fileList = new ArrayList<File>(size);
			String combName = "";
			
			for(int i = 0; i < size; i++)
			{
				File fl = blockQueue.removeFirst();
				fileList.add(fl);
				
				if(i != size - 1)
				{
					combName = combName + fl.getName() + "+";
				}
				else
				{
					combName = combName + fl.getName();
				}
			}
			File combfile = new File(output,combName);
			
			if (!combfile.createNewFile()) 
			{
				System.err.println("Create new block failure.");
				return;
			}
			
			ArrayList<RandomAccessFile> raList = new ArrayList<RandomAccessFile>(size);
			ArrayList<FileChannel> kInputs = new ArrayList<FileChannel>(size);
			
			for(int i = 0; i < size; i++)
			{
				RandomAccessFile ra = new RandomAccessFile(fileList.get(i),"r");
				raList.add(ra);
				kInputs.add(ra.getChannel());
			}
			RandomAccessFile mf = new RandomAccessFile(combfile, "rw");
			 
			/* My code here
			 * This is where we merge all of the blocks with merge sort for
			 * already sorted lists. We also track the freq of terms here
			 */
			merge(kInputs,mf.getChannel());
			
			for(int i = 0; i < size; i++)
			{
				(raList.get(i)).close();
				(fileList.get(i)).delete();
			}
			mf.close();
			blockQueue.add(combfile);
		}
		long end = System.currentTimeMillis();
		
		if(verbose)
		{
			System.out.println("Merge phase took " + ((end - begin)/1000) + " seconds");
		}

		/* Dump constructed index back into file system */
		File indexFile = blockQueue.removeFirst();
		if(sanity)
		{
			sanityCheck(indexFile);
		}
		if(args[0].equals("VB"))
		{
			indexFile = compressVB(indexFile,new File(output, "VBCompressionStep.index"));
		}
		indexFile.renameTo(new File(output, "corpus.index"));
		

		BufferedWriter termWriter = new BufferedWriter(new FileWriter(new File(output, "term.dict")));
		
		for (String term : termDict.keySet()) 
		{
			termWriter.write(term + "\t" + termDict.get(term) + "\n");
		}
		termWriter.close();

		
		BufferedWriter docWriter = new BufferedWriter(new FileWriter(new File(output, "doc.dict")));
		for (String doc : docDict.keySet()) 
		{
			docWriter.write(doc + "\t" + docDict.get(doc) + "\n");
		}
		docWriter.close();

		
		BufferedWriter postWriter = new BufferedWriter(new FileWriter(new File(output, "posting.dict")));
		for (Integer termId : postingDict.keySet())
		{
			postWriter.write(termId + "\t" + postingDict.get(termId).getFirst()
					+ "\t" + postingDict.get(termId).getSecond() + "\n");
		}
		postWriter.close();
	}

}
