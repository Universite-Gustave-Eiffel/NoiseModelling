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
 * Results of BR_PtGrid.
 * 
 * @author Nicolas Fortin
 * @author Pierre Aumond 07/06/2016
 */
public class PropagationResultPtRecord_f {
    private long receiverRecordRow;
    private int cellId;
    private double receiverLvl;
    private double receiverLvl63;
    private double receiverLvl125;
    private double receiverLvl250;
    private double receiverLvl500;
    private double receiverLvl1000;
    private double receiverLvl2000;
    private double receiverLvl4000;
    private double receiverLvl8000;


    public PropagationResultPtRecord_f(long receiverRecordRow, int cellId, double receiverLvl,
                                       double receiverLvl63, double receiverLvl125, double receiverLvl250, double receiverLvl500,
                                       double receiverLvl1000, double receiverLvl2000, double receiverLvl4000, double receiverLvl8000) {
        this.receiverRecordRow = receiverRecordRow;
        this.cellId = cellId;
        this.receiverLvl = receiverLvl;
        this.receiverLvl63 = receiverLvl63;
        this.receiverLvl125 = receiverLvl125;
        this.receiverLvl250 = receiverLvl250;
        this.receiverLvl500 = receiverLvl500;
        this.receiverLvl1000 = receiverLvl1000;
        this.receiverLvl2000 = receiverLvl2000;
        this.receiverLvl4000 = receiverLvl4000;
        this.receiverLvl8000 = receiverLvl8000;
    }

    public int getCellId() {
        return cellId;
    }

    public void setCellId(int cellId) {
        this.cellId = cellId;
    }

    public double getReceiverLvl(int freq) {
        switch (freq) {
            case 63:
                receiverLvl=receiverLvl63;
                break;
            case 125:
                receiverLvl=receiverLvl125;
                break;
            case 250:
                receiverLvl=receiverLvl250;
                break;
            case 500:
                receiverLvl=receiverLvl500;
                break;
            case 1000:
                receiverLvl=receiverLvl1000;
                break;
            case 2000:
                receiverLvl=receiverLvl2000;
                break;
            case 4000:
                receiverLvl=receiverLvl4000;
                break;
            case 8000:
                receiverLvl=receiverLvl8000;
                break;
            default:
                receiverLvl=receiverLvl;
        }
        return receiverLvl;
    }

    public void setReceiverLvl(double receiverLvl) {
        this.receiverLvl = receiverLvl;
    }

    public long getReceiverRecordRow() {
        return receiverRecordRow;
    }

    public void setReceiverRecordRow(long receiverRecordRow) {
        this.receiverRecordRow = receiverRecordRow;
    }


}
