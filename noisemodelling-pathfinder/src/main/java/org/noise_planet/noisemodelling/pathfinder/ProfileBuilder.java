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
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.ItemVisitor;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.triangulate.quadedge.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

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
    /** Class {@link java.util.logging.Logger}. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileBuilder.class);
    /** Default RTree node capacity. */
    private static final int TREE_NODE_CAPACITY = 20;
    /** {@link Geometry} factory. */
    private static final GeometryFactory FACTORY = new GeometryFactory();
    private static final double DELTA = 1e-3;

    /** If true, no more data can be add. */
    private boolean isFeedingFinished = false;
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
    private double maxLineLength = 15;
    /** List of buildings. */
    private final List<Building> buildings = new ArrayList<>();
    /** List of walls. */
    private final List<Wall> walls = new ArrayList<>();
    /** Building RTree. */
    private STRtree buildingTree;
    /** Global RTree. */
    private STRtree rtree;

    /** List of topographic points. */
    private final List<Coordinate> topoPoints = new ArrayList<>();
    /** List of topographic lines. */
    private final List<LineString> topoLines = new ArrayList<>();
    /** Topographic triangle facets. */
    private List<Triangle> topoTriangles = new ArrayList<>();
    /** Topographic Vertices .*/
    private List<Coordinate> vertices = new ArrayList<>();
    /** Topographic RTree. */
    private STRtree topoTree;

    /** List of ground effects. */
    private final List<GroundEffect> groundEffects = new ArrayList<>();

    /** Sources geometries.*/
    private final List<Geometry> sources = new ArrayList<>();
    /** Sources RTree. */
    private STRtree sourceTree;
    /** Receivers .*/
    private final List<Coordinate> receivers = new ArrayList<>();

    /** List of processed walls. */
    private final List<Wall> processedWalls = new ArrayList<>();

    /** Global envelope of the builder. */
    private Envelope envelope;
    /** Maximum area of triangles. */
    private double maxArea;

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
        if(building.poly == null) {
            LOGGER.error("Cannot add a building with null geometry.");
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
            polyCoords[l-1] = new Coordinate(coords[0]);
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
            polyCoords[l-1] = new Coordinate(coords[0]);
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
        return addBuilding(geom, Double.NaN, id);
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
            polyCoords[l-1] = new Coordinate(coords[0]);
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
            polyCoords[l-1] = new Coordinate(coords[0]);
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
            polyCoords[l-1] = new Coordinate(coords[0]);
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
        return addBuilding(geom, Double.NaN, alphas, -1);
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
            polyCoords[l-1] = new Coordinate(coords[0]);
        }
        else {
            polyCoords = coords;
        }
        return addBuilding(FACTORY.createPolygon(polyCoords), Double.NaN, alphas, -1);
    }

    /**
     * Add the given {@link Geometry} footprint, height and alphas (absorption coefficients) as building.
     * @param geom   Building footprint.
     * @param alphas Absorption coefficients.
     * @param id     Database primary key.
     */
    public ProfileBuilder addBuilding(Geometry geom, List<Double> alphas, int id) {
        return addBuilding(geom, Double.NaN, alphas, id);
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
            polyCoords[l-1] = new Coordinate(coords[0]);
        }
        else {
            polyCoords = coords;
        }
        return addBuilding(FACTORY.createPolygon(polyCoords), Double.NaN, alphas, id);
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
            Building building = new Building(poly, height, alphas, id);
            buildings.add(building);
            buildingTree.insert(building.poly.getEnvelopeInternal(), buildings.size());
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
            List<Wall> wallList = new ArrayList<>();
            for(int i=0; i<geom.getNumPoints()-1; i++) {
                Wall wall = new Wall(geom.getCoordinateN(i), geom.getCoordinateN(i+1), id, IntersectionType.BUILDING);
                wall.setHeight(height);
                wall.setAlpha(alphas);
                wallList.add(wall);
            }
            walls.addAll(wallList);
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
            if (Double.isNaN(point.z)) {
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
     * Retrieve the sources list.
     * @return The sources list.
     */
    public List<Geometry> getSources() {
        return sources;
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
        //Process sources
        if(!sources.isEmpty()) {
            sourceTree = new STRtree();
            for (int i = 0; i < sources.size(); i++) {
                sourceTree.insert(sources.get(i).getEnvelopeInternal(), i);
            }
        }
        //Process topographic points and lines
        if(topoPoints.size()+topoLines.size() > 1) {
            //Feed the Delaunay layer
            LayerDelaunay layerDelaunay = new LayerTinfour();
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
            List<Wall> topoWalls = new ArrayList<>();
            for (int i = 0; i < topoTriangles.size(); i++) {
                Triangle tri = topoTriangles.get(i);
                Coordinate vA = vertices.get(tri.getA());
                Coordinate vB = vertices.get(tri.getB());
                Coordinate vC = vertices.get(tri.getC());
                Envelope env = FACTORY.createLineString(new Coordinate[]{vA, vB, vC}).getEnvelopeInternal();
                topoTree.insert(env, i);
                topoWalls.add(new Wall(vA, vB, i, TOPOGRAPHY));
                topoWalls.add(new Wall(vB, vC, i, TOPOGRAPHY));
                topoWalls.add(new Wall(vC, vA, i, TOPOGRAPHY));
            }
            List<Wall> toRemove = new ArrayList<>();
            for(int i=0; i<topoWalls.size(); i++) {
                Wall wall = topoWalls.get(i);
                List<Wall> walls = topoWalls.subList(i, topoWalls.size());
                for(Wall w : walls) {
                    if((w.getLine().p0.equals(wall.getLine().p0) && w.getLine().p1.equals(wall.getLine().p1)) ||
                            (w.getLine().p0.equals(wall.getLine().p1) && w.getLine().p1.equals(wall.getLine().p0))) {
                        toRemove.add(wall);
                    }
                }
            }
            topoWalls.removeAll(toRemove);
            for(Wall wall : topoWalls) {
                processedWalls.add(wall);
                rtree.insert(wall.getLine().toGeometry(FACTORY).getEnvelopeInternal(), processedWalls.size() - 1);
            }
        }
        //Update building z
        if(topoTree != null) {
            for (Building b : buildings) {
                if(Double.isNaN(b.poly.getCoordinate().z) || b.poly.getCoordinate().z == 0.0) {
                    b.poly.apply(new UpdateZ(b.height + b.updateZTopo(this)));
                }
            }
            for (Wall w : walls) {
                if(Double.isNaN(w.line.p0.z) || w.line.p0.z == 0.0) {
                    w.line.p0.z = w.height + getZGround(w.line.p0);
                }
                if(Double.isNaN(w.line.p1.z) || w.line.p1.z == 0.0) {
                    w.line.p1.z = w.height + getZGround(w.line.p1);
                }
            }
        }
        else {
            for (Building b : buildings) {
                if(b != null && b.poly != null && b.poly.getCoordinate() != null && (
                        Double.isNaN(b.poly.getCoordinate().z) || b.poly.getCoordinate().z == 0.0)) {
                    b.poly.apply(new UpdateZ(b.height));
                }
            }
            for (Wall w : walls) {
                if(Double.isNaN(w.line.p0.z) || w.line.p0.z == 0.0) {
                    w.line.p0.z = w.height;
                }
                if(Double.isNaN(w.line.p1.z) || w.line.p1.z == 0.0) {
                    w.line.p1.z = w.height;
                }
            }
        }
        //Process buildings
        rtree = new STRtree(buildingNodeCapacity);
        for (int j = 0; j < buildings.size(); j++) {
            Building building = buildings.get(j);
            Coordinate[] coords = building.poly.getCoordinates();
            for (int i = 0; i < coords.length - 1; i++) {
                LineSegment lineSegment = new LineSegment(coords[i], coords[i + 1]);
                processedWalls.add(new Wall(lineSegment, j, IntersectionType.BUILDING));
                rtree.insert(lineSegment.toGeometry(FACTORY).getEnvelopeInternal(), processedWalls.size()-1);
            }
        }
        for (int j = 0; j < walls.size(); j++) {
            Wall wall = walls.get(j);
            Coordinate[] coords = new Coordinate[]{wall.line.p0, wall.line.p1};
            for (int i = 0; i < coords.length - 1; i++) {
                LineSegment lineSegment = new LineSegment(coords[i], coords[i + 1]);
                processedWalls.add(new Wall(lineSegment, j, IntersectionType.WALL));
                rtree.insert(lineSegment.toGeometry(FACTORY).getEnvelopeInternal(), processedWalls.size()-1);
            }
        }
        //Process the ground effects
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
                Coordinate[] coords = poly.getCoordinates();
                for (int k = 0; k < coords.length - 1; k++) {
                    LineSegment line = new LineSegment(coords[k], coords[k + 1]);
                    processedWalls.add(new Wall(line, j, IntersectionType.GROUND_EFFECT));
                    rtree.insert(line.toGeometry(FACTORY).getEnvelopeInternal(), processedWalls.size() - 1);
                }
            }
        }
        return this;
    }

    public double getZ(Coordinate reflectionPt) {
        List<Integer> ids = buildingTree.query(new Envelope(reflectionPt));
        if(ids.isEmpty()) {
            return getZGround(reflectionPt);
        }
        else {
            return buildings.get(ids.get(0)-1).getGeometry().getCoordinate().z;
        }
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

    /**
     * Retrieve the cutting profile following the line build from the given coordinates.
     * @param c0 Starting point.
     * @param c1 Ending point.
     * @return Cutting profile.
     */
    public CutProfile getProfile(Coordinate c0, Coordinate c1, double gS) {
        CutProfile profile = new CutProfile();

        List<LineSegment> lines = new ArrayList<>();
        LineSegment fullLine = new LineSegment(c0, c1);
        double l = fullLine.getLength();
        //If the line length if greater than the MAX_LINE_LENGTH value, split it into multiple lines
        if(l < maxLineLength) {
            lines.add(fullLine);
        }
        else {
            double frac = maxLineLength /l;
            for(int i = 0; i<l/ maxLineLength; i++) {
                Coordinate p0 = fullLine.pointAlong(i*frac);
                p0.z = fullLine.p0.z + (fullLine.p1.z - fullLine.p0.z) * fullLine.segmentFraction(p0);
                Coordinate p1 = fullLine.pointAlong(Math.min((i+1)*frac, 1.0));
                p1.z = fullLine.p0.z + (fullLine.p1.z - fullLine.p0.z) * fullLine.segmentFraction(p1);
                lines.add(new LineSegment(p0, p1));
            }
        }
        //Topography
        if(topoTree != null) {
            List<CutPoint> topoCutPts = new ArrayList<>();
            for (LineSegment line : lines) {
                List<Integer> indexes = new ArrayList<>(topoTree.query(line.toGeometry(FACTORY).getEnvelopeInternal()));
                indexes = indexes.stream().distinct().collect(Collectors.toList());
                for (int i : indexes) {
                    Triangle triangle = topoTriangles.get(i);
                    LineSegment triLine = new LineSegment(vertices.get(triangle.getA()), vertices.get(triangle.getB()));
                    Coordinate intersection = line.intersection(triLine);
                    if (intersection != null) {
                        intersection.z = triLine.p0.z + (triLine.p1.z - triLine.p0.z) * triLine.segmentFraction(intersection);
                        topoCutPts.add(new CutPoint(intersection, TOPOGRAPHY, i));
                    }
                    triLine = new LineSegment(vertices.get(triangle.getB()), vertices.get(triangle.getC()));
                    intersection = line.intersection(triLine);
                    if (intersection != null) {
                        intersection.z = triLine.p0.z + (triLine.p1.z - triLine.p0.z) * triLine.segmentFraction(intersection);
                        topoCutPts.add(new CutPoint(intersection, TOPOGRAPHY, i));
                    }
                    triLine = new LineSegment(vertices.get(triangle.getC()), vertices.get(triangle.getA()));
                    intersection = line.intersection(triLine);
                    if (intersection != null) {
                        intersection.z = triLine.p0.z + (triLine.p1.z - triLine.p0.z) * triLine.segmentFraction(intersection);
                        topoCutPts.add(new CutPoint(intersection, TOPOGRAPHY, i));
                    }
                }
            }
            List<CutPoint> toRemove = new ArrayList<>();
            for(int i=0; i<topoCutPts.size(); i++) {
                CutPoint pt = topoCutPts.get(i);
                List<CutPoint> remaining = topoCutPts.subList(i+1, topoCutPts.size());
                for(CutPoint rPt : remaining) {
                    if(pt.getCoordinate().x == rPt.getCoordinate().x &&
                            pt.getCoordinate().y == rPt.getCoordinate().y) {
                        toRemove.add(pt);
                        break;
                    }
                }
            }
            topoCutPts.removeAll(toRemove);
            topoCutPts.forEach(profile::addCutPt);
        }
        //Buildings and Ground effect
        if(rtree != null) {
            List<Integer> indexes = new ArrayList<>();
            for (LineSegment line : lines) {
                indexes.addAll(rtree.query(line.toGeometry(FACTORY).getEnvelopeInternal()));
            }
            indexes = indexes.stream().distinct().collect(Collectors.toList());
            for (int i : indexes) {
                Wall facetLine = processedWalls.get(i);
                Coordinate intersection = fullLine.intersection(facetLine.line);
                if (intersection != null) {
                    if(!Double.isNaN(facetLine.line.p0.z) && !Double.isNaN(facetLine.line.p1.z)) {
                        intersection.z = Vertex.interpolateZ(intersection, facetLine.line.p0, facetLine.line.p1);
                    }
                    else if(topoTree == null) {
                        intersection.z = Double.NaN;
                    }
                    else {
                        intersection.z = getZGround(intersection);
                    }

                    if(facetLine.type == IntersectionType.BUILDING) {
                        profile.addBuildingCutPt(intersection, facetLine.originId);
                    }
                    else if(facetLine.type == IntersectionType.WALL) {
                        profile.addWallCutPt(intersection, facetLine.originId);
                    }
                    else if(facetLine.type == IntersectionType.GROUND_EFFECT) {
                        Coordinate c = new Coordinate(intersection.x, intersection.y, getZ(intersection));
                        profile.addGroundCutPt(c, facetLine.originId);
                    }
                }
            }
        }

        //Sort all the cut point in order to set the ground coefficients.
        profile.sort();
        profile.addSource(c0);
        profile.addReceiver(c1);


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
        GroundEffect currentGround = null;
        Point p0 = FACTORY.createPoint(c0);
        for(GroundEffect ground : groundEffects) {
            if(ground.geom.contains(p0)) {
                currentGround = ground;
            }
        }
        List<CutPoint> toRemove = new ArrayList<>();
        CutPoint previous = null;
        for(CutPoint cut : profile.pts) {
            if(cut.type == IntersectionType.GROUND_EFFECT) {
                if(currentGround == groundEffects.get(cut.id)) {
                    currentGround = null;
                }
                else {
                    currentGround = groundEffects.get(cut.id);
                    if(previous != null &&
                            cut.getCoordinate().x == previous.getCoordinate().x &&
                            cut.getCoordinate().y == previous.getCoordinate().y &&
                            previous.getType() != SOURCE &&
                            previous.getType() != RECEIVER ) {
                        toRemove.add(previous);
                    }
                }
            }
            cut.groundCoef = currentGround != null ? currentGround.coef : gS;
            previous = cut;
        }
        profile.pts.removeAll(toRemove);
        toRemove.clear();
        //Get all the topo points which are aligned with their predecessor and successor and remove them
        Coordinate cTopo0 = null;
        Coordinate cTopo1 = null;
        Coordinate cTopo2 = null;
        for(int i=0; i<profile.pts.size(); i++) {
            //In case of Source or Receiver, use Z topo
            if(profile.pts.get(i).type == TOPOGRAPHY || profile.pts.get(i).type == RECEIVER || profile.pts.get(i).type == SOURCE){
                if(cTopo0 == null) {
                    cTopo0 = profile.pts.get(i).coordinate;
                    cTopo0 = new Coordinate(cTopo0.x, cTopo0.y, profile.pts.get(i).type == SOURCE ? getZGround(cTopo0) : cTopo0.z);
                }
                else if(cTopo1 == null) {
                    cTopo1 = profile.pts.get(i).coordinate;
                }
                else {
                    if(cTopo2 != null) {
                        cTopo0 = cTopo1;
                        cTopo1 = cTopo2;
                    }
                    cTopo2 = profile.pts.get(i).coordinate;
                    cTopo2 = new Coordinate(cTopo2.x, cTopo2.y, profile.pts.get(i).type == RECEIVER ? getZGround(cTopo2) : cTopo2.z);

                    if (cTopo0.z == cTopo1.z && cTopo1.z == cTopo2.z ||
                            CGAlgorithms3D.distancePointSegment(cTopo1, cTopo0, cTopo2) < DELTA) {
                        final Coordinate cTopo = cTopo1;
                        toRemove.add(profile.pts.stream()
                                .filter(cut -> cut.coordinate.x==cTopo.x && cut.coordinate.y==cTopo.y)
                                .findFirst()
                                .get());
                    }
                }
            }
        }
        profile.pts.removeAll(toRemove);
        return profile;
    }

    /**
     * Get the topographic height of a point.
     * @param c Coordinate of the point.
     * @return Topographic height of the point.
     */
    @Deprecated
    public double getZGround(Coordinate c) {
        if(topoTree == null) {
            return 0.0;
        }
        List<Integer> list = new ArrayList<>();
        Envelope env = new Envelope(c);
        while(list.isEmpty()) {
            env.expandBy(maxLineLength);
            list = (List<Integer>)topoTree.query(env);
        }
        for (int i : list) {
            Triangle tri = topoTriangles.get(i);
            Coordinate p1 = vertices.get(tri.getA());
            Coordinate p2 = vertices.get(tri.getB());
            Coordinate p3 = vertices.get(tri.getC());
            Polygon poly = FACTORY.createPolygon(new Coordinate[]{p1, p2, p3, p1});
            if (poly.intersects(FACTORY.createPoint(c))) {
                return Vertex.interpolateZ(c, p1, p2, p3);
            }
        }
        return 0.0;
    }
    public double getZGround(CutPoint cut) {
        if(cut.zGround != null) {
            return cut.zGround;
        }
        if(topoTree == null) {
            cut.zGround = null;
            return 0.0;
        }
        List<Integer> list = new ArrayList<>();
        Envelope env = new Envelope(cut.coordinate);
        while(list.isEmpty()) {
            env.expandBy(maxLineLength);
            list = (List<Integer>)topoTree.query(env);
        }
        for (int i : list) {
            Triangle tri = topoTriangles.get(i);
            Coordinate p1 = vertices.get(tri.getA());
            Coordinate p2 = vertices.get(tri.getB());
            Coordinate p3 = vertices.get(tri.getC());
            Polygon poly = FACTORY.createPolygon(new Coordinate[]{p1, p2, p3, p1});
            if (poly.intersects(FACTORY.createPoint(cut.coordinate))) {
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
    enum IntersectionType {BUILDING, WALL, TOPOGRAPHY, GROUND_EFFECT, SOURCE, RECEIVER;

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
        private final List<CutPoint> pts = new ArrayList<>();
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
         * @param coord Coordinate of the cutting point.
         * @param id    Id of the cut building.
         */
        public void addBuildingCutPt(Coordinate coord, int id) {
            pts.add(new CutPoint(coord, IntersectionType.BUILDING, id));
            pts.get(pts.size()-1).buildingId = id;
            hasBuildingInter = true;
        }

        /**
         * Add a building cutting point.
         * @param coord Coordinate of the cutting point.
         * @param id    Id of the cut building.
         */
        public void addWallCutPt(Coordinate coord, int id) {
            pts.add(new CutPoint(coord, IntersectionType.WALL, id));
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
        public void sort() {
            pts.sort(CutPoint::compareTo);
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
            double totLength = new LineSegment(p0.getCoordinate(), p1.getCoordinate()).getLength();
            double rsLength = 0.0;
            //List<CutPoint> pts = getCutPoints().stream().sorted(CutPoint::compareTo).collect(Collectors.toList());

            List<CutPoint> pts = getCutPoints().stream()
                    .filter(cutPoint -> cutPoint.getType() != TOPOGRAPHY)
                    .filter(cutPoint -> cutPoint.getType() != IntersectionType.BUILDING)
                    .sorted(CutPoint::compareTo).collect(Collectors.toList());


            for(CutPoint cut : pts) {
                if(cut.compareTo(current)>0 && cut.compareTo(p1)<0) {
                    LineSegment seg = new LineSegment(current.getCoordinate(), cut.getCoordinate());
                    rsLength += seg.getLength() * current.getGroundCoef();
                    current = cut;
                }
            }
            LineSegment seg = new LineSegment(current.getCoordinate(), p1.getCoordinate());
            rsLength += seg.getLength() * p1.getGroundCoef();
            return rsLength / totLength;
        }

        public double getGPath() {
            return getGPath(getSource(), getReceiver());
        }

        public boolean isFreeField() {
            if(isFreeField == null) {
                isFreeField = true;
                List<CutPoint> pts = getCutPoints().stream()
                        .filter(cutPoint -> cutPoint.getType() == IntersectionType.BUILDING ||
                                cutPoint.getType() == IntersectionType.WALL ||
                                cutPoint.getType() == TOPOGRAPHY)
                        .collect(Collectors.toList());
                LineSegment srcRcvLine = new LineSegment(source.getCoordinate(), receiver.getCoordinate());
                for(CutPoint pt : pts) {
                    double frac = srcRcvLine.segmentFraction(pt.getCoordinate());
                    double z = source.getCoordinate().z + frac * (receiver.getCoordinate().z-source.getCoordinate().z);
                    if(z < pt.getCoordinate().z) {
                        isFreeField = false;
                        break;
                    }
                }
            }
            return isFreeField;
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

        /**
         * Constructor using a {@link Coordinate}.
         * @param coord Coordinate to copy.
         * @param type  Intersection type.
         * @param id    Identifier of the cut element.
         */
        public CutPoint(Coordinate coord, IntersectionType type, int id) {
            this.coordinate = new Coordinate(coord.x, coord.y, coord.z);
            this.type = type;
            this.id = id;
            this.buildingId = -1;
            this.wallId = -1;
            this.groundCoef = 0;
            this.wallAlpha = new ArrayList<>();
            this.height = 0;
            this.zGround = null;
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
            return str;
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
    }

    /**
     * Building represented by its {@link Geometry} footprint and its height.
     */
    public static class Building {
        /** Building footprint. */
        private final Polygon poly;

        public double getHeight() {
            return height;
        }

        /** Height of the building. */
        private final double height;
        private double zTopo = Double.NaN;
        /** Absorption coefficients. */
        private final List<Double> alphas;
        /** Primary key of the building in the database. */
        private int pk = -1;

        /**
         * Main constructor.
         * @param poly   {@link Geometry} footprint of the building.
         * @param height Height of the building.
         * @param alphas Absorption coefficients.
         * @param key Primary key of the building in the database.
         */
        public Building(Polygon poly, double height, List<Double> alphas, int key) {
            this.poly = poly;
            this.height = height;
            this.alphas = new ArrayList<>();
            this.alphas.addAll(alphas);
            this.pk = key;
        }

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
    }

    /**
     * Building wall or topographic triangle mesh side.
     */
    public static class Wall {
        /** Segment of the wall. */
        private final LineSegment line;
        /** Type of the wall */
        private final IntersectionType type;
        /** Id or index of the source building or topographic triangle. */
        private final int originId;
        /** Wall alpha value. */
        private List<Double> alphas;
        /** Wall height, if -1, use z coordinate. */
        private double height;

        /**
         * Constructor using segment and id.
         * @param line     Segment of the wall.
         * @param originId Id or index of the source building or topographic triangle.
         */
        public Wall(LineSegment line, int originId, IntersectionType type) {
            this.line = line;
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
            this.line = new LineSegment(p0, p1);
            this.originId = originId;
            this.type = type;
            this.alphas = new ArrayList<>();
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

        /**
         * Retrieve the segment.
         * @return Segment of the wall.
         */
        public LineSegment getLine() {
            return line;
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
    public List<Coordinate> getWideAnglePointsByBuilding(int build, double minAngle, double maxAngle) {
        List <Coordinate> verticesBuilding = new ArrayList<>();
        Coordinate[] ring = getBuilding(build-1).getGeometry().getExteriorRing().getCoordinates();
        if(!Orientation.isCCW(ring)) {
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
        Envelope pathEnv = new Envelope(p1, p2);
        try {
            buildingTree.query(pathEnv, visitor);
        } catch (IllegalStateException ex) {
            //Ignore
        }
    }
}
