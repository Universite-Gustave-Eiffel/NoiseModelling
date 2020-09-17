package org.noise_planet.noisemodelling.wps.matsim

import groovy.sql.Sql
import org.h2gis.functions.io.dbf.DBFRead
import org.h2gis.functions.io.geojson.GeoJsonRead
import org.h2gis.functions.io.geojson.GeoJsonWrite
import org.noise_planet.noisemodelling.wps.Database_Manager.Add_Primary_Key
import org.noise_planet.noisemodelling.wps.JdbcTestCase
import org.noise_planet.noisemodelling.wps.Matsim.Noise_From_Attenuation_Matrix
import org.noise_planet.noisemodelling.wps.Others_Tools.Add_Laeq_Leq_columns
import org.noise_planet.noisemodelling.wps.Others_Tools.ZerodB_Source_From_Roads
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TestNoiseFromAttenuationMatrix extends JdbcTestCase {
    Logger logger = LoggerFactory.getLogger(TestNoiseFromAttenuationMatrix.class);

    void testDefault() {
        GeoJsonRead.readGeoJson(connection, this.class.getResource("tables/att_matrix.geojson").getFile(), "ATT_MATRIX")
        GeoJsonRead.readGeoJson(connection, this.class.getResource("tables/matsim_roads.geojson").getFile(), "MATSIM_ROADS");
        DBFRead.read(connection, this.class.getResource("tables/matsim_roads_stats.dbf").getFile(), "MATSIM_ROADS_STATS")

        Sql sql = new Sql(connection);
        sql.execute("ALTER TABLE MATSIM_ROADS ALTER COLUMN PK INT NOT NULL;");
        sql.execute("ALTER TABLE MATSIM_ROADS ADD PRIMARY KEY (PK)");
        sql.execute("CREATE INDEX ON MATSIM_ROADS(LINK_ID)");
        sql.execute("CREATE INDEX ON MATSIM_ROADS_STATS(LINK_ID)");
        sql.execute("CREATE INDEX ON MATSIM_ROADS_STATS(TIMESTRING)");
        sql.execute("CREATE INDEX ON ATT_MATRIX (IDSOURCE)");

        String result = runWps();

        GeoJsonWrite.writeGeoJson(connection, "noise_map.geojson", "NOISE_MAP");

        assertEquals("Process done. Table of receivers NOISE_MAP created !", result);
    }

    private String runWps() {
        return new Noise_From_Attenuation_Matrix().exec(connection, [
                matsimRoads: "MATSIM_ROADS",
                matsimRoadsStats: "MATSIM_ROADS_STATS",
                attenuationTable: "ATT_MATRIX",
                // timeString: "12_13",
                outTableName: "NOISE_MAP"
        ])
    }
}
