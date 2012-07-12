/**
 * NoiseMap is a scientific computation plugin for OrbisGIS to quickly evaluate the
 * noise impact on European action plans and urban mobility plans. This model is
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
 * Copyright (C) 2011-1012 IRSTV (FR CNRS 2488)
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
            
            System.out.println("Profiling took : "+ (TimeProfiler.getTaskStats("profilerTask").getMax() /(ni*1e6))+ " ms per task");
        }
	public void testProfiling () throws InterruptedException {
            long taskSleep=2;
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
