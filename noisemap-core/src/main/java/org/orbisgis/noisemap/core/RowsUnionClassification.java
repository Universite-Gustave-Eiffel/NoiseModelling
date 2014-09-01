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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This is a record for a value of the has map of the function ST_TableGeometryUnion.
 * This class aggregates rows index, the goal is to reduce memory usage, ordering by ascending index,
 * and may be optimize row query thanks to interval row number.
 * 
 * @author Nicolas Fortin
 */
public class RowsUnionClassification implements Iterable<RowInterval> {
    //TODO store int instead of Integer
    private List<Integer> rowrange=new ArrayList<Integer>(); //Row intervals ex: 0,15,50,60 for 0 to 15 and 50 to 60

    /**
     * Default constructor
     */
    public RowsUnionClassification() {

    }
    /**
     * 
     * @param row First row id
     */
    public RowsUnionClassification(int row) {
        rowrange.add(row);
        rowrange.add(row);
    }
    /**
     * 
     * @param rowbegin First row id
     * @param rowend Last row id
     */
    public RowsUnionClassification(int rowbegin, int rowend) {
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
     * @deprecated Use RowsUnionClassification has an iterator
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

    @Override
    public Iterator<RowInterval> iterator() {
        return new RowIterator(rowrange.iterator());
    }
    private class RowIterator implements Iterator<RowInterval> {
        Iterator<Integer> itR;

        public RowIterator(Iterator<Integer> itR) {
            this.itR = itR;
        }
        
        @Override
        public boolean hasNext() {
            return itR.hasNext();
        }

        @Override
        public RowInterval next() {
            return new RowInterval(itR.next(),itR.next()+1);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }
}
