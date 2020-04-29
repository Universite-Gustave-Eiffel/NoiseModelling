package org.noise_planet.nmtutorial01;

import org.cts.crs.CRSException;
import org.cts.op.CoordinateOperationException;
import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.api.ProgressVisitor;
import org.h2gis.functions.io.geojson.GeoJsonRead;
import org.h2gis.utilities.SFSUtilities;
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

        // Convert Open Street Map data to buildings, roads and receivers
        logger.info("Extract OpenStreetMap objects");
        sql.execute(String.format("CALL OSMREAD('%s', 'MAP')", Main.class.getResource("map.osm.gz").getFile()));

        sql.execute(String.format("RUNSCRIPT FROM '%s'", Main.class.getResource("import_buildings.sql").getFile()));

        sql.execute(String.format("RUNSCRIPT FROM '%s'", Main.class.getResource("import_roads.sql").getFile()));

        sql.execute(String.format("RUNSCRIPT FROM '%s'", Main.class.getResource("import_vegetation.sql").getFile()));

        sql.execute(String.format("RUNSCRIPT FROM '%s'", Main.class.getResource("create_receivers.sql").getFile()));


        // Import MNT

        logger.info("Import digital elevation model");

        GeoJsonRead.readGeoJson(connection, Main.class.getResource("dem_lorient.geojson").getFile(), "DEM");



        // Init NoiseModelling
        PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS_RAW", "ROADS", "RECEIVERS");

        pointNoiseMap.setMaximumPropagationDistance(200.0d);
        pointNoiseMap.setSoundReflectionOrder(0);
        pointNoiseMap.setComputeHorizontalDiffraction(true);
        pointNoiseMap.setComputeVerticalDiffraction(true);
        // Building height field name
        pointNoiseMap.setHeightField("HEIGHT");
        // Import table with Snow, Forest, Grass, Pasture field polygons. Attribute G is associated with each polygon
        pointNoiseMap.setSoilTableName("SURFACE_RAW");
        // Point cloud height above sea level POINT(X Y Z)
        pointNoiseMap.setDemTable("DEM");
        // Do not propagate for low emission or far away sources.
        // error in dB
        pointNoiseMap.setMaximumError(0.1d);

        // Init custom input in order to compute more than just attenuation

        TrafficPropagationProcessDataFactory trafficPropagationProcessDataFactory = new TrafficPropagationProcessDataFactory();
        pointNoiseMap.setPropagationProcessDataFactory(trafficPropagationProcessDataFactory);
        RootProgressVisitor progressLogger = new RootProgressVisitor(1, true, 1);

        pointNoiseMap.initialize(connection, new EmptyProgressVisitor());

        // Set of already processed receivers
        Set<Long> receivers = new HashSet<>();
        ProgressVisitor progressVisitor = progressLogger.subProcess(pointNoiseMap.getGridDim()*pointNoiseMap.getGridDim());
        logger.info("start");
        long start = System.currentTimeMillis();
        System.out.println("Rec\tSource\tLevel");
        // Iterate over computation areas
        for (int i = 0; i < pointNoiseMap.getGridDim(); i++) {
            for (int j = 0; j < pointNoiseMap.getGridDim(); j++) {
                // Run ray propagation
                IComputeRaysOut out = pointNoiseMap.evaluateCell(connection, i, j, progressVisitor, receivers);
                // Return results with level spectrum for each source/receiver tuple
                if(out instanceof ComputeRaysOut) {
                    ComputeRaysOut cellStorage = (ComputeRaysOut) out;
                    exportScene(String.format("target/scene_%d_%d.kml", i, j), cellStorage.inputData.freeFieldFinder, cellStorage);
                    for(ComputeRaysOut.VerticeSL v : cellStorage.receiversAttenuationLevels) {
                        double globalDbValue = ComputeRays.wToDba(ComputeRays.sumArray(ComputeRays.dbaToW(v.value)));
                        System.out.println(String.format("%d\t%d\t%.2f", v.receiverId, v.sourceId, globalDbValue));
                    }
                }
            }
        }
        long computationTime = System.currentTimeMillis() - start;
        logger.info(String.format("Computed in %d ms, %.2f ms per receiver", computationTime,computationTime / (double)receivers.size()));

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