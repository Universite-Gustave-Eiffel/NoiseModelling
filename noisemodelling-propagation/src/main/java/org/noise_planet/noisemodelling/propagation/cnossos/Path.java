/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.propagation.cnossos;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.math.Vector3D;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutPoint;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;
import org.noise_planet.noisemodelling.pathfinder.utils.geometry.Orientation;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.noise_planet.noisemodelling.pathfinder.utils.geometry.GeometryUtils.projectPointOnSegment;

/**
 * PropagationPath
 * @author Nicolas Fortin
 * @author Pierre Aumond
 */

// todo get out all the useless computations and functions
// todo please revise public, private, etc.

public class Path {
    public static final int FOOTER_RESERVED_SIZE = 120; // reserved size for geojson footer
    CutProfile cutProfile; // vertical plane between source and receiver used to compute the propagation ray path attributes
    // given by user
    private SegmentPath srSegment; // list of source-receiver path (including prime path)
    private List<PointPath> pointList; // list of points (source, receiver or diffraction and reflection points)
    private List<SegmentPath> segmentList; // list of segments [S,O1] and [On-1,R] (O1 and On-1 are respectively the first diffraction point and On-1 the last diffration point)
    private boolean favourable; // if true, favourable meteorological condition path with curved rays
    private String timePeriod=""; // time period if relevant (day, evening, night or other parameters, use LDenConfig.TIME_PERIOD)
    Orientation sourceOrientation = new Orientation(0,0,0);
    public Orientation raySourceReceiverDirectivity = new Orientation(); // direction of the source->receiver path relative to the source heading
    double gs;
    // computed in Augmented Path
    public boolean keepAbsorption = false;

    public Path() {
    }

    public Path(CutProfile cutProfile) {
        this.cutProfile = cutProfile;
        setSourceOrientation(cutProfile.getSource().orientation);
    }

    public Path(Path other) {
        this.cutProfile = other.cutProfile;
        this.srSegment = other.srSegment;
        this.pointList = other.pointList;
        this.segmentList = other.segmentList;
        this.favourable = other.favourable;
        this.timePeriod = other.timePeriod;
        this.sourceOrientation = other.sourceOrientation;
        this.raySourceReceiverDirectivity = other.raySourceReceiverDirectivity;
        this.gs = other.gs;
        this.keepAbsorption = other.keepAbsorption;
    }

    /**
     * 3D intersections points of the ray
     * @return
     */
    public List<CutPoint> getCutPoints() {
        if(cutProfile == null) {
            return new ArrayList<>();
        } else {
            return cutProfile.cutPoints;
        }
    }

    /**
     * @return Get vertical plane between source and receiver used to compute the propagation ray path attributes
     */
    public CutProfile getCutProfile() {
        return cutProfile;
    }

    /**
     * @param cutProfile vertical plane between source and receiver used to compute the propagation ray path attributes
     */
    public void setCutProfile(CutProfile cutProfile) {
        this.cutProfile = cutProfile;
    }


    /**
     * @return time period if relevant (day, evening, night or other parameters, use LDenConfig.TIME_PERIOD)
     */
    public String getTimePeriod() {
        return timePeriod;
    }

    /**
     * @param timePeriod time period if relevant (day, evening, night or other parameters, use LDenConfig.TIME_PERIOD)
     */
    public void setTimePeriod(String timePeriod) {
        this.timePeriod = timePeriod;
    }

    public Orientation getSourceOrientation() {
        return sourceOrientation;
    }

    public void setSourceOrientation(Orientation sourceOrientation) {
        this.sourceOrientation = sourceOrientation;
    }

    public Orientation getRaySourceReceiverDirectivity() {
        return raySourceReceiverDirectivity;
    }

    public void setRaySourceReceiverDirectivity(Orientation raySourceReceiverDirectivity) {
        this.raySourceReceiverDirectivity = raySourceReceiverDirectivity;
    }

    /**
     * @return Ground factor of the source area. Gs=0 for road platforms, slab tracks. Gs=1 for rail tracks on ballast
     */
    public double getGs() {
        return gs;
    }

    public void setGs(double gs) {
        this.gs = gs;
    }


    /**
     * @return Propagation path as a geometry object
     */
    public LineString asGeom() {
        // try to compute 3d ray geometry using two different list of points (one in 2D and the ground cut points in 3d)
        GeometryFactory geometryFactory = new GeometryFactory();
        Coordinate[] coordinates = new Coordinate[pointList == null ? 0  : pointList.size()];
        int i=0;
        double cutPointDistance = 0;
        int cutPointCursor = 0;
        if(getCutPoints().isEmpty() || coordinates.length <= 1) {
            return geometryFactory.createLineString();
        }
        for(PointPath pointPath : pointList) {
            // report x,y from cut point
            while(cutPointCursor < getCutPoints().size() - 1) {
                if(pointPath.coordinate.x > cutPointDistance) {
                    cutPointCursor++;
                    cutPointDistance += getCutPoints().get(cutPointCursor-1).getCoordinate()
                            .distance(getCutPoints().get(cutPointCursor).getCoordinate());
                } else {
                    break;
                }
            }
            Coordinate rayPoint = new Coordinate(getCutPoints().get(cutPointCursor).getCoordinate());
            rayPoint.setZ(pointPath.coordinate.y);
            if(cutPointCursor > 0) {
                final Coordinate p0 = getCutPoints().get(cutPointCursor - 1).getCoordinate();
                final Coordinate p1 = getCutPoints().get(cutPointCursor).getCoordinate();
                double distanceP0P1 = p1.distance(p0);
                // compute ratio of pointPath position between p0 and p1
                double ratio = Math.min(1, Math.max(0, (pointPath.coordinate.x - (cutPointDistance - distanceP0P1)) / distanceP0P1));
                // interpolate x,y coordinates
                rayPoint = new LineSegment(p0, p1).pointAlong(ratio);
                rayPoint.setZ(pointPath.coordinate.y);
            }
            coordinates[i++] = new Coordinate(rayPoint);
        }
        return geometryFactory.createLineString(coordinates);
    }

    public List<PointPath> getPointList() {return pointList;}

    public List<SegmentPath> getSegmentList() {return segmentList;}

    public SegmentPath getSRSegment() {return srSegment;}

    public void setPointList(List<PointPath> pointList) {this.pointList = pointList;}

    public void setSegmentList(List<SegmentPath>  segmentList) {this.segmentList = segmentList;}

    public void setSRSegment(SegmentPath srSegment) {this.srSegment = srSegment;}


    public Path(List<SegmentPath> segmentList) {
        this.segmentList = segmentList;
    }

    public boolean isFavourable() {
        return favourable;
    }

    public void setFavourable(boolean favourable) {
        this.favourable = favourable;
    }

    double computeZs(SegmentPath segmentPath) {
        double zs = pointList.get(segmentPath.idPtStart).coordinate.z - projectPointOnSegment(pointList.get(segmentPath.idPtStart).coordinate,segmentPath.meanGdPlane,segmentPath.pInit).z;
        return ((zs > 0) ? zs : 0); // Section 2.5.3 - If the equivalent height of a point becomes negative, i.e. if the point is located below the mean ground plane, a null height is retained, and the equivalent point is then identical with its possible image.
    }

    public double computeZr(SegmentPath segmentPath) {
        double zr = pointList.get(segmentPath.idPtFinal).coordinate.z - projectPointOnSegment(pointList.get(segmentPath.idPtFinal).coordinate,segmentPath.meanGdPlane,segmentPath.pInit).z;
        return ((zr > 0) ? zr : 0); // Section 2.5.3 - If the equivalent height of a point becomes negative, i.e. if the point is located below the mean ground plane, a null height is retained, and the equivalent point is then identical with its possible image.
    }

    /**
     * Eq 2.5.19
     * @param segmentPath
     * @return
     */
    public double computeZsPrime(SegmentPath segmentPath) {
        // The height corrections deltazs and deltazr convey the effect of the sound ray bending. deltazT accounts for the effect of the turbulence.
        double alpha0 = 2 * Math.pow(10, -4);
        double deltazt = 6 * Math.pow(10, -3) * segmentPath.dp / (segmentPath.zsH + segmentPath.zrH);
        double deltazs = alpha0 * Math.pow((segmentPath.zsH / (segmentPath.zsH + segmentPath.zrH)), 2) * (Math.pow(segmentPath.dp, 2) / 2);
        return segmentPath.zsH + deltazs + deltazt;
    }

    /**
     * Eq 2.5.19
     * @param segmentPath
     * @return
     */
    public double computeZrPrime(SegmentPath segmentPath) {
        // The height corrections deltazs and deltazr convey the effect of the sound ray bending. deltazT accounts for the effect of the turbulence.
        double alpha0 = 2 * Math.pow(10, -4);
        double deltazt = 6 * Math.pow(10, -3) * segmentPath.dp / (segmentPath.zsH + segmentPath.zrH);
        double deltazr = alpha0 * Math.pow((segmentPath.zrH / (segmentPath.zsH + segmentPath.zrH)), 2) * (Math.pow(segmentPath.dp, 2) / 2);
        return segmentPath.zrH + deltazr + deltazt;
    }

    /*
     * Eq.2.5.24 and Eq. 2.5.25
     * @param dSeg
     * @param d
     * @return
     */

    private double getRayCurveLength(double dSeg,double d) {
        double gamma = Math.max(1000,8*d); // Eq. 2.5.24
        return 2*gamma*Math.asin(dSeg/(2*gamma)); // Eq. 2.5.25

    }


    /**
     *
     * @param out
     * @param p
     * @throws IOException
     */
    public static void writeCoordinate(DataOutputStream out, Coordinate p) throws IOException {
        out.writeDouble(p.x);
        out.writeDouble(p.y);
        out.writeDouble(p.z);
    }


    /**
     *
     * @param in
     * @return
     * @throws IOException
     */
    public static Coordinate readCoordinate(DataInputStream in) throws IOException {
        return new Coordinate(in.readDouble(), in.readDouble(), in.readDouble());
    }

    /**
     *
     * @param out
     * @param p
     * @throws IOException
     */
    public static void writeVector(DataOutputStream out, Vector3D p) throws IOException {
        out.writeDouble(p.getX());
        out.writeDouble(p.getY());
        out.writeDouble(p.getZ());
    }


    /**
     *
     * @param in
     * @return
     * @throws IOException
     */
    public static Vector3D readVector(DataInputStream in) throws IOException {
        return new Vector3D(in.readDouble(), in.readDouble(), in.readDouble());
    }

}
