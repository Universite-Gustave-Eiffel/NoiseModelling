/**
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact on urban mobility plans. This model is
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
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
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
package org.orbisgis.noisemap.core;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 
 * @author Nicolas Fortin
 */
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
	public boolean hasAvaibleQueueSlot() {
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
