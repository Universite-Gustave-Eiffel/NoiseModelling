package org.noisemap.profile;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This class centralize informations on profiling
 * use the ProfileTask class profile tasks
 * This class is only used for retrieving stats through the getTasksStats method
 */
public  class TimeProfiler {
    private final static Map<String, StatInfo> stats = Collections.synchronizedMap(new  HashMap<String, StatInfo>());

    private TimeProfiler() {
    }
    
    public static synchronized void endTask(String name,Long taskLength) {
        StatInfo stat = stats.get(name);
        if(stat == null) {
            stat = new StatInfo(name);
            stats.put(name, stat);
        }
        stat.taskFinish(taskLength);
    }
    
    public static synchronized Collection<StatInfo> getTasksStats() {
        return stats.values();
    }

}

