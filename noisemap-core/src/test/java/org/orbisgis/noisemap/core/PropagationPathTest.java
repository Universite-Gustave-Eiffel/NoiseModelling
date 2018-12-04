package org.orbisgis.noisemap.core;

import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class PropagationPathTest {

    private static final double ERROR_EPSILON_TEST_T = 0.2;
    private static final List<Integer> freqLvl = Collections.unmodifiableList(Arrays.asList(63, 125, 250, 500, 1000, 2000,
            4000, 8000));


    private void splCompare(double[] resultW, String testName, double[] expectedLevel, double splEpsilon) {
        for (int i = 0; i < resultW.length; i++) {
            double dba = resultW[i];
            double expected = expectedLevel[i];
            assertEquals("Unit test " + testName + " failed at " + freqLvl.get(i) + " Hz", expected, dba, splEpsilon);
        }
    }

    private static ArrayList<Double> asW(double... dbValues) {
        ArrayList<Double> ret = new ArrayList<>(dbValues.length);
        for (double db_m : dbValues) {
            ret.add(PropagationProcess.dbaToW(db_m));
        }
        return ret;
    }

    /**
     * Test for test
     */
    @Test
    public void TestPropagationPath(){
        boolean favorable = true;
        List<PropagationPath.PointPath> points = new ArrayList<PropagationPath.PointPath>();
        List<PropagationPath.SegmentPath>  segments = new ArrayList<PropagationPath.SegmentPath>();

        points.add(new PropagationPath.PointPath(new Coordinate(0,0,0),1,0,Double.NaN,PropagationPath.PointPath.POINT_TYPE.SRCE));
        points.add(new PropagationPath.PointPath(new Coordinate(10,0,0),1,Double.NaN,0.5,PropagationPath.PointPath.POINT_TYPE.DIFH));
        points.add(new PropagationPath.PointPath(new Coordinate(20,0,0),1,Double.NaN,0.5,PropagationPath.PointPath.POINT_TYPE.DIFH));
        points.add(new PropagationPath.PointPath(new Coordinate(30,30,0),1,0,Double.NaN,PropagationPath.PointPath.POINT_TYPE.RECV));
        segments.add(new PropagationPath.SegmentPath(1));
        segments.add(new PropagationPath.SegmentPath(1));

        PropagationPath propagationPath = new PropagationPath(favorable,points,segments);
        PropagationProcessPathData propData = new PropagationProcessPathData();
        propData.setTemperature(15);
        propData.setHumidity(70);

        EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
        evaluateAttenuationCnossos.evaluate(propagationPath, propData);
    }

    /**
     * Sound propagation
     * T01
     * Horizontal ground with homogeneous properties, close receiver - Reflective ground (G=0)
     */
    @Test
    public void T01(){
        boolean favorable = false;
        List<PropagationPath.PointPath> points = new ArrayList<PropagationPath.PointPath>();
        List<PropagationPath.SegmentPath>  segments = new ArrayList<PropagationPath.SegmentPath>();

        points.add(new PropagationPath.PointPath(new Coordinate(0,0,1),0,0,Double.NaN,PropagationPath.PointPath.POINT_TYPE.SRCE));
        points.add(new PropagationPath.PointPath(new Coordinate(200,0,4),0,0,Double.NaN,PropagationPath.PointPath.POINT_TYPE.RECV));
        segments.add(new PropagationPath.SegmentPath(0));

        PropagationPath propagationPath = new PropagationPath(favorable,points,segments);
        PropagationProcessPathData propData = new PropagationProcessPathData();
        propData.setTemperature(15);
        propData.setHumidity(70);

        EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
        splCompare(evaluateAttenuationCnossos.evaluate(propagationPath, propData), "Test T01", new double[]{-54, -54.1, -54.2, -54.5, -54.8, -55.8, -59.3, -73.0}, ERROR_EPSILON_TEST_T);


    }

    /**
     * Sound propagation
     * T02H
     * Horizontal ground with homogeneous properties, road source - Absorbing ground (G=1)
     */
    @Test
    public void T02H() throws LayerDelaunayError {
        List<PropagationPath.PointPath> points = new ArrayList<PropagationPath.PointPath>();
        List<PropagationPath.SegmentPath> segments = new ArrayList<PropagationPath.SegmentPath>();

        points.add(new PropagationPath.PointPath(new Coordinate(0, 0, 0.05), 0, 0, Double.NaN, PropagationPath.PointPath.POINT_TYPE.SRCE));
        points.add(new PropagationPath.PointPath(new Coordinate(200, 0, 4), 0, 0, Double.NaN, PropagationPath.PointPath.POINT_TYPE.RECV));
        segments.add(new PropagationPath.SegmentPath(1));

        PropagationPath propagationPath = new PropagationPath(false, points, segments);
        PropagationProcessPathData propData = new PropagationProcessPathData();
        propData.setTemperature(15);
        propData.setHumidity(70);

        EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
        splCompare(evaluateAttenuationCnossos.evaluate(propagationPath, propData), "Test T02H", new double[]{-57, -57.1, -57.9, -64.7, -70.6, -76.7, -82.0, -88.3}, ERROR_EPSILON_TEST_T);
    }

    /**
     * Sound propagation
     * T02F
     * Horizontal ground with homogeneous properties, road source - Absorbing ground (G=1)
     */
    @Test
    public void T02F() throws LayerDelaunayError {
        List<PropagationPath.PointPath> points = new ArrayList<PropagationPath.PointPath>();
        List<PropagationPath.SegmentPath> segments = new ArrayList<PropagationPath.SegmentPath>();

        points.add(new PropagationPath.PointPath(new Coordinate(0, 0, 0), 0.05, 0, Double.NaN, PropagationPath.PointPath.POINT_TYPE.SRCE));
        points.add(new PropagationPath.PointPath(new Coordinate(200, 0, 0), 4, 0, Double.NaN, PropagationPath.PointPath.POINT_TYPE.RECV));
        segments.add(new PropagationPath.SegmentPath(1));

        PropagationPath propagationPath = new PropagationPath(true, points, segments);
        PropagationProcessPathData propData = new PropagationProcessPathData();
        propData.setTemperature(15);
        propData.setHumidity(70);

        EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
        splCompare(evaluateAttenuationCnossos.evaluate(propagationPath, propData), "Test T02F", new double[]{-57, -57.1, -57.2, -58.5, -65.8, -60.4, -62.3, -76.0}, ERROR_EPSILON_TEST_T);
    }

    /**
     * Sound propagation
     * T03F
     * Horizontal ground with homogeneous properties, road source - Absorbing ground (G=0.7)
     */
    @Test
    public void T03F() throws LayerDelaunayError {
        List<PropagationPath.PointPath> points = new ArrayList<PropagationPath.PointPath>();
        List<PropagationPath.SegmentPath> segments = new ArrayList<PropagationPath.SegmentPath>();

        points.add(new PropagationPath.PointPath(new Coordinate(0, 0, 0), 0.05, 0, Double.NaN, PropagationPath.PointPath.POINT_TYPE.SRCE));
        points.add(new PropagationPath.PointPath(new Coordinate(200, 0, 0), 4, 0, Double.NaN, PropagationPath.PointPath.POINT_TYPE.RECV));
        segments.add(new PropagationPath.SegmentPath(0.7));

        PropagationPath propagationPath = new PropagationPath(true, points, segments);
        PropagationProcessPathData propData = new PropagationProcessPathData();
        propData.setTemperature(15);
        propData.setHumidity(70);

        EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
        splCompare(evaluateAttenuationCnossos.evaluate(propagationPath, propData), "Test T03F", new double[]{-56.1, -56.2, -56.3, -56.6, -61.6, -61.1, -61.4, -75.1}, ERROR_EPSILON_TEST_T);
    }

    /**
     * Sound propagation
     * T03H
     * Horizontal ground with homogeneous properties, road source - Absorbing ground (G=0.7)
     */
    @Test
    public void T03H() throws LayerDelaunayError {
        List<PropagationPath.PointPath> points = new ArrayList<PropagationPath.PointPath>();
        List<PropagationPath.SegmentPath> segments = new ArrayList<PropagationPath.SegmentPath>();

        points.add(new PropagationPath.PointPath(new Coordinate(0, 0, 0), 0.05, 0, Double.NaN, PropagationPath.PointPath.POINT_TYPE.SRCE));
        points.add(new PropagationPath.PointPath(new Coordinate(200, 0, 0), 4, 0, Double.NaN, PropagationPath.PointPath.POINT_TYPE.RECV));
        segments.add(new PropagationPath.SegmentPath(0.7));

        PropagationPath propagationPath = new PropagationPath(false, points, segments);
        PropagationProcessPathData propData = new PropagationProcessPathData();
        propData.setTemperature(15);
        propData.setHumidity(70);

        EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
        splCompare(evaluateAttenuationCnossos.evaluate(propagationPath, propData), "Test T03H", new double[]{-56.1, -56.2, -56.3, -60.6, -66.0, -72.6, -80.8, -88.8}, ERROR_EPSILON_TEST_T);
    }

}