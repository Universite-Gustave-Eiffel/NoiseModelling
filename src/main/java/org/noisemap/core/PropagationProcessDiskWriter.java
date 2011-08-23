package org.noisemap.core;

/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

import java.util.Stack;

import org.gdms.data.values.Value;
import org.gdms.driver.DiskBufferDriver;
import org.gdms.driver.DriverException;
import org.gdms.data.values.ValueFactory;

public class PropagationProcessDiskWriter implements Runnable {
	private Thread thread;
	private boolean watchingStack = true;
	private Stack<PropagationResultRecord> toDriver;
	private DiskBufferDriver driver;

	public PropagationProcessDiskWriter(Stack<PropagationResultRecord> toDriver,
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
					PropagationResultRecord values = toDriver.pop();
					Value[] row = new Value[6];
					row[0] = ValueFactory.createValue(values.getTriangle());
                                        row[1] = ValueFactory.createValue(values.getV1());
                                        row[2] = ValueFactory.createValue(values.getV2());
                                        row[3] = ValueFactory.createValue(values.getV3());
                                        row[4] = ValueFactory.createValue(values.getCellId());
                                        row[5] = ValueFactory.createValue(values.getTriId());
					driver.addValues(row);
				}
			} catch (DriverException e) {
				e.printStackTrace();
				break;
			}

		}
	}

}
