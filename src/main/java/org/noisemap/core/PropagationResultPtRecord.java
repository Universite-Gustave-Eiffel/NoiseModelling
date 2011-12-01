/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/


package org.noisemap.core;

/**
 * Results of BR_PtGrid
 */
public class PropagationResultPtRecord {
    private long receiverRecordRow;
    private int cellId;
    private double receiverLvl;

    public PropagationResultPtRecord(long receiverRecordRow, int cellId, double receiverLvl) {
        this.receiverRecordRow = receiverRecordRow;
        this.cellId = cellId;
        this.receiverLvl = receiverLvl;
    }

    public int getCellId() {
        return cellId;
    }

    public void setCellId(int cellId) {
        this.cellId = cellId;
    }

    public double getReceiverLvl() {
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
