package org.noise_planet.nmtutorial01;

import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.api.ProgressVisitor;
import org.h2gis.functions.io.shp.SHPRead;
import org.h2gis.utilities.SFSUtilities;
import org.noise_planet.noisemodelling.propagation.IComputeRaysOut;
import org.noise_planet.noisemodelling.propagation.RootProgressVisitor;
import org.noise_planet.noisemodelling.propagation.jdbc.PointNoiseMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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
    static void main(String[] args) throws SQLException, IOException {
        // Read working directory argument
        String dataFolder = "datafull/";
        String workingDir = "";
        if (args.length > 0) {
            workingDir = args[0];
        }

        // Init output logger
        Logger logger = LoggerFactory.getLogger(Main.class);
        logger.info(String.format("Working directory is %s", new File(workingDir).getAbsolutePath()));


        // Create spatial database
        //TimeZone tz = TimeZone.getTimeZone("UTC")
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

        //df.setTimeZone(tz)
        String dbName = new File(workingDir + df.format(new Date())).toURI().toString();
        Connection connection = SFSUtilities.wrapConnection(DbUtilities.createSpatialDataBase(dbName, true));
        Statement sql = connection.createStatement();

        // Evaluate receiver points using provided buildings

        sql.execute("DROP TABLE IF EXISTS BUILDINGS");

        logger.info("Read building file");
        SHPRead.readShape(connection, dataFolder+"buildings.shp", "BUILDINGS");
        SHPRead.readShape(connection, dataFolder+"study_area.shp", "STUDY_AREA");
        sql.execute("CREATE SPATIAL INDEX ON BUILDINGS(THE_GEOM)");
        logger.info("Building file loaded");

        // Load or create receivers points
        SHPRead.readShape(connection, dataFolder+"receivers_sel.shp", "RECEIVERS");
        sql.execute("CREATE SPATIAL INDEX ON RECEIVERS(THE_GEOM)");

        // Load roads
        logger.info("Read road geometries and traffic");
        SHPRead.readShape(connection, dataFolder+"troncon2012.shp", "ROADS");
        sql.execute("CREATE SPATIAL INDEX ON ROADS(THE_GEOM)");
        logger.info("Road file loaded");

        // Load ground type
        logger.info("Read ground surface categories");
        SHPRead.readShape(connection, dataFolder+"ground_type.shp", "GROUND_TYPE");
        sql.execute("CREATE SPATIAL INDEX ON GROUND_TYPE(THE_GEOM)");
        logger.info("Surface categories file loaded");

        // Init NoiseModelling
        PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS", "ROADS", "RECEIVERS");
        //pointNoiseMap.setSoilTableName("GROUND_TYPE")
        pointNoiseMap.setMaximumPropagationDistance(750.0d);
        pointNoiseMap.setSoundReflectionOrder(1);
        pointNoiseMap.setComputeHorizontalDiffraction(true);
        pointNoiseMap.setComputeVerticalDiffraction(true);
        pointNoiseMap.setHeightField("HAUTEUR");
        pointNoiseMap.setThreadCount(3); // Use 4 cpu threads
        pointNoiseMap.setMaximumError(0.1d);
        PropagationPathStorageFactory storageFactory = new PropagationPathStorageFactory();
        TrafficPropagationProcessDataFactory trafficPropagationProcessDataFactory = new TrafficPropagationProcessDataFactory();
        pointNoiseMap.setPropagationProcessDataFactory(trafficPropagationProcessDataFactory);
        pointNoiseMap.setComputeRaysOutFactory(storageFactory);
        storageFactory.setWorkingDir(new File(workingDir).getAbsolutePath());
        try {
            storageFactory.openPathOutputFile(new File(workingDir, "rays.gz").getAbsolutePath());
            RootProgressVisitor progressLogger = new RootProgressVisitor(1, true, 1);
            pointNoiseMap.initialize(connection, new EmptyProgressVisitor());
            // Set of already processed receivers
            Set<Long> receivers = new HashSet<>();
            ProgressVisitor progressVisitor = progressLogger.subProcess(pointNoiseMap.getGridDim()*pointNoiseMap.getGridDim());
            for (int i = 0; i < pointNoiseMap.getGridDim(); i++) {
                for (int j = 0; j < pointNoiseMap.getGridDim(); j++) {
                    IComputeRaysOut out = pointNoiseMap.evaluateCell(connection, i, j, progressVisitor, receivers);
                }
            }
        } finally {
            storageFactory.closeWriteThread();
        }
    }
}