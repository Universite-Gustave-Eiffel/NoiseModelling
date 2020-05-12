package org.noise_planet.noisemodelling.emission.jdbc;

import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SFSUtilities;
import org.h2gis.utilities.TableLocation;
import org.h2gis.utilities.jts_utils.Contouring;
import org.h2gis.utilities.jts_utils.TriMarkers;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import org.noise_planet.noisemodelling.propagation.ComputeRays;

import java.sql.*;
import java.util.*;

public class BezierContouring {
    static final int BATCH_MAX_SIZE = 500;
    String pointTable = "LDEN_RESULT";
    String triangleTable = "TRIANGLES";
    String outputTable = "CONTOURING_NOISE_MAP";
    String pointTableField = "LAEQ";
    List<Double> isoLevels;
    int srid;

    /**
     * @param isoLevels Iso levels in dB
     */
    public BezierContouring(List<Double> isoLevels, int srid) {
        this.isoLevels = new ArrayList<>(isoLevels.size());
        this.srid = srid;
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

    /**
     * Extract all polygons of the provided geometry
     * @param geom geometry
     * @param polygons polygon list
     */
    void explode(Geometry geom, List<Polygon> polygons) {
        if(geom instanceof Polygon) {
            polygons.add((Polygon)geom);
        } else if(geom instanceof GeometryCollection){
            for(int i = 0; i < geom.getNumGeometries(); i++) {
                explode(geom.getGeometryN(i), polygons);
            }
        }
    }

    /**
     * Merge polygons of the same iso levels then apply bezier filtering on outer and inner rings.
     * Finally insert those polygons into the output table
     * @param connection jdbc connection (h2gis or postgis)
     * @param cellId area id (aggregate polygons by large area in order to avoid memory overloading)
     * @param polys Polygons by isolevel
     */
    void processCell(Connection connection, int cellId, Map<Short, ArrayList<Geometry>> polys) throws SQLException {
        int batchSize = 0;
        try(PreparedStatement ps = connection.prepareStatement("INSERT INTO " + TableLocation.parse(outputTable)
                + "(cellid, the_geom, isolevel) VALUES (?, ?, ?);")) {
            for (Map.Entry<Short, ArrayList<Geometry>> entry : polys.entrySet()) {
                // Merge triangles
                CascadedPolygonUnion union = new CascadedPolygonUnion(entry.getValue());
                Geometry mergeTriangles = union.union();
                ArrayList<Polygon> polygons = new ArrayList<>();
                explode(mergeTriangles, polygons);
                for(Polygon polygon : polygons) {
                    // TODO bezier interpolation
                    polygon.setSRID(srid);
                    int parameterIndex = 1;
                    ps.setInt(parameterIndex++, cellId);
                    ps.setObject(parameterIndex++, polygon);
                    ps.setInt(parameterIndex++, entry.getKey());
                    ps.addBatch();
                    batchSize++;
                    if (batchSize >= BATCH_MAX_SIZE) {
                        ps.executeBatch();
                        ps.clearBatch();
                        batchSize = 0;
                    }
                }
            }
            if (batchSize > 0) {
                ps.executeBatch();
            }
        }
    }

    public void createTable(Connection connection) throws SQLException {
        List<String> fields = JDBCUtilities.getFieldNames(connection.getMetaData(), TableLocation.parse(pointTable).toString());
        int pk = JDBCUtilities.getIntegerPrimaryKey(connection, TableLocation.parse(pointTable).toString());
        if(pk == 0) {
            throw new SQLException(pointTable+" does not contain a primary key");
        }
        String pkField = fields.get(pk - 1);
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), srid);
        Map<Short, ArrayList<Geometry>> polys = new HashMap<>();
        int lastCellId = -1;
        try(Statement st = connection.createStatement()) {
            st.execute("DROP TABLE IF EXISTS " + TableLocation.parse(outputTable));
            st.execute("CREATE TABLE " + TableLocation.parse(outputTable) + "(PK SERIAL, CELL_ID INTEGER, THE_GEOM GEOMETRY, ISOLVL INTEGER);");
            String query = "SELECT CELL_ID, t.pk, ST_X(p1.the_geom) xa,ST_Y(p1.the_geom) ya,ST_X(p1.the_geom) xb,ST_Y(p1.the_geom) yb,ST_X(p1.the_geom) xc,ST_Y(p1.the_geom) yc, p1."+pointTableField+" lvla, p2."+pointTableField+" lvlb, p3."+pointTableField+" lvlc FROM "+triangleTable+" t, "+pointTable+" p1,"+pointTable+" p2,"+pointTable+" p3 WHERE t.PK_1 = p1."+pkField+" and t.PK_2 = p2."+pkField+" AND t.PK_3 = p3."+pkField+" order by cell_id;";
            try(ResultSet rs = st.executeQuery(query)) {
                // Cache columns index
                int xa = 0, xb = 0, xc = 0, ya = 0, yb = 0, yc = 0, lvla = 0, lvlb = 0, lvlc = 0, cell_id = 0;
                ResultSetMetaData resultSetMetaData = rs.getMetaData();
                for (int columnId = 1; columnId <= resultSetMetaData.getColumnCount(); columnId++) {
                    switch (resultSetMetaData.getColumnName(columnId).toUpperCase()) {
                        case "XA":
                            xa = columnId;
                            break;
                        case "XB":
                            xb = columnId;
                            break;
                        case "XC":
                            xc = columnId;
                            break;
                        case "YA":
                            ya = columnId;
                            break;
                        case "YB":
                            yb = columnId;
                            break;
                        case "YC":
                            yc = columnId;
                            break;
                        case "LVLA":
                            lvla = columnId;
                            break;
                        case "LVLB":
                            lvlb = columnId;
                            break;
                        case "LVLC":
                            lvlc = columnId;
                            break;
                        case "CELL_ID":
                            cell_id = columnId;
                            break;
                    }
                }
                if (xa == 0 || xb == 0 || xc == 0 || ya == 0 || yb == 0 || yc == 0 || lvla == 0 || lvlb == 0 ||
                        lvlc == 0 || cell_id == 0) {
                    throw new SQLException("Missing field in input tables");
                }
                while(rs.next()) {
                    int cellId = rs.getInt(cell_id);
                    // Process polygons of last cell
                    if(cellId != lastCellId && lastCellId != -1) {
                        processCell(connection, cellId, polys);
                        polys.clear();
                    }
                    lastCellId = cellId;
                    // Split current triangle
                    Coordinate a = new Coordinate(rs.getDouble(xa), rs.getDouble(ya));
                    Coordinate b = new Coordinate(rs.getDouble(xb), rs.getDouble(yb));
                    Coordinate c = new Coordinate(rs.getDouble(xc), rs.getDouble(yc));
                    // Fetch data
                    TriMarkers triMarkers = new TriMarkers(a, b, c, ComputeRays.dbaToW(rs.getDouble(lvla)),
                            ComputeRays.dbaToW(rs.getDouble(lvlb)),
                            ComputeRays.dbaToW(rs.getDouble(lvlc)));
                    // Split triangle
                    Map<Short, Deque<TriMarkers>> res = Contouring.processTriangle(triMarkers, isoLevels);
                    for(Map.Entry<Short, Deque<TriMarkers>> entry : res.entrySet()) {
                        if(!polys.containsKey(entry.getKey())) {
                            polys.put(entry.getKey(), new ArrayList<>());
                        }
                        ArrayList<Geometry> polygonsArray = polys.get(entry.getKey());
                        for(TriMarkers tri : entry.getValue()) {
                            Polygon poly = geometryFactory.createPolygon(new Coordinate[]{tri.p0, tri.p1, tri.p2, tri.p0});
                            polygonsArray.add(poly);
                        }
                    }
                }
            }
        }
    }
}
