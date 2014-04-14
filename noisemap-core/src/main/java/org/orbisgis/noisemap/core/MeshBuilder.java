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
package org.orbisgis.noisemap.core;


import java.util.HashMap;
import java.util.LinkedList;
import java.util.LinkedList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import org.jdelaunay.delaunay.evaluator.InsertionEvaluator;

import java.util.List;


/**
 * MeshBuilder is a Delaunay Structure builder.
 * TODO enable add and query of geometry object (other than
 * fitting elements) into the delaunay triangulation.
 * It can also add the point with Z to complete the mesh with the topography
 *
 * @author Nicolas Fortin
 * @author SU Qi
 */


public class MeshBuilder {
    private List<Triangle> triVertices;
    private List<Coordinate> vertices;
    private List<Triangle> triNeighbors; // Neighbors
    private InsertionEvaluator insertionEvaluator;
    private LinkedList<PolygonWithHeight> polygonWithHeight = new LinkedList<PolygonWithHeight>();//list polygon with height
    private HashMap<Integer, PolygonWithHeight> buildingWithID = new HashMap<Integer, PolygonWithHeight>();//list to save all of buildings(both new polygon and old polygon) when do the merge building.
    private Envelope geometriesBoundingBox = null;
    private LinkedList<Coordinate> topoPoints = new LinkedList<Coordinate>();


    private Quadtree ptQuadForMergeBuilding = new Quadtree();//Quad tree to test intersection between exist buildings and new building

    public static class PolygonWithHeight {
        private Geometry geo;
        //If we add the topographic, the building height will be the average ToPo Height+ Building Height of all vertices
        private double height;

        public PolygonWithHeight(Geometry geo, double height) {

            this.geo = geo;
            this.height = height;
        }

        public Geometry getGeometry() {

            return this.geo;
        }

        public double getHeight() {
            return this.height;
        }

        public void setGeometry(Geometry geo) {
            this.geo = geo;
        }

        public void setHeight(Double height) {
            this.height = height;
        }
    }

    /**
     * Triangle refinement
     *
     * @param insertionEvaluator
     */
    public void setInsertionEvaluator(InsertionEvaluator insertionEvaluator) {
        this.insertionEvaluator = insertionEvaluator;
    }

    public MeshBuilder() {
        super();
    }

    /**
     * Retrieve triangle list
     *
     * @return
     */
    public List<Triangle> getTriangles() {
        return triVertices;
    }

    /**
     * Retrieve neighbors triangle list
     *
     * @return
     */
    public List<Triangle> getTriNeighbors() {
        return triNeighbors;
    }

    /**
     * @return vertices list
     */
    public List<Coordinate> getVertices() {
        return vertices;
    }


    /**
     * Retrieve Buildings polygon with the height
     * @return the polygons(merged)  with a height "without" the effect Topograhic.
     */
    public LinkedList<PolygonWithHeight> getPolygonWithHeight() {
        return polygonWithHeight;

    }


    public void addGeometry(Geometry obstructionPoly) {
        if (this.geometriesBoundingBox == null) {
            this.geometriesBoundingBox = new Envelope(obstructionPoly.getEnvelopeInternal());
        } else {
            this.geometriesBoundingBox.expandToInclude(obstructionPoly.getEnvelopeInternal());
        }
        //no height defined, set it to Max value
        polygonWithHeight.add(new PolygonWithHeight(obstructionPoly, Double.MAX_VALUE));
    }


    /**
     * Add a new building with height and merge this new building with existing buildings if they have intersections
     * When we merge the buildings, we will use The shortest height to new building
     *
     * @param obstructionPoly  building's Geometry
     * @param heightofBuilding buidling's Height
     */
    @SuppressWarnings("unchecked")
    public void addGeometry(Geometry obstructionPoly, double heightofBuilding) {
        PolygonWithHeight newbuilding = new PolygonWithHeight(obstructionPoly, heightofBuilding);
        if (this.geometriesBoundingBox == null) {
            this.geometriesBoundingBox = new Envelope(obstructionPoly.getEnvelopeInternal());
        } else {
            this.geometriesBoundingBox.expandToInclude(obstructionPoly.getEnvelopeInternal());
        }
        //if there is no building
        if (buildingWithID.isEmpty()) {
            polygonWithHeight.add(newbuilding);
            buildingWithID.put(buildingWithID.size(), newbuilding);
            //add this building to QuadTree
            ptQuadForMergeBuilding.insert(obstructionPoly.getEnvelopeInternal(), new EnvelopeWithIndex<Integer>(obstructionPoly.getEnvelopeInternal(),
                    buildingWithID.size() - 1));

        } else {
            //check if a new building have the intersection with other buildings
            List<EnvelopeWithIndex<Integer>> result = ptQuadForMergeBuilding.query(obstructionPoly.getEnvelopeInternal());
            //if no intersection
            if (result.isEmpty()) {
                polygonWithHeight.add(newbuilding);
                buildingWithID.put(buildingWithID.size(), newbuilding);
                ptQuadForMergeBuilding.insert(obstructionPoly.getEnvelopeInternal(), new EnvelopeWithIndex<Integer>(obstructionPoly.getEnvelopeInternal(),
                        buildingWithID.size() - 1));
            }
            //if we may have intersection, get the building who intersected with this new building using ID
            //we use the less height building's height and give it to the intersected Geo
            else {
                Geometry newBuildingModified = obstructionPoly;
                double minHeight = heightofBuilding;
                for (EnvelopeWithIndex<Integer> envel : result) {
                    int intersectedBuildingID = envel.getId();
                    PolygonWithHeight intersectedBuilidng = buildingWithID.get(intersectedBuildingID);
                    //if new Polygon interset old Polygon && intersection is not a Point
                    if (intersectedBuilidng.getGeometry().intersects(obstructionPoly) && !(intersectedBuilidng.getGeometry().intersection(obstructionPoly) instanceof Point)) {
                        //we merge the building and give it a new height
                        newBuildingModified = intersectedBuilidng.getGeometry().union(newBuildingModified);
                        if (minHeight > intersectedBuilidng.getHeight()) {
                            //if the new building's height less than old intersected building, we get the min height
                            minHeight = intersectedBuilidng.getHeight();

                        }
                        //if we are sure a old building have intersection with new building,
                        //we will remove the old building in the building list and QuadTree(not remove in buildingWithID list)
                        polygonWithHeight.remove(intersectedBuilidng);
                        ptQuadForMergeBuilding.remove(intersectedBuilidng.getGeometry().getEnvelopeInternal(), new EnvelopeWithIndex<Integer>(intersectedBuilidng.getGeometry().getEnvelopeInternal(),
                                intersectedBuildingID));


                    }


                }
                PolygonWithHeight newPoly = new PolygonWithHeight(newBuildingModified, minHeight);
                polygonWithHeight.add(newPoly);
                buildingWithID.put(buildingWithID.size(), newPoly);
                //Because we dont remove the building in HashMap buildingWithID, so the buildingWithID will keep both new or old bulding
                ptQuadForMergeBuilding.insert(newBuildingModified.getEnvelopeInternal(), new EnvelopeWithIndex<Integer>(newBuildingModified.getEnvelopeInternal(),
                        buildingWithID.size() - 1));

            }
        }


    }

    /**
     * Add the Topographic Point in the mesh data, to complet the topograhic data.
     *
     * @param point Topographic Point
     */
    public void addTopographicPoint(Coordinate point) {

        if (!topoPoints.contains(point)) {
            if (Double.isNaN(point.z)) {
                point.setCoordinate(new Coordinate(point.x, point.y, 0.));
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

        if (intersectedGeometry instanceof GeometryCollection) {
            for (int j = 0; j < intersectedGeometry.getNumGeometries(); j++) {
                Geometry subGeom = intersectedGeometry.getGeometryN(j);
                explodeAndAddPolygon(subGeom, delaunayTool, buildingID);
            }
        } else if (intersectedGeometry instanceof Polygon) {
            addPolygon((Polygon) intersectedGeometry, delaunayTool, buildingID);
        } else if (intersectedGeometry instanceof LineString) {
            delaunayTool.addLineString((LineString) intersectedGeometry, buildingID);
        }
    }

    public void finishPolygonFeeding(Envelope boundingBoxFilter) throws LayerDelaunayError {
        finishPolygonFeeding(new GeometryFactory().toGeometry(boundingBoxFilter));
    }

    public void finishPolygonFeeding(Geometry boundingBoxGeom) throws LayerDelaunayError {
        if (boundingBoxGeom != null) {
            this.geometriesBoundingBox = boundingBoxGeom.getEnvelopeInternal();
        }

        LayerJDelaunay delaunayTool = new LayerJDelaunay();
        //add buildings to JDelaunay
        int i = 1;
        for (PolygonWithHeight polygon : polygonWithHeight) {
            explodeAndAddPolygon(polygon.getGeometry(), delaunayTool, i);
            i++;
        }
        //add topoPoints to JDelaunay
        //no check if the point in the building
        if (!topoPoints.isEmpty()) {
            for (Coordinate topoPoint : topoPoints) {
                delaunayTool.addTopoPoint(topoPoint);
            }
        }

        // Insert the main rectangle
        if (!(boundingBoxGeom instanceof Polygon)) {
            return;
        }
        delaunayTool.addPolygon((Polygon) boundingBoxGeom, false);
        //explodeAndAddPolygon(allbuilds, delaunayTool);
        //Process delaunay Triangulation
        delaunayTool.setMinAngle(0.);
        //computeNeighbors
        delaunayTool.setRetrieveNeighbors(true);
        // Refine result
        if(insertionEvaluator != null) {
            delaunayTool.processDelaunay(0.1, insertionEvaluator);
        } else {
            delaunayTool.processDelaunay();
        }
        // Get results
        this.triVertices = delaunayTool.getTriangles();
        this.vertices = delaunayTool.getVertices();
        this.triNeighbors = delaunayTool.getNeighbors();
    }

    //function just for test MergePolygon
    public void testMergeGetPolygonWithHeight() {

        for (PolygonWithHeight polygon : polygonWithHeight) {
            System.out.println("Polygon is:" + polygon.getGeometry().toString());
            System.out.println("Building height is:" + polygon.getHeight());
        }
    }

}