package org.noisemap.core;

/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import java.util.ArrayList;
import java.util.Iterator;

public class QueryQuadTree implements QueryGeometryStructure {
	private Quadtree quad = new Quadtree();

	@Override
	public void appendGeometry(Geometry newGeom, Integer externalId) {
		quad.insert(newGeom.getEnvelopeInternal(),
				new EnvelopeWithIndex<Integer>(newGeom.getEnvelopeInternal(),
						externalId));
	}
        /**
         * @return Number of items
         */
        public int size() {
            return quad.size();
        }
	@Override
	public Iterator<Integer> query(Envelope queryEnv) {
		@SuppressWarnings("unchecked")
		ArrayList<EnvelopeWithIndex<Integer>> resq = (ArrayList<EnvelopeWithIndex<Integer>>) quad
				.query(queryEnv);
		ArrayList<Integer> ret = new ArrayList<Integer>(resq.size());
		for (EnvelopeWithIndex<Integer> it : resq) {
			if (queryEnv.intersects(it)) {
				ret.add(it.getId());
			}
		}
		return ret.iterator();
	}

}
