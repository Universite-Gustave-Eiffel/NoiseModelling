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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.grap.utilities.EnvelopeUtil;

import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.math.Vector2D;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.buffer.BufferParameters;
import java.util.*;

/**
 * FastObstructionTest is a Delaunay Structure to speed up the search of
 * visibility test. TODO enable add and query of geometry object (other than
 * fitting elements) into the delaunay triangulation. To do something called
 * visibility culling.
 * @author Nicolas Fortin
 */
public class FastObstructionTest {
	public static final double epsilon = 1e-7;
	public static final double wideAngleTranslationEpsilon = 0.01;
	private long nbObstructionTest=0;
	private List<Triangle> triVertices;
	private List<Coordinate> vertices;
	private List<Triangle> triNeighbors; // Neighbors
	private LinkedList<Geometry> toUnite = new LinkedList<Geometry>(); // Polygon
        private LinkedHashMap<Integer, Double> toUnites= new LinkedHashMap<Integer,Double>(); // Polygon with height(key Int coordinate)
        private LinkedHashMap<Geometry, Double> toUnitetest= new LinkedHashMap<Geometry, Double>();//test Polygon with height(key Geometry)
	private LinkedList<Double> height= new  LinkedList<Double>(); // Height of Polygon
        private LinkedList<PolygonWithHeight> polygonwithheight= new LinkedList<PolygonWithHeight>();
        private Envelope geometriesBoundingBox=null;
	// union;
	private QueryGeometryStructure triIndex = null; //TODO remove
	private int lastFountPointTriTest = 0;
	private List<Float> verticesOpenAngle = null;
	private List<Coordinate> verticesOpenAngleTranslated = null; /*Open angle*/
        private int BuildingIndex=0;
        
        private static class PolygonWithHeight{
            private Geometry geo;
            private double height;
            public PolygonWithHeight(Geometry geo,double height){
            
                this.geo=geo;
                this.height=height;
            }
            public Geometry getGeometry(){
            
                return this.geo;
            }
            public double getHeight(){
                return this.height;
            }
        
        }
        
	public FastObstructionTest() {
		super();
	}
	public long getNbObstructionTest() {
		return nbObstructionTest;
	}
	/**
	 * Retrieve triangle list, only for debug and unit test purpose
	 * @return
	 */
	public List<Triangle> getTriangles() {
		return triVertices;
	}

        
	/**
	 * Retrieve vertices list, only for debug and unit test purpose
	 * @return
	 */
	public List<Coordinate> getVertices() {
		return vertices;
	}
	public void addGeometry(Geometry obstructionPoly) {
		if(this.geometriesBoundingBox==null) {
			this.geometriesBoundingBox=new Envelope(obstructionPoly.getEnvelopeInternal());
		} else {
			this.geometriesBoundingBox.expandToInclude(obstructionPoly.getEnvelopeInternal());
		}
		toUnite.add(obstructionPoly);
                height.add(0.);
                
	}
        
        /**
         * Add height of building 
         * @return
         */
      	public void addGeometry(Geometry obstructionPoly, double heightofBuilding) {
		if(this.geometriesBoundingBox==null) {
			this.geometriesBoundingBox=new Envelope(obstructionPoly.getEnvelopeInternal());
		} else {
			this.geometriesBoundingBox.expandToInclude(obstructionPoly.getEnvelopeInternal());
		}
                
                toUnite.add(obstructionPoly);
                polygonwithheight.add(new PolygonWithHeight(obstructionPoly, heightofBuilding));
             
        }
                
	private Geometry merge(LinkedList<Geometry> toUnite, double bufferSize) {
		GeometryFactory geometryFactory = new GeometryFactory();
		Geometry geoArray[] = new Geometry[toUnite.size()];
		toUnite.toArray(geoArray);
		GeometryCollection polygonCollection = geometryFactory
				.createGeometryCollection(geoArray);
		return polygonCollection.buffer(bufferSize, 0,
				BufferParameters.CAP_SQUARE);
	}
  
	private void addPolygon(Polygon newpoly, LayerJDelaunay delaunayTool,
			Geometry boundingBox) throws LayerDelaunayError {
		delaunayTool.addPolygon(newpoly, true);
	}
        
        private void addPolygon(Polygon newpoly, LayerJDelaunay delaunayTool,
			Geometry boundingBox, double heightofPolygon) throws LayerDelaunayError {
		delaunayTool.addPolygon(newpoly, true, heightofPolygon);
	}
        
	private void explodeAndAddPolygon(Geometry intersectedGeometry,
			LayerJDelaunay delaunayTool, Geometry boundingBox)
			throws LayerDelaunayError {
                
		if (intersectedGeometry instanceof MultiPolygon
				|| intersectedGeometry instanceof GeometryCollection) {
			for (int j = 0; j < intersectedGeometry.getNumGeometries(); j++) {
				Geometry subGeom = intersectedGeometry.getGeometryN(j);
				explodeAndAddPolygon(subGeom, delaunayTool, boundingBox);
			}
		} else if (intersectedGeometry instanceof Polygon) {
                        for(PolygonWithHeight geo : polygonwithheight){
                            if(geo.getGeometry().equals(intersectedGeometry)){
                              addPolygon((Polygon) intersectedGeometry, delaunayTool, boundingBox, geo.getHeight());
                              return;
                            }
                        }
                        addPolygon((Polygon) intersectedGeometry, delaunayTool, boundingBox, 0.);
		} else if (intersectedGeometry instanceof LineString) {
			delaunayTool.addLineString((LineString) intersectedGeometry);
		}
	}

	// feeding
	public void finishPolygonFeeding(Envelope boundingBoxFilter)
			throws LayerDelaunayError {
		if(boundingBoxFilter!=null) {
			if(this.geometriesBoundingBox!=null) {
				this.geometriesBoundingBox.expandToInclude(boundingBoxFilter);
			} else {
				this.geometriesBoundingBox=boundingBoxFilter;
			}
		}
		
		verticesOpenAngle = null;
		LayerJDelaunay delaunayTool = new LayerJDelaunay();
		// Merge polygon
		Geometry allbuilds = merge(toUnite, 0.);
		toUnite.clear();
		// Insert the main rectangle
		Geometry linearRing = EnvelopeUtil.toGeometry(this.geometriesBoundingBox);
		if (!(linearRing instanceof LinearRing)) {
			return;
		}
		GeometryFactory factory = new GeometryFactory();
		Polygon boundingBox = new Polygon((LinearRing) linearRing, null,
				factory);
		delaunayTool.addPolygon(boundingBox, false);
		// Remove geometries out of the bounding box
		allbuilds = allbuilds.intersection(boundingBox);
		explodeAndAddPolygon(allbuilds, delaunayTool, boundingBox);
		// Process delaunay Triangulation
		delaunayTool.setMinAngle(0.);
		delaunayTool.setRetrieveNeighbors(true);
		
		delaunayTool.processDelaunay();

		// Get results
		this.triVertices = delaunayTool.getTriangles();
		this.vertices = delaunayTool.getVertices();
		this.triNeighbors = delaunayTool.getNeighbors();
		// /////////////////////////////////
		// Feed Query Structure to find triangle, by coordinate

                triIndex = new QueryQuadTree();
		int triind = 0;
		for (Triangle tri : this.triVertices) {
			final Coordinate[] triCoords = { vertices.get(tri.getA()),
					vertices.get(tri.getB()), vertices.get(tri.getC()),
					vertices.get(tri.getA()) };
			Polygon newpoly = factory.createPolygon(
					factory.createLinearRing(triCoords), null);
			triIndex.appendGeometry(newpoly, triind);
			triind++;
		}
	}

	/**
	 * Compute the next triangle index.Find the shortest intersection point of
	 * triIndex segments to the p1 coordinate
	 * 
	 * @param triIndex
	 *            Triangle index
	 * @param propagationLine
	 *            Propagation line
	 * @return Next triangle to the specified direction, -1 if there is no
	 *         triangle neighbor.
	 */
	private int getNextTri(final int triIndex,
			final LineSegment propagationLine,
			HashSet<Integer> navigationHistory) {
		//NonRobustLineIntersector linters = new NonRobustLineIntersector();
		final Triangle tri = this.triVertices.get(triIndex);
		int nearestIntersectionSide = -1;
                int idneigh;
		double nearestIntersectionPtDist = Double.MAX_VALUE;
		// Find intersection pt
		final Coordinate aTri = this.vertices.get(tri.getA());
		final Coordinate bTri = this.vertices.get(tri.getB());
		final Coordinate cTri = this.vertices.get(tri.getC());
		double distline_line;
		// Intersection First Side
                idneigh=this.triNeighbors.get(
                                                triIndex).get(2);
                if (idneigh!=-1 && !navigationHistory.contains(idneigh)) {
                    distline_line=propagationLine.distance(new LineSegment(aTri, bTri));
                    if (distline_line<FastObstructionTest.epsilon &&
                            distline_line < nearestIntersectionPtDist) {
                        nearestIntersectionPtDist = distline_line;
                        nearestIntersectionSide = 2;
                    }
                }
		// Intersection Second Side
                idneigh=this.triNeighbors.get(
                                                triIndex).get(0);
                if (idneigh!=-1 && !navigationHistory.contains(idneigh)) {
                    distline_line=propagationLine.distance(new LineSegment(bTri, cTri));
                    if (distline_line<FastObstructionTest.epsilon &&
                            distline_line < nearestIntersectionPtDist) {
                            nearestIntersectionPtDist = distline_line;
                            nearestIntersectionSide = 0;
                    }
                }

		// Intersection Third Side
                idneigh=this.triNeighbors.get(
                                                triIndex).get(1);
                if (idneigh!=-1 && !navigationHistory.contains(idneigh)) {
                    distline_line=propagationLine.distance(new LineSegment(cTri, aTri));
                    if (distline_line<FastObstructionTest.epsilon &&
                            distline_line < nearestIntersectionPtDist) {
                            nearestIntersectionSide = 1;
                    }
                }
		if (nearestIntersectionSide != -1) {
			return this.triNeighbors.get(triIndex).get(nearestIntersectionSide);
		} else {
			return -1;
		}
	}

	/**
	 * Fast dot in triangle test
	 * 
	 * @see http://www.blackpawn.com/texts/pointinpoly/default.html
	 * @param p
	 *            Coordinate of the point
	 * @param a
	 *            Coordinate of the A vertex of triangle
	 * @param b
	 *            Coordinate of the B vertex of triangle
	 * @param c
	 *            Coordinate of the C vertex of triangle
	 * @return
	 */
	private boolean dotInTri(Coordinate p, Coordinate a, Coordinate b,
			Coordinate c) {
		Vector2D v0 = new Vector2D(c.x - a.x, c.y - a.y);
		Vector2D v1 = new Vector2D(b.x - a.x, b.y - a.y);
		Vector2D v2 = new Vector2D(p.x - a.x, p.y - a.y);

		// Compute dot products
		double dot00 = v0.dot(v0);
		double dot01 = v0.dot(v1);
		double dot02 = v0.dot(v2);
		double dot11 = v1.dot(v1);
		double dot12 = v1.dot(v2);

		// Compute barycentric coordinates
		double invDenom = 1 / (dot00 * dot11 - dot01 * dot01);
		double u = (dot11 * dot02 - dot01 * dot12) * invDenom;
		double v = (dot00 * dot12 - dot01 * dot02) * invDenom;

		// Check if point is in triangle
		return (u > (0. - epsilon)) && (v > (0. - epsilon))
				&& (u + v < (1. + epsilon));

	}

	Coordinate[] getTriangle(int triIndex) {
		final Triangle tri = this.triVertices.get(triIndex);
		Coordinate[] coords = { this.vertices.get(tri.getA()),
				this.vertices.get(tri.getB()), this.vertices.get(tri.getC()) };
		return coords;
	}

	/**
	 * Return the triangle id from a point coordinate inside the triangle
	 * 
	 * @param pt
	 * @return Triangle Id, Or -1 if no triangle has been found
	 */

	private int getTriangleIdByCoordinate(Coordinate pt) {
		// Shortcut, test if the last found triangle contain this point, if not
		// use the quadtree
		Coordinate[] trit = getTriangle(lastFountPointTriTest);
		if (dotInTri(pt, trit[0], trit[1], trit[2])) {
			return lastFountPointTriTest;
		}
		Envelope ptEnv = new Envelope(pt);
		Iterator<Integer> res = triIndex.query(new Envelope(ptEnv));
		while (res.hasNext()) {
                        int triId = res.next();
			Coordinate[] tri = getTriangle(triId);
			if (dotInTri(pt, tri[0], tri[1], tri[2])) {
				lastFountPointTriTest = triId;
				return triId;
			}
		}
		return -1;
	}

	/**
	 * Add open angle to verticesAngle array (merge with existing open angle if
	 * exists)
	 * 
	 * @param vertexId
	 *            Index of vertex in the verticesAngle array
	 * @param vertexCoordinate
	 *            Coordinate of the vertex
	 * @param left
	 *            CCW Neighbor 1 of vertex (open angle)
	 * @param right
	 *            CCW Neighbor 2 of vertex (open angle)
	 * @param verticesAngle
	 *            Array of Array of open angle
	 */
	public static void updateMinMax(int vertexId, Coordinate vertexCoordinate,
			Coordinate left, Coordinate right,
			ArrayList<ArrayList<Double>> verticesAngle) {
		ArrayList<Double> curVertex = verticesAngle.get(vertexId);
		Coordinate refPosition = new Coordinate(vertexCoordinate.x + 1,
				vertexCoordinate.y);
		double ccw1 = (float) Angle.angleBetweenOriented(refPosition,
				vertexCoordinate, left);
		double ccw2 = (float) Angle.angleBetweenOriented(refPosition,
				vertexCoordinate, right);
		// Iterate over the open angle array and find something ~ equal to
		// ccw1/ccw2 angle
		boolean inserted = false;
		// Update existing angle ranges
		boolean doNewLoop = true;
		while (doNewLoop) {
			doNewLoop = false;
			for (int idrange = 0; idrange < curVertex.size() - 1; idrange += 2) {
				if (curVertex.get(idrange).compareTo(ccw2) == 0) {
					inserted = true;
					if (curVertex.size() > 2) {
						// Remove merged element and reloop
						doNewLoop = true;
						inserted = false;
						ccw2 = curVertex.get(idrange + 1);
						curVertex.remove(idrange);
						curVertex.remove(idrange);
					} else {
						curVertex.set(idrange, ccw1);
					}
					break;
				} else if (curVertex.get(idrange + 1).compareTo(ccw1) == 0) {
					inserted = true;
					if (curVertex.size() > 2) {
						// Remove merged element and reloop
						doNewLoop = true;
						inserted = false;
						ccw1 = curVertex.get(idrange);
						curVertex.remove(idrange);
						curVertex.remove(idrange);
					} else {
						curVertex.set(idrange + 1, ccw2);
					}
					break;
				}
			}
		}
		// Angles not contiguous
		if (!inserted) {
			curVertex.add(ccw1);
			curVertex.add(ccw2);
		}
	}

	/**
	 * 
	 * @param minAngle
	 *            Minimum angle [0-2Pi]
	 * @param maxAngle
	 *            Maximum angle [0-2Pi]
	 * @return List of corners within parameters range
	 */
	public List<Coordinate> getWideAnglePoints(double minAngle, double maxAngle) {
		List<Coordinate> wideAnglePts = new ArrayList<Coordinate>(
				vertices.size());
		if (verticesOpenAngle == null) {
			verticesOpenAngle = new ArrayList<Float>(vertices.size()); // Reserve
																		// size
			verticesOpenAngleTranslated = new ArrayList<Coordinate>(
					vertices.size());
			// Vertex open angle. For each vertex
			// [ccwmin,ccwmax,ccwmin,ccwmax,..]
			ArrayList<ArrayList<Double>> verticesOpenAnglesTuples = new ArrayList<ArrayList<Double>>(
					vertices.size());
			for (int idvert = 0; idvert < vertices.size(); idvert++) {
				verticesOpenAngle.add(0.f);
				verticesOpenAnglesTuples.add(new ArrayList<Double>());
			}
			int triId = 0;
			for (Triangle tri : this.triVertices) {
				// Compute angle at each corner, then add to vertices angle
				// array
				Coordinate triA = vertices.get(tri.getA());
				Coordinate triB = vertices.get(tri.getB());
				Coordinate triC = vertices.get(tri.getC());
				// Add A vertex min/max angle
				updateMinMax(tri.getA(), triA, triB, triC,
						verticesOpenAnglesTuples);
				verticesOpenAngle.set(tri.getA(),
						(float) (verticesOpenAngle.get(tri.getA()) + Angle
								.angleBetween(triB, triA, triC)));
				// Add B vertex angle
				updateMinMax(tri.getB(), triB, triC, triA,
						verticesOpenAnglesTuples);
				verticesOpenAngle.set(tri.getB(),
						(float) (verticesOpenAngle.get(tri.getB()) + Angle
								.angleBetween(triA, triB, triC)));
				// Add C vertex angle
				updateMinMax(tri.getC(), triC, triA, triB,
						verticesOpenAnglesTuples);
				verticesOpenAngle.set(tri.getC(),
						(float) (verticesOpenAngle.get(tri.getC()) + Angle
								.angleBetween(triB, triC, triA)));
				triId++;
			}

			for (int idvert = 0; idvert < vertices.size(); idvert++) {
				// Compute median angle of open angle point
				ArrayList<Double> curvert = verticesOpenAnglesTuples
						.get(idvert);
				Coordinate curVert = vertices.get(idvert);
				if (curvert.size() == 2) {
					double ccw1 = curvert.get(0);
					double ccw2 = curvert.get(1);
					if (ccw1 > ccw2) {
						ccw1 = ccw1 - (2 * Math.PI);
					}
					double midAngle = ((ccw2 - ccw1) / 2.) + ccw1;
					verticesOpenAngleTranslated.add(new Coordinate(curVert.x
							+ (Math.cos(midAngle) * wideAngleTranslationEpsilon), curVert.y
							+ (Math.sin(midAngle) * wideAngleTranslationEpsilon)));
				} else {
					verticesOpenAngleTranslated.add(curVert);
				}
			}
		}
		int idvert = 0;
		for (Float angleVertex : verticesOpenAngle) {
			if (angleVertex >= minAngle && angleVertex <= maxAngle) {
				wideAnglePts.add(verticesOpenAngleTranslated.get(idvert));
			}
			idvert++;
		}
		return wideAnglePts;
	}

	/**
	 * Compute the list of segments corresponding to holes and domain limitation
	 * 
	 * @param maxDist
	 *            Maximum distance from origin to segments
	 * @param p1
	 *            Origin of search
	 * @return List of segment
	 */
	public LinkedList<LineSegment> getLimitsInRange(double maxDist,
			Coordinate p1) {
		LinkedList<LineSegment> walls = new LinkedList<LineSegment>();
		int curTri = getTriangleIdByCoordinate(p1);
		int nextTri = -1;
		short firstSide = 0;
		HashSet<Integer> navigationHistory = new HashSet<Integer>(); // List all
																		// triangles
																		// already
																		// processed
		Stack<Integer> navigationNodes = new Stack<Integer>(); // List the
																// current queue
																// of triangles
																// the process
																// go through
		Stack<Short> navigationSide = new Stack<Short>(); //History of current processing side
		
		while (curTri != -1) {
			navigationHistory.add(curTri);
			// for each side of the triangle
			Triangle neighboors = this.triNeighbors.get(curTri);
			nextTri = -1;
			for (short idside = firstSide; idside < 3; idside++) {
				if (!navigationHistory.contains(neighboors.get(idside))) {
					IntSegment segVerticesIndex = this.triVertices.get(curTri)
							.getSegment(idside);
					LineSegment side = new LineSegment(
							this.vertices.get(segVerticesIndex.getA()),
							this.vertices.get(segVerticesIndex.getB()));
					Coordinate closestPoint = side.closestPoint(p1);
					if (closestPoint.distance(p1) <= maxDist) {
						// In this direction there is a hole or this is outside
						// of the geometry
						if (neighboors.get(idside) == -1) {
							walls.add(side);
						} else {
							// Store currentTriangle Id. This is where to go
							// back when there is no more navigable neighbors at
							// the next triangle
							navigationNodes.add(curTri);
							navigationSide.add(idside);
							firstSide=0;
							nextTri = neighboors.get(idside);
							break; // Next triangle
						}
					}
				}
			}
			if (nextTri == -1 && !navigationNodes.empty()) {
				// All the side have been rejected, go back by one on the
				// navigation
				nextTri = navigationNodes.pop();
				firstSide = navigationSide.pop();
			}
			curTri = nextTri;
		}
		return walls;
	}

	public boolean isFreeField(Coordinate p1, Coordinate p2) {
		nbObstructionTest++;
		LineSegment propaLine = new LineSegment(p1, p2);
		int curTri = getTriangleIdByCoordinate(p1);
		HashSet<Integer> navigationHistory = new HashSet<Integer>();
		while (curTri != -1) {
			navigationHistory.add(curTri);
			Coordinate[] tri = getTriangle(curTri);
			if (dotInTri(p2, tri[0], tri[1], tri[2])) {
				return true;
			}
			curTri = this.getNextTri(curTri, propaLine, navigationHistory);
		}
		return false;
	}
}
