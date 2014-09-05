package org.orbisgis.noisemap.core.jdbc;

import com.vividsolutions.jts.densify.Densifier;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.buffer.BufferOp;
import com.vividsolutions.jts.operation.buffer.BufferParameters;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SpatialResultSet;
import org.h2gis.utilities.TableLocation;
import org.orbisgis.noisemap.core.JTSUtility;
import org.orbisgis.noisemap.core.LayerDelaunayError;
import org.orbisgis.noisemap.core.MeshBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;

/**
 * Create noise map using JDBC connection. SQL syntax is compatible with H2 and PostGIS.
 * @author Nicolas Fortin
 */
public class TriangleNoiseMap {
    private String destinationTable = "";
    private String buildings = "";
    private String sources = "";
    private String sound_lvl_field = "DB_M";
    private double maximum_propagation_distance = 750;
    private double maximum_reflection_distance = 50;
    private int subdivision_level = -1; // TODO Guess it from maximum_propagation_distance and source extent
    private int sound_reflection_order = 2;
    private int sound_diffraction_order = 1;
    private double wall_absorption = 0.05;
    private final static double BUILDING_BUFFER = 0.5;
    private Logger logger = LoggerFactory.getLogger(TriangleNoiseMap.class);
    private static final String heightField = "height";
    private GeometryFactory geometryFactory = new GeometryFactory();
    private boolean doMultiThreading = true;

    private void explodeAndAddPolygon(Geometry intersectedGeometry,
                                      MeshBuilder delaunayTool, Geometry boundingBox)
            throws LayerDelaunayError {
        long beginAppendPolygons = System.currentTimeMillis();
        if (intersectedGeometry instanceof GeometryCollection) {
            for (int j = 0; j < intersectedGeometry.getNumGeometries(); j++) {
                Geometry subGeom = intersectedGeometry.getGeometryN(j);
                explodeAndAddPolygon(subGeom, delaunayTool, boundingBox);
            }
        } else {
            delaunayTool.addGeometry(intersectedGeometry);
        }
    }

    private Geometry merge(LinkedList<Geometry> toUnite, double bufferSize) {
        Geometry geoArray[] = new Geometry[toUnite.size()];
        toUnite.toArray(geoArray);
        GeometryCollection polygonCollection = geometryFactory
                .createGeometryCollection(geoArray);
        BufferOp bufferOp = new BufferOp(polygonCollection,
                new BufferParameters(BufferParameters.DEFAULT_QUADRANT_SEGMENTS, BufferParameters.CAP_SQUARE,
                BufferParameters.JOIN_MITRE, BufferParameters.DEFAULT_MITRE_LIMIT));
        return bufferOp.getResultGeometry(bufferSize);
    }

    /**
     * Compute the envelope corresping to parameters
     *
     * @param mainEnvelope Global envelope
     * @param cellI        I cell index
     * @param cellJ        J cell index
     * @param cellIMax     I cell count
     * @param cellJMax     J cell count
     * @param cellWidth    Cell width meter
     * @param cellHeight   Cell height meter
     * @return Envelope of the cell
     */
    private static Envelope getCellEnv(Envelope mainEnvelope, int cellI, int cellJ,
                                      int cellIMax, int cellJMax, double cellWidth, double cellHeight) {
        return new Envelope(mainEnvelope.getMinX() + cellI * cellWidth,
                mainEnvelope.getMinX() + cellI * cellWidth + cellWidth,
                mainEnvelope.getMinY() + cellHeight * cellJ,
                mainEnvelope.getMinY() + cellHeight * cellJ + cellHeight);
    }

    private void feedDelaunay(Connection connection, String buildingsTable,String buildingsTableGeomColumn, int spatialBuildingsFieldIndex,
                              MeshBuilder delaunayTool, Envelope boundingBoxFilter,
                              double srcDistance, LinkedList<LineString> delaunaySegments,
                              double minRecDist, double srcPtDist, double triangleSide) throws SQLException,
            LayerDelaunayError {
        Envelope extendedEnvelope = new Envelope(boundingBoxFilter);
        extendedEnvelope.expandBy(srcDistance * 2.);
        Geometry linearRing = geometryFactory.toGeometry(boundingBoxFilter);
        if (!(linearRing instanceof LinearRing)) {
            return;
        }
        Polygon boundingBox = geometryFactory.createPolygon((LinearRing)linearRing);
        LinkedList<Geometry> toUnite = new LinkedList<Geometry>();
        Envelope fetchBox = new Envelope(boundingBoxFilter);
        fetchBox.expandBy(BUILDING_BUFFER);
        Geometry fetchGeometry = geometryFactory.toGeometry(fetchBox);
        try(PreparedStatement st = connection.prepareStatement("SELECT "+ TableLocation.quoteIdentifier(buildingsTableGeomColumn) +
                " FROM "+buildingsTable+" WHERE "+ TableLocation.quoteIdentifier(buildingsTableGeomColumn) + " && ?");
            SpatialResultSet rs = st.executeQuery().unwrap(SpatialResultSet.class)) {
            while(rs.next()) {
                Geometry geometry = rs.getGeometry(buildingsTableGeomColumn);
                if(geometry!= null && geometry.intersects(fetchGeometry)) {
                    toUnite.add(geometry);
                }
            }
        }
        // Reduce small artifacts to avoid, shortest geometry to be
        // over-triangulated
        LinkedList<Geometry> toUniteFinal = new LinkedList<Geometry>();
        if (!toUnite.isEmpty()) {
            logger.info("Merge buildings");
            Geometry bufferBuildings = merge(toUnite, BUILDING_BUFFER);
            // Remove small artifacts due to buildings buffer
            if(triangleSide > 0) {
                bufferBuildings = Densifier.densify(bufferBuildings, triangleSide);
            }
            toUniteFinal.add(bufferBuildings); // Add buildings to triangulation
        }

        // Merge roads
        if (minRecDist > 0.01) {
            LinkedList<Geometry> toUniteRoads = new LinkedList<Geometry>(delaunaySegments);
            if (!toUniteRoads.isEmpty()) {
                // Build Polygons buffer from roads lines
                logger.info("Merge roads");
                Geometry bufferRoads = merge(toUniteRoads, minRecDist / 2);
                // Remove small artifacts due to multiple buffer crosses
                bufferRoads = TopologyPreservingSimplifier.simplify(bufferRoads,
                        minRecDist / 2);
                // Densify roads to set more receiver near roads.
                if(srcPtDist > 0){
                    bufferRoads = Densifier.densify(bufferRoads, srcPtDist);
                } else if (triangleSide > 0) {
                    bufferRoads = Densifier.densify(bufferRoads, triangleSide);
                }
                //Add points buffer to the final triangulation, this will densify sound level extraction near
                //toUniteFinal.add(makeBufferSegmentsNearRoads(toUniteRoads,srcPtDist));
                //roads, and helps to reduce over estimation due to inappropriate interpolation.
                toUniteFinal.add(bufferRoads); // Merge roads with minRecDist m
                // buffer
            }
        }
        logger.info("Merge roads and buildings");
        Geometry union = merge(toUniteFinal, 0.); // Merge roads and buildings
        // together
        // Remove geometries out of the bounding box
        logger.info("Remove roads and buildings outside study area");
        union = union.intersection(boundingBox);
        explodeAndAddPolygon(union, delaunayTool, boundingBox);
    }

    private static Double DbaToW(Double dBA) {
        return Math.pow(10., dBA / 10.);
    }
    /**
     *
     * @param connection
     * @throws SQLException
     */
    public void execute(Connection connection) throws SQLException {
        if(maximum_propagation_distance < maximum_reflection_distance) {
            throw new SQLException(new IllegalArgumentException(
                    "Maximum wall seeking distance cannot be superior than maximum propagation distance"));
        }
        if(sources.isEmpty()) {
            throw new SQLException("A sound source table must be provided");
        }
        ThreadPool threadManager = null;
        ProgressionOrbisGisManager pmManager = null;
    }

    public void setDestinationTable(String destinationTable) {
        this.destinationTable = destinationTable;
    }

    public void setBuildings(String buildings) {
        this.buildings = buildings;
    }

    public void setSources(String sources) {
        this.sources = sources;
    }

    public void setSound_lvl_field(String sound_lvl_field) {
        this.sound_lvl_field = sound_lvl_field;
    }

    public void setMaximum_propagation_distance(double maximum_propagation_distance) {
        this.maximum_propagation_distance = maximum_propagation_distance;
    }

    public void setMaximum_reflection_distance(double maximum_reflection_distance) {
        this.maximum_reflection_distance = maximum_reflection_distance;
    }

    public void setSubdivision_level(int subdivision_level) {
        this.subdivision_level = subdivision_level;
    }

    public void setSound_reflection_order(int sound_reflection_order) {
        this.sound_reflection_order = sound_reflection_order;
    }

    public void setSound_diffraction_order(int sound_diffraction_order) {
        this.sound_diffraction_order = sound_diffraction_order;
    }

    public void setWall_absorption(double wall_absorption) {
        this.wall_absorption = wall_absorption;
    }
}
