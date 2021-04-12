package org.noise_planet.noisemodelling.pathfinder;

import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.strtree.STRtree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinfour.common.*;
import org.tinfour.standard.IncrementalTin;
import org.tinfour.utils.TriangleCollector;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

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
    private HashMap<Integer, LayerTinfour.BuildingWithID> buildingWithID = new HashMap<Integer, LayerTinfour.BuildingWithID>();
    private boolean computeNeighbors = false;
    private double maxArea = 0;

    private GeometryFactory factory = new GeometryFactory();


    // Output data
    private List<Coordinate> vertices = new ArrayList<Coordinate>();
    private List<Triangle> triangles = new ArrayList<Triangle>();
    private List<Triangle> neighbors = new ArrayList<Triangle>(); // The first neighbor triangle is opposite the first corner of triangle  i

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
    }

    private List<SimpleTriangle> computeTriangles(IncrementalTin incrementalTin) {
        ArrayList<SimpleTriangle> triangles = new ArrayList<>(incrementalTin.countTriangles().getCount());
        TriangleBuilder triangleBuilder = new TriangleBuilder(triangles);
        TriangleCollector.visitSimpleTriangles(incrementalTin, triangleBuilder);
        return triangles;
    }

    private static class TriangleBuilder implements Consumer<SimpleTriangle> {
        ArrayList<SimpleTriangle> triangles;

        public TriangleBuilder(ArrayList<SimpleTriangle> triangles) {
            this.triangles = triangles;
        }

        @Override
        public void accept(SimpleTriangle triangle) {
            triangles.add(triangle);
        }
    }

    private static Coordinate getCentroid(SimpleTriangle triangle) {
        Vertex va = triangle.getVertexA();
        Vertex vb = triangle.getVertexB();
        Vertex vc = triangle.getVertexC();
        double cx = ( va.getX() + vb.getX() + vc.getX() ) / 3d;
        double cy = ( va.getY() + vb.getY() + vc.getY() ) / 3d;
        double cz = ( va.getZ() + vb.getZ() + vc.getZ() ) / 3d;
        return new Coordinate( cx, cy, cz);
    }

    @Override
    public void processDelaunay() throws LayerDelaunayError {
        triangles.clear();
        vertices.clear();

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

        STRtree buildingsRtree = new STRtree(Math.max(10, buildingWithID.size()));
        for (Map.Entry<Integer, LayerTinfour.BuildingWithID> buildingWithIDEntry : buildingWithID.entrySet()) {
            buildingsRtree.insert(buildingWithIDEntry.getValue().building.getEnvelopeInternal(), buildingWithIDEntry.getKey());
        }

        IncrementalTin tin;
        boolean refine;
        List<SimpleTriangle> simpleTriangles = new ArrayList<>();
        do {
            // Triangulate
            tin = new IncrementalTin();
            // Add points
            tin.add(meshPoints, null);
            // Add constraints
            List<IConstraint> constraints = new ArrayList<>(segments.size());
            for(int i=0; i < segments.size(); i+=2) {
                constraints.add(new LinearConstraint(meshPoints.get(segments.get(i)), meshPoints.get(segments.get(i+1))));
            }
            tin.addConstraints(constraints, maxArea > 0);
            refine = false;

            simpleTriangles = computeTriangles(tin);
            // Will triangulate multiple time if refinement is necessary
            if(maxArea > 0) {
                for (SimpleTriangle triangle : simpleTriangles) {
                    if(triangle.getArea() > maxArea) {
                        // Insert steiner point in circumcircle
                        Coordinate circumcentre = org.locationtech.jts.geom.Triangle.circumcentre(toCoordinate(triangle.getVertexA()),
                                toCoordinate(triangle.getVertexB()),
                                toCoordinate(triangle.getVertexC()));
                        // Do not add steiner points into buildings
                        Envelope searchEnvelope = new Envelope(circumcentre);
                        searchEnvelope.expandBy(1.);
                        List polyInters = buildingsRtree.query(searchEnvelope);
                        boolean inBuilding = false;
                        for (Object id : polyInters) {
                            if (id instanceof Integer) {
                                LayerTinfour.BuildingWithID inPoly = buildingWithID.get(id);
                                if (inPoly.building.contains(factory.createPoint(circumcentre))) {
                                    inBuilding = true;
                                    break;
                                }
                            }
                        }
                        if(!inBuilding) {
                            meshPoints.add(new Vertex(circumcentre.x, circumcentre.y, circumcentre.z));
                            refine = true;
                        }
                    }
                }
            }
        } while (refine);
        List<Vertex> verts = tin.getVertices();
        vertices = new ArrayList<>(verts.size());
        Map<Vertex, Integer> vertIndex = new HashMap<>();
        for(Vertex v : verts) {
            vertIndex.put(v, vertices.size());
            vertices.add(toCoordinate(v));
        }
        Map<Integer, Integer> edgeIndexToTriangleIndex = new HashMap<>();
        for(SimpleTriangle t : simpleTriangles) {
            int triangleAttribute = 0;
            // Insert steiner point in centroid
            Coordinate centroid = getCentroid(t);
            // Do not add steiner points into buildings
            Envelope searchEnvelope = new Envelope(centroid);
            searchEnvelope.expandBy(1.);
            List polyInters = buildingsRtree.query(searchEnvelope);
            for (Object id : polyInters) {
                if (id instanceof Integer) {
                    LayerTinfour.BuildingWithID inPoly = buildingWithID.get(id);
                    if (inPoly.building.contains(factory.createPoint(centroid))) {
                        triangleAttribute = (int) id;
                        break;
                    }
                }
            }
            triangles.add(new Triangle(vertIndex.get(t.getVertexA()), vertIndex.get(t.getVertexB()),vertIndex.get(t.getVertexC()), triangleAttribute));
            edgeIndexToTriangleIndex.put(t.getEdgeA().getIndex(), triangles.size() - 1);
            edgeIndexToTriangleIndex.put(t.getEdgeB().getIndex(), triangles.size() - 1);
            edgeIndexToTriangleIndex.put(t.getEdgeC().getIndex(), triangles.size() - 1);
        }
        if(computeNeighbors) {
            for(SimpleTriangle t : simpleTriangles) {
                Integer neighA = edgeIndexToTriangleIndex.get(t.getEdgeA().getDual().getIndex());
                Integer neighB = edgeIndexToTriangleIndex.get(t.getEdgeB().getDual().getIndex());
                Integer neighC = edgeIndexToTriangleIndex.get(t.getEdgeC().getDual().getIndex());
                neighbors.add(new Triangle(neighA != null ? neighA : -1,
                        neighB != null ? neighB : -1,
                        neighC != null ? neighC : -1));
            }
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
        return triangles;
    }

    private static Coordinate toCoordinate(Vertex v) {
        return new Coordinate(v.getX(), v.getY(), v.getZ());
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

        protected int addVertex(Vertex pt) {
            Integer index = pts.get(pt);
            if (index == null) {
                index = maxIndex.getAndAdd(1);
                pts.put(pt, index);
            }
            return index;
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
