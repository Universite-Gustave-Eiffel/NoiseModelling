package org.noisemap.core;

/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import java.util.Iterator;

/**
 * QueryGeometryStructure aims to speed up the query of a geometry collection
 * inside a region envelope
 * 
 * @author N.Fortin J.Picaut (IFSTTAR 2011)
 */

public interface QueryGeometryStructure {

	void appendGeometry(final Geometry newGeom, Integer externalId);

	Iterator<Integer> query(Envelope queryEnv);

}
