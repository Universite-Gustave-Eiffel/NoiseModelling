/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */


package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.api.ProgressVisitor;
import org.h2gis.utilities.TableLocation;
import org.h2gis.utilities.dbtypes.DBTypes;
import org.h2gis.utilities.dbtypes.DBUtils;
import org.locationtech.jts.geom.*;
import org.noise_planet.noisemodelling.jdbc.input.DefaultTableLoader;
import org.noise_planet.noisemodelling.jdbc.utils.CellIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

import static org.h2gis.utilities.GeometryTableUtilities.getGeometryColumnNames;
import static org.h2gis.utilities.GeometryTableUtilities.getSRID;
/**
 * Common attributes and functions across DelaunayGrid and NoiseMap receiver computation
 * @author Nicolas Fortin
 */
public abstract class GridMapMaker {
    // When computing cell size, try to keep propagation distance away from the cell
    // inferior to this ratio (in comparison with cell width)
    protected static final double MINIMAL_BUFFER_RATIO = 0.3;
    protected DefaultTableLoader.BuildingTableParameters buildingTableParameters = new DefaultTableLoader.BuildingTableParameters();
    protected final String sourcesTableName;
    protected String soilTableName = "";
    // Digital elevation model table. (Contains points or triangles)
    protected String demTable = "";
    protected String sound_lvl_field = "DB_M";
    // True if Z of sound source and receivers are relative to the ground
    protected boolean receiverHasAbsoluteZCoordinates = false;
    protected boolean sourceHasAbsoluteZCoordinates = false;
    protected double maximumPropagationDistance = 750;
    protected double maximumReflectionDistance = 100;
    protected double gs = 0;
    // Soil areas are split by the provided size in order to reduce the propagation time
    protected double groundSurfaceSplitSideLength = 200;
    protected int soundReflectionOrder = 2;

    protected boolean bodyBarrier = false; // it needs to be true if train propagation is computed (multiple reflection between the train and a screen)
    public boolean verbose = true;
    protected boolean computeHorizontalDiffraction = true;
    protected boolean computeVerticalDiffraction = true;

    protected GeometryFactory geometryFactory;

    // Initialised attributes
    /**
     *  Side computation cell count (same on X and Y)
     */
    protected int gridDim = 0;
    protected Envelope mainEnvelope = new Envelope();

    public GridMapMaker(String buildingsTableName, String sourcesTableName) {
        this.buildingTableParameters.buildingsTableName = buildingsTableName;
        this.sourcesTableName = sourcesTableName;
    }

    public DefaultTableLoader.BuildingTableParameters getBuildingTableParameters() {
        return buildingTableParameters;
    }

    public GeometryFactory getGeometryFactory() {
        return geometryFactory;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Compute the envelope corresponding to parameters
     * @param cellIndex Cell location
     * @return Envelope of the cell
     */
    public Envelope getCellEnv(CellIndex cellIndex) {
        return  getCellEnv(mainEnvelope, cellIndex.getLatitudeIndex(),
                cellIndex.getLongitudeIndex(), getCellWidth(), getCellHeight());
    }
    /**
     * Compute the envelope corresping to parameters
     *
     * @param mainEnvelope Global envelope
     * @param cellI        I cell index
     * @param cellJ        J cell index
     * @param cellWidth    Cell width meter
     * @param cellHeight   Cell height meter
     * @return Envelope of the cell
     */
    public static Envelope getCellEnv(Envelope mainEnvelope, int cellI, int cellJ, double cellWidth,
                                      double cellHeight) {
        return new Envelope(mainEnvelope.getMinX() + cellI * cellWidth,
                mainEnvelope.getMinX() + cellI * cellWidth + cellWidth,
                mainEnvelope.getMinY() + cellHeight * cellJ,
                mainEnvelope.getMinY() + cellHeight * cellJ + cellHeight);
    }

    public double getGroundSurfaceSplitSideLength() {
        return groundSurfaceSplitSideLength;
    }

    public void setGroundSurfaceSplitSideLength(double groundSurfaceSplitSideLength) {
        this.groundSurfaceSplitSideLength = groundSurfaceSplitSideLength;
    }


    /**
     * true if train propagation is computed (multiple reflection between the train and a screen)
     */
    public void setBodyBarrier(boolean bodyBarrier) {
        this.bodyBarrier = bodyBarrier;
    }

    public double getCellWidth() {
        return mainEnvelope.getWidth() / gridDim;
    }

    public double getCellHeight() {
        return mainEnvelope.getHeight() / gridDim;
    }

    abstract protected Envelope getComputationEnvelope(Connection connection) throws SQLException;

    /**
     * Fetch scene attributes, compute best computation cell size.
     * @param connection Active connection
     * @throws java.sql.SQLException
     */
    public void initialize(Connection connection, ProgressVisitor progression) throws SQLException {
        if(soundReflectionOrder > 0 && maximumPropagationDistance < maximumReflectionDistance) {
            throw new SQLException(new IllegalArgumentException(
                    "Maximum wall seeking distance cannot be superior than maximum propagation distance"));
        }
        int srid = 0;
        DBTypes dbTypes = DBUtils.getDBType(connection.unwrap(Connection.class));
        if(!sourcesTableName.isEmpty()) {
            srid = getSRID(connection, TableLocation.parse(sourcesTableName, dbTypes));
        }
        if(srid == 0) {
            srid = getSRID(connection, TableLocation.parse(buildingTableParameters.buildingsTableName, dbTypes));
        }
        geometryFactory = new GeometryFactory(new PrecisionModel(), srid);

        // Steps of execution
        // Evaluation of the main bounding box (sourcesTableName+buildingsTableName)
        // Split domain into 4^subdiv cells
        // For each cell :
        // Expand bounding box cell by maxSrcDist
        // Build delaunay triangulation from buildingsTableName polygon processed by
        // intersection with non extended bounding box
        // Save the list of sourcesTableName index inside the extended bounding box
        // Save the list of buildingsTableName index inside the extended bounding box
        // Make a structure to keep the following information
        // Triangle list with the 3 vertices index
        // Vertices list (as receivers)
        // For each vertices within the cell bounding box (not the extended
        // one)
        // Find all sourcesTableName within maxSrcDist
        // For All found sourcesTableName
        // Test if there is a gap(no building) between source and receiver
        // if not then append the distance attenuated sound level to the
        // receiver
        // Save the triangle geometry with the db_m value of the 3 vertices
        if(mainEnvelope.isNull()) {
            // 1 Step - Evaluation of the main bounding box (sources)
            setMainEnvelope(getComputationEnvelope(connection));
        }
    }

    /**
     * @return Side computation cell count (same on X and Y)
     */
    public int getGridDim() {
        return gridDim;
    }

    public void setGridDim(int gridDim) {
        this.gridDim = gridDim;
    }

    /**
     * This table must contain a POLYGON column, where Z values are wall bottom position relative to sea level.
     * It may also contain a height field (0-N] average building height from the ground.
     * @return Table name that contains buildings
     */
    public String getBuildingsTableName() {
        return buildingTableParameters.buildingsTableName;
    }

    /**
     * This table must contain a POINT or LINESTRING column
     * @return Table name that contain linear and/or punctual sound sources geometries.*
     */
    public String getSourcesTableName() {
        return sourcesTableName;
    }

    /**
     * Extracted from NMPB 2008-2 7.3.2
     * Soil areas POLYGON, with a dimensionless coefficient G:
     *  - Law, meadow, field of cereals G=1
     *  - Undergrowth (resinous or decidious) G=1
     *  - Compacted earth, track G=0.3
     *  - Road surface G=0
     *  - Smooth concrete G=0
     * @return Table name of grounds properties
     */
    public String getSoilTableName() {
        return soilTableName;
    }

    /**
     * @return True if provided Z value are sea level (false for relative to ground level)
     */
    public boolean isReceiverHasAbsoluteZCoordinates() {
        return receiverHasAbsoluteZCoordinates;
    }

    /**
     *
     * @param receiverHasAbsoluteZCoordinates True if provided Z value are sea level (false for relative to ground level)
     */
    public void setReceiverHasAbsoluteZCoordinates(boolean receiverHasAbsoluteZCoordinates) {
        this.receiverHasAbsoluteZCoordinates = receiverHasAbsoluteZCoordinates;
    }

    /**
     * @return True if provided Z value are sea level (false for relative to ground level)
     */
    public boolean isSourceHasAbsoluteZCoordinates() {
        return sourceHasAbsoluteZCoordinates;
    }

    /**
     * @param sourceHasAbsoluteZCoordinates True if provided Z value are sea level (false for relative to ground level)
     */
    public void setSourceHasAbsoluteZCoordinates(boolean sourceHasAbsoluteZCoordinates) {
        this.sourceHasAbsoluteZCoordinates = sourceHasAbsoluteZCoordinates;
    }

    public boolean iszBuildings() {
        return buildingTableParameters.zBuildings;
    }

    public void setzBuildings(boolean zBuildings) {
        buildingTableParameters.zBuildings = zBuildings;
    }

    /**
     * Extracted from NMPB 2008-2 7.3.2
     * Soil areas POLYGON, with a dimensionless coefficient G:
     *  - Law, meadow, field of cereals G=1
     *  - Undergrowth (resinous or decidious) G=1
     *  - Compacted earth, track G=0.3
     *  - Road surface G=0
     *  - Smooth concrete G=0
     * @param soilTableName Table name of grounds properties
     */
    public void setSoilTableName(String soilTableName) {
        this.soilTableName = soilTableName;
    }

    /**
     * Digital Elevation model table name. Currently only a table with POINTZ column is supported.
     * DEM points too close with buildings are not fetched.
     * @return Digital Elevation model table name
     */
    public String getDemTable() {
        return demTable;
    }

    /**
     * Digital Elevation model table name. Currently only a table with POINTZ column is supported.
     * DEM points too close with buildings are not fetched.
     * @param demTable Digital Elevation model table name
     */
    public void setDemTable(String demTable) {
        this.demTable = demTable;
    }

    /**
     * Field name of the {@link #sourcesTableName}HERTZ. Where HERTZ is a number [100-5000].
     * Without the hertz value.
     * @return Hertz field prefix
     */
    public String getSound_lvl_field() {
        return sound_lvl_field;
    }

    /**
     * Field name of the {@link #sourcesTableName}HERTZ. Where HERTZ is a number [100-5000].
     * Without the hertz value.
     * @param sound_lvl_field Hertz field prefix
     */
    public void setSound_lvl_field(String sound_lvl_field) {
        this.sound_lvl_field = sound_lvl_field;
    }

    /**
     * @return Sound propagation stop at this distance, default to 750m.
     * Computation cell size if proportional with this value.
     */

    public double getMaximumPropagationDistance() {
        return maximumPropagationDistance;
    }

    /**
     * @param maximumPropagationDistance  Sound propagation stop at this distance, default to 750m.
     * Computation cell size if proportional with this value.
     */
    public void setMaximumPropagationDistance(double maximumPropagationDistance) {
        this.maximumPropagationDistance = maximumPropagationDistance;
    }

    /**
     *
     */
    public void setGs(double gs) {
        this.gs = gs;
    }

    public double getGs() {
        return this.gs;
    }

    /**
     * @return Reflection and diffraction maximum search distance, default to 400m.
    */
    public double getMaximumReflectionDistance() {
        return maximumReflectionDistance;
    }

    /**
     * @param maximumReflectionDistance Reflection and diffraction seek walls and corners up to X meters
     *                                  from the direct propagation line. Default to 100m.
     */
    public void setMaximumReflectionDistance(double maximumReflectionDistance) {
        this.maximumReflectionDistance = maximumReflectionDistance;
    }

    /**
     * @return Sound reflection order. 0 order mean 0 reflection depth.
     * 2 means propagation of rays up to 2 collision with walls.
     */
    public int getSoundReflectionOrder() {
        return soundReflectionOrder;
    }

    /**
     * @param soundReflectionOrder Sound reflection order. 0 order mean 0 reflection depth.
     * 2 means propagation of rays up to 2 collision with walls.
     */
    public void setSoundReflectionOrder(int soundReflectionOrder) {
        this.soundReflectionOrder = soundReflectionOrder;
    }

    /**
     * @return True if diffraction rays will be computed on vertical edges (around buildings)
     */
    public boolean isComputeHorizontalDiffraction() {
        return computeHorizontalDiffraction;
    }

    /**
     * @param computeHorizontalDiffraction True if diffraction rays will be computed on vertical edges (around buildings)
     */
    public void setComputeHorizontalDiffraction(boolean computeHorizontalDiffraction) {
        this.computeHorizontalDiffraction = computeHorizontalDiffraction;
    }

    /**
     * @return Global default wall absorption on sound reflection.
     */
    public double getWallAbsorption() {
        return buildingTableParameters.defaultWallAbsorption;
    }

    /**
     * @param wallAbsorption Set default global wall absorption on sound reflection.
     */
    public void setWallAbsorption(double wallAbsorption) {
        buildingTableParameters.defaultWallAbsorption = wallAbsorption;
    }

    /**
     * @return {@link #getBuildingsTableName()} eName} table field name for buildings height above the ground.
     */

    public String getHeightField() {
        return buildingTableParameters.heightField;
    }

    /**
     * @param heightField {@link #getBuildingsTableName()}} table field name for buildings height above the ground.
     */
    public void setHeightField(String heightField) {
        buildingTableParameters.heightField = heightField;
    }

    /**
     * @return The envelope of computation area.
    */
    public Envelope getMainEnvelope() {
        return mainEnvelope;
    }

    /**
     * Set computation area. Update the property subdivisionLevel and gridDim.
     * @param mainEnvelope Computation area
     */
    public void setMainEnvelope(Envelope mainEnvelope) {
        this.mainEnvelope = mainEnvelope;
        if(gridDim == 0) {
            // Split domain into 4^subdiv cells
            // Compute subdivision level using envelope and maximum propagation distance
            double greatestSideLength = mainEnvelope.maxExtent();
            int subdivisionLevel = 0;
            while(maximumPropagationDistance / (greatestSideLength / Math.pow(2, subdivisionLevel)) < MINIMAL_BUFFER_RATIO) {
                subdivisionLevel++;
            }
            gridDim = (int) Math.pow(2, subdivisionLevel);
        }
    }

    /**
     * @return True if diffraction of horizontal edges is computed.
     */
    public boolean isComputeVerticalDiffraction() {
        return computeVerticalDiffraction;
    }

    /**
     * Activate of deactivate diffraction of horizontal edges. Height of buildings must be provided.
     * @param computeVerticalDiffraction New value
     */
    public void setComputeVerticalDiffraction(boolean computeVerticalDiffraction) {
        this.computeVerticalDiffraction = computeVerticalDiffraction;
    }

}
