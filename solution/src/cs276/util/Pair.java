package cs276.util;

import java.util.*;

/**
 * A generic-typed pair of objects.
 * 
 * @author Dan Klein
 */
public class Pair<F, S> {
	private F first;
	private S second;

	public F getFirst() {
		return first;
	}

	public S getSecond() {
		return second;
	}

	public void setFirst(F val) {
		this.first = val;
	}

	public void setSecond(S val) {
		this.second = val;
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Pair))
			return false;

		@SuppressWarnings("unchecked")
		final Pair<F, S> pair = (Pair<F, S>) o;

		if (first != null ? !first.equals(pair.first) : pair.first != null)
			return false;
		if (second != null ? !second.equals(pair.second) : pair.second != null)
			return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = (first != null ? first.hashCode() : 0);
		result = 29 * result + (second != null ? second.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "(" + getFirst() + ", " + getSecond() + ")";
	}

	public Pair(F first, S second) {
		this.first = first;
		this.second = second;
	}

	public static <E, F> Pair<E, F> make(E car, F cdr) {
		return new Pair<E, F>(car, cdr);
	}
	
	//
	//overriding the compareTo method
	public int compareTo(Pair<Integer,Integer> pr) 
	{
		if(((Integer)this.getFirst()).compareTo(((Integer)pr.getFirst()))!=0) 
		{
			return ((Integer)this.getFirst()).compareTo(((Integer)pr.getFirst()));
		}
		else 
		{
			return ((Integer)this.getSecond()).compareTo(((Integer)pr.getSecond()));
		}
	}
	
	// Overriding the compare method
		public int compare(Pair<Integer, Integer> pr1, Pair<Integer, Integer> pr2)
		{
			if((Integer)pr1.getFirst() - (Integer)pr2.getFirst() != 0)
			{
				return (Integer)pr1.getFirst() - (Integer)pr2.getFirst();
			}
			else
			{
				return (Integer)pr1.getSecond() - (Integer)pr2.getSecond();
			}
	
		}
}
