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
package org.noise_planet.noisemodelling.pathfinder;

import org.locationtech.jts.algorithm.Angle;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.ItemVisitor;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.math.Vector2D;
import org.locationtech.jts.triangulate.quadedge.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * FastObstructionTest speed up the search of
 * visibility test and get the 3D diffraction data. TODO  using income data to do something called
 * visibility culling.
 *
 * @author Nicolas Fortin
 * @author SU Qi
 * @author Pierre Aumond
 */
public class FastObstructionTest {
    public static final double epsilon = 1e-7;
    public static final double wideAngleTranslationEpsilon = 0.01;
    private static final double MINIMAL_REFLECTION_WALL_LENGTH = 1.0;
    private List<Triangle> triVertices;
    private List<Coordinate> vertices;
    private List<Triangle> triNeighbors; // Neighbors
    private List<MeshBuilder.PolygonWithHeight> polygonWithHeight = new ArrayList<MeshBuilder.PolygonWithHeight>();//list polygon with height
    private Envelope meshEnvelope;

    private STRtree triIndex = null;
    private List<Float> verticesOpenAngle = null;
    private List<Coordinate> verticesOpenAngleTranslated = null; /*Open angle*/
    private boolean hasBuildingWithHeight;
    private Logger logger = LoggerFactory.getLogger(FastObstructionTest.class);
    //data for calculate 3D diffraction,
    //first coordinate is the coordinate after the changing coordinate system, the second parameter will keep the data of original coordinate system
    /**
     * New constructor, initialize buildings, triangles and points from mesh data
     *
     * @param buildings    list of buildings with their height
     * @param triangles    list of triangles including buildingID which correspondent the building e.x: triangle.buildingID=1 <=> buildings[0]
     *                     If a triangle is not in building, buildingID for this triangle is 0
     * @param triNeighbors list of neighbors triangle
     * @param points       list of all points in mesh, this points includes vertices of building, Topographic points, vertices of boundingBox
     */
    public FastObstructionTest(List<MeshBuilder.PolygonWithHeight> buildings,
                               List<Triangle> triangles, List<Triangle> triNeighbors, List<Coordinate> points) {

        List<MeshBuilder.PolygonWithHeight> polygonWithHeightArray = new ArrayList<MeshBuilder.PolygonWithHeight>(buildings.size());
        hasBuildingWithHeight = false;
        for(MeshBuilder.PolygonWithHeight poly : buildings) {
            polygonWithHeightArray.add(poly.copy());
            hasBuildingWithHeight = hasBuildingWithHeight || poly.hasHeight();
        }
        this.polygonWithHeight = polygonWithHeightArray;
        this.triVertices = triangles;
        this.triNeighbors = triNeighbors;
        this.vertices = points;
        meshEnvelope = new Envelope();
        for(Coordinate p : points) {
            meshEnvelope.expandToInclude(p);
        }

        // /////////////////////////////////
        // Feed Query Structure to find triangle, by coordinate

        triIndex = new STRtree();
        for (int triId = 0; triId < this.triVertices.size(); triId++) {
            Triangle tri = triVertices.get(triId);
            Envelope env = new Envelope(vertices.get(tri.getA()), vertices.get(tri.getB()));
            env.expandToInclude(vertices.get(tri.getC()));
            triIndex.insert(env, triId);
        }
        //give a average height to each building
        setAverageBuildingHeight(this.polygonWithHeight);
    }

    /**
     * Find all buildings (polygons) that 2D cross the line p1->p2
     * @param p1 first point of line
     * @param p2 second point of line
     * @param visitor Iterate over found buildings
     * @return Building identifier (1-n) intersected by the line
     */
    public void getBuildingsOnPath(Coordinate p1, Coordinate p2, IntersectionRayVisitor visitor) {
        // Navigate through triangle feeding the visitor with intersecting buildings
        LineSegment propaLine = new LineSegment(p1, p2);
        //get receiver triangle id
        int curTriP1 = getTriangleIdByCoordinate(p1);
        HashSet<Integer> navigationHistory = new HashSet<Integer>();
        int navigationTri = curTriP1;
        while (navigationTri != -1) {
            navigationHistory.add(navigationTri);
            Coordinate[] tri = getTriangle(navigationTri);
            if (dotInTri(p2, tri[0], tri[1], tri[2])) {
                return;
            }
            TriIdWithIntersection propaTri = this.getNextTri(navigationTri, propaLine, navigationHistory);
            if(propaTri.isIntersectionOnBuilding()) {
                visitor.visitItem(propaTri.getBuildingId());
                if(!visitor.doContinue()) {
                    return;
                }
            }
            navigationTri = propaTri.getTriID();
        }
    }

    public Envelope getMeshEnvelope() {
        return meshEnvelope;
    }

    /**
     * @return True if buildings height is given
     */
    public boolean isHasBuildingWithHeight() {
        return hasBuildingWithHeight;
    }

    /**
     * Retrieve triangle list, only for debug and unit test purpose
     *
     * @return Triangle list
     */
    public List<Triangle> getTriangles() {
        return Collections.unmodifiableList(triVertices);
    }


    /**
     * @return vertices list, only for debug and unit test purpose
     */
    public List<Coordinate> getVertices() {
        return Collections.unmodifiableList(vertices);
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
    private TriIdWithIntersection getNextTri(final int triIndex,
                                             final LineSegment propagationLine,
                                             HashSet<Integer> navigationHistory) {
        final Triangle tri = this.triVertices.get(triIndex);
        final Triangle triNeighbors = this.triNeighbors.get(triIndex);
        int nearestIntersectionSide = -1;
        int idneigh;

        double nearestIntersectionPtDist = Double.MAX_VALUE;
        // Find intersection pt
        final Coordinate aTri = this.vertices.get(tri.getA());
        final Coordinate bTri = this.vertices.get(tri.getB());
        final Coordinate cTri = this.vertices.get(tri.getC());
        double distline_line;
        Coordinate intersection = new Coordinate();
        //if there is no intersection, by default we set the - max value to Topography intersection to avoid the problem
        double zTopoIntersection = -Double.MAX_VALUE;
        double zPropagationRayIntersection;
        // Intersection First Side
        idneigh = triNeighbors.get(2);
        if (idneigh != -1 && !navigationHistory.contains(idneigh)) {
            Coordinate[] closestPoints = propagationLine.closestPoints(new LineSegment(aTri, bTri));
            Coordinate intersectionTest = null;
            if(closestPoints.length == 2 && closestPoints[0].distance(closestPoints[1]) < epsilon) {
                intersectionTest = closestPoints[0];
            }
            if(intersectionTest != null) {
                distline_line = propagationLine.p1.distance(intersectionTest);
                if (distline_line < nearestIntersectionPtDist) {
                    intersection = intersectionTest;
                    nearestIntersectionPtDist = distline_line;
                    nearestIntersectionSide = 2;
                    //we will get the intersection point coordinate with(x,y,NaN)
                    //get this point Z using interested segment.
                    zTopoIntersection = calculateLinearInterpolation(aTri, bTri, intersection);
                }
            }
        }
        // Intersection Second Side
        idneigh = triNeighbors.get(0);
        if (idneigh != -1 && !navigationHistory.contains(idneigh)) {
            Coordinate[] closestPoints = propagationLine.closestPoints(new LineSegment(bTri, cTri));
            Coordinate intersectionTest = null;
            if(closestPoints.length == 2 && closestPoints[0].distance(closestPoints[1]) < epsilon) {
                intersectionTest = closestPoints[0];
            }
            if(intersectionTest != null) {
                distline_line = propagationLine.p1.distance(intersectionTest);
                if (distline_line < nearestIntersectionPtDist) {
                    intersection = intersectionTest;
                    nearestIntersectionPtDist = distline_line;
                    nearestIntersectionSide = 0;
                    //we will get the intersection point coordinate with(x,y,NaN)
                    //get this point Z using interested segment.
                    zTopoIntersection = calculateLinearInterpolation(bTri, cTri, intersection);
                }
            }
        }
        // Intersection Third Side
        idneigh = triNeighbors.get(1);
        if (idneigh != -1 && !navigationHistory.contains(idneigh)) {
            Coordinate[] closestPoints = propagationLine.closestPoints(new LineSegment(cTri, aTri));
            Coordinate intersectionTest = null;
            if(closestPoints.length == 2 && closestPoints[0].distance(closestPoints[1]) < epsilon) {
                intersectionTest = closestPoints[0];
            }
            if(intersectionTest != null) {
                distline_line = propagationLine.p1.distance(intersectionTest);
                if (distline_line < nearestIntersectionPtDist) {
                    intersection = intersectionTest;
                    nearestIntersectionSide = 1;
                    //we will get the intersection point coordinate with(x,y,NaN)
                    //get this point Z using interested segment.
                    zTopoIntersection = calculateLinearInterpolation(cTri, aTri, intersection);
                }
            }
        }
        if (nearestIntersectionSide != -1) {
            //get this point Z using propagation line
            zPropagationRayIntersection = calculateLinearInterpolation(propagationLine.p0, propagationLine.p1, intersection);
            // Manage blocking buildings
            int neightBuildingId = this.triVertices.get(triNeighbors.get(nearestIntersectionSide)).getAttribute();
            int rayBuildingId = 0;
            // Current tri is in building
            if(tri.getAttribute() != 0) {
                rayBuildingId = tri.getAttribute();
                MeshBuilder.PolygonWithHeight building = polygonWithHeight.get(tri.getAttribute() - 1);
                // Stop propagation if ray collide with the building
                if(!building.hasHeight() || Double.isNaN(zPropagationRayIntersection) || zPropagationRayIntersection < building.getHeight()) {
                    return new TriIdWithIntersection(triNeighbors.get(nearestIntersectionSide),
                            new Coordinate(intersection.x, intersection.y, zPropagationRayIntersection),
                            true,false, rayBuildingId);
                }
            }
            // Next tri is in building
            if(neightBuildingId != 0) {
                rayBuildingId = neightBuildingId;
                MeshBuilder.PolygonWithHeight building = polygonWithHeight.get(neightBuildingId - 1);
                // Stop propagation if ray collide with the building
                if(!building.hasHeight() || Double.isNaN(zPropagationRayIntersection) || zPropagationRayIntersection < building.getHeight()) {
                    return new TriIdWithIntersection(triNeighbors.get(nearestIntersectionSide),
                            new Coordinate(intersection.x, intersection.y, zPropagationRayIntersection),
                            true,false, rayBuildingId);
                }
            }
            //If the Z calculated by propagation Line >= Z calculated by intersected line, we will find next triangle
            if (Double.isNaN(zPropagationRayIntersection) || zPropagationRayIntersection + epsilon >= zTopoIntersection) {
                return new TriIdWithIntersection(triNeighbors.get(nearestIntersectionSide),
                        new Coordinate(intersection.x, intersection.y, zPropagationRayIntersection),
                        false, false, rayBuildingId);
            }
            //Else, the Z of Topographic intersection > Z calculated by propagation Line, the Topographic intersection will block the propagation line
            else {
                //Propagation line blocked by the topography
                return new TriIdWithIntersection(triNeighbors.get(nearestIntersectionSide),
                        new Coordinate(intersection.x, intersection.y, zTopoIntersection),
                        false,true, rayBuildingId);
            }
        } else {
            return new TriIdWithIntersection(-1,
                    new Coordinate(),
                    false,false, 0);
        }
    }

    private boolean dotInTri(Coordinate p, Coordinate a, Coordinate b,
                             Coordinate c) {
        return dotInTri(p, a, b, c, null);
    }
    /**
     * Fast dot in triangle test
     * <p/>
     * {@see http://www.blackpawn.com/texts/pointinpoly/default.html}
     *
     * @param p coordinate of the point
     * @param a coordinate of the A vertex of triangle
     * @param b coordinate of the B vertex of triangle
     * @param c coordinate of the C vertex of triangle
     * @return True if dot is in triangle
     */
    private boolean dotInTri(Coordinate p, Coordinate a, Coordinate b,
                             Coordinate c, AtomicReference<Double> error) {
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

        if(error != null) {
            double err = 0;
            err += Math.max(0, -u);
            err += Math.max(0, -v);
            err += Math.max(0, (u + v) - 1);
            error.set(err);
        }

        // Check if point is in triangle
        return (u > (0. - epsilon)) && (v > (0. - epsilon))
                && (u + v < (1. + epsilon));

    }

    Coordinate[] getTriangle(int triIndex) {
        final Triangle tri = this.triVertices.get(triIndex);
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
        List res = triIndex.query(new Envelope(ptEnv));
        double minDistance = Double.MAX_VALUE;
        int minDistanceTriangle = -1;
        for(Object objInd : res) {
            int triId = (Integer) objInd;
            Coordinate[] tri = getTriangle(triId);
            AtomicReference<Double> err = new AtomicReference<>(0.);
            dotInTri(pt, tri[0], tri[1], tri[2], err);
            if (err.get() < minDistance) {
                minDistance = err.get();
                minDistanceTriangle = triId;
            }
        }
        return minDistanceTriangle;
    }

    /**
     * Add open angle to verticesAngle array (merge with existing open angle if
     * exists)
     *
     * @param vertexId         Index of vertex in the verticesAngle array
     * @param vertexCoordinate coordinate of the vertex
     * @param left             CCW Neighbor 1 of vertex (open angle)
     * @param right            CCW Neighbor 2 of vertex (open angle)
     * @param verticesAngle    Array of Array of open angle
     */
    public static void updateMinMax(int vertexId, Coordinate vertexCoordinate,
                                    Coordinate left, Coordinate right,
                                    ArrayList<ArrayList<Double>> verticesAngle) {
        List<Double> curVertex = verticesAngle.get(vertexId);
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
                        // Remove merged element and loop again
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
                        // Remove merged element and loop again
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
     * @param minAngle Minimum angle [0-2Pi]
     * @param maxAngle Maximum angle [0-2Pi]
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
                if (tri.getAttribute() < 1) {
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
                                    .angleBetween(triB, triA, triC))
                    );
                    // Add B vertex angle
                    updateMinMax(tri.getB(), triB, triC, triA,
                            verticesOpenAnglesTuples);
                    verticesOpenAngle.set(tri.getB(),
                            (float) (verticesOpenAngle.get(tri.getB()) + Angle
                                    .angleBetween(triA, triB, triC))
                    );
                    // Add C vertex angle
                    updateMinMax(tri.getC(), triC, triA, triB,
                            verticesOpenAnglesTuples);
                    verticesOpenAngle.set(tri.getC(),
                            (float) (verticesOpenAngle.get(tri.getC()) + Angle
                                    .angleBetween(triB, triC, triA))
                    );
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
     * Return wall corners with an offset offward the building volume
     * @param build Building identifier [1-buildingCount]
     * @param minAngle Minimum angle [0-2Pi]
     * @param maxAngle Maximum angle [0-2Pi]
     * @return List of corners within parameters range
     */
    public List<Coordinate> getWideAnglePointsByBuilding(int build, double minAngle, double maxAngle) {
        List <Coordinate> verticesBuilding = new ArrayList<>();
        Coordinate[] ring = getBuilding(build).getExteriorRing().getCoordinates();
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
                        getBuildingRoofZ(build) + wideAngleTranslationEpsilon);
                verticesBuilding.add(offsetPt);
            }
        }
        verticesBuilding.add(verticesBuilding.get(0));
        return verticesBuilding;
    }

    /**
     * Compute the list of segments corresponding to holes and domain limitation
     *
     * @param maxDist Maximum distance from origin to segments
     * @param p1      Origin of search
     * @return List of segment
     */
    public LinkedList<Wall> getLimitsInRange(double maxDist,
                                             Coordinate p1, boolean goThroughWalls) {
        LinkedList<Wall> walls = new LinkedList<>();
        int curTri = getTriangleIdByCoordinate(p1);
        int p1Building = 0;
        p1Building = triVertices.get(curTri).getAttribute();
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
            if(firstSide == 0) {
                navigationHistory.add(curTri);
            }
            // for each side of the triangle
            Triangle neighbors = this.triNeighbors.get(curTri);
            nextTri = -1;
            for (short sideId = firstSide; sideId < 3; sideId++) {
                Triangle tri = this.triVertices.get(curTri);
                IntSegment segVerticesIndex = tri
                        .getSegment(sideId);
                int wallBuildingId = 0;
                if(neighbors.get(sideId) != -1) {
                    wallBuildingId = triVertices.get(neighbors.get(sideId)).getAttribute();
                }
                Wall wall = new Wall(
                        this.vertices.get(segVerticesIndex.getA()),
                        this.vertices.get(segVerticesIndex.getB()),wallBuildingId);
                Coordinate closestPoint = wall.closestPoint(p1);
                if (closestPoint.distance(p1) <= maxDist) {
                    // Propagate search in this direction if this is not the domain limitation
                    if (neighbors.get(sideId) != -1) {
                        // If the triangle side is a wal
                        if (wall.getBuildingId() >= 1 && wall.getLength() > MINIMAL_REFLECTION_WALL_LENGTH) {
                            walls.add(wall);
                        }
                        if((goThroughWalls || wall.getBuildingId() == 0 || wall.getBuildingId() == p1Building) && !navigationHistory.contains(neighbors.get(sideId))) {
                            // Store currentTriangle Id. This is where to go
                            // back when there is no more navigable neighbors at
                            // the next triangle
                            navigationNodes.add(curTri);
                            navigationSide.add(sideId);
                            firstSide = 0;
                            nextTri = neighbors.get(sideId);
                            break; // Next triangle
                        }
                    }
                }
            }
            if (nextTri == -1 && !navigationNodes.empty()) {
                // All the side have been rejected, go back by one on the
                // navigation
                nextTri = navigationNodes.pop();
                firstSide = (short) (navigationSide.pop() + 1);
            }
            curTri = nextTri;
        }
        return walls;
    }

    /**
     * Get ray curve length from homogeneous CNOSSOS p .94
     * @param MN distance between M and N
     * @param Gamma curve radius
     * @return MNFav, Distance ray curve
     */
    public double getRayCurveLength(double MN, double Gamma) {

        return 2*Gamma*Math.asin(MN/(2*Gamma));

    }

    /**
     * Interpolate Z value in the triangle that contain p1
     * @param p1 Extraction point
     * @return Interpolated Z value, NaN if out of bounds
     */
    public double getHeightAtPosition(Coordinate p1) {
        int curTri = getTriangleIdByCoordinate(p1);
        if(curTri >= 0) {
            Triangle triangle = triVertices.get(curTri);
            org.locationtech.jts.geom.Triangle tri =
                    new org.locationtech.jts.geom.Triangle(vertices.get(triangle.getA()),
                            vertices.get(triangle.getB()),vertices.get(triangle.getC()));

            return tri.interpolateZ(p1);
        } else {
            return Double.NaN;
        }
    }





    /**
     * @param buildingId Building identifier [1-n]
     * @return Position of building roof
     */
    public double getBuildingRoofZ(int buildingId) {
        return polygonWithHeight.get(buildingId - 1).getHeight();
    }

    public List<Double> getBuildingAlpha(int buildingId) {
        return polygonWithHeight.get(buildingId - 1).getAlpha();
    }

    public Polygon getBuilding(int buildingId) {
        return (Polygon) polygonWithHeight.get(buildingId - 1).geo;
    }

    public List<MeshBuilder.PolygonWithHeight> getPolygonWithHeight() {
        return Collections.unmodifiableList(polygonWithHeight);
    }

    /**
     * @return Number of stored buildings
     */
    public int getBuildingCount() {
        return polygonWithHeight.size();
    }


    /*
     * compute diffraction.
     */
    public boolean isFreeField(Coordinate p1, Coordinate p2) {
        return computePropagationPath(p1, p2, true, null, false);
    }

    /**
     *
     * @param p1 Start propagation path
     * @param p2 End propagation path
     * @param stopOnIntersection Stop if the segment between p1 and p2 intersects with topography or buildings
     * @param path [out] Intersection list with triangle sides.
     * @param includePoints Include p1 and p2 into path output
     * @return True if the propagation goes from p1 to p2.
     */
    public boolean computePropagationPath(Coordinate p1, Coordinate p2, boolean stopOnIntersection, List<TriIdWithIntersection> path, boolean includePoints) {
        LineSegment propaLine = new LineSegment(p1, p2);

        //get receiver triangle id
        int curTriP1 = getTriangleIdByCoordinate(p1);
        //get source triangle id
        int curTriP2 = getTriangleIdByCoordinate(p2);
        if(curTriP1 == -1) {
            logger.error(String.format("Propagation path point (%.2f, %.2f) is outside of bounds", p1.x, p1.y));
            return false;
        }
        if(curTriP2 == -1) {
            logger.error(String.format("Propagation path point (%.2f, %.2f) is outside of bounds", p2.x, p2.y));
            return false;
        }
        Coordinate[] triP1 = getTriangle(curTriP1);
        Coordinate[] triP2 = getTriangle(curTriP2);
        Triangle buildingP1 = this.triVertices.get(curTriP1);
        Triangle buildingP2 = this.triVertices.get(curTriP2);
        if(stopOnIntersection) {
            if (buildingP1.getAttribute() >= 1) {
                MeshBuilder.PolygonWithHeight building = polygonWithHeight.get(buildingP1.getAttribute() - 1);
                if (!building.hasHeight() || Double.isNaN(p1.z) || building.getHeight() >= p1.z) {
                    //receiver is in the building so this propagation line is invisible
                    return false;
                }
            }
            if (buildingP2.getAttribute() >= 1) {
                MeshBuilder.PolygonWithHeight building = polygonWithHeight.get(buildingP2.getAttribute() - 1);
                if (!building.hasHeight() || Double.isNaN(p2.z) || building.getHeight() >= p2.z) {
                    //receiver is in the building so this propagation line is invisible
                    return false;
                }
            }
        }
        double zTopoP1 = getTopoZByGiven3Points(triP1[0], triP1[1], triP1[2], p1);
        double zTopoP2 = getTopoZByGiven3Points(triP2[0], triP2[1], triP2[2], p2);
        if(includePoints) {
            path.add(new TriIdWithIntersection(curTriP1, new Coordinate(p1.x, p1.y, zTopoP1)));
        }
        if (stopOnIntersection && ((!Double.isNaN(p1.z) && p1.z + epsilon < zTopoP1)
                || (!Double.isNaN(p2.z) && p2.z + epsilon < zTopoP2))) {
            //Z value of origin or destination is lower than topography. FreeField is always false in this case
            if(includePoints) {
                path.add(new TriIdWithIntersection(curTriP2, new Coordinate(p2.x, p2.y, zTopoP2), false, false,
                        buildingP2.getAttribute()));
            }
            return false;
        }

        HashSet<Integer> navigationHistory = new HashSet<Integer>();
        int navigationTri = curTriP1;
        while (navigationTri != -1) {
            navigationHistory.add(navigationTri);
            Coordinate[] tri = getTriangle(navigationTri);
            if (dotInTri(p2, tri[0], tri[1], tri[2])) {
                if(includePoints) {
                    path.add(new TriIdWithIntersection(curTriP2, new Coordinate(p2.x, p2.y, zTopoP2), false, false,
                            buildingP2.getAttribute()));
                }
                return true;
            }
            TriIdWithIntersection propaTri = this.getNextTri(navigationTri, propaLine, navigationHistory);
            if (path != null && propaTri.getTriID() >= 0) {
                path.add(propaTri);
            }
            if (!stopOnIntersection || !propaTri.isIntersectionOnBuilding() && !propaTri.isIntersectionOnTopography()) {
                navigationTri = propaTri.getTriID();
            } else {
                navigationTri = -1;
            }
        }
        // Can't find a way to p2
        return false;
    }

    private TriIdWithIntersection updateZ(TriIdWithIntersection pt) {
        if(pt.getBuildingId() > 0) {
            return new TriIdWithIntersection(pt.getTriID(),
                    new Coordinate(pt.getCoorIntersection().x,pt.getCoorIntersection().y,
                            polygonWithHeight.get(pt.getBuildingId() - 1).getHeight()), pt.isIntersectionOnBuilding(),
                    pt.isIntersectionOnTopography(), pt.getBuildingId());
        } else {
            return pt;
        }
    }

    /**
     * @param allInterPoints Path between two points
     * {@link #computePropagationPath(org.locationtech.jts.geom.Coordinate, org.locationtech.jts.geom.Coordinate,
     * boolean, java.util.List, boolean)}
     * @return Ground position of provided line path.
     */
    public List<Coordinate> getGroundProfile(List<TriIdWithIntersection> allInterPoints) {
        List<Coordinate> groundProfile = new ArrayList<>(allInterPoints.size());
        for(TriIdWithIntersection tri : allInterPoints) {
            Triangle triangle = triVertices.get(tri.getTriID());
            double zTri = getTopoZByGiven3Points(vertices.get(triangle.getA()), vertices.get(triangle.getB()),
                    vertices.get(triangle.getC()), tri.getCoorIntersection());
            groundProfile.add(new Coordinate(tri.getCoorIntersection().x, tri.getCoorIntersection().y, zTri));
        }
        return groundProfile;
    }

    /**
     * Get the distance of all intersections (after the filtration by algorithm Jarvis March)  between the source and the receiver to compute vertical diffraction
     * Must called after finishPolygonFeeding
     *
     * @param receiver coordinate receiver
     * @param source coordinate source
     * @return DiffractionWithSoilEffectZone
     * Double list=DiffractionWithSoilEffectZone.diffractionData : data prepared to compute diffraction
     * Double[DELTA_DISTANCE]:delta distance;
     * Double[E_LENGTH]:e;
     * Double[FULL_DIFFRACTION_DISTANCE]:the full distance of diffraction path
     * if Double[DELTA_DISTANCE],Double[E_LENGTH],Double[FULL_DIFFRACTION_DISTANCE],Double[Full_Distance_With_Soil_Effect] are -1. then no useful intersections.
     */
    @SuppressWarnings("unchecked")
    public DiffractionWithSoilEffetZone getPath(Coordinate receiver, Coordinate source, List<TriIdWithIntersection> allInterPoints) {
        //set default data
        DiffractionWithSoilEffetZone totData = new DiffractionWithSoilEffetZone(null, null, -1,-1, -1, -1,-1,
                new ArrayList<Coordinate>(), new ArrayList<Coordinate>(),new ArrayList<Coordinate>(),0);

        /*
        data for calculate 3D diffraction,
        first coordinate is the coordinate after the modification coordinate system,
        the second parameter will keep the data of original coordinate system
        */

        LineSegment rOZone = new LineSegment(new Coordinate(-1, -1), new Coordinate(-1, -1));
        LineSegment sOZone = new LineSegment(new Coordinate(-1, -1), new Coordinate(-1, -1));

        if(allInterPoints == null) {
            allInterPoints = new ArrayList<>();
            computePropagationPath(receiver, source, false, allInterPoints, true);
        }
        if(allInterPoints.isEmpty()) {
            return totData;
        }
        List<TriIdWithIntersection> interPoints = new ArrayList<>();
        // Keep only intersection between land and buildings.
        TriIdWithIntersection lastInter = null;
        for(TriIdWithIntersection inter : allInterPoints.subList(1,allInterPoints.size() - 1)) {
            if(inter.getBuildingId() > 0 && (lastInter == null || lastInter.getBuildingId() == 0)) {
                interPoints.add(updateZ(inter));
            } else if(lastInter != null && inter.getBuildingId() == 0 && lastInter.getBuildingId() > 0) {
                interPoints.add(updateZ(lastInter));
            } else if(inter.isIntersectionOnTopography()) {
                interPoints.add(inter);
            }
            lastInter = inter;
        }
        if(lastInter != null && lastInter.getBuildingId() > 0) {
            interPoints.add(updateZ(lastInter));
        }
        if(!hasBuildingWithHeight) {
            // Cannot compute envelope is building height is not available
            return totData;
        }

        //add point receiver and point source into list head and tail.
        // Change from ground height for receiver and source to real receiver and source height
        if(receiver.distance(allInterPoints.get(0)) > receiver.distance(allInterPoints.get(allInterPoints.size() - 1))) {
            // the first inter point should be the receiver not the source
            // reverse the order of the lists
            Collections.reverse(allInterPoints);
            Collections.reverse(interPoints);
        }
        interPoints.add(0, new TriIdWithIntersection(allInterPoints.get(0), receiver));
        interPoints.add(new TriIdWithIntersection(allInterPoints.get(allInterPoints.size() - 1), source));
        double angle = new LineSegment(source, receiver).angle();
        //change coordinate system from 3D to 2D
        List<Coordinate> newPoints = JTSUtility.getNewCoordinateSystem(new ArrayList<Coordinate>(interPoints));

        double[] pointsX;
        pointsX = new double[newPoints.size()];
        double[] pointsY;
        pointsY = new double[newPoints.size()];

        for (int i = 0; i < newPoints.size(); i++) {
            pointsX[i] = newPoints.get(i).x;
            if (!Double.isNaN(newPoints.get(i).y)) {
                pointsY[i] = newPoints.get(i).y;
            } else {
                pointsY[i] = 0.;
            }
            newPoints.get(i).setCoordinate(new Coordinate(pointsX[i], pointsY[i]));
        }
        //algorithm to get the upper side of the convex hull
        List<Coordinate> upperHull = JTSUtility.getXAscendingHullPoints(newPoints.toArray(new Coordinate[newPoints.size()]));
        //JarvisMarch jm = new JarvisMarch(new JarvisMarch.Points(pointsX, pointsY));
        //JarvisMarch.Points points = jm.calculateHull();
        //List<Integer> pointsId = jm.getHullPointId();

        //if there are no useful intersection
        if(upperHull.size() > 2) {
            //after jarvis march we must have the length of list of points superior than 2, so we have useful points
            Coordinate osCorner = interPoints.get(interPoints.size() - 2).getCoorIntersection();
            LinkedList<LineSegment> path = new LinkedList<>();
            for (int i = 0; i < upperHull.size() - 1; i++) {
                path.add(new LineSegment(upperHull.get(i), upperHull.get(i+1)));
            }
            List<Coordinate> path3D = new ArrayList<>();
            Coordinate coordinate = new Coordinate();
            int i =0;
            for (i =0; i<path.size();i++) {
                coordinate = JTSUtility.getOldCoordinateSystem(path.get(i).p0, angle);
                coordinate.x = receiver.x-coordinate.x;
                coordinate.y = receiver.y-coordinate.y;
                path3D.add(coordinate);
            }
            i--;
            coordinate = JTSUtility.getOldCoordinateSystem(path.get(i).p1, angle);
            coordinate.x = receiver.x-coordinate.x;
            coordinate.y = receiver.y-coordinate.y;
            path3D.add(coordinate);

            //we used coordinate after change coordinate system to get the right distance.
            double distanceRandS = path.getFirst().p0.distance(path.getLast().p1);              //distance of receiver and source

            // Get fullpathdistance in favourable condition p.94
            double Gamma = Math.max(1000,8*distanceRandS);
            double pathDistancefav=0.0;

            double pathDistance = 0.0;//distance of path
            //prepare data to compute pure diffraction
            //h0 in expression diffraction:the highest point intersection
            double pointHeight = 0.0;
            for(LineSegment aPath : path) {
                pathDistance = aPath.getLength() + pathDistance;
                pathDistancefav=pathDistancefav + getRayCurveLength(aPath.getLength(), Gamma);
                if (aPath.p0.y > pointHeight) {
                    pointHeight = aPath.p0.y;
                }
            }
            if (Double.isInfinite(pathDistance)) {
                return totData;
            }
            double e = pathDistance - path.getFirst().getLength() - path.getLast().getLength();//distance without first part path and last part path
            double deltaDistance = pathDistance - distanceRandS;                                //delta distance
            double deltaDistancefav = pathDistancefav - getRayCurveLength(distanceRandS, Gamma);                                //delta distance favourable

            //todo cas FIGURE VI.10 p. 93 Third case

            //if we have soil data
            Coordinate[] firstPart = new Coordinate[2];
            Coordinate[] lastPart = new Coordinate[2];
            firstPart[0] = receiver;
            //get original coordinate for first intersection with building
            firstPart[1] = interPoints.get(1).getCoorIntersection();

            //get original coordinate for last intersection with building
            lastPart[0] = osCorner;
            lastPart[1] = source;
            //receiver-first intersection zone aims to calculate ground effect
            rOZone = new LineSegment(firstPart[0], firstPart[1]);
            //last intersection-source zone aims to calculate ground effect (between rOZone and sOZone we ignore ground effect)
            sOZone = new LineSegment(lastPart[0], lastPart[1]);


            // Compute ground projected path in order to compute mean ground formulae later
            // receiver part
            List<Coordinate> roGround = new ArrayList<>();
            // Compute ground profile from R to first O (first building corner)
            int rOIndex = 0;
            for(TriIdWithIntersection tri : allInterPoints) {
                rOIndex++;
                if (tri.isIntersectionOnBuilding() || tri.isIntersectionOnTopography()) {
                    break;
                }
            }
            roGround.addAll(getGroundProfile(allInterPoints.subList(0, rOIndex)));
            // Source part
            // Compute ground profile from last O to S (last building corner)
            int sOIndex = allInterPoints.size() - 1;
            for(int index = allInterPoints.size() - 1; index >= 0; index--) {
                TriIdWithIntersection tri = allInterPoints.get(index);
                sOIndex = index;
                if (tri.isIntersectionOnBuilding() || tri.isIntersectionOnTopography()) {
                    break;
                }
            }
            List<Coordinate> oSGround = getGroundProfile(allInterPoints.subList(sOIndex, allInterPoints.size()));
            // As the insertion was done reversed this is actually sOGround, reverse again in order to fit with variable name
            totData = new DiffractionWithSoilEffetZone(rOZone, sOZone, deltaDistance,deltaDistancefav, e, pathDistance,pathDistancefav,
                    roGround, oSGround,path3D,pointHeight);


        }
        return totData;
    }

    /**
     * Get the distance of all intersections between the source and the receiver to compute vertical diffraction when source and receiver see them
     * Must called after finishPolygonFeeding
     *
     * @param receiver coordinate receiver
     * @param source coordinate source
     * @return DiffractionWithSoilEffectZone
     * Double list=DiffractionWithSoilEffectZone.diffractionData : data prepared to compute diffraction
     * Double[DELTA_DISTANCE]:delta distance;
     * Double[E_LENGTH]:e;
     * Double[FULL_DIFFRACTION_DISTANCE]:the full distance of diffraction path
     * if Double[DELTA_DISTANCE],Double[E_LENGTH],Double[FULL_DIFFRACTION_DISTANCE],Double[Full_Distance_With_Soil_Effect] are -1. then no useful intersections.
     */
    @SuppressWarnings("unchecked")
    public DiffractionWithSoilEffetZone getPathInverse(Coordinate receiver, Coordinate source) {
        //set default data
        DiffractionWithSoilEffetZone totData = new DiffractionWithSoilEffetZone(null, null, -1,-1, -1, -1,-1,
                new ArrayList<Coordinate>(), new ArrayList<Coordinate>(), new ArrayList<Coordinate>(),0);
        /*
        data for calculate 3D diffraction,
        first coordinate is the coordinate after the modification coordinate system,
        the second parameter will keep the data of original coordinate system
        */

        LineSegment rOZone = new LineSegment(new Coordinate(-1, -1), new Coordinate(-1, -1));
        LineSegment sOZone = new LineSegment(new Coordinate(-1, -1), new Coordinate(-1, -1));

        List<TriIdWithIntersection> allInterPoints = new ArrayList<>();
        computePropagationPath(receiver, source, false, allInterPoints, true);
        if(allInterPoints.isEmpty()) {
            return totData;
        }
        List<TriIdWithIntersection> interPoints = new ArrayList<>();
        // Keep only intersection between land and buildings.
        TriIdWithIntersection lastInter = null;
        for(TriIdWithIntersection inter : allInterPoints.subList(1,allInterPoints.size() - 1)) {
            if(inter.getBuildingId() > 0 && (lastInter == null || lastInter.getBuildingId() == 0)) {
                interPoints.add(updateZ(inter));
            } else if(lastInter != null && inter.getBuildingId() == 0 && lastInter.getBuildingId() > 0) {
                interPoints.add(updateZ(lastInter));
            }
            lastInter = inter;
        }
        if(lastInter != null && lastInter.getBuildingId() > 0) {
            interPoints.add(updateZ(lastInter));
        }
        if(!hasBuildingWithHeight) {
            // Cannot compute envelope is building height is not available
            return totData;
        }

        //add point receiver and point source into list head and tail.
        // Change from ground height for receiver and source to real receiver and source height
        interPoints.add(0, new TriIdWithIntersection(allInterPoints.get(0), receiver));
        interPoints.add(new TriIdWithIntersection(allInterPoints.get(allInterPoints.size() - 1), source));
        //change coordinate system from 3D to 2D
        List<Coordinate> newPoints = JTSUtility.getNewCoordinateSystem(new ArrayList<Coordinate>(interPoints));

        double[] pointsX;
        pointsX = new double[newPoints.size()];
        double[] pointsY;
        pointsY = new double[newPoints.size()];

        for (int i = 0; i < newPoints.size(); i++) {
            pointsX[i] = newPoints.get(i).x;
            if (!Double.isNaN(newPoints.get(i).y)) {
                pointsY[i] = newPoints.get(i).y;
            } else {
                pointsY[i] = 0.;
            }
            newPoints.get(i).setCoordinate(new Coordinate(pointsX[i], pointsY[i]));
        }
        //algorithm JarvisMarch to get the convex hull
        JarvisMarch jm = new JarvisMarch(new JarvisMarch.Points(pointsX, pointsY));
        JarvisMarch.Points points = jm.calculateSmallestTriangle();
        JarvisMarch.Points pointsA = jm.calculateSmallestTriangle();

        double a = (points.y[2]-points.y[0])/(points.x[2]-points.x[0]);
        pointsA.y[1]=pointsA.x[1]*a+(points.y[0]-a*points.x[0]);

        List<Integer> pointsId = jm.getHullPointId();

        //if there are no useful intersection
        if (points.x.length <= 2) {
            //after jarvis march if we get the length of list of points less than 2, so we have no useful points
            return totData;
        } else {
            Coordinate osCorner = interPoints.get(interPoints.size() - 2).getCoorIntersection();
            LinkedList<LineSegment> path = new LinkedList<>();
            LinkedList<LineSegment> pathA = new LinkedList<>();
            if (points.x[2] < points.x[1])
            {
                path.add(new LineSegment(new Coordinate(points.x[0], points.y[0]), new Coordinate(points.x[points.x.length-1], points.y[points.x.length-1])));
                path.add(new LineSegment(new Coordinate(pointsA.x[0], pointsA.y[0]), new Coordinate(pointsA.x[points.x.length-1], pointsA.y[points.x.length-1])));
                // FreeField test
                TriIdWithIntersection interBegin = interPoints.get(pointsId.get(0));
                osCorner = interBegin;
                TriIdWithIntersection interEnd = interPoints.get(pointsId.get(points.x.length-1));
                if (interBegin.getBuildingId() != interEnd.getBuildingId()) {
                    Coordinate testPBegin = new Coordinate(interBegin.getCoorIntersection());
                    Coordinate testPEnd = new Coordinate(interEnd.getCoorIntersection());
                    testPBegin.setOrdinate(Coordinate.Z, points.y[0] + epsilon);
                    testPEnd.setOrdinate(Coordinate.Z, points.y[points.x.length-1] + epsilon);
                    if (!isFreeField(testPBegin, testPEnd)) {
                        return totData;
                    }
                }
                for (int i = points.x.length-2; i > 0 ; i--) {
                    if (!(points.x[i+1] > points.x[i])) {
                        path.add(new LineSegment(new Coordinate(points.x[i+1], points.y[i+1]), new Coordinate(points.x[i], points.y[i])));
                        pathA.add(new LineSegment(new Coordinate(pointsA.x[i+1], pointsA.y[i+1]), new Coordinate(pointsA.x[i], pointsA.y[i])));
                        // FreeField test
                        interBegin = interPoints.get(pointsId.get(i+1));
                        osCorner = interBegin;
                        interEnd = interPoints.get(pointsId.get(i ));
                        if (interBegin.getBuildingId() != interEnd.getBuildingId()) {
                            Coordinate testPBegin = new Coordinate(interBegin.getCoorIntersection());
                            Coordinate testPEnd = new Coordinate(interEnd.getCoorIntersection());
                            testPBegin.setOrdinate(Coordinate.Z, points.y[i+1] + epsilon);
                            testPEnd.setOrdinate(Coordinate.Z, points.y[i ] + epsilon);
                            if (!isFreeField(testPBegin, testPEnd)) {
                                return totData;
                            }
                        }
                    } else {
                        break;
                    }
                }

            }else {
                for (int i = 0; i < points.x.length - 1; i++) {
                    if (!(points.x[i] > points.x[i + 1])) {
                        path.add(new LineSegment(new Coordinate(points.x[i], points.y[i]), new Coordinate(points.x[i + 1], points.y[i + 1])));
                        pathA.add(new LineSegment(new Coordinate(pointsA.x[i], pointsA.y[i]), new Coordinate(pointsA.x[i + 1], pointsA.y[i + 1])));

                        // FreeField test
                        TriIdWithIntersection interBegin = interPoints.get(pointsId.get(i));
                        osCorner = interBegin;
                        TriIdWithIntersection interEnd = interPoints.get(pointsId.get(i + 1));
                        if (interBegin.getBuildingId() != interEnd.getBuildingId()) {
                            Coordinate testPBegin = new Coordinate(interBegin.getCoorIntersection());
                            Coordinate testPEnd = new Coordinate(interEnd.getCoorIntersection());
                            testPBegin.setOrdinate(Coordinate.Z, points.y[i] + epsilon);
                            testPEnd.setOrdinate(Coordinate.Z, points.y[i + 1] + epsilon);
                            if (!isFreeField(testPBegin, testPEnd)) {
                                return totData;
                            }
                        }
                    } else {
                        break;
                    }
                }
            }


            //we used coordinate after change coordinate system to get the right distance.
            double distanceRandS = path.getFirst().p0.distance(path.getLast().p1);              //distance of receiver and source

            // Get fullpathdistance in favourable condition p.94
            double Gamma = Math.max(1000,8*distanceRandS);
            double pathDistancefav=0.0;
            double pathDistance = 0.0;//distance of path
            //prepare data to compute pure diffraction
            //h0 in expression diffraction:the highest point intersection
            double pointHeight = 0.0;
            for(LineSegment aPath : path) {
                pathDistance = aPath.getLength() + pathDistance;
                pathDistancefav=pathDistancefav + getRayCurveLength(aPath.getLength(), Gamma);
                if (aPath.p0.y > pointHeight) {
                    pointHeight = aPath.p0.y;
                }
            }

            // Path distance using poitn A p94 CNOSSOS VI-27
            double pathDistanceA = 0.0;//distance of path
            for(LineSegment aPath : pathA) {
                pathDistanceA = pathDistanceA + getRayCurveLength(aPath.getLength(), Gamma);
            }

            if (Double.isInfinite(pathDistance)) {
                return totData;
            }
            double e = pathDistance - path.getFirst().getLength() - path.getLast().getLength();//distance without first part path and last part path
            double deltaDistance = -pathDistance + distanceRandS;                                //delta distance
            double deltaDistancefav = 2*pathDistanceA - pathDistancefav - getRayCurveLength(distanceRandS, Gamma);                                //delta distance favourable

            //todo cas FIGURE VI.10 p. 93 Third case

            //if we have soil data
            Coordinate[] firstPart = new Coordinate[2];
            Coordinate[] lastPart = new Coordinate[2];
            firstPart[0] = receiver;
            //get original coordinate for first intersection with building
            firstPart[1] = interPoints.get(pointsId.get(1)).getCoorIntersection();

            //get original coordinate for last intersection with building
            lastPart[0] = interPoints.get(pointsId.get(1)).getCoorIntersection();
            lastPart[1] = source;
            //receiver-first intersection zone aims to calculate ground effect
            rOZone = new LineSegment(firstPart[0], firstPart[1]);
            //last intersection-source zone aims to calculate ground effect (between rOZone and sOZone we ignore ground effect)
            sOZone = new LineSegment(lastPart[0], lastPart[1]);


            // Compute ground projected path in order to compute mean ground formulae later
            // receiver part
            List<Coordinate> roGround = new ArrayList<>();
            // Compute ground profile from R to first O (first building corner)
            int rOIndex = 0;
            for(TriIdWithIntersection tri : allInterPoints) {
                rOIndex++;
                if (tri.isIntersectionOnBuilding()) {
                    break;
                }
            }
            roGround.addAll(getGroundProfile(allInterPoints.subList(0, rOIndex)));
            // Source part
            // Compute ground profile from last O to S (last building corner)
            int sOIndex = allInterPoints.size() - 1;
            for(int index = allInterPoints.size() - 1; index >= 0; index--) {
                TriIdWithIntersection tri = allInterPoints.get(index);
                sOIndex = index;
                if (tri.isIntersectionOnBuilding()) {
                    break;
                }
            }
            List<Coordinate> oSGround = getGroundProfile(allInterPoints.subList(sOIndex, allInterPoints.size()));
            // As the insertion was done reversed this is actually sOGround, reverse again in order to fit with variable name
            totData = new DiffractionWithSoilEffetZone(rOZone, sOZone, deltaDistance,deltaDistancefav, e, pathDistance,pathDistancefav,
                    roGround, oSGround, oSGround,pointHeight);
            return totData;
        }
    }


    /**
     * We will get all of building corners Z and set the building a average height using corner Z and original building height
     *
     * @param polygonWithHeight
     */
    private void setAverageBuildingHeight(List<MeshBuilder.PolygonWithHeight> polygonWithHeight) {

        for (MeshBuilder.PolygonWithHeight polygon : polygonWithHeight) {
            //When we get all of building, we will set every vertices of the same building a same Z,
            //using the Average "z+height"
            Coordinate[] buildingCoor = polygon.getGeometry().getCoordinates();
            double buildingHeight = polygon.getHeight();
            //if the building is closed
            Double sumBuildingHeight = 0.;
            Double averageBuildingHeight = 0.;
            if (buildingHeight == Double.MAX_VALUE) {
                averageBuildingHeight = buildingHeight;
            } else {
                if (buildingCoor[0].equals(buildingCoor[buildingCoor.length - 1]) && buildingCoor.length - 1 >= 3) {
                    for (int j = 0; j < buildingCoor.length - 1; j++) {
                        sumBuildingHeight += buildingCoor[j].z;
                    }

                    averageBuildingHeight = sumBuildingHeight / (buildingCoor.length - 1);
                }

            }
            //set the averageBuildingZ
            polygon.setHeight(averageBuildingHeight + buildingHeight);
        }

    }

    /**
     * https://stackoverflow.com/questions/9970281/java-calculating-the-angle-between-two-points-in-degrees
     * Calculates the angle from centerPt to targetPt in degrees.
     * The return should range from [0,360), rotating CLOCKWISE,
     * 0 and 360 degrees represents NORTH,
     * 90 degrees represents EAST, etc...
     *
     * Assumes all points are in the same coordinate space.  If they are not,
     * you will need to call SwingUtilities.convertPointToScreen or equivalent
     * on all arguments before passing them  to this function.
     *
     * @param centerPt   Point we are rotating around.
     * @param targetPt   Point we want to calcuate the angle to.
     * @return angle in degrees.  This is the angle from centerPt to targetPt.
     */
    public static double calcRotationAngleInDegrees(Coordinate centerPt, Coordinate targetPt)
    {
        // calculate the angle theta from the deltaY and deltaX values
        // (atan2 returns radians values from [-PI,PI])
        // 0 currently points EAST.
        // NOTE: By preserving Y and X param order to atan2,  we are expecting
        // a CLOCKWISE angle direction.
        double theta = Math.atan2(targetPt.y - centerPt.y, targetPt.x - centerPt.x);

        // rotate the theta angle clockwise by 90 degrees
        // (this makes 0 point NORTH)
        // NOTE: adding to an angle rotates it clockwise.
        // subtracting would rotate it counter-clockwise
        theta += Math.PI/2.0;

        // convert from radians to degrees
        // this will give you an angle from [0->270],[-180,0]
        double angle = Math.toDegrees(theta);

        // convert to positive range [0-360)
        // since we want to prevent negative angles, adjust them now.
        // we can assume that atan2 will not return a negative value
        // greater than one partial rotation
        if (angle < 0) {
            angle += 360;
        }

        return angle;
    }

    /**
     * Calculate the Z of intersection point
     * {@see http://en.wikipedia.org/wiki/Linear_interpolation}
     *
     * @param p1           a point of intersected segment
     * @param p2           other point of intersected segment
     * @param intersection the intersection which includes the x and y
     * @return z of intersection point
     */
    private double calculateLinearInterpolation(Coordinate p1, Coordinate p2, Coordinate intersection) {
        return Vertex.interpolateZ(intersection, p1,p2);
    }

    /**
     * Equation Plane: ax+by+cz+d=0, can be fixed by 3 given points
     * When we fix a,b,c,d by given 3 points, we can get Z of given point X,Y
     * z=-(ax+by+d)/c
     *
     * @param p1    first point
     * @param p2    second point
     * @param p3    third point
     * @param point the point which includes the x and y
     * @return z of point
     * {@see http://en.wikipedia.org/wiki/Plane_%28geometry%29}
     */

    private double getTopoZByGiven3Points(Coordinate p1, Coordinate p2, Coordinate p3, Coordinate point) {
        return Vertex.interpolateZ(point, p1, p2, p3);
    }

    public static final class Wall extends LineSegment {
        private int buildingId = 0;

        public Wall(Coordinate p0, Coordinate p1, int buildingId) {
            super(p0, p1);
            this.buildingId = buildingId;
        }

        public int getBuildingId() {
            return buildingId;
        }
    }


    /**
     * Convert triangle mesh to sql instructions for debug purposes
     * @return
     */
    public String dumpMesh() {
        StringBuilder sb = new StringBuilder();
        // Dump Triangles
        sb.append("DROP TABLE IF EXISTS TRIANGLES, TRI_NEIGHBOURS;\n");
        sb.append("CREATE TABLE TRIANGLES(id serial, the_geom POLYGON);\n");
        sb.append("CREATE TABLE TRI_NEIGHBOURS(id serial, the_geom LINESTRING);\n");
        GeometryFactory gf = new GeometryFactory();
        WKTWriter wktWriter = new WKTWriter(3);
        int idTriangle = 0;
        for( Triangle t : triVertices) {
            Coordinate[] line = new Coordinate[] {vertices.get(t.getA()), vertices.get(t.getB()), vertices.get(t.getC()), vertices.get(t.getA())};
            sb.append(String.format("INSERT INTO TRIANGLES(THE_GEOM) VALUES ('%s');\n", gf.createPolygon(line)));
            Coordinate from = new org.locationtech.jts.geom.Triangle(vertices.get(t.getA()), vertices.get(t.getB()), vertices.get(t.getC())).centroid();
            // Dump neighbours links
            Triangle neigh = triNeighbors.get(idTriangle);
            for(int n = 0; n < 3; n++) {
                int vIndex = neigh.get(n);
                if(vIndex >= 0) {
                    Triangle tn = triVertices.get(vIndex);
                    Coordinate to = new org.locationtech.jts.geom.Triangle(vertices.get(tn.getA()), vertices.get(tn.getB()), vertices.get(tn.getC())).centroid();
                    sb.append(String.format("INSERT INTO TRI_NEIGHBOURS(THE_GEOM) VALUES ('%s');\n", gf.createLineString(new Coordinate[]{from, to})));
                }
            }
            idTriangle++;
        }
        return sb.toString();
    }



    public static abstract class IntersectionRayVisitor implements ItemVisitor {
        Set<Integer> buildingsprocessed = new HashSet<>();
        List<MeshBuilder.PolygonWithHeight> polygonWithHeight;
        Coordinate p1;
        Coordinate p2;
        LineString seg;
        public IntersectionRayVisitor(List<MeshBuilder.PolygonWithHeight> polygonWithHeight, Coordinate p1,
                                      Coordinate p2) {
            this.polygonWithHeight = polygonWithHeight;
            this.p1 = p1;
            this.p2 = p2;
            seg = new LineSegment(p1, p2).toGeometry(new GeometryFactory());
        }

        public abstract void addBuilding(int buildingId);

        @Override
        public void visitItem(Object item) {
            int buildingId = (Integer) item;
            if(!buildingsprocessed.contains(buildingId)) {
                buildingsprocessed.add(buildingId);
                addBuilding(buildingId);
            }
        }

        public boolean doContinue() {
            return true;
        }
    }
}
