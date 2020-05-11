package org.noise_planet.nmtutorial01;

import org.cts.crs.CRSException;
import org.cts.op.CoordinateOperationException;
import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.api.ProgressVisitor;
import org.h2gis.functions.io.geojson.GeoJsonRead;
import org.h2gis.utilities.SFSUtilities;
import org.noise_planet.noisemodelling.emission.jdbc.LDENConfig;
import org.noise_planet.noisemodelling.emission.jdbc.LDENPointNoiseMapFactory;
import org.noise_planet.noisemodelling.propagation.*;
import org.noise_planet.noisemodelling.propagation.jdbc.PointNoiseMap;
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
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

class Main {
    public static void main(String[] args) throws SQLException, IOException {
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
        Connection connection = SFSUtilities.wrapConnection(DbUtilities.createSpatialDataBase(dbName, true));
        Statement sql = connection.createStatement();

        // Import BUILDINGS

        logger.info("Import buildings");

        GeoJsonRead.readGeoJson(connection, Main.class.getResource("buildings.geojson").getFile(), "BUILDINGS");

        // Import noise source

        logger.info("Import noise source");

        GeoJsonRead.readGeoJson(connection, Main.class.getResource("lw_roads.geojson").getFile(), "LW_ROADS");
        // Set primary key
        sql.execute("ALTER TABLE LW_ROADS ALTER COLUMN PK INTEGER NOT NULL");
        sql.execute("ALTER TABLE LW_ROADS ADD PRIMARY KEY (PK)");

        // Import BUILDINGS

        logger.info("Import evaluation coordinates");

        GeoJsonRead.readGeoJson(connection, Main.class.getResource("receivers.geojson").getFile(), "RECEIVERS");
        // Set primary key
        sql.execute("ALTER TABLE RECEIVERS ALTER COLUMN PK INTEGER NOT NULL");
        sql.execute("ALTER TABLE RECEIVERS ADD PRIMARY KEY (PK)");


        // Import MNT

        logger.info("Import digital elevation model");

        GeoJsonRead.readGeoJson(connection, Main.class.getResource("dem_lorient.geojson").getFile(), "DEM");

        // Init NoiseModelling
        PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS", "LW_ROADS", "RECEIVERS");

        pointNoiseMap.setMaximumPropagationDistance(500.0d);
        pointNoiseMap.setSoundReflectionOrder(0);
        pointNoiseMap.setComputeHorizontalDiffraction(true);
        pointNoiseMap.setComputeVerticalDiffraction(true);
        // Building height field name
        pointNoiseMap.setHeightField("HEIGHT");
        // Point cloud height above sea level POINT(X Y Z)
        pointNoiseMap.setDemTable("DEM");
        // Do not propagate for low emission or far away sources.
        // error in dB
        pointNoiseMap.setMaximumError(0.1d);

        // Init custom input in order to compute more than just attenuation
        // LW_ROADS contain Day Evening Night emission spectrum
        LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_LW_DEN);

        LDENPointNoiseMapFactory tableWriter = new LDENPointNoiseMapFactory(connection, ldenConfig);

        tableWriter.setKeepRays(true);

        pointNoiseMap.setPropagationProcessDataFactory(tableWriter);
        pointNoiseMap.setComputeRaysOutFactory(tableWriter);

        RootProgressVisitor progressLogger = new RootProgressVisitor(1, true, 1);

        pointNoiseMap.initialize(connection, new EmptyProgressVisitor());

        // force the creation of a 2x2 cells
        pointNoiseMap.setGridDim(2);


        // Set of already processed receivers
        Set<Long> receivers = new HashSet<>();
        ProgressVisitor progressVisitor = progressLogger.subProcess(pointNoiseMap.getGridDim()*pointNoiseMap.getGridDim());
        logger.info("start");
        long start = System.currentTimeMillis();

        // Iterate over computation areas
        try {
            tableWriter.start();
            for (int i = 0; i < pointNoiseMap.getGridDim(); i++) {
                for (int j = 0; j < pointNoiseMap.getGridDim(); j++) {
                    // Run ray propagation
                    IComputeRaysOut out = pointNoiseMap.evaluateCell(connection, i, j, progressVisitor, receivers);
                    // Export as a Google Earth 3d scene
                    if (out instanceof ComputeRaysOut) {
                        ComputeRaysOut cellStorage = (ComputeRaysOut) out;
                        exportScene(String.format(Locale.ROOT,"target/scene_%d_%d.kml", i, j), cellStorage.inputData.freeFieldFinder, cellStorage);
                    }
                }
            }
        } finally {
            tableWriter.stop();
        }
        long computationTime = System.currentTimeMillis() - start;
        logger.info(String.format(Locale.ROOT, "Computed in %d ms, %.2f ms per receiver", computationTime,computationTime / (double)receivers.size()));

    }


    public static void exportScene(String name, FastObstructionTest manager, ComputeRaysOut result) throws IOException {
        try {
            FileOutputStream outData = new FileOutputStream(name);
            KMLDocument kmlDocument = new KMLDocument(outData);
            kmlDocument.setInputCRS("EPSG:2154");
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
}