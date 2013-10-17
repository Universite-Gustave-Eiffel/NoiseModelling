/**
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact on urban mobility plans. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 *
 * This version is developed at French IRSTV Institute and at IFSTTAR
 * (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 *
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
 *
 * Copyright (C) 2011 IFSTTAR
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
 *
 * Noisemap is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Noisemap. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.noisemap.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.index.strtree.STRtree;
import org.apache.log4j.Logger;

import org.jdelaunay.delaunay.ConstrainedMesh;
import org.jdelaunay.delaunay.geometries.DEdge;
import org.jdelaunay.delaunay.geometries.DPoint;
import org.jdelaunay.delaunay.geometries.DTriangle;
import org.jdelaunay.delaunay.error.DelaunayError;
import org.jdelaunay.delaunay.geometries.Element;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFilter;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

/**
 * 
 * @author Nicolas Fortin
 */
public class LayerJDelaunay implements LayerDelaunay {
	private static Logger logger = Logger.getLogger(LayerJDelaunay.class
			.getName());
	private List<Coordinate> vertices = new ArrayList<Coordinate>();
	private ArrayList<DEdge> constraintEdge = new ArrayList<DEdge>();
	private LinkedList<DPoint> ptToInsert = new LinkedList<DPoint>();
	private List<Coordinate> holes = new LinkedList<Coordinate>();
    private Double maxArea; // maximum area, if set a grid of points is added before triangulation
    /** If a grid point is nearest than another point by this distance then the grid point is not added */
    private static final double EPSILON_AREA_CONSTRAINT = 1;
	private boolean debugMode=false; //output primitives in a text file
	private boolean computeNeighbors=false;
	List<Triangle> triangles = new ArrayList<Triangle>();
    private final static int GRID_PROP = 4;
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
		Element foundEl=trilst.get(trilst.size()/2).searchPointContainer(pt);
		if(foundEl instanceof DTriangle) {
			return (DTriangle)foundEl;
		}else{
			for(DTriangle tri : trilst) {
				if(tri.contains(pt)) {
					return tri;
				}
			}
			return null;
		}
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

    /**
     * This function return insertSegment if insertSegment is not collinear with staticSegment.
     * Otherwise it return the difference of insertSegment and staticSegment
     * @param factory Geometry factory
     * @param staticSegment Segment one
     * @param insertSegment Segment two
     * @return LineSegment or null
     */
    private static LineSegment[] computeIntersection(GeometryFactory factory, LineSegment staticSegment, LineSegment insertSegment) {
        LineString staticSegmentLine = staticSegment.toGeometry(factory);
        LineString insertSegmentLine = insertSegment.toGeometry(factory);
        Geometry diff = staticSegmentLine.difference(insertSegmentLine);
        if(diff instanceof LineString) {
            return new LineSegment[] { insertSegment };
        } else {
            return new LineSegment[] { insertSegment };
        }
    }

	@Override
	public void processDelaunay() throws LayerDelaunayError {
		if (delaunayTool != null && (!ptToInsert.isEmpty() || !constraintEdge.isEmpty())) {
			try {
                if(maxArea != null) {
                    ArrayList<DEdge> edges = new ArrayList<DEdge>(constraintEdge);
                    // Build a PointsMerge tool to not add duplicates
                    Envelope gridEnv = null;
                    if(!ptToInsert.isEmpty()) {
                        gridEnv = new Envelope(ptToInsert.get(0).getCoordinate());
                    } else if(!constraintEdge.isEmpty()) {
                        gridEnv = new Envelope(constraintEdge.get(0).getStartPoint().getCoordinate());
                    }
                    for(DPoint dPoint : ptToInsert) {
                        gridEnv.expandToInclude(dPoint.getCoordinate());
                    }
                    for(DEdge edge : constraintEdge) {
                        gridEnv.expandToInclude(edge.getStartPoint().getCoordinate());
                        gridEnv.expandToInclude(edge.getEndPoint().getCoordinate());
                    }
                    // Points of delaunay input has been inserted, and final grid envelope computed
                    // Compute delta as a point must be inserted at each corner + begin side and side
                    // in order to guarantee continuity between cells
                    // Now insert grid points
                    double requestedDeltaGrid = Math.sqrt(maxArea * 2); // Mul by 2 as square is made of 2 triangles
                    double xCount = Math.ceil((gridEnv.getMaxX() - gridEnv.getMinX()) / requestedDeltaGrid);
                    double yCount = Math.ceil((gridEnv.getMaxY() - gridEnv.getMinY()) / requestedDeltaGrid);
                    double xDelta = (gridEnv.getMaxX() - gridEnv.getMinX()) / xCount;
                    double yDelta = (gridEnv.getMaxY() - gridEnv.getMinY()) / yCount;
                    GeometryFactory factory = new GeometryFactory();
                    // Set negative weigths for grid  as this is less important constraint as polygons.
                    Map<Integer, Integer> weights = new HashMap<Integer, Integer>();
                    weights.put(GRID_PROP, -1);
                    delaunayTool.setWeights(weights);
                    for(int xi = 0; xi < xCount; xi++) {
                        for(int yi = 0; yi < yCount; yi++) {
                            Coordinate gridPoint = new Coordinate(gridEnv.getMinX() + xi * xDelta, gridEnv.getMinY() + yi * yDelta, 0);
                            LineSegment horizontal = new LineSegment(gridPoint, new Coordinate(gridEnv.getMinX() + (xi + 1) * xDelta, gridEnv.getMinY() + yi * yDelta, 0));
                            LineSegment vertical = new LineSegment(gridPoint, new Coordinate(gridEnv.getMinX() + xi * xDelta, gridEnv.getMinY() + (yi + 1) * yDelta, 0));
                            // Insert horizontal constraint
                            /**
                            List results = colinearTestRTree.query(new Envelope(horizontal.p0, horizontal.p1));
                            boolean doInsert = true;
                            LineString segment = factory.createLineString()
                            for(Object index : results) {
                                if(index instanceof Integer) {
                                    DEdge edge = constraintEdge.get((Integer) index);
                                    // If grid segment is partially colinear with edge then update grid segment to complete the line
                                    // if grid segment is totally colinear, do not include the edge

                                }
                            }
                            */
                            DEdge hEdge = new DEdge(new DPoint(horizontal.p0), new DPoint(horizontal.p1));
                            hEdge.setProperty(GRID_PROP);
                            edges.add(hEdge);
                            DEdge vEdge = new DEdge(new DPoint(vertical.p0), new DPoint(vertical.p1));
                            vEdge.setProperty(GRID_PROP);
                            edges.add(vEdge);
                        }
                    }
                    delaunayTool.setPoints(ptToInsert);
                    // Push segments
                    delaunayTool.setConstraintEdges(edges);
                } else {
                    delaunayTool.setPoints(ptToInsert);
                    // Push segments
                    delaunayTool.setConstraintEdges(constraintEdge);
                }
				
				if(debugMode) {
					try
					{
					//Debug mode write input & output data to files
					File file = new File("./layerjdlaunaydebug"+System.currentTimeMillis()+".txt");	
					// Initialization
					PrintWriter out = new PrintWriter(new FileOutputStream(file));
						
					out.printf("DPoint pts[]={");
					// write pts
					for(DPoint pt : delaunayTool.getPoints()) {
						out.printf("new DPoint(%s, %s, %s),\r\n",Double.toString(pt.getX()),Double.toString(pt.getY()),Double.toString(pt.getZ()));
					}
					out.printf("};\r\n");
					//write segments
					out.printf("DEdge edges[]={");
					// write pts

					for(DEdge edge : delaunayTool.getConstraintEdges()) {
						DPoint pt=edge.getStartPoint();
						DPoint pt2=edge.getEndPoint();
						out.printf("new DEdge(%s, %s, %s,%s, %s, %s),\r\n",Double.toString(pt.getX()),Double.toString(pt.getY()),Double.toString(pt.getZ()),Double.toString(pt2.getX()),Double.toString(pt2.getY()),Double.toString(pt2.getZ()));
					}
					out.printf("};\r\n");					
					out.close();
					} catch (FileNotFoundException e) {
						throw new LayerDelaunayError(e.getMessage());
					}
				}
				
				
				delaunayTool.forceConstraintIntegrity();
				delaunayTool.processDelaunay();
                // Post-refinement
                if(maxArea != null) {
                    // refine mesh
                }
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
						throw new LayerDelaunayError("hole outside domain ("+hole+")");
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
							if(!ed.isLocked() || ed.getProperty() == GRID_PROP) {
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
						boolean orientationReversed=false;
						Coordinate [] ring = new Coordinate [] {triangle.getPoint(0).getCoordinate(),triangle.getPoint(1).getCoordinate(),triangle.getPoint(2).getCoordinate(),triangle.getPoint(0).getCoordinate()};
						if(!CGAlgorithms.isCCW(ring)) {
							Coordinate tmp= new Coordinate(ring[0]);
							ring[0]=ring[2];
							ring[2]=tmp;
							orientationReversed=true;
						}
							
						int a = getOrAppendVertices(ring[0], vertices, hashOfArrayIndex);
						int b = getOrAppendVertices(ring[1], vertices, hashOfArrayIndex);
						int c = getOrAppendVertices(ring[2], vertices, hashOfArrayIndex);
						triangles.add(new Triangle(a, b, c));
						if(this.computeNeighbors) {
							Triangle gidTri=new Triangle(-1,-1,-1);
							for(int i=0;i<3;i++) {
								DTriangle neighTriangle = triangle.getOppositeEdge(triangle.getPoint(i)).getOtherTriangle(triangle);
								if(neighTriangle!=null && neighTriangle.getExternalGID()!=0) {
									gidTri.set(i,neighTriangle.getGID());
								}
							}
							if(!orientationReversed) {
								gidTriangle.add(gidTri);
							} else {
								gidTriangle.add(new Triangle(gidTri.getC(),gidTri.getB(),gidTri.getA()));
							}
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
				String msgStack=new String();
				for(StackTraceElement lign : e.getStackTrace()) {
					msgStack.concat(lign.toString()+"\n");
				}
				throw new LayerDelaunayError(e.getMessage()+msgStack);
			}
		}
	}
	private void addHole(Coordinate holePosition) throws LayerDelaunayError
	{
		holes.add(holePosition);
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
			addHole(newPoly.getInteriorPoint().getCoordinate());
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
					if(!isEmpty) {
						addHole(interiorPoint);
					}
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
		this.getOrAppendVertices(vertexCoordinate, vertices, hashOfArrayIndex);
	}

	@Override
	public void setMaxArea(Double maxArea) throws LayerDelaunayError {
		this.maxArea = maxArea;
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
