package lcpc_son;

import java.util.ArrayList;
import java.util.Stack;

import org.gdms.data.values.Value;

/**
 * Way to store data computed by thread
 * @author fortin
 *
 */
public class PropagationProcessOut {
	private Stack<ArrayList<Value>> toDriver;

	private long totalBuildingObstructionTest=0;
	private long totalGridIndexQuery=0;
	private long nb_couple_receiver_src=0;
	private long nb_obstr_test=0;
	
	public PropagationProcessOut(Stack<ArrayList<Value>> toDriver) {
		super();
		this.toDriver = toDriver;
	}
	public void addValues(Value... row)
	{
		ArrayList<Value> newArray=new ArrayList<Value>(row.length);
		for(int i=0;i<row.length;i++)
		{
			newArray.add(row[i]);
		}
		toDriver.push(newArray);
	}
	public long getTotalBuildingObstructionTest() {
		return totalBuildingObstructionTest;
	}
	public long getTotalGridIndexQuery() {
		return totalGridIndexQuery;
	}
	public long getNb_couple_receiver_src() {
		return nb_couple_receiver_src;
	}
	public long getNb_obstr_test() {
		return nb_obstr_test;
	}
	public synchronized void appendSourceCount(int srcCount)
	{
		nb_couple_receiver_src+=srcCount;
	}
	public synchronized void appendGridIndexQueryTime(long queryTime)
	{
		totalGridIndexQuery+=queryTime;
	}
	public synchronized void appendObstructionTestQueryTime(long queryTime)
	{
		totalBuildingObstructionTest+=queryTime;
	}
	public synchronized void appendFreeFieldTestCount(long freeFieldTestCount)
	{
		nb_obstr_test+=freeFieldTestCount;	
	}
	public synchronized void log(String str)
	{
		
	}
}
