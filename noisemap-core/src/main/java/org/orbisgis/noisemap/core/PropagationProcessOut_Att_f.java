/**
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact on urban mobility plans. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 * <p>
 * This version is developed at French IRSTV Institute and at IFSTTAR
 * (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 * <p>
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
 * <p>
 * Copyright (C) 2011 IFSTTAR
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
 * <p>
 * Noisemap is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * Noisemap. If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.orbisgis.noisemap.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Way to store data computed by thread.
 * Multiple threads use the same Out, then all methods has been synchronized
 *
 * @author Nicolas Fortin
 * @author Pierre Aumond 07/06/2016
 */
public class PropagationProcessOut_Att_f {

    public static final class verticeSL {
        public final int sourceId;
        public final int receiverId;
        public final double[] value;

        verticeSL(int receiverId, int sourceId, double[] value) {
            this.sourceId = sourceId;
            this.receiverId = receiverId;
            this.value = value;
        }
    }

    private long nb_couple_receiver_src = 0;
    private long nb_obstr_test = 0;
    private long nb_image_receiver = 0;
    private long nb_reflexion_path = 0;
    private long nb_diffraction_path = 0;
    private long cellComputed = 0;
    List<PropagationProcessOut_Att_f.verticeSL> VerticeSoundLevel = new ArrayList<>();

    public List<verticeSL> getVerticesSoundLevel() {
        return VerticeSoundLevel;
    }

    public void setVerticeSoundLevel(List<verticeSL> verticeSoundLevel) {
        VerticeSoundLevel = verticeSoundLevel;
    }

//public void setVerticesSoundLevel(setVerticeSoundLevel) {
    //	ArrayList<verticeSoundLevel> setVerticeSoundLevel = new ArrayList<>();
    //}

//    public void setVerticeSoundLevel(int receiverId, int sourceId, double[] value) {
//        VerticeSoundLevel.add(new verticeSL(receiverId, sourceId, value));
//
//    }


    public synchronized long getNb_couple_receiver_src() {
        return nb_couple_receiver_src;
    }

    public synchronized long getNb_obstr_test() {
        return nb_obstr_test;
    }

    public synchronized void appendReflexionPath(long added) {
        nb_reflexion_path += added;
    }

    public synchronized void appendDiffractionPath(long added) {
        nb_diffraction_path += added;
    }

    public synchronized long getNb_diffraction_path() {
        return nb_diffraction_path;
    }

    public synchronized void appendImageReceiver(long added) {
        nb_image_receiver += added;
    }

    public synchronized long getNb_image_receiver() {
        return nb_image_receiver;
    }

    public synchronized long getNb_reflexion_path() {
        return nb_reflexion_path;
    }

    public synchronized void appendSourceCount(long srcCount) {
        nb_couple_receiver_src += srcCount;
    }

    public synchronized void appendFreeFieldTestCount(long freeFieldTestCount) {
        nb_obstr_test += freeFieldTestCount;
    }

    public synchronized void log(String str) {

    }

    /**
     * Increment cell computed counter by 1
     */
    public synchronized void appendCellComputed() {
        cellComputed += 1;
    }

    public synchronized long getCellComputed() {
        return cellComputed;
    }

}
