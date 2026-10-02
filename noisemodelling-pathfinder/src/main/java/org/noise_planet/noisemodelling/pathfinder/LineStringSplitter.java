package org.noise_planet.noisemodelling.pathfinder;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import java.util.List;

import static java.lang.Double.isNaN;
import static java.lang.Math.*;

/**
 * Utility to split LineString into representative points used as point-sources.
 *
 * Single responsibility: compute sample points along a LineString.
 * This class should only decide where to place sample points on linear
 * sources (based on length and a segment size constraint). It must not
 * be concerned with orientation, visibility, or profile creation.
 */
public final class LineStringSplitter {
    private LineStringSplitter() {}

    /**
     * Splits a LineString into representative points used as point-sources.
     * If the linear sound source length is inferior to half the distance between the nearest point of the sound
     * source and the receiver then it can be modelled as a single point source.
     *
     * @param geom LineString geometry to split
     * @param segmentSizeConstraint Maximum segment size constraint
     * @param pts Output list to store computed points
     * @return target segment size used (or geom length for short geometries)
     */
    public static double splitLineStringIntoPoints(LineString geom, double segmentSizeConstraint,
                                                   List<Coordinate> pts) {
        double geomLength = geom.getLength();
        
        if (isShortGeometry(geomLength, segmentSizeConstraint)) {
            return handleShortGeometry(geom, pts);
        } else {
            return handleLongGeometry(geom, geomLength, segmentSizeConstraint, pts);
        }
    }

    /**
     * Checks if the geometry is considered short based on the segment size constraint.
     * 
     * @param geomLength Length of the geometry
     * @param segmentSizeConstraint Maximum segment size constraint
     * @return true if geometry is short, false otherwise
     */
    private static boolean isShortGeometry(double geomLength, double segmentSizeConstraint) {
        return geomLength < segmentSizeConstraint;
    }

    /**
     * Handles short geometries by computing and adding a single midpoint.
     * 
     * @param geom LineString geometry
     * @param pts Output list to store the midpoint
     * @return The geometry length
     */
    private static double handleShortGeometry(LineString geom, List<Coordinate> pts) {
        Coordinate[] points = geom.getCoordinates();
        double segmentLength = 0;
        final double targetSegmentSize = geom.getLength() / 2.0;
        
        for (int i = 0; i < points.length - 1; i++) {
            Coordinate a = points[i];
            final Coordinate b = points[i + 1];
            double length = calculateDistance(a, b);
            
            if (length + segmentLength > targetSegmentSize) {
                double segmentLengthFraction = (targetSegmentSize - segmentLength) / length;
                Coordinate midPoint = interpolateCoordinate(a, b, segmentLengthFraction);
                pts.add(midPoint);
                break;
            }
            segmentLength += length;
        }
        return geom.getLength();
    }

    /**
     * Handles long geometries by splitting them into multiple segments.
     * 
     * @param geom LineString geometry
     * @param geomLength Length of the geometry
     * @param segmentSizeConstraint Maximum segment size constraint
     * @param pts Output list to store computed points
     * @return The target segment size used
     */
    private static double handleLongGeometry(LineString geom, double geomLength, 
                                           double segmentSizeConstraint, List<Coordinate> pts) {
        double targetSegmentSize = calculateTargetSegmentSize(geomLength, segmentSizeConstraint);
        Coordinate[] points = geom.getCoordinates();
        
        SegmentState state = new SegmentState();
        
        for (int i = 0; i < points.length - 1; i++) {
            processSegment(points[i], points[i + 1], targetSegmentSize, state, pts);
        }
        
        // Add final midpoint if exists
        if (state.midPoint != null) {
            pts.add(state.midPoint);
        }
        
        return targetSegmentSize;
    }

    /**
     * Calculates the target segment size for long geometries.
     * 
     * @param geomLength Length of the geometry
     * @param segmentSizeConstraint Maximum segment size constraint
     * @return Calculated target segment size
     */
    private static double calculateTargetSegmentSize(double geomLength, double segmentSizeConstraint) {
        return geomLength / ceil(geomLength / segmentSizeConstraint);
    }

    /**
     * Processes a single segment between two coordinates.
     * 
     * @param startPoint Starting coordinate
     * @param endPoint Ending coordinate
     * @param targetSegmentSize Target size for each segment
     * @param state Current segment processing state
     * @param pts Output list to store computed points
     */
    private static void processSegment(Coordinate startPoint, Coordinate endPoint, double targetSegmentSize,
                                     SegmentState state, List<Coordinate> pts) {
        Coordinate a = startPoint;
        Coordinate b = endPoint;
        double length = calculateDistance(a, b);
        
        while (length + state.segmentLength > targetSegmentSize) {
            double segmentLengthFraction = (targetSegmentSize - state.segmentLength) / length;
            Coordinate splitPoint = interpolateCoordinate(a, b, segmentLengthFraction);
            
            // Calculate midpoint if not yet found
            if (state.midPoint == null && length + state.segmentLength > targetSegmentSize / 2) {
                double midFraction = (targetSegmentSize / 2.0 - state.segmentLength) / length;
                state.midPoint = interpolateCoordinate(a, b, midFraction);
            }
            
            pts.add(state.midPoint);
            
            // Update for next iteration
            a = splitPoint;
            length = calculateDistance(a, b);
            state.segmentLength = 0;
            state.midPoint = null;
        }
        
        // Check for midpoint in remaining segment
        if (state.midPoint == null && length + state.segmentLength > targetSegmentSize / 2) {
            double midFraction = (targetSegmentSize / 2.0 - state.segmentLength) / length;
            state.midPoint = interpolateCoordinate(a, b, midFraction);
        }
        
        state.segmentLength += length;
    }

    /**
     * Calculates the distance between two coordinates, handling NaN values.
     * 
     * @param a First coordinate
     * @param b Second coordinate
     * @return Distance between coordinates
     */
    private static double calculateDistance(Coordinate a, Coordinate b) {
        double length = a.distance3D(b);
        if (isNaN(length)) {
            length = a.distance(b);
        }
        return length;
    }

    /**
     * Interpolates a coordinate between two points using the given fraction.
     * 
     * @param a Starting coordinate
     * @param b Ending coordinate
     * @param fraction Interpolation fraction (0.0 to 1.0)
     * @return Interpolated coordinate
     */
    private static Coordinate interpolateCoordinate(Coordinate a, Coordinate b, double fraction) {
        return new Coordinate(
            a.x + fraction * (b.x - a.x),
            a.y + fraction * (b.y - a.y),
            a.z + fraction * (b.z - a.z)
        );
    }

    /**
     * Helper class to maintain state during segment processing.
     */
    private static class SegmentState {
        double segmentLength = 0.0;
        Coordinate midPoint = null;
    }
}
