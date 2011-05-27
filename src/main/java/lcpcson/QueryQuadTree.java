package lcpcson;
/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

import java.util.ArrayList;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.quadtree.Quadtree;

public class QueryQuadTree<index_t> implements QueryGeometryStructure<index_t> {
	private Quadtree quad=new Quadtree();

	@Override
	public void AppendGeometry(Geometry newGeom, index_t externalId) {
		quad.insert(newGeom.getEnvelopeInternal(), new EnvelopeWithIndex<index_t>(newGeom.getEnvelopeInternal(), externalId));
	}

	@Override
	public ArrayList<index_t> query(Envelope queryEnv) {
		@SuppressWarnings("unchecked")
		ArrayList<EnvelopeWithIndex<index_t>> resq = (ArrayList<EnvelopeWithIndex<index_t>>) quad.query(queryEnv);
		ArrayList<index_t> ret=new ArrayList<index_t> (resq.size());
		for(EnvelopeWithIndex<index_t> it : resq)
		{
			if(queryEnv.intersects(it))
				ret.add(it.getId());
		}
		return ret;
	}

}
