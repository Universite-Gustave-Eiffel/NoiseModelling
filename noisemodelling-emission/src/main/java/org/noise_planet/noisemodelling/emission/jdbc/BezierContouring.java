package org.noise_planet.noisemodelling.emission.jdbc;

import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SFSUtilities;
import org.h2gis.utilities.TableLocation;
import org.h2gis.utilities.jts_utils.Contouring;
import org.h2gis.utilities.jts_utils.TriMarkers;
import org.locationtech.jts.geom.Coordinate;
import org.noise_planet.noisemodelling.propagation.ComputeRays;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

public class BezierContouring {
    String pointTable = "LDEN_RESULT";
    String triangleTable = "TRIANGLES";
    String outputTable = "CONTOURING_NOISE_MAP";
    String pointTableField = "LAEQ";
    List<Double> isoLevels;

    /**
     * @param isoLevels Iso levels in dB
     */
    public BezierContouring(List<Double> isoLevels) {
        this.isoLevels = new ArrayList<>(isoLevels.size());
        for(double lvl : isoLevels) {
            isoLevels.add(ComputeRays.dbaToW(lvl));
        }
    }

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
            //try(st.execute("SELECT * FROM " + TableLocation.parse(triangleTable) + " t, " + TableLocation.parse(pointTable)+ " p where t.PK_1 = p."+pkField))
            String query = "SELECT CELL_ID, t.pk, ST_X(p1.the_geom) xa,ST_Y(p1.the_geom) ya,ST_X(p1.the_geom) xb,ST_Y(p1.the_geom) yb,ST_X(p1.the_geom) xc,ST_Y(p1.the_geom) yc, p1."+pointTableField+" lvla, p2."+pointTableField+" lvlb, p3."+pointTableField+" lvlc FROM "+triangleTable+" t, "+pointTable+" p1,"+pointTable+" p2,"+pointTable+" p3 WHERE t.PK_1 = p1."+pkField+" and t.PK_2 = p2."+pkField+" AND t.PK_3 = p3."+pkField+";";
            try(ResultSet rs = st.executeQuery(query)) {
                while(rs.next()) {
                    int areaId = rs.getInt("CELL_ID");
                    Coordinate a = new Coordinate(rs.getDouble("xa"), rs.getDouble("ya"));
                    Coordinate b = new Coordinate(rs.getDouble("xb"), rs.getDouble("yb"));
                    Coordinate c = new Coordinate(rs.getDouble("xc"), rs.getDouble("yc"));
                    // Fetch data
                    TriMarkers triMarkers = new TriMarkers(a, b, c, ComputeRays.dbaToW(rs.getDouble("lvla")),
                            ComputeRays.dbaToW(rs.getDouble("lvlb")),
                            ComputeRays.dbaToW(rs.getDouble("lvlc")));
                    // Split triangle
                    Map<Short, Deque<TriMarkers>> res = Contouring.processTriangle(triMarkers, isoLevels);
                }
            }
        }
    }
}
