/*
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact in urban areas. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 *
 * This plugin is currently developed by the Environmental Acoustics Laboratory (LAE) of Ifsttar
 * (http://wwww.lae.ifsttar.fr/) in collaboration with the Lab-STICC CNRS laboratory.
 * It was initially developed as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 *
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * <nicolas.fortin@ifsttar.fr>
 *
 * Copyright (C) 2011-2016 IFSTTAR-CNRS
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
 * For more information concerning NoiseM@p, please consult: <http://noisemap.orbisgis.org/>
 *
 * For more information concerning OrbisGis, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 *
 * info_at_ orbisgis.org
 */

package org.orbisgis.noisemap.h2;

import org.h2gis.h2spatialapi.ProgressVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeListener;

/**
 * @author Nicolas Fortin
 */
public class ProgressLogger implements ProgressVisitor {
    private static final Logger LOGGER = LoggerFactory.getLogger("gui."+ProgressLogger.class);
    private int receiverCount = 1;
    private int processed = 0;
    private int lastLogProgression = 0;

    @Override
    public ProgressVisitor subProcess(int i) {
        receiverCount = i;
        processed = 0;
        return this;
    }

    @Override
    public void endStep() {
        synchronized (this) {
            processed = Math.min(receiverCount, processed + 1);
            int prog = (int) ((processed / (double) receiverCount) * 100);
            if (prog != lastLogProgression) {
                lastLogProgression = prog;
                LOGGER.info(prog+" %");
            }
        }
    }

    @Override
    public void setStep(int i) {

    }

    @Override
    public int getStepCount() {
        return 0;
    }

    @Override
    public void endOfProgress() {

    }

    @Override
    public double getProgression() {
        return 0;
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public void cancel() {

    }

    @Override
    public void addPropertyChangeListener(String s, PropertyChangeListener listener) {

    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {

    }
}
