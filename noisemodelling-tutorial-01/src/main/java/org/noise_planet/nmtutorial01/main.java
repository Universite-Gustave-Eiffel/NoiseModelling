package org.noise_planet.nmtutorial01;

import org.cts.crs.CRSException;
import org.cts.op.CoordinateOperationException;
import org.h2.value.ValueBoolean;
import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.api.ProgressVisitor;
import org.h2gis.functions.io.csv.CSVDriverFunction;
import org.h2gis.functions.io.geojson.GeoJsonRead;
import org.h2gis.utilities.GeometryTableUtilities;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.TableLocation;
import org.h2gis.utilities.dbtypes.DBTypes;
import org.noise_planet.noisemodelling.jdbc.BezierContouring;
import org.noise_planet.noisemodelling.jdbc.LDENConfig;
import org.noise_planet.noisemodelling.jdbc.LDENPointNoiseMapFactory;
import org.noise_planet.noisemodelling.jdbc.PointNoiseMap;
import org.noise_planet.noisemodelling.jdbc.TriangleNoiseMap;
import org.noise_planet.noisemodelling.pathfinder.IComputeRaysOut;
import org.noise_planet.noisemodelling.pathfinder.LayerDelaunayError;
import org.noise_planet.noisemodelling.pathfinder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.RootProgressVisitor;
import org.noise_planet.noisemodelling.pathfinder.utils.KMLDocument;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class Main {
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
        String dbName = new File(workingDir + df.format(new Date())).toURI().toString();
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

        // Import BUILDINGS

        logger.info("Generate receivers grid for noise map rendering");

        TriangleNoiseMap noiseMap = new TriangleNoiseMap("BUILDINGS", "LW_ROADS");
        noiseMap.setGridDim(1);
        noiseMap.setMaximumArea(250);
        noiseMap.setIsoSurfaceInBuildings(true);

        AtomicInteger pk = new AtomicInteger(0);
        noiseMap.initialize(connection, new EmptyProgressVisitor());
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

        pointNoiseMap.setMaximumPropagationDistance(160.0d);
        pointNoiseMap.setSoundReflectionOrder(1);
        pointNoiseMap.setComputeHorizontalDiffraction(true);
        pointNoiseMap.setComputeVerticalDiffraction(true);
        // Building height field name
        pointNoiseMap.setHeightField("HEIGHT");
        // Point cloud height above sea level POINT(X Y Z)
       // pointNoiseMap.setDemTable("DEM");
        // Do not propagate for low emission or far away sources.
        // error in dB
        pointNoiseMap.setMaximumError(0.1d);
        pointNoiseMap.setNoiseFloor(35d);

        // Init custom input in order to compute more than just attenuation
        // LW_ROADS contain Day Evening Night emission spectrum
        LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_LW_DEN);

        ldenConfig.setComputeLDay(true);
        ldenConfig.setComputeLEvening(true);
        ldenConfig.setComputeLNight(true);
        ldenConfig.setComputeLDEN(true);

        LDENPointNoiseMapFactory tableWriter = new LDENPointNoiseMapFactory(connection, ldenConfig);

        tableWriter.setKeepRays(false);

        pointNoiseMap.setPropagationProcessDataFactory(tableWriter);
        pointNoiseMap.setComputeRaysOutFactory(tableWriter);

        RootProgressVisitor progressLogger = new RootProgressVisitor(1, true, 1);

        pointNoiseMap.initialize(connection, new EmptyProgressVisitor());

        // force the creation of a 2x2 cells
        pointNoiseMap.setGridDim(1);


        // Set of already processed receivers
        Set<Long> receivers = new HashSet<>();

        logger.info("start");
        long start = System.currentTimeMillis();

        // Iterate over computation areas
        try {
            tableWriter.start();
            // Fetch cell identifiers with receivers
            Map<PointNoiseMap.CellIndex, Integer> cells = pointNoiseMap.searchPopulatedCells(connection);
            ProgressVisitor progressVisitor = progressLogger.subProcess(cells.size());
            for(PointNoiseMap.CellIndex cellIndex : new TreeSet<>(cells.keySet())) {
                // Run ray propagation
                IComputeRaysOut out = pointNoiseMap.evaluateCell(connection, cellIndex.getLatitudeIndex(), cellIndex.getLongitudeIndex(), progressVisitor, receivers);
                // Export as a Google Earth 3d scene
                if (out instanceof ComputeRaysOutAttenuation) {
                    ComputeRaysOutAttenuation cellStorage = (ComputeRaysOutAttenuation) out;
                    exportScene(String.format(Locale.ROOT,"target/scene_%d_%d.kml", cellIndex.getLatitudeIndex(), cellIndex.getLongitudeIndex()), cellStorage.inputData.profileBuilder, cellStorage);
                }
            }
        } finally {
            tableWriter.stop();
        }
        long computationTime = System.currentTimeMillis() - start;
        logger.info(String.format(Locale.ROOT, "Computed in %d ms, %.2f ms per receiver", computationTime,computationTime / (double)receivers.size()));


        logger.info("Create iso contours");
        List<Double> isoLevels = BezierContouring.NF31_133_ISO; // default values
        int srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse("LW_ROADS", DBTypes.H2GIS));
        BezierContouring bezierContouring = new BezierContouring(isoLevels, srid);
        bezierContouring.setSmoothCoefficient(0.5);
        bezierContouring.createTable(connection);
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
            }
            if(result != null) {
                kmlDocument.writeProfile(builder.getProfile(result.getInputData().sourceGeometries.get(0).getCoordinate(),result.getInputData().receivers.get(0)));
            }

            kmlDocument.writeFooter();
        } catch (XMLStreamException | CoordinateOperationException | CRSException ex) {
            throw new IOException(ex);
        }
    }

}