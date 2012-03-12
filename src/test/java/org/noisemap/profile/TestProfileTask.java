/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.noisemap.profile;

import junit.framework.TestCase;

public class TestProfileTask extends TestCase {
        public void testProfileTheProfiler() {
            int ni=10000;
            ProfileTask profilerTask = new ProfileTask("profilerTask");
            for(int i=0;i<ni;i++) {
                ProfileTask subTask = new ProfileTask("subProfilerTask");
                subTask.end();
            }
            profilerTask.end();
            
            System.out.println("Profiling took : "+ (TimeProfiler.getTaskStats("profilerTask").getMax() / ni)+ " nanosec per task");
        }
	public void testProfiling () throws InterruptedException {
            long taskSleep=20;
            int ni=100;
            ProfileTask mainTask = new ProfileTask("mainTask");
            ProfileTask subTask = new ProfileTask("subTask");
            for(int i=0;i<ni;i++) {
                subTask.start(); //You can instanciate here or do a new start
                Thread.sleep(taskSleep);
                subTask.end();
            }
            mainTask.end();
            
            
            //Print profiling info
            boolean hasMainStat=false;
            boolean hasTaskStat=false;
            System.out.println("TestProfileTask______________________________");
            for(StatInfo info : TimeProfiler.getTasksStats()) {
                System.out.println(info.getName()+" Task min: "+info.getMin() / 1e6+" ms max: "+info.getMax() / 1e6+" ms avg: "+info.getAverage() / 1e6+" ms");
                if(info.getName().equals("mainTask")) {
                    hasMainStat=true;
                } else if(info.getName().equals("subTask")) {
                    hasTaskStat=true;
                }
            }
            //All stats must be avaible
            assertTrue(hasMainStat && hasTaskStat);
	}
}
