/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/


package org.noisemap.core;

import java.util.ArrayList;
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
    public Iterator<Integer> getRowRanges() {
        return this.rowrange.iterator();
    }
    public void addRow(int row) {
        // Iterate over the row range array and find contiguous row
        // row deb/row end angle
        int rowdeb=row;
        int rowend=row;
        boolean inserted = false;
        // Update existing angle ranges
        boolean doNewLoop = true;
        while (doNewLoop) {
                doNewLoop = false;
                for (int idrange = 0; idrange < rowrange.size() - 1; idrange += 2) {
                        if (rowrange.get(idrange)==rowend + 1) {
                                inserted = true;
                                if (rowrange.size() > 2) {
                                        // Remove merged element and reloop
                                        doNewLoop = true;
                                        inserted = false;
                                        rowend = rowrange.get(idrange + 1);
                                        rowrange.remove(idrange);
                                        rowrange.remove(idrange);
                                } else {
                                        rowrange.set(idrange, rowdeb);
                                }
                                break;
                        } else if (rowrange.get(idrange + 1)==rowdeb - 1) {
                                inserted = true;
                                if (rowrange.size() > 2) {
                                        // Remove merged element and reloop
                                        doNewLoop = true;
                                        inserted = false;
                                        rowdeb = rowrange.get(idrange);
                                        rowrange.remove(idrange);
                                        rowrange.remove(idrange);
                                } else {
                                        rowrange.set(idrange + 1, rowend);
                                }
                                break;
                        }
                }
        }
        // Row is not contiguous with others
        if (!inserted) {
                rowrange.add(rowdeb);
                rowrange.add(rowend);
        }
    }
}
