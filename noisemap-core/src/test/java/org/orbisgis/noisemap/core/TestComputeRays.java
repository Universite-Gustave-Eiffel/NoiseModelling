package org.orbisgis.noisemap.core;

import org.apache.commons.math3.geometry.euclidean.threed.Line;
import org.apache.commons.math3.geometry.euclidean.threed.Plane;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.functions.spatial.affine_transformations.ST_Translate;
import org.h2gis.functions.spatial.volume.GeometryExtrude;
import org.h2gis.utilities.jts_utils.CoordinateUtils;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFilter;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.locationtech.jts.geom.util.GeometryEditor;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.math.Plane3D;
import org.locationtech.jts.math.Vector2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class TestComputeRays {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestComputeRays.class);


    /**
     * Offset de Z coordinates by the height of the ground
     */
    public static final class SetCoordinateSequenceFilter implements CoordinateSequenceFilter {
        AtomicBoolean geometryChanged = new AtomicBoolean(false);
        double newValue;

        public SetCoordinateSequenceFilter(double newValue) {
            this.newValue = newValue;
        }

        @Override
        public void filter(CoordinateSequence coordinateSequence, int i) {
            Coordinate pt = coordinateSequence.getCoordinate(i);
            pt.setOrdinate(2,newValue);
            geometryChanged.set(true);
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public boolean isGeometryChanged() {
            return geometryChanged.get();
        }
    }

    private static final List<Integer> freqLvl = Collections.unmodifiableList(Arrays.asList(63, 125, 250, 500, 1000, 2000,
            4000, 8000));

    private static final double ERROR_EPSILON_TEST_T = 0.2;


    private void splCompare(double[] resultW, String testName, double[] expectedLevel, double splEpsilon) {
        for (int i = 0; i < resultW.length; i++) {
            double dba = resultW[i];
            double expected = expectedLevel[i];
            assertEquals("Unit test " + testName + " failed at " + freqLvl.get(i) + " Hz", expected, dba, splEpsilon);
        }
    }

    private void writeVTKmesh(String filename, ComputeRaysOut propDataOut, MeshBuilder mesh) throws IOException {

        int lengthPolygon = mesh.getPolygonWithHeight().get(0).geo.getBoundary().getCoordinates().length;

        FileWriter fileWriter = new FileWriter(filename);
        fileWriter.write("# vtk DataFile Version 2.0\n");
        fileWriter.write("PropagationPath\n");
        fileWriter.write("ASCII\n");
        fileWriter.write("DATASET POLYDATA\n");
        fileWriter.write("POINTS " + String.valueOf(propDataOut.propagationPaths.get(0).getPointList().size() + 2 * lengthPolygon) + " float\n");

        GeometryFactory geometryFactory = new GeometryFactory();
        List<Coordinate> coordinates = new ArrayList<>();
        for (PropagationPath.PointPath p : propDataOut.propagationPaths.get(0).getPointList()) {
            coordinates.add(p.coordinate);
            fileWriter.write(String.valueOf(p.coordinate.x) + " " + String.valueOf(p.coordinate.y) + " " + String.valueOf(p.coordinate.z) + "\n");
        }
        LineString factoryLineString = geometryFactory.createLineString(coordinates.toArray(new Coordinate[coordinates.size()]));
        WKTWriter wktWriter = new WKTWriter(3);
        mesh.getPolygonWithHeight().get(0).geo.getCoordinate();
        for (int j = 0; j < lengthPolygon; j++) {
            double x = mesh.getPolygonWithHeight().get(0).geo.getBoundary().getCoordinates()[j].x;
            double y = mesh.getPolygonWithHeight().get(0).geo.getBoundary().getCoordinates()[j].y;
            double z = mesh.getPolygonWithHeight().get(0).geo.getBoundary().getCoordinates()[j].z;
            fileWriter.write(String.valueOf(x) + " " + String.valueOf(y) + " " + String.valueOf(z) + "\n");
            x = mesh.getPolygonWithHeight().get(0).geo.getBoundary().getCoordinates()[j].x;
            y = mesh.getPolygonWithHeight().get(0).geo.getBoundary().getCoordinates()[j].y;
            z = mesh.getPolygonWithHeight().get(0).getHeight();
            fileWriter.write(String.valueOf(x) + " " + String.valueOf(y) + " " + String.valueOf(z) + "\n");
        }

        fileWriter.write("LINES 1\n");
        fileWriter.write(String.valueOf(propDataOut.propagationPaths.get(0).getPointList().size()));
        int i = 0;
        for (PropagationPath.PointPath p : propDataOut.propagationPaths.get(0).getPointList()) {
            fileWriter.write(" " + String.valueOf(i));
            i++;
        }
        fileWriter.write("\n");

        fileWriter.write("POLYGONS 1 " + String.valueOf(2 * lengthPolygon + 1) + "\n");

        fileWriter.write(String.valueOf(2 * lengthPolygon));
        for (int j = 0; j < 2 * lengthPolygon; j++) {
            fileWriter.write(" " + String.valueOf(j + i));
        }
        fileWriter.write("\n");

        fileWriter.close();
    }

    private static void addGeometry(List<Geometry> geom, Geometry polygon) {
        if (polygon instanceof Polygon) {
            geom.add((Polygon) polygon);
        } else {
            for (int i = 0; i < polygon.getNumGeometries(); i++) {
                addGeometry(geom, polygon.getGeometryN(i));
            }
        }

    }

    private void writePLY(String filename, MeshBuilder mesh) throws IOException, LayerDelaunayError {
        PointsMerge pointsMerge = new PointsMerge(0.01);
        List<Geometry> triVertices2 = new ArrayList<>();
        Map<String,Integer> vertices2 = new HashMap<>();
        List<Coordinate> vertices3 = new ArrayList<>();
        GeometryFactory geometryFactory = new GeometryFactory();
        int k=0;
        for (MeshBuilder.PolygonWithHeight polygon : mesh.getPolygonWithHeight()) {
            double sumBuildingHeight=0;
            double minimumHeight = Double.MAX_VALUE;
            int count=0;
            for (Coordinate coordinate : polygon.getGeometry().getCoordinates()) {
                sumBuildingHeight += coordinate.z;
                minimumHeight = Math.min(minimumHeight, coordinate.z);
                count++;
            }
            double averageBuildingHeight = sumBuildingHeight / count;
            SetCoordinateSequenceFilter absoluteCoordinateSequenceFilter = new SetCoordinateSequenceFilter(minimumHeight);
            Polygon base = (Polygon) polygon.getGeometry().copy();
            base.apply(absoluteCoordinateSequenceFilter);
            GeometryCollection buildingExtruded = GeometryExtrude.extrudePolygonAsGeometry(base, polygon.getHeight() + (averageBuildingHeight - minimumHeight));
            addGeometry(triVertices2, buildingExtruded);
            for (Coordinate coordinate : buildingExtruded.getCoordinates()) {
                vertices2.put(coordinate.toString(),k);
                vertices3.add(coordinate);
                k++;
            }

        }
        int vertexCountG = mesh.getVertices().size();
        int vertexCountB = vertices3.size();
        int faceCountG = mesh.getTriangles().size();
        int faceCountB = triVertices2.size();
        int vertexCount = vertexCountG + vertexCountB;
        int faceCount = faceCountG + faceCountB;
        FileWriter fileWriter = new FileWriter(filename);
        fileWriter.write("ply\n");
        fileWriter.write("format ascii 1.0\n");
        fileWriter.write("element vertex " + vertexCount + "\n");
        fileWriter.write("property float x\n");
        fileWriter.write("property float y\n");
        fileWriter.write("property float z\n");
        fileWriter.write("property uchar green\n");
        fileWriter.write("property uchar red\n");
        fileWriter.write("property uchar blue\n");
        fileWriter.write("element face " + faceCount + "\n");
        fileWriter.write("property list uchar int vertex_index\n");
        fileWriter.write("end_header\n");

        for (int i = 0; i < vertexCountG; i++) {
            fileWriter.write(mesh.getVertices().get(i).x + " " + mesh.getVertices().get(i).y + " " + (mesh.getVertices().get(i).z) + " " + "255 0 0\n");
        }
        // Iterating over values only
        for (Coordinate vertice : vertices3) {
            //System.out.println("Value = " + value);
            fileWriter.write(vertice.x + " " + vertice.y + " " + (vertice.z) + " " + "0 0 255\n");
        }

        for (int i = 0; i < faceCountG; i++) {
            fileWriter.write("3 " + mesh.getTriangles().get(i).getA() + " " + mesh.getTriangles().get(i).getB() + " " + (mesh.getTriangles().get(i).getC()) + "\n");
        }
        for (int i=0;i<faceCountB;i++){
            Coordinate[] coordinates = triVertices2.get(i).getCoordinates();
            fileWriter.write(coordinates.length + " " );
            for (int j=0;j<coordinates.length;j++){
              fileWriter.write((vertexCountG+ vertices2.get(coordinates[j].toString()))+" ");
            }
            fileWriter.write("\n" );
        }
        fileWriter.close();
    }

    /**
     * Test vertical edge diffraction ray computation
     * @throws LayerDelaunayError
     * @throws ParseException
     */
    @Test
    public void TestcomputeVerticalEdgeDiffraction() throws LayerDelaunayError, ParseException {
        GeometryFactory factory = new GeometryFactory();
        WKTReader wktReader = new WKTReader(factory);
        List<Geometry> srclst = new ArrayList<Geometry>();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(0, 0, 0.), new Coordinate(20, 15, 0.));
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();
        mesh.addGeometry(wktReader.read("POLYGON((5 6, 6 5, 7 5, 7 8, 6 8, 5 7, 5 6))"), 4);
        mesh.addGeometry(wktReader.read("POLYGON((9 7, 11 7, 11 11, 9 11, 9 7))"), 4);
        mesh.addGeometry(wktReader.read("POLYGON((12 8, 13 8, 13 10, 12 10, 12 8))"), 4);
        mesh.addGeometry(wktReader.read("POLYGON((10 4, 11 4, 11 6, 10 6, 10 4))"), 4);
        mesh.finishPolygonFeeding(cellEnvelope);
        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(), mesh.getTriNeighbors(), mesh.getVertices());

        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        PropagationProcessData processData = new PropagationProcessData(new ArrayList<>(), manager, sourcesIndex, srclst, new ArrayList<>(), new ArrayList<>(), 0, 99, 1000,1000,0,0,new double[0],0,0,new EmptyProgressVisitor(), new ArrayList<>(), true);
        ComputeRays computeRays = new ComputeRays(processData, new ComputeRaysOut());
        Coordinate p1 = new Coordinate(2, 6.5, 1.6);
        Coordinate p2 = new Coordinate(14, 6.5, 1.6);

        List<Coordinate> ray = computeRays.computeSideHull(true,p1, p2);
        int i = 0;
        assertEquals(0, p1.distance(ray.get(i++)),0.02);
        assertEquals(0, new Coordinate(9, 11).distance(ray.get(i++)),0.02);
        assertEquals(0, new Coordinate(11, 11).distance(ray.get(i++)),0.02);
        assertEquals(0, new Coordinate(13, 10).distance(ray.get(i++)),0.02);
        assertEquals(0, p2.distance(ray.get(i++)),0.02);

        ray = computeRays.computeSideHull(false,p1, p2);
        i = 0;
        assertEquals(0, p1.distance(ray.get(i++)),0.02);
        assertEquals(0, new Coordinate(6, 5).distance(ray.get(i++)),0.02);
        assertEquals(0, new Coordinate(10, 4).distance(ray.get(i++)),0.02);
        assertEquals(0, new Coordinate(11, 4).distance(ray.get(i++)),0.02);
        assertEquals(0, p2.distance(ray.get(i++)),0.02);

        ray = computeRays.computeSideHull(false,p2, p1);
        i = 0;
        assertEquals(0, p2.distance(ray.get(i++)),0.02);
        assertEquals(0, new Coordinate(13, 10).distance(ray.get(i++)),0.02);
        assertEquals(0, new Coordinate(11, 11).distance(ray.get(i++)),0.02);
        assertEquals(0, new Coordinate(9, 11).distance(ray.get(i++)),0.02);
        assertEquals(0, p1.distance(ray.get(i++)),0.02);

        ray = computeRays.computeSideHull(true,p2, p1);
        i = 0;
        assertEquals(0, p2.distance(ray.get(i++)),0.02);
        assertEquals(0, new Coordinate(11, 4).distance(ray.get(i++)),0.02);
        assertEquals(0, new Coordinate(10, 4).distance(ray.get(i++)),0.02);
        assertEquals(0, new Coordinate(6, 5).distance(ray.get(i++)),0.02);
        assertEquals(0, p1.distance(ray.get(i++)),0.02);


        //LOGGER.info(factory.createLineString(ray.toArray(new Coordinate[ray.size()])).toString());
    }


    /**
     * Test vertical edge diffraction ray computation
     * @throws LayerDelaunayError
     * @throws ParseException
     */
    @Test
    public void TestcomputeVerticalEdgeDiffractionRayOverBuilding() throws LayerDelaunayError, ParseException {
        GeometryFactory factory = new GeometryFactory();
        WKTReader wktReader = new WKTReader(factory);
        List<Geometry> srclst = new ArrayList<Geometry>();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(0, 0, 0.), new Coordinate(20, 15, 0.));
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();
        mesh.addGeometry(wktReader.read("POLYGON((5 5, 7 5, 7 6, 8 6, 8 8, 5 8, 5 5))"), 4.3);
        mesh.addGeometry(wktReader.read("POLYGON((9 7, 10 7, 10 9, 9 9, 9 7))"), 4.3);
        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(), mesh.getTriNeighbors(), mesh.getVertices());


        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        PropagationProcessData processData = new PropagationProcessData(new ArrayList<>(), manager, sourcesIndex, srclst, new ArrayList<>(), new ArrayList<>(), 0, 99, 1000,1000,0,0,new double[0],0,0,new EmptyProgressVisitor(), new ArrayList<>(), true);
        ComputeRays computeRays = new ComputeRays(processData, new ComputeRaysOut());
        Coordinate p1 = new Coordinate(4, 3, 3);
        Coordinate p2 = new Coordinate(13, 10, 6.7);

        assertFalse(manager.isFreeField(p1, p2));

        // Check the computation of convex corners of a building
        List<Coordinate> b1OffsetRoof = manager.getWideAnglePointsByBuilding(1,Math.PI * (1 + 1 / 16.0), Math.PI * (2 - (1 / 16.)));
        int i = 0;
        assertEquals(0, new Coordinate(5,5).distance(b1OffsetRoof.get(i++)),2*FastObstructionTest.wideAngleTranslationEpsilon);
        assertEquals(0, new Coordinate(7,5).distance(b1OffsetRoof.get(i++)),2*FastObstructionTest.wideAngleTranslationEpsilon);
        assertEquals(0, new Coordinate(8,6).distance(b1OffsetRoof.get(i++)),2*FastObstructionTest.wideAngleTranslationEpsilon);
        assertEquals(0, new Coordinate(8,8).distance(b1OffsetRoof.get(i++)),2*FastObstructionTest.wideAngleTranslationEpsilon);
        assertEquals(0, new Coordinate(5,8).distance(b1OffsetRoof.get(i++)),2*FastObstructionTest.wideAngleTranslationEpsilon);
        assertEquals(0, new Coordinate(5,5).distance(b1OffsetRoof.get(i++)),2*FastObstructionTest.wideAngleTranslationEpsilon);


        List<Coordinate> ray = computeRays.computeSideHull(true,p1, p2);
        i = 0;
        assertEquals(0, p1.distance(ray.get(i++)),0.02);
        assertEquals(0, new Coordinate(5, 8).distance(ray.get(i++)),0.02);
        assertEquals(0, p2.distance(ray.get(i++)),0.02);


        ray = computeRays.computeSideHull(false,p1, p2);
        i = 0;
        assertEquals(0, p1.distance(ray.get(i++)),0.02);
        assertEquals(0, new Coordinate(7, 5).distance(ray.get(i++)),0.02);
        assertEquals(0, p2.distance(ray.get(i++)),0.02);


        ray = computeRays.computeSideHull(false,p2, p1);
        i = 0;
        assertEquals(0, p2.distance(ray.get(i++)),0.02);
        assertEquals(0, new Coordinate(5, 8).distance(ray.get(i++)),0.02);
        assertEquals(0, p1.distance(ray.get(i++)),0.02);

        ray = computeRays.computeSideHull(true,p2, p1);
        i = 0;
        assertEquals(0, p2.distance(ray.get(i++)),0.02);
        assertEquals(0, new Coordinate(7, 5).distance(ray.get(i++)),0.02);
        assertEquals(0, p1.distance(ray.get(i++)),0.02);
    }

    /**
     * Test vertical edge diffraction ray computation with receiver in concave building
     * This configuration is not supported currently, so it must return no rays.
     * @throws LayerDelaunayError
     * @throws ParseException
     */
    @Test
    public void TestConcaveVerticalEdgeDiffraction() throws LayerDelaunayError, ParseException {
        GeometryFactory factory = new GeometryFactory();
        WKTReader wktReader = new WKTReader(factory);
        List<Geometry> srclst = new ArrayList<Geometry>();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(0, 0, 0.), new Coordinate(20, 15, 0.));
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();
        mesh.addGeometry(wktReader.read("POLYGON((5 6, 4 5, 7 5, 7 8, 4 8, 5 7, 5 6))"), 4);
        mesh.addGeometry(wktReader.read("POLYGON((9 7, 11 7, 11 11, 9 11, 9 7))"), 4);
        mesh.addGeometry(wktReader.read("POLYGON((12 8, 13 8, 13 10, 12 10, 12 8))"), 4);
        mesh.addGeometry(wktReader.read("POLYGON((10 4, 11 4, 11 6, 10 6, 10 4))"), 4);
        mesh.finishPolygonFeeding(cellEnvelope);
        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(), mesh.getTriNeighbors(), mesh.getVertices());

        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        PropagationProcessData processData = new PropagationProcessData(new ArrayList<>(), manager, sourcesIndex, srclst, new ArrayList<>(), new ArrayList<>(), 0, 99, 1000,1000,0,0,new double[0],0,0,new EmptyProgressVisitor(), new ArrayList<>(), true);
        ComputeRays computeRays = new ComputeRays(processData, new ComputeRaysOut());
        Coordinate p1 = new Coordinate(4.5, 6.5, 1.6);
        Coordinate p2 = new Coordinate(14, 6.5, 1.6);

        List<Coordinate> ray = computeRays.computeSideHull(true,p1, p2);
        assertTrue(ray.isEmpty());
        ray = computeRays.computeSideHull(false,p1, p2);
        assertTrue(ray.isEmpty());
        ray = computeRays.computeSideHull(false,p2, p1);
        assertTrue(ray.isEmpty());
        ray = computeRays.computeSideHull(true,p2, p1);
        assertTrue(ray.isEmpty());
    }
    //@Test
    public void benchmarkComputeVerticalEdgeDiffraction() throws LayerDelaunayError, ParseException {
        Coordinate[] buildingShell = new Coordinate[]{
                new Coordinate(1,1),
                new Coordinate(2,0),
                new Coordinate(1,-1),
                new Coordinate(-1,-1),
                new Coordinate(-2,0),
                new Coordinate(-1,1),
                new Coordinate(1,1)};
        int nbCols = 20;
        int nbRows = 20;
        int xSpace = 4;
        int ySpace = 4;
        int yOffset = 2;
        // Generate buildings procedurally
        GeometryFactory factory = new GeometryFactory();
        Polygon building = factory.createPolygon(buildingShell);
        Envelope envelope = new Envelope(building.getEnvelopeInternal());
        MeshBuilder mesh = new MeshBuilder();
        for(int xStep = 0; xStep < nbCols; xStep++) {
            for(int yStep=0; yStep < nbRows; yStep++) {
                int offset = xStep % 2 == 0 ? 0 : yOffset;
                Geometry translatedGeom = AffineTransformation.translationInstance(xStep * xSpace, yStep * ySpace + offset).transform(building);
                mesh.addGeometry(translatedGeom, 4);
                envelope.expandToInclude(translatedGeom.getEnvelopeInternal());
            }
        }
        envelope.expandBy(10);
        mesh.finishPolygonFeeding(envelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(), mesh.getTriNeighbors(), mesh.getVertices());

        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        PropagationProcessData processData = new PropagationProcessData(new ArrayList<>(), manager, sourcesIndex, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 0, 99, 1000,1000,0,0,new double[0],0,0,new EmptyProgressVisitor(), new ArrayList<>(), true);
        ComputeRays computeRays = new ComputeRays(processData, new ComputeRaysOut());

        Vector2D pRef = new Vector2D(1,2);
        Random r = new Random(0);
        int nbHull = 1200;
        // Warmup
        for(int i=0; i < 10; i++) {
            int xStep = r.nextInt(nbCols);
            int offset = xStep % 2 == 0 ? 0 : yOffset;
            Coordinate p1 = pRef.translate(new Coordinate(xStep*xSpace,r.nextInt(nbRows)*ySpace + offset));
            xStep = r.nextInt(nbCols);
            offset = xStep % 2 == 0 ? 0 : yOffset;
            Coordinate p2 = pRef.translate(new Coordinate(xStep*xSpace,r.nextInt(nbRows)*ySpace + offset));
            p1.setOrdinate(2, 1.6);
            p2.setOrdinate(2, 1.6);

            List<Coordinate> h1 = computeRays.computeSideHull(true,p1, p2);
            List<Coordinate> h2 = computeRays.computeSideHull(false,p1, p2);

        }
        long start = System.currentTimeMillis();
        for(int i=0; i < nbHull; i++) {
            int xStep = r.nextInt(nbCols);
            int offset = xStep % 2 == 0 ? 0 : yOffset;
            Coordinate p1 = pRef.translate(new Coordinate(xStep*xSpace,r.nextInt(nbRows)*ySpace + offset));
            xStep = r.nextInt(nbCols);
            offset = xStep % 2 == 0 ? 0 : yOffset;
            Coordinate p2 = pRef.translate(new Coordinate(xStep*xSpace,r.nextInt(nbRows)*ySpace + offset));
            p1.setOrdinate(2, 1.6);
            p2.setOrdinate(2, 1.6);

            List<Coordinate> h1 = computeRays.computeSideHull(true,p1, p2);
            List<Coordinate> h2 = computeRays.computeSideHull(false,p1, p2);

        }
        long timeLen = System.currentTimeMillis() - start;
        LOGGER.info(String.format("Benchmark done in %d millis. %d millis by hull", timeLen, timeLen / nbHull));
    }

    public Plane ComputeZeroRadPlane(Coordinate p0, Coordinate p1) {
        Vector3D s = new Vector3D(p0.x, p0.y, p0.z);
        Vector3D r = new Vector3D(p1.x, p1.y, p1.z);
        double angle = Math.atan2(p1.y - p0.y, p1.x - p0.x);
        // Compute rPrime, the third point of the plane that is at -PI/2 with SR vector
        Vector3D rPrime = s.add(new Vector3D(Math.cos(angle - Math.PI / 2),Math.sin(angle - Math.PI / 2),0));
        Plane p = new Plane(r, s, rPrime, 1e-6);
        // Normal of the cut plane should be upward
        if(p.getNormal().getZ() < 0) {
            p.revertSelf();
        }
        return p;
    }

    public Vector3D transform(Plane plane, Vector3D p) {
        org.apache.commons.math3.geometry.euclidean.twod.Vector2D sp = plane.toSubSpace(p);
        return new Vector3D(sp.getX(), sp.getY(), plane.getOffset(p));
    }

    @Test
    public void testVolumePlaneIntersection() {
        Vector3D[] buildingA = new Vector3D[]{new Vector3D(5, 5, 4),
                new Vector3D(7, 5, 4),
                new Vector3D(7, 6, 4),
                new Vector3D(8, 6, 4),
                new Vector3D(8, 8, 4),
                new Vector3D(5, 8, 4),
                new Vector3D(5, 5, 4)};

        Vector3D[] buildingB = new Vector3D[]{new Vector3D(9, 7, 4),
                new Vector3D(9, 7, 4),
                new Vector3D(10, 7, 4),
                new Vector3D(10, 9, 4),
                new Vector3D(9, 9, 4),
                new Vector3D(9, 7, 4)};

        Coordinate s = new Coordinate(7.5, 5.5, 3);

        Coordinate r = new Coordinate(6.5,8.5,10);

        Plane plane = ComputeZeroRadPlane(r, s);
        //LOGGER.info(String.format("plane %s", plane.getNormal()));

        GeometryFactory geometryFactory = new GeometryFactory();
        WKTWriter wktWriter = new WKTWriter(3);
        Coordinate[] projPoly = new Coordinate[buildingA.length];
        List<Coordinate> polyCut = new ArrayList<>();
        Vector3D lastV = null;
        for(int idp = 0; idp < buildingA.length; idp++) {
            Vector3D v0 = transform(plane, buildingA[idp]);
            projPoly[idp] = new Coordinate(v0.getX(), v0.getY(), v0.getZ());
            if(v0.getZ() >= 0) {
                if(lastV != null && lastV.getZ() < 0) {
                    // Interpolate vector
                    Vector3D i = plane.intersection(new Line(buildingA[idp-1],buildingA[idp],FastObstructionTest.epsilon));
                    Vector3D ip = transform(plane, i);
                    polyCut.add(new Coordinate(ip.getX(), ip.getY(), 0));
                }
                Vector3D i = plane.intersection(new Line(new Vector3D(buildingA[idp].getX(),buildingA[idp].getY(),Double.MIN_VALUE),buildingA[idp],FastObstructionTest.epsilon));
                org.apache.commons.math3.geometry.euclidean.twod.Vector2D iCut = plane.toSubSpace(i);
                polyCut.add(new Coordinate(iCut.getX(), iCut.getY(), 0));
            } else if (lastV != null && lastV.getZ() >= 0) {
                // Interpolate vector
                Vector3D i = plane.intersection(new Line(buildingA[idp-1],buildingA[idp],FastObstructionTest.epsilon));
                Vector3D ip = transform(plane, i);
                polyCut.add(new Coordinate(ip.getX(), ip.getY(), 0));
            }
            lastV = v0;
        }
        //projPoly[buildingA.length - 1] = projPoly[0];
        Polygon poly = geometryFactory.createPolygon(projPoly);
        // Reproj to domain
        for(int i=0; i < polyCut.size(); i++) {
            Vector3D pointOnPlane = plane.toSpace(new org.apache.commons.math3.geometry.euclidean.twod.Vector2D(polyCut.get(i).x, polyCut.get(i).y));
            //pointOnPlane = pointOnPlane.add(plane.getNormal().scalarMultiply(polyCut.get(i).z));
            polyCut.set(i, new Coordinate(pointOnPlane.getX(), pointOnPlane.getY(), pointOnPlane.getZ()));
        }
        if(!polyCut.get(polyCut.size() - 1).equals(polyCut.get(0))) {
            polyCut.add(polyCut.get(0));
        }
        Polygon poly2 = geometryFactory.createPolygon(polyCut.toArray(new Coordinate[polyCut.size()]));
        //LOGGER.info(String.format("Building \n%s", wktWriter.write(poly)));
        //LOGGER.info(String.format("Building cut \n%s", wktWriter.write(poly2)));

    }

    private void writeVTK(String filename, ComputeRaysOut propDataOut) throws IOException {


        FileWriter fileWriter = new FileWriter(filename);
        fileWriter.write("# vtk DataFile Version 2.0\n");
        fileWriter.write("PropagationPath\n");
        fileWriter.write("ASCII\n");
        fileWriter.write("DATASET POLYDATA\n");
        int nbPoints = 0;
        for (int j = 0; j < propDataOut.propagationPaths.size(); j++) {
            nbPoints = nbPoints + propDataOut.propagationPaths.get(j).getPointList().size();
        }
        fileWriter.write("\n");
        fileWriter.write("POINTS " + String.valueOf(nbPoints) + " float\n");

        GeometryFactory geometryFactory = new GeometryFactory();
        List<Coordinate> coordinates = new ArrayList<>();
        for (int j = 0; j < propDataOut.propagationPaths.size(); j++) {
            for (PropagationPath.PointPath p : propDataOut.propagationPaths.get(j).getPointList()) {
                coordinates.add(p.coordinate);
                fileWriter.write(String.valueOf(p.coordinate.x) + " " + String.valueOf(p.coordinate.y) + " " + String.valueOf(p.coordinate.z) + "\n");
            }
        }
        LineString factoryLineString = geometryFactory.createLineString(coordinates.toArray(new Coordinate[coordinates.size()]));
        WKTWriter wktWriter = new WKTWriter(3);

        fileWriter.write("\n");
        fileWriter.write("LINES " + String.valueOf(propDataOut.propagationPaths.size()) + " " + String.valueOf(nbPoints + propDataOut.propagationPaths.size()) + "\n");
        int i = 0;
        for (int j = 0; j < propDataOut.propagationPaths.size(); j++) {
            fileWriter.write(String.valueOf(propDataOut.propagationPaths.get(j).getPointList().size()));

            for (PropagationPath.PointPath p : propDataOut.propagationPaths.get(j).getPointList()) {
                fileWriter.write(" " + String.valueOf(i));
                i++;
            }
            fileWriter.write("\n");
        }


        fileWriter.close();
    }


    private static ArrayList<Double> asW(double... dbValues) {
        ArrayList<Double> ret = new ArrayList<>(dbValues.length);
        for (double db_m : dbValues) {
            ret.add(PropagationProcess.dbaToW(db_m));
        }
        return ret;
    }

    /**
     * Test Direct Field
     */
    @Test
    public void DirectRay() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(0, 0, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-250, 250, -250, 50)), 0.));

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();
        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 0, 0, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(200, 0, 4),0, energeticSum, debug);


        /*PropagationProcessPathData propData = new PropagationProcessPathData();
        propData.setTemperature(15);
        propData.setHumidity(70);
        EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
        splCompare(evaluateAttenuationCnossos.evaluate(propDataOut.propagationPaths.get(0), propData), "Test T01", new double[]{-54, -54.1, -54.2, -54.5, -54.8, -55.8, -59.3, -73.0}, ERROR_EPSILON_TEST_T);
*/
        String filename = "target/test.vtk";
        try {
            writeVTK(filename, propDataOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Test TC05 -- Reduced receiver height to include diffraction in some frequency bands
     */
    @Test
    public void TC05() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -20, 80)), 0.9));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -20, 80)), 0.5));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -20, 80)), 0.2));

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();
        // Add topographic points
        //x1
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 0, 0, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 14), 0,energeticSum, debug);

        String filename = "target/T05.vtk";
        String filename2 = "target/T05.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * Test TC06 -- Reduced receiver height to include diffraction in some frequency bands
     * This test
     */

    public void TC06() throws LayerDelaunayError {
        // TODO Rayleigh stuff

        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 500, -20, 80)), 0.9));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -20, 80)), 0.5));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -20, 80)), 0.2));

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();
        // Add topographic points
        //x1
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 0, 0, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();
        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 11.5), 0,energeticSum, debug);
        assertEquals(true, false);
    }


    /**
     * Test TC07 -- Flat ground with spatially varying acoustic properties and long barrier
     */
    @Test
    public void TC07() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(100, 240, 0),
                new Coordinate(100.1, 240, 0),
                new Coordinate(265.1, -180, 0),
                new Coordinate(265, -180, 0),
                new Coordinate(100, 240, 0)}), 6);

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 1, 0, 250, 250, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 4), 0,energeticSum, debug);

        String filename = "target/T07.vtk";
        String filename2 = "target/T07.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Test TC08 -- Flat ground with spatially varying acoustic properties and short barrier
     */
    @Test
    public void TC08() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(175, 50, 0),
                new Coordinate(175.01, 50, 0),
                new Coordinate(190.01, 10, 0),
                new Coordinate(190, 10, 0),
                new Coordinate(175, 50, 0)}), 6);

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 1, 1, 300, 300, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 4), 0,energeticSum, debug);

        String filename = "target/T08.vtk";
        String filename2 = "target/T08.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Test TC09 -- Ground with spatially varying heights and and acoustic properties and short
     * barrier
     */

    public void TC09() throws LayerDelaunayError {
        // Impossible shape for NoiseModelling
        assertEquals(true, false);
    }

    /**
     * Test TC10 -- Flat ground with homogeneous acoustic properties and cubic building – receiver
     * at low height
     */

    public void TC10() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(50, 10, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(55, 5, 0),
                new Coordinate(65, 5, 0),
                new Coordinate(65, 15, 0),
                new Coordinate(55, 15, 0),
                new Coordinate(55, 5, 0)}), 10);

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 1, 5, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(70, 10, 4), 0,energeticSum, debug);
        String filename = "target/T09.vtk";
        String filename2 = "target/T09.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Test TC11 -- Flat ground with homogeneous acoustic properties and cubic building – receiver
     * at large height
     */
    @Test
    public void TC11() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(50, 10, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(55, 5,0),
                new Coordinate(65, 5,0),
                new Coordinate(65, 15,0),
                new Coordinate(55, 15,0),
                new Coordinate(55, 5,0)}), 10);

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 1, 5, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(70, 10, 15), 0,energeticSum, debug);
        String filename = "target/T11.vtk";
        String filename2 = "target/T11.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Test TC12 -- Flat ground with homogeneous acoustic properties and polygonal building –
     * receiver at low height
     */
    @Test
    public void TC12() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(0, 10, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(11., 15.5, 0),
                new Coordinate(12., 13, 0),
                new Coordinate(14.5, 12, 0),
                new Coordinate(17.0, 13, 0),
                new Coordinate(18.0, 15.5, 0),
                new Coordinate(17.0, 18, 0),
                new Coordinate(14.5, 19, 0),
                new Coordinate(12.0, 18, 0),
                new Coordinate(11, 15.5, 0)}), 10);

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 1, 5, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(30, 20, 6), 0,energeticSum, debug);
        String filename = "target/T12.vtk";
        String filename2 = "target/T12.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * Test TC13 -- Ground with spatially varying heights and acoustic properties and polygonal
     * building
     */
    @Test
    public void TC13() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(169.4, 41.0, 0),
                new Coordinate(172.5, 33.5, 0),
                new Coordinate(180.0, 30.4, 0),
                new Coordinate(187.5, 33.5, 0),
                new Coordinate(190.6, 41.0, 0),
                new Coordinate(187.5, 48.5, 0),
                new Coordinate(180.0, 51.6, 0),
                new Coordinate(172.5, 48.5, 0),
                new Coordinate(169.4, 41.0, 0)}), 30);

        //x1
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 1, 5, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 28.5), 0,energeticSum, debug);
        String filename = "target/T13.vtk";
        String filename2 = "target/T13.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Test TC14 -- Flat ground with homogeneous acoustic properties and polygonal building –
     * receiver at large height
     */
    @Test
    public void TC14() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(8, 10, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-300, 300, -300, 300)), 0.2));

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(11., 15.5, 0),
                new Coordinate(12., 13, 0),
                new Coordinate(14.5, 12, 0),
                new Coordinate(17.0, 13, 0),
                new Coordinate(18.0, 15.5, 0),
                new Coordinate(17.0, 18, 0),
                new Coordinate(14.5, 19, 0),
                new Coordinate(12.0, 18, 0),
                new Coordinate(11, 15.5, 0)}), 10);

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 1, 5, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(25, 20, 23), 0,energeticSum, debug);
        String filename = "target/T14.vtk";
        String filename2 = "target/T14.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Test TC15 -- Flat ground with homogeneous acoustic properties and four buildings
     */
    @Test
    public void TC15() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(50, 10, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-250, 250, -250, 250)), 0.5));

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(55.0, 5.0, 0),
                new Coordinate(65.0, 5.0, 0),
                new Coordinate(65.0, 15.0, 0),
                new Coordinate(55.0, 15.0, 0),
                new Coordinate(55.0, 5.0, 0)}), 8);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(70, 14.5, 0),
                new Coordinate(80.0, 10.2, 0),
                new Coordinate(80.0, 20.2, 0),
                new Coordinate(70, 14.5, 0)}), 12);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(90.1, 19.5, 0),
                new Coordinate(93.3, 17.8, 0),
                new Coordinate(87.3, 6.6, 0),
                new Coordinate(84.1, 8.3, 0),
                new Coordinate(90.1, 19.5, 0)}), 10);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(94.9, 14.1, 0),
                new Coordinate(98.02, 12.37, 0),
                new Coordinate(92.03, 1.2, 0),
                new Coordinate(88.86, 2.9, 0),
                new Coordinate(94.9, 14.1, 0)}), 10);

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 1, 5, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(100, 15, 5), 0,energeticSum, debug);
        String filename = "target/T15.vtk";
        String filename2 = "target/T15.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Reflecting barrier on ground with spatially varying heights and acoustic properties
     */
    @Test
    public void TC16() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();
        // Add topographic points
        //x1
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(114, 52, 0),
                new Coordinate(170, 60, 0),
                new Coordinate(170, 62, 0),
                new Coordinate(114, 54, 0),
                new Coordinate(114, 52, 0)}), 15);

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 1, 0, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 14), 0,energeticSum, debug);

        String filename = "target/T16.vtk";
        String filename2 = "target/T16.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * Reflecting two barrier on ground with spatially varying heights and acoustic properties
     */
    @Test
    public void TC16b() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(114, 52, 0),
                new Coordinate(170, 60, 0),
                new Coordinate(170, 62, 0),
                new Coordinate(114, 54, 0),
                new Coordinate(114, 52, 0)}), 20);

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(114, 12, 0),
                new Coordinate(170, 30, 0),
                new Coordinate(170, 32, 0),
                new Coordinate(114, 14, 0),
                new Coordinate(114, 12, 0)}), 20);

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 2, 0, 1000, 1000, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 15), 0,energeticSum, debug);

        String filename = "target/T16b.vtk";
        String filename2 = "target/T16b.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * TC17 - Reflecting barrier on ground with spatially varying heights and acoustic properties
     * reduced receiver height
     */

    public void TC17() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();
        // Add topographic points
        //x1
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(114, 52, 0),
                new Coordinate(170, 60, 0),
                new Coordinate(170, 62, 0),
                new Coordinate(114, 54, 0),
                new Coordinate(114, 52, 0)}), 15);

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 1, 0, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 11.5), 0,energeticSum, debug);

        String filename = "target/T17.vtk";
        String filename2 = "target/T17.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertEquals(true, false); // because rayleigh distance
    }

    /**
     * TC18 - Screening and reflecting barrier on ground with spatially varying heights and
     * acoustic properties
     */

    public void TC18() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();
        // Add topographic points
        //x1
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(114, 52),
                new Coordinate(170, 60),
                new Coordinate(170, 61),
                new Coordinate(114, 53),
                new Coordinate(114, 52)}), 15);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(87, 50),
                new Coordinate(92, 32),
                new Coordinate(92, 33),
                new Coordinate(87, 51),
                new Coordinate(87, 50)}), 12);

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        String filename2 = "target/T18.ply";
        try {
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 1, 4, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 12), 0,energeticSum, debug);

        String filename = "target/T18.vtk";

        try {
            writeVTK(filename, propDataOut);
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertEquals(true, true);
    }


    /**
     * TC18b - Screening and reflecting barrier on ground with spatially varying heights and
     * acoustic properties
     */
    @Test
    public void TC18b() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();
        // Add topographic points
        //x1
        mesh.addTopographicPoint(new Coordinate(0, 80, 2));
        mesh.addTopographicPoint(new Coordinate(225, 80, 2));
        mesh.addTopographicPoint(new Coordinate(225, -20, 2));
        mesh.addTopographicPoint(new Coordinate(0, -20, 2));
        mesh.addTopographicPoint(new Coordinate(120, -20, 2));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        mesh.addTopographicPoint(new Coordinate(225, 80, 2));
        mesh.addTopographicPoint(new Coordinate(225, -20, 2));
        mesh.addTopographicPoint(new Coordinate(0, -20, 2));
        mesh.addTopographicPoint(new Coordinate(0, 80, 2));
        mesh.addTopographicPoint(new Coordinate(120, 80, 2));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(114, 52),
                new Coordinate(170, 60),
                new Coordinate(170, 61),
                new Coordinate(114, 53),
                new Coordinate(114, 52)}), 15);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(87, 50),
                new Coordinate(92, 32),
                new Coordinate(92, 33),
                new Coordinate(87, 51),
                new Coordinate(87, 50)}), 12);

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        String filename2 = "target/T18b.ply";
        try {
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(new ArrayList<>(), manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 1, 10, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        computeRays.makeRelativeZToAbsolute();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();


        computeRays.computeRaysAtPosition(new Coordinate(200 ,50 ,12 + manager.getHeightAtPosition(new Coordinate(200, 50, 12))), 0,energeticSum, debug);

        String filename = "target/T18b.vtk";

        try {
            writeVTK(filename, propDataOut);
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertEquals(true, true);
    }

    /**
     * TC19 - Complex object and 2 barriers on ground with spatially varying heights and
     * acoustic properties
     */
    @Test
    public void TC19() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();
        // Add topographic points
        //x1
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(156, 28),
                new Coordinate(145, 7),
                new Coordinate(145, 8),
                new Coordinate(156, 29),
                new Coordinate(156, 28)}), 14);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(175, 35),
                new Coordinate(188, 19),
                new Coordinate(188, 20),
                new Coordinate(175, 36),
                new Coordinate(175, 35)}), 14.5);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(100, 24),
                new Coordinate(118, 24),
                new Coordinate(118, 30),
                new Coordinate(100, 30),
                new Coordinate(100, 24)}), 12);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(100, 15.1),
                new Coordinate(118, 15.1),
                new Coordinate(118, 23.9),
                new Coordinate(100, 23.9),
                new Coordinate(100, 15.1)}), 7);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(100, 9),
                new Coordinate(118, 9),
                new Coordinate(118, 15),
                new Coordinate(100, 15),
                new Coordinate(100, 9)}), 12);

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        String filename2 = "target/T19.ply";
        try {
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 1, 1, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(200, 30, 14), 0,energeticSum, debug);



        String filename = "target/T19.vtk";

        try {
            writeVTK(filename, propDataOut);
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertEquals(true, true);
    }

    /**
     * TC20 - Ground with spatially varying heights and acoustic properties
     */

    public void TC20() throws LayerDelaunayError {
        //Tables 221 – 222 are not shown in this draft.

        assertEquals(false, true);
    }

    /**
     * TC21 - Building on ground with spatially varying heights and acoustic properties
     */

    public void TC21() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(167.2, 39.5),
                new Coordinate(151.6, 48.5),
                new Coordinate(141.1, 30.3),
                new Coordinate(156.7, 21.3),
                new Coordinate(159.7, 26.5),
                new Coordinate(151.0, 31.5),
                new Coordinate(155.5, 39.3),
                new Coordinate(164.2, 34.3),
                new Coordinate(167.2, 39.5)}), 11.5);

        //x1
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        String filename2 = "target/T21.ply";
        try {

            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 1, 5, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(187.05, 25, 14), 0,energeticSum, debug);



        String filename = "target/T21.vtk";
        try {
            writeVTK(filename, propDataOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertEquals(true, false);
    }


    /**
     * TC22 - Building with receiver backside on ground with spatially varying heights and
     * acoustic properties
     */

    public void TC22() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(197, 36.0, 0),
                new Coordinate(179, 36, 0),
                new Coordinate(179, 15, 0),
                new Coordinate(197, 15, 0),
                new Coordinate(197, 21, 0),
                new Coordinate(187, 21, 0),
                new Coordinate(187, 30, 0),
                new Coordinate(197, 30, 0),
                new Coordinate(197, 36, 0)}), 20);

        //x1
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 1, 5, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(187.05, 25, 14), 0,energeticSum, debug);
        String filename = "target/T22.vtk";
        String filename2 = "target/T22.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertEquals(true, false);

    }


    /**
     * TC23 – Two buildings behind an earth-berm on flat ground with homogeneous acoustic
     * properties
     */
    public void TC23() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(38, 14, 1)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(75, 34, 0),
                new Coordinate(110, 34, 0),
                new Coordinate(110, 26, 0),
                new Coordinate(75, 26, 0),
                new Coordinate(75, 34, 0)}), 9);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(83, 18, 0),
                new Coordinate(118, 18, 0),
                new Coordinate(118, 10, 0),
                new Coordinate(83, 10, 0),
                new Coordinate(83, 18, 0)}), 8);

        //x1
        mesh.addTopographicPoint(new Coordinate(30, -14, 0));
        mesh.addTopographicPoint(new Coordinate(122, -14, 0));
        mesh.addTopographicPoint(new Coordinate(122, 45, 0));
        mesh.addTopographicPoint(new Coordinate(30, 45, 0));
        mesh.addTopographicPoint(new Coordinate(59.6, -9.87, 0));
        mesh.addTopographicPoint(new Coordinate(76.84, -5.28, 10));
        mesh.addTopographicPoint(new Coordinate(63.71, 41.16, 10));
        mesh.addTopographicPoint(new Coordinate(46.27, 36.28, 10));
        mesh.addTopographicPoint(new Coordinate(46.27, 36.28, 10));
        mesh.addTopographicPoint(new Coordinate(54.68, 37.59, 10));
        mesh.addTopographicPoint(new Coordinate(55.93, 37.93, 10));
        mesh.addTopographicPoint(new Coordinate(59.60, -9.87, 10));
        mesh.addTopographicPoint(new Coordinate(67.35, -6.83, 10));
        mesh.addTopographicPoint(new Coordinate(68.68, -6.49, 10));
        mesh.addTopographicPoint(new Coordinate(54.68, 37.59, 10));
        mesh.addTopographicPoint(new Coordinate(55.93, 37.39, 10));
        //x2
        mesh.addTopographicPoint(new Coordinate(122, -14, 0));
        mesh.addTopographicPoint(new Coordinate(122, 45, 0));
        mesh.addTopographicPoint(new Coordinate(30, 45, 0));
        mesh.addTopographicPoint(new Coordinate(30, -14, 0));
        mesh.addTopographicPoint(new Coordinate(76.84, -5.28, 10));
        mesh.addTopographicPoint(new Coordinate(63.71, 41.16, 10));
        mesh.addTopographicPoint(new Coordinate(46.27, 36.28, 10));
        mesh.addTopographicPoint(new Coordinate(59.60, -9.87, 10));
        mesh.addTopographicPoint(new Coordinate(54.68, 37.59, 10));
        mesh.addTopographicPoint(new Coordinate(55.93, 37.93, 10));
        mesh.addTopographicPoint(new Coordinate(63.71, 41.16, 10));
        mesh.addTopographicPoint(new Coordinate(67.35, -6.83, 10));
        mesh.addTopographicPoint(new Coordinate(68.68, -6.49, 10));
        mesh.addTopographicPoint(new Coordinate(76.84, -5.28, 10));
        mesh.addTopographicPoint(new Coordinate(67.35, -6.93, 10));
        mesh.addTopographicPoint(new Coordinate(68.68, -6.49, 10));
        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 1, 0, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(187.05, 25, 14), 0,energeticSum, debug);
        String filename = "target/T23.vtk";
        String filename2 = "target/T23.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertEquals(true, false);

    }

    /**
     * TC24 – Two buildings behind an earth-berm on flat ground with homogeneous acoustic
     * properties – receiver position modified
     */
    public void TC24() throws LayerDelaunayError {

        assertEquals(true, false);

    }

    /**
     * TC25 – Replacement of the earth-berm by a barrier
     */
    public void TC25() throws LayerDelaunayError {

        assertEquals(true, false);

    }

    /**
     * TC26 – Road source with influence of retrodiffraction
     */

    public void TC26() throws LayerDelaunayError {

        assertEquals(true, false);

    }

    /**
     * TC27 Source located in flat cut with retro-diffraction
     */
    public void TC27() throws LayerDelaunayError {

        assertEquals(true, false);

    }

    /**
     * TC28 Propagation over a large distance with many buildings between source and
     * receiver
     */
    @Test
    public void TC28() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        ////////////////////////////////////////////////////////////////////////////
        //Add road source as one point
        List<Geometry> srclst = new ArrayList<Geometry>();
        srclst.add(factory.createPoint(new Coordinate(0, 50, 4)));
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-1500., -1500., 0.), new Coordinate(1500., 1500., 0.));
        //Add source sound level
        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
        // GeometrySoilType
        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-11, 1011, -300, 300)), 0.5));

        //Build query structure for sources
        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
        int idsrc = 0;
        for (Geometry src : srclst) {
            sourcesIndex.appendGeometry(src, idsrc);
            idsrc++;
        }
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(113, 10, 0),
                new Coordinate(127, 16, 0),
                new Coordinate(102, 70, 0),
                new Coordinate(88, 64, 0),
                new Coordinate(113, 10, 0)}), 6);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(176, 19, 0),
                new Coordinate(164, 88, 0),
                new Coordinate(184, 91, 0),
                new Coordinate(196, 22, 0),
                new Coordinate(176, 19, 0)}), 10);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(250, 70, 0),
                new Coordinate(250, 180, 0),
                new Coordinate(270, 180, 0),
                new Coordinate(270, 70, 0),
                new Coordinate(250, 70, 0)}), 14);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(332, 32, 0),
                new Coordinate(348, 126, 0),
                new Coordinate(361, 108, 0),
                new Coordinate(349, 44, 0),
                new Coordinate(332, 32, 0)}), 10);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(400, 5, 0),
                new Coordinate(400, 85, 0),
                new Coordinate(415, 85, 0),
                new Coordinate(415, 5, 0),
                new Coordinate(400, 5, 0)}), 9);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(444, 47, 0),
                new Coordinate(436, 136, 0),
                new Coordinate(516, 143, 0),
                new Coordinate(521, 89, 0),
                new Coordinate(506, 87, 0),
                new Coordinate(502, 127, 0),
                new Coordinate(452, 123, 0),
                new Coordinate(459, 48, 0),
                new Coordinate(444, 47, 0)}), 12);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(773, 12, 0),
                new Coordinate(728, 90, 0),
                new Coordinate(741, 98, 0),
                new Coordinate(786, 20, 0),
                new Coordinate(773, 12, 0)}), 14);

        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(972, 82, 0),
                new Coordinate(979, 121, 0),
                new Coordinate(993, 118, 0),
                new Coordinate(986, 79, 0),
                new Coordinate(972, 82, 0)}), 8);

        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        List<Coordinate> vert = mesh.getVertices();
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());
        // rose of favourable conditions
        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};

        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
                freqLvl, 1, 5, 1500, 1500, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);

        ComputeRaysOut propDataOut = new ComputeRaysOut();
        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);

        computeRays.initStructures();

        double energeticSum[] = new double[freqLvl.size()];
        List<PropagationDebugInfo> debug = new ArrayList<>();
        computeRays.computeRaysAtPosition(new Coordinate(1000, 100, 1), 0,energeticSum, debug);
        String filename = "target/T28.vtk";
        String filename2 = "target/T28.ply";
        try {
            writeVTK(filename, propDataOut);
            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertEquals(true, true);

    }

    /**
     * TestPLY - Test ply
     */
    public void Tply() throws LayerDelaunayError {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Add building
        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
                new Coordinate(167.2, 39.5),
                new Coordinate(151.6, 48.5),
                new Coordinate(141.1, 30.3),
                new Coordinate(156.7, 21.3),
                new Coordinate(159.7, 26.5),
                new Coordinate(151.0, 31.5),
                new Coordinate(155.5, 39.3),
                new Coordinate(164.2, 34.3),
                new Coordinate(167.2, 39.5)}), 10);

        mesh.finishPolygonFeeding(cellEnvelope);

        String filename2 = "target/T_ply.ply";
        try {

            writePLY(filename2, mesh);
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertEquals(true, false);
    }



}