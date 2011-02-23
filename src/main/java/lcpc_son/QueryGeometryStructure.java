package lcpc_son;

import java.util.ArrayList;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 * QueryGeometryStructure aims to speed up the query of a geometry collection inside a region envelope
 * @author N.Fortin J.Picaut (IFSTTAR 2011)
 */
 
public interface QueryGeometryStructure<index_t> {
	
	public void AppendGeometry(final Geometry newGeom,final index_t externalId);
	public ArrayList<index_t> query(Envelope queryEnv);

}
