package org.noise_planet.noisemodelling.pathfinder;

import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinfour.common.*;
import org.tinfour.standard.IncrementalTin;
import org.tinfour.utils.TriangleCollector;

import java.util.*;
import java.util.function.Consumer;

public class LayerTinfour implements LayerDelaunay {
    private double epsilon = 0.001; // merge of Vertex instances below this distance
    private static final Logger LOGGER = LoggerFactory.getLogger(LayerTinfour.class);

    //private Map<Vertex, Integer> pts = new HashMap<Vertex, Integer>();
    //private List<Integer> segments = new ArrayList<Integer>();
    List<IConstraint> constraints = new ArrayList<>();
    List<Integer> constraintIndex = new ArrayList<>();

    Quadtree ptsIndex = new Quadtree();
    private boolean computeNeighbors = false;
    private double maxArea = 0;

    // Output data
    private List<Coordinate> vertices = new ArrayList<Coordinate>();
    private List<Triangle> triangles = new ArrayList<Triangle>();
    private List<Triangle> neighbors = new ArrayList<Triangle>(); // The first neighbor triangle is opposite the first corner of triangle  i

    private Vertex addCoordinate(Coordinate coordinate, int index) {
        List result = ptsIndex.query(new Envelope(coordinate));
        Vertex found = null;
        for(Object vertex : result) {
            if(vertex instanceof Vertex) {
                if(((Vertex) vertex).getDistance(coordinate.x, coordinate.y) < epsilon) {
                    found = (Vertex) vertex;
                    break;
                }
            }
        }
        if(found == null) {
            found = new Vertex(coordinate.x, coordinate.y, coordinate.z, index);
            ptsIndex.insert(new Envelope(coordinate),  found);
        }
        return found;
    }

    private List<SimpleTriangle> computeTriangles(IncrementalTin incrementalTin) {
        ArrayList<SimpleTriangle> triangles = new ArrayList<>(incrementalTin.countTriangles().getCount());
        TriangleBuilder triangleBuilder = new TriangleBuilder(triangles);
        TriangleCollector.visitSimpleTriangles(incrementalTin, triangleBuilder);
        return triangles;
    }

    /**
     * @return Merge vertices closer than specified epsilon
     */
    public double getEpsilon() {
        return epsilon;
    }

    /**
     * @param epsilon Merge vertices closer than specified epsilon
     */
    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
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

        List<Vertex> meshPoints = ptsIndex.queryAll();

        IncrementalTin tin;
        boolean refine;
        List<SimpleTriangle> simpleTriangles = new ArrayList<>();
        do {
            // Triangulate
            tin = new IncrementalTin();
            // Add points
            tin.add(meshPoints, null);
            // Add constraints
            tin.addConstraints(constraints, maxArea > 0);
            refine = false;

            simpleTriangles = computeTriangles(tin);
            // Will triangulate multiple time if refinement is necessary
            if(maxArea > 0) {
                for (SimpleTriangle triangle : simpleTriangles) {
                    if(triangle.getArea() > maxArea) {
                        // Insert steiner point in circumcircle
                        Coordinate centroid = getCentroid(triangle);
                        meshPoints.add(new Vertex(centroid.x, centroid.y, centroid.z));
                        refine = true;
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
            if(t.getContainingRegion() != null) {
                if(t.getContainingRegion().getConstraintIndex() < constraintIndex.size()) {
                    triangleAttribute = constraintIndex.get(t.getContainingRegion().getConstraintIndex());
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
        // To avoid errors we set NaN Z coordinates to 0.
        LayerTinfour.SetZFilter zFilter = new LayerTinfour.SetZFilter();
        newPoly.apply(zFilter);
        GeometryFactory factory = new GeometryFactory();
        final Coordinate[] coordinates = newPoly.getExteriorRing().getCoordinates();
        // Exterior ring must be CCW
        if(!Orientation.isCCW(coordinates)) {
            CoordinateArrays.reverse(coordinates);
        }
        if (coordinates.length >= 4) {
            List<Vertex> vertexList = new ArrayList<>();
            for(int vId = 0; vId < coordinates.length - 1 ; vId++) {
                vertexList.add(addCoordinate(coordinates[vId], buildingId));
            }
            PolygonConstraint polygonConstraint = new PolygonConstraint(vertexList);
            constraints.add(polygonConstraint);
            constraintIndex.add(buildingId);
        }
        // Append holes
        final int holeCount = newPoly.getNumInteriorRing();
        for (int holeIndex = 0; holeIndex < holeCount; holeIndex++) {
            LineString holeLine = newPoly.getInteriorRingN(holeIndex);
            final Coordinate[] hCoordinates = holeLine.getCoordinates();
            // Holes must be CW
            if(Orientation.isCCW(hCoordinates)) {
                CoordinateArrays.reverse(hCoordinates);
            }
            // Should be clock wise
            List<Vertex> vertexList = new ArrayList<>();
            for(int vId = 0; vId < hCoordinates.length - 1 ; vId++) {
                vertexList.add(addCoordinate(hCoordinates[vId], buildingId));
            }
            PolygonConstraint polygonConstraint = new PolygonConstraint(vertexList);
            constraints.add(polygonConstraint);
            constraintIndex.add(buildingId);
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
        addCoordinate(vertexCoordinate, 0);
    }

    @Override
    public void setMaxArea(Double maxArea) throws LayerDelaunayError {
        this.maxArea = Math.max(0, maxArea);
    }

    //add buildingID to edge property and to points property
    public void addLineString(LineString lineToProcess, int buildingID) throws LayerDelaunayError {
        Coordinate[] coordinates = lineToProcess.getCoordinates();
        List<Vertex> vertexList = new ArrayList<>();
        for(Coordinate coordinate : coordinates) {
            vertexList.add(addCoordinate(coordinate, buildingID));
        }
        LinearConstraint linearConstraint = new LinearConstraint(vertexList);
        constraints.add(linearConstraint);
        constraintIndex.add(buildingID);
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
}
