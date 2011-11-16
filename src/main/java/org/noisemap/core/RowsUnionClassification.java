/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/


package org.noisemap.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This is a record for a value of the hashmap of the function ST_TableGeometryUnion
 */
public class RowsUnionClassification {
    private List<Integer> rowrange=new ArrayList<Integer>(); //Row intervals ex: 0,15,50,60 for 0 to 15 and 50 to 60

    RowsUnionClassification(int row) {
        rowrange.add(row);
        rowrange.add(row);
    }
    /**
     * Return an iteror for reading row line ranges
     * To iterate over 
     * @return
     */
    public Iterator<Integer> getRowRanges() {
        return this.rowrange.iterator();
    }
    public void addRow(int row) {
        // Iterate over the row range array and find contiguous row
        boolean inserted = false;
        int index=-1;
        //Search another End Range to this row number
        index = Collections.binarySearch(rowrange, row-1);
        if(index >=0) {
            inserted = true;
            if(index % 2==0) {
                rowrange.set(index+1, row); //Cover case [row,row], index+1 must be updated
            } else {
                rowrange.set(index, row);
            }
            //Search if this number link with end of another range
            int indexAnother = Collections.binarySearch(rowrange, row+1);
            if(indexAnother >=0) {
                //That the case, we must update the current range and delete two elements.
                rowrange.set(index,rowrange.get(indexAnother+1));
                rowrange.remove(indexAnother);
                rowrange.remove(indexAnother);
            }
        } else {
            //Search another Begin Range to this row number
            index = Collections.binarySearch(rowrange, row+1);
            if(index >=0) {
                inserted = true;
                rowrange.set(index, row);
            }
        }
        if(!inserted) {
            //New range
            rowrange.add(-index-1,row);
            rowrange.add(-index-1,row);
        }
    }
}
