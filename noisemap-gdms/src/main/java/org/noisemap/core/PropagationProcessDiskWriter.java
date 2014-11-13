/**
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact on urban mobility plans. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 *
 * This version is developed at French IRSTV Institute and at IFSTTAR
 * (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 *
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
 *
 * Copyright (C) 2011 IFSTTAR
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
 *
 * Noisemap is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Noisemap. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.noisemap.core;

import java.util.Stack;

import org.gdms.data.values.Value;
import org.gdms.driver.DiskBufferDriver;
import org.gdms.driver.DriverException;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DataSet;
import org.orbisgis.noisemap.core.PropagationResultPtRecord;
import org.orbisgis.noisemap.core.PropagationResultTriRecord;

/**
 * 
 * @author Nicolas Fortin
 */
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
