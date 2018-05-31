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


import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.strtree.STRtree;

import java.util.ArrayList;
import java.util.LinkedList;
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
    private static final int BUILDING_COUNT_HINT = 1500; // 2-3 km² average buildings
    private List<PolygonWithHeight> polygonWithHeight = new ArrayList<>(BUILDING_COUNT_HINT);//list polygon with height
    private List<LineString> envelopeSplited = new ArrayList<>();
    private Envelope geometriesBoundingBox = null;
    private List<Coordinate> topoPoints = new LinkedList<Coordinate>();
    private boolean computeNeighbors = true;
    private double maximumArea = 0;
    private GeometryFactory factory = new GeometryFactory();
    private static final int EPSILON_MESH = 2; //Decimal value, Used for merged geometry precision

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

    public void mergeBuildings(Geometry boundingBoxGeom) {
        // Delaunay triangulation request good quality input data
        // We have to merge buildings that may overlap
        Geometry[] toUnion = new Geometry[polygonWithHeight.size() + 1];
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
        if(boundingBoxGeom instanceof Polygon) {
          // Add envelope to union of geometry
          toUnion[i] = ((Polygon)(boundingBoxGeom)).getExteriorRing();
        } else {
          toUnion[i] = factory.createPolygon(new Coordinate[0]);
        }
        Geometry geomCollection = factory.createGeometryCollection(toUnion);
        geomCollection = geomCollection.union();
        List<PolygonWithHeight> mergedPolygonWithHeight = new ArrayList<>(geomCollection.getNumGeometries());
        // For each merged buildings fetch all contained buildings and take the minimal height then insert into mergedPolygonWithHeight
        for(int idGeom = 0; idGeom < geomCollection.getNumGeometries(); idGeom++) {
            //fetch all contained buildings
            Geometry geometryN = geomCollection.getGeometryN(idGeom);
            if(geometryN instanceof Polygon) {
                List polyInters = buildingsRtree.query(geometryN.getEnvelopeInternal());
                double minHeight = Double.MAX_VALUE;
                boolean foundHeight = false;
                for (Object id : polyInters) {
                    if (id instanceof Integer) {
                        PolygonWithHeight inPoly = polygonWithHeight.get((int) id);
                        if (inPoly.hasHeight && inPoly.getGeometry().intersects(geometryN)) {
                            minHeight = Math.min(minHeight, inPoly.getHeight());
                            foundHeight = true;
                        }
                    }
                }
                if(foundHeight) {
                    mergedPolygonWithHeight.add(new PolygonWithHeight(geometryN, minHeight));
                } else {
                    mergedPolygonWithHeight.add(new PolygonWithHeight(geometryN));
                }
            } else if(geometryN instanceof LineString) {
              // Exterior envelope
              envelopeSplited.add((LineString)geometryN);
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

    private void addPolygon(Polygon newpoly, LayerDelaunay delaunayTool,
                            int buildingID) throws LayerDelaunayError {
        // Fix clock wise orientation of the polygon and inner holes
        newpoly.normalize();
        delaunayTool.addPolygon(newpoly, buildingID);
    }

    private void explodeAndAddPolygon(Geometry intersectedGeometry,
                                      LayerDelaunay delaunayTool, int buildingID)
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
        // Insert the main rectangle
        if (!(boundingBoxGeom instanceof Polygon)) {
          return;
        }
        if (boundingBoxGeom != null) {
            this.geometriesBoundingBox = boundingBoxGeom.getEnvelopeInternal();
        }

        LayerDelaunay delaunayTool = new LayerPoly2Tri();
        //merge buildings
        mergeBuildings(boundingBoxGeom);

        //add buildings to delaunay triangulation
        int i = 1;
        for (PolygonWithHeight polygon : polygonWithHeight) {
            explodeAndAddPolygon(polygon.getGeometry(), delaunayTool, i);
            i++;
        }
        for (LineString lineString : envelopeSplited) {
          delaunayTool.addLineString(lineString, -1);
        }
        //add topoPoints to JDelaunay
        //no check if the point in the building
        if (!topoPoints.isEmpty()) {
            for (Coordinate topoPoint : topoPoints) {
                delaunayTool.addVertex(topoPoint);
            }
        }
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
        delaunayTool.processDelaunay();
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