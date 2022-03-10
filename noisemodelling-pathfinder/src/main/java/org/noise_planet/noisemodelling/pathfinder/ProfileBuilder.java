/*
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
package org.noise_planet.noisemodelling.pathfinder;

import org.locationtech.jts.algorithm.Angle;
import org.locationtech.jts.algorithm.CGAlgorithms3D;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFilter;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.ItemVisitor;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.locationtech.jts.triangulate.quadedge.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.lang.Double.NaN;
import static java.lang.Double.isNaN;
import static org.locationtech.jts.algorithm.Orientation.isCCW;
import static org.noise_planet.noisemodelling.pathfinder.JTSUtility.dist2D;
import static org.noise_planet.noisemodelling.pathfinder.ProfileBuilder.IntersectionType.*;

//TODO use NaN for building height
//TODO fix wall references id in order to use also real wall database key
//TODO check how the wall alpha are set to the cut point
//TODO check how the topo and building height are set to cut point
//TODO check how the building pk is set to cut point
//TODO difference between Z and height (z = height+topo)
//TODO create class org.noise_planet.noisemodelling.pathfinder.ComputeCnossosRays which is a copy of computeRays using ProfileBuilder

/**
 * Builder constructing profiles from buildings, topography and ground effects.
 */
public class ProfileBuilder {
    public static final double epsilon = 1e-7;
    /** Class {@link java.util.logging.Logger}. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileBuilder.class);
    /** Default RTree node capacity. */
    private static final int TREE_NODE_CAPACITY = 5;
    /** {@link Geometry} factory. */
    private static final GeometryFactory FACTORY = new GeometryFactory();
    private static final double DELTA = 1e-3;

    /** If true, no more data can be add. */
    private boolean isFeedingFinished = false;
    /** Wide angle points of a building polygon */
    private Map<Integer, ArrayList<Coordinate>> buildingsWideAnglePoints = new HashMap<>();
    /** Building RTree node capacity. */
    private int buildingNodeCapacity = TREE_NODE_CAPACITY;
    /** Topographic RTree node capacity. */
    private int topoNodeCapacity = TREE_NODE_CAPACITY;
    /** Ground RTree node capacity. */
    private int groundNodeCapacity = TREE_NODE_CAPACITY;
    /**
     * Max length of line part used for profile retrieving.
     * @see ProfileBuilder#getProfile(Coordinate, Coordinate)
     */
    private double maxLineLength = 60;
    /** List of buildings. */
    private final List<Building> buildings = new ArrayList<>();
    /** List of walls. */
    private final List<Wall> walls = new ArrayList<>();
    /** Building RTree. */
    private final STRtree buildingTree;
    /** Building RTree. */
    private final STRtree wallTree = new STRtree(TREE_NODE_CAPACITY);
    /** Global RTree. */
    private STRtree rtree;
    private STRtree groundEffectsRtree = new STRtree(TREE_NODE_CAPACITY);


    /** List of topographic points. */
    private final List<Coordinate> topoPoints = new ArrayList<>();
    /** List of topographic lines. */
    private final List<LineString> topoLines = new ArrayList<>();
    /** Topographic triangle facets. */
    private List<Triangle> topoTriangles = new ArrayList<>();
    /** Topographic triangle neighbors. */
    private List<Triangle> topoNeighbors = new ArrayList<>();
    /** Topographic Vertices .*/
    private List<Coordinate> vertices = new ArrayList<>();
    /** Topographic RTree. */
    private STRtree topoTree;

    /** List of ground effects. */
    private final List<GroundEffect> groundEffects = new ArrayList<>();

    /** Receivers .*/
    private final List<Coordinate> receivers = new ArrayList<>();

    /** List of processed walls. */
    private final List<Wall> processedWalls = new ArrayList<>();

    /** Global envelope of the builder. */
    private Envelope envelope;
    /** Maximum area of triangles. */
    private double maxArea;

    /** if true take into account z value on Buildings Polygons
     * In this case, z represent the altitude (from the sea to the top of the wall) */
    private boolean zBuildings = false;


    public void setzBuildings(boolean zBuildings) {
        this.zBuildings = zBuildings;
    }


    /**
     * Main empty constructor.
     */
    public ProfileBuilder() {
        buildingTree = new STRtree(buildingNodeCapacity);
    }

    //TODO : when a source/receiver are underground, should an offset be applied ?
    /**
     * Constructor setting parameters.
     * @param buildingNodeCapacity Building RTree node capacity.
     * @param topoNodeCapacity     Topographic RTree node capacity.
     * @param groundNodeCapacity   Ground RTree node capacity.
     * @param maxLineLength        Max length of line part used for profile retrieving.
     */
    public ProfileBuilder(int buildingNodeCapacity, int topoNodeCapacity, int groundNodeCapacity, int maxLineLength) {
        this.buildingNodeCapacity = buildingNodeCapacity;
        this.topoNodeCapacity = topoNodeCapacity;
        this.groundNodeCapacity = groundNodeCapacity;
        this.maxLineLength = maxLineLength;
        buildingTree = new STRtree(buildingNodeCapacity);
    }

    /**
     * Add the given {@link Geometry} footprint.
     * @param building Building.
     */
    public ProfileBuilder addBuilding(Building building) {
        if(building.poly == null || building.poly.isEmpty()) {
            LOGGER.error("Cannot add a building with null or empty geometry.");
        }
        else if(!isFeedingFinished) {
            if(envelope == null) {
                envelope = building.poly.getEnvelopeInternal();
            }
            else {
                envelope.expandToInclude(building.poly.getEnvelopeInternal());
            }
            buildings.add(building);
            buildingTree.insert(building.poly.getEnvelopeInternal(), buildings.size());
            return this;
        }
        else{
            LOGGER.warn("Cannot add building, feeding is finished.");
        }
        return null;
    }

    /**
     * Add the given {@link Geometry} footprint.
     * @param geom   Building footprint.
     */
    public ProfileBuilder addBuilding(Geometry geom) {
        return addBuilding(geom, -1);
    }

    /**
     * Add the given {@link Geometry} footprint.
     * @param coords Building footprint coordinates.
     */
    public ProfileBuilder addBuilding(Coordinate[] coords) {
        Coordinate[] polyCoords;
        int l = coords.length;
        if(coords[0] != coords[l-1]) {
            polyCoords = Arrays.copyOf(coords, l+1);
            polyCoords[l] = new Coordinate(coords[0]);
        }
        else {
            polyCoords = coords;
        }
        return addBuilding(FACTORY.createPolygon(polyCoords), -1);
    }

    /**
     * Add the given {@link Geometry} footprint and height as building.
     * @param geom   Building footprint.
     * @param height Building height.
     */
    public ProfileBuilder addBuilding(Geometry geom, double height) {
        return addBuilding(geom, height, new ArrayList<>());
    }

    /**
     * Add the given {@link Geometry} footprint.
     * @param coords Building footprint coordinates.
     * @param height Building height.
     */
    public ProfileBuilder addBuilding(Coordinate[] coords, double height) {
        Coordinate[] polyCoords;
        int l = coords.length;
        if(coords[0] != coords[l-1]) {
            polyCoords = Arrays.copyOf(coords, l+1);
            polyCoords[l] = new Coordinate(coords[0]);
        }
        else {
            polyCoords = coords;
        }
        return addBuilding(FACTORY.createPolygon(polyCoords), height, new ArrayList<>());
    }

    /**
     * Add the given {@link Geometry} footprint.
     * @param geom   Building footprint.
     * @param id     Database primary key.
     */
    public ProfileBuilder addBuilding(Geometry geom, int id) {
        return addBuilding(geom, NaN, id);
    }

    /**
     * Add the given {@link Geometry} footprint.
     * @param coords Building footprint coordinates.
     * @param id     Database primary key.
     */
    public ProfileBuilder addBuilding(Coordinate[] coords, int id) {
        Coordinate[] polyCoords;
        int l = coords.length;
        if(coords[0] != coords[l-1]) {
            polyCoords = Arrays.copyOf(coords, l+1);
            polyCoords[l] = new Coordinate(coords[0]);
        }
        else {
            polyCoords = coords;
        }
        return addBuilding(FACTORY.createPolygon(polyCoords), id);
    }

    /**
     * Add the given {@link Geometry} footprint, height, alphas (absorption coefficients) and a database id as building.
     * @param geom   Building footprint.
     * @param height Building height.
     * @param id     Database id.
     */
    public ProfileBuilder addBuilding(Geometry geom, double height, int id) {
        return addBuilding(geom, height, new ArrayList<>(), id);
    }

    /**
     * Add the given {@link Geometry} footprint.
     * @param coords Building footprint coordinates.
     * @param height Building height.
     * @param id     Database primary key.
     */
    public ProfileBuilder addBuilding(Coordinate[] coords, double height, int id) {
        Coordinate[] polyCoords;
        int l = coords.length;
        if(coords[0] != coords[l-1]) {
            polyCoords = Arrays.copyOf(coords, l+1);
            polyCoords[l] = new Coordinate(coords[0]);
        }
        else {
            polyCoords = coords;
        }
        return addBuilding(FACTORY.createPolygon(polyCoords), height, new ArrayList<>(), id);
    }

    /**
     * Add the given {@link Geometry} footprint, height and alphas (absorption coefficients) as building.
     * @param geom   Building footprint.
     * @param height Building height.
     * @param alphas Absorption coefficients.
     */
    public ProfileBuilder addBuilding(Geometry geom, double height, List<Double> alphas) {
        return addBuilding(geom, height, alphas, -1);
    }

    /**
     * Add the given {@link Geometry} footprint.
     * @param coords Building footprint coordinates.
     * @param height Building height.
     * @param alphas Absorption coefficients.
     */
    public ProfileBuilder addBuilding(Coordinate[] coords, double height, List<Double> alphas) {
        Coordinate[] polyCoords;
        int l = coords.length;
        if(coords[0] != coords[l-1]) {
            polyCoords = Arrays.copyOf(coords, l+1);
            polyCoords[l] = new Coordinate(coords[0]);
        }
        else {
            polyCoords = coords;
        }
        return addBuilding(FACTORY.createPolygon(polyCoords), height, alphas, -1);
    }

    /**
     * Add the given {@link Geometry} footprint, height and alphas (absorption coefficients) as building.
     * @param geom   Building footprint.
     * @param alphas Absorption coefficients.
     */
    public ProfileBuilder addBuilding(Geometry geom, List<Double> alphas) {
        return addBuilding(geom, NaN, alphas, -1);
    }

    /**
     * Add the given {@link Geometry} footprint.
     * @param coords Building footprint coordinates.
     * @param alphas Absorption coefficients.
     */
    public ProfileBuilder addBuilding(Coordinate[] coords, List<Double> alphas) {
        Coordinate[] polyCoords;
        int l = coords.length;
        if(coords[0] != coords[l-1]) {
            polyCoords = Arrays.copyOf(coords, l+1);
            polyCoords[l] = new Coordinate(coords[0]);
        }
        else {
            polyCoords = coords;
        }
        return addBuilding(FACTORY.createPolygon(polyCoords), NaN, alphas, -1);
    }

    /**
     * Add the given {@link Geometry} footprint, height and alphas (absorption coefficients) as building.
     * @param geom   Building footprint.
     * @param alphas Absorption coefficients.
     * @param id     Database primary key.
     */
    public ProfileBuilder addBuilding(Geometry geom, List<Double> alphas, int id) {
        return addBuilding(geom, NaN, alphas, id);
    }

    /**
     * Add the given {@link Geometry} footprint.
     * @param coords Building footprint coordinates.
     * @param alphas Absorption coefficients.
     * @param id     Database primary key.
     */
    public ProfileBuilder addBuilding(Coordinate[] coords, List<Double> alphas, int id) {
        Coordinate[] polyCoords;
        int l = coords.length;
        if(coords[0] != coords[l-1]) {
            polyCoords = Arrays.copyOf(coords, l+1);
            polyCoords[l] = new Coordinate(coords[0]);
        }
        else {
            polyCoords = coords;
        }
        return addBuilding(FACTORY.createPolygon(polyCoords), NaN, alphas, id);
    }

    /**
     * Add the given {@link Geometry} footprint, height, alphas (absorption coefficients) and a database primary key
     * as building.
     * @param geom   Building footprint.
     * @param height Building height.
     * @param alphas Absorption coefficients.
     * @param id     Database primary key.
     */
    public ProfileBuilder addBuilding(Geometry geom, double height, List<Double> alphas, int id) {
        if(geom == null && ! (geom instanceof Polygon)) {
            LOGGER.error("Building geometry should be Polygon");
            return null;
        }
        Polygon poly = (Polygon)geom;
        if(!isFeedingFinished) {
            if(envelope == null) {
                envelope = geom.getEnvelopeInternal();
            }
            else {
                envelope.expandToInclude(geom.getEnvelopeInternal());
            }
            Building building = new Building(poly, height, alphas, id, zBuildings);
            buildings.add(building);
            buildingTree.insert(building.poly.getEnvelopeInternal(), buildings.size());
            //TODO : generalization of building coefficient
            addGroundEffect(geom, 0);
            return this;
        }
        else{
            LOGGER.warn("Cannot add building, feeding is finished.");
            return null;
        }
    }

    /**
     * Add the given {@link Geometry} footprint.
     * @param height Building height.
     * @param alphas Absorption coefficients.
     * @param id     Database primary key.
     */
    public ProfileBuilder addBuilding(Coordinate[] coords, double height, List<Double> alphas, int id) {
        Coordinate[] polyCoords;
        int l = coords.length;
        if(coords[0] != coords[l-1]) {
            polyCoords = Arrays.copyOf(coords, l+1);
            polyCoords[l-1] = new Coordinate(coords[0]);
        }
        else {
            polyCoords = coords;
        }
        return addBuilding(FACTORY.createPolygon(polyCoords), height, alphas, id);
    }

    /**
     * Add the given {@link Geometry} footprint, height, alphas (absorption coefficients) and a database id as wall.
     * @param geom   Wall footprint.
     * @param height Wall height.
     * @param id     Database key.
     */
    public ProfileBuilder addWall(LineString geom, double height, int id) {
        return addWall(geom, height, new ArrayList<>(), id);
    }

    /**
     * Add the given {@link Geometry} footprint, height, alphas (absorption coefficients) and a database id as wall.
     * @param coords Wall footprint coordinates.
     * @param height Wall height.
     * @param id     Database key.
     */
    public ProfileBuilder addWall(Coordinate[] coords, double height, int id) {
        return addWall(FACTORY.createLineString(coords), height, new ArrayList<>(), id);
    }

    /**
     * Add the given {@link Geometry} footprint, height, alphas (absorption coefficients) and a database id as wall.
     * @param geom   Wall footprint.
     * @param id     Database key.
     */
    public ProfileBuilder addWall(LineString geom, int id) {
        return addWall(geom, 0.0, new ArrayList<>(), id);
    }

    /**
     * Add the given {@link Geometry} footprint, height, alphas (absorption coefficients) and a database id as wall.
     * @param coords Wall footprint coordinates.
     * @param id     Database key.
     */
    public ProfileBuilder addWall(Coordinate[] coords, int id) {
        return addWall(FACTORY.createLineString(coords), 0.0, new ArrayList<>(), id);
    }

    /**
     * Add the given {@link Geometry} footprint, height, alphas (absorption coefficients) and a database id as wall.
     * @param geom   Wall footprint.
     * @param height Wall height.
     * @param alphas Absorption coefficient.
     * @param id     Database key.
     */
    public ProfileBuilder addWall(LineString geom, double height, List<Double> alphas, int id) {
        if(!isFeedingFinished) {
            if(envelope == null) {
                envelope = geom.getEnvelopeInternal();
            }
            else {
                envelope.expandToInclude(geom.getEnvelopeInternal());
            }
            for(int i=0; i<geom.getNumPoints()-1; i++) {
                Wall wall = new Wall(geom.getCoordinateN(i), geom.getCoordinateN(i+1), id, IntersectionType.BUILDING, i!=0, i!=geom.getNumPoints()-2);
                wall.setHeight(height);
                wall.setAlpha(alphas);
                walls.add(wall);
                wallTree.insert(wall.line.getEnvelopeInternal(), walls.size());
            }
            return this;
        }
        else{
            LOGGER.warn("Cannot add building, feeding is finished.");
            return null;
        }
    }

    /**
     * Add the given {@link Geometry} footprint, height, alphas (absorption coefficients) and a database id as wall.
     * @param coords Wall footprint coordinates.
     * @param id     Database key.
     */
    public ProfileBuilder addWall(Coordinate[] coords, double height, List<Double> alphas, int id) {
        return addWall(FACTORY.createLineString(coords), height, alphas, id);
    }

    /**
     * Add the topographic point in the data, to complete the topographic data.
     * @param point Topographic point.
     */
    public ProfileBuilder addTopographicPoint(Coordinate point) {
        if(!isFeedingFinished) {
            //Force to 3D
            if (isNaN(point.z)) {
                point.setCoordinate(new Coordinate(point.x, point.y, 0.));
            }
            if(envelope == null) {
                envelope = new Envelope(point);
            }
            else {
                envelope.expandToInclude(point);
            }
            this.topoPoints.add(point);
        }
        return this;
    }

    /**
     * Add the topographic line in the data, to complete the topographic data.
     */
    public ProfileBuilder addTopographicLine(double x0, double y0, double z0, double x1, double y1, double z1) {
        if(!isFeedingFinished) {
            LineString lineSegment = FACTORY.createLineString(new Coordinate[]{new Coordinate(x0, y0, z0), new Coordinate(x1, y1, z1)});
            if(envelope == null) {
                envelope = lineSegment.getEnvelopeInternal();
            }
            else {
                envelope.expandToInclude(lineSegment.getEnvelopeInternal());
            }
            this.topoLines.add(lineSegment);
        }
        return this;
    }

    /**
     * Add the topographic line in the data, to complete the topographic data.
     * @param lineSegment Topographic line.
     */
    public ProfileBuilder addTopographicLine(LineString lineSegment) {
        if(!isFeedingFinished) {
            if(envelope == null) {
                envelope = lineSegment.getEnvelopeInternal();
            }
            else {
                envelope.expandToInclude(lineSegment.getEnvelopeInternal());
            }
            this.topoLines.add(lineSegment);
        }
        return this;
    }

    /**
     * Add a ground effect.
     * @param geom        Ground effect area footprint.
     * @param coefficient Ground effect coefficient.
     */
    public ProfileBuilder addGroundEffect(Geometry geom, double coefficient) {
        if(!isFeedingFinished) {
            if(envelope == null) {
                envelope = geom.getEnvelopeInternal();
            }
            else {
                envelope.expandToInclude(geom.getEnvelopeInternal());
            }
            this.groundEffects.add(new GroundEffect(geom, coefficient));
        }
        return this;
    }

    /**
     * Add a ground effect.
     * @param minX        Ground effect minimum X.
     * @param maxX        Ground effect maximum X.
     * @param minY        Ground effect minimum Y.
     * @param maxY        Ground effect maximum Y.
     * @param coefficient Ground effect coefficient.
     */
    public ProfileBuilder addGroundEffect(double minX, double maxX, double minY, double maxY, double coefficient) {
        if(!isFeedingFinished) {
            Geometry geom = FACTORY.createPolygon(new Coordinate[]{
                    new Coordinate(minX, minY),
                    new Coordinate(minX, maxY),
                    new Coordinate(maxX, maxY),
                    new Coordinate(maxX, minY),
                    new Coordinate(minX, minY)
            });
            if(envelope == null) {
                envelope = geom.getEnvelopeInternal();
            }
            else {
                envelope.expandToInclude(geom.getEnvelopeInternal());
            }
            this.groundEffects.add(new GroundEffect(geom, coefficient));
        }
        return this;
    }

    public List<Wall> getProcessedWalls() {
        return processedWalls;
    }

    /**
     * Retrieve the building list.
     * @return The building list.
     */
    public List<Building> getBuildings() {
        return buildings;
    }

    /**
     * Retrieve the count of building add to this builder.
     * @return The count of building.
     */
    public int getBuildingCount() {
        return buildings.size();
    }

    /**
     * Retrieve the building with the given id (id is starting from 1).
     * @param id Id of the building
     * @return The building corresponding to the given id.
     */
    public Building getBuilding(int id) {
        return buildings.get(id);
    }

    /**
     * Retrieve the wall list.
     * @return The wall list.
     */
    public List<Wall> getWalls() {
        return walls;
    }

    /**
     * Retrieve the count of wall add to this builder.
     * @return The count of wall.
     */
    public int getWallCount() {
        return walls.size();
    }

    /**
     * Retrieve the wall with the given id (id is starting from 1).
     * @param id Id of the wall
     * @return The wall corresponding to the given id.
     */
    public Wall getWall(int id) {
        return walls.get(id);
    }

    /**
     * Clear the building list.
     */
    public void clearBuildings() {
        buildings.clear();
    }

    /**
     * Retrieve the global profile envelope.
     * @return The global profile envelope.
     */
    public Envelope getMeshEnvelope() {
        return envelope;
    }

    /**
     * Add a constraint on maximum triangle area.
     * @param maximumArea Value in square meter.
     */
    public void setMaximumArea(double maximumArea) {
        maxArea = maximumArea;
    }

    /**
     * Retrieve the topographic triangles.
     * @return The topographic triangles.
     */
    public List<Triangle> getTriangles() {
        return topoTriangles;
    }

    /**
     * Retrieve the topographic vertices.
     * @return The topographic vertices.
     */
    public List<Coordinate> getVertices() {
        return vertices;
    }

    /**
     * Retrieve the receivers list.
     * @return The receivers list.
     */
    public List<Coordinate> getReceivers() {
        return receivers;
    }

    /**
     * Retrieve the ground effects.
     * @return The ground effects.
     */
    public List<GroundEffect> getGroundEffects() {
        return groundEffects;
    }

    /**
     * Finish the data feeding. Once called, no more data can be added and process it in order to prepare the
     * profile retrieving.
     * The building are processed to include each facets into a RTree
     * The topographic points and lines are meshed using delaunay and triangles facets are included into a RTree
     *
     * @return True if the finishing has been successfully done, false otherwise.
     */
    public ProfileBuilder finishFeeding() {
        isFeedingFinished = true;

        //Process topographic points and lines
        if(topoPoints.size()+topoLines.size() > 1) {
            //Feed the Delaunay layer
            LayerDelaunay layerDelaunay = new LayerTinfour();
            layerDelaunay.setRetrieveNeighbors(true);
            try {
                layerDelaunay.setMaxArea(maxArea);
            } catch (LayerDelaunayError e) {
                LOGGER.error("Unable to set the Delaunay triangle maximum area.", e);
                return null;
            }
            try {
                for (Coordinate topoPoint : topoPoints) {
                    layerDelaunay.addVertex(topoPoint);
                }
            } catch (LayerDelaunayError e) {
                LOGGER.error("Error while adding topographic points to Delaunay layer.", e);
                return null;
            }
            try {
                for (LineString topoLine : topoLines) {
                    //TODO ensure the attribute parameter is useless
                    layerDelaunay.addLineString(topoLine, -1);
                }
            } catch (LayerDelaunayError e) {
                LOGGER.error("Error while adding topographic points to Delaunay layer.", e);
                return null;
            }
            //Process Delaunay
            try {
                layerDelaunay.processDelaunay();
            } catch (LayerDelaunayError e) {
                LOGGER.error("Error while processing Delaunay.", e);
                return null;
            }
            try {
                topoTriangles = layerDelaunay.getTriangles();
                topoNeighbors = layerDelaunay.getNeighbors();
            } catch (LayerDelaunayError e) {
                LOGGER.error("Error while getting triangles", e);
                return null;
            }
            //Feed the RTree
            topoTree = new STRtree(topoNodeCapacity);
            try {
                vertices = layerDelaunay.getVertices();
            } catch (LayerDelaunayError e) {
                LOGGER.error("Error while getting vertices", e);
                return null;
            }
            // wallIndex set will merge shared triangle segments
            Set<IntegerTuple> wallIndex = new HashSet<>();
            for (int i = 0; i < topoTriangles.size(); i++) {
                final Triangle tri = topoTriangles.get(i);
                wallIndex.add(new IntegerTuple(tri.getA(), tri.getB(), i));
                wallIndex.add(new IntegerTuple(tri.getB(), tri.getC(), i));
                wallIndex.add(new IntegerTuple(tri.getC(), tri.getA(), i));
                // Insert triangle in rtree
                Coordinate vA = vertices.get(tri.getA());
                Coordinate vB = vertices.get(tri.getB());
                Coordinate vC = vertices.get(tri.getC());
                Envelope env = FACTORY.createLineString(new Coordinate[]{vA, vB, vC}).getEnvelopeInternal();
                topoTree.insert(env, i);
            }
            topoTree.build();
            //TODO : Seems to be useless, to check
            /*for (IntegerTuple wallId : wallIndex) {
                Coordinate vA = vertices.get(wallId.nodeIndexA);
                Coordinate vB = vertices.get(wallId.nodeIndexB);
                Wall wall = new Wall(vA, vB, wallId.triangleIdentifier, TOPOGRAPHY);
                processedWalls.add(wall);
            }*/
        }
        //Update building z
        if(topoTree != null) {
            for (Building b : buildings) {
                if(isNaN(b.poly.getCoordinate().z) || b.poly.getCoordinate().z == 0.0 || !zBuildings) {
                    b.poly2D_3D();
                    b.poly.apply(new UpdateZ(b.height + b.updateZTopo(this)));
                }
            }
            for (Wall w : walls) {
                if(isNaN(w.p0.z) || w.p0.z == 0.0) {
                    w.p0.z = w.height + getZGround(w.p0);
                }
                if(isNaN(w.p1.z) || w.p1.z == 0.0) {
                    w.p1.z = w.height + getZGround(w.p1);
                }
            }
        }
        else {
            for (Building b : buildings) {
                if(b != null && b.poly != null && b.poly.getCoordinate() != null && (!zBuildings ||
                        isNaN(b.poly.getCoordinate().z) || b.poly.getCoordinate().z == 0.0)) {

                    b.poly2D_3D();
                    b.poly.apply(new UpdateZ(b.height));
                }

            }
            for (Wall w : walls) {
                if(isNaN(w.p0.z) || w.p0.z == 0.0) {
                    w.p0.z = w.height;
                }
                if(isNaN(w.p1.z) || w.p1.z == 0.0) {
                    w.p1.z = w.height;
                }
            }
        }
        //Process buildings
        rtree = new STRtree(buildingNodeCapacity);
        buildingsWideAnglePoints.clear();
        for (int j = 0; j < buildings.size(); j++) {
            Building building = buildings.get(j);
            buildingsWideAnglePoints.put(j + 1,
                    getWideAnglePointsByBuilding(j + 1, 0, 2 * Math.PI));
            List<Wall> walls = new ArrayList<>();
            Coordinate[] coords = building.poly.getCoordinates();
            for (int i = 0; i < coords.length - 1; i++) {
                LineSegment lineSegment = new LineSegment(coords[i], coords[i + 1]);
                Wall w = new Wall(lineSegment, j, IntersectionType.BUILDING).setProcessedWallIndex(processedWalls.size());
                walls.add(w);
                w.setAlpha(building.alphas);
                processedWalls.add(w);
                rtree.insert(lineSegment.toGeometry(FACTORY).getEnvelopeInternal(), processedWalls.size()-1);
            }
            building.setWalls(walls);
        }
        for (int j = 0; j < walls.size(); j++) {
            Wall wall = walls.get(j);
            Coordinate[] coords = new Coordinate[]{wall.p0, wall.p1};
            for (int i = 0; i < coords.length - 1; i++) {
                LineSegment lineSegment = new LineSegment(coords[i], coords[i + 1]);
                Wall w = new Wall(lineSegment, j, IntersectionType.WALL).setProcessedWallIndex(processedWalls.size());
                w.setAlpha(wall.alphas);
                processedWalls.add(w);
                rtree.insert(lineSegment.toGeometry(FACTORY).getEnvelopeInternal(), processedWalls.size()-1);
            }
        }
        //Process the ground effects
        groundEffectsRtree = new STRtree(TREE_NODE_CAPACITY);
        for (int j = 0; j < groundEffects.size(); j++) {
            GroundEffect effect = groundEffects.get(j);
            List<Polygon> polygons = new ArrayList<>();
            if (effect.geom instanceof Polygon) {
                polygons.add((Polygon) effect.geom);
            }
            if (effect.geom instanceof MultiPolygon) {
                MultiPolygon multi = (MultiPolygon) effect.geom;
                for (int i = 0; i < multi.getNumGeometries(); i++) {
                    polygons.add((Polygon) multi.getGeometryN(i));
                }
            }
            for (Polygon poly : polygons) {
                groundEffectsRtree.insert(poly.getEnvelopeInternal(), j);
                Coordinate[] coords = poly.getCoordinates();
                for (int k = 0; k < coords.length - 1; k++) {
                    LineSegment line = new LineSegment(coords[k], coords[k + 1]);
                    processedWalls.add(new Wall(line, j, GROUND_EFFECT).setProcessedWallIndex(processedWalls.size()));
                    rtree.insert(new Envelope(line.p0, line.p1), processedWalls.size() - 1);
                }
            }
        }
        rtree.build();
        groundEffectsRtree.build();
        return this;
    }

    public double getZ(Coordinate reflectionPt) {
        List<Integer> ids = buildingTree.query(new Envelope(reflectionPt));
        if(ids.isEmpty()) {
            return getZGround(reflectionPt);
        }
        else {
            for(Integer id : ids) {
                Geometry buildingGeometry =  buildings.get(id - 1).getGeometry();
                if(buildingGeometry.getEnvelopeInternal().intersects(reflectionPt)) {
                    return buildingGeometry.getCoordinate().z;
                }
            }
            return getZGround(reflectionPt);
        }
    }

    public List<Wall> getWallsIn(Envelope env) {
        List<Wall> list = new ArrayList<>();
        List<Integer> indexes = rtree.query(env);
        for(int i : indexes) {
            Wall w = getProcessedWalls().get(i);
            if(w.getType().equals(BUILDING) || w.getType().equals(WALL)) {
                list.add(w);
            }
        }
        return list;
    }

    private static class UpdateZ implements CoordinateSequenceFilter {

        private boolean done = false;
        private final double z;

        public UpdateZ(double z) {
            this.z = z;
        }

        @Override
        public boolean isGeometryChanged() {
            return true;
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public void filter(CoordinateSequence seq, int i) {

            seq.setOrdinate(i, 2, z);

            if (i == seq.size()) {
                done = true;
            }
        }
    }

    /**
     * Retrieve the cutting profile following the line build from the given coordinates.
     * @param c0 Starting point.
     * @param c1 Ending point.
     * @return Cutting profile.
     */
    public CutProfile getProfile(Coordinate c0, Coordinate c1) {
        return getProfile(c0, c1, 0.0);
    }

    /**
     * Retrieve the cutting profile following the line build from the given cut points.
     * @param c0 Starting point.
     * @param c1 Ending point.
     * @return Cutting profile.
     */
    public CutProfile getProfile(CutPoint c0, CutPoint c1) {
        return getProfile(c0, c1, 0.0);
    }

    /**
     * Retrieve the cutting profile following the line build from the given cut points.
     * @param c0 Starting point.
     * @param c1 Ending point.
     * @return Cutting profile.
     */
    public CutProfile getProfile(CutPoint c0, CutPoint c1, double gS) {
        CutProfile profile = getProfile(c0.getCoordinate(), c1.getCoordinate(), gS);

        profile.source.buildingId = c0.buildingId;
        profile.source.groundCoef = c0.groundCoef;
        profile.source.wallAlpha = c0.wallAlpha;

        profile.receiver.buildingId = c1.buildingId;
        profile.receiver.groundCoef = c1.groundCoef;
        profile.receiver.wallAlpha = c1.wallAlpha;

        return profile;
    }

    public static List<LineSegment> splitSegment(Coordinate c0, Coordinate c1, double maxLineLength) {
        List<LineSegment> lines = new ArrayList<>();
        LineSegment fullLine = new LineSegment(c0, c1);
        double l = dist2D(c0, c1);
        //If the line length if greater than the MAX_LINE_LENGTH value, split it into multiple lines
        if(l < maxLineLength) {
            lines.add(fullLine);
        }
        else {
            double frac = maxLineLength /l;
            for(int i = 0; i<l/ maxLineLength; i++) {
                Coordinate p0 = fullLine.pointAlong(i*frac);
                p0.z = c0.z + (c1.z - c0.z) * i*frac;
                Coordinate p1 = fullLine.pointAlong(Math.min((i+1)*frac, 1.0));
                p1.z = c0.z + (c1.z - c0.z) * Math.min((i+1)*frac, 1.0);
                lines.add(new LineSegment(p0, p1));
            }
        }
        return lines;
    }

    /**
     * Retrieve the cutting profile following the line build from the given coordinates.
     * @param c0 Starting point.
     * @param c1 Ending point.
     * @return Cutting profile.
     */
    public CutProfile getProfile(Coordinate c0, Coordinate c1, double gS) {
        CutProfile profile = new CutProfile();

        //Topography
        if(topoTree != null) {
            addTopoCutPts(c0, c1, profile);
        }
        // Split line into segments for structures based on RTree in order to limit the number of queries
        // (for large area of the line segment envelope)
        LineSegment fullLine = new LineSegment(c0, c1);
        List<LineSegment> lines = splitSegment(c0, c1, maxLineLength);

        //Buildings and Ground effect
        if(rtree != null) {
            addGroundBuildingCutPts(lines, fullLine, profile);
        }

        //Sort all the cut point in order to set the ground coefficients.
        profile.sort(c0, c1);
        //Add base cut for buildings
        addBuildingBaseCutPts(profile, c0, c1);


        //If ordering puts source at last position, reverse the list
        if(profile.pts.get(0) != profile.source) {
            if(profile.pts.get(profile.pts.size()-1) != profile.source && profile.pts.get(0) != profile.source) {
                LOGGER.error("The source have to be first or last cut point");
            }
            if(profile.pts.get(profile.pts.size()-1) != profile.receiver && profile.pts.get(0) != profile.receiver) {
                LOGGER.error("The receiver have to be first or last cut point");
            }
            profile.reverse();
        }


        //Sets the ground effects
        //Check is source is inside ground
        setGroundEffects(profile, c0, gS);

        return profile;
    }

    private void setGroundEffects(CutProfile profile, Coordinate c0, double gS) {
        Stack<List<Integer>> stack = new Stack<>();
        GroundEffect currentGround = null;
        int currGrdI = -1;
        Point p0 = FACTORY.createPoint(c0);
        List<Integer> groundEffectsResult = (List<Integer>)groundEffectsRtree.query(new Envelope(c0));
        for(Integer groundEffectIndex : groundEffectsResult) {
            GroundEffect ground = groundEffects.get(groundEffectIndex);
            if(ground.geom.contains(p0)) {
                currentGround = ground;
                break;
            }
        }
        List<Integer> currGrounds = new ArrayList<>();
        List<Integer> nextGrounds = new ArrayList<>();
        boolean first = true;
        List<CutPoint> pts = profile.pts;
        //Loop on each cut points
        for (int i = 0; i < pts.size(); i++) {
            CutPoint cut = pts.get(i);
            //If the cut point is not a Ground effect, simply apply the current ground coef
            if (cut.type != GROUND_EFFECT) {
                cut.groundCoef = currentGround != null ? currentGround.coef : gS;
            } else {
                int j=i;
                CutPoint next = pts.get(j);
                //Pass all the cut points located at the same position as the current point.
                while(cut.coordinate.equals2D(next.coordinate)){
                    //If the current ground effect list has never been filled, fill it.
                    if(first && next.type == GROUND_EFFECT){
                        currGrounds.add(next.id);
                    }
                    //Apply the current ground effect tfor the case that the current cut point is at the same position as the receiver point.
                    next.groundCoef = currentGround != null ? currentGround.coef : gS;
                    if(j+1==pts.size()){
                        break;
                    }
                    next = pts.get(++j);
                }
                first = false;
                //Try to find the next ground effect cut point
                while((next = pts.get(j)).type != GROUND_EFFECT && j<pts.size()-1){
                    next.groundCoef = currentGround != null ? currentGround.coef : gS;
                    j++;
                }
                //If there is no more ground effect, exit loop
                if(j==pts.size()-1){
                    //Use the current ground effect for the remaining cut point
                    for(int idx : currGrounds) {
                        if(currentGround != null && currentGround.coef != groundEffects.get(idx).coef){
                            currentGround =groundEffects.get(idx);
                        }
                    }
                    continue;
                }
                CutPoint nextNext = pts.get(j);
                //Fill the next ground effect list
                while(next.coordinate.equals2D(nextNext.coordinate)){
                    if(nextNext.type == GROUND_EFFECT){
                        nextGrounds.add(nextNext.id);
                    }
                    if(j+1==pts.size()){
                        break;
                    }
                    nextNext = pts.get(++j);
                }
                nextNext = pts.get(j-1);

                boolean found = false;
                //Find the ground effect which will be applied from current position to next
                for(int idx : currGrounds) {
                    if(nextGrounds.contains(idx)){
                        currGrdI = idx;
                        found = true;
                        break;
                    }
                }
                //If no ground effect found, it means that the current ground effect contains an other ground effect.
                //Store the current ground effect in a stack and use the next ground effect
                if(!found){
                    currGrdI = nextGrounds.get(0);
                    stack.push(currGrounds);
                }
                if(currGrdI != -1) {
                    currentGround = groundEffects.get(currGrdI);
                }
                CutPoint cutPt = pts.get(i);
                //Apply the ground effect after the current coint up to the next ground effect
                while(!nextNext.coordinate.equals2D(cutPt.coordinate)){
                    if(found){
                        cutPt.groundCoef = currentGround != null ? currentGround.coef : gS;
                    }
                    i++;
                    if(i==pts.size()){
                        break;
                    }
                    cutPt = pts.get(i);
                }
                i--;
                currGrounds = nextGrounds;
                //remove the used ground effect from the list of next ground effect to avoid to reuse it
                if(found) {
                    currGrounds.remove((Object) currGrdI);
                }
                nextGrounds = new ArrayList<>();
                if(!currGrounds.isEmpty()) {
                    currentGround = groundEffects.get(currGrounds.get(0));
                }
                else {
                    if(stack.isEmpty()) {
                        currentGround = null;
                    }
                    //If there is no more ground effect, try to pop the stack
                    else{
                        currGrounds = stack.pop();
                        if(currGrounds.isEmpty()) {
                            currentGround = null;
                        } else {
                            currentGround = groundEffects.get(currGrounds.get(0));
                        }
                    }
                }
            }
        }
    }

    private void addBuildingBaseCutPts(CutProfile profile, Coordinate c0, Coordinate c1) {
        ArrayList<CutPoint> pts = new ArrayList<>(profile.pts.size());
        int buildId = -1;
        CutPoint lastBuild = null;
        for(int i=0; i<profile.pts.size(); i++) {
            ProfileBuilder.CutPoint cut = profile.pts.get(i);
            if(cut.getType().equals(BUILDING)) {
                if (buildId == -1) {
                    buildId = cut.getId();
                    CutPoint grd = new CutPoint(cut);
                    grd.getCoordinate().z = getZGround(cut);
                    pts.add(grd);
                    pts.add(cut);
                }
                else if(buildId == cut.getId()) {
                    pts.add(cut);
                }
                else {
                    CutPoint grd0 = new CutPoint(lastBuild);
                    grd0.getCoordinate().z = getZGround(grd0);
                    pts.add(pts.indexOf(lastBuild)+1, grd0);
                    CutPoint grd1 = new CutPoint(cut);
                    grd1.getCoordinate().z = getZGround(grd1);
                    pts.add(grd1);
                    pts.add(cut);
                    buildId = cut.getId();
                }
                lastBuild = cut;
            }
            else if(cut.getType().equals(RECEIVER)) {
                if(buildId != -1) {
                    buildId = -1;
                    CutPoint grd0 = new CutPoint(pts.get(pts.size()-1));
                    grd0.getCoordinate().z = getZGround(grd0);
                    pts.add(grd0);
                }
                pts.add(cut);
            }
            else {
                pts.add(cut);
            }
        }
        if(buildId != -1) {
            CutPoint grd0 = new CutPoint(lastBuild);
            grd0.getCoordinate().z = getZGround(grd0);
            pts.add(pts.indexOf(lastBuild)+1, grd0);
        }
        profile.pts = pts;
        profile.addSource(c0);
        profile.addReceiver(c1);
    }

    private void addGroundBuildingCutPts(List<LineSegment> lines, LineSegment fullLine, CutProfile profile) {
        List<Integer> indexes = new ArrayList<>();
        for (LineSegment line : lines) {
            indexes.addAll(rtree.query(new Envelope(line.p0, line.p1)));
        }
        indexes = indexes.stream().distinct().collect(Collectors.toList());
        Map<Integer, Coordinate> processedGround = new HashMap<>();
        for (int i : indexes) {
            Wall facetLine = processedWalls.get(i);
            Coordinate intersection = fullLine.intersection(facetLine.ls);
            if (intersection != null) {
                intersection = new Coordinate(intersection);
                if(!isNaN(facetLine.p0.z) && !isNaN(facetLine.p1.z)) {
                    if(facetLine.p0.z == facetLine.p1.z) {
                        intersection.z = facetLine.p0.z;
                    }
                    else {
                        intersection.z = facetLine.p0.z + ((intersection.x - facetLine.p0.x) / (facetLine.p1.x - facetLine.p0.x) * (facetLine.p1.z - facetLine.p0.z));
                    }
                }
                else if(topoTree == null) {
                    intersection.z = NaN;
                }
                else {
                    intersection.z = getZGround(intersection);
                }
                if(facetLine.type == IntersectionType.BUILDING) {
                    profile.addBuildingCutPt(intersection, facetLine.originId, i, facetLine.p0.equals(intersection)||facetLine.p1.equals(intersection));
                }
                else if(facetLine.type == IntersectionType.WALL) {
                    profile.addWallCutPt(intersection, facetLine.originId, facetLine.p0.equals(intersection)||facetLine.p1.equals(intersection));
                }
                else if(facetLine.type == GROUND_EFFECT) {
                    if(!intersection.equals(facetLine.p0) && !intersection.equals(facetLine.p1)) {
                        //Add cut point only if the a same orifinId is for two different coordinate to avoid having
                        // more than one cutPoint with the same id on the same coordinate
                        if(processedGround.containsKey(facetLine.originId) ){
                            if(intersection.equals(processedGround.get(facetLine.originId))) {
                                processedGround.remove(facetLine.originId);
                            }else{
                                profile.addGroundCutPt(intersection, facetLine.originId);
                                profile.addGroundCutPt(processedGround.remove(facetLine.originId), facetLine.originId);
                            }
                        }
                        else {
                            processedGround.put(facetLine.originId, intersection);
                        }
                    }
                }
            }
        }
        for(Map.Entry<Integer, Coordinate> entry : processedGround.entrySet()){
            profile.addGroundCutPt(entry.getValue(), entry.getKey());
        }
    }

    /**
     * Compute the next triangle index.Find the shortest intersection point of
     * triIndex segments to the p1 coordinate
     *
     * @param triIndex        Triangle index
     * @param propagationLine Propagation line
     * @return Next triangle to the specified direction, -1 if there is no
     * triangle neighbor.
     */
    private int getNextTri(final int triIndex,
                           final LineSegment propagationLine,
                           HashSet<Integer> navigationHistory, final Coordinate segmentIntersection) {
        final Triangle tri = topoTriangles.get(triIndex);
        final Triangle triNeighbors = topoNeighbors.get(triIndex);
        int nearestIntersectionSide = -1;
        int idNeighbor;

        double nearestIntersectionPtDist = Double.MAX_VALUE;
        // Find intersection pt
        final Coordinate aTri = this.vertices.get(tri.getA());
        final Coordinate bTri = this.vertices.get(tri.getB());
        final Coordinate cTri = this.vertices.get(tri.getC());
        double distline_line;
        // Intersection First Side
        idNeighbor = triNeighbors.get(2);
        if (!navigationHistory.contains(idNeighbor)) {
            LineSegment triSegment = new LineSegment(aTri, bTri);
            Coordinate[] closestPoints = propagationLine.closestPoints(triSegment);
            Coordinate intersectionTest = null;
            if(closestPoints.length == 2 && closestPoints[0].distance(closestPoints[1]) < JTSUtility.TRIANGLE_INTERSECTION_EPSILON) {
                intersectionTest = new Coordinate(closestPoints[0].x, closestPoints[0].y, Vertex.interpolateZ(closestPoints[0], triSegment.p0, triSegment.p1));
            }
            if(intersectionTest != null) {
                distline_line = propagationLine.p1.distance(intersectionTest);
                if (distline_line < nearestIntersectionPtDist) {
                    segmentIntersection.setCoordinate(intersectionTest);
                    nearestIntersectionPtDist = distline_line;
                    nearestIntersectionSide = 2;
                }
            }
        }
        // Intersection Second Side
        idNeighbor = triNeighbors.get(0);
        if (!navigationHistory.contains(idNeighbor)) {
            LineSegment triSegment = new LineSegment(bTri, cTri);
            Coordinate[] closestPoints = propagationLine.closestPoints(triSegment);
            Coordinate intersectionTest = null;
            if(closestPoints.length == 2 && closestPoints[0].distance(closestPoints[1]) < JTSUtility.TRIANGLE_INTERSECTION_EPSILON) {
                intersectionTest = new Coordinate(closestPoints[0].x, closestPoints[0].y, Vertex.interpolateZ(closestPoints[0], triSegment.p0, triSegment.p1));
            }
            if(intersectionTest != null) {
                distline_line = propagationLine.p1.distance(intersectionTest);
                if (distline_line < nearestIntersectionPtDist) {
                    segmentIntersection.setCoordinate(intersectionTest);
                    nearestIntersectionPtDist = distline_line;
                    nearestIntersectionSide = 0;
                }
            }
        }
        // Intersection Third Side
        idNeighbor = triNeighbors.get(1);
        if (!navigationHistory.contains(idNeighbor)) {
            LineSegment triSegment = new LineSegment(cTri, aTri);
            Coordinate[] closestPoints = propagationLine.closestPoints(triSegment);
            Coordinate intersectionTest = null;
            if(closestPoints.length == 2 && closestPoints[0].distance(closestPoints[1]) < JTSUtility.TRIANGLE_INTERSECTION_EPSILON) {
                intersectionTest = new Coordinate(closestPoints[0].x, closestPoints[0].y, Vertex.interpolateZ(closestPoints[0], triSegment.p0, triSegment.p1));
            }
            if(intersectionTest != null) {
                distline_line = propagationLine.p1.distance(intersectionTest);
                if (distline_line < nearestIntersectionPtDist) {
                    segmentIntersection.setCoordinate(intersectionTest);
                    nearestIntersectionSide = 1;
                }
            }
        }
        if(nearestIntersectionSide > -1) {
            return triNeighbors.get(nearestIntersectionSide);
        } else {
            return -1;
        }
    }


    /**
     * Get coordinates of triangle vertices
     * @param triIndex Index of triangle
     * @return triangle vertices
     */
    Coordinate[] getTriangle(int triIndex) {
        final Triangle tri = this.topoTriangles.get(triIndex);
        return new Coordinate[]{this.vertices.get(tri.getA()),
                this.vertices.get(tri.getB()), this.vertices.get(tri.getC())};
    }


    /**
     * Return the triangle id from a point coordinate inside the triangle
     *
     * @param pt Point test
     * @return Triangle Id, Or -1 if no triangle has been found
     */

    public int getTriangleIdByCoordinate(Coordinate pt) {
        Envelope ptEnv = new Envelope(pt);
        ptEnv.expandBy(1);
        List res = topoTree.query(new Envelope(ptEnv));
        double minDistance = Double.MAX_VALUE;
        int minDistanceTriangle = -1;
        for(Object objInd : res) {
            int triId = (Integer) objInd;
            Coordinate[] tri = getTriangle(triId);
            AtomicReference<Double> err = new AtomicReference<>(0.);
            JTSUtility.dotInTri(pt, tri[0], tri[1], tri[2], err);
            if (err.get() < minDistance) {
                minDistance = err.get();
                minDistanceTriangle = triId;
            }
        }
        return minDistanceTriangle;
    }

    public void addTopoCutPts(Coordinate p1, Coordinate p2, CutProfile profile) {
        List<Coordinate> coordinates = getTopographicProfile(p1, p2);
        // Remove unnecessary points
        ArrayList<Coordinate> retainedCoordinates = new ArrayList<>(coordinates.size());
        for(int i =0; i < coordinates.size(); i++) {
            // Always add first and last points
            Coordinate previous;
            Coordinate current = coordinates.get(i);
            Coordinate next;
            if(retainedCoordinates.isEmpty()) {
                previous = new Coordinate(p1.x, p1.y, getZGround(p1));
            } else {
                previous = retainedCoordinates.get(retainedCoordinates.size() - 1);
            }
            if(i == coordinates.size() - 1) {
                next = new Coordinate(p2.x, p2.y, getZGround(p2));
            } else {
                next = coordinates.get(i + 1);
            }
            // Do not add topographic points which are simply the linear interpolation between two points
            if(CGAlgorithms3D.distancePointSegment(current, previous, next) >= DELTA) {
                retainedCoordinates.add(coordinates.get(i));
            }
        }
        // Feed profile
        profile.reservePoints(retainedCoordinates.size());
        for(int i =0; i < retainedCoordinates.size(); i++) {
            profile.addTopoCutPt(retainedCoordinates.get(i), i);
        }
    }

    /**
     * Find closest triangle that intersects with segment
     * @param segment Segment to intersects will all triangles
     * @param intersection Found closest intersection point with p0
     * @param intersectionTriangle Found closest intersection triangle
     * @return True if at least one triangle as been found on intersection
     */
    boolean findClosestTriangleIntersection(LineSegment segment, final Coordinate intersection, AtomicInteger intersectionTriangle) {
        Envelope queryEnvelope = new Envelope(segment.p0);
        queryEnvelope.expandToInclude(segment.p1);
        if(queryEnvelope.getHeight() < 1.0 || queryEnvelope.getWidth() < 1) {
            queryEnvelope.expandBy(1.0);
        }
        List res = topoTree.query(queryEnvelope);
        double minDistance = Double.MAX_VALUE;
        int minDistanceTriangle = -1;
        GeometryFactory factory = new GeometryFactory();
        LineString lineString = factory.createLineString(new Coordinate[]{segment.p0, segment.p1});
        Coordinate intersectionPt = null;
        for(Object objInd : res) {
            int triId = (Integer) objInd;
            Coordinate[] tri = getTriangle(triId);
            Geometry triangleGeometry = factory.createPolygon(new Coordinate[]{ tri[0], tri[1], tri[2], tri[0]});
            if(triangleGeometry.intersects(lineString)) {
                Coordinate[] nearestCoordinates = DistanceOp.nearestPoints(triangleGeometry, lineString);
                for (Coordinate nearestCoordinate : nearestCoordinates) {
                    double distance = nearestCoordinate.distance(segment.p0);
                    if (distance < minDistance) {
                        minDistance = distance;
                        minDistanceTriangle = triId;
                        intersectionPt = nearestCoordinate;
                    }
                }
            }
        }
        if(minDistanceTriangle != -1) {
            Coordinate[] tri = getTriangle(minDistanceTriangle);
            // Compute interpolated Z of the intersected point on the nearest triangle
            intersectionPt.setZ(Vertex.interpolateZ(intersectionPt, tri[0], tri[1], tri[2]));
            intersection.setCoordinate(intersectionPt);
            intersectionTriangle.set(minDistanceTriangle);
            return true;
        } else {
            return false;
        }
    }

    public List<Coordinate> getTopographicProfile(Coordinate p1, Coordinate p2) {
        List<Coordinate> outputPoints = new ArrayList<>();
        //get origin triangle id
        int curTriP1 = getTriangleIdByCoordinate(p1);
        LineSegment propaLine = new LineSegment(p1, p2);
        if(curTriP1 == -1) {
            // we are outside of the bounds of the triangles
            // Find the closest triangle to p1
            Coordinate intersectionPt = new Coordinate();
            AtomicInteger minDistanceTriangle = new AtomicInteger();
            if(findClosestTriangleIntersection(propaLine, intersectionPt, minDistanceTriangle)) {
                outputPoints.add(intersectionPt);
                curTriP1 = minDistanceTriangle.get();
            } else {
                return outputPoints;
            }
        }
        HashSet<Integer> navigationHistory = new HashSet<Integer>();
        int navigationTri = curTriP1;
        while (navigationTri != -1) {
            navigationHistory.add(navigationTri);
            Coordinate intersectionPt = new Coordinate();
            int propaTri = this.getNextTri(navigationTri, propaLine, navigationHistory, intersectionPt);
            // Found next triangle (if propaTri >= 0)
            // extract X,Y,Z values of intersection with triangle segment
            if(!Double.isNaN(intersectionPt.z)) {
                outputPoints.add(intersectionPt);
            }
            navigationTri = propaTri;
        }
        return outputPoints;
    }

    /**
     * Get the topographic height of a point.
     * @param c Coordinate of the point.
     * @return Topographic height of the point.
     */
    @Deprecated
    public double getZGround(Coordinate c) {
        return getZGround(new CutPoint(c, TOPOGRAPHY, -1));
    }

    public double getZGround(CutPoint cut) {
        if(cut.zGround != null) {
            return cut.zGround;
        }
        if(topoTree == null) {
            cut.zGround = null;
            return 0.0;
        }
        Envelope env = new Envelope(cut.coordinate);
        List<Integer> list = (List<Integer>)topoTree.query(env);
        for (int i : list) {
            final Triangle tri = topoTriangles.get(i);
            final Coordinate p1 = vertices.get(tri.getA());
            final Coordinate p2 = vertices.get(tri.getB());
            final Coordinate p3 = vertices.get(tri.getC());
            if(JTSUtility.dotInTri(cut.coordinate, p1, p2, p3)) {
                double z = Vertex.interpolateZ(cut.coordinate, p1, p2, p3);
                cut.zGround = z;
                return z;
            }
        }
        cut.zGround = null;
        return 0.0;
    }

    /**
     * Different type of intersection.
     */
    public enum IntersectionType {BUILDING, WALL, TOPOGRAPHY, GROUND_EFFECT, SOURCE, RECEIVER;

        PointPath.POINT_TYPE toPointType(PointPath.POINT_TYPE dflt) {
            if(this.equals(SOURCE)){
                return PointPath.POINT_TYPE.SRCE;
            }
            else if(this.equals(RECEIVER)){
                return PointPath.POINT_TYPE.RECV;
            }
            else {
                return dflt;
            }
        }
    }

    /**
     * Cutting profile containing all th cut points with there x,y,z position.
     */
    public static class CutProfile {
        /** List of cut points. */
        private ArrayList<CutPoint> pts = new ArrayList<>();
        /** Source cut point. */
        private CutPoint source;
        /** Receiver cut point. */
        private CutPoint receiver;
        //TODO cache has intersection properties
        /** True if contains a building cutting point. */
        private Boolean hasBuildingInter = false;
        /** True if contains a topography cutting point. */
        private Boolean hasTopographyInter = false;
        /** True if contains a ground effect cutting point. */
        private Boolean hasGroundEffectInter = false;
        private Boolean isFreeField;
        private Orientation srcOrientation;

        /**
         * Add the source point.
         * @param coord Coordinate of the source point.
         */
        public void addSource(Coordinate coord) {
            source = new CutPoint(coord, SOURCE, -1);
            pts.add(0, source);
        }

        /**
         * Add the receiver point.
         * @param coord Coordinate of the receiver point.
         */
        public void addReceiver(Coordinate coord) {
            receiver = new CutPoint(coord, RECEIVER, -1);
            pts.add(receiver);
        }

        /**
         * Add a building cutting point.
         * @param coord      Coordinate of the cutting point.
         * @param buildingId Id of the cut building.
         */
        public void addBuildingCutPt(Coordinate coord, int buildingId, int wallId, boolean corner) {
            CutPoint cut = new CutPoint(coord, IntersectionType.BUILDING, buildingId, corner);
            cut.wallId = wallId;
            pts.add(cut);
            pts.get(pts.size()-1).buildingId = buildingId;
            hasBuildingInter = true;
        }

        /**
         * Add a building cutting point.
         * @param coord Coordinate of the cutting point.
         * @param id    Id of the cut building.
         */
        public void addWallCutPt(Coordinate coord, int id, boolean corner) {
            pts.add(new CutPoint(coord, IntersectionType.WALL, id, corner));
            pts.get(pts.size()-1).wallId = id;
            hasBuildingInter = true;
        }

        /**
         * Add a topographic cutting point.
         * @param coord Coordinate of the cutting point.
         * @param id    Id of the cut topography.
         */
        public void addTopoCutPt(Coordinate coord, int id) {
            pts.add(new CutPoint(coord, TOPOGRAPHY, id));
            hasTopographyInter = true;
        }

        /**
         * In order to reduce the number of reallocation, reserve the provided points size
         * @param numberOfPointsToBePushed
         */
        public void reservePoints(int numberOfPointsToBePushed) {
            pts.ensureCapacity(pts.size() + numberOfPointsToBePushed);
        }

        /**
         * Add a ground effect cutting point.
         * @param coord Coordinate of the cutting point.
         * @param id    Id of the cut topography.
         */
        public void addGroundCutPt(Coordinate coord, int id) {
            pts.add(new CutPoint(coord, IntersectionType.GROUND_EFFECT, id));
            hasGroundEffectInter = true;
        }

        /**
         * Retrieve the cutting points.
         * @return The cutting points.
         */
        public List<CutPoint> getCutPoints() {
            return Collections.unmodifiableList(pts);
        }

        /**
         * Retrieve the profile source.
         * @return The profile source.
         */
        public CutPoint getSource() {
            return source;
        }

        /**
         * Retrieve the profile receiver.
         * @return The profile receiver.
         */
        public CutPoint getReceiver() {
            return receiver;
        }

        /**
         * Sort the CutPoints by there coordinates
         */
        public void sort(Coordinate c0, Coordinate c1) {
            if(c0.x<=c1.x){
                if(c0.y<=c1.y){
                    pts.sort(CutPoint::compareTox01y01);
                }
                else {
                    pts.sort(CutPoint::compareTox01y10);
                }
            }
            if(c0.x>c1.x){
                if(c0.y<=c1.y){
                    pts.sort(CutPoint::compareTox10y01);
                }
                else {
                    pts.sort(CutPoint::compareTox10y10);
                }
            }
        }

        /**
         * Add an existing CutPoint.
         * @param cutPoint CutPoint to add.
         */
        public void addCutPt(CutPoint cutPoint) {
            pts.add(cutPoint);
        }

        /**
         * Reverse the order of the CutPoints.
         */
        public void reverse() {
            Collections.reverse(pts);
        }

        public void setSrcOrientation(Orientation srcOrientation){
            this.srcOrientation = srcOrientation;
        }

        public Orientation getSrcOrientation(){
            return srcOrientation;
        }

        public boolean intersectBuilding(){
            return hasBuildingInter;
        }

        public boolean intersectTopography(){
            return hasTopographyInter;
        }

        public boolean intersectGroundEffect(){
            return hasGroundEffectInter;
        }

        public double getGPath(CutPoint p0, CutPoint p1) {
            CutPoint current = p0;
            double totLength = dist2D(p0.getCoordinate(), p1.getCoordinate());
            double rsLength = 0.0;
            List<CutPoint> pts = new ArrayList<>();
            for(CutPoint cut : getCutPoints()) {
                if(cut.getType() != TOPOGRAPHY && cut.getType() != BUILDING) {
                    pts.add(cut);
                }
            }
            if(p0.compareTo(p1)<=0) {
                pts.sort(CutPoint::compareTo);
            } else {
                pts.sort(Collections.reverseOrder());
            }
            int dir = -p0.compareTo(p1);
            for(CutPoint cut : pts) {
                if(dir*cut.compareTo(current)>=0 && dir*cut.compareTo(p1)<0) {
                    rsLength += dist2D(current.getCoordinate(), cut.getCoordinate()) * current.getGroundCoef();
                    current = cut;
                }
            }
            rsLength += dist2D(current.getCoordinate(), p1.getCoordinate()) * p1.getGroundCoef();
            return rsLength / totLength;
        }

        public double getGPath() {
            return getGPath(getSource(), getReceiver());
        }

        public boolean isFreeField() {
            if(isFreeField == null) {
                isFreeField = true;
                Coordinate s = getSource().getCoordinate();
                Coordinate r = getReceiver().getCoordinate();
                List<CutPoint> tmp = new ArrayList<>();
                boolean allMatch = true;
                for(CutPoint cut : pts) {
                    if(cut.getType() == BUILDING || cut.getType() == WALL) {
                        tmp.add(cut);
                    }
                    else if(cut.getType() == TOPOGRAPHY) {
                        tmp.add(cut);
                    }
                    if(!(cut.getCoordinate().equals(s) || cut.getCoordinate().equals(r))) {
                        allMatch = false;
                    }
                }
                if(allMatch) {
                    return true;
                }
                List<CutPoint> ptsWithouGroundEffect = pts.stream()
                        .filter(cut -> !cut.getType().equals(GROUND_EFFECT))
                        .collect(Collectors.toList());
                for(CutPoint pt : ptsWithouGroundEffect) {
                    double frac = (pt.coordinate.x-s.x)/(r.x-s.x);
                    double z = source.getCoordinate().z + frac * (receiver.getCoordinate().z-source.getCoordinate().z);
                    if(z < pt.getCoordinate().z && !pt.isCorner()) {
                        isFreeField = false;
                        break;
                    }
                }
            }
            return isFreeField;
        }

        @Override
        public String toString() {
            return "CutProfile{" + "pts=" + pts + ", source=" + source + ", receiver=" + receiver + ", " +
                    "hasBuildingInter=" + hasBuildingInter + ", hasTopographyInter=" + hasTopographyInter + ", " +
                    "hasGroundEffectInter=" + hasGroundEffectInter + ", isFreeField=" + isFreeField + ", " +
                    "srcOrientation=" + srcOrientation + '}';
        }
    }

    /**
     * Profile cutting point.
     */
    public static class CutPoint implements Comparable<CutPoint> {
        /** {@link Coordinate} of the cut point. */
        private Coordinate coordinate;
        /** Intersection type. */
        private final IntersectionType type;
        /** Identifier of the cut element. */
        private final int id;
        /** Identifier of the building containing the point. -1 if no building. */
        private int buildingId;
        /** Identifier of the wall containing the point. -1 if no wall. */
        private int wallId;
        /** Height of the building containing the point. NaN of no building. */
        private double height;
        /** Topographic height of the point. */
        private Double zGround;
        /** Ground effect coefficient. 0 if there is no coefficient. */
        private double groundCoef;
        /** Wall alpha. NaN if there is no coefficient. */
        private List<Double> wallAlpha;
        private boolean corner;

        /**
         * Constructor using a {@link Coordinate}.
         * @param coord Coordinate to copy.
         * @param type  Intersection type.
         * @param id    Identifier of the cut element.
         */
        public CutPoint(Coordinate coord, IntersectionType type, int id, boolean corner) {
            this.coordinate = new Coordinate(coord.x, coord.y, coord.z);
            this.type = type;
            this.id = id;
            this.buildingId = -1;
            this.wallId = -1;
            this.groundCoef = 0;
            this.wallAlpha = new ArrayList<>();
            this.height = 0;
            this.zGround = null;
            this.corner = corner;
        }
        public CutPoint(Coordinate coord, IntersectionType type, int id) {
            this(coord, type, id, false);
        }
        public CutPoint(CutPoint cut) {
            this.coordinate = new Coordinate(cut.getCoordinate());
            this.type = cut.type;
            this.id = cut.id;
            this.buildingId = cut.buildingId;
            this.wallId = cut.wallId;
            this.groundCoef = cut.groundCoef;
            this.wallAlpha = new ArrayList<>(cut.wallAlpha);
            this.height = cut.height;
            this.zGround = cut.zGround;
            this.corner = corner;
        }

        /**
         * Sets the id of the building containing the point.
         * @param buildingId Id of the building containing the point.
         */
        public void setBuildingId(int buildingId) {
            this.buildingId = buildingId;
            this.wallId = -1;
        }

        /**
         * Sets the id of the wall containing the point.
         * @param wallId Id of the wall containing the point.
         */
        public void setWallId(int wallId) {
            this.wallId = wallId;
            this.buildingId = -1;
        }

        /**
         * Sets the ground coefficient of this point.
         * @param groundCoef The ground coefficient of this point.
         */
        public void setGroundCoef(double groundCoef) {
            this.groundCoef = groundCoef;
        }

        /**
         * Sets the building height.
         * @param height The building height.
         */
        public void setHeight(double height) {
            this.height = height;
        }

        /**
         * Sets the topographic height.
         * @param zGround The topographic height.
         */
        public void setzGround(double zGround) {
            this.zGround = zGround;
        }

        /**
         * Sets the wall alpha.
         * @param wallAlpha The wall alpha.
         */
        public void setWallAlpha(List<Double> wallAlpha) {
            this.wallAlpha = wallAlpha;
        }

        /**
         * Retrieve the coordinate of the point.
         * @return The coordinate of the point.
         */
        public Coordinate getCoordinate(){
            return coordinate;
        }

        /**
         * Retrieve the identifier of the cut element.
         * @return Identifier of the cut element.
         */
        public int getId() {
            return id;
        }

        /**
         * Retrieve the identifier of the building containing the point. If no building, returns -1.
         * @return Building identifier or -1
         */
        public int getBuildingId() {
            return buildingId;
        }

        /**
         * Retrieve the identifier of the wall containing the point. If no wall, returns -1.
         * @return Wall identifier or -1
         */
        public int getWallId() {
            return wallId;
        }

        /**
         * Retrieve the ground effect coefficient of the point. If there is no coefficient, returns 0.
         * @return Ground effect coefficient or NaN.
         */
        public double getGroundCoef() {
            return groundCoef;
        }

        /**
         * Retrieve the height of the building containing the point. If there is no building, returns NaN.
         * @return The building height, or NaN if no building.
         */
        public double getHeight() {
            return height;
        }

        /**
         * Retrieve the topographic height of the point.
         * @return The topographic height of the point.
         */
        public double getzGround() {
            return zGround;
        }

        /**
         * Return the wall alpha value.
         * @return The wall alpha value.
         */
        public List<Double> getWallAlpha() {
            return wallAlpha;
        }

        public IntersectionType getType() {
            return type;
        }

        @Override
        public String toString() {
            String str = "";
            str += type.name();
            str += " ";
            str += "(" + coordinate.x +"," + coordinate.y +"," + coordinate.z + ") ; ";
            str += "grd : " + groundCoef + " ; ";
            str += "topoH : " + zGround + " ; ";
            str += "buildH : " + height + " ; ";
            str += "buildId : " + buildingId + " ; ";
            str += "alpha : " + wallAlpha + " ; ";
            str += "id : " + id + " ; ";
            return str;
        }


        public int compareTox01y01(CutPoint cutPoint) {
            if(this.coordinate.x < cutPoint.coordinate.x ||
                    (this.coordinate.x == cutPoint.coordinate.x && this.coordinate.y < cutPoint.coordinate.y)) {
                return -1;
            }
            if(this.coordinate.x == cutPoint.coordinate.x && this.coordinate.y == cutPoint.coordinate.y) {
                return 0;
            }
            else {
                return 1;
            }
        }

        public int compareTox10y01(CutPoint cutPoint) {
            if(this.coordinate.x > cutPoint.coordinate.x ||
                    (this.coordinate.x == cutPoint.coordinate.x && this.coordinate.y < cutPoint.coordinate.y)) {
                return -1;
            }
            if(this.coordinate.x == cutPoint.coordinate.x && this.coordinate.y == cutPoint.coordinate.y) {
                return 0;
            }
            else {
                return 1;
            }
        }

        public int compareTox01y10(CutPoint cutPoint) {
            if(this.coordinate.x < cutPoint.coordinate.x ||
                    (this.coordinate.x == cutPoint.coordinate.x && this.coordinate.y > cutPoint.coordinate.y)) {
                return -1;
            }
            if(this.coordinate.x == cutPoint.coordinate.x && this.coordinate.y == cutPoint.coordinate.y) {
                return 0;
            }
            else {
                return 1;
            }
        }

        public int compareTox10y10(CutPoint cutPoint) {
            if(this.coordinate.x > cutPoint.coordinate.x ||
                    (this.coordinate.x == cutPoint.coordinate.x && this.coordinate.y > cutPoint.coordinate.y)) {
                return -1;
            }
            if(this.coordinate.x == cutPoint.coordinate.x && this.coordinate.y == cutPoint.coordinate.y) {
                return 0;
            }
            else {
                return 1;
            }
        }

        @Override
        public int compareTo(CutPoint cutPoint) {
            if(this.coordinate.x < cutPoint.coordinate.x ||
                    (this.coordinate.x == cutPoint.coordinate.x && this.coordinate.y < cutPoint.coordinate.y)) {
                return -1;
            }
            if(this.coordinate.x == cutPoint.coordinate.x && this.coordinate.y == cutPoint.coordinate.y) {
                return 0;
            }
            else {
                return 1;
            }
        }

        public boolean isCorner(){
            return corner;
        }
    }

    public interface Obstacle{
        Collection<? extends Wall> getWalls();
    }

    /**
     * Building represented by its {@link Geometry} footprint and its height.
     */
    public static class Building implements Obstacle {
        /** Building footprint. */
        private Polygon poly;
        /** Height of the building. */
        private final double height;
        private double zTopo = 0.0;
        /** Absorption coefficients. */
        private final List<Double> alphas;

        /** if true take into account z value on Buildings Polygons */
        private final boolean zBuildings;

        /** Primary key of the building in the database. */
        private int pk = -1;
        private List<Wall> walls = new ArrayList<>();

        /**
         *
         */
        public void poly2D_3D(){

            GeometryFactory f = new GeometryFactory();

            LinearRing shell2D = poly.getExteriorRing();
            Coordinate[] newCoordinate = new Coordinate[shell2D.getNumPoints()];
            for (int idCoordinate=0;idCoordinate<newCoordinate.length;idCoordinate++) {
                newCoordinate[idCoordinate] = new Coordinate(shell2D.getCoordinateN(idCoordinate).getX(),shell2D.getCoordinateN(idCoordinate).getY(),0.0);
            }

            LinearRing shell3D = f.createLinearRing(newCoordinate);

            LinearRing[] holes = new LinearRing[poly.getNumInteriorRing()];
            for (int idHole=0;idHole<holes.length;idHole++){
                LinearRing lr2D = poly.getInteriorRingN(idHole);
                newCoordinate = new Coordinate[lr2D.getNumPoints()];
                for (int idCoordinate=0;idCoordinate<newCoordinate.length;idCoordinate++) {
                    newCoordinate[idCoordinate] = new Coordinate(lr2D.getCoordinateN(idCoordinate).getX(),
                            lr2D.getCoordinateN(idCoordinate).getY(),
                            0.0);
                }

                holes[idHole]=f.createLinearRing(newCoordinate);
            }


            Polygon newPoly = f.createPolygon(shell3D, holes);
            this.poly = newPoly;
        }


        /**
         * Main constructor.
         * @param poly   {@link Geometry} footprint of the building.
         * @param height Height of the building.
         * @param alphas Absorption coefficients.
         * @param key Primary key of the building in the database.
         */
        public Building(Polygon poly, double height, List<Double> alphas, int key, boolean zBuildings) {
            this.poly = poly;
            this.height = height;
            this.alphas = new ArrayList<>();
            this.alphas.addAll(alphas);
            this.pk = key;
            this.zBuildings = zBuildings;
        }

        /**
         * get Height from Building
         * @return height
         */
        public double getHeight() { return height; }


        /**
         * Retrieve the building footprint.
         * @return The building footprint.
         */
        public Polygon getGeometry() {
            return poly;
        }

        /**
         * Retrieve the absorption coefficients.
         * @return The absorption coefficients.
         */
        public List<Double> getAlphas() {
            return alphas;
        }

        /**
         * Retrieve the primary key of the building in the database. If there is no primary key, returns -1.
         * @return The primary key of the building in the database or -1.
         */
        public int getPrimaryKey() {
            return pk;
        }

        //TODO use instead the min Ztopo
        public double updateZTopo(ProfileBuilder profileBuilder) {
            Coordinate[] coordinates = poly.getCoordinates();
            double minZ = 0.0;
            for (int i = 0; i < coordinates.length-1; i++) {
                minZ += profileBuilder.getZGround(coordinates[i]);
            }
            zTopo = minZ/(coordinates.length-1);
            return zTopo;
        }

        public double getZ() {
            return zTopo + height;
        }

        public void setWalls(List<Wall> walls) {
            this.walls = walls;
            walls.forEach(w -> w.setObstacle(this));
        }

        @Override
        public Collection<? extends Wall> getWalls() {
            return walls;
        }
    }

    /**
     * Building wall or topographic triangle mesh side.
     */
    public static class Wall implements Obstacle {
        /** Segment of the wall. */
        private final LineString line;
        /** Type of the wall */
        private final IntersectionType type;
        /** Id or index of the source building or topographic triangle. */
        private final int originId;
        /** Wall alpha value. */
        private List<Double> alphas;
        /** Wall height, if -1, use z coordinate. */
        private double height;
        private boolean hasP0Neighbour = false;
        private boolean hasP1Neighbour = false;
        public Coordinate p0;
        public Coordinate p1;
        private LineSegment ls;
        private Obstacle obstacle = this;
        private int processedWallIndex;

        /**
         * Constructor using segment and id.
         * @param line     Segment of the wall.
         * @param originId Id or index of the source building or topographic triangle.
         */
        public Wall(LineSegment line, int originId, IntersectionType type) {
            this.p0 = line.p0;
            this.p1 = line.p1;
            this.line = FACTORY.createLineString(new Coordinate[]{p0, p1});
            this.ls = line;
            this.originId = originId;
            this.type = type;
            this.alphas = new ArrayList<>();
        }
        /**
         * Constructor using segment and id.
         * @param line     Segment of the wall.
         * @param originId Id or index of the source building or topographic triangle.
         */
        public Wall(LineString line, int originId, IntersectionType type) {
            this.line = line;
            this.p0 = line.getCoordinateN(0);
            this.p1 = line.getCoordinateN(line.getNumPoints()-1);
            this.ls = new LineSegment(p0, p1);
            this.originId = originId;
            this.type = type;
            this.alphas = new ArrayList<>();
        }

        /**
         * Constructor using start/end point and id.
         * @param p0       Start point of the segment.
         * @param p1       End point of the segment.
         * @param originId Id or index of the source building or topographic triangle.
         */
        public Wall(Coordinate p0, Coordinate p1, int originId, IntersectionType type) {
            this.line = FACTORY.createLineString(new Coordinate[]{p0, p1});
            this.p0 = p0;
            this.p1 = p1;
            this.ls = new LineSegment(p0, p1);
            this.originId = originId;
            this.type = type;
            this.alphas = new ArrayList<>();
        }

        /**
         * Constructor using start/end point and id.
         * @param p0       Start point of the segment.
         * @param p1       End point of the segment.
         * @param originId Id or index of the source building or topographic triangle.
         */
        public Wall(Coordinate p0, Coordinate p1, int originId, IntersectionType type, boolean hasP0Neighbour, boolean hasP1Neighbour) {
            this.line = FACTORY.createLineString(new Coordinate[]{p0, p1});
            this.p0 = p0;
            this.p1 = p1;
            this.ls = new LineSegment(p0, p1);
            this.originId = originId;
            this.type = type;
            this.alphas = new ArrayList<>();
            this.hasP0Neighbour = hasP0Neighbour;
            this.hasP1Neighbour = hasP1Neighbour;
        }

        /**
         * @return Index of this wall in the ProfileBuild list
         */
        public int getProcessedWallIndex() {
            return processedWallIndex;
        }

        /**
         * @param processedWallIndex Index of this wall in the ProfileBuild list
         */
        public Wall setProcessedWallIndex(int processedWallIndex) {
            this.processedWallIndex = processedWallIndex;
            return this;
        }

        /**
         * Sets the wall alphas.
         * @param alphas Wall alphas.
         */
        public void setAlpha(List<Double> alphas) {
            this.alphas = alphas;
        }

        /**
         * Sets the wall height.
         * @param height Wall height.
         */
        public void setHeight(double height) {
            this.height = height;
        }

        public void setObstacle(Obstacle obstacle) {
            this.obstacle = obstacle;
        }

        /**
         * Retrieve the segment.
         * @return Segment of the wall.
         */
        public LineString getLine() {
            return line;
        }

        public LineSegment getLineSegment() {
            return ls;
        }

        /**
         * Retrieve the id or index of the source building or topographic triangle.
         * @return Id or index of the source building or topographic triangle.
         */
        public int getOriginId() {
            return originId;
        }

        /**
         * Retrieve the alphas of the wall.
         * @return Alphas of the wall.
         */
        public List<Double> getAlphas() {
            return alphas;
        }

        /**
         * Retrieve the height of the wall.
         * @return Height of the wall.
         */
        public double getHeight() {
            return height;
        }

        public IntersectionType getType() {
            return type;
        }

        public boolean hasP0Neighbour() {
            return hasP0Neighbour;
        }

        public boolean hasP1Neighbour() {
            return hasP1Neighbour;
        }

        public Obstacle getObstacle() {
            return obstacle;
        }

        @Override
        public Collection<? extends Wall> getWalls() {
            return Collections.singleton(this);
        }
    }

    /**
     * Ground effect.
     */
    public static class GroundEffect {
        /** Ground effect area footprint. */
        private final Geometry geom;
        /** Ground effect coefficient. */
        private final double coef;

        /**
         * Main constructor
         * @param geom Ground effect area footprint.
         * @param coef Ground effect coefficient.
         */
        public GroundEffect(Geometry geom, double coef) {
            this.geom = geom;
            this.coef = coef;
        }

        /**
         * Retrieve the ground effect area footprint.
         * @return The ground effect area footprint.
         */
        public Geometry getGeometry() {
            return geom;
        }

        /**
         * Retrieve the ground effect coefficient.
         * @return The ground effect coefficient.
         */
        public double getCoefficient(){
            return coef;
        }
    }


    //TODO methods to check
    public static final double wideAngleTranslationEpsilon = 0.01;

    /**
     * @param build 1-n based building identifier
     * @return
     */
    public ArrayList<Coordinate> getPrecomputedWideAnglePoints(int build) {
        return buildingsWideAnglePoints.get(build);
    }

    public ArrayList<Coordinate> getWideAnglePointsByBuilding(int build, double minAngle, double maxAngle) {
        ArrayList <Coordinate> verticesBuilding = new ArrayList<>();
        Coordinate[] ring = getBuilding(build-1).getGeometry().getExteriorRing().getCoordinates().clone();
        if(!isCCW(ring)) {
            for (int i = 0; i < ring.length / 2; i++) {
                Coordinate temp = ring[i];
                ring[i] = ring[ring.length - 1 - i];
                ring[ring.length - 1 - i] = temp;
            }
        }
        for(int i=0; i < ring.length - 1; i++) {
            int i1 = i > 0 ? i-1 : ring.length - 2;
            int i3 = i + 1;
            double smallestAngle = Angle.angleBetweenOriented(ring[i1], ring[i], ring[i3]);
            double openAngle;
            if(smallestAngle >= 0) {
                // corresponds to a counterclockwise (CCW) rotation
                openAngle = smallestAngle;
            } else {
                // corresponds to a clockwise (CW) rotation
                openAngle = 2 * Math.PI + smallestAngle;
            }
            // Open Angle is the building angle in the free field area
            if(openAngle > minAngle && openAngle < maxAngle) {
                // corresponds to a counterclockwise (CCW) rotation
                double midAngle = openAngle / 2;
                double midAngleFromZero = Angle.angle(ring[i], ring[i1]) + midAngle;
                Coordinate offsetPt = new Coordinate(
                        ring[i].x + Math.cos(midAngleFromZero) * wideAngleTranslationEpsilon,
                        ring[i].y + Math.sin(midAngleFromZero) * wideAngleTranslationEpsilon,
                        buildings.get(build - 1).getGeometry().getCoordinate().z + wideAngleTranslationEpsilon);
                verticesBuilding.add(offsetPt);
            }
        }
        verticesBuilding.add(verticesBuilding.get(0));
        return verticesBuilding;
    }

    /**
     * Find all buildings (polygons) that 2D cross the line p1->p2
     * @param p1 first point of line
     * @param p2 second point of line
     * @param visitor Iterate over found buildings
     * @return Building identifier (1-n) intersected by the line
     */
    public void getBuildingsOnPath(Coordinate p1, Coordinate p2, ItemVisitor visitor) {
        try {
            List<LineSegment> lines = splitSegment(p1, p2, maxLineLength);
            for(LineSegment segment : lines) {
                Envelope pathEnv = new Envelope(segment.p0, segment.p1);
                    buildingTree.query(pathEnv, visitor);
            }
        } catch (IllegalStateException ex) {
            //Ignore
        }
    }

    public void getWallsOnPath(Coordinate p1, Coordinate p2, ItemVisitor visitor) {
        Envelope pathEnv = new Envelope(p1, p2);
        try {
            wallTree.query(pathEnv, visitor);
        } catch (IllegalStateException ex) {
            //Ignore
        }
    }


    /**
     * Hold two integers. Used to store unique triangle segments
     */
    private static class IntegerTuple {
        int nodeIndexA;
        int nodeIndexB;
        int triangleIdentifier;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IntegerTuple that = (IntegerTuple) o;
            return nodeIndexA == that.nodeIndexA && nodeIndexB == that.nodeIndexB;
        }

        @Override
        public String toString() {
            return "IntegerTuple{" + "nodeIndexA=" + nodeIndexA + ", nodeIndexB=" + nodeIndexB + ", " +
                    "triangleIdentifier=" + triangleIdentifier + '}';
        }

        @Override
        public int hashCode() {
            return Objects.hash(nodeIndexA, nodeIndexB);
        }

        public IntegerTuple(int nodeIndexA, int nodeIndexB, int triangleIdentifier) {
            if(nodeIndexA < nodeIndexB) {
                this.nodeIndexA = nodeIndexA;
                this.nodeIndexB = nodeIndexB;
            } else {
                this.nodeIndexA = nodeIndexB;
                this.nodeIndexB = nodeIndexA;
            }
            this.triangleIdentifier = triangleIdentifier;
        }
    }
}
