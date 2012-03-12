/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.noisemap.profile;

import junit.framework.TestCase;

public class TestProfileTask extends TestCase {

	public void testProfiling () throws InterruptedException {
            ProfileTask mainTask = new ProfileTask("mainTask");
            for(int i=0;i<100;i++) {
                ProfileTask subTask = new ProfileTask("subTask");
                Thread.sleep(20);
                subTask.end();
            }
            mainTask.end();
            
            
            //Print profiling info
            System.out.println("TestProfileTask______________________________");
            for(StatInfo info : TimeProfiler.getTasksStats()) {
                System.out.println(info.getName()+" Task min: "+info.getMin() / 1e6+" ms max: "+info.getMax() / 1e6+" ms avg: "+info.getAverage() / 1e6+" ms");
            }
	}
}
