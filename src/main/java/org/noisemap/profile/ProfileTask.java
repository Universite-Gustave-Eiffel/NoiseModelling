package org.noisemap.profile;

/**
 * When a task must be profiled, instanciate then call the start method 
 * if this is not done on instanciation.Finally when the task has been done,
 * call the end method.
 * Results can be seend thanks to the TimeProfiler static class
 */
public class ProfileTask {
    private Long startTask;
    private String name;
    public ProfileTask(String name) {
        this.name=name;
        startTask=System.nanoTime();
    }
    /**
     * If the 
     * @return 
     */
    public ProfileTask start() {
        startTask = System.nanoTime();
        return this;
    }
    public void end() {
        if(startTask!=null) {
            TimeProfiler.endTask(name,System.nanoTime() - startTask);
        }
    }
}