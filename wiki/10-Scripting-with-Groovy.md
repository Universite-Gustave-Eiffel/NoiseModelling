It is possible to run NoiseModelling without using OrbisGIS.

The following sample demonstrate the use of the Groovy scripting language to command the execution of NoiseModelling  The advantage is the ability to run it on dedicated server without X11.

A ready package published here contain all useful dependencies:

https://github.com/Ifsttar/NoiseModelling/releases/download/2.1.2-SNAPSHOT/orbisgis-noisemodelling-2018-07-18.zip

You have to install **Java** from version 7 (Greater is ok)

Unzip the file orbisgis.zip

You have to download and unzip the Groovy binary on your system http://groovy-lang.org

Create a new file "noisemodelling_script.groovy" in the root orbisgis folder containing the following Groovy script:

It run the Quick-Start sample with the evaluation of specific point.

```groovy
 // Sample groovy script
// Install groovy then run start.sh

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashSet;
import java.sql.Connection;
import java.sql.Statement;
import org.h2gis.utilities.SFSUtilities;
import org.orbisgis.noisemap.h2.BR_PtGrid3D;
import org.orbisgis.noisemap.h2.BR_PtGrid;
import org.orbisgis.noisemap.h2.BR_SpectrumRepartition;
import org.orbisgis.noisemap.h2.BR_EvalSource;
import org.orbisgis.noisemap.core.jdbc.PointNoiseMap;
import org.h2gis.api.EmptyProgressVisitor;
import org.orbisgis.noisemap.core.PropagationResultPtRecord;
import org.h2gis.functions.factory.H2GISFunctions;

Class.forName("org.h2.Driver")

// Open memory H2 table
Connection connection = SFSUtilities.wrapConnection(DriverManager.getConnection("jdbc:h2:mem:syntax","sa", "sa"));
try {
    Statement st = connection.createStatement()

    // Import spatial and noise functions, domains and drivers
    // If you are using a file database, you have to do only that once.
    org.h2gis.functions.factory.H2GISFunctions.load(connection);
    H2GISFunctions.registerFunction(st, new BR_PtGrid3D(), "");
    H2GISFunctions.registerFunction(st, new BR_PtGrid(), "");
    H2GISFunctions.registerFunction(st, new BR_SpectrumRepartition(), "");
    H2GISFunctions.registerFunction(st, new BR_EvalSource(), "");

    // Run external SQL script for input data
    // You could also import data files using drivers http://www.h2gis.org/docs/1.2/h2drivers/
    st.execute("RUNSCRIPT FROM 'input.sql'");

    // Set receiver (microphone) position
    st.execute("DROP TABLE IF EXISTS RECEIVERS");
    st.execute("CREATE TABLE RECEIVERS(the_geom POINT, GID SERIAL)");
    st.execute("INSERT INTO RECEIVERS(the_geom) VALUES ('POINT(-275 -18 20)')");
    st.execute("INSERT INTO RECEIVERS(the_geom) VALUES ('POINT(-275 -18 1.6)')");

    // Configure noisemap with specified receivers
    PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS", "ROADS_SRC", "RECEIVERS");
    pointNoiseMap.setSoundDiffractionOrder(0);
    pointNoiseMap.setSoundReflectionOrder(0);
    pointNoiseMap.initialize(connection, new EmptyProgressVisitor());
    // Run propagation on all cells
    // A cell is a subdivision of the computation area
    List<PropagationResultPtRecord> result = new ArrayList<>();
    HashSet<Integer> processedReceivers = new HashSet<Integer>();
    for(int cellI = 0; cellI < pointNoiseMap.getGridDim(); cellI++) {
        for(int cellJ = 0; cellJ < pointNoiseMap.getGridDim(); cellJ++) {
            result.addAll(pointNoiseMap.evaluateCell(connection, cellI, cellJ, new EmptyProgressVisitor(), processedReceivers));
        }
    }

    // Output value
    println("Receiver 0 at "+Math.round(10*Math.log10(result.get(0).getReceiverLvl()))+" dB(A)")
} finally {
    connection.close()
}
```

The input.sql file has been copied from:
https://github.com/Ifsttar/NoiseModelling/blob/master/sampleFiles/noisemap_1_input_data.sql
https://github.com/Ifsttar/NoiseModelling/blob/master/sampleFiles/noisemap_2_traffic_to_noise_source.sql

In order to run it using groovy you have to specify the location of Jar dependencies:

## Windows
```bat
groovy -cp "bundle/commons-compress-1.9.jar;bin/cts-1.3.3.jar;bin/spatial-utilities-1.2.0.jar;bin/h2spatial-api-1.2.0.jar;bin/jts-1.13.jar:bin/slf4j-api-1.6.0.jar;bin/slf4j-log4j12-1.6.0.jar;bin/log4j-1.2.16.jar;bundle/*;sys-bundle/*" noisemodelling_script.groovy
```

## Linux-Mac

Linux/Mac
```sh
groovy -cp bundle/commons-compress-1.9.jar:bin/cts-1.3.3.jar:bin/spatial-utilities-1.2.0.jar:bin/h2spatial-api-1.2.0.jar:bin/jts-1.13.jar:bin/slf4j-api-1.6.0.jar:bin/slf4j-log4j12-1.6.0.jar:bin/log4j-1.2.16.jar:bundle/*:sys-bundle/* noisemodelling_script.groovy
```

The result in this sample is printed, but you can also use the SQL function BR_PTGRID and save the table result as a shape file using a driver http://www.h2gis.org/docs/1.2/h2drivers/