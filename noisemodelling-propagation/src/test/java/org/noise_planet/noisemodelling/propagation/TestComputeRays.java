package org.noise_planet.noisemodelling.propagation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import org.cts.crs.CRSException;
import org.cts.op.CoordinateOperationException;
import org.h2gis.functions.spatial.crs.ST_Transform;
import org.h2gis.functions.spatial.volume.GeometryExtrude;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFilter;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.math.Vector2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class TestComputeRays {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestComputeRays.class);

    /**
     * Test vertical edge diffraction ray computation
     * @throws LayerDelaunayError
     * @throws ParseException
     */
    @Test
    public void TestcomputeVerticalEdgeDiffraction() throws LayerDelaunayError, ParseException {
        GeometryFactory factory = new GeometryFactory();
        WKTReader wktReader = new WKTReader(factory);
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

        PropagationProcessData processData = new PropagationProcessData(manager);
        // new ArrayList<>(), manager, sourcesIndex, srclst, new ArrayList<>(), new ArrayList<>(), 0, 99, 1000,1000,0,0,new double[0],0,0,new EmptyProgressVisitor(), new ArrayList<>(), true

        ComputeRays computeRays = new ComputeRays(processData);
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
    }


    /**
     * Test vertical edge diffraction ray computation
     * @throws LayerDelaunayError
     * @throws ParseException
     */
    @Test
    public void TestComputeHorizontalEdgeDiffraction() throws LayerDelaunayError, ParseException {
        GeometryFactory factory = new GeometryFactory();
        WKTReader wktReader = new WKTReader(factory);
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(316705, 6706347, 0.), new Coordinate(316828, 6706469, 0.));
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();
        mesh.addGeometry(wktReader.read("POLYGON ((316759.81 6706397.4101739, 316759.81 6706403.99033324, 316765.7002829955 6706404.19000385, 316778.88539775345 6706404.489665548, 316777.49275745824 6706400.809116197, 316765.598667453 6706399.209910818, 316765.5966749492 6706399.209431015, 316765.594822107 6706399.208555082, 316765.59318675095 6706399.207319812, 316765.5918375706 6706399.205777088, 316765.59083123534 6706399.203991711, 316765.59021001414 6706399.202038671, 316765.58999999997 6706399.2, 316765.58999999997 6706397.509829072, 316759.81 6706397.4101739))"), 16.68046);
        mesh.addGeometry(wktReader.read("POLYGON ((316755.91050631634 6706412.408966506, 316756.3094447798 6706419.689593465, 316765.78984906914 6706419.290418547, 316765.6900012205 6706412.900156232, 316765.69 6706412.9, 316765.69 6706412.20970156, 316762.3996971088 6706412.109995412, 316762.39766060974 6706412.109722513, 316762.3957228751 6706412.109039148, 316762.3939657122 6706412.107974169, 316762.39246330503 6706412.106572536, 316762.3912790822 6706412.104893423, 316762.3904630394 6706412.10300772, 316762.3900496281 6706412.100995037, 316762.1910047661 6706410.110546417, 316758.81 6706410.30942905, 316758.81 6706412.1, 316758.8097809892 6706412.102081406, 316758.80913354986 6706412.104071641, 316758.8080860413 6706412.10588353, 316758.8066843467 6706412.107437708, 316758.8049898632 6706412.108666099, 316758.8030768129 6706412.109514894, 316758.8010289915 6706412.109946918, 316755.91050631634 6706412.408966506))"), 16.73458);
        cellEnvelope.expandBy(200);
        mesh.finishPolygonFeeding(cellEnvelope);
        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(), mesh.getTriNeighbors(), mesh.getVertices());

        DiffractionWithSoilEffetZone eff = manager.getPath(new Coordinate(316876.05185368325, 6706318.789634008, 22.089050196052437),
                new Coordinate(316747.10402055364, 6706422.950335046, 12.808121783800553));
        assertEquals(3, eff.getPath().size());
    }

    /**
     * Regression test for hull points in intersection with buildings
     */
    @Test
    public void TestComputeDiffractionRaysComplex()  throws LayerDelaunayError, ParseException {
        GeometryFactory factory = new GeometryFactory();
        WKTReader wktReader = new WKTReader(factory);
        //Scene dimension
        Envelope cellEnvelope = new Envelope();
        Coordinate p1 = new Coordinate(316886.8727665055, 6703857.739385221, 8.581142709966494);
        Coordinate p2 = new Coordinate(316876.05185368325, 6703918.789634008, 13.667755518192145);
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        mesh.addGeometry(wktReader.read("POLYGON ((316901.11218957935 6703895.907300464, 316907.20238788123 6703894.409710717, 316907.20420580526 6703894.409072553, 316907.2058676055 6703894.408097605, 316907.2073115943 6703894.406822067, 316907.20848416915 6703894.405293286, 316907.20934180304 6703894.403568014, 316907.20985265967 6703894.401710292, 316907.20999777544 6703894.399789084, 316907.20977176365 6703894.397875704, 316906.711831376 6703892.10734992, 316913.4929281489 6703890.412075727, 316914.09035236185 6703892.602631174, 316914.0910465184 6703892.604453669, 316914.09208120627 6703892.606106775, 316914.0934170728 6703892.60752762, 316914.09500331036 6703892.608662164, 316914.0967795891 6703892.609467257, 316914.098678351 6703892.609912277, 316914.1006273796 6703892.6099803, 316914.10255254694 6703892.609668738, 316926.5927824364 6703889.312248047, 316928.7878607862 6703897.992785159, 316916.29752967303 6703901.190309923, 316903.30721426976 6703904.487851526, 316901.11218957935 6703895.907300464))"), 11.915885805791621);
        mesh.addGeometry(wktReader.read("POLYGON ((316886.41232341167 6703903.607226911, 316897.992788823 6703900.7121105585, 316899.7878130121 6703907.692760183, 316888.3070780876 6703910.587902033, 316886.41232341167 6703903.607226911))"), 13.143551238469575);
        mesh.addGeometry(wktReader.read("POLYGON ((316888.1122565511 6703867.407367232, 316896.10231072427 6703865.509729366, 316896.1023971443 6703865.509708434, 316904.19264749985 6703863.512115754, 316905.7879625209 6703870.092790215, 316897.6974903785 6703872.190320031, 316897.69565429183 6703872.190993624, 316897.6939844259 6703872.192011705, 316897.69254465215 6703872.193335333, 316897.6913900411 6703872.194913881, 316897.6905647558 6703872.19668697, 316897.6901003631 6703872.198586781, 316897.6900146256 6703872.200540646, 316897.6903108229 6703872.202473832, 316898.88782459043 6703876.8927360885, 316896.5070718511 6703877.487924273, 316895.9096476382 6703875.297368825, 316895.9089607805 6703875.295561034, 316895.9079388003 6703875.293919256, 316895.9066199183 6703875.292504889, 316895.9050534592 6703875.291370831, 316895.9032980068 6703875.290559494, 316895.9014192128 6703875.29010122, 316895.8994873419 6703875.290013149, 316895.8975746438 6703875.290298575, 316892.70774903445 6703876.087754977, 316892.20980580675 6703873.598038838, 316892.2091870762 6703873.596050616, 316892.2081700073 6703873.594233633, 316892.2067986987 6703873.592666672, 316892.20513260836 6703873.591417673, 316892.2032439755 6703873.590540791, 316892.20121468866 6703873.590074047, 316892.1991327346 6703873.590037678, 316892.19708838384 6703873.59043326, 316889.9070482306 6703874.287402003, 316888.1122565511 6703867.407367232))"), 12.447629178041673);
        mesh.addGeometry(wktReader.read("POLYGON ((316874.71224225214 6703907.007098008, 316884.49264060217 6703904.4122984465, 316885.88800615596 6703910.292767566, 316882.7975012198 6703911.090317226, 316882.79569543176 6703911.090973888, 316882.79404857155 6703911.091963801, 316882.7926214425 6703911.093250415, 316882.7914667354 6703911.09478623, 316882.7906270828 6703911.096514541, 316882.79013348534 6703911.098371537, 316882.79000416707 6703911.100288658, 316882.7902439024 6703911.102195121, 316883.68809233804 6703915.092632613, 316877.207229554 6703916.687921914, 316875.10968736705 6703908.497519089, 316875.1096623494 6703908.497423373, 316874.71224225214 6703907.007098008))"), 13.675755295119075);
        mesh.addGeometry(wktReader.read("POLYGON ((316909.9120555793 6703863.50729387, 316912.69263484754 6703862.8121490525, 316915.7878816859 6703875.692369767, 316913.0074268087 6703876.288181527, 316909.9120555793 6703863.50729387))"), 10.563372307530404);
        mesh.addGeometry(wktReader.read("POLYGON ((316864.71217490366 6703906.807692843, 316867.59278650914 6703906.2117042355, 316868.69034157164 6703910.302591286, 316868.691015592 6703910.304390946, 316868.6920214139 6703910.306028446, 316868.69332189154 6703910.3074433105, 316868.69486899726 6703910.308583287, 316868.6966055951 6703910.309406275, 316868.6984675512 6703910.309881882, 316868.70038610185 6703910.309992543, 316868.70229039335 6703910.309734171, 316872.1022903933 6703909.509734171, 316872.1040185817 6703909.50915702, 316872.10561336356 6703909.508275878, 316872.1070217964 6703909.507119998, 316872.1081971238 6703909.505727754, 316872.10910032806 6703909.504145362, 316872.10970142495 6703909.502425356, 316872.4082232103 6703908.308338215, 316874.09833733283 6703907.910664304, 316875.09133194154 6703908.506461069, 316877.18779957696 6703916.692668026, 316870.6072353747 6703918.287956317, 316869.60968663864 6703914.397516247, 316869.6090061109 6703914.395653741, 316869.607971459 6703914.393962133, 316869.6066233658 6703914.392507936, 316869.6050148389 6703914.39134833, 316869.60320912633 6703914.390528913, 316869.60127722955 6703914.390081901, 316869.59929511155 6703914.390024874, 316869.59734071 6703914.390360074, 316866.70739797025 6703915.187585657, 316865.80974391196 6703911.297751404, 316865.8097139901 6703911.297625469, 316864.71217490366 6703906.807692843))"), 13.227635299718834);
        mesh.addGeometry(wktReader.read("POLYGON ((316878.5120586163 6703875.207141824, 316886.39273419575 6703873.112278695, 316888.5878448322 6703881.992498998, 316883.2977917435 6703883.190246867, 316883.29591523844 6703883.190872311, 316883.29419854766 6703883.19185487, 316883.2927088358 6703883.193156104, 316883.29150438716 6703883.194725101, 316883.2906323251 6703883.196500476, 316883.29012676864 6703883.198412769, 316883.2900074975 6703883.200387161, 316883.2902791779 6703883.202346406, 316883.9902791779 6703886.102346405, 316883.99093748967 6703886.104227399, 316883.99195572 6703886.105940501, 316883.99329342984 6703886.107417676, 316883.99489749194 6703886.108600256, 316883.99670420075 6703886.109441277, 316883.9986418026 6703886.109907336, 316884.0006333454 6703886.109979923, 316884.00259973475 6703886.109656157, 316886.59276802506 6703885.412303156, 316887.1878184773 6703887.792504964, 316881.90736221685 6703888.988079967, 316878.5120586163 6703875.207141824))"), 12.286729118682786);
        mesh.addGeometry(wktReader.read("POLYGON ((316882.81188369356 6703911.1072608605, 316885.89284142124 6703910.312174995, 316887.08777371957 6703914.892748807, 316883.90655846545 6703915.68805262, 316883.70964576973 6703915.097314532, 316882.81188369356 6703911.1072608605))"), 9.484434135999829);
        mesh.addGeometry(wktReader.read("POLYGON ((316881.91189041897 6703889.007560945, 316887.19214319426 6703887.812032015, 316887.78838154906 6703890.892596849, 316882.60756777594 6703892.187800292, 316881.91189041897 6703889.007560945))"), 10.568596285589448);
        mesh.addGeometry(wktReader.read("POLYGON ((316882.61290170497 6703892.207082339, 316887.79261431424 6703890.9121541865, 316888.2880030182 6703892.992786742, 316883.30653076805 6703894.287969527, 316882.61290170497 6703892.207082339))"), 9.10452432502548);
        mesh.addGeometry(wktReader.read("POLYGON ((316905.8121263122 6703864.407576221, 316909.8925868034 6703863.511865381, 316911.4878736878 6703870.092423779, 316907.40741319663 6703870.988134619, 316905.8121263122 6703864.407576221))"), 11.07579981877306);
        mesh.addGeometry(wktReader.read("POLYGON ((316856.5121665223 6703915.707105044, 316858.00720681547 6703921.487927511, 316864.4879847223 6703919.8926591035, 316863.5902691971 6703916.1023046635, 316863.5900090117 6703916.100424443, 316863.59010879387 6703916.098528932, 316863.5905649486 6703916.096686421, 316863.591361041 6703916.094963295, 316863.5924683885 6703916.093421639, 316863.5938470941 6703916.092116996, 316863.59544748423 6703916.091096371, 316863.597211898 6703916.090396537, 316866.6881294962 6703915.193033365, 316865.79253813525 6703911.312137467, 316858.91274731315 6703913.007158394, 316859.5095358266 6703914.896988687, 316859.50994610385 6703914.89896317, 316859.50995188195 6703914.90097982, 316859.50955292606 6703914.902956621, 316859.50876546133 6703914.904813179, 316859.5076215132 6703914.90647399, 316859.5061676051 6703914.907871509, 316859.5044628661 6703914.908948901, 316859.5025766265 6703914.90966235, 316856.5121665223 6703915.707105044))"),13.50627531988858);

        cellEnvelope.expandToInclude(mesh.getGeometriesBoundingBox());
        cellEnvelope.expandToInclude(p1);
        cellEnvelope.expandToInclude(p2);
        cellEnvelope.expandBy(100);

        mesh.finishPolygonFeeding(cellEnvelope);


        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(), mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData processData = new PropagationProcessData(manager);
        //new ArrayList<>(), manager, sourcesIndex, srclst, new ArrayList<>(), new ArrayList<>(), 0, 99, 1000,1000,0,0,new double[0],0,0,new EmptyProgressVisitor(), new ArrayList<>(), true
        ComputeRays computeRays = new ComputeRays(processData);

        computeRays.initStructures();

        assertFalse(manager.isFreeField(p1, p2));

        List<Coordinate> pts = computeRays.computeSideHull(true, p1, p2);
        assertEquals(5, pts.size());
        for(int i=0; i < pts.size() - 1; i++) {
            assertTrue(manager.isFreeField(pts.get(i), pts.get(i+1)));
        }

        pts = computeRays.computeSideHull(false, p1, p2);
        assertEquals(5, pts.size());
        for(int i=0; i < pts.size() - 1; i++) {
            assertTrue(manager.isFreeField(pts.get(i), pts.get(i+1)));
        }

        ArrayList<PropagationDebugInfo> dbg = new ArrayList<>();
        List<PropagationPath> prop = computeRays.directPath(p2, p1, true, true, dbg);
        // 3 paths
        // 1 over the building
        assertEquals(3,prop.size());
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
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(0, 0, 0.), new Coordinate(20, 15, 0.));
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();
        mesh.addGeometry(wktReader.read("POLYGON((5 5, 7 5, 7 6, 8 6, 8 8, 5 8, 5 5))"), 4.3);
        mesh.addGeometry(wktReader.read("POLYGON((9 7, 10 7, 10 9, 9 9, 9 7))"), 4.3);
        mesh.finishPolygonFeeding(cellEnvelope);

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(), mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData processData = new PropagationProcessData(manager);
        ComputeRays computeRays = new ComputeRays(processData);
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

        PropagationProcessData processData = new PropagationProcessData(manager);
        ComputeRays computeRays = new ComputeRays(processData);
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

    @Test
    public void testHillHideReceiverSourceRay() throws LayerDelaunayError, ParseException, IOException, XMLStreamException, CoordinateOperationException, CRSException {

        GeometryFactory factory = new GeometryFactory();
        WKTReader wktReader = new WKTReader(factory);
        //Scene dimension
        Coordinate proj = new Coordinate( 356372.67, 6686702.14);
        double zOffset = 10;
        Envelope cellEnvelope = new Envelope(new Coordinate(-300, -300, 0.), new Coordinate(500, 500, 0.));
        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        // Create DEM using gaussian 2d function
        int pointCount = 50;
        double mountainX = -80;
        double mountainY = 50;
        double mountainWidth = 8;
        double mountainHeight = 50;
        double mountainLength = 8;
        double domainXmax = cellEnvelope.getMaxX();
        double domainXmin = cellEnvelope.getMinX();
        double domainYmax = cellEnvelope.getMaxY();
        double domainYmin = cellEnvelope.getMinY();
        for(int x = 0; x < pointCount; x++) {
            for(int y = 0; y < pointCount; y++) {
                double xp = x * ((domainXmax - domainXmin) / pointCount) + domainXmin;
                double yp = y * ((domainYmax - domainYmin) / pointCount) + domainYmin;
                double zp = mountainHeight * Math.exp(-(Math.pow(x - ((mountainX - domainXmin) /
                        (domainXmax - domainXmin) * pointCount)  , 2) / mountainWidth  +
                        Math.pow(y - ((mountainY - domainYmin) / (domainYmax - domainYmin) *
                                pointCount) ,2) / mountainLength )) + zOffset;
                Coordinate p = new Coordinate(xp+proj.x, yp+proj.y, zp);
                mesh.addTopographicPoint(p);
            }
        }
        cellEnvelope = mesh.getGeometriesBoundingBox();
        cellEnvelope.expandBy(50);
        mesh.finishPolygonFeeding(cellEnvelope);
        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(), mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData processData = new PropagationProcessData(manager);
        ComputeRays computeRays = new ComputeRays(processData);
        Coordinate p1 = new Coordinate(4.5, 6.5, 1.6);
        Coordinate p2 = new Coordinate(14, 6.5, 1.6);

//        KMLDocument kmlDocument = new KMLDocument(new FileOutputStream("target/topotest.kml"));
//        kmlDocument.setInputCRS("EPSG:2154");
//        kmlDocument.writeHeader();
//        kmlDocument.writeTopographic(manager.getTriangles(), manager.getVertices());
//        kmlDocument.writeFooter();
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

        PropagationProcessData processData = new PropagationProcessData(manager);
        ComputeRays computeRays = new ComputeRays(processData);

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
//
//    private void writeVTKmesh(String filename, ComputeRaysOut propDataOut, MeshBuilder mesh) throws IOException {
//
//        int lengthPolygon = mesh.getPolygonWithHeight().get(0).geo.getBoundary().getCoordinates().length;
//
//        FileWriter fileWriter = new FileWriter(filename);
//        fileWriter.write("# vtk DataFile Version 2.0\n");
//        fileWriter.write("PropagationPath\n");
//        fileWriter.write("ASCII\n");
//        fileWriter.write("DATASET POLYDATA\n");
//        fileWriter.write("POINTS " + String.valueOf(propDataOut.propagationPaths.get(0).getPointList().size() + 2 * lengthPolygon) + " float\n");
//
//        GeometryFactory geometryFactory = new GeometryFactory();
//        List<Coordinate> coordinates = new ArrayList<>();
//        for (PropagationPath.PointPath p : propDataOut.propagationPaths.get(0).getPointList()) {
//            coordinates.add(p.coordinate);
//            fileWriter.write(String.valueOf(p.coordinate.x) + " " + String.valueOf(p.coordinate.y) + " " + String.valueOf(p.coordinate.z) + "\n");
//        }
//        LineString factoryLineString = geometryFactory.createLineString(coordinates.toArray(new Coordinate[coordinates.size()]));
//        WKTWriter wktWriter = new WKTWriter(3);
//        mesh.getPolygonWithHeight().get(0).geo.getCoordinate();
//        for (int j = 0; j < lengthPolygon; j++) {
//            double x = mesh.getPolygonWithHeight().get(0).geo.getBoundary().getCoordinates()[j].x;
//            double y = mesh.getPolygonWithHeight().get(0).geo.getBoundary().getCoordinates()[j].y;
//            double z = mesh.getPolygonWithHeight().get(0).geo.getBoundary().getCoordinates()[j].z;
//            fileWriter.write(String.valueOf(x) + " " + String.valueOf(y) + " " + String.valueOf(z) + "\n");
//            x = mesh.getPolygonWithHeight().get(0).geo.getBoundary().getCoordinates()[j].x;
//            y = mesh.getPolygonWithHeight().get(0).geo.getBoundary().getCoordinates()[j].y;
//            z = mesh.getPolygonWithHeight().get(0).getHeight();
//            fileWriter.write(String.valueOf(x) + " " + String.valueOf(y) + " " + String.valueOf(z) + "\n");
//        }
//
//        fileWriter.write("LINES 1\n");
//        fileWriter.write(String.valueOf(propDataOut.propagationPaths.get(0).getPointList().size()));
//        int i = 0;
//        for (PropagationPath.PointPath p : propDataOut.propagationPaths.get(0).getPointList()) {
//            fileWriter.write(" " + String.valueOf(i));
//            i++;
//        }
//        fileWriter.write("\n");
//
//        fileWriter.write("POLYGONS 1 " + String.valueOf(2 * lengthPolygon + 1) + "\n");
//
//        fileWriter.write(String.valueOf(2 * lengthPolygon));
//        for (int j = 0; j < 2 * lengthPolygon; j++) {
//            fileWriter.write(" " + String.valueOf(j + i));
//        }
//        fileWriter.write("\n");
//
//        fileWriter.close();
//    }
//
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
//
//
    private void writeVTK(String filename, ComputeRaysOut propDataOut) throws IOException {


        FileWriter fileWriter = new FileWriter(filename);
        fileWriter.write("# vtk DataFile Version 2.0\n");
        fileWriter.write("PropagationPath\n");
        fileWriter.write("ASCII\n");
        fileWriter.write("DATASET POLYDATA\n");
        int nbPoints = 0;
        List<PropagationPath> propagationPaths = propDataOut.getPropagationPaths();
        for (int j = 0; j < propagationPaths.size(); j++) {
            nbPoints = nbPoints + propagationPaths.get(j).getPointList().size();
        }
        fileWriter.write("\n");
        fileWriter.write("POINTS " + String.valueOf(nbPoints) + " float\n");

        GeometryFactory geometryFactory = new GeometryFactory();
        List<Coordinate> coordinates = new ArrayList<>();
        for (int j = 0; j < propagationPaths.size(); j++) {
            for (PropagationPath.PointPath p : propagationPaths.get(j).getPointList()) {
                coordinates.add(p.coordinate);
                fileWriter.write(String.valueOf(p.coordinate.x) + " " + String.valueOf(p.coordinate.y) + " " + String.valueOf(p.coordinate.z) + "\n");
            }
        }
        LineString factoryLineString = geometryFactory.createLineString(coordinates.toArray(new Coordinate[coordinates.size()]));
        WKTWriter wktWriter = new WKTWriter(3);

        fileWriter.write("\n");
        fileWriter.write("LINES " + String.valueOf(propagationPaths.size()) + " " + String.valueOf(nbPoints + propagationPaths.size()) + "\n");
        int i = 0;
        for (int j = 0; j < propagationPaths.size(); j++) {
            fileWriter.write(String.valueOf(propagationPaths.get(j).getPointList().size()));

            for (PropagationPath.PointPath p : propagationPaths.get(j).getPointList()) {
                fileWriter.write(" " + String.valueOf(i));
                i++;
            }
            fileWriter.write("\n");
        }


        fileWriter.close();
    }
//
//
//    private static ArrayList<Double> asW(double... dbValues) {
//        ArrayList<Double> ret = new ArrayList<>(dbValues.length);
//        for (double db_m : dbValues) {
//            ret.add(PropagationProcess.dbaToW(db_m));
//        }
//        return ret;
//    }
//
//    /**
//     * Test Direct Field
//     */
//    @Test
//    public void DirectRay() throws LayerDelaunayError, IOException {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(0, 0, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-250, 250, -250, 50)), 0.));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 0, 0, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(200, 0, 4),0, energeticSum, debug);
//
//
//        /*PropagationProcessPathData propData = new PropagationProcessPathData();
//        propData.setTemperature(15);
//        propData.setHumidity(70);
//        EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
//        splCompare(evaluateAttenuationCnossos.evaluate(propDataOut.propagationPaths.get(0), propData), "Test T01", new double[]{-54, -54.1, -54.2, -54.5, -54.8, -55.8, -59.3, -73.0}, ERROR_EPSILON_TEST_T);
//*/
//        String filename = "target/test.vtk";
//        try {
//            writeVTK(filename, propDataOut);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    /**
//     * Test TC05 -- Reduced receiver height to include diffraction in some frequency bands
//     */
//    @Test
//    public void TC05() throws LayerDelaunayError, IOException {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -20, 80)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -20, 80)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -20, 80)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//        // Add topographic points
//        //x1
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        //x2
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 0, 0, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 14), 0,energeticSum, debug);
//
//        String filename = "target/T05.vtk";
//        String filename2 = "target/T05.ply";
//        try {
//            writeVTK(filename, propDataOut);
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }
//
//
//    /**
//     * Test TC06 -- Reduced receiver height to include diffraction in some frequency bands
//     * This test
//     */
//
//    public void TC06() throws LayerDelaunayError {
//        // TODO Rayleigh stuff
//
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 500, -20, 80)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -20, 80)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -20, 80)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//        // Add topographic points
//        //x1
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        //x2
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 0, 0, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 11.5), 0,energeticSum, debug);
//        assertEquals(true, false);
//    }
//
//
//    /**
//     * Test TC07 -- Flat ground with spatially varying acoustic properties and long barrier
//     */
//    @Test
//    public void TC07() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(100, 240, 0),
//                new Coordinate(100.1, 240, 0),
//                new Coordinate(265.1, -180, 0),
//                new Coordinate(265, -180, 0),
//                new Coordinate(100, 240, 0)}), 6);
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 0, 250, 250, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 4), 0,energeticSum, debug);
//
//        String filename = "target/T07.vtk";
//        String filename2 = "target/T07.ply";
//        try {
//            writeVTK(filename, propDataOut);
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }
//
//    /**
//     * Test TC08 -- Flat ground with spatially varying acoustic properties and short barrier
//     */
//    @Test
//    public void TC08() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(175, 50, 0),
//                new Coordinate(175.01, 50, 0),
//                new Coordinate(190.01, 10, 0),
//                new Coordinate(190, 10, 0),
//                new Coordinate(175, 50, 0)}), 6);
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 1, 300, 300, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 4), 0,energeticSum, debug);
//
//        String filename = "target/T08.vtk";
//        String filename2 = "target/T08.ply";
//        try {
//            writeVTK(filename, propDataOut);
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }
//
//    /**
//     * Test TC09 -- Ground with spatially varying heights and and acoustic properties and short
//     * barrier
//     */
//
//    public void TC09() throws LayerDelaunayError {
//        // Impossible shape for NoiseModelling
//        assertEquals(true, false);
//    }
//
//    /**
//     * Test TC10 -- Flat ground with homogeneous acoustic properties and cubic building – receiver
//     * at low height
//     */
//
//    public void TC10() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(50, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(55, 5, 0),
//                new Coordinate(65, 5, 0),
//                new Coordinate(65, 15, 0),
//                new Coordinate(55, 15, 0),
//                new Coordinate(55, 5, 0)}), 10);
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 5, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(70, 10, 4), 0,energeticSum, debug);
//        String filename = "target/T09.vtk";
//        String filename2 = "target/T09.ply";
//        try {
//            writeVTK(filename, propDataOut);
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }
//
//    /**
//     * Test TC11 -- Flat ground with homogeneous acoustic properties and cubic building – receiver
//     * at large height
//     */
//    @Test
//    public void TC11() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(50, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(55, 5,0),
//                new Coordinate(65, 5,0),
//                new Coordinate(65, 15,0),
//                new Coordinate(55, 15,0),
//                new Coordinate(55, 5,0)}), 10);
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 5, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(70, 10, 15), 0,energeticSum, debug);
//        String filename = "target/T11.vtk";
//        String filename2 = "target/T11.ply";
//        try {
//            writeVTK(filename, propDataOut);
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }
//
//    /**
//     * Test TC12 -- Flat ground with homogeneous acoustic properties and polygonal building –
//     * receiver at low height
//     */
//    @Test
//    public void TC12() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(0, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(11., 15.5, 0),
//                new Coordinate(12., 13, 0),
//                new Coordinate(14.5, 12, 0),
//                new Coordinate(17.0, 13, 0),
//                new Coordinate(18.0, 15.5, 0),
//                new Coordinate(17.0, 18, 0),
//                new Coordinate(14.5, 19, 0),
//                new Coordinate(12.0, 18, 0),
//                new Coordinate(11, 15.5, 0)}), 10);
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 5, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(30, 20, 6), 0,energeticSum, debug);
//        String filename = "target/T12.vtk";
//        String filename2 = "target/T12.ply";
//        try {
//            writeVTK(filename, propDataOut);
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }
//
//
//    /**
//     * Test TC13 -- Ground with spatially varying heights and acoustic properties and polygonal
//     * building
//     */
//    @Test
//    public void TC13() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(169.4, 41.0, 0),
//                new Coordinate(172.5, 33.5, 0),
//                new Coordinate(180.0, 30.4, 0),
//                new Coordinate(187.5, 33.5, 0),
//                new Coordinate(190.6, 41.0, 0),
//                new Coordinate(187.5, 48.5, 0),
//                new Coordinate(180.0, 51.6, 0),
//                new Coordinate(172.5, 48.5, 0),
//                new Coordinate(169.4, 41.0, 0)}), 30);
//
//        //x1
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        //x2
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 5, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 28.5), 0,energeticSum, debug);
//        String filename = "target/T13.vtk";
//        String filename2 = "target/T13.ply";
//        try {
//            writeVTK(filename, propDataOut);
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * Test TC14 -- Flat ground with homogeneous acoustic properties and polygonal building –
//     * receiver at large height
//     */
//    @Test
//    public void TC14() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(8, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-300, 300, -300, 300)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(11., 15.5, 0),
//                new Coordinate(12., 13, 0),
//                new Coordinate(14.5, 12, 0),
//                new Coordinate(17.0, 13, 0),
//                new Coordinate(18.0, 15.5, 0),
//                new Coordinate(17.0, 18, 0),
//                new Coordinate(14.5, 19, 0),
//                new Coordinate(12.0, 18, 0),
//                new Coordinate(11, 15.5, 0)}), 10);
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 5, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(25, 20, 23), 0,energeticSum, debug);
//        String filename = "target/T14.vtk";
//        String filename2 = "target/T14.ply";
//        try {
//            writeVTK(filename, propDataOut);
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//


    /**
     * Test TC14 -- Flat ground with homogeneous acoustic properties and polygonal building –
     * receiver at large height
     */
    @Test
    public void TC14() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

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
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData rayData = new PropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(25, 20, 23));
        rayData.addSource(factory.createPoint(new Coordinate(8, 10, 1)));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(-300, 300, -300, 300)), 0.2));
        rayData.setComputeVerticalDiffraction(true);
        PropagationProcessPathData attData = new PropagationProcessPathData();
        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);
        //exportRays("target/T14.geojson", propDataOut);
        exportScene("target/T14.kml", manager, propDataOut);
        //assertRaysEquals(TestComputeRays.class.getResourceAsStream("T14.geojson"), propDataOut);
    }
    /**
     * Test TC15 -- Flat ground with homogeneous acoustic properties and four buildings
     */
    @Test
    public void TC15() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

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
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData rayData = new PropagationProcessData(manager);
        rayData.addReceiver(new Coordinate(100, 15, 5));
        rayData.addSource(factory.createPoint(new Coordinate(50, 10, 1)));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.setComputeVerticalDiffraction(true);
        rayData.addSoilType(new GeoWithSoilType(factory.toGeometry(new Envelope(-250, 250, -250, 250)), 0.5));
        PropagationProcessPathData attData = new PropagationProcessPathData();
        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);
        assertRaysEquals(TestComputeRays.class.getResourceAsStream("T15.geojson"), propDataOut);
    }

    private void exportRays(String name, ComputeRaysOut result) throws IOException {
        FileOutputStream outData = new FileOutputStream(name);
        GeoJSONDocument jsonDocument = new GeoJSONDocument(outData);
        jsonDocument.setRounding(1);
        jsonDocument.writeHeader();
        for(PropagationPath propagationPath : result.getPropagationPaths()) {
            jsonDocument.writeRay(propagationPath);
        }
        jsonDocument.writeFooter();
    }

    private void exportScene(String name, FastObstructionTest manager, ComputeRaysOut result) throws IOException {
        try {
            Coordinate proj = new Coordinate( 351714.794877, 6685824.856402, 0);
            FileOutputStream outData = new FileOutputStream(name);
            KMLDocument kmlDocument = new KMLDocument(outData);
            kmlDocument.setInputCRS("EPSG:2154");
            kmlDocument.setOffset(proj);
            kmlDocument.writeHeader();
            if(manager != null) {
                kmlDocument.writeTopographic(manager.getTriangles(), manager.getVertices());
            }
            if(result != null) {
                kmlDocument.writeRays(result.getPropagationPaths());
            }
            if(manager != null && manager.isHasBuildingWithHeight()) {
                kmlDocument.writeBuildings(manager);
            }
            kmlDocument.writeFooter();
        } catch (XMLStreamException | CoordinateOperationException | CRSException ex) {
            throw new IOException(ex);
        }
    }

    private void assertRaysEquals(InputStream expected, ComputeRaysOut result) throws IOException {
        // Parse expected
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(expected);
        // Generate result
        ByteOutputStream outData = new ByteOutputStream();
        GeoJSONDocument jsonDocument = new GeoJSONDocument(outData);
        jsonDocument.setRounding(1);
        jsonDocument.writeHeader();
        for(PropagationPath propagationPath : result.getPropagationPaths()) {
            jsonDocument.writeRay(propagationPath);
        }
        jsonDocument.writeFooter();
        JsonNode resultNode = mapper.readTree(outData.toString());
        // Check equality
        assertEquals(rootNode, resultNode);
    }
//
//
//    /**
//     * Reflecting barrier on ground with spatially varying heights and acoustic properties
//     */
//    @Test
//    public void TC16() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//        // Add topographic points
//        //x1
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        //x2
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(114, 52, 0),
//                new Coordinate(170, 60, 0),
//                new Coordinate(170, 62, 0),
//                new Coordinate(114, 54, 0),
//                new Coordinate(114, 52, 0)}), 15);
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 0, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 14), 0,energeticSum, debug);
//
//        String filename = "target/T16.vtk";
//        String filename2 = "target/T16.ply";
//        try {
//            writeVTK(filename, propDataOut);
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }
//
//
//    /**
//     * Reflecting two barrier on ground with spatially varying heights and acoustic properties
//     */
//    @Test
//    public void TC16b() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(114, 52, 0),
//                new Coordinate(170, 60, 0),
//                new Coordinate(170, 62, 0),
//                new Coordinate(114, 54, 0),
//                new Coordinate(114, 52, 0)}), 20);
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(114, 12, 0),
//                new Coordinate(170, 30, 0),
//                new Coordinate(170, 32, 0),
//                new Coordinate(114, 14, 0),
//                new Coordinate(114, 12, 0)}), 20);
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 2, 0, 1000, 1000, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 15), 0,energeticSum, debug);
//
//        String filename = "target/T16b.vtk";
//        String filename2 = "target/T16b.ply";
//        try {
//            writeVTK(filename, propDataOut);
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }
//
//    /**
//     * TC17 - Reflecting barrier on ground with spatially varying heights and acoustic properties
//     * reduced receiver height
//     */
//
//    public void TC17() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//        // Add topographic points
//        //x1
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        //x2
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(114, 52, 0),
//                new Coordinate(170, 60, 0),
//                new Coordinate(170, 62, 0),
//                new Coordinate(114, 54, 0),
//                new Coordinate(114, 52, 0)}), 15);
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 0, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 11.5), 0,energeticSum, debug);
//
//        String filename = "target/T17.vtk";
//        String filename2 = "target/T17.ply";
//        try {
//            writeVTK(filename, propDataOut);
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        assertEquals(true, false); // because rayleigh distance
//    }
//
//    /**
//     * TC18 - Screening and reflecting barrier on ground with spatially varying heights and
//     * acoustic properties
//     */
//
//    public void TC18() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//        // Add topographic points
//        //x1
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        //x2
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(114, 52),
//                new Coordinate(170, 60),
//                new Coordinate(170, 61),
//                new Coordinate(114, 53),
//                new Coordinate(114, 52)}), 15);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(87, 50),
//                new Coordinate(92, 32),
//                new Coordinate(92, 33),
//                new Coordinate(87, 51),
//                new Coordinate(87, 50)}), 12);
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//
//        String filename2 = "target/T18.ply";
//        try {
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 4, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(200, 50, 12), 0,energeticSum, debug);
//
//        String filename = "target/T18.vtk";
//
//        try {
//            writeVTK(filename, propDataOut);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        assertEquals(true, true);
//    }
//
//
//    /**
//     * TC18b - Screening and reflecting barrier on ground with spatially varying heights and
//     * acoustic properties
//     */
//    @Test
//    public void TC18b() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//        // Add topographic points
//        //x1
//        mesh.addTopographicPoint(new Coordinate(0, 80, 2));
//        mesh.addTopographicPoint(new Coordinate(225, 80, 2));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 2));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 2));
//        mesh.addTopographicPoint(new Coordinate(120, -20, 2));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        //x2
//        mesh.addTopographicPoint(new Coordinate(225, 80, 2));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 2));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 2));
//        mesh.addTopographicPoint(new Coordinate(0, 80, 2));
//        mesh.addTopographicPoint(new Coordinate(120, 80, 2));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(114, 52),
//                new Coordinate(170, 60),
//                new Coordinate(170, 61),
//                new Coordinate(114, 53),
//                new Coordinate(114, 52)}), 15);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(87, 50),
//                new Coordinate(92, 32),
//                new Coordinate(92, 33),
//                new Coordinate(87, 51),
//                new Coordinate(87, 50)}), 12);
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//
//        String filename2 = "target/T18b.ply";
//        try {
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(new ArrayList<>(), manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 10, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        computeRays.makeRelativeZToAbsolute();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//
//
//        computeRays.computeRaysAtPosition(new Coordinate(200 ,50 ,12 + manager.getHeightAtPosition(new Coordinate(200, 50, 12))), 0,energeticSum, debug);
//
//        String filename = "target/T18b.vtk";
//
//        try {
//            writeVTK(filename, propDataOut);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        assertEquals(true, true);
//    }
//
//    /**
//     * TC19 - Complex object and 2 barriers on ground with spatially varying heights and
//     * acoustic properties
//     */
//    @Test
//    public void TC19() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//        // Add topographic points
//        //x1
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        //x2
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(156, 28),
//                new Coordinate(145, 7),
//                new Coordinate(145, 8),
//                new Coordinate(156, 29),
//                new Coordinate(156, 28)}), 14);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(175, 35),
//                new Coordinate(188, 19),
//                new Coordinate(188, 20),
//                new Coordinate(175, 36),
//                new Coordinate(175, 35)}), 14.5);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(100, 24),
//                new Coordinate(118, 24),
//                new Coordinate(118, 30),
//                new Coordinate(100, 30),
//                new Coordinate(100, 24)}), 12);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(100, 15.1),
//                new Coordinate(118, 15.1),
//                new Coordinate(118, 23.9),
//                new Coordinate(100, 23.9),
//                new Coordinate(100, 15.1)}), 7);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(100, 9),
//                new Coordinate(118, 9),
//                new Coordinate(118, 15),
//                new Coordinate(100, 15),
//                new Coordinate(100, 9)}), 12);
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//
//        String filename2 = "target/T19.ply";
//        try {
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 1, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(200, 30, 14), 0,energeticSum, debug);
//
//
//
//        String filename = "target/T19.vtk";
//
//        try {
//            writeVTK(filename, propDataOut);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        assertEquals(true, true);
//    }
//
//    /**
//     * TC20 - Ground with spatially varying heights and acoustic properties
//     */
//
//    public void TC20() throws LayerDelaunayError {
//        //Tables 221 – 222 are not shown in this draft.
//
//        assertEquals(false, true);
//    }
//
//    /**
//     * TC21 - Building on ground with spatially varying heights and acoustic properties
//     */
//
//    public void TC21() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(167.2, 39.5),
//                new Coordinate(151.6, 48.5),
//                new Coordinate(141.1, 30.3),
//                new Coordinate(156.7, 21.3),
//                new Coordinate(159.7, 26.5),
//                new Coordinate(151.0, 31.5),
//                new Coordinate(155.5, 39.3),
//                new Coordinate(164.2, 34.3),
//                new Coordinate(167.2, 39.5)}), 11.5);
//
//        //x1
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        //x2
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//
//        String filename2 = "target/T21.ply";
//        try {
//
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 5, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(187.05, 25, 14), 0,energeticSum, debug);
//
//
//
//        String filename = "target/T21.vtk";
//        try {
//            writeVTK(filename, propDataOut);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        assertEquals(true, false);
//    }
//
//
//    /**
//     * TC22 - Building with receiver backside on ground with spatially varying heights and
//     * acoustic properties
//     */
//
//    public void TC22() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(10, 10, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(197, 36.0, 0),
//                new Coordinate(179, 36, 0),
//                new Coordinate(179, 15, 0),
//                new Coordinate(197, 15, 0),
//                new Coordinate(197, 21, 0),
//                new Coordinate(187, 21, 0),
//                new Coordinate(187, 30, 0),
//                new Coordinate(197, 30, 0),
//                new Coordinate(197, 36, 0)}), 20);
//
//        //x1
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(120, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        //x2
//        mesh.addTopographicPoint(new Coordinate(225, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(225, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, -20, 0));
//        mesh.addTopographicPoint(new Coordinate(0, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(120, 80, 0));
//        mesh.addTopographicPoint(new Coordinate(205, -5, 10));
//        mesh.addTopographicPoint(new Coordinate(205, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, 75, 10));
//        mesh.addTopographicPoint(new Coordinate(185, -5, 10));
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 5, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(187.05, 25, 14), 0,energeticSum, debug);
//        String filename = "target/T22.vtk";
//        String filename2 = "target/T22.ply";
//        try {
//            writeVTK(filename, propDataOut);
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        assertEquals(true, false);
//
//    }
//
//
//    /**
//     * TC23 – Two buildings behind an earth-berm on flat ground with homogeneous acoustic
//     * properties
//     */
//    public void TC23() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(38, 14, 1)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5));
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(75, 34, 0),
//                new Coordinate(110, 34, 0),
//                new Coordinate(110, 26, 0),
//                new Coordinate(75, 26, 0),
//                new Coordinate(75, 34, 0)}), 9);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(83, 18, 0),
//                new Coordinate(118, 18, 0),
//                new Coordinate(118, 10, 0),
//                new Coordinate(83, 10, 0),
//                new Coordinate(83, 18, 0)}), 8);
//
//        //x1
//        mesh.addTopographicPoint(new Coordinate(30, -14, 0));
//        mesh.addTopographicPoint(new Coordinate(122, -14, 0));
//        mesh.addTopographicPoint(new Coordinate(122, 45, 0));
//        mesh.addTopographicPoint(new Coordinate(30, 45, 0));
//        mesh.addTopographicPoint(new Coordinate(59.6, -9.87, 0));
//        mesh.addTopographicPoint(new Coordinate(76.84, -5.28, 10));
//        mesh.addTopographicPoint(new Coordinate(63.71, 41.16, 10));
//        mesh.addTopographicPoint(new Coordinate(46.27, 36.28, 10));
//        mesh.addTopographicPoint(new Coordinate(46.27, 36.28, 10));
//        mesh.addTopographicPoint(new Coordinate(54.68, 37.59, 10));
//        mesh.addTopographicPoint(new Coordinate(55.93, 37.93, 10));
//        mesh.addTopographicPoint(new Coordinate(59.60, -9.87, 10));
//        mesh.addTopographicPoint(new Coordinate(67.35, -6.83, 10));
//        mesh.addTopographicPoint(new Coordinate(68.68, -6.49, 10));
//        mesh.addTopographicPoint(new Coordinate(54.68, 37.59, 10));
//        mesh.addTopographicPoint(new Coordinate(55.93, 37.39, 10));
//        //x2
//        mesh.addTopographicPoint(new Coordinate(122, -14, 0));
//        mesh.addTopographicPoint(new Coordinate(122, 45, 0));
//        mesh.addTopographicPoint(new Coordinate(30, 45, 0));
//        mesh.addTopographicPoint(new Coordinate(30, -14, 0));
//        mesh.addTopographicPoint(new Coordinate(76.84, -5.28, 10));
//        mesh.addTopographicPoint(new Coordinate(63.71, 41.16, 10));
//        mesh.addTopographicPoint(new Coordinate(46.27, 36.28, 10));
//        mesh.addTopographicPoint(new Coordinate(59.60, -9.87, 10));
//        mesh.addTopographicPoint(new Coordinate(54.68, 37.59, 10));
//        mesh.addTopographicPoint(new Coordinate(55.93, 37.93, 10));
//        mesh.addTopographicPoint(new Coordinate(63.71, 41.16, 10));
//        mesh.addTopographicPoint(new Coordinate(67.35, -6.83, 10));
//        mesh.addTopographicPoint(new Coordinate(68.68, -6.49, 10));
//        mesh.addTopographicPoint(new Coordinate(76.84, -5.28, 10));
//        mesh.addTopographicPoint(new Coordinate(67.35, -6.93, 10));
//        mesh.addTopographicPoint(new Coordinate(68.68, -6.49, 10));
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 0, 400, 400, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(187.05, 25, 14), 0,energeticSum, debug);
//        String filename = "target/T23.vtk";
//        String filename2 = "target/T23.ply";
//        try {
//            writeVTK(filename, propDataOut);
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        assertEquals(true, false);
//
//    }
//
//    /**
//     * TC24 – Two buildings behind an earth-berm on flat ground with homogeneous acoustic
//     * properties – receiver position modified
//     */
//    public void TC24() throws LayerDelaunayError {
//
//        assertEquals(true, false);
//
//    }
//
//    /**
//     * TC25 – Replacement of the earth-berm by a barrier
//     */
//    public void TC25() throws LayerDelaunayError {
//
//        assertEquals(true, false);
//
//    }
//
//    /**
//     * TC26 – Road source with influence of retrodiffraction
//     */
//
//    public void TC26() throws LayerDelaunayError {
//
//        assertEquals(true, false);
//
//    }
//
//    /**
//     * TC27 Source located in flat cut with retro-diffraction
//     */
//    public void TC27() throws LayerDelaunayError {
//
//        assertEquals(true, false);
//
//    }
//
//    /**
//     * TC28 Propagation over a large distance with many buildings between source and
//     * receiver
//     */
//    @Test
//    public void TC28() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        ////////////////////////////////////////////////////////////////////////////
//        //Add road source as one point
//        List<Geometry> srclst = new ArrayList<Geometry>();
//        srclst.add(factory.createPoint(new Coordinate(0, 50, 4)));
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-1500., -1500., 0.), new Coordinate(1500., 1500., 0.));
//        //Add source sound level
//        List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
//        srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0));
//        // GeometrySoilType
//        List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
//        geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-11, 1011, -300, 300)), 0.5));
//
//        //Build query structure for sources
//        QueryGeometryStructure sourcesIndex = new QueryQuadTree();
//        int idsrc = 0;
//        for (Geometry src : srclst) {
//            sourcesIndex.appendGeometry(src, idsrc);
//            idsrc++;
//        }
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(113, 10, 0),
//                new Coordinate(127, 16, 0),
//                new Coordinate(102, 70, 0),
//                new Coordinate(88, 64, 0),
//                new Coordinate(113, 10, 0)}), 6);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(176, 19, 0),
//                new Coordinate(164, 88, 0),
//                new Coordinate(184, 91, 0),
//                new Coordinate(196, 22, 0),
//                new Coordinate(176, 19, 0)}), 10);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(250, 70, 0),
//                new Coordinate(250, 180, 0),
//                new Coordinate(270, 180, 0),
//                new Coordinate(270, 70, 0),
//                new Coordinate(250, 70, 0)}), 14);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(332, 32, 0),
//                new Coordinate(348, 126, 0),
//                new Coordinate(361, 108, 0),
//                new Coordinate(349, 44, 0),
//                new Coordinate(332, 32, 0)}), 10);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(400, 5, 0),
//                new Coordinate(400, 85, 0),
//                new Coordinate(415, 85, 0),
//                new Coordinate(415, 5, 0),
//                new Coordinate(400, 5, 0)}), 9);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(444, 47, 0),
//                new Coordinate(436, 136, 0),
//                new Coordinate(516, 143, 0),
//                new Coordinate(521, 89, 0),
//                new Coordinate(506, 87, 0),
//                new Coordinate(502, 127, 0),
//                new Coordinate(452, 123, 0),
//                new Coordinate(459, 48, 0),
//                new Coordinate(444, 47, 0)}), 12);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(773, 12, 0),
//                new Coordinate(728, 90, 0),
//                new Coordinate(741, 98, 0),
//                new Coordinate(786, 20, 0),
//                new Coordinate(773, 12, 0)}), 14);
//
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(972, 82, 0),
//                new Coordinate(979, 121, 0),
//                new Coordinate(993, 118, 0),
//                new Coordinate(986, 79, 0),
//                new Coordinate(972, 82, 0)}), 8);
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        //Retrieve Delaunay triangulation of scene
//        List<Coordinate> vert = mesh.getVertices();
//        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
//                mesh.getTriNeighbors(), mesh.getVertices());
//        // rose of favourable conditions
//        double[] favrose = new double[]{0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25};
//
//        PropagationProcessData rayData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
//                freqLvl, 1, 5, 1500, 1500, 1., 0., favrose, 0.1, 0, null, geoWithSoilTypeList, true);
//
//        ComputeRaysOut propDataOut = new ComputeRaysOut();
//        ComputeRays computeRays = new ComputeRays(rayData, propDataOut);
//
//        computeRays.initStructures();
//
//        double energeticSum[] = new double[freqLvl.size()];
//        List<PropagationDebugInfo> debug = new ArrayList<>();
//        computeRays.computeRaysAtPosition(new Coordinate(1000, 100, 1), 0,energeticSum, debug);
//        String filename = "target/T28.vtk";
//        String filename2 = "target/T28.ply";
//        try {
//            writeVTK(filename, propDataOut);
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        assertEquals(true, true);
//
//    }
//
//    /**
//     * TestPLY - Test ply
//     */
//    public void Tply() throws LayerDelaunayError {
//        GeometryFactory factory = new GeometryFactory();
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//
//        //Create obstruction test object
//        MeshBuilder mesh = new MeshBuilder();
//
//        // Add building
//        mesh.addGeometry(factory.createPolygon(new Coordinate[]{
//                new Coordinate(167.2, 39.5),
//                new Coordinate(151.6, 48.5),
//                new Coordinate(141.1, 30.3),
//                new Coordinate(156.7, 21.3),
//                new Coordinate(159.7, 26.5),
//                new Coordinate(151.0, 31.5),
//                new Coordinate(155.5, 39.3),
//                new Coordinate(164.2, 34.3),
//                new Coordinate(167.2, 39.5)}), 10);
//
//        mesh.finishPolygonFeeding(cellEnvelope);
//
//        String filename2 = "target/T_ply.ply";
//        try {
//
//            writePLY(filename2, mesh);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        assertEquals(true, false);
//    }
//
//

}