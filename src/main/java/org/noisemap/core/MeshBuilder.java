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
import com.vividsolutions.jts.index.quadtree.Quadtree;
import java.util.*;
import static org.noisemap.core.FastObstructionTest.epsilon;
import static org.noisemap.core.FastObstructionTest.updateMinMax;
import static org.noisemap.core.FastObstructionTest.wideAngleTranslationEpsilon;

/**
 * MeshBuilder is a Delaunay Structure.
 * TODO enable add and query of geometry object (other than
 * fitting elements) into the delaunay triangulation. 
 * It can also add the point with height to complet the mesh with the topograhic
 * @author SU Qi
 */

    

public class MeshBuilder {
	public static final double epsilon = 1e-7;
	public static final double wideAngleTranslationEpsilon = 0.01;
	private long nbObstructionTest=0;
	private List<Triangle> triVertices;
	private List<Coordinate> vertices;
	private List<Triangle> triNeighbors; // Neighbors
	private LinkedList<Double> height= new  LinkedList<Double>(); // Height of Polygon
        private LinkedList<PolygonWithHeight> polygonWithHeight= new LinkedList<PolygonWithHeight>();//list polygon with height
        private HashMap <Integer,PolygonWithHeight> buildingWithID=new HashMap<Integer,PolygonWithHeight>();//list to save all of buildings(both new polygon and old polygon) when do the merge building.
        private Envelope geometriesBoundingBox=null;

	private QueryGeometryStructure triIndex = null; //TODO remove
	private int lastFountPointTriTest = 0;
	private List<Float> verticesOpenAngle = null;
	private List<Coordinate> verticesOpenAngleTranslated = null; /*Open angle*/
        //private LinkedList<Integer> BuildingTriangleIndex= new LinkedList<Integer>(); /* the buildings list between source and receiver. Reconstruction after get a new source-reciver */
        //private LinkedList<Coordinate> pointsIntersection= new LinkedList<Coordinate>();/* the intersection of the segment source-receiver and builiding's side. Reconstruction after get a new source-reciver */
        private Quadtree ptQuadForMergeBuilding = new Quadtree();//
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
            public void setGeometry(Geometry geo){
                this.geo=geo;
            }
            
        }
        
         private static class TriIdWithIntersection{
            private int triID;
            private Coordinate coorIntersection;
            public TriIdWithIntersection(int triID, Coordinate coorIntersection){
            
                this.triID=triID;
                this.coorIntersection=coorIntersection;
            }
            public int gettriID(){
            
                return this.triID;
            }
            public Coordinate getcoorIntersection(){
                return this.coorIntersection;
            }
        
        }
        
	public MeshBuilder() {
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
                //no height defined, set it to Max value
                polygonWithHeight.add(new PolygonWithHeight(obstructionPoly, Double.MAX_VALUE));
	}
   
        
        /**
         * Add a new building with height and merge this new building with existing buildings if they have intersections 
         * When we merge the buildings, we will use The shortest height to new building
         * @param obstructionPoly
	 *            building's Geometry
	 * @param heightofBuilding
	 *            buidling's Height
         * @return
         */
                @SuppressWarnings("unchecked")
      	public void addGeometry(Geometry obstructionPoly, double heightofBuilding) {
                PolygonWithHeight newbuilding=new PolygonWithHeight(obstructionPoly, heightofBuilding); 
           	if(this.geometriesBoundingBox==null) {
			this.geometriesBoundingBox=new Envelope(obstructionPoly.getEnvelopeInternal());
		} else {
			this.geometriesBoundingBox.expandToInclude(obstructionPoly.getEnvelopeInternal());
		}
                //if there is no building 
                if(buildingWithID.isEmpty()){
                    polygonWithHeight.add(newbuilding);
                    buildingWithID.put(buildingWithID.size(),newbuilding);
                    //add this building to QuadTree
                    ptQuadForMergeBuilding.insert(obstructionPoly.getEnvelopeInternal(),new EnvelopeWithIndex<Integer>(obstructionPoly.getEnvelopeInternal(),
				buildingWithID.size()-1));
                    
                }
                else{
                    //check if a new building have the intersection with other buildings
                    List<EnvelopeWithIndex<Integer>> result = ptQuadForMergeBuilding.query(obstructionPoly.getEnvelopeInternal());
                    //if no intersection 
                    if (result.isEmpty()){
                        polygonWithHeight.add(newbuilding);
                        buildingWithID.put(buildingWithID.size(),newbuilding);
                        ptQuadForMergeBuilding.insert(obstructionPoly.getEnvelopeInternal(),new EnvelopeWithIndex<Integer>(obstructionPoly.getEnvelopeInternal(),
				buildingWithID.size()-1));
                    }
                    //if we may have intersection, get the building who intersected with this new building using ID
                    //we use the less height building's height and give it to the intersected Geo
                    else{
                            Geometry newBuildingModified=obstructionPoly;
                            double minHeight=heightofBuilding;
                            for(EnvelopeWithIndex<Integer> envel : result){
                                int intersectedBuildingID=envel.getId();
                                PolygonWithHeight intersectedBuilidng=buildingWithID.get(intersectedBuildingID);
                                //if new Polygon interset old Polygon
                                if(intersectedBuilidng.getGeometry().intersects(obstructionPoly)){
                                    //we merge the building and give it a new height
                                    newBuildingModified=intersectedBuilidng.getGeometry().union(newBuildingModified);
                                    if (minHeight>intersectedBuilidng.getHeight()){
                                    //if the new building's height less than old intersected building, we get the min height 
                                        minHeight=intersectedBuilidng.getHeight();
                                        
                                    }
                                    //if we are sure a old building have intersection with new building, 
                                    //we will remove the old building in the building list and QuadTree(not remove in buildingWithID list)
                                    polygonWithHeight.remove(intersectedBuilidng);
                                    ptQuadForMergeBuilding.remove(intersectedBuilidng.getGeometry().getEnvelopeInternal(),new EnvelopeWithIndex<Integer>(intersectedBuilidng.getGeometry().getEnvelopeInternal(),
				intersectedBuildingID));
                                    
                                        
                                 }
                                 
                                     
                            }
                            PolygonWithHeight newPoly=new PolygonWithHeight(newBuildingModified,minHeight);
                            polygonWithHeight.add(newPoly);
                            buildingWithID.put(buildingWithID.size(), newPoly);
                            //Because we dont remove the building in HashMap buildingWithID, so the buildingWithID will keep both new or old bulding
                            ptQuadForMergeBuilding.insert(newBuildingModified.getEnvelopeInternal(),new EnvelopeWithIndex<Integer>(newBuildingModified.getEnvelopeInternal(),
				buildingWithID.size()-1));

                    }
                }
                
                
                
                
        }
              
        private void addPolygon(Polygon newpoly, LayerJDelaunay delaunayTool,
			 int buildingID) throws LayerDelaunayError {
		delaunayTool.addPolygon(newpoly, true, buildingID);
	}
        
	private void explodeAndAddPolygon(Geometry intersectedGeometry,
			LayerJDelaunay delaunayTool, int buildingID)
			throws LayerDelaunayError {
                
		if (intersectedGeometry instanceof MultiPolygon
				|| intersectedGeometry instanceof GeometryCollection) {
                 
                        
                        for (int j = 0; j < intersectedGeometry.getNumGeometries(); j++) {
			Geometry subGeom = intersectedGeometry.getGeometryN(j);
			explodeAndAddPolygon(subGeom, delaunayTool,buildingID);
               
			}
		} else if (intersectedGeometry instanceof Polygon) {
                        addPolygon((Polygon) intersectedGeometry, delaunayTool,buildingID);
		} else if (intersectedGeometry instanceof LineString) {
			delaunayTool.addLineString((LineString) intersectedGeometry,buildingID);
		}
	}

	// feeding
        @SuppressWarnings("unchecked")
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
		
                //add buildings to JDelaunay
                for(int i=1;i<=polygonWithHeight.size();i++){
                    //element's property deafult is 0 so we use from 1 to give the buildingID
                    //e.x: building ID 1=polygonWithHeight.get(0)
                    explodeAndAddPolygon(polygonWithHeight.get(i-1).getGeometry(), delaunayTool,i);
                }


		// Insert the main rectangle
		Geometry linearRing = EnvelopeUtil.toGeometry(this.geometriesBoundingBox);
		if (!(linearRing instanceof LinearRing)) {
			return;
		}
		GeometryFactory factory = new GeometryFactory();
		Polygon boundingBox = new Polygon((LinearRing) linearRing, null,
				factory);
		delaunayTool.addPolygon(boundingBox, false);
                //explodeAndAddPolygon(allbuilds, delaunayTool);
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
                //add: search triangle without height
                if (idneigh!=-1 && !navigationHistory.contains(idneigh) && this.triNeighbors.get(triIndex).getBuidlingID()==0) {
                    distline_line=propagationLine.distance(new LineSegment(aTri, bTri));
                    if (distline_line<FastObstructionTest.epsilon &&
                            distline_line < nearestIntersectionPtDist && this.triVertices.get(idneigh).getBuidlingID()==0) {
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
                            distline_line < nearestIntersectionPtDist && this.triVertices.get(idneigh).getBuidlingID()==0) {
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
                            distline_line < nearestIntersectionPtDist && this.triVertices.get(idneigh).getBuidlingID()==0) {
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
	 * Compute the next triangle index.Find the shortest intersection point of
	 * triIndex segments to the p1 coordinate and add the triangles in building to the list
         * and add the point of intersection(between segment of source-reciver and segment of triangle) to the list
	 * 
	 * @param triIndex
	 *            Triangle index
	 * @param propagationLine
	 *            Propagation line
	 * 
	 */
	private TriIdWithIntersection getNextTriWithIntersection(final int triIndex,
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
                Coordinate intersection=new Coordinate();
                if (idneigh!=-1 && !navigationHistory.contains(idneigh)) {
                    distline_line=propagationLine.distance(new LineSegment(aTri, bTri));
                    if (distline_line<FastObstructionTest.epsilon &&
                            distline_line < nearestIntersectionPtDist) {
                        nearestIntersectionPtDist = distline_line;
                        nearestIntersectionSide = 2;
                        if(tri.getBuidlingID()>0||this.triVertices.get(this.triNeighbors.get(triIndex).get(nearestIntersectionSide)).getBuidlingID()>0){
                            intersection=propagationLine.intersection(new LineSegment(aTri, bTri));
                        }
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
                            if(tri.getBuidlingID()>0||this.triVertices.get(this.triNeighbors.get(triIndex).get(nearestIntersectionSide)).getBuidlingID()>0){
                                intersection=propagationLine.intersection(new LineSegment(bTri, cTri));
                            }
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
                            if(tri.getBuidlingID()>0||this.triVertices.get(this.triNeighbors.get(triIndex).get(nearestIntersectionSide)).getBuidlingID()>0){
                                intersection=propagationLine.intersection(new LineSegment(cTri, aTri));
                            }
                    }
                }
                
                int BuildingTriID=this.triNeighbors.get(triIndex).get(nearestIntersectionSide);
                boolean triNeighborIsBuidling=false;
                double nextTriHeight=0.;
                if (this.triVertices.get(BuildingTriID).getBuidlingID()==0){
                    nextTriHeight=0.;
                }
                else if(this.triVertices.get(BuildingTriID).getBuidlingID()>0){
                    nextTriHeight=this.polygonWithHeight.get(this.triVertices.get(BuildingTriID).getBuidlingID()-1).getHeight();
                }
                
                if(tri.getBuidlingID()>0 &&(nextTriHeight>0)){
                    //intersection is in the building
                    triNeighborIsBuidling=true;
                }
                //add height to this intersection
                if(tri.getBuidlingID()==0&&nextTriHeight>0){
                    intersection.z=nextTriHeight;
                }
                else if(tri.getBuidlingID()>0&&nextTriHeight>0){
                    intersection.z=Math.max(nextTriHeight, this.polygonWithHeight.get(tri.getBuidlingID()-1).getHeight());
                }
                else if(tri.getBuidlingID()>0&&nextTriHeight==0){
                    intersection.z=this.polygonWithHeight.get(tri.getBuidlingID()-1).getHeight();
                }
                else if(tri.getBuidlingID()==0&&nextTriHeight==0){
                    intersection.z=0.;
                }
                
		if (nearestIntersectionSide != -1) {
                    /*
                    //if the nearest triangle in the building, save this triangle to building list
                         if(this.triVertices.get(BuildingTriID).getHeight()!=0 &&!BuildingTriangleIndex.contains(BuildingTriID)){
                              BuildingTriangleIndex.add(BuildingTriID);
                         }
                    //if intersection is not in the building, save this intersection to intersection list
                    */    
                    if(!triNeighborIsBuidling&&!intersection.equals3D(new Coordinate(0.0,0.0,0.))){
                                  //every buidling whcih is between ray source-receiver have 2 intersections, 
                                  //if the intersection is corner of the buiding, pointsIntersection will save 2 times with the same value  
                                  return new TriIdWithIntersection(BuildingTriID,intersection);
                    }
                    else{
                                  return new TriIdWithIntersection(BuildingTriID,new Coordinate(-1,-1,-1));
                    }
			 
		} else {
			 return new TriIdWithIntersection(-1,new Coordinate(-1,-1,-1));
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
			
                        
			for (Triangle tri : this.triVertices) {
                            if(tri.getBuidlingID()<1){
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
                            }
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
                                                // In this direction there is a building or this is outside
                                                // of the geometry
                                                if (triVertices.get(neighboors.get(idside)).getBuidlingID()>=1) {
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

        /*
         * compute diffraction.
         */
	public boolean isFreeField(Coordinate p1, Coordinate p2) {
		nbObstructionTest++;
		LineSegment propaLine = new LineSegment(p1, p2);
                
		int curTri = getTriangleIdByCoordinate(p1);
		HashSet<Integer> navigationHistory = new HashSet<Integer>();
                if(this.triVertices.get(curTri).getBuidlingID()==0){
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
                else{
                    return false;
                }
        }
        
        
        /**
         * 
         * Get the distance of all intersections (after the filtration by algo Jarvis March)  between the source and the receiver to compute vertical diffraction 
         * Must called after finishPolygonFeeding
	 * @param p1
	 *            Coordiante receiver
	 * @param p2
	 *            Coordiante source
	 * @return Double list: data prepared to compute diffraction
         *         Double[0]:delta distance;
         *         Doulbe[1]:e;
         *         Double[2]:the heigh of the highest intersection(this one is used to compute Ch, NMPB 2008 page 33, Ch is given by 1 now ,so not useful for now)
         *         Double[3]:the full distance of difrraction path
         *         if Double[0],Double[1],Double[2],Double[3] are -1. then no usefull intersections.
         */
        public Double[] getPath(Coordinate p1, Coordinate p2) {
		//BuildingTriangleIndex.clear();
                
                Double[] data=new Double[4];
		LineSegment propaLine = new LineSegment(p1, p2);
		int curTri = getTriangleIdByCoordinate(p1);
                LinkedList<Coordinate> pointsIntersection= new LinkedList<Coordinate>();
		HashSet<Integer> navigationHistory = new HashSet<Integer>();
		while (curTri != -1) {
			navigationHistory.add(curTri);
			Coordinate[] tri = getTriangle(curTri);
			if (dotInTri(p2, tri[0], tri[1], tri[2])) {
				break;
			}
                        TriIdWithIntersection tirIDWithIntersection=this.getNextTriWithIntersection(curTri, propaLine, navigationHistory);
			curTri=tirIDWithIntersection.gettriID();
                        Coordinate coorIntersection=tirIDWithIntersection.getcoorIntersection();
                        if(!coorIntersection.equals(new Coordinate(-1,-1,-1))){
                            pointsIntersection.add(coorIntersection);
                        }
		}

		
                //add point receiver and point source into list head and tail.
                pointsIntersection.addFirst(p1);
                pointsIntersection.addLast(p2);
                //change Coordinate system from 3D to 2D 
                LinkedList<Coordinate> newPoints= getNewCoordinateSystem(pointsIntersection);
                double[] pointsX;
                pointsX=new double[newPoints.size()];
                double[] pointsY;
                pointsY=new double[newPoints.size()];
                for(int i=0;i<newPoints.size();i++){
                    pointsX[i]=newPoints.get(i).x;
                    if(newPoints.get(i).y!=Double.NaN){
                        pointsY[i]=newPoints.get(i).y;
                    }
                    else{
                        pointsY[i]=0.;
                    }
                            
                }
                //algo JarvisMarch to get the convex hull           
                JarvisMarch jm=new JarvisMarch(new JarvisMarch.Points(pointsX,pointsY));
                JarvisMarch.Points points=jm.calculateHull();
                //if there are no useful intersection 
                if(points.x.length<=2){
                    data[0]=-1.;
                    data[1]=-1.;
                    data[2]=-1.;
                    data[3]=-1.;
                    return data;
                
                }
                else{
                    LinkedList<LineSegment> path=new LinkedList<LineSegment>(); 
                    for (int i=0;i<points.x.length-1;i++){
                        
                        path.add(new LineSegment(new Coordinate(points.x[i],points.y[i]),new Coordinate(points.x[i+1],points.y[i+1])));
                        //When we get a point we will check if this point is equal with P2 we will stop finding next point 
                        if(p2.equals(new Coordinate(points.x[i],points.y[i]))){
                            break;
                        }

                    }
                    double distancepath=0.0;//distance of path

                    //prepare data to compute pure diffraction
                    //h0 in expression diffraction:the highest point intersection
                    double heightpoint=0.0;
                    for(int i=0;i<path.size();i++){
                        distancepath=path.get(i).getLength()+distancepath;
                        if(path.get(i).p0.y>heightpoint){
                            heightpoint=path.get(i).p0.y;
                        }
                    }
                    //we used coordinate after change coordinate system to get the right distance.
                    double distanceRandS=path.getFirst().p0.distance(path.getLast().p1);//distance of receiver and source
                    double e=distancepath-path.getFirst().getLength()-path.getLast().getLength();//distance without first part path and last part path
                    double deltadistance=distancepath-distanceRandS;//delt distance


                    data[0]=deltadistance;
                    data[1]=e;
                    data[2]=heightpoint;
                    data[3]=distancepath;
                    return data;
                
                }
                
	}
        /*
         * 
         * get Triangles(in buildings)'s coordiante, must called after setTriBuildingList
      
        public LinkedList<Coordinate[]> getTriBuildingCoordinate(){
            LinkedList<Coordinate[]> TriBuilding= new LinkedList<Coordinate[]>();
            for(int i: BuildingTriangleIndex){
                TriBuilding.add(getTriangle(i));
            }
           
            return TriBuilding;
        }
        */
        
        
        /*
         * this function just for testing the height of building
         * get Heights of Triangles of Building, must called after setTriBuildingList
       
        public LinkedList<Double> getTriBuildingHeight(){
            LinkedList<Double> TriBuildingHeight= new LinkedList<Double>();
            for(int i:BuildingTriangleIndex){
                TriBuildingHeight.add(this.triVertices.get(i).getHeight());
            }
            return TriBuildingHeight;
        }
        */
        
        /*
         * get coordiantes(with height) of all intersections
         * 
         */
        /*
        private LinkedList<Coordinate> getIntersection(){
            LinkedList<Coordinate> intersection=new LinkedList<Coordinate>();
            for(Coordinate inter:this.pointsIntersection){
                intersection.add(inter);
            }
            
            return intersection;
        
        } 
        */
        

        
        /*
        
        
        public LinkedList<Coordinate> getListofIntersection(Coordinate p1, Coordinate p2){
            LinkedList<Coordinate> list=new LinkedList<Coordinate>();
            for(PolygonWithHeight building:polygonwithheight){
                        
                       
                 
            }
            
            return list; 
        }
        */
        
        
        /*
         * changeCoordinateSystem, use original coordinate in 3D to change into a new markland in 2D with new x' and y' is original height of point
         * http://en.wikipedia.org/wiki/Rotation_matrix
         * http://read.pudn.com/downloads93/ebook/364220/zbzh.pdf
        */
        private LinkedList<Coordinate> getNewCoordinateSystem(LinkedList<Coordinate> listpoints){
            LinkedList<Coordinate> newcoord=new LinkedList<Coordinate>();
            //get angle by ray source-receiver with the X-axis.
            double angle=new LineSegment(listpoints.getFirst(),listpoints.getLast()).angle(); 
            double sin=Math.sin(angle);
            double cos=Math.cos(angle);
                
            for(int i=0;i<listpoints.size();i++){
                double newX=(listpoints.get(i).x-listpoints.get(0).x)*cos+(listpoints.get(i).y-listpoints.get(0).y)*sin;
                newcoord.add(new Coordinate(newX,listpoints.get(i).z));
            
            
            }
    
            return newcoord;
        
        }
        //function just for test MergePolygon
        public void testMergeGetPolygonWithHeight(){
            
            for(PolygonWithHeight polygon:polygonWithHeight){
                System.out.println("Polygon is:"+ polygon.getGeometry().toString());
                System.out.println("Building height is:"+ polygon.getHeight());
            }
        }

}