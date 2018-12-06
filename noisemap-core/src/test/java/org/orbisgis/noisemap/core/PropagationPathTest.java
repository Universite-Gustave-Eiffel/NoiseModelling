package org.orbisgis.noisemap.core;

import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.math.Vector3D;

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
        List<PropagationPath.SegmentPath> srPath = new ArrayList<PropagationPath.SegmentPath>();
        Vector3D flatTopography = new Vector3D(new Coordinate(0,0,0),new Coordinate(0,0,0));

        points.add(new PropagationPath.PointPath(new Coordinate(0,0,0),1,0,Double.NaN,PropagationPath.PointPath.POINT_TYPE.SRCE));
        points.add(new PropagationPath.PointPath(new Coordinate(10,0,0),1,Double.NaN,0.5,PropagationPath.PointPath.POINT_TYPE.DIFH));
        points.add(new PropagationPath.PointPath(new Coordinate(20,0,0),1,Double.NaN,0.5,PropagationPath.PointPath.POINT_TYPE.DIFH));
        points.add(new PropagationPath.PointPath(new Coordinate(30,30,0),1,0,Double.NaN,PropagationPath.PointPath.POINT_TYPE.RECV));
        segments.add(new PropagationPath.SegmentPath(1, flatTopography));
        segments.add(new PropagationPath.SegmentPath(1, flatTopography));
        srPath.add(new PropagationPath.SegmentPath(1,flatTopography));

        PropagationPath propagationPath = new PropagationPath(favorable,points,segments,srPath);
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
        List<PropagationPath.SegmentPath> srPath = new ArrayList<PropagationPath.SegmentPath>();
        Vector3D flatTopography = new Vector3D(new Coordinate(0,0,0),new Coordinate(0,0,0));

        points.add(new PropagationPath.PointPath(new Coordinate(0,0,1),0,0,Double.NaN,PropagationPath.PointPath.POINT_TYPE.SRCE));
        points.add(new PropagationPath.PointPath(new Coordinate(200,0,4),0,0,Double.NaN,PropagationPath.PointPath.POINT_TYPE.RECV));
        segments.add(new PropagationPath.SegmentPath(0, flatTopography));
        srPath.add(new PropagationPath.SegmentPath(0,flatTopography));

        PropagationPath propagationPath = new PropagationPath(favorable,points,segments, srPath);
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
        List<PropagationPath.SegmentPath> srPath = new ArrayList<PropagationPath.SegmentPath>();
        Vector3D flatTopography = new Vector3D(new Coordinate(0,0,0),new Coordinate(200,0,0));

        points.add(new PropagationPath.PointPath(new Coordinate(0, 0, 0.05), 0, 0, Double.NaN, PropagationPath.PointPath.POINT_TYPE.SRCE));
        points.add(new PropagationPath.PointPath(new Coordinate(200, 0, 4), 0, 0, Double.NaN, PropagationPath.PointPath.POINT_TYPE.RECV));
        srPath.add(new PropagationPath.SegmentPath(1,flatTopography));

        PropagationPath propagationPath = new PropagationPath(false,points,segments, srPath);
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
        List<PropagationPath.SegmentPath> srPath = new ArrayList<PropagationPath.SegmentPath>();
        Vector3D flatTopography = new Vector3D(new Coordinate(0,0,0),new Coordinate(200,0,0));

        points.add(new PropagationPath.PointPath(new Coordinate(0, 0, 0.05), 0, 0, Double.NaN, PropagationPath.PointPath.POINT_TYPE.SRCE));
        points.add(new PropagationPath.PointPath(new Coordinate(200, 0, 4), 0, 0, Double.NaN, PropagationPath.PointPath.POINT_TYPE.RECV));
        srPath.add(new PropagationPath.SegmentPath(1,flatTopography));

        PropagationPath propagationPath = new PropagationPath(true,points,segments, srPath);
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
        List<PropagationPath.SegmentPath> srPath = new ArrayList<PropagationPath.SegmentPath>();
        Vector3D flatTopography = new Vector3D(new Coordinate(0,0,0),new Coordinate(200,0,0));

        points.add(new PropagationPath.PointPath(new Coordinate(0, 0, 0.05), 0.0, 0, Double.NaN, PropagationPath.PointPath.POINT_TYPE.SRCE));
        points.add(new PropagationPath.PointPath(new Coordinate(200, 0, 4.), 0., 0, Double.NaN, PropagationPath.PointPath.POINT_TYPE.RECV));
        srPath.add(new PropagationPath.SegmentPath(0.7,flatTopography));

        PropagationPath propagationPath = new PropagationPath(true,points,segments, srPath);
        PropagationProcessPathData propData = new PropagationProcessPathData();
        propData.setTemperature(15);
        propData.setHumidity(70);
        propData.setPrime2520(true);

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
        List<PropagationPath.SegmentPath> srPath = new ArrayList<PropagationPath.SegmentPath>();
        Vector3D flatTopography = new Vector3D(new Coordinate(0,0,0),new Coordinate(200,0,0));

        points.add(new PropagationPath.PointPath(new Coordinate(0, 0, 0.05), 0., 0, Double.NaN, PropagationPath.PointPath.POINT_TYPE.SRCE));
        points.add(new PropagationPath.PointPath(new Coordinate(200, 0, 4.), 0., 0, Double.NaN, PropagationPath.PointPath.POINT_TYPE.RECV));
        srPath.add(new PropagationPath.SegmentPath(0.7,flatTopography));

        PropagationPath propagationPath = new PropagationPath(false,points,segments, srPath);
        PropagationProcessPathData propData = new PropagationProcessPathData();
        propData.setTemperature(15);
        propData.setHumidity(70);
        propData.setGDisc(true);
        EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
        splCompare(evaluateAttenuationCnossos.evaluate(propagationPath, propData), "Test T03H", new double[]{-56.1, -56.2, -56.3, -60.6, -66.0, -72.6, -80.8, -88.8}, ERROR_EPSILON_TEST_T);
    }


    /**
     * Sound propagation
     * T05
     * Diffraction on horizontal edges - building
     *
     * @throws LayerDelaunayError
     */
    @Test
    public void T05H() throws LayerDelaunayError {

        List<PropagationPath.PointPath> points = new ArrayList<PropagationPath.PointPath>();
        List<PropagationPath.SegmentPath> segments = new ArrayList<PropagationPath.SegmentPath>();
        List<PropagationPath.SegmentPath> srPath = new ArrayList<PropagationPath.SegmentPath>();
        Vector3D flatTopography = new Vector3D(new Coordinate(0,0,0),new Coordinate(150,0,0));

        points.add(new PropagationPath.PointPath(new Coordinate(0, 0, 0.05), 0, 0, Double.NaN, PropagationPath.PointPath.POINT_TYPE.SRCE));
        points.add(new PropagationPath.PointPath(new Coordinate(50, 0, 10.0), 0, 0, Double.NaN, PropagationPath.PointPath.POINT_TYPE.DIFH));
        points.add(new PropagationPath.PointPath(new Coordinate(100, 0, 10.0), 0, 0, Double.NaN, PropagationPath.PointPath.POINT_TYPE.DIFH));
        points.add(new PropagationPath.PointPath(new Coordinate(150, 0, 2.0), 0, 0, Double.NaN, PropagationPath.PointPath.POINT_TYPE.RECV));
        // only first and last segment are necessary, even if it is possible to add more.
        segments.add(new PropagationPath.SegmentPath(0, flatTopography));
        segments.add(new PropagationPath.SegmentPath(0, flatTopography));
        srPath.add(new PropagationPath.SegmentPath(0,flatTopography));

        PropagationPath propagationPath = new PropagationPath(false,points,segments, srPath);
        PropagationProcessPathData propData = new PropagationProcessPathData();
        propData.setTemperature(15);
        propData.setHumidity(70);

        EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
        splCompare(evaluateAttenuationCnossos.evaluate(propagationPath, propData), "Test T05H", new double[]{-63.4, -67.2, -70.6, -73.9, -74.6, -75.3, -78.0, -88.2}, ERROR_EPSILON_TEST_T);

  }

    /**
     * Sound propagation
     * T05
     * Diffraction on horizontal edges - building
     *
     * @throws LayerDelaunayError
     */
    @Test
    public void T05F() throws LayerDelaunayError {

        List<PropagationPath.PointPath> points = new ArrayList<PropagationPath.PointPath>();
        List<PropagationPath.SegmentPath> segments = new ArrayList<PropagationPath.SegmentPath>();
        List<PropagationPath.SegmentPath> srPath = new ArrayList<PropagationPath.SegmentPath>();
        Vector3D flatTopography = new Vector3D(new Coordinate(0,0,0),new Coordinate(150,0,0));

        points.add(new PropagationPath.PointPath(new Coordinate(0, 0, 0.05), 0, 0, Double.NaN, PropagationPath.PointPath.POINT_TYPE.SRCE));
        points.add(new PropagationPath.PointPath(new Coordinate(50, 0, 10.0), 0, 0, Double.NaN, PropagationPath.PointPath.POINT_TYPE.DIFH));
        points.add(new PropagationPath.PointPath(new Coordinate(100, 0, 10.0), 0, 0, Double.NaN, PropagationPath.PointPath.POINT_TYPE.DIFH));
        points.add(new PropagationPath.PointPath(new Coordinate(150, 0, 2.0), 0, 0, Double.NaN, PropagationPath.PointPath.POINT_TYPE.RECV));
        // only first and last segment are necessary, even if it is possible to add more.
        segments.add(new PropagationPath.SegmentPath(0.0, flatTopography));
        segments.add(new PropagationPath.SegmentPath(0.0, flatTopography));
        srPath.add(new PropagationPath.SegmentPath(0.0,flatTopography));

        PropagationPath propagationPath = new PropagationPath(true,points,segments, srPath);
        PropagationProcessPathData propData = new PropagationProcessPathData();
        propData.setTemperature(15);
        propData.setHumidity(70);

        EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
        splCompare(evaluateAttenuationCnossos.evaluate(propagationPath, propData), "Test T05F", new double[]{-63.2, -67.0, -70.4, -73.6, -74.6, -75.3, -78.0, -88.3}, ERROR_EPSILON_TEST_T);

    }


    /**
     * Sound propagation
     * TC01
     *
     * @throws LayerDelaunayError
     */
    @Test
    public void TC01H() throws LayerDelaunayError {

        List<PropagationPath.PointPath> points = new ArrayList<PropagationPath.PointPath>();
        List<PropagationPath.SegmentPath> segments = new ArrayList<PropagationPath.SegmentPath>();
        List<PropagationPath.SegmentPath> srPath = new ArrayList<PropagationPath.SegmentPath>();

        Vector3D flatTopography = new Vector3D(new Coordinate(10,10,0),new Coordinate(200,50,0));
        points.add(new PropagationPath.PointPath(new Coordinate(10, 10, 1), 0, 0, Double.NaN, PropagationPath.PointPath.POINT_TYPE.SRCE));
        points.add(new PropagationPath.PointPath(new Coordinate(200, 50, 4), 0, 0, Double.NaN, PropagationPath.PointPath.POINT_TYPE.RECV));
        segments.add(new PropagationPath.SegmentPath(0, flatTopography));
        srPath.add(new PropagationPath.SegmentPath(0,flatTopography));

        PropagationPath propagationPath = new PropagationPath(false,points,segments, srPath);
        PropagationProcessPathData propData = new PropagationProcessPathData();
        propData.setTemperature(10);
        propData.setHumidity(70);

        splCompare(new EvaluateAttenuationCnossos().evaluate(propagationPath, propData), "Test TC01H", new double[]{39.21-93, 39.16-93, 39.03-93, 38.86-93, 38.53-93, 37.36-93, 32.87-93, 16.54-93}, ERROR_EPSILON_TEST_T);

    }

    /**
     * Sound propagation
     * TC01
     *
     * @throws LayerDelaunayError
     */
    @Test
    public void TC01F() throws LayerDelaunayError {

        List<PropagationPath.PointPath> points = new ArrayList<PropagationPath.PointPath>();
        List<PropagationPath.SegmentPath> segments = new ArrayList<PropagationPath.SegmentPath>();
        List<PropagationPath.SegmentPath> srPath = new ArrayList<PropagationPath.SegmentPath>();
        Vector3D flatTopography = new Vector3D(new Coordinate(10,10,0),new Coordinate(200,50,0));

        points.add(new PropagationPath.PointPath(new Coordinate(10, 10, 1), 0, 0, Double.NaN, PropagationPath.PointPath.POINT_TYPE.SRCE));
        points.add(new PropagationPath.PointPath(new Coordinate(200, 50, 4), 0, 0, Double.NaN, PropagationPath.PointPath.POINT_TYPE.RECV));
        segments.add(new PropagationPath.SegmentPath(0, flatTopography));
        srPath.add(new PropagationPath.SegmentPath(0,flatTopography));

        PropagationPath propagationPath = new PropagationPath(true,points,segments, srPath);
        PropagationProcessPathData propData = new PropagationProcessPathData();
        propData.setTemperature(10);
        propData.setHumidity(70);

        EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
        splCompare(evaluateAttenuationCnossos.evaluate(propagationPath, propData), "Test TC01F", new double[]{40.58-93, 40.52-93, 40.40-93, 40.23-93, 39.89-93, 38.72-93, 34.24-93, 17.90-93}, ERROR_EPSILON_TEST_T);

    }

}