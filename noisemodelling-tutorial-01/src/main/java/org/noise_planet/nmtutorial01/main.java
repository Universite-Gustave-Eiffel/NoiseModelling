package org.noise_planet.nmtutorial01;

import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.api.ProgressVisitor;
import org.h2gis.functions.io.shp.SHPRead;
import org.h2gis.utilities.SFSUtilities;
import org.noise_planet.noisemodelling.propagation.ComputeRays;
import org.noise_planet.noisemodelling.propagation.ComputeRaysOut;
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

        // Convert Open Street Map data to buildings, roads and receivers
        logger.info("Extract OpenStreetMap buildings");
        sql.execute(String.format("CALL OSMREAD('%s', 'MAP')", Main.class.getResource("map.osm.gz").getFile()));

        sql.execute(String.format("RUNSCRIPT FROM '%s'", Main.class.getResource("import_buildings.sql").getFile()));

        sql.execute(String.format("RUNSCRIPT FROM '%s'", Main.class.getResource("import_roads.sql").getFile()));

        sql.execute(String.format("RUNSCRIPT FROM '%s'", Main.class.getResource("create_receivers.sql").getFile()));

        // Init NoiseModelling
        PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS_RAW", "ROADS", "RECEIVERS");
        // Ground surface category
        //pointNoiseMap.setSoilTableName("GROUND_TYPE")
        // Digital Elevation Model ( x,y,z Point cloud)
        //pointNoiseMap.setDemTable("DEM");
        pointNoiseMap.setMaximumPropagationDistance(750.0d);
        pointNoiseMap.setSoundReflectionOrder(0);
        pointNoiseMap.setComputeHorizontalDiffraction(true);
        pointNoiseMap.setComputeVerticalDiffraction(true);
        // Building height field name
        pointNoiseMap.setHeightField("HEIGHT");
        // Do not propagate for low emission or far away sources.
        // error in dB
        pointNoiseMap.setMaximumError(0.1d);
        PropagationPathStorageFactory storageFactory = new PropagationPathStorageFactory();
        TrafficPropagationProcessDataFactory trafficPropagationProcessDataFactory = new TrafficPropagationProcessDataFactory();
        pointNoiseMap.setPropagationProcessDataFactory(trafficPropagationProcessDataFactory);
        pointNoiseMap.setComputeRaysOutFactory(storageFactory);
        storageFactory.setWorkingDir(new File(workingDir).getAbsolutePath());

        RootProgressVisitor progressLogger = new RootProgressVisitor(1, true, 1);

        pointNoiseMap.initialize(connection, new EmptyProgressVisitor());

        // Set of already processed receivers
        Set<Long> receivers = new HashSet<>();
        ProgressVisitor progressVisitor = progressLogger.subProcess(pointNoiseMap.getGridDim()*pointNoiseMap.getGridDim());
        logger.info("start");
        long start = System.currentTimeMillis();
        logger.info("Rec\tSource\tLevel");
        for (int i = 0; i < pointNoiseMap.getGridDim(); i++) {
            for (int j = 0; j < pointNoiseMap.getGridDim(); j++) {
                IComputeRaysOut out = pointNoiseMap.evaluateCell(connection, i, j, progressVisitor, receivers);
                if(out instanceof PropagationPathStorage) {
                    PropagationPathStorage cellStorage = (PropagationPathStorage) out;
                    for(ComputeRaysOut.verticeSL v : cellStorage.receiversAttenuationLevels) {
                        double globalDbValue = ComputeRays.wToDba(ComputeRays.sumArray(ComputeRays.dbaToW(v.value)));
                        logger.info(String.format("%d\t%d\t%.2f", v.receiverId, v.sourceId, globalDbValue));
                    }
                }
            }
        }
        long computationTime = System.currentTimeMillis() - start;
        logger.info(String.format("Computed in %d ms, %.2f ms per receiver", computationTime,computationTime / (double)receivers.size()));

    }
}