package cs276.util;

import java.util.*;

public class Freq implements Comparator<Freq>, Comparable<Freq>
{
	private Integer termID;
	private Integer freq;

	public Integer getTermId() 
	{
		return termID;
	}

	public Integer getFreq() 
	{
		return freq;
	}

	public String toString() 
	{
		return "(" + getTermId() + ": " + getFreq() + ")";
	}

	public Freq(Integer termID, Integer freq) 
	{
		this.termID = termID;
		this.freq = freq;
	}

	
	// Overriding the compareTo method
	public int compareTo(Freq fr)
	{
		return (this.getFreq()).compareTo(fr.getFreq());
	}

	// Overriding the compare method
	public int compare(Freq fr1, Freq fr2)
	{
		return fr1.getFreq() - fr2.getFreq();
	}
}
