package org.noise_planet.noisemodelling.pathfinder;

import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.strtree.STRtree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinfour.common.PolygonConstraint;
import org.tinfour.common.Vertex;
import org.tinfour.standard.IncrementalTin;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class LayerTinfour implements LayerDelaunay {
    // Precision
    private MathContext mathContext = MathContext.DECIMAL64;
    private static final Logger LOGGER = LoggerFactory.getLogger(LayerTinfour.class);

    private double r(double v) {
        return new BigDecimal(v).round(mathContext).doubleValue();
    }

    private Map<Vertex, Integer> pts = new HashMap<Vertex, Integer>();
    private List<Integer> segments = new ArrayList<Integer>();
    private AtomicInteger pointsCount = new AtomicInteger(0);
    private LayerTinfour.PointHandler pointHandler = new LayerTinfour.PointHandler(this, pts, pointsCount);
    private LayerTinfour.LineStringHandler lineStringHandler = new LayerTinfour.LineStringHandler(this, pts, pointsCount, segments);
    private List<Coordinate> vertices = new ArrayList<Coordinate>();
    private HashMap<Integer, LayerTinfour.BuildingWithID> buildingWithID = new HashMap<Integer, LayerTinfour.BuildingWithID>();

    private boolean computeNeighbors = false;
    private List<Triangle> triangles = new ArrayList<Triangle>();
    private List<Triangle> neighbors = new ArrayList<Triangle>(); // The first neighbor triangle is opposite the first corner of triangle  i
    private HashMap<Integer, LinkedList<Integer>> hashOfArrayIndex = new HashMap<Integer, LinkedList<Integer>>();
    private double maxArea = 0;

    private GeometryFactory factory = new GeometryFactory();

    private static Coordinate TPointToCoordinate(Vertex tPoint) {
        return new Coordinate(tPoint.getX(), tPoint.getY(), tPoint.getZ());
    }

    private static Vertex CoordinateToTPoint(Coordinate coordinate) {
        return new Vertex(coordinate.x, coordinate.y, coordinate.z, -1);
    }

    private static Vertex CoordinateToTPoint(Coordinate coordinate, int attribute) {
        return new Vertex(coordinate.x, coordinate.y, coordinate.z, attribute);
    }

    private static final class BuildingWithID {
        private Polygon building;


        public BuildingWithID(Polygon building) {
            this.building = building;

        }


        public boolean isTriangleInBuilding(Vertex point) {
            return this.building.intersects(new GeometryFactory().createPoint(TPointToCoordinate(point)));
        }


    }

    private int getOrAppendVertices(Coordinate newCoord, List<Coordinate> vertices, HashMap<Integer, LinkedList<Integer>> hashOfArrayIndex) {
        // We can obtain the same hash with two different coordinate (4 Bytes or
        // 8 Bytes against 12 or 24 Bytes) , then we use a list as the value of
        // the hashmap
        // First step - Search the vertice parameter within the hashMap
        int newCoordIndex = -1;
        Integer coordinateHash = newCoord.hashCode();
        LinkedList<Integer> listOfIndex = hashOfArrayIndex.get(coordinateHash);
        if (listOfIndex != null) // There are the same hash value
        {
            for (int vind : listOfIndex) // Loop inside the coordinate index
            {
                if (newCoord.equals3D(vertices.get(vind))) // the coordinate is
                // equal to the
                // existing
                // coordinate
                {
                    newCoordIndex = vind;
                    break; // Exit for loop
                }
            }
            if (newCoordIndex == -1) {
                // No vertices has been found, we push the new coordinate into
                // the existing linked list
                newCoordIndex = vertices.size();
                listOfIndex.add(newCoordIndex);
                vertices.add(newCoord);
            }
        } else {
            // Push a new hash element
            listOfIndex = new LinkedList<Integer>();
            newCoordIndex = vertices.size();
            listOfIndex.add(newCoordIndex);
            vertices.add(newCoord);
            hashOfArrayIndex.put(coordinateHash, listOfIndex);
        }
        return newCoordIndex;
    }

    @Override
    public void processDelaunay() throws LayerDelaunayError {
        triangles.clear();
        neighbors.clear();
        vertices.clear();
        hashOfArrayIndex.clear();

        // Create input data for Poly2Tri
        int[] index = new int[segments.size()];
        for (int i = 0; i < index.length; i++) {
            index[i] = segments.get(i);
        }
        // Construct final points array by reversing key,value of hash map
        Vertex[] ptsArray = new Vertex[pointsCount.get()];
        for(Map.Entry<Vertex, Integer> entry : pts.entrySet()) {
            ptsArray[entry.getValue()] = entry.getKey();
        }

        List<Vertex> meshPoints = new ArrayList<>(Arrays.asList(ptsArray));

        STRtree buildingsRtree = null;
        if(maxArea > 0) {
            // STRtree minimal size is 2
            buildingsRtree = new STRtree(Math.max(10, buildingWithID.size()));
            for (Map.Entry<Integer, LayerTinfour.BuildingWithID> buildingWithIDEntry : buildingWithID.entrySet()) {
                buildingsRtree.insert(buildingWithIDEntry.getValue().building.getEnvelopeInternal(), buildingWithIDEntry.getKey());
            }
        }

        IncrementalTin tin = new IncrementalTin();

        boolean refine;
        do {
            // Triangulate
            Poly2Tri.triangulate(TriangulationAlgorithm.DTSweep, convertedInput);
            refine = false;

            // Will triangulate multiple time if refinement is necessary
            if(buildingsRtree != null && maxArea > 0) {
                List<DelaunayTriangle> trianglesDelaunay = convertedInput.getTriangles();
                for (DelaunayTriangle triangle : trianglesDelaunay) {
                    if(triangle.area() > maxArea) {
                        // Insert steiner point in centroid
                        TPoint centroid = triangle.centroid();
                        // Do not add steiner points into buildings
                        Envelope searchEnvelope = new Envelope(TPointToCoordinate(centroid));
                        searchEnvelope.expandBy(1.);
                        List polyInters = buildingsRtree.query(searchEnvelope);
                        boolean inBuilding = false;
                        for (Object id : polyInters) {
                            if (id instanceof Integer) {
                                LayerTinfour.BuildingWithID inPoly = buildingWithID.get(id);
                                if (inPoly.building.contains(factory.createPoint(TPointToCoordinate(centroid)))) {
                                    inBuilding = true;
                                    break;
                                }
                            }
                        }
                        if(!inBuilding) {
                            meshPoints.add(centroid);
                            refine = true;
                        }
                    }
                }
                if(refine) {
                    convertedInput = new ConstrainedPointSet(meshPoints, index);
                }
            }
        } while (refine);


        List<DelaunayTriangle> trianglesDelaunay = convertedInput.getTriangles();
        List<Integer> triangleAttribute = Arrays.asList(new Integer[trianglesDelaunay.size()]);
        // Create an index of triangles instance for fast neighbors search
        Map<DelaunayTriangle, Integer> triangleSearch = new HashMap<>(trianglesDelaunay.size());
        int triangleIndex = 0;
        if (computeNeighbors) {
            for (DelaunayTriangle triangle : trianglesDelaunay) {
                triangleSearch.put(triangle, triangleIndex);
                triangleIndex++;
            }
        }

        //Build ArrayList for binary search
        //test add height
        int triangleId = 0;
        for (DelaunayTriangle triangle : trianglesDelaunay) {
            Coordinate[] ring = new Coordinate[]{TPointToCoordinate(triangle.points[0]), TPointToCoordinate(triangle.points[1]), TPointToCoordinate(triangle.points[2]), TPointToCoordinate(triangle.points[0])};
            //if one of three vertices have buildingID and buildingID>=1
            if (getPointAttribute(triangle.points[0]) >= 1 || getPointAttribute(triangle.points[1]) >= 1 || getPointAttribute(triangle.points[2]) >= 1) {
                int propertyBulidingID = 0;
                for (int i = 0; i <= 2; i++) {
                    int potentialBuildingID = getPointAttribute(triangle.points[i]);
                    if (potentialBuildingID >= 1) {
                        //get the Barycenter of the triangle so we can sure this point is in this triangle and we will check if the building contain this point
                        if (this.buildingWithID.get(potentialBuildingID).isTriangleInBuilding(triangle.centroid())) {
                            propertyBulidingID = potentialBuildingID;
                            break;
                        }
                    }
                }
                triangleAttribute.set(triangleId, propertyBulidingID);
            } else {
                //if there are less than 3 points have buildingID this triangle is out of building
                triangleAttribute.set(triangleId, 0);
            }

            if (!Orientation.isCCW(ring)) {
                Coordinate tmp = new Coordinate(ring[0]);
                ring[0] = ring[2];
                ring[2] = tmp;
            }

            int a = getOrAppendVertices(ring[0], vertices, hashOfArrayIndex);
            int b = getOrAppendVertices(ring[1], vertices, hashOfArrayIndex);
            int c = getOrAppendVertices(ring[2], vertices, hashOfArrayIndex);
            triangles.add(new Triangle(a, b, c, triangleAttribute.get(triangleId)));
            if (computeNeighbors) {
                // Compute neighbors index
                neighbors.add(new Triangle(getTriangleIndex(triangleSearch, triangle.neighborAcross(triangle.points[0])), getTriangleIndex(triangleSearch, triangle.neighborAcross(triangle.points[1])), getTriangleIndex(triangleSearch, triangle.neighborAcross(triangle.points[2]))));
            }
            triangleId++;
        }
    }


    public static final class SetZFilter implements CoordinateSequenceFilter {
        private boolean done = false;
        private boolean resetToZero = false;

        public SetZFilter() {

        }

        public SetZFilter(boolean resetToZero) {
            this.resetToZero = resetToZero;
        }

        @Override
        public void filter(CoordinateSequence seq, int i) {
            double x = seq.getX(i);
            double y = seq.getY(i);
            double z = seq.getOrdinate(i, 2);
            seq.setOrdinate(i, 0, x);
            seq.setOrdinate(i, 1, y);
            if (Double.isNaN(z) || resetToZero) {
                seq.setOrdinate(i, 2, 0);
            } else {
                seq.setOrdinate(i, 2, z);
            }
            if (i == seq.size()) {
                done = true;
            }
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public boolean isGeometryChanged() {
            return true;
        }
    }

    /**
     * Add height of building
     *
     * @return
     */
    @Override
    public void addPolygon(Polygon newPoly, int buildingId) throws LayerDelaunayError {

        //// To avoid errors we set the Z coordinate to 0.
        LayerTinfour.SetZFilter zFilter = new LayerTinfour.SetZFilter();
        newPoly.apply(zFilter);
        GeometryFactory factory = new GeometryFactory();
        final Coordinate[] coordinates = newPoly.getExteriorRing().getCoordinates();
        if (coordinates.length > 1) {
            LineString newLineString = factory.createLineString(coordinates);
            this.addLineString(newLineString, buildingId);
            this.buildingWithID.put(buildingId, new LayerTinfour.BuildingWithID(newPoly));
        }
        // Append holes
        final int holeCount = newPoly.getNumInteriorRing();
        for (int holeIndex = 0; holeIndex < holeCount; holeIndex++) {
            LineString holeLine = newPoly.getInteriorRingN(holeIndex);
            // Convert hole into a polygon, then compute an interior point
            Polygon polyBuffnew = factory.createPolygon(factory.createLinearRing(holeLine.getCoordinates()), null);
            if (polyBuffnew.getArea() > 0.) {
                Coordinate interiorPoint = polyBuffnew.getInteriorPoint().getCoordinate();
                if (!factory.createPoint(interiorPoint).intersects(holeLine)) {
                    this.addLineString(holeLine, buildingId);
                } else {
                    LOGGER.info("Warning : hole rejected, can't find interior point.");
                }
            } else {
                LOGGER.info("Warning : hole rejected, area=0");
            }
        }
    }

    @Override
    public void setMinAngle(Double minAngle) {
        // TODO Auto-generated method stub

    }

    @Override
    public void hintInit(Envelope bBox, long polygonCount, long verticesCount) throws LayerDelaunayError {
    }

    @Override
    public List<Coordinate> getVertices() throws LayerDelaunayError {
        return this.vertices;
    }

    @Override
    public List<Triangle> getTriangles() throws LayerDelaunayError {
        return this.triangles;
    }

    @Override
    public void addVertex(Coordinate vertexCoordinate) throws LayerDelaunayError {
        pointHandler.addPt(vertexCoordinate, -1);
    }

    @Override
    public void setMaxArea(Double maxArea) throws LayerDelaunayError {
        this.maxArea = Math.max(0, maxArea);
    }

    //add buildingID to edge property and to points property
    public void addLineString(LineString lineToProcess, int buildingID) throws LayerDelaunayError {
        lineStringHandler.reset();
        lineStringHandler.setAttribute(buildingID);
        lineToProcess.apply(lineStringHandler);
    }
    //add buildingID to edge property and to points property

    @Override
    public void reset() {
        // TODO Auto-generated method stub

    }

    @Override
    public List<Triangle> getNeighbors() throws LayerDelaunayError {
        if (computeNeighbors) {
            return neighbors;
        } else {
            throw new LayerDelaunayError("You must call setRetrieveNeighbors(True) before process delaunay triangulation");
        }
    }

    @Override
    public void setRetrieveNeighbors(boolean retrieve) {
        this.computeNeighbors = retrieve;

    }


    private static class PointHandler implements CoordinateFilter {
        private LayerTinfour delaunayData;
        private Map<Vertex, Integer> pts;
        private AtomicInteger maxIndex;

        public PointHandler(LayerTinfour delaunayData, Map<Vertex, Integer> pts, AtomicInteger maxIndex) {
            this.delaunayData = delaunayData;
            this.pts = pts;
            this.maxIndex = maxIndex;
        }

        public Coordinate[] getPoints() {
            Coordinate[] ret = new Coordinate[pts.size()];
            int i = 0;
            for (Vertex pt : pts.keySet()) {
                ret[i] = TPointToCoordinate(pt);
                i++;
            }
            return ret;
        }

        protected int addPt(Coordinate coordinate, int attribute) {
            Vertex pt = new Vertex(delaunayData.r(coordinate.x), delaunayData.r(coordinate.y), Double.isNaN(coordinate.z) ? 0 : delaunayData.r(coordinate.z), attribute);
            Integer index = pts.get(pt);
            if (index == null) {
                index = maxIndex.getAndAdd(1);
                pts.put(pt, index);
            }
            return index;
        }

        @Override
        public void filter(Coordinate pt) {
            addPt(pt, -1);
        }
    }

    private static final class LineStringHandler extends LayerTinfour.PointHandler {
        private List<Integer> segments;
        private int firstPtIndex = -1;
        private int attribute = -1;

        public LineStringHandler(LayerTinfour delaunayData, Map<Vertex, Integer> pts, AtomicInteger maxIndex, List<Integer> segments) {
            super(delaunayData, pts, maxIndex);
            this.segments = segments;
        }

        /**
         * New line string
         */
        public void reset() {
            firstPtIndex = -1;
            attribute = -1;
        }

        public void setAttribute(int attribute) {
            this.attribute = attribute;
        }

        @Override
        public void filter(Coordinate pt) {
            if (firstPtIndex == -1) {
                firstPtIndex = addPt(pt, attribute);
            } else {
                int secondPt = addPt(pt, attribute);
                if (secondPt != firstPtIndex) {
                    segments.add(firstPtIndex);
                    segments.add(secondPt);
                    firstPtIndex = secondPt;
                }
            }
        }
    }

}
