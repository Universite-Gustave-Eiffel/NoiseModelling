package org.noise_planet.noisemodelling.pathfinder;

import java.util.ArrayList;
import java.util.List;
    

import org.locationtech.jts.algorithm.CGAlgorithms3D;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.triangulate.quadedge.Vertex;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.Bridge;
import org.noise_planet.noisemodelling.pathfinder.path.Scene;
import org.noise_planet.noisemodelling.pathfinder.path.BridgeRelationship;
    

/**
 * Utility responsible for converting Z values of geometries and coordinate lists
 * from relative to absolute representations using a {@link ProfileBuilder}.
 *
 * Single responsibility: convert Z values from relative to absolute form. This class
 * transforms elevation (Z) information by adding ground elevation using the
 * provided {@link ProfileBuilder}. It must not perform sampling, visibility
 * checks, pathfinding, or high-level profile assembly.
 *
 * <p>This class treats simple coordinate lists (receivers) differently from complex
 * geometry objects (sources). Coordinate lists are mutated in-place; geometries are
 * rebuilt when necessary (for example, to project a LineString onto the DEM and
 * insert interpolated vertices).
 */
public class ElevationConverter {
    Scene scene;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final double MIN_INTERPOLATION_DISTANCE = 0.1;
    public static final double DEFAULT_SOURCE_HEIGHT_ON_ROAD = 0.05;
    public static final double DEFAULT_SOURCE_HEIGHT_BELOW_BRIDGE = -0.05;


    /**
     * Create a converter with a {@link Scene} used to query ground elevation (DEM) 
     * and bridge properties when converting Z values.
     *
     * @param scene the scene containing ProfileBuilder and bridge properties for elevation conversion
     */
    public ElevationConverter(Scene scene) {
        this.scene = scene;
    }
        

    /**
     * Change Z values of a list of {@link Coordinate} objects from relative to absolute
     * by adding ground elevation.
     *
     * <p>The ground elevation at each coordinate is added to the coordinate's Z.
     * The method mutates the provided list in-place.
     *
     * @param coords list of coordinates to modify (mutated in-place)
     */
    public void changeCoordinates(List<Coordinate> coords) {
        for (Coordinate coord : coords) {
            coord.setZ(coord.getZ() + scene.profileBuilder.getZGround(coord));
        }
    }

    /**
     * Calculate absolute elevation for a source coordinate based on source type.
     * This static method provides elevation conversion logic that can be used by
     * other components (e.g., SourceCollector) during source point sampling.
     * 
     * @param coord Source coordinate (relative Z from LW_ROADS)
     * @param bridgeRelationship Source bridge relationship
     * @param scene Scene containing DEM and bridge data
     * @return Absolute elevation (sea level reference)
     */
    public static double calculateAbsoluteElevation(Coordinate coord, BridgeRelationship bridgeRelationship, Scene scene) {
        if(Double.isNaN(coord.z)) {
            throw new IllegalArgumentException("Coordinate Z value is NaN");
        }
        ProfileBuilder profileBuilder = scene.profileBuilder;
        if (profileBuilder == null) {
            throw new IllegalStateException("ProfileBuilder is required for elevation calculation");
        }
        
        // If bridgeRelationship is null, treat as SOURCE_NOT_RELATED_TO_BRIDGE (default behavior)
        if (bridgeRelationship == null) {
            return profileBuilder.getZGround(coord) + coord.z;
        }
        
        BridgeRelationship.RelationType relationType = bridgeRelationship.getRelationType();
        
        switch (relationType) {
            case SOURCE_NOT_RELATED_TO_BRIDGE:
                // the input Z is relative to the ground
                // Ground elevation + original relative Z
                return profileBuilder.getZGround(coord) + coord.z;
                
            case ACTUAL_SOURCE_ON_BRIDGE:
                // the input Z is relative to the ground
                long bridgePkOn = bridgeRelationship.getBridgePkOn();
                Bridge bridgeOn = profileBuilder.getBridgeByPk(bridgePkOn);
                if (bridgeOn == null) {
                    throw new IllegalStateException("Bridge not found: " + bridgePkOn);
                }
                double deckHeightOn = bridgeOn.getDeckHeightAtPoint(coord);
                if (Double.isNaN(deckHeightOn)) {
                    throw new IllegalStateException("Cannot get deck height at coordinate: " + coord);
                }
                // Bridge deck height + original relative Z
                return deckHeightOn + coord.z;
                
            case IMAGINARY_SOURCE_UNDER_BRIDGE:
                // the input Z is relative to the ground
                long bridgePkUnder = bridgeRelationship.getBridgePkAbove();
                Bridge bridgeUnder = profileBuilder.getBridgeByPk(bridgePkUnder);
                if (bridgeUnder == null) {
                    throw new IllegalStateException("Bridge not found: " + bridgePkUnder);
                }
                double deckHeightUnder = bridgeUnder.getDeckHeightAtPoint(coord);
                double deckThicknessUnder = bridgeUnder.getDeckThicknessAtPoint(coord);
                if (Double.isNaN(deckHeightUnder) || Double.isNaN(deckThicknessUnder)) {
                    throw new IllegalStateException("Cannot get bridge properties at coordinate: " + coord);
                }
                // Bridge bottom + original relative Z
                return (deckHeightUnder - deckThicknessUnder) + coord.z;
                
            case MIRROR_SOURCE:
                // the input Z is absolute value (NOT relative to the ground)
                // MIRROR_SOURCE elevation calculated using reflection formula
                long bridgePkAbove = bridgeRelationship.getBridgePkAbove();
                Bridge bridgeAbove = profileBuilder.getBridgeByPk(bridgePkAbove);
                if (bridgeAbove == null) {
                    throw new IllegalStateException("Bridge above not found: " + bridgePkAbove);
                }
                double deckHeightAbove = bridgeAbove.getDeckHeightAtPoint(coord);
                double deckThicknessAbove = bridgeAbove.getDeckThicknessAtPoint(coord);
                if (Double.isNaN(deckHeightAbove) || Double.isNaN(deckThicknessAbove)) {
                    throw new IllegalStateException("Cannot get bridge above properties at coordinate: " + coord);
                }
                
                double bridgeBottom = deckHeightAbove - deckThicknessAbove;
                if (bridgeBottom < coord.z) {
                    throw new IllegalStateException("Bridge bottom is below the source coordinate: " + coord);
                }
                return coord.z + 2.0 * (bridgeBottom - coord.z);
                
            default:
                throw new IllegalArgumentException("Unknown source type: " + relationType);
        }
    }


    /**
     * Converts MultiLineString from relative to absolute coordinates.
     * 
     * @param multiLineString MultiLineString to convert
     * @return converted MultiLineString with absolute coordinates
     */
    private Geometry convertMultiLineStringToAbsolute(MultiLineString multiLineString) {
        LineString[] newGeom = new LineString[multiLineString.getNumGeometries()];
        for (int idGeom = 0; idGeom < multiLineString.getNumGeometries(); idGeom++) {
            newGeom[idGeom] = projectLineStringOntoDEM(
                (LineString) multiLineString.getGeometryN(idGeom), MIN_INTERPOLATION_DISTANCE);
        }
        return GEOMETRY_FACTORY.createMultiLineString(newGeom);
    }

    

    /**
     * Project a LineString onto the digital elevation model (DEM) by applying ground elevation to Z and
     * interpolating topographic points along each segment. Removes topographic points that are within
     * minInterpolationDistanceMm (in millimetres) of a linear interpolation between neighbours to avoid
     * redundant vertices produced by DEM triangulation.
     *
     * @param lineString the input LineString
     * @param minInterpolationDistanceMm minimum interpolation distance in millimetres; points whose deviation
     *        from the linear interpolation is less than this value are ignored
     * @return a new LineString with adjusted Z values and possibly additional vertices
     */
    private LineString projectLineStringOntoDEM(LineString lineString, double minInterpolationDistanceMm) {
        ArrayList<Coordinate> newGeomCoordinates = new ArrayList<>();
        Coordinate[] coordinates = lineString.getCoordinates();
        
        for (int idPoint = 0; idPoint < coordinates.length - 1; idPoint++) {
            processLineSegment(coordinates[idPoint], coordinates[idPoint + 1], 
                             newGeomCoordinates, minInterpolationDistanceMm, idPoint == 0);
        }
        
        return GEOMETRY_FACTORY.createLineString(newGeomCoordinates.toArray(new Coordinate[0]));
    }

    /**
     * Processes a single line segment by fetching topographic profile and adding interpolated points.
     * 
     * @param p0 start point of the segment
     * @param p1 end point of the segment
     * @param newGeomCoordinates output list to accumulate coordinates
     * @param minInterpolationDistanceMm minimum interpolation distance in millimetres
     * @param isFirstSegment true if this is the first segment in the line
     */
    private void processLineSegment(Coordinate p0, Coordinate p1, ArrayList<Coordinate> newGeomCoordinates,
                                   double minInterpolationDistanceMm, boolean isFirstSegment) {
        List<Coordinate> groundProfileCoordinates = new ArrayList<>();
        scene.profileBuilder.fetchTopographicProfile(groundProfileCoordinates, p0, p1, false);
        newGeomCoordinates.ensureCapacity(newGeomCoordinates.size() + groundProfileCoordinates.size());

        if (groundProfileCoordinates.size() < 2) {
            newGeomCoordinates.add(p0);
            newGeomCoordinates.add(p1);
        } else {

            // Handle complex segment with topographic profile            
            if (isFirstSegment) {
                // Add the start point of the first segment with ground elevation
                newGeomCoordinates.add(new Coordinate(p0.x, p0.y, p0.z + groundProfileCoordinates.get(0).z));
            }
            // Add filtered intermediate points
            Coordinate previous = groundProfileCoordinates.get(0);
            
            for (int groundPoint = 1; groundPoint < groundProfileCoordinates.size() - 1; groundPoint++) {
                final Coordinate current = groundProfileCoordinates.get(groundPoint);
                final Coordinate next = groundProfileCoordinates.get(groundPoint + 1);
                
                if (CGAlgorithms3D.distancePointSegment(current, previous, next) >= minInterpolationDistanceMm) {
                    previous = current;
                    Coordinate interpolatedPoint = new Coordinate(current.x, current.y, current.z + Vertex.interpolateZ(current, p0, p1));
                    newGeomCoordinates.add(interpolatedPoint);
                }
            }

            // Add the end point of the segment with ground elevation
            newGeomCoordinates.add(new Coordinate(p1.x, p1.y, p1.z + groundProfileCoordinates.get(groundProfileCoordinates.size() - 1).z));

        }
    }

}
