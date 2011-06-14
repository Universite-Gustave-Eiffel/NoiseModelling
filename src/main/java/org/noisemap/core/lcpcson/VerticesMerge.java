package lcpcson;

/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

import java.util.ArrayList;
import java.util.List;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFilter;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.quadtree.Quadtree;

/**
 * @brief Vertices merging This class can merge vertices of any geometries by a
 *        maximum distance, from another closed vertices You must keep the
 *        instance of this object, then call reset() before filter another
 *        geometry Use getOrAppendVertices to retrieve an unique and ordered
 *        index from a coordinate.
 * 
 * @sample VerticesMerge mergeTool = new VerticesMerge();
 *         ageomtry.apply(mergeTool);
 */
public class VerticesMerge implements CoordinateSequenceFilter {
	private ArrayList<Coordinate> vertices = new ArrayList<Coordinate>();
	private Quadtree ptQuad = new Quadtree();
	private boolean done = false;
	private boolean change = false;
	private static final double EPSILON = 1.;
	private static final double DIST_MERGE = 1.;

	public ArrayList<Coordinate> getVertices() {
		return vertices;
	}

	@SuppressWarnings("unchecked")
	public int getOrAppendVertices(Coordinate newCoord) {
		// We can obtain the same hash with two different coordinate (4 Bytes or
		// 8 Bytes against 12 or 24 Bytes) , then we use a list as the value of
		// the hashmap
		// First step - Search the vertice parameter within the hashMap
		int newCoordIndex = -1;
		Envelope queryEnv = new Envelope(newCoord);
		queryEnv.expandBy(1.f);
		List<EnvelopeWithIndex<Integer>> result = ptQuad.query(queryEnv);
		for (EnvelopeWithIndex<Integer> envel : result) {
			Coordinate foundCoord = vertices.get(envel.getId());
			if (foundCoord.distance(newCoord) < DIST_MERGE) {
				return envel.getId();
			}
		}
		// Not found then
		// Append to the list and QuadTree
		vertices.add(newCoord);
		newCoordIndex = vertices.size() - 1;
		ptQuad.insert(queryEnv, new EnvelopeWithIndex<Integer>(queryEnv,
				newCoordIndex));
		return newCoordIndex;
	}

	@Override
	public void filter(CoordinateSequence seq, int i) {
		Coordinate coordinate = seq.getCoordinate(i);
		coordinate.x = ((long) (coordinate.x / EPSILON)) * EPSILON;
		coordinate.x = ((long) (coordinate.y / EPSILON)) * EPSILON;
		coordinate.z = 0.;
		int id = this.getOrAppendVertices(coordinate);
		if (id < vertices.size() - 1) {
			coordinate = this.vertices.get(id);
			seq.setOrdinate(i, 0, coordinate.x);
			seq.setOrdinate(i, 1, coordinate.y);
			seq.setOrdinate(i, 2, 0);
			change = true;
		}
		if (i == seq.size()) {
			done = true;
		}
	}

	public void reset() {
		done = false;
		this.change = false;
	}

	@Override
	public boolean isDone() {
		return done;
	}

	@Override
	public boolean isGeometryChanged() {
		return change;
	}

}
