package org.noisemap.core;

/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

import java.util.ArrayList;
import java.util.Stack;

import org.gdms.data.values.Value;

/**
 * Way to store data computed by thread
 * 
 * @author fortin
 * 
 */
public class PropagationProcessOut {
	private Stack<ArrayList<Value>> toDriver;

	private long totalBuildingObstructionTest = 0;
	private long totalGridIndexQuery = 0;
	private long nb_couple_receiver_src = 0;
	private long nb_obstr_test = 0;
	private long nb_image_receiver = 0;
	private long nb_reflexion_path = 0;
	private long totalReflexionTime = 0;
	private long cellComputed = 0;

	public PropagationProcessOut(Stack<ArrayList<Value>> toDriver) {
		super();
		this.toDriver = toDriver;
	}

	public synchronized void addValues(Value... row) {
		ArrayList<Value> newArray = new ArrayList<Value>(row.length);
		for (int i = 0; i < row.length; i++) {
			newArray.add(row[i]);
		}
		toDriver.push(newArray);
	}

	public synchronized long getTotalBuildingObstructionTest() {
		return totalBuildingObstructionTest;
	}

	public synchronized long getTotalGridIndexQuery() {
		return totalGridIndexQuery;
	}

	public synchronized long getNb_couple_receiver_src() {
		return nb_couple_receiver_src;
	}

	public synchronized long getNb_obstr_test() {
		return nb_obstr_test;
	}
	public synchronized void appendReflexionPath(long added) {
		nb_reflexion_path+=added;
	}
	public synchronized void appendImageReceiver(long added) {
		nb_image_receiver+=added;
	}
	public synchronized long getNb_image_receiver() {
		return nb_image_receiver;
	}

	public synchronized long getNb_reflexion_path() {
		return nb_reflexion_path;
	}

	public synchronized void appendSourceCount(int srcCount) {
		nb_couple_receiver_src += srcCount;
	}

	public synchronized void appendGridIndexQueryTime(long queryTime) {
		totalGridIndexQuery += queryTime;
	}

	public synchronized void appendObstructionTestQueryTime(long queryTime) {
		totalBuildingObstructionTest += queryTime;
	}

	public synchronized void appendFreeFieldTestCount(long freeFieldTestCount) {
		nb_obstr_test += freeFieldTestCount;
	}

	public synchronized void appendTotalReflexionTime(long reflTime) {
		totalReflexionTime += reflTime;
	}

	public synchronized long getTotalReflexionTime() {
		return totalReflexionTime;
	}

	public synchronized void log(String str) {

	}

	/**
	 * Increment cell computed counter by 1
	 */
	public synchronized void appendCellComputed() {
		cellComputed += 1;
	}

	public synchronized long getCellComputed() {
		return cellComputed;
	}
}
