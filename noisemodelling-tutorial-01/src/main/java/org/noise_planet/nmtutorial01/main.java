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

        // Create spatial database
        //TimeZone tz = TimeZone.getTimeZone("UTC")
        DateFormat df = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.getDefault());

        //df.setTimeZone(tz)
        String dbName = new File(workingDir + df.format(new Date())).toURI().toString();
        Connection connection = SFSUtilities.wrapConnection(DbUtilities.createSpatialDataBase(dbName, true));
        Statement sql = connection.createStatement();

        // Evaluate receiver points using provided buildings
        logger.info("Extract OpenStreetMap buildings");
        sql.execute(String.format("CALL OSMREAD('%s', 'MAP')", Main.class.getResource("map.osm.gz").getFile()));
        sql.execute("DROP TABLE IF EXISTS MAP_BUILDINGS;");
        sql.execute("CREATE TABLE MAP_BUILDINGS(ID_WAY BIGINT PRIMARY KEY) AS SELECT DISTINCT ID_WAY \n" +
                "FROM MAP_WAY_TAG WT, MAP_TAG T \n" + "WHERE WT.ID_TAG = T.ID_TAG AND T.TAG_KEY IN ('building');\n" +
                "DROP TABLE IF EXISTS MAP_BUILDINGS_GEOM;");
        sql.execute("CREATE TABLE MAP_BUILDINGS_GEOM AS SELECT ID_WAY, \n" +
                "ST_MAKEPOLYGON(ST_MAKELINE(THE_GEOM)) THE_GEOM FROM (SELECT (SELECT \n" +
                "ST_ACCUM(THE_GEOM) THE_GEOM FROM (SELECT N.ID_NODE, N.THE_GEOM,WN.ID_WAY IDWAY FROM \n" +
                "MAP_NODE N,MAP_WAY_NODE WN WHERE N.ID_NODE = WN.ID_NODE ORDER BY \n" +
                "WN.NODE_ORDER) WHERE  IDWAY = W.ID_WAY) THE_GEOM ,W.ID_WAY FROM MAP_WAY W,MAP_BUILDINGS B \n" +
                "WHERE W.ID_WAY = B.ID_WAY) GEOM_TABLE WHERE ST_GEOMETRYN(THE_GEOM,1) = \n" +
                "ST_GEOMETRYN(THE_GEOM, ST_NUMGEOMETRIES(THE_GEOM)) AND ST_NUMGEOMETRIES(THE_GEOM) > \n" + "2;");
        sql.execute("DROP TABLE MAP_BUILDINGS;");
        sql.execute("alter table MAP_BUILDINGS_GEOM add column height double;");
        sql.execute("update MAP_BUILDINGS_GEOM set height = (select round(\"VALUE\" * 3.0 + RAND() * 2,1) from " +
                "MAP_WAY_TAG where id_tag = 152 and id_way = MAP_BUILDINGS_GEOM.id_way);");
        sql.execute("update MAP_BUILDINGS_GEOM set height = round(4 + RAND() * 2,1) where height is null;");
        sql.execute("drop table if exists BUILDINGS;");
        sql.execute("create table BUILDINGS(id_way serial, the_geom geometry, height double)" +
                " as select id_way,  ST_SimplifyPreserveTopology(st_buffer(" +
                "ST_TRANSFORM(ST_SETSRID(THE_GEOM, 4326), 2154), -0.1, 'join=mitre'),0.1) the_geom ," +
                " height from MAP_BUILDINGS_GEOM;");
    /*


 -- Filter OSM entities to keep only roads
DROP TABLE IF EXISTS MAP_ROADS;
CREATE TABLE MAP_ROADS(ID_WAY BIGINT PRIMARY KEY,HIGHWAY_TYPE varchar(30) ) AS SELECT DISTINCT ID_WAY, VALUE HIGHWAY_TYPE FROM MAP_WAY_TAG WT, MAP_TAG T
WHERE WT.ID_TAG = T.ID_TAG AND T.TAG_KEY IN ('highway');
DROP TABLE IF EXISTS MAP_ROADS_GEOM;
-- Make roads lines and convert coordinates from angle to meter
-- EPSG 32630 is UTM 30-N http://spatialreference.org/ref/epsg/wgs-84-utm-zone-30n/
CREATE TABLE MAP_ROADS_GEOM AS SELECT ID_WAY, st_updatez(ST_precisionreducer(ST_SIMPLIFYPRESERVETOPOLOGY(ST_TRANSFORM(ST_SETSRID(ST_MAKELINE(THE_GEOM), 4326), 2154),0.1),1), 0.05) THE_GEOM, HIGHWAY_TYPE T FROM (SELECT (SELECT
ST_ACCUM(THE_GEOM) THE_GEOM FROM (SELECT N.ID_NODE, N.THE_GEOM,WN.ID_WAY IDWAY FROM MAP_NODE
N,MAP_WAY_NODE WN WHERE N.ID_NODE = WN.ID_NODE ORDER BY WN.NODE_ORDER) WHERE  IDWAY = W.ID_WAY)
THE_GEOM ,W.ID_WAY, B.HIGHWAY_TYPE FROM MAP_WAY W,MAP_ROADS B WHERE W.ID_WAY = B.ID_WAY) GEOM_TABLE;
DROP TABLE MAP_ROADS;
    **/

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