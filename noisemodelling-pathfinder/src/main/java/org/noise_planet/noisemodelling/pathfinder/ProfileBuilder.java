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
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.ItemVisitor;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.triangulate.quadedge.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
        buildingTree = new STRtree(TREE_NODE_CAPACITY);
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
    }

    /**
     * Add the given {@link Geometry} footprint.
     * @param building Building.
     */
    public Building addBuilding(Building building) {
        if(!isFeedingFinished) {
            if(envelope == null) {
                envelope = building.poly.getEnvelopeInternal();
            }
            else {
                envelope.expandToInclude(building.poly.getEnvelopeInternal());
            }
            buildings.add(building);
            buildingTree.insert(building.poly.getEnvelopeInternal(), buildings.size());
            return building;
        }
        else{
            LOGGER.warn("Cannot add building, feeding is finished.");
            return null;
        }
    }

    /**
     * Add the given {@link Geometry} footprint.
     * @param geom   Building footprint.
     */
    public Building addBuilding(Geometry geom) {
        return addBuilding(geom, -1);
    }

    /**
     * Add the given {@link Geometry} footprint and height as building.
     * @param geom   Building footprint.
     * @param height Building height.
     */
    public Building addBuilding(Geometry geom, double height) {
        return addBuilding(geom, height, new ArrayList<>());
    }

    /**
     * Add the given {@link Geometry} footprint.
     * @param geom   Building footprint.
     * @param id     Database primary key.
     */
    public Building addBuilding(Geometry geom, int id) {
        return addBuilding(geom, Double.NaN, id);
    }

    /**
     * Add the given {@link Geometry} footprint, height, alphas (absorption coefficients) and a database id as building.
     * @param geom   Building footprint.
     * @param height Building height.
     * @param id     Database id.
     */
    public Building addBuilding(Geometry geom, double height, int id) {
        return addBuilding(geom, height, new ArrayList<>(), id);
    }

    /**
     * Add the given {@link Geometry} footprint, height and alphas (absorption coefficients) as building.
     * @param geom   Building footprint.
     * @param height Building height.
     * @param alphas Absorption coefficients.
     */
    public Building addBuilding(Geometry geom, double height, List<Double> alphas) {
        return addBuilding(geom, height, alphas, -1);
    }

    /**
     * Add the given {@link Geometry} footprint, height and alphas (absorption coefficients) as building.
     * @param geom   Building footprint.
     * @param alphas Absorption coefficients.
     */
    public Building addBuilding(Geometry geom, List<Double> alphas) {
        return addBuilding(geom, Double.NaN, alphas, -1);
    }

    /**
     * Add the given {@link Geometry} footprint, height and alphas (absorption coefficients) as building.
     * @param geom   Building footprint.
     * @param alphas Absorption coefficients.
     * @param id     Database primary key.
     */
    public Building addBuilding(Geometry geom, List<Double> alphas, int id) {
        return addBuilding(geom, Double.NaN, alphas, id);
    }

    /**
     * Add the given {@link Geometry} footprint, height, alphas (absorption coefficients) and a database primary key
     * as building.
     * @param geom   Building footprint.
     * @param height Building height.
     * @param alphas Absorption coefficients.
     * @param id     Database primary key.
     */
    public Building addBuilding(Geometry geom, double height, List<Double> alphas, int id) {
        if(! (geom instanceof Polygon)) {
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
            return building;
        }
        else{
            LOGGER.warn("Cannot add building, feeding is finished.");
            return null;
        }
    }

    /**
     * Add the given {@link Geometry} footprint, height, alphas (absorption coefficients) and a database id as wall.
     * @param geom   Wall footprint.
     * @param height Wall height.
     * @param id     Database key.
     */
    public List<Wall> addWalls(LineString geom, double height, int id) {
        return addWalls(geom, height, 0.0, id);
    }

    /**
     * Add the given {@link Geometry} footprint, height, alphas (absorption coefficients) and a database id as wall.
     * @param geom   Wall footprint.
     * @param id     Database key.
     */
    public List<Wall> addWalls(LineString geom, int id) {
        return addWalls(geom, 0.0, -1, id);
    }

    /**
     * Add the given {@link Geometry} footprint, height, alphas (absorption coefficients) and a database id as wall.
     * @param geom   Wall footprint.
     * @param height Wall height.
     * @param alphas Absorption coefficient.
     * @param id     Database key.
     */
    public List<Wall> addWalls(LineString geom, double height, double alphas, int id) {
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
            return wallList;
        }
        else{
            LOGGER.warn("Cannot add building, feeding is finished.");
            return null;
        }
    }

    /**
     * Add the topographic point in the data, to complete the topographic data.
     * @param point Topographic point.
     */
    public void addTopographicPoint(Coordinate point) {
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
    }

    /**
     * Add the topographic line in the data, to complete the topographic data.
     * @param lineSegment Topographic line.
     */
    public void addTopographicLine(LineString lineSegment) {
        if(!isFeedingFinished) {
            if(envelope == null) {
                envelope = lineSegment.getEnvelopeInternal();
            }
            else {
                envelope.expandToInclude(lineSegment.getEnvelopeInternal());
            }
            this.topoLines.add(lineSegment);
        }
    }

    /**
     * Add a ground effect.
     * @param geom        Ground effect area footprint.
     * @param coefficient Ground effect coefficient.
     */
    public void addGroundEffect(Geometry geom, double coefficient) {
        if(!isFeedingFinished) {
            if(envelope == null) {
                envelope = geom.getEnvelopeInternal();
            }
            else {
                envelope.expandToInclude(geom.getEnvelopeInternal());
            }
            this.groundEffects.add(new GroundEffect(geom, coefficient));
        }
    }

    /**
     * Add teh given coordinate as source.
     * @param coordinate Source coordinate to add.
     */
    public void addSource(Coordinate coordinate) {
        sources.add(FACTORY.createPoint(new Coordinate(coordinate.x, coordinate.y, coordinate.z)));
    }

    /**
     * Add teh given geometry as source.
     * @param geometry Source geometry to add.
     */
    public void addSource(Geometry geometry) {
        sources.add(geometry);
    }

    /**
     * Add teh given coordinate as receiver.
     * @param coordinate Receiver coordinate to add.
     */
    public void addReceiver(Coordinate coordinate) {
        receivers.add(new Coordinate(coordinate.x, coordinate.y, coordinate.z));
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
    public boolean finishFeeding() {
        isFeedingFinished = true;
        //Process sources
        if(!sources.isEmpty()) {
            sourceTree = new STRtree();
            for (int i = 0; i < sources.size(); i++) {
                sourceTree.insert(sources.get(i).getEnvelopeInternal(), i);
            }
        }
        //Process buildings
        rtree = new STRtree(buildingNodeCapacity);
        for (int j = 0; j < buildings.size(); j++) {
            Building building = buildings.get(j);
            Coordinate[] coords = building.poly.getCoordinates();
            for (int i = 0; i < coords.length - 1; i++) {
                LineSegment lineSegment = new LineSegment(new Coordinate(coords[i].x, coords[i].y, Double.isNaN(building.height) ? coords[i].z : building.height),
                        new Coordinate(coords[i + 1].x, coords[i + 1].y, Double.isNaN(building.height) ? coords[i+1].z : building.height));
                processedWalls.add(new Wall(lineSegment, j, IntersectionType.BUILDING));
                rtree.insert(lineSegment.toGeometry(FACTORY).getEnvelopeInternal(), processedWalls.size()-1);
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
                return false;
            }
            try {
                for (Coordinate topoPoint : topoPoints) {
                    layerDelaunay.addVertex(topoPoint);
                }
            } catch (LayerDelaunayError e) {
                LOGGER.error("Error while adding topographic points to Delaunay layer.", e);
                return false;
            }
            try {
                for (LineString topoLine : topoLines) {
                    //TODO ensure the attribute parameter is useless
                    layerDelaunay.addLineString(topoLine, -1);
                }
            } catch (LayerDelaunayError e) {
                LOGGER.error("Error while adding topographic points to Delaunay layer.", e);
                return false;
            }
            //Process Delaunay
            try {
                layerDelaunay.processDelaunay();
            } catch (LayerDelaunayError e) {
                LOGGER.error("Error while processing Delaunay.", e);
                return false;
            }
            try {
                topoTriangles = layerDelaunay.getTriangles();
            } catch (LayerDelaunayError e) {
                LOGGER.error("Error while getting triangles", e);
                return false;
            }
            //Feed the RTree
            topoTree = new STRtree(topoNodeCapacity);
            try {
                vertices = layerDelaunay.getVertices();
            } catch (LayerDelaunayError e) {
                LOGGER.error("Error while getting vertices", e);
                return false;
            }
            List<Wall> topoWalls = new ArrayList<>();
            for (int i = 0; i < topoTriangles.size(); i++) {
                Triangle tri = topoTriangles.get(i);
                Envelope env = FACTORY.createLineString(new Coordinate[]{
                                vertices.get(tri.getA()),
                                vertices.get(tri.getB()),
                                vertices.get(tri.getC())}).getEnvelopeInternal();
                topoTree.insert(env, i);
                topoWalls.add(new Wall(vertices.get(tri.getA()), vertices.get(tri.getB()), i, IntersectionType.TOPOGRAPHY));
                topoWalls.add(new Wall(vertices.get(tri.getB()), vertices.get(tri.getC()), i, IntersectionType.TOPOGRAPHY));
                topoWalls.add(new Wall(vertices.get(tri.getC()), vertices.get(tri.getA()), i, IntersectionType.TOPOGRAPHY));
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
        return true;
    }

    /**
     * Retrieve the cutting profile following the line build from the given coordinates.
     * @param c0 Starting point.
     * @param c1 Ending point.
     * @return Cutting profile.
     */
    public CutProfile getProfile(Coordinate c0, Coordinate c1) {
        CutProfile profile = new CutProfile();

        profile.addSource(c0);
        boolean firstBuildingFound = false;

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
                lines.add(new LineSegment(fullLine.pointAlong(i*frac), fullLine.pointAlong(Math.min((i+1)*frac, 1.0))));
            }
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
                        intersection.z = getTopoZ(intersection);
                    }

                    if(facetLine.type == IntersectionType.BUILDING) {
                        profile.addBuildingCutPt(intersection, facetLine.originId);
                    }
                    else if(facetLine.type == IntersectionType.GROUND_EFFECT) {
                        profile.addGroundCutPt(intersection, facetLine.originId);
                    }
                }
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
                        topoCutPts.add(new CutPoint(intersection, IntersectionType.TOPOGRAPHY, i));
                    }
                    triLine = new LineSegment(vertices.get(triangle.getB()), vertices.get(triangle.getC()));
                    intersection = line.intersection(triLine);
                    if (intersection != null) {
                        intersection.z = triLine.p0.z + (triLine.p1.z - triLine.p0.z) * triLine.segmentFraction(intersection);
                        topoCutPts.add(new CutPoint(intersection, IntersectionType.TOPOGRAPHY, i));
                    }
                    triLine = new LineSegment(vertices.get(triangle.getC()), vertices.get(triangle.getA()));
                    intersection = line.intersection(triLine);
                    if (intersection != null) {
                        intersection.z = triLine.p0.z + (triLine.p1.z - triLine.p0.z) * triLine.segmentFraction(intersection);
                        topoCutPts.add(new CutPoint(intersection, IntersectionType.TOPOGRAPHY, i));
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
        //Receiver
        profile.addReceiver(c1);

        //Sort all the cut point in order to set the ground coefficients.
        profile.sort();
        //If ordering puts source at last position, reverse the list
        if(profile.pts.get(0) != profile.source) {
            if(profile.pts.get(profile.pts.size()-1) != profile.source) {
                LOGGER.error("The source have to be first or last cut point");
            }
            if(profile.pts.get(0) != profile.receiver) {
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
        for(CutPoint cut : profile.pts) {
            if(cut.type == IntersectionType.GROUND_EFFECT) {
                if(currentGround == groundEffects.get(cut.id)) {
                    currentGround = null;
                }
                else if(currentGround == null) {
                    currentGround = groundEffects.get(cut.id);
                }
            }
            cut.groundCoef = currentGround != null ? currentGround.coef : 1;
        }

        return profile;
    }

    /**
     * Get the topographic height of a point.
     * @param c Coordinate of the point.
     * @return Topographic height of the point.
     */
    private double getTopoZ(Coordinate c) {
        List list = new ArrayList<>();
        Envelope env = new Envelope(c);
        while(list.isEmpty()) {
            env.expandBy(maxLineLength);
            list = topoTree.query(env);
        }
        Triangle tri = topoTriangles.get((int)list.get(0));
        Coordinate p1 = vertices.get(tri.getA());
        Coordinate p2 = vertices.get(tri.getB());
        Coordinate p3 = vertices.get(tri.getC());
        return Vertex.interpolateZ(c, p1, p2, p3);
    }

    /**
     * Different type of intersection.
     */
    enum IntersectionType {BUILDING, TOPOGRAPHY, GROUND_EFFECT, SOURCE, RECEIVER;

        PointPath.POINT_TYPE toPointType(PointPath.POINT_TYPE dflt) {
            if(this.equals(ProfileBuilder.IntersectionType.SOURCE)){
                return PointPath.POINT_TYPE.SRCE;
            }
            else if(this.equals(ProfileBuilder.IntersectionType.RECEIVER)){
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

        /**
         * Add the source point.
         * @param coord Coordinate of the source point.
         */
        public void addSource(Coordinate coord) {
            source = new CutPoint(coord, IntersectionType.SOURCE, -1);
            pts.add(source);
        }

        /**
         * Add the receiver point.
         * @param coord Coordinate of the receiver point.
         */
        public void addReceiver(Coordinate coord) {
            receiver = new CutPoint(coord, IntersectionType.RECEIVER, -1);
            pts.add(receiver);
        }

        /**
         * Add a building cutting point.
         * @param coord Coordinate of the cutting point.
         * @param id    Id of the cut building.
         */
        public void addBuildingCutPt(Coordinate coord, int id) {
            pts.add(new CutPoint(coord, IntersectionType.BUILDING, id));
            hasBuildingInter = true;
        }

        /**
         * Add a topographic cutting point.
         * @param coord Coordinate of the cutting point.
         * @param id    Id of the cut topography.
         */
        public void addTopoCutPt(Coordinate coord, int id) {
            pts.add(new CutPoint(coord, IntersectionType.TOPOGRAPHY, id));
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
            return pts;
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

        public double getGS(CutPoint p0, CutPoint p1) {
            CutPoint current = p0;
            double totLength = new LineSegment(p0.getCoordinate(), p1.getCoordinate()).getLength();
            double rsLength = 0.0;
            List<CutPoint> pts = getCutPoints().stream().sorted(CutPoint::compareTo).collect(Collectors.toList());
            for(CutPoint cut : pts) {
                if(cut.compareTo(current)>=0 && cut.compareTo(p1)<=0) {
                    LineSegment seg = new LineSegment(current.getCoordinate(), cut.getCoordinate());
                    rsLength += seg.getLength() * current.getGroundCoef();
                    current = cut;
                }
            }
            LineSegment seg = new LineSegment(current.getCoordinate(), p1.getCoordinate());
            rsLength += seg.getLength() * current.getGroundCoef();
            return rsLength / totLength;
        }

        public double getGS() {
            return getGS(getSource(), getReceiver());
        }
    }

    /**
     * Profile cutting point.
     */
    public static class CutPoint implements Comparable<CutPoint> {
        /** {@link Coordinate} of the cut point. */
        private final Coordinate coordinate;
        /** Intersection type. */
        private final IntersectionType type;
        /** Identifier of the cut element. */
        private final int id;
        /** Identifier of the building containing the point. -1 if no building. */
        private int buildingId;
        /** Height of the building containing the point. NaN of no building. */
        private double buildingHeight;
        /** Topographic height of the point. */
        private double topoHeight;
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
            this.groundCoef = 0;
            this.wallAlpha = new ArrayList<>();
            this.buildingHeight = 0;
            this.topoHeight = 0;
        }

        /**
         * Sets the id of the building containing the point.
         * @param buildingId Id of the building containing the point.
         */
        public void setBuildingId(int buildingId) {
            this.buildingId = buildingId;
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
         * @param buildingHeight The building height.
         */
        public void setBuildingHeight(double buildingHeight) {
            this.buildingHeight = buildingHeight;
        }

        /**
         * Sets the topographic height.
         * @param topoHeight The topographic height.
         */
        public void setTopoHeight(double topoHeight) {
            this.topoHeight = topoHeight;
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
        public double getBuildingHeight() {
            return buildingHeight;
        }

        /**
         * Retrieve the topographic height of the point.
         * @return The topographic height of the point.
         */
        public double getTopoHeight() {
            return topoHeight;
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
            str += "topoH : " + topoHeight + " ; ";
            str += "buildH : " + buildingHeight + " ; ";
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
        /** Height of the building. */
        private final double height;
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
         * Retrieve the building height.
         * @return The building height.
         */
        public double getHeight() {
            return height;
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
        private double alpha;
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
            this.alpha = 0;
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
            this.alpha = 0;
        }

        /**
         * Sets the wall alpha.
         * @param alpha Wall alpha.
         */
        public void setAlpha(double alpha) {
            this.alpha = alpha;
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
         * Retrieve the alpha of the wall.
         * @return Alpha of the wall.
         */
        public double getAlpha() {
            return alpha;
        }

        /**
         * Retrieve the height of the wall.
         * @return Height of the wall.
         */
        public double getHeight() {
            return height;
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
                        buildings.get(build - 1).getHeight() + wideAngleTranslationEpsilon);
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
