package lcpc_son;

import org.orbisgis.progress.IProgressMonitor;

/**
 * ProgressionManager class aims to easily manage the update of process progression information
 * It is used in a hierarchical way, allowing multi-threading algorithm
 * @author fortin
 *
 */
public class ProgressionOrbisGisManager implements Runnable {
	private Thread thread;
	private ProgressionProcess rootProcess;
	private IProgressMonitor monitor;
	private long updateInterval=1000;
	private boolean enabled=true;
	public ProgressionOrbisGisManager(long taskCount,IProgressMonitor monitor)
	{
		thread = new Thread(this);
		this.rootProcess=new ProgressionProcess(null, taskCount);
		this.monitor=monitor;
	}
	public void start()
	{
		thread.start();
	}
	public void join()
	{
	   try {
	      thread.join();
	    } catch (Exception e) {
	    	return;
	    }
	}
	public boolean isRunning()
	{
		return thread.isAlive();
	}
	public double getUpdateInterval() {
		return updateInterval;
	}
	/**
	 * Set the progress manager update interval in ms
	 * @param updateInterval Time in ms
	 */
	public void setUpdateInterval(long updateInterval) {
		this.updateInterval = updateInterval;
	}
	/**
	 * Get a new subprocess instance
	 * @param subprocess_size Sub Process estimated work item (sub-sub process count)
	 * @return
	 */
	public ProgressionProcess NextSubProcess(long subprocess_size)
	{
		return rootProcess.NextSubProcess(subprocess_size);
	}
	/**
	 * A subprocess computation has been done (same as call NextSubProcess then destroy the returned object)
	 */
	public synchronized void NextSubProcessEnd()
	{
		rootProcess.NextSubProcessEnd();
	}
	/**
	 * 
	 * @return The main progression value [0-1]
	 */
	public double GetMainProgression()
	{
		return rootProcess.GetProcessProgression();
	}
	/**
	 * Stop the update of IProgressMonitor
	 */
	public void stop()
	{
		enabled=false;
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(enabled)
		{
			//System.out.println("Progression :"+GetMainProgression());
			monitor.progressTo((int)(GetMainProgression()*100));
			if(monitor.isCancelled())
				break;
			try {
				Thread.sleep(updateInterval);
			} catch (InterruptedException e) {
				break;
			}
		}
	}
}
