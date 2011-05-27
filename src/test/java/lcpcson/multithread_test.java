package lcpcson;
import lcpcson.ThreadPool;
import java.util.concurrent.TimeUnit;

public class multithread_test {
	public static void main(String[] args) throws InterruptedException 
	{
		Runtime runtime = Runtime.getRuntime();
		ThreadPool ThreadsManager= new ThreadPool(runtime.availableProcessors()+1,runtime.availableProcessors()+1,Long.MAX_VALUE,TimeUnit.SECONDS);
		for(int i=1;i<=30;i++)
		{
			ActiviteEnParallele newActivity = new ActiviteEnParallele(i);
			ThreadsManager.executeBlocking(newActivity);
			System.out.println("Thread "+i+" submited !");
			System.out.println("There are "+ThreadsManager.getQueue().size()+" queue threads of "+ThreadsManager.getMaximumPoolSize()+" maximum pool thread.");
			System.out.println("There are "+ThreadsManager.getPoolSize()+" pool threads");
		}

		System.out.println("Wait for termination of the lasts propagation process"); 
		while(ThreadsManager.getRemainingTasks()>0)
		{
			Thread.sleep(100);
		}
		System.out.println("End of program"); 
	}
}