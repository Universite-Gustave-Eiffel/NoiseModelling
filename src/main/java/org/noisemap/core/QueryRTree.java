/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.noisemap.core;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.strtree.STRtree;
import java.util.Iterator;

/**
 * Connector for RTree
 */
public class QueryRTree implements QueryGeometryStructure {
    private STRtree rTree;
    public QueryRTree() {
        rTree = new STRtree();
    }

    @Override
    public void appendGeometry(Geometry newGeom, Integer externalId) {
        rTree.insert(newGeom.getEnvelopeInternal(), externalId);
    }

    @Override
    public Iterator<Integer> query(Envelope queryEnv) {
        return rTree.query(queryEnv).iterator();
    }
    
}
