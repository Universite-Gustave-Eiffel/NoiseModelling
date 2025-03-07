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

package org.noise_planet.noisemodelling.wps

import org.h2gis.utilities.JDBCUtilities
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.noisemodelling.runner.Main

import javax.sql.DataSource
import java.sql.Connection
import java.sql.SQLException

@Ignore
class JdbcTestCase  extends GroovyTestCase {
    Connection connection
    File dbFile = new File(new File("build/tmp"), UUID.randomUUID().toString().replace("-", "")+".mv.db")

    @Before
    void setUp() {
        DataSource dataSource = Main.createDataSource("sa", "sa", dbFile.getParent(), dbFile.getName().replace(".mv.db", ""), false)
        connection = JDBCUtilities.wrapConnection(dataSource.getConnection())
    }

    @After
    void tearDown() throws SQLException {
        connection.close()
        dbFile.delete()
    }
}
