package lcpcson;
/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

import java.util.ArrayList;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 * QueryGeometryStructure aims to speed up the query of a geometry collection inside a region envelope
 * @author N.Fortin J.Picaut (IFSTTAR 2011)
 */
 
public interface QueryGeometryStructure<T> {
	
	void appendGeometry(final Geometry newGeom,final T externalId);
	ArrayList<T> query(Envelope queryEnv);

}
