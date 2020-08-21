package org.noise_planet.noisemodelling.gui;

import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SFSUtilities;
import org.h2gis.utilities.TableLocation;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class DataBaseManager {

    private String dbName;
    private List<String> availableTables;

    public List<String> getAvailableTables() {
        return availableTables;
    }

    public void refreshAvailableTables() throws SQLException, ClassNotFoundException {
        availableTables.clear();
        Connection connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));
        List<String> ignorelst = List.of("SPATIAL_REF_SYS", "GEOMETRY_COLUMNS");
        List<String> tables = JDBCUtilities.getTableNames(connection.getMetaData(), null, "PUBLIC", "%", null);
        for (String table : tables) {
            TableLocation tab = TableLocation.parse(table);
            if (!ignorelst.contains(tab.getTable())) {
                availableTables.add(tab.getTable());
            }
        }
        connection.close();
    }

    DataBaseManager(String name) throws SQLException, ClassNotFoundException {
        dbName = name;
        availableTables = new ArrayList<String>();
        refreshAvailableTables();
    }

    public TableModel getTableModel(String tableName) throws SQLException, ClassNotFoundException {
        Connection connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));
        Statement statement = connection.createStatement();
        ResultSet res = statement.executeQuery("SELECT * FROM " + tableName + " LIMIT 100");
        ResultSetMetaData metaData = res.getMetaData();
        int numberOfColumns = metaData.getColumnCount();
        Vector columnNames = new Vector();

        // Get the column names
        for (int column = 0; column < numberOfColumns; column++) {
            columnNames.addElement(metaData.getColumnLabel(column + 1));
        }

        // Get all rows.
        Vector rows = new Vector();

        while (res.next()) {
            Vector newRow = new Vector();

            for (int i = 1; i <= numberOfColumns; i++) {
                newRow.addElement(res.getObject(i));
            }

            rows.addElement(newRow);
        }
        connection.close();
        return new DefaultTableModel(rows, columnNames);
    }

    public ListModel getTableListModel() throws SQLException, ClassNotFoundException {
        DefaultListModel<String> listModel = new DefaultListModel<String>();
        for (String table: availableTables) {
            listModel.addElement(table);
        }
        return listModel;
    }

    public Connection openConnection() throws SQLException, ClassNotFoundException {
        return SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));
    }

    public void closeConnection(Connection connection) throws SQLException {
        connection.close();
    }

/*
    public H2gisSpatialTable getSpatialTable(String table) throws SQLException, ClassNotFoundException {
        Connection connection = openConnection();
        H2gisSpatialTable spatialTable = null;
        H2GIS h2gis = H2GIS.open(connection);
        spatialTable = (H2gisSpatialTable) h2gis.getSpatialTablegetSpatialTable(table);
        connection.close();
        return spatialTable;
    }
*/
}
