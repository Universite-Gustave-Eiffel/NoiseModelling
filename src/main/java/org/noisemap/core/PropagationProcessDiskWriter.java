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
import org.gdms.driver.DataSet;

public class PropagationProcessDiskWriter implements Runnable {
	private Thread thread;
	private boolean watchingStack = true;
	private Stack<PropagationResultTriRecord> triToDriver;
	private Stack<PropagationResultPtRecord> ptToDriver;
	private DiskBufferDriver driver;
        private DataSet sdsReceivers;

        public PropagationProcessDiskWriter(Stack<PropagationResultTriRecord> triToDriver, Stack<PropagationResultPtRecord> ptToDriver, DiskBufferDriver driver, DataSet sdsReceivers) {
            thread = new Thread(this);
            this.triToDriver = triToDriver;
            this.ptToDriver = ptToDriver;
            this.driver = driver;
            this.sdsReceivers = sdsReceivers;
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
                int fieldCount=0;
                try {
                    if(sdsReceivers!=null) {
                        fieldCount = sdsReceivers.getMetadata().getFieldCount();
                    }
                    while (watchingStack) {
                        try {
                                Thread.sleep(10);
                        } catch (InterruptedException e) {
                                break;
                        }
                        if(triToDriver!=null) {
                            while (!triToDriver.empty()) {
                                    PropagationResultTriRecord values = triToDriver.pop();
                                    Value[] row = new Value[6];
                                    row[0] = ValueFactory.createValue(values.getTriangle());
                                    row[1] = ValueFactory.createValue(values.getV1());
                                    row[2] = ValueFactory.createValue(values.getV2());
                                    row[3] = ValueFactory.createValue(values.getV3());
                                    row[4] = ValueFactory.createValue(values.getCellId());
                                    row[5] = ValueFactory.createValue(values.getTriId());
                                    driver.addValues(row);
                            }
                        }else if(ptToDriver!=null && sdsReceivers!=null) {
                            while (!ptToDriver.empty()) {
                                PropagationResultPtRecord values = ptToDriver.pop();
                                final Value[] newValues = new Value[fieldCount + 2];
                                final Value[] receiverValues=sdsReceivers.getRow(values.getReceiverRecordRow());
                                System.arraycopy(receiverValues, 0, newValues, 0, receiverValues.length);
                                //Add dB value and cellId
                                newValues[fieldCount] = ValueFactory.createValue(values.getReceiverLvl());
                                newValues[fieldCount+1] = ValueFactory.createValue(values.getCellId());
                                driver.addValues(newValues);
                            }
                        }
                    }
                } catch (DriverException e) {
                        e.printStackTrace();
                }
	}

}
