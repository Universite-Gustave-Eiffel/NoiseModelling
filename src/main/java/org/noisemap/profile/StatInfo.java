package org.noisemap.profile;

/**
 * Merged Stat info for a task category
 */
public class StatInfo {
    private String name; //task group name
    private Long max = 0L; //max processing time
    private Long min = Long.MAX_VALUE; //min processing time
    private Long sum = 0L; //Sum of all processing
    private Long count = 0L; //Number of tasks
    public StatInfo(String name) {
        this.name=name;
    }

    public String getName() {
        return name;
    }
    
    public Long getMax() {
        return max;
    }
    public void taskFinish(Long time) {
        max = Math.max(max, time);
        min = Math.min(min,time);
        count++;
        sum+=time;
    }
    public Long getMin() {
        return min;
    }
    public Long getAverage() {
        return (sum / count);
    }
}