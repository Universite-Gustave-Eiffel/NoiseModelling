package org.noisemap.core;

/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.jdelaunay.delaunay.ConstrainedMesh;
import org.jdelaunay.delaunay.DEdge;
import org.jdelaunay.delaunay.DPoint;
import org.jdelaunay.delaunay.DTriangle;
import org.jdelaunay.delaunay.DelaunayError;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFilter;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

public class LayerJDelaunay implements LayerDelaunay {
	private static Logger logger = Logger.getLogger(LayerJDelaunay.class
			.getName());
	private List<Coordinate> vertices = new ArrayList<Coordinate>();
	private ArrayList<DEdge> constraintEdge = new ArrayList<DEdge>();
	private LinkedList<DPoint> ptToInsert = new LinkedList<DPoint>();
	private List<Coordinate> holes = new LinkedList<Coordinate>();
	
	private boolean computeNeighbors=false;
	List<Triangle> triangles = new ArrayList<Triangle>();
	private List<Triangle> neighbors = new ArrayList<Triangle>(); // The
																		// first
																		// neighbor
																		// of
																		// triangle
																		// i is
																		// opposite
																		// the
																		// first
																		// corner
																		// of
																		// triangle
																		// i
	HashMap<Integer, LinkedList<Integer>> hashOfArrayIndex = new HashMap<Integer, LinkedList<Integer>>();

	private static DTriangle findTriByCoordinate(Coordinate pos,List<DTriangle> trilst) throws DelaunayError {
		DPoint pt;
		if(Double.isNaN(pos.z)) {
			pt=new DPoint(pos.x,pos.y,0);
		} else {
			pt=new DPoint(pos);
		}
		return trilst.get(trilst.size()/2).searchPointContainer(pt);
	}
	private static class SetZFilter implements CoordinateSequenceFilter {
		private boolean done = false;

		@Override
		public void filter(CoordinateSequence seq, int i) {
			double x = seq.getX(i);
			double y = seq.getY(i);
			seq.setOrdinate(i, 0, x);
			seq.setOrdinate(i, 1, y);
			seq.setOrdinate(i, 2, 0);
			if (i == seq.size()) {
				done = true;
			}
		}

		@Override
		public boolean isDone() {
			return done;
		}

		@Override
		public boolean isGeometryChanged() {
			return true;
		}
	}

	private int getOrAppendVertices(Coordinate newCoord,
			List<Coordinate> vertices,
			HashMap<Integer, LinkedList<Integer>> hashOfArrayIndex) {
		// We can obtain the same hash with two different coordinate (4 Bytes or
		// 8 Bytes against 12 or 24 Bytes) , then we use a list as the value of
		// the hashmap
		// First step - Search the vertice parameter within the hashMap
		int newCoordIndex = -1;
		Integer coordinateHash = newCoord.hashCode();
		LinkedList<Integer> listOfIndex = hashOfArrayIndex.get(coordinateHash);
		if (listOfIndex != null) // There are the same hash value
		{
			for (int vind : listOfIndex) // Loop inside the coordinate index
			{
				if (newCoord.equals3D(vertices.get(vind))) // the coordinate is
															// equal to the
															// existing
															// coordinate
				{
					newCoordIndex = vind;
					break; // Exit for loop
				}
			}
			if (newCoordIndex == -1) {
				// No vertices has been found, we push the new coordinate into
				// the existing linked list
				newCoordIndex = vertices.size();
				listOfIndex.add(newCoordIndex);
				vertices.add(newCoord);
			}
		} else {
			// Push a new hash element
			listOfIndex = new LinkedList<Integer>();
			newCoordIndex = vertices.size();
			listOfIndex.add(newCoordIndex);
			vertices.add(newCoord);
			hashOfArrayIndex.put(coordinateHash, listOfIndex);
		}
		return newCoordIndex;
	}

	private ConstrainedMesh delaunayTool = null;

	@Override
	public void processDelaunay() throws LayerDelaunayError {
		if (delaunayTool != null) {
			try {
				// Push segments
				delaunayTool.setPoints(ptToInsert);
				delaunayTool.setConstraintEdges(constraintEdge);
				//delaunayTool.forceConstraintIntegrity();
				delaunayTool.processDelaunay();
				constraintEdge.clear();
				ptToInsert.clear();
				List<DTriangle> trianglesDelaunay = delaunayTool
						.getTriangleList();
				//triangles.ensureCapacity(trianglesDelaunay.size());// reserve
																	// memory
				HashMap<Integer, Integer> gidToIndex = new HashMap<Integer, Integer>();
				ArrayList<Triangle> gidTriangle=new ArrayList<Triangle>(trianglesDelaunay.size());
				
				//Build ArrayList for binary search
				
				//Remove triangles
				for(Coordinate hole : holes) {
					DTriangle foundTri=findTriByCoordinate(hole,trianglesDelaunay);
					if(foundTri == null) {
						throw new LayerDelaunayError("hole outside domain");
					}
					//Navigate through neighbors until it reach a deleted tri or locked segment
					Stack<DTriangle> navHistoryTri=new Stack<DTriangle>();
					Stack<Short> navHistoryDir=new Stack<Short>();
					navHistoryTri.push(foundTri);
					navHistoryDir.push((short)0);//Set as hole
					foundTri.setExternalGID(0);
					while(!navHistoryTri.empty()) {
						if(navHistoryDir.peek()==3) {
							navHistoryTri.pop();
							navHistoryDir.pop();							
						} else {
							DEdge ed = navHistoryTri.peek().getEdge(navHistoryDir.peek());
							if(!ed.isLocked()) {
								DTriangle neigh=ed.getOtherTriangle(navHistoryTri.peek());
								if(neigh != null) {
									if(neigh.getExternalGID()!=0) { //Not set as destroyed
										neigh.setExternalGID(0); //Set as hole
										navHistoryDir.push((short)(navHistoryDir.pop()+1));
										navHistoryDir.push((short)-1);
										navHistoryTri.push(neigh);
									}
								}
							}
							navHistoryDir.push((short)(navHistoryDir.pop()+1));
						}
					}
				}
				
				for (DTriangle triangle : trianglesDelaunay) {
					if(triangle.getExternalGID()!=0) //Not a hole
					{
						Coordinate [] ring = new Coordinate [] {triangle.getPoint(0).getCoordinate(),triangle.getPoint(1).getCoordinate(),triangle.getPoint(2).getCoordinate(),triangle.getPoint(0).getCoordinate()};
						if(!CGAlgorithms.isCCW(ring)) {
							Coordinate tmp= new Coordinate(ring[0]);
							ring[0]=ring[2];
							ring[2]=tmp;
						}
							
						int a = getOrAppendVertices(ring[0], vertices, hashOfArrayIndex);
						int b = getOrAppendVertices(ring[1], vertices, hashOfArrayIndex);
						int c = getOrAppendVertices(ring[2], vertices, hashOfArrayIndex);
						triangles.add(new Triangle(a, b, c));
						if(this.computeNeighbors) {
							Triangle gidTri=new Triangle(-1,-1,-1);
							for(int i=0;i<3;i++) {
								DTriangle neighTriangle = triangle.getEdge(i).getOtherTriangle(triangle);
								if(neighTriangle!=null && neighTriangle.getExternalGID()!=0) {
									gidTri.set(i,neighTriangle.getGID());
								}
							}
							gidTriangle.add(gidTri);
							gidToIndex.put(triangle.getGID(),gidTriangle.size()-1);
						}
					}
				}
				if(this.computeNeighbors) {
					//Translate GID to local index
					for(Triangle tri : gidTriangle) {
						Triangle localTri=new Triangle(-1,-1,-1);
						for(int i=0;i<3;i++) {
							int index=tri.get(i);
							if(index!=-1)
								localTri.set(i, gidToIndex.get(index));
						}
						neighbors.add(localTri);
					}
				}
				delaunayTool = null;

			} catch (DelaunayError e) {
				throw new LayerDelaunayError(e.getMessage());
			}
		}
	}

	@Override
	public void addPolygon(Polygon newPoly, boolean isEmpty)
			throws LayerDelaunayError {

		if (delaunayTool == null) {
			delaunayTool = new ConstrainedMesh();
		}

		// To avoid errors we set the Z coordinate to 0.
		SetZFilter zFilter = new SetZFilter();
		newPoly.apply(zFilter);
		GeometryFactory factory = new GeometryFactory();
		final Coordinate[] coordinates = newPoly.getExteriorRing()
				.getCoordinates();
		if (coordinates.length > 1) {
			LineString newLineString = factory.createLineString(coordinates);
			this.addLineString(newLineString);
		}
		if (isEmpty) {
			holes.add(newPoly.getInteriorPoint().getCoordinate());
		}
		// Append holes
		final int holeCount = newPoly.getNumInteriorRing();
		for (int holeIndex = 0; holeIndex < holeCount; holeIndex++) {
			LineString holeLine = newPoly.getInteriorRingN(holeIndex);
			// Convert hole into a polygon, then compute an interior point
			Polygon polyBuffnew = factory.createPolygon(
					factory.createLinearRing(holeLine.getCoordinates()), null);
			if (polyBuffnew.getArea() > 0.) {
				Coordinate interiorPoint = polyBuffnew.getInteriorPoint()
						.getCoordinate();
				if (!factory.createPoint(interiorPoint).intersects(holeLine)) {
					if(!isEmpty)
						holes.add(interiorPoint);
					this.addLineString(holeLine);
				} else {
					logger.info("Warning : hole rejected, can't find interior point.");
				}
			} else {
				logger.info("Warning : hole rejected, area=0");
			}
		}
	}

	@Override
	public void setMinAngle(Double minAngle) {
		// TODO Auto-generated method stub
		if (delaunayTool != null) {
			// delaunayTool.setMinAngle(minAngle);
		}
	}

	@Override
	public void hintInit(Envelope bBox, long polygonCount, long verticesCount)
			throws LayerDelaunayError {
	}

	@Override
	public List<Coordinate> getVertices() throws LayerDelaunayError {
		return this.vertices;
	}

	@Override
	public List<Triangle> getTriangles() throws LayerDelaunayError {
		return this.triangles;
	}

	@Override
	public void addVertex(Coordinate vertexCoordinate)
			throws LayerDelaunayError {
		/*
		 * try { MyPoint newpt=new
		 * MyPoint(vertexCoordinate.x,vertexCoordinate.y,0.);
		 * newpt.setUseZ(false); this.ptToInsert.add(newpt); } catch
		 * (DelaunayError e) { throw new LayerDelaunayError(e.getMessage()); }
		 */
	}

	@Override
	public void setMaxArea(Double maxArea) throws LayerDelaunayError {
		// TODO Auto-generated method stub

	}

	@Override
	public void addLineString(LineString lineToProcess)
			throws LayerDelaunayError {
		Coordinate[] coords = lineToProcess.getCoordinates();
		try {
			for (int ind = 1; ind < coords.length; ind++) {
				this.constraintEdge.add(new DEdge(new DPoint(coords[ind - 1]),
						new DPoint(coords[ind])));
			}
		} catch (DelaunayError e) {
			throw new LayerDelaunayError(e.getMessage());
		}
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Triangle> getNeighbors() throws LayerDelaunayError {
		if(computeNeighbors) {
			return neighbors;
		} else {
			throw new LayerDelaunayError("You must call setRetrieveNeighbors(True) before process delaunay triangulation");
		}
	}

	@Override
	public void setRetrieveNeighbors(boolean retrieve) {
		this.computeNeighbors=retrieve;
		
	}

}
