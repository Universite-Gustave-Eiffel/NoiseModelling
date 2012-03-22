/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/
package org.noisemap.core;
/**
 * Interval of index, [begin-end[
 * Use it in a for loop.
 * < for(int i=begin;i<end;i++)
 */
public class RowInterval {
    final int begin;
    final int end;

    public RowInterval(int begin, int end) {
        this.begin = begin;
        this.end = end;
    }

    public int getBegin() {
        return begin;
    }

    public int getEnd() {
        return end;
    }
    
}
