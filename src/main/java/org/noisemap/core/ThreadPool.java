package org.noisemap.core;

/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPool extends ThreadPoolExecutor {

	/**
	 * Default constructor. Set CorePoolSize size to 32 Set Maximum pool size to
	 * 256 Set Keep Alive Time to 60 seconds
	 */
	public ThreadPool() {
		super(32, 256, 60, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());
	}

	/**
	 * Default constructor. Set CorePoolSize size to 32 Set Maximum pool size to
	 * 256 Set Keep Alive Time to 60 seconds
	 */
	public ThreadPool(int queueSize) {
		super(32, 256, 60, TimeUnit.SECONDS,
				queueSize < 0 ? new LinkedBlockingQueue<Runnable>()
						: (queueSize == 0 ? new SynchronousQueue<Runnable>()
								: new ArrayBlockingQueue<Runnable>(queueSize)));
	}

	/**
	 * Size constructor.
	 * 
	 */
	public ThreadPool(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit,
				new LinkedBlockingQueue<Runnable>());
	}

	/**
	 * Size constructor.
	 * 
	 */
	public ThreadPool(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	}

	/**
	 * Size constructor.
	 * 
	 */
	public ThreadPool(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
				handler);
	}

	/**
	 * Size constructor.
	 * 
	 */
	public ThreadPool(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
			RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
				threadFactory, handler);
	}

	/**
	 * Size constructor.
	 * 
	 */
	public ThreadPool(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
				threadFactory);
	}

	/**
	 * @return True if poolSize is inferior of maximum pool size
	 */
	boolean hasAvaibleQueueSlot() {
		return this.getQueue().size() + this.getActiveCount() < this
				.getMaximumPoolSize();
	}

	/**
	 * 
	 * @return Remaining threads Running and queued
	 */
	public int getRemainingTasks() {
		return this.getQueue().size() + this.getActiveCount();
	}

	/**
	 * Wait for free queue slot if poolSize is superior or equal of maximum pool
	 * size then executes the given task sometime in the future. The task may
	 * execute in a new thread or in an existing pooled thread. If the task
	 * cannot be submitted for execution, either because this executor has been
	 * shutdown or because its capacity has been reached, the task is handled by
	 * the current RejectedExecutionHandler.
	 * 
	 * @param command
	 */
	public void executeBlocking(Runnable command) {
		while (this.getQueue().size() + this.getActiveCount() >= this
				.getMaximumPoolSize()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				return; // do not execute
			}
		}
		super.execute(command);
	}
}
