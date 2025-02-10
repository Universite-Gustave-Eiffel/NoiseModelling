package org.noise_planet.nmtutorial01;

import org.h2.value.ValueBoolean;
import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.functions.io.geojson.GeoJsonRead;
import org.h2gis.functions.io.shp.SHPWrite;
import org.h2gis.utilities.GeometryTableUtilities;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.TableLocation;
import org.h2gis.utilities.dbtypes.DBTypes;
import org.h2gis.utilities.dbtypes.DBUtils;
import org.noise_planet.noisemodelling.jdbc.NoiseMapByReceiverMaker;
import org.noise_planet.noisemodelling.jdbc.input.DefaultTableLoader;
import org.noise_planet.noisemodelling.jdbc.utils.IsoSurface;
import org.noise_planet.noisemodelling.jdbc.DelaunayReceiversMaker;
import org.noise_planet.noisemodelling.pathfinder.delaunay.LayerDelaunayError;
import org.noise_planet.noisemodelling.pathfinder.utils.profiler.RootProgressVisitor;
import org.noise_planet.noisemodelling.propagation.cnossos.AttenuationCnossosParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class Main {
    public final static int MAX_OUTPUT_PROPAGATION_PATHS = 50000;

    public static void mainWithConnection(Connection connection, String workingDir)  throws SQLException, IOException, LayerDelaunayError {
        DBTypes dbType = DBUtils.getDBType(connection.unwrap(Connection.class));

        TableLocation tableLwRoads = TableLocation.parse("LW_ROADS", dbType);
        TableLocation tableBuildings = TableLocation.parse("BUILDINGS", dbType);
        TableLocation tableDemLorient = TableLocation.parse("DEM", dbType);
        String heightField = dbType.equals(DBTypes.POSTGIS) ? "height"  : "HEIGHT";

        // Init output logger
        Logger logger = LoggerFactory.getLogger(Main.class);

        Statement sql = connection.createStatement();

        // Import BUILDINGS

        logger.info("Import buildings");

        GeoJsonRead.importTable(connection, Main.class.getResource("buildings.geojson").getFile(), tableBuildings.toString(),
                ValueBoolean.TRUE);

        // Import noise source

        logger.info("Import noise source");

        GeoJsonRead.importTable(connection, Main.class.getResource("lw_roads.geojson").getFile(), tableLwRoads.toString(),
                ValueBoolean.TRUE);
        // Set primary key
        sql.execute("ALTER TABLE "+tableLwRoads+" ALTER COLUMN PK SET NOT NULL");
        sql.execute("ALTER TABLE "+tableLwRoads+" ADD PRIMARY KEY (PK)");
        sql.execute("DELETE FROM "+tableLwRoads+" WHERE PK != 102");

        // Import BUILDINGS

        logger.info("Generate receivers grid for noise map rendering");

        DelaunayReceiversMaker noiseMap = new DelaunayReceiversMaker(tableBuildings.toString(),
                tableLwRoads.toString());

        AtomicInteger pk = new AtomicInteger(0);
        noiseMap.initialize(connection, new EmptyProgressVisitor());
        noiseMap.setGridDim(1);
        noiseMap.setMaximumArea(0);
        noiseMap.setIsoSurfaceInBuildings(false);
        noiseMap.setHeightField(heightField);

        sql.execute("DROP TABLE IF EXISTS RECEIVERS;");
        sql.execute("DROP TABLE IF EXISTS TRIANGLES;");
        for (int i = 0; i < noiseMap.getGridDim(); i++) {
            for (int j = 0; j < noiseMap.getGridDim(); j++) {
                logger.info("Compute cell " + (i * noiseMap.getGridDim() + j + 1) + " of " + noiseMap.getGridDim() * noiseMap.getGridDim());
                noiseMap.generateReceivers(connection, i, j, "RECEIVERS", "TRIANGLES", pk);
            }
        }
        // Import MNT

        logger.info("Import digital elevation model");

        GeoJsonRead.importTable(connection, Main.class.getResource("dem_lorient.geojson").getFile(),
                tableDemLorient.toString(),
                ValueBoolean.TRUE);

        // Init NoiseModelling
        NoiseMapByReceiverMaker noiseMapByReceiverMaker = new NoiseMapByReceiverMaker(tableBuildings.toString(),
                tableLwRoads.toString(), "RECEIVERS");
        noiseMapByReceiverMaker.setMaximumPropagationDistance(100.0);
        noiseMapByReceiverMaker.setSoundReflectionOrder(0);
        noiseMapByReceiverMaker.setThreadCount(1);
        noiseMapByReceiverMaker.setComputeHorizontalDiffraction(false);
        noiseMapByReceiverMaker.setComputeVerticalDiffraction(true);
        noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().exportReceiverPosition = true;

        // Building height field name
        noiseMapByReceiverMaker.setHeightField(heightField);
        // Point cloud height above sea level POINT(X Y Z)
        noiseMapByReceiverMaker.setDemTable(tableDemLorient.toString());

        RootProgressVisitor progressLogger = new RootProgressVisitor(1, true, 1);

        DefaultTableLoader tableLoader = (DefaultTableLoader) noiseMapByReceiverMaker.getTableLoader();

        AttenuationCnossosParameters dayParameters = new AttenuationCnossosParameters();
        dayParameters.setTemperature(10);
        AttenuationCnossosParameters eveningParameters = new AttenuationCnossosParameters();
        eveningParameters.setTemperature(16);
        AttenuationCnossosParameters nightParameters = new AttenuationCnossosParameters();
        nightParameters.setTemperature(20);

        tableLoader.cnossosParametersPerPeriod.put("D", dayParameters);
        tableLoader.cnossosParametersPerPeriod.put("E", eveningParameters);
        tableLoader.cnossosParametersPerPeriod.put("N", nightParameters);

        noiseMapByReceiverMaker.setGridDim(1);

        noiseMapByReceiverMaker.run(connection, progressLogger);

        logger.info("Create iso contours");
        int srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse("LW_ROADS", DBTypes.H2GIS));
        List<Double> isoLevels = IsoSurface.NF31_133_ISO; // default values
        IsoSurface isoSurface = new IsoSurface(isoLevels, srid);
        isoSurface.setSmoothCoefficient(0.5);
        isoSurface.setPointTable(TableLocation.parse(noiseMapByReceiverMaker.getNoiseMapDatabaseParameters().receiversLevelTable, dbType).toString());
        isoSurface.createTable(connection, "IDRECEIVER");
        logger.info("Export iso contours");
        SHPWrite.exportTable(connection, Paths.get(workingDir, isoSurface.getOutputTable()+".shp").toString(),
                isoSurface.getOutputTable(), ValueBoolean.TRUE);
    }

    public static void main(String[] args) throws SQLException, IOException, LayerDelaunayError {
        // Init output logger
        Logger logger = LoggerFactory.getLogger(Main.class);

        // Read working directory argument
        String workingDir = "target";
        if (args.length > 0) {
            workingDir = args[0];
        }
        File workingDirPath = new File(workingDir).getAbsoluteFile();
        if(!workingDirPath.exists()) {
            if(!workingDirPath.mkdirs()) {
                logger.error("Cannot create working directory {}", workingDir);
                return;
            }
        }

        logger.info("Working directory is {}", workingDirPath.getAbsolutePath());

        // Create spatial database named to current time
        DateFormat df = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.getDefault());

        // Open connection to database
        String dbName = Paths.get(workingDir,  "db_" + df.format(new Date())).toFile().toURI().toString();
        Connection connection = JDBCUtilities.wrapConnection(DbUtilities.createSpatialDataBase(dbName, true));
        mainWithConnection(connection, workingDir);
    }

}