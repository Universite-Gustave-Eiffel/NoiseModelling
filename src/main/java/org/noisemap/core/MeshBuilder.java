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


import java.util.LinkedList;
import java.util.List;


import org.grap.utilities.EnvelopeUtil;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import java.util.*;


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
        private LinkedList<PolygonWithHeight> polygonWithHeight= new LinkedList<PolygonWithHeight>();//list polygon with height
        private HashMap <Integer,PolygonWithHeight> buildingWithID=new HashMap<Integer,PolygonWithHeight>();//list to save all of buildings(both new polygon and old polygon) when do the merge building.
        private Envelope geometriesBoundingBox=null;
        private LinkedList<Coordinate> topoPoints=new LinkedList<Coordinate>();

   
        private Quadtree ptQuadForMergeBuilding = new Quadtree();//Quad tree to test intersection between exist buildings and new building
        public static class PolygonWithHeight{
            private Geometry geo;
            //If we add the topographic, the building height will be the average ToPo Height+ Building Height of all vertices  
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
            public void setHeight(Double height){
                this.height=height;
            }
        }
        
        
	public MeshBuilder() {
		super();
	}
	public long getNbObstructionTest() {
		return nbObstructionTest;
	}
	/**
	 * Retrieve triangle list
	 * @return
	 */
	public List<Triangle> getTriangles() {
		return triVertices;
	}

 	/**
	 * Retrieve neighbors triangle list
	 * @return
	 */
	public List<Triangle> getTriNeighbors() {
		return triNeighbors;
	}
        
	/**
	 * Retrieve vertices list
	 * @return
	 */
	public List<Coordinate> getVertices() {
		return vertices;
	}
        
        
	/**
	 * Retrieve Buildings polygon with the height 
         * retrun the polygons(merged)  with a height "without" the effect Topograhic.
	 * @return
	 */        
        public LinkedList<PolygonWithHeight> getPolygonWithHeight(){
                return polygonWithHeight;
        
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
        
        /**
         * Add the Topograhic Point in the mesh data, to complet the topograhic data.
         * @param point 
         */
        public void addTopograhicPoint(Coordinate point){
            
                if(!topoPoints.contains(point)){
                    if(Double.isNaN(point.z))
                    {
                        point.setCoordinate(new Coordinate(point.x,point.y,0.));
                    }
                    this.topoPoints.add(point);
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
		
		LayerJDelaunay delaunayTool = new LayerJDelaunay();
                //add buildings to JDelaunay
                for(int i=1;i<=polygonWithHeight.size();i++){
                    explodeAndAddPolygon(polygonWithHeight.get(i-1).getGeometry(), delaunayTool,i);
                }
                //add topoPoints to JDelaunay
                //no check if the point in the building
                if(!topoPoints.isEmpty()){
                    for(int j=0;j<topoPoints.size();j++){

                        delaunayTool.addTopoPoint(topoPoints.get(j));

                    }
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
		//Process delaunay Triangulation
		delaunayTool.setMinAngle(0.);
                //computeNeighbors
		delaunayTool.setRetrieveNeighbors(true);
		delaunayTool.processDelaunay();
                // Get results
		this.triVertices = delaunayTool.getTriangles();
		this.vertices = delaunayTool.getVertices();
		this.triNeighbors = delaunayTool.getNeighbors();


	}

	

        //function just for test MergePolygon
        public void testMergeGetPolygonWithHeight(){
            
            for(PolygonWithHeight polygon:polygonWithHeight){
                System.out.println("Polygon is:"+ polygon.getGeometry().toString());
                System.out.println("Building height is:"+ polygon.getHeight());
            }
        }

}