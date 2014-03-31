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

/**
 * ProgressionProcess is generated only by the progressionManager or another
 * ProgressionProcess.
 * 
 * @author Nicolas Fortin (IFSTTAR/EASE)
 * 
 */
public class ProgressionProcess {
	protected ProgressionProcess parentProcess;
	private long subprocess_size;
	private double subprocess_done = 0;

	public ProgressionProcess(ProgressionProcess parentProcess,
			long subprocess_size) {
		this.parentProcess = parentProcess;
		this.subprocess_size = subprocess_size;
	}

	/**
	 * 
	 * @return The progression on this process [0-1]
	 */
	public synchronized double getProcessProgression() {
		return subprocess_done / subprocess_size;
	}

	/**
	 * 
	 * @return The main progression value [0-1]
	 */
	public synchronized double getMainProgression() {
		ProgressionProcess prog = this;
		while (prog.parentProcess != null) {
			prog = prog.parentProcess;
		}
		return prog.getProcessProgression();
	}

	@Override
	protected synchronized void finalize() throws Throwable {
		// do finalization here
		if (this.parentProcess != null) {
			// Complete remaining process
			if (Double.compare(subprocess_done, subprocess_size)  != 0) {
				this.parentProcess
						.pushProgression(1 - (subprocess_done / subprocess_size));
			}
		}
		super.finalize();
	}

	/**
	 * Get a new subprocess instance
	 * 
	 * @param subprocess_size
	 *            Sub Process estimated work item (sub-sub process count)
	 * @return
	 */
	public synchronized ProgressionProcess nextSubProcess(long subprocess_size) {
		return new ProgressionProcess(this, subprocess_size);
	}

	/**
	 * A subprocess computation has been done (same as call NextSubProcess then
	 * destroy the returned object)
	 */
	public synchronized void nextSubProcessEnd() {
		pushProgression(1.0);
	}

	/**
	 * Optional, When the current process is done call this method. Or let the
	 * garbage collector free the object
	 */
	public synchronized void processFinished() {
		if (Double.compare(subprocess_done, subprocess_size)  != 0) {
			this.parentProcess
					.pushProgression(1 - (subprocess_done / subprocess_size));
			subprocess_done = subprocess_size;
		}
	}

	protected synchronized void pushProgression(double incProg) {
		if (subprocess_done + incProg <= subprocess_size) {
			subprocess_done += incProg;
			if (parentProcess != null) {
				parentProcess.pushProgression((incProg / subprocess_size));
			}
		}
	}
}
