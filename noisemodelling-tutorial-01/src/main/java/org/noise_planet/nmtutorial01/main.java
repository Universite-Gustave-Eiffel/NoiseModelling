package org.noise_planet.nmtutorial01;

import org.cts.crs.CRSException;
import org.cts.op.CoordinateOperationException;
import org.h2.value.ValueBoolean;
import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.api.ProgressVisitor;
import org.h2gis.functions.io.csv.CSVDriverFunction;
import org.h2gis.functions.io.geojson.GeoJsonRead;
import org.h2gis.functions.io.shp.SHPWrite;
import org.h2gis.utilities.GeometryMetaData;
import org.h2gis.utilities.GeometryTableUtilities;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.TableLocation;
import org.h2gis.utilities.dbtypes.DBTypes;
import org.h2gis.utilities.dbtypes.DBUtils;
import org.noise_planet.noisemodelling.jdbc.BezierContouring;
import org.noise_planet.noisemodelling.jdbc.LDENConfig;
import org.noise_planet.noisemodelling.jdbc.LDENPointNoiseMapFactory;
import org.noise_planet.noisemodelling.jdbc.PointNoiseMap;
import org.noise_planet.noisemodelling.jdbc.TriangleNoiseMap;
import org.noise_planet.noisemodelling.pathfinder.IComputeRaysOut;
import org.noise_planet.noisemodelling.pathfinder.LayerDelaunayError;
import org.noise_planet.noisemodelling.pathfinder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.PropagationPath;
import org.noise_planet.noisemodelling.pathfinder.RootProgressVisitor;
import org.noise_planet.noisemodelling.pathfinder.utils.JVMMemoryMetric;
import org.noise_planet.noisemodelling.pathfinder.utils.KMLDocument;
import org.noise_planet.noisemodelling.pathfinder.utils.ProfilerThread;
import org.noise_planet.noisemodelling.pathfinder.utils.ProgressMetric;
import org.noise_planet.noisemodelling.pathfinder.utils.ReceiverStatsMetric;
import org.noise_planet.noisemodelling.propagation.ComputeRaysOutAttenuation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class Main {
    public final static int MAX_OUTPUT_PROPAGATION_PATHS = 50;

    public static void main(String[] args) throws SQLException, IOException, LayerDelaunayError {
        // Init output logger
        Logger logger = LoggerFactory.getLogger(Main.class);

        // Read working directory argument
        String workingDir = "target/";
        if (args.length > 0) {
            workingDir = args[0];
        }
        File workingDirPath = new File(workingDir).getAbsoluteFile();
        if(!workingDirPath.exists()) {
            if(!workingDirPath.mkdirs()) {
                logger.error(String.format("Cannot create working directory %s", workingDir));
                return;
            }
        }

        logger.info(String.format("Working directory is %s", workingDirPath.getAbsolutePath()));

        // Create spatial database named to current time
        DateFormat df = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.getDefault());

        // Open connection to database
        String dbName = new File(workingDir + "db_" + df.format(new Date())).toURI().toString();
        Connection connection = JDBCUtilities.wrapConnection(DbUtilities.createSpatialDataBase(dbName, true));
        Statement sql = connection.createStatement();

        // Import BUILDINGS

        logger.info("Import buildings");

        GeoJsonRead.importTable(connection, Main.class.getResource("buildings.geojson").getFile(), "BUILDINGS",
                ValueBoolean.TRUE);

        // Import noise source

        logger.info("Import noise source");

        GeoJsonRead.importTable(connection, Main.class.getResource("lw_roads.geojson").getFile(), "LW_ROADS",
                ValueBoolean.TRUE);
        // Set primary key
        sql.execute("ALTER TABLE LW_ROADS ALTER COLUMN PK INTEGER NOT NULL");
        sql.execute("ALTER TABLE LW_ROADS ADD PRIMARY KEY (PK)");
        sql.execute("DELETE FROM LW_ROADS WHERE PK != 102");

        // Import BUILDINGS

        logger.info("Generate receivers grid for noise map rendering");

        TriangleNoiseMap noiseMap = new TriangleNoiseMap("BUILDINGS", "LW_ROADS");

        AtomicInteger pk = new AtomicInteger(0);
        noiseMap.initialize(connection, new EmptyProgressVisitor());
        noiseMap.setGridDim(1);
        noiseMap.setMaximumArea(0);
        noiseMap.setIsoSurfaceInBuildings(false);

        for (int i = 0; i < noiseMap.getGridDim(); i++) {
            for (int j = 0; j < noiseMap.getGridDim(); j++) {
                logger.info("Compute cell " + (i * noiseMap.getGridDim() + j + 1) + " of " + noiseMap.getGridDim() * noiseMap.getGridDim());
                noiseMap.generateReceivers(connection, i, j, "RECEIVERS", "TRIANGLES", pk);
            }
        }
        // Import MNT

        logger.info("Import digital elevation model");

        GeoJsonRead.importTable(connection, Main.class.getResource("dem_lorient.geojson").getFile(), "DEM",
                ValueBoolean.TRUE);

        // Init NoiseModelling
        PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS", "LW_ROADS", "RECEIVERS");

        pointNoiseMap.setMaximumPropagationDistance(100.0);
        pointNoiseMap.setSoundReflectionOrder(0);
        pointNoiseMap.setComputeHorizontalDiffraction(false);
        pointNoiseMap.setComputeVerticalDiffraction(true);
        // Building height field name
        pointNoiseMap.setHeightField("HEIGHT");
        // Point cloud height above sea level POINT(X Y Z)
        pointNoiseMap.setDemTable("DEM");

        // Init custom input in order to compute more than just attenuation
        // LW_ROADS contain Day Evening Night emission spectrum
        LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_LW_DEN);

        ldenConfig.setComputeLDay(true);
        ldenConfig.setComputeLEvening(true);
        ldenConfig.setComputeLNight(true);
        ldenConfig.setComputeLDEN(true);
        ldenConfig.setExportRaysMethod(LDENConfig.ExportRaysMethods.TO_MEMORY);

        LDENPointNoiseMapFactory tableWriter = new LDENPointNoiseMapFactory(connection, ldenConfig);

        pointNoiseMap.setPropagationProcessDataFactory(tableWriter);
        pointNoiseMap.setComputeRaysOutFactory(tableWriter);

        RootProgressVisitor progressLogger = new RootProgressVisitor(1, true, 1);

        pointNoiseMap.initialize(connection, new EmptyProgressVisitor());

        pointNoiseMap.setGridDim(1);

        LocalDateTime now = LocalDateTime.now();
        ProfilerThread profilerThread = new ProfilerThread(new File(String.format("profile_%d_%d_%d_%dh%d.csv",
                now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour(), now.getMinute())));
        profilerThread.addMetric(tableWriter);
        profilerThread.addMetric(new ProgressMetric(progressLogger));
        profilerThread.addMetric(new JVMMemoryMetric());
        profilerThread.addMetric(new ReceiverStatsMetric());
        profilerThread.setWriteInterval(60);
        profilerThread.setFlushInterval(60);
        pointNoiseMap.setProfilerThread(profilerThread);
        // Set of already processed receivers
        Set<Long> receivers = new HashSet<>();

        logger.info("start");
        long start = System.currentTimeMillis();

        // Iterate over computation areas
        try {
            tableWriter.start();
            new Thread(profilerThread).start();
            // Fetch cell identifiers with receivers
            Map<PointNoiseMap.CellIndex, Integer> cells = pointNoiseMap.searchPopulatedCells(connection);
            ProgressVisitor progressVisitor = progressLogger.subProcess(cells.size());
            for(PointNoiseMap.CellIndex cellIndex : new TreeSet<>(cells.keySet())) {
                // Run ray propagation
                IComputeRaysOut out = pointNoiseMap.evaluateCell(connection, cellIndex.getLatitudeIndex(), cellIndex.getLongitudeIndex(), progressVisitor, receivers);
                // Export as a Google Earth 3d scene
                if (out instanceof ComputeRaysOutAttenuation) {
                    ComputeRaysOutAttenuation cellStorage = (ComputeRaysOutAttenuation) out;
                    // restrict the number of rays to export
                    List<PropagationPath> propagationPaths = new ArrayList<>();
                    for(PropagationPath p : ((ComputeRaysOutAttenuation)out).propagationPaths) {
                        if(p.getPointList().size() > 3) {
                            propagationPaths.add(p);
                            if(propagationPaths.size() > MAX_OUTPUT_PROPAGATION_PATHS) {
                                break;
                            }
                        }
                    }
                    ((ComputeRaysOutAttenuation)out).propagationPaths.clear();
                    ((ComputeRaysOutAttenuation)out).propagationPaths.addAll(propagationPaths);
                    exportScene(String.format(Locale.ROOT,"target/scene_%d_%d.kml", cellIndex.getLatitudeIndex(), cellIndex.getLongitudeIndex()), cellStorage.inputData.profileBuilder, cellStorage);
                }
            }
        } finally {
            profilerThread.stop();
            tableWriter.stop();
        }
        long computationTime = System.currentTimeMillis() - start;
        logger.info(String.format(Locale.ROOT, "Computed in %d ms, %.2f ms per receiver", computationTime,computationTime / (double)receivers.size()));


        logger.info("Create iso contours");
        int srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse("LW_ROADS", DBTypes.H2GIS));
        List<Double> isoLevels = BezierContouring.NF31_133_ISO; // default values
        GeometryMetaData m = GeometryTableUtilities.getMetaData(connection, "RECEIVERS", "THE_GEOM");
        sql.execute("ALTER TABLE " + ldenConfig.getlDenTable() +
                " ADD COLUMN THE_GEOM "+m.getSQL());
        sql.execute(" UPDATE "+ldenConfig.getlDenTable()+" SET THE_GEOM = (SELECT THE_GEOM FROM RECEIVERS R " +
                "WHERE R.PK = " + ldenConfig.getlDenTable() + ".IDRECEIVER)");
        BezierContouring bezierContouring = new BezierContouring(isoLevels, srid);
        bezierContouring.setSmoothCoefficient(0.5);
        bezierContouring.setPointTable(ldenConfig.getlDenTable());
        bezierContouring.createTable(connection);
        logger.info("Export iso contours");
        SHPWrite.exportTable(connection, "target/"+bezierContouring.getOutputTable()+".shp", bezierContouring.getOutputTable(), ValueBoolean.TRUE);
        if(JDBCUtilities.tableExists(connection,  ldenConfig.getRaysTable())) {
            SHPWrite.exportTable(connection, "target/" + ldenConfig.getRaysTable() + ".shp", ldenConfig.getRaysTable(), ValueBoolean.TRUE);
        }
    }

    public static void exportScene(String name, ProfileBuilder builder, ComputeRaysOutAttenuation result) throws IOException {
        try {
            FileOutputStream outData = new FileOutputStream(name);
            KMLDocument kmlDocument = new KMLDocument(outData);
            kmlDocument.setInputCRS("EPSG:2154");
            kmlDocument.writeHeader();
            if(builder != null) {
                kmlDocument.writeTopographic(builder.getTriangles(), builder.getVertices());
            }
            if(result != null) {
                kmlDocument.writeRays(result.getPropagationPaths());
            }
            if(builder != null) {
                kmlDocument.writeBuildings(builder);
                if(result != null && !result.getInputData().sourceGeometries.isEmpty() && !result.getInputData().receivers.isEmpty()) {
                    String layerName = "S:"+result.getInputData().sourcesPk.get(0)+" R:" + result.getInputData().receiversPk.get(0);
                    kmlDocument.writeProfile(layerName, builder.getProfile(result.getInputData().
                            sourceGeometries.get(0).getCoordinate(),result.getInputData().receivers.get(0)));
                }
            }

            kmlDocument.writeFooter();
        } catch (XMLStreamException | CoordinateOperationException | CRSException ex) {
            throw new IOException(ex);
        }
    }

}