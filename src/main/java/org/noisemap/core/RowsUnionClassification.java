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
 * This class agregates rows index, the goal is to reduce memory usage, ordering by ascending index, and may be optimize row query thanks to interval row number
 */
public class RowsUnionClassification {
    //TODO store int instead of Integer
    private List<Integer> rowrange=new ArrayList<Integer>(); //Row intervals ex: 0,15,50,60 for 0 to 15 and 50 to 60

    /**
     * 
     * @param row First row id
     */
    RowsUnionClassification(int row) {
        rowrange.add(row);
        rowrange.add(row);
    }
    /**
     * 
     * @param row First row id
     */
    RowsUnionClassification(int rowbegin, int rowend) {
        if(rowend<rowbegin) {
            throw new IllegalArgumentException("Begin row index must be inferior or equal to end row index.");
        }
        rowrange.add(rowbegin);
        rowrange.add(rowend);
    }
    /**
     * Return an iteror for reading row line ranges
     * To iterate over 
     * @return An integer, begin of a range then end of the range, then begin of next range etc..
     * @warning begin and end values are included [begin-end]
     */
    public Iterator<Integer> getRowRanges() {
        return this.rowrange.iterator();
    }
    
    /**
     * @return The number of Integer in this instance
     */
    public int getItemCount() {
        return rowrange.size();
    }
    
    /**
     * Does this container has intervals
     * @return True if this container is empty, false otherwise
     */
    public boolean isEmpty() {
        return this.rowrange.isEmpty();
    }
    /**
     * Add a row index in the list
     * @param row The row index. Duplicates are not pushed, and do not raise errors.
     * @TODO Add function to push a range instead of a single row index
     * @TODO refactor, use only one call of binarySearch !
     */
    public void addRow(int row) {
        // Iterate over the row range array and find contiguous row
        boolean inserted = false;
        //Search another End Range to this row number
        int index = Collections.binarySearch(rowrange, row-1);
        if(index >=0) {
            if(index % 2==0) {
                if(rowrange.get(index+1)<row) {
                    rowrange.set(index+1, row); //Cover case [row,row], index+1 must be updated
                    index++;
                } else {
                    return; //The end range cover the row value
                }
            } else {
                rowrange.set(index, row);
            }
            //Search if this number link with end of another range
            int indexAnother = Collections.binarySearch(rowrange, row+1);
            if(indexAnother >=0) {
                if(indexAnother % 2!=0 && rowrange.get(indexAnother-1)==row+1) {
                    indexAnother--;
                }
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
                if(index % 2==0) {
                    inserted = true;
                    rowrange.set(index, row);
                } else {
                    if(rowrange.get(index-1)>row) {
                        rowrange.set(index -1, row);
                    }
                }
            }
        }
        if(!inserted) {
            //Find if this range is already contained in the intervals
            index = Collections.binarySearch(rowrange, row);
            if(index < 0) {
                index=-index-1; //retrieve the nearest index by order
                if(index != rowrange.size()) {
                    if(index % 2==0) {  //If index corresponding to begin of a range
                           if(row >= rowrange.get(index) && row <= rowrange.get(index+1) ) {
                               return; //Nothing to do, row is already in the array
                           }
                    } else {            //If index corresponding to the end of a range
                           if(row >= rowrange.get(index-1) && row <= rowrange.get(index) ) {
                               return; //Nothing to do, row is already in the array
                           }
                    }
                }
                //New range
                rowrange.add(index,row);
                rowrange.add(index,row);
            }
        }
    }
}
