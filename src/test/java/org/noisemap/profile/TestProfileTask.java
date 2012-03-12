/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.noisemap.profile;

import junit.framework.TestCase;

public class TestProfileTask extends TestCase {
        private final static Long MAIN_EPSILON = 50L; //millisec epsilon
        private final static Long TASK_EPSILON = 5L; //millisec epsilon
        private void assertTime(long timeNano,long targetTimeMilli,long epsilon) {
            assertTrue((timeNano / 1e6) > targetTimeMilli - epsilon && (timeNano / 1e6) < targetTimeMilli + epsilon);
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
                    assertTime(info.getAverage(), ni * taskSleep,MAIN_EPSILON);
                    hasMainStat=true;
                } else if(info.getName().equals("subTask")) {
                    assertTime(info.getAverage(), taskSleep,TASK_EPSILON);
                    hasTaskStat=true;
                }
            }
            //All stats must be avaible
            assertTrue(hasMainStat && hasTaskStat);
	}
}
