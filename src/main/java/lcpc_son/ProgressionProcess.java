package lcpc_son;
/***********************************
 * ANR EvalPDU
 * Lcpc 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/




/**
 * ProgressionProcess is generated only by the progressionManager or another ProgressionProcess
 * @author Nicolas Fortin (IFSTTAR/EASE)
 *
 */
public class ProgressionProcess {
	protected ProgressionProcess parentProcess;
	private long subprocess_size;
	private double subprocess_done=0;
	public ProgressionProcess(ProgressionProcess parentProcess,long subprocess_size)
	{
		this.parentProcess=parentProcess;
		this.subprocess_size=subprocess_size;
	}
	/**
	 * 
	 * @return The progression on this process [0-1]
	 */
	public double getProcessProgression()
	{
		return subprocess_done/subprocess_size;
	}
	/**
	 * 
	 * @return The main progression value [0-1]
	 */
	public double getMainProgression()
	{
		ProgressionProcess prog=this;
		while(prog.parentProcess!=null)
			prog=prog.parentProcess;
		return prog.getProcessProgression();
	}
	protected void finalize() throws Throwable
	{
	  //do finalization here
	  if(this.parentProcess!=null)
	  {
		  //Complete remaining process
		  if(subprocess_done!=subprocess_size)
			  this.parentProcess.pushProgression(1-(subprocess_done/subprocess_size));
	  }
	  super.finalize();
	} 
	/**
	 * Get a new subprocess instance
	 * @param subprocess_size Sub Process estimated work item (sub-sub process count)
	 * @return
	 */
	public ProgressionProcess nextSubProcess(long subprocess_size)
	{
		return new ProgressionProcess(this,subprocess_size);
	}
	/**
	 * A subprocess computation has been done (same as call NextSubProcess then destroy the returned object)
	 */
	public synchronized void nextSubProcessEnd()
	{
		pushProgression(1.0);
	}
	/**
	 * Optional, When the current process is done call this method. Or let the garbage collector free the object
	 */
	public synchronized void processFinished()
	{
		  if(subprocess_done!=subprocess_size)
		  {
			  this.parentProcess.pushProgression(1-(subprocess_done/subprocess_size));
			  subprocess_done=1.;
		  }		
	}
	protected synchronized void pushProgression(double incProg)
	{
		if(subprocess_done+incProg<=subprocess_size)
		{
			subprocess_done+=incProg;
			if(parentProcess!=null)
				parentProcess.pushProgression((incProg/subprocess_size));
		}
	}
}
