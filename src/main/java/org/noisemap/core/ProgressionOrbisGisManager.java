package org.noisemap.core;

/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

import org.orbisgis.progress.ProgressMonitor;
import java.util.Stack;
import org.apache.commons.math.stat.regression.SimpleRegression;

/**
 * ProgressionManager class aims to easily manage the update of process
 * progression information It is used in a hierarchical way, allowing
 * multi-threading algorithm
 * If it is not attached to a ProgressMonitor instance then It will print
 * the progression with an estimation of remaining time.
 */
public class ProgressionOrbisGisManager implements Runnable {
    private Thread thread;
    private ProgressionProcess rootProcess;
    private ProgressMonitor monitor;
    private long updateInterval = 1000;
    private boolean enabled = true;
    private Stack<Double> progressionHistoryValue=new Stack<Double>();
    private Stack<Long> progressionHistoryTime=new Stack<Long>();
    private final static double historyTimeStep=10*1000;
    private long lastPushedProgress=0;
    private double lastEstimation=0;
    private static String getHumanTime(long millisec)  {
        long day=millisec/(1000*3600*24);
        long millirest=millisec%(1000*3600*24);
        long hour=millirest/(1000*3600);
        millirest %= (1000 * 3600);
        long minute=millirest/(1000*60);
        millirest %= (1000 * 60);
        long sec=millirest/1000;
        return day+"d "+hour+"h "+minute+"m "+sec+"s";
    }
    public ProgressionOrbisGisManager(long taskCount, ProgressMonitor monitor) {
        thread = new Thread(this);
        this.rootProcess = new ProgressionProcess(null, taskCount);
        this.monitor = monitor;
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

    public double getUpdateInterval() {
            return updateInterval;
    }

    /**
     * Set the progress manager update interval in ms
     *
     * @param updateInterval
     *            Time in ms
     */
    public void setUpdateInterval(long updateInterval) {
            this.updateInterval = updateInterval;
    }

    /**
     * Get a new subprocess instance
     *
     * @param subprocess_size
     *            Sub Process estimated work item (sub-sub process count)
     * @return
     */
    public ProgressionProcess nextSubProcess(long subprocess_size) {
            return rootProcess.nextSubProcess(subprocess_size);
    }

    /**
     * A subprocess computation has been done (same as call NextSubProcess then
     * destroy the returned object)
     */
    public synchronized void nextSubProcessEnd() {
            rootProcess.nextSubProcessEnd();
    }

    /**
     *
     * @return The main progression value [0-1]
     */
    public double getMainProgression() {
            return rootProcess.getProcessProgression();
    }

    /**
     * Stop the update of IProgressMonitor
     */
    public void stop() {
            enabled = false;
    }

    @Override
    public void run() {
        while (enabled) {
            if(monitor!=null) {
                monitor.progressTo((int) (getMainProgression() * 100));
                if (monitor.isCancelled()) {
                        break;
                }
            }else{
                double progression=(getMainProgression() * 100);
               
                if(progressionHistoryTime.isEmpty()) {
                    progressionHistoryTime.push(System.currentTimeMillis());
                    progressionHistoryValue.push(progression);
                }else{
                    if(lastPushedProgress<System.currentTimeMillis()-historyTimeStep) {
                        //reg.addData(progression,System.currentTimeMillis());
                        lastPushedProgress=System.currentTimeMillis();
                        if((int)(progression-progressionHistoryValue.lastElement())>=1) {
                            progressionHistoryTime.push(System.currentTimeMillis());
                            progressionHistoryValue.push(progression);
                        }
                        //Estimate end of computation
                        SimpleRegression reg= new SimpleRegression();
                        double prog[][]=new double[progressionHistoryTime.size()+1][2];
                        for(int t=0;t<progressionHistoryTime.size();t++) {
                            prog[t][0]=progressionHistoryValue.get(t);
                            prog[t][1]=progressionHistoryTime.get(t);
                        }
                        prog[progressionHistoryTime.size()][0]=progression;
                        prog[progressionHistoryTime.size()][1]=System.currentTimeMillis();
                        reg.addData(prog);
                        lastEstimation=reg.predict(100);
                    }
                }
                 
                //Round
                progression=(((int)(progression * 100000))/100000.);
                if(lastEstimation>0) {
                    System.out.println(progression+" % remaining "+getHumanTime((long)lastEstimation-System.currentTimeMillis()));
                } else {
                    System.out.println(progression+" %");
                }
               
            }
            try {
                    Thread.sleep(updateInterval);
            } catch (InterruptedException e) {
                    break;
            }
        }
    }
}
