package org.noise_planet.noisemodelling.emission.jdbc;

import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SFSUtilities;
import org.h2gis.utilities.TableLocation;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class BezierContouring {
    String pointTable = "LDEN_RESULT";
    String triangleTable = "TRIANGLE";
    String outputTable = "CONTOURING_NOISE_MAP";
    String pointTableField = "LAEQ";

    public String getPointTableField() {
        return pointTableField;
    }


    /**
     * @param pointTableField Field with level in dB(A)
     */
    public void setPointTableField(String pointTableField) {
        this.pointTableField = pointTableField;
    }

    public String getPointTable() {
        return pointTable;
    }

    /**
     * Point table where primary key is referenced by triangle table PK_1, PK_2, PK_3
     * @param pointTable table name
     */
    public void setPointTable(String pointTable) {
        this.pointTable = pointTable;
    }

    public String getTriangleTable() {
        return triangleTable;
    }

    /**
     * Triangle table with fields THE_GEOM, PK_1, PK_2, PK_3, CELL_ID
     * @param triangleTable table name
     */
    public void setTriangleTable(String triangleTable) {
        this.triangleTable = triangleTable;
    }

    public String getOutputTable() {
        return outputTable;
    }

    public void setOutputTable(String outputTable) {
        this.outputTable = outputTable;
    }

    void createTable(Connection connection) throws SQLException {
        List<String> fields = JDBCUtilities.getFieldNames(connection.getMetaData(), TableLocation.parse(pointTable).toString());
        int pk = JDBCUtilities.getIntegerPrimaryKey(connection, TableLocation.parse(pointTable).toString());
        if(pk == 0) {
            throw new SQLException(pointTable+" does not contain a primary key");
        }
        String pkField = fields.get(pk - 1);
        try(Statement st = connection.createStatement()) {
            try(st.execute("SELECT * FROM " + TableLocation.parse(triangleTable) + " t, " + TableLocation.parse(pointTable)+ " p where t.PK_1 = p."+pkField))
            // Fetch data
            String geomFieldName = SFSUtilities.getFirstGeometryFieldName()
        }
    }
}
