package org.noisemap.core;

/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

import java.util.ArrayList;
import java.util.Stack;

import org.gdms.data.values.Value;
import org.gdms.driver.DiskBufferDriver;
import org.gdms.driver.DriverException;

public class PropagationProcessDiskWriter implements Runnable {
	private Thread thread;
	private boolean watchingStack = true;
	private Stack<ArrayList<Value>> toDriver;
	private DiskBufferDriver driver;

	public PropagationProcessDiskWriter(Stack<ArrayList<Value>> toDriver,
			DiskBufferDriver driver) {
		thread = new Thread(this);
		this.toDriver = toDriver;
		this.driver = driver;
	}

	public void start() {
		thread.start();
	}

	public void join() {
		try {
			thread.join();
		} catch (Exception e) {
			return;
		}
	}

	public boolean isRunning() {
		return thread.isAlive();
	}

	public void stopWatchingStack() {
		watchingStack = false;
	}

	@Override
	public void run() {
		while (watchingStack) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				break;
			}

			try {
				while (!toDriver.empty()) {
					ArrayList<Value> values = toDriver.pop();
					Value[] row = new Value[values.size()];
					for (int i = 0; i < row.length; i++) {
						row[i] = values.get(i);
					}
					driver.addValues(row);
				}
			} catch (DriverException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				break;
			}

		}
	}

}
