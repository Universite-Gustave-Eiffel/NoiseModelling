package org.noisemap.core;
/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/
/**
 * @brief Sources merging This class can merge source point by a
 *        maximum distance, from another closed source.
 *  TODO use hashmap with position/epsilon_merge
 */

import java.util.List;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.quadtree.Quadtree;

public class PointsMerge {
	private Quadtree ptQuad = new Quadtree();
	private double distMerge = 1.;

	
	public PointsMerge(double distMerge) {
		super();
		this.distMerge = distMerge;
	}


	@SuppressWarnings("unchecked")
	/**
	 * Compute unique index for the coordinate
	 * Index count from 0 to n
	 * If the new vertex is closer than distMerge with an another vertex then it will return its index.
	 * @return The index of the vertex
	 */
	public int getOrAppendVertex(Coordinate newCoord) {
		int newCoordIndex = -1;
		Envelope queryEnv = new Envelope(newCoord);
		queryEnv.expandBy(this.distMerge*2);
		List<EnvelopeWithIndex<Integer>> result = this.ptQuad.query(queryEnv);
		for (EnvelopeWithIndex<Integer> envel : result) {
			if (envel.getPosition().distance(newCoord) < this.distMerge) {
				return envel.getId();
			}
		}
		// Not found then
		// Append to the list and QuadTree
		newCoordIndex = this.ptQuad.size();
		this.ptQuad.insert(queryEnv, new EnvelopeWithIndex<Integer>(queryEnv,
				newCoordIndex));
		return newCoordIndex;
	}
}
