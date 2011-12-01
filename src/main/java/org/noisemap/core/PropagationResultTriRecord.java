/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

package org.noisemap.core;

import com.vividsolutions.jts.geom.Geometry;

/**
 *
 * @author fortin
 */
public class PropagationResultTriRecord {


    private Geometry triangle;
    private double v1,v2,v3;
    private long cellId,triId;

    public PropagationResultTriRecord(Geometry triangle, double v1, double v2, double v3, long cellId, long triId) {
        this.triangle = triangle;
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.cellId = cellId;
        this.triId = triId;
    }

    public long getCellId() {
        return cellId;
    }

    public long getTriId() {
        return triId;
    }


    public Geometry getTriangle() {
        return triangle;
    }

    public double getV1() {
        return v1;
    }

    public double getV2() {
        return v2;
    }

    public double getV3() {
        return v3;
    }
}
