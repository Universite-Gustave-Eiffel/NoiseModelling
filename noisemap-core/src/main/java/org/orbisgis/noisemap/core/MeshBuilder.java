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


import java.util.ArrayList;
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
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.operation.buffer.BufferParameters;
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
    private static final int BUILDING_COUNT_HINT = 1500; // 2-3 km² average buildings
    private List<PolygonWithHeight> polygonWithHeight = new ArrayList<>(BUILDING_COUNT_HINT);//list polygon with height
    private Envelope geometriesBoundingBox = null;
    private List<Coordinate> topoPoints = new LinkedList<Coordinate>();
    private boolean computeNeighbors = true;
    private double maximumArea = 0;
    private GeometryFactory factory = new GeometryFactory();
    private static final double EPSILON_MESH = 0;

    public static class PolygonWithHeight {
        private final Geometry geo;
        //If we add the topographic, the building height will be the average ToPo Height+ Building Height of all vertices
        private double height;
        private final boolean hasHeight;

        public PolygonWithHeight(Geometry geo) {
            this.geo = geo;
            this.height = Double.MAX_VALUE;
            this.hasHeight = false;
        }

        public PolygonWithHeight(Geometry geo, double height) {
            this.geo = geo;
            this.height = height;
            this.hasHeight = true;
        }

        public Geometry getGeometry() {

            return this.geo;
        }

        public double getHeight() {
            return this.height;
        }

        public void setHeight(Double height) {
            this.height = height;
        }

        /**
         * @return True if height property has been set
         */
        public boolean hasHeight() {
            return hasHeight;
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
     * @return the polygons(merged)  with a height "without" the effect Topographic.
     */
    public List<PolygonWithHeight> getPolygonWithHeight() {
        return polygonWithHeight;

    }


    public void addGeometry(Geometry obstructionPoly) {
        addGeometry(new PolygonWithHeight(obstructionPoly));
    }

    private void addGeometry(PolygonWithHeight poly) {
        if (this.geometriesBoundingBox == null) {
            this.geometriesBoundingBox = new Envelope(poly.getGeometry().getEnvelopeInternal());
        } else {
            this.geometriesBoundingBox.expandToInclude(poly.getGeometry().getEnvelopeInternal());
        }
        polygonWithHeight.add(poly);
    }


    /**
     * Add a new building with height and merge this new building with existing buildings if they have intersections
     * When we merge the buildings, we will use The shortest height to new building
     *
     * @param obstructionPoly  building's Geometry
     * @param heightofBuilding building's Height
     */
    @SuppressWarnings("unchecked")
    public void addGeometry(Geometry obstructionPoly, double heightofBuilding) {
        addGeometry(new PolygonWithHeight(obstructionPoly, heightofBuilding));
    }

    public void mergeBuildings() {
        if(polygonWithHeight.isEmpty()) {
            return;
        }
        // Delaunay triangulation request good quality input data
        // We have to merge buildings that may overlap
        Geometry[] toUnion = new Geometry[polygonWithHeight.size()];
        STRtree buildingsRtree;
        if(toUnion.length > 10) {
            buildingsRtree = new STRtree(toUnion.length);
        } else {
            buildingsRtree = new STRtree();
        }
        int i = 0;
        for(PolygonWithHeight poly : polygonWithHeight) {
            toUnion[i] = poly.getGeometry();
            buildingsRtree.insert(poly.getGeometry().getEnvelopeInternal(), i);
            i++;
        }
        Geometry merged = factory.createGeometryCollection(toUnion).buffer(EPSILON_MESH, 0, BufferParameters.CAP_SQUARE );
        List<PolygonWithHeight> mergedPolygonWithHeight = new ArrayList<>(merged.getNumGeometries());
        // For each merged buildings fetch all contained buildings and take the minimal height then insert into mergedPolygonWithHeight
        for(int idGeom = 0; idGeom < merged.getNumGeometries(); idGeom++) {
            //fetch all contained buildings
            Geometry mergedBuilding = merged.getGeometryN(idGeom);
            if(mergedBuilding instanceof Polygon) {
                List polyInters = buildingsRtree.query(mergedBuilding.getEnvelopeInternal());
                double minHeight = Double.MAX_VALUE;
                for (Object id : polyInters) {
                    if (id instanceof Integer) {
                        PolygonWithHeight inPoly = polygonWithHeight.get((int) id);
                        if (inPoly.getGeometry().intersects(mergedBuilding)) {
                            minHeight = Math.min(minHeight, inPoly.getHeight());
                        }
                    }
                }
                mergedPolygonWithHeight.add(new PolygonWithHeight(mergedBuilding, minHeight));
            }
        }
        polygonWithHeight = mergedPolygonWithHeight;
    }

    /**
     * Add the Topographic Point in the mesh data, to complete the topographic data.
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
        //merge buildings
        mergeBuildings();
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
        delaunayTool.setRetrieveNeighbors(computeNeighbors);
        ////////////////////
        // Refine result
        // Triangle area
        if(maximumArea > 0) {
            delaunayTool.setMaxArea(maximumArea);
        }
        if(insertionEvaluator != null) {
            delaunayTool.processDelaunay(0.1, insertionEvaluator);
        } else {
            delaunayTool.processDelaunay();
        }
        // Get results
        this.triVertices = delaunayTool.getTriangles();
        this.vertices = delaunayTool.getVertices();
        if(computeNeighbors) {
            this.triNeighbors = delaunayTool.getNeighbors();
        }
    }

    /**
     * Add a constraint on maximum triangle area.
     * @param maximumArea Value in square meter.
     */
    public void setMaximumArea(double maximumArea) {
        this.maximumArea = Math.max(0, maximumArea);
    }

    public void setComputeNeighbors(boolean computeNeighbors) {
        this.computeNeighbors = computeNeighbors;
    }

    //function just for test MergePolygon
    public void testMergeGetPolygonWithHeight() {

        for (PolygonWithHeight polygon : polygonWithHeight) {
            System.out.println("Polygon is:" + polygon.getGeometry().toString());
            System.out.println("Building height is:" + polygon.getHeight());
        }
    }

}