/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */

/**
 * @Author Pierre Aumond, Université Gustave Eiffel
 * @Author Paul Chapron, IGN
 */


package org.noise_planet.noisemodelling.wps.Source_Activity


import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.TableLocation
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.Point
import org.noise_planet.noisemodelling.pathfinder.RootProgressVisitor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection

title = 'Pedestrian localisation '

description = '&#10145;&#65039; Locate some pedestrian in the city thanks to a walkable area polygon and a PointsOfInterests layer<br><br>' +
        'The following output tables will be created: <br>' +
        '- <b> PEDESTRIANS </b>: a table containing the pedestrians in their corresponding areas'


inputs = [
        walkableArea        : [
                name       : 'walkableArea',
                title      : 'walkableArea',
                description: 'Walkable area polygon generated in the Import_OSM_Pedestrian script',
                type       : String.class
        ],
        cellSize            : [
                name       : 'cellSize',
                title      : 'cellSize',
                description: 'Size of the grid cell used to perform the spatial density analysis KDE',
                type       : Double.class,
                min        : 0, max: 1
        ],
        pointsOfInterests   : [
                name       : 'PointsOfInterests',
                description: 'Layer containing the points of interest in the study area issued from the Import_OSM_Pedestrian script ',
                title      : 'PointsOfInterests',
                type       : String.class
        ],
        bandwidthKDE        : [
                name       : 'bandwidthKDE',
                description: 'Bandwidth to be used in the KDE analysis. This will modify the extent to which the KDE will search for the points of interest and therefore their density.<br> ' +
                        'This is an optional parameter.',
                title      : 'bandwidthKDE',
                type       : Double.class,
                min        : 0, max: 1
        ],
        coeffLeisure        : [
                name       : 'coeffLeisure',
                description: 'Weight/coefficient of the Leisure points of interest in the linear regression.<br> ' +
                        'This is an optional parameter.',
                title      : 'coeffLeisure',
                type       : Double.class,
                min        : 0, max: 1
        ],
        coeffCulture        : [
                name       : 'coeffCulture',
                description: 'Weight/coefficient of the Culture points of interest in the linear regression.<br> ' +
                        'This is an optional parameter.',
                title      : 'coeffCulture',
                type       : Double.class,
                min        : 0, max: 1
        ],
        coeffFoodDrink      : [
                name       : 'coeffFoodDrink',
                description: 'Weight/coefficient of the Food_Drink points of interest in the linear regression.<br> ' +
                        'This is an optional parameter.',
                title      : 'coeffFoodDrink',
                type       : Double.class,
                min        : 0, max: 1
        ],
        coeffEducation      : [
                name       : 'coeffEducation',
                description: 'Weight/coefficient of the Education points of interest in the linear regression.<br> ' +
                        'This is an optional parameter.',
                title      : 'coeffEducation',
                type       : Double.class,
                min        : 0, max: 1
        ],
        coeffFootpath       : [
                name       : 'coeffFootpath',
                description: 'Weight/coefficient of the Footpath points of interest in the linear regression.<br> ' +
                        'This is an optional parameter.',
                title      : 'coeffFootpath',
                type       : Double.class,
                min        : 0, max: 1
        ],
        coeffTourismSleep   : [
                name       : 'coeffTourismSleep',
                description: 'Weight/coefficient of the Tourism_Sleep points of interest in the linear regression.<br> ' +
                        'This is an optional parameter.',
                title      : 'coeffTourismSleep',
                type       : Double.class,
                min        : 0, max: 1
        ],
        coeffPublicTransport: [
                name       : 'coeffPublicTransport',
                description: 'Weight/coefficient of the Public_Transport points of interest in the linear regression.<br> ' +
                        'This is an optional parameter.',
                title      : 'coeffPublicTransport',
                type       : Double.class,
                min        : 0, max: 1
        ],
        coeffReligion       : [
                name       : 'coeffReligion',
                description: 'Weight/coefficient of the Religion points of interest in the linear regression.<br> ' +
                        'This is an optional parameter.',
                title      : 'coeffReligion',
                type       : Double.class,
                min        : 0, max: 1
        ],
        coeffTourism        : [
                name       : 'coeffTourism',
                description: 'Weight/coefficient of the Tourism points of interest in the linear regression.<br> ' +
                        'This is an optional parameter.',
                title      : 'coeffTourism',
                type       : Double.class,
                min        : 0, max: 1
        ],
        coeffShop           : [
                name       : 'coeffShop',
                description: 'Weight/coefficient of the Shop points of interest in the linear regression.<br> ' +
                        'This is an optional parameter.',
                title      : 'coeffShop',
                type       : Double.class,
                min        : 0, max: 1
        ],
        coeffSport          : [
                name       : 'coeffSport',
                description: 'Weight/coefficient of the Sport points of interest in the linear regression.<br> ' +
                        'This is an optional parameter.',
                title      : 'coeffSport',
                type       : Double.class,
                min        : 0, max: 1
        ],
        coeffTrees          : [
                name       : 'coeffTrees',
                description: 'Weight/coefficient of the Trees points of interest in the linear regression.<br> ' +
                        'This is an optional parameter.',
                title      : 'coeffTrees',
                type       : Double.class,
                min        : 0, max: 1
        ],
        coeffIntercept          : [
                name       : 'coeffIntercept',
                description: ' intercept in the linear regression.<br> ' +
                        'This is an optional parameter.',
                title      : 'coeffIntercept',
                type       : Double.class,
                min        : 0, max: 1
        ],
        coeffIndTransport   : [
                name       : 'coeffIndTransport',
                description: 'Weight/coefficient of the Individual_Transport points of interest in the linear regression.<br> ' +
                        'This is an optional parameter.',
                title      : 'coeffIndTransport',
                type       : Double.class,
                min        : 0, max: 1
        ]
]

outputs = [
        result: [
                name       : 'Result output string',
                title      : 'Result output string',
                description: 'This type of result does not allow the blocks to be linked together.',
                type       : String.class
        ]
]

static Connection openGeoserverDataStoreConnection(String dbName) {
    if (dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}


def run(input) {

    // Get name of the database
    // by default an embedded h2gis database is created
    // Advanced user can replace this database for a postGis or h2Gis server database.
    String dbName = "h2gisdb"

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable {
        Connection connection ->
            return [result: exec(connection, input)]
    }
}


def exec(connection, input) {

    // output string, the information given back to the user
    String resultString = null


    // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    // print to command window
    logger.info('Start : Sampling Pietons')
    logger.info("inputs {}", input) // log inputs of the run

    // Defining input variables
    Double coeffLeisure = 1
    if (input['coeffLeisure']) {
        coeffLeisure = input['coeffLeisure']
    }

    Double coeffCulture = 1
    if (input['coeffCulture']) {
        coeffCulture = input['coeffCulture']
    }

    Double coeffFoodDrink = 1
    if (input['coeffFoodDrink']) {
        coeffFoodDrink = input['coeffFoodDrink']
    }

    Double coeffEducation = 1
    if (input['coeffEducation']) {
        coeffEducation = input['coeffEducation']
    }

    Double coeffFootpath = 1
    if (input['coeffFootpath']) {
        coeffFootpath = input['coeffFootpath']
    }

    Double coeffTourismSleep = 1
    if (input['coeffTourismSleep']) {
        coeffTourismSleep = input['coeffTourismSleep']
    }

    Double coeffPublicTransport = 1
    if (input['coeffPublicTransport']) {
        coeffPublicTransport = input['coeffPublicTransport']
    }

    Double coeffReligion = 1
    if (input['coeffReligion']) {
        coeffReligion = input['coeffReligion']
    }

    Double coeffTourism = 1
    if (input['coeffTourism']) {
        coeffTourism = input['coeffTourism']
    }

    Double coeffShop = 1
    if (input['coeffShop']) {
        coeffShop = input['coeffShop']
    }

    Double coeffSport = 1
    if (input['coeffSport']) {
        coeffSport = input['coeffSport']
    }

    Double coeffTrees = 1
    if (input['coeffTrees']) {
        coeffTrees = input['coeffTrees']
    }
    Double coeffIntercept = 1
    if (input['coeffIntercept']) {
        coeffIntercept = input['coeffIntercept']
    }


    Double coeffIndTransport = 1
    if (input['coeffIndTransport']) {
        coeffIndTransport = input['coeffIndTransport']
    }


    String walkableArea = "PEDESTRIAN_AREA"
    if (input['walkableArea']) {
        walkableArea = input['walkableArea']
    }
    walkableArea = walkableArea.toUpperCase()


    Double cellSize = 2
    if (input['cellSize']) {
        cellSize = input['cellSize']
    }

    Double bwKDE = 150
    if (input['bandwidthKDE']) {
        bwKDE = input['bandwidthKDE']
    }
    logger.info("bandwidthKDE")

    String pointsOfInterests = "PEDESTRIAN_POIS"
    if (input['pointsOfInterests']) {
        pointsOfInterests = input['pointsOfInterests']
    }
    pointsOfInterests = pointsOfInterests.toUpperCase()

    Sql sql = new Sql(connection)


    int srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(pointsOfInterests))
    logger.info("SRID de la couche de lines" + srid)

    int poly_srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(walkableArea))
    logger.info("SRID de la couche de polygones" + poly_srid)

    /** Centroids in cells upon a polygon collection **/
    logger.info("#########################")
    logger.info("RASTERIZE...")

    def table_exists = sql.firstRow("SELECT COUNT (*) FROM information_schema.TABLES WHERE TABLE_NAME = ('CELLGRID_ON_AREA');")
    if (table_exists[0] == 1) {
        logger.info("Table CELLGRID_ON_AREA already exists...")
    } else {
        sql.execute("DROP TABLE CELLGRID IF EXISTS;")
        sql.execute("CREATE TABLE CELLGRID AS SELECT * FROM ST_MakeGrid(\'" + walkableArea + "\' , " + cellSize + " , " + cellSize + ");")
        //sql.execute("CREATE TABLE CELLGRID AS SELECT * FROM ST_MakeGrid('"+walkableArea+"', " +cellSize+ ", " +cellSize+");")
        sql.execute("CREATE SPATIAL INDEX ON CELLGRID(the_geom);")
        sql.execute("CREATE SPATIAL INDEX ON " + walkableArea + "(the_geom);")

        sql.execute("DROP TABLE CELLGRID_ON_AREA IF EXISTS ;")
        sql.execute("CREATE TABLE CELLGRID_ON_AREA AS SELECT c.id pk,  ST_ACCUM(ST_Intersection(zem.the_geom,c.the_geom)) the_geom FROM " + walkableArea + " zem , CELLGRID c WHERE ST_intersects(c.the_geom,zem.the_geom) AND c.the_geom && zem.the_geom GROUP BY c.id ;")

        sql.execute("DROP TABLE CELLGRID IF EXISTS ;")

    }

    /** KDE computation**/
    table_exists = sql.firstRow("SELECT COUNT (*) FROM information_schema.TABLES WHERE TABLE_NAME = ('POIS_DENSITY');")
    if (table_exists[0] == 1) {
        logger.info("Density table POIS_DENSITY already exists!")

        sql.execute("DROP TABLE POIS_DENSITY_TEMP IF EXISTS;")
        sql.execute("CREATE TABLE POIS_DENSITY_TEMP (pk INTEGER,the_geom GEOMETRY,density FLOAT,leisure_density FLOAT,culture_density FLOAT,foodDrink_density FLOAT,education_density FLOAT,footpath_density FLOAT,tourismSleep_density FLOAT,publicTransport_density FLOAT,religion_density FLOAT,tourism_density FLOAT,shop_density FLOAT,sport_density FLOAT,trees_density FLOAT,indTransport_density FLOAT) ;")

        // Insert data into the 'POIS_DENSITY' table using placeholders '?'
        def qry_add_density_values = 'INSERT INTO POIS_DENSITY_TEMP (pk,the_geom,density,leisure_density,culture_density,foodDrink_density,education_density,footpath_density,tourismSleep_density,publicTransport_density,religion_density,tourism_density,shop_density,sport_density,trees_density,indTransport_density) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);'
        // Batch insert operation using the SQL query defined above. It batches the insert operations in groups of 3. Why 3?
        sql.withBatch(3, qry_add_density_values) { ps2 ->
            // Retrieve rows with columns 'pk' and 'the_geom' from the 'POIS_DENSITY' table
            sql.eachRow("SELECT pk,the_geom,density,leisure_density,culture_density,foodDrink_density,education_density,footpath_density,tourismSleep_density,publicTransport_density,religion_density,tourism_density,shop_density,sport_density,trees_density,indTransport_density FROM  POIS_DENSITY ") { row ->
                // Extract the current 'pk' value as an integer
                int pk = row[0] as Integer
                // Extract current 'the_geom' value as a geometry object (polygon)
                Geometry the_poly = row[1] as Geometry
                double leisure_density = row[3] as Double
                double culture_density = row[4] as Double
                double foodDrink_density = row[5] as Double
                double education_density = row[6] as Double
                double footpath_density = row[7] as Double
                double tourismSleep_density = row[8] as Double
                double publicTransport_density = row[9] as Double
                double religion_density = row[10] as Double
                double tourism_density = row[11] as Double
                double shop_density = row[12] as Double
                double sport_density = row[13] as Double
                double trees_density = row[14] as Double
                double indTransport_density = row[15] as Double
                double density = coeffLeisure * leisure_density + coeffCulture * culture_density + coeffFoodDrink * foodDrink_density
                +coeffEducation * education_density + coeffFootpath * footpath_density + coeffTourismSleep * tourismSleep_density
                +coeffPublicTransport * publicTransport_density + coeffReligion * religion_density + coeffTourism * tourism_density
                +coeffShop * shop_density + coeffSport * sport_density + coeffTrees * trees_density + coeffIndTransport * indTransport_density

                //logger.info("here density is " + density)
                // Add the values 'pk', 'the_poly' and 'density' to the batch for insertion
                ps2.addBatch(pk, the_poly, density, leisure_density, culture_density, foodDrink_density, education_density, footpath_density, tourismSleep_density, publicTransport_density, religion_density,
                        tourism_density, shop_density, sport_density, trees_density, indTransport_density)
            }
            //  densityprogressLogger.endStep()
        }

        sql.execute("DROP TABLE POIS_DENSITY IF EXISTS;")
        sql.execute("CREATE TABLE POIS_DENSITY AS SELECT * FROM POIS_DENSITY_TEMP;")
        sql.execute("DROP TABLE POIS_DENSITY_TEMP IF EXISTS;")

    } else {

        logger.info("#########################")
        logger.info("POINTS OF INTERESTS")

        // Retrieve the count of records in the table specified by the 'pointsOfInterests' variable where TYPE is 'food_drink'
        // Result s assigned to 'food_drink_count' as integer
        int food_drink_count = sql.firstRow('SELECT COUNT(*) FROM ' + pointsOfInterests + ' WHERE \'TYPE\' = \'food_drink\'')[0] as Integer
        // Create a new ArrayList named 'poi_food_drink' to store Point objects
        List<Point> poi_leisure = new ArrayList<Point>()
        List<Point> poi_culture = new ArrayList<Point>()
        List<Point> poi_food_drink = new ArrayList<Point>()
        List<Point> poi_education = new ArrayList<Point>()
        List<Point> poi_footpath = new ArrayList<Point>()
        List<Point> poi_tourism_sleep = new ArrayList<Point>()
        List<Point> poi_public_transport = new ArrayList<Point>()
        List<Point> poi_religion = new ArrayList<Point>()
        List<Point> poi_tourism = new ArrayList<Point>()
        List<Point> poi_shop = new ArrayList<Point>()
        List<Point> poi_sport = new ArrayList<Point>()
        List<Point> poi_trees = new ArrayList<Point>()
        List<Point> poi_individual_transport = new ArrayList<Point>()
        // Progress tracking with 'food_drink_count' as the total number of steps, 'true' for logging and 1 for the step increment
        RootProgressVisitor KDEprogressLogger = new RootProgressVisitor(food_drink_count, true, 1)
        // Retrieve records with columns 'pk' and 'the_geom' from the table specified by the 'pointsOfInterests' variable
        // Iteration over each returned row by the query and processes it by using the closure defined inside the curly braces
        sql.eachRow("SELECT pk, the_geom, type from " + pointsOfInterests) { row ->
            // Extract the geometry from the second column of the current row and cast it to a geometry object
            def geom = row[1] as Geometry
            def type = row[2] as String
            // Is the extracted geometry an instance of 'Point'? If it is, add it to the 'poi_food_drink' list
            if (geom instanceof Point) {

                switch (type) {
                    case "leisure":
                        poi_leisure.add(geom)
                        break

                    case "culture":
                        poi_culture.add(geom)
                        break

                    case "food_drink":
                        poi_food_drink.add(geom)
                        break

                    case "education":
                        poi_education.add(geom)
                        break

                    case "footpath":
                        poi_footpath.add(geom)
                        break

                    case "tourism_sleep":
                        poi_tourism_sleep.add(geom)
                        break

                    case "public_transport":
                        poi_public_transport.add(geom)
                        break

                    case "religion":
                        poi_religion.add(geom)
                        break

                    case "tourism":
                        poi_tourism.add(geom)
                        break

                    case "shop":
                        poi_shop.add(geom)
                        break

                    case "sport":
                        poi_sport.add(geom)
                        break

                    case "trees":
                        poi_trees.add(geom)
                        break

                    case "individual_transport":
                        poi_individual_transport.add(geom)
                        break
                }

            }
            // Completion of the current step in the progress tracking
            KDEprogressLogger.endStep()
        }

        sql.execute("DROP TABLE POIS_DENSITY IF EXISTS;")
        sql.execute("CREATE TABLE POIS_DENSITY (pk INTEGER,the_geom GEOMETRY,density FLOAT,leisure_density FLOAT,culture_density FLOAT,foodDrink_density FLOAT,education_density FLOAT,footpath_density FLOAT,tourismSleep_density FLOAT,publicTransport_density FLOAT,religion_density FLOAT,tourism_density FLOAT,shop_density FLOAT,sport_density FLOAT,trees_density FLOAT,indTransport_density FLOAT) ;")

        logger.info("(empty) Density table POIS_DENSITY created")

        RootProgressVisitor densityprogressLogger = new RootProgressVisitor(food_drink_count, true, 1)

        // Insert data into the 'POIS_DENSITY' table using placeholders '?'
        def qry_add_density_values = 'INSERT INTO POIS_DENSITY (pk,the_geom,density,leisure_density,culture_density,foodDrink_density,education_density,footpath_density,tourismSleep_density,publicTransport_density,religion_density,tourism_density,shop_density,sport_density,trees_density,indTransport_density) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);'
        // Batch insert operation using the SQL query defined above. It batches the insert operations in groups of 3. Why 3?
        sql.withBatch(3, qry_add_density_values) { ps ->
            // Retrieve rows with columns 'pk' and 'the_geom' from the 'CELLGRID_ON_AREA' table
            sql.eachRow("SELECT pk, the_geom FROM  CELLGRID_ON_AREA ") { row ->
                // Extract the current 'pk' value as an integer
                int pk = row[0] as Integer
                // Extract current 'the_geom' value as a geometry object (polygon)
                Geometry the_poly = row[1] as Geometry
                // Calculate the centroid of the bounding envelope of the geometry
                Point centroid = the_poly.getEnvelope().getCentroid()
                // Uses a function called 'densityChatGPT' that is defined later in the code to calculate the density of POIs in the geometry
                double leisure_density = densityChatGPT(bwKDE, centroid, poi_leisure)
                double culture_density = densityChatGPT(bwKDE, centroid, poi_culture)
                double foodDrink_density = densityChatGPT(bwKDE, centroid, poi_food_drink)
                double education_density = densityChatGPT(bwKDE, centroid, poi_education)
                double footpath_density = densityChatGPT(bwKDE, centroid, poi_footpath)
                double tourismSleep_density = densityChatGPT(bwKDE, centroid, poi_tourism_sleep)
                double publicTransport_density = densityChatGPT(bwKDE, centroid, poi_public_transport)
                double religion_density = densityChatGPT(bwKDE, centroid, poi_religion)
                double tourism_density = densityChatGPT(bwKDE, centroid, poi_tourism)
                double shop_density = densityChatGPT(bwKDE, centroid, poi_shop)
                double sport_density = densityChatGPT(bwKDE, centroid, poi_sport)
                double trees_density = densityChatGPT(bwKDE, centroid, poi_trees)
                double indTransport_density = densityChatGPT(bwKDE, centroid, poi_individual_transport)

                double density = coeffLeisure * leisure_density + coeffCulture * culture_density + coeffFoodDrink * foodDrink_density
                +coeffEducation * education_density + coeffFootpath * footpath_density + coeffTourismSleep * tourismSleep_density
                +coeffPublicTransport * publicTransport_density + coeffReligion * religion_density + coeffTourism * tourism_density
                +coeffShop * shop_density + coeffSport * sport_density + coeffTrees * trees_density + coeffIndTransport * indTransport_density + coeffIntercept

                //logger.info("here density is " + density)
                // Add the values 'pk', 'the_poly' and 'density' to the batch for insertion
                ps.addBatch(pk, the_poly, density, leisure_density, culture_density, foodDrink_density, education_density, footpath_density, tourismSleep_density, publicTransport_density, religion_density,
                        tourism_density, shop_density, sport_density, trees_density, indTransport_density)
            }
            densityprogressLogger.endStep()
        }
    }
    // Retrieve the sum of density values from the 'POIS_DENSITY' table and store in 'sum_densities' as a double
    double sum_densities = sql.firstRow('SELECT SUM(density) FROM POIS_DENSITY')[0] as Double

    // Explode polygons in order to calculate density in each polygon
    sql.execute("DROP TABLE POIS_DENSITY_POLYGONS IF EXISTS;")
    sql.execute("CREATE TABLE POIS_DENSITY_POLYGONS AS SELECT * FROM ST_EXPLODE(\'POIS_DENSITY\');")

    // Retrieve the POIs in each polygon and store
    // Calculate the probability for each point within the polygon. Multiply the density value associated with each polygon by the area of the polygon
    // Probability of a pedestrian being present in each specific area
    sql.execute("DROP TABLE PEDESTRIANS_PROBABILITY IF EXISTS;")
    sql.execute("CREATE TABLE PEDESTRIANS_PROBABILITY AS SELECT ST_POINTONSURFACE(the_geom) the_geom , density * ST_AREA(the_geom) probability FROM POIS_DENSITY_POLYGONS ORDER BY THE_GEOM ;")
    sql.execute("ALTER TABLE PEDESTRIANS_PROBABILITY ADD PK INT AUTO_INCREMENT PRIMARY KEY;")
    sql.execute("DROP TABLE POIS_DENSITY_POLYGONS IF EXISTS;")


    logger.info("somme des densités" + sum_densities)

    logger.info("#########################")
    logger.info("It is the time to sample")

    // Sampling
    sql.execute("DROP TABLE PEDESTRIANS IF EXISTS;")
    // Calculate the number of pedestrians 'nbPedestrian' in each area based on the 'probability' value for each area. The GREATEST function ensures that there is at least one pedestrian in each area
    // FLOOR(probability*10) Calculates the integer part of the product of 'probability' and 10
    // RAND() < (probability*10 - FLOOR(probability*10)) Generates a random value between 0 and 1 and compares it with the fractional part of probability*10
    // If the random value is less than the fractional part, it adds 1 pedestrian to the count; otherwise it adds 0 WHY?
    // FROM PEDESTRIANS_PROBABILITY WHERE RAND() < probability*10 Filters the rows from PEDESTRIANS_PROBABILITY based on the condition that a random value is less than the 'probability' value * 10. This filters out areas with low probabilities.
    // The resulting PEDESTRIANS table will contain 'the_geom' and 'nbPedestrian'
    sql.execute("CREATE TABLE PEDESTRIANS AS SELECT PK PK, the_geom the_geom, GREATEST(FLOOR(probability)+  CASE WHEN RAND() < (probability - FLOOR(probability)) THEN 1 ELSE 0 END,1) AS nbPedestrian FROM PEDESTRIANS_PROBABILITY WHERE RAND() < probability ;")
    // sql.execute("DROP TABLE PEDESTRIANS_PROBABILITY IF EXISTS;")
    sql.execute("ALTER TABLE PEDESTRIANS ALTER COLUMN PK int NOT NULL;")
    sql.execute("ALTER TABLE PEDESTRIANS ADD PRIMARY KEY (PK);")

    // sql.execute("ALTER TABLE PEDESTRIANS ADD PK INT AUTO_INCREMENT PRIMARY KEY;")

    return ["Process done. Table of outputs created !"]
}


//compute the density estimate of sample points at a given location X(x,y)

/**
 * Compute density using KDE
 * Thank you to my new friend ChatGPT
 * @param bandwidth
 * @param location
 * @param poi
 * @return
 */
double densityChatGPT(double bandwidth, Point location, List<Point> poi) {
    //double density = 0
    if (!poi.isEmpty()) {
        // Compute the distances between the target point and all input objects
        List<Double> distances = poi.collect { object ->
            // new DistanceToPoint(object).computeDistance(location,object,)
            location.distance(object)
        }

// Compute the kernel density estimate for the target point
        double kernelSum = distances.collect { distance ->
            Math.exp(-0.5 * Math.pow((distance / bandwidth), 2))
        }.sum()

        //double density = kernelSum / (Math.sqrt(2 * Math.PI) * bandwidth * poi.size())
        density = kernelSum / (Math.PI * 2 * bandwidth * bandwidth)
    }

    return density
}


double density_2(Point location, List<Point> poi) {


    List<Double> poiX = poi.collect { p -> p.x }
    List<Double> poiY = poi.collect { p -> p.y }

    Double wX = Math.sqrt(variance(vecX))
    Double wY = Math.sqrt(variance(vecY))


    Double locx = location.x
    Double locy = location.y

    List<Double> terms = []

    for (p in poi) {
        Double Xi = p.x
        Double Yi = p.y
        Double kernelTerm = exp(-(Math.pow((locx - Xi), 2) / (2 * Math.pow(wX, 2))) - (Math.pow((locy - Yi), 2) / (2 * Math.pow(wY, 2))))
        terms.add(kernelTerm)
    }

    Double kernelSum = terms.sum()

    int n = poi.size()
    Double result = kernelSum / (n * 2 * Math.PI * wX * wY)
    return (result)
}


double densityEstimate(double[] sample_x, double[] sample_y, double[] X) {
    assert (sample_x.size() == sample_y.size())

    int n = sample_x.size()
    double[] bandwidth = bandwidthMatrixSelectionScott(sample_x, sample_y, 2)
    double[] X_minus_Xi = [0]
    //double res= (1/n)  *   X_minus_Xi.collect(i -> gaussianKernelwithDiagonalBandwidth(X_minus_Xi bandwidth ) )
    double res = 0.0
    return res

}


/**
 Compute the value of the density estimate at point location(x,y)
 full formula of the 2D multivariate kernel density estimate is
 density = {\textstyle K_{\mathbf {H}}(\mathbf {x} )={(2\pi )^{-d/2}}\mathbf {|H|} ^{-1/2}e^{-{\frac {1}{2}}\mathbf {x^{T}} \mathbf {H^{-1}} \mathbf {x}}}Here H is diagonal
 **/
double gaussianKernelwithDiagonalBandwidth(Double[] X, Double poi_x, Double poi_y, Double[] bandwidth_matrix_diagonal_values) {

    // coordinates of the X vector
    double x = X[1]
    double y = X[2]

    // determinant of the diagonal matrix
    double Hdet = bandwidth_matrix_diagonal_values[1] * bandwidth_matrix_diagonal_values[2]
    // H inverse matrix
    double[] H_inverse = [1 / bandwidth_matrix_diagonal_values[1], 1 / bandwidth_matrix_diagonal_values[2]]
    // argument of the exponential : Xtransposed . H^-1 .X =  x * Hinverse11 * x + y * Hinverse22 * y  since H is diagonal
    double x_THinverse_x = x * H_inverse[1] * x + y * H_inverse[2] * y

    // kernel value for vector X=[x,y]
    Double density = 1 / (2 * Math.PI) * 1 / (sqrt(Hdet)) * exp(-0.5 * x_THinverse_x)
    return (density)
}


double variance(List<Double> x) {
    int n = x.size()
    double mu = x.sum() / n
    double variance = x.collect { i -> return Math.pow(i - mu, 2) }.sum() * (1 / (n - 1))
    // variance empirique debiaisée
    return (variance)
}


// Based on Scott's rule of thumb  : sqrt(Hii) =  n ^( -1 / d+4) * sigma_j,
// with n the number of data points
// with d the number of spatial dimension
// j is the index of these dimension
// 2D case ; j=1 sigma_1 = std(x) , j=2sigma_2=std(y) coordinates of the sample
// return the two diagonal term of the bandwidth Matrix H
double[] bandwidthMatrixSelectionScott(double[] x, double[] y, d = 2) {
    assert (x.size() == y.size())
    int n = x.size() //number of sample points
    double sigma_x = Math.sqrt(variance(x))
    double sigma_y = Math.sqrt(variance(y))
    double H11 = Math.pow(Math.pow(n, (-1 / d + 4)) * sigma_x, 2)   //first diagonal term
    double H22 = Math.pow(Math.pow(n, (-1 / d + 4)) * sigma_y, 2)   //first diagonal term
    double[] res = [H11, H22]
    return res
}

