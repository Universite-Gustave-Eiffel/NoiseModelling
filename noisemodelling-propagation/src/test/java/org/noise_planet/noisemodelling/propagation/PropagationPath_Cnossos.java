package org.noise_planet.noisemodelling.propagation;

import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.math.Vector3D;
import org.noise_planet.noisemodelling.pathfinder.LayerDelaunayError;
import org.noise_planet.noisemodelling.pathfinder.PointPath;
import org.noise_planet.noisemodelling.pathfinder.PropagationPath;
import org.noise_planet.noisemodelling.pathfinder.SegmentPath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class PropagationPath_Cnossos {

    private static final double ERROR_EPSILON_TEST_T = 0.2;
    private static final double ERROR_EPSILON_TEST_T06 = 0.3;
    private static final double ERROR_EPSILON_TEST_TC01H = 0.3;
    private static final double ERROR_EPSILON_TEST_TC01F = 0.3;


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
            ret.add(Utils.dbToW(db_m));
        }
        return ret;
    }

    /**
     * Test for test
     */
    @Test
    public void TestPropagationPath(){
        boolean favorable = true;
        List<PointPath> points = new ArrayList<PointPath>();
        List<SegmentPath>  segments = new ArrayList<SegmentPath>();
        List<SegmentPath> srPath = new ArrayList<SegmentPath>();
        Vector3D flatTopography = new Vector3D(new Coordinate(0,0,0),new Coordinate(0,0,0));

        points.add(new PointPath(new Coordinate(0,0,0),1,new double[0], -1, PointPath.POINT_TYPE.SRCE));
        points.add(new PointPath(new Coordinate(10,0,0),Double.NaN,Collections.nCopies(8, 0.5), -1, PointPath.POINT_TYPE.DIFH));
        points.add(new PointPath(new Coordinate(20,0,0),Double.NaN,Collections.nCopies(8, 0.5), -1, PointPath.POINT_TYPE.DIFH));
        points.add(new PointPath(new Coordinate(30,30,0),0,new double[0], -1, PointPath.POINT_TYPE.RECV));
        segments.add(new SegmentPath(1, flatTopography,new Coordinate(0,0,0)));
        segments.add(new SegmentPath(1, flatTopography,new Coordinate(0,0,0)));
        srPath.add(new SegmentPath(1,flatTopography,new Coordinate(0,0,0)));

        PropagationPath propagationPath = new PropagationPath(favorable,points,segments,srPath);
        propagationPath.setGs(0);
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
        List<PointPath> points = new ArrayList<PointPath>();
        List<SegmentPath>  segments = new ArrayList<SegmentPath>();
        List<SegmentPath> srPath = new ArrayList<SegmentPath>();
        Vector3D flatTopography = new Vector3D(new Coordinate(0,0,0),new Coordinate(0,0,0));

        points.add(new PointPath(new Coordinate(0,0,1),0,new ArrayList<>(), -1, PointPath.POINT_TYPE.SRCE));
        points.add(new PointPath(new Coordinate(200,0,4),0,new ArrayList<>(), -1, PointPath.POINT_TYPE.RECV));
        segments.add(new SegmentPath(0, flatTopography,new Coordinate(0,0,0)));
        srPath.add(new SegmentPath(0,flatTopography,new Coordinate(0,0,0)));

        PropagationPath propagationPath = new PropagationPath(favorable,points,segments, srPath);
        propagationPath.setGs(0.0);
        PropagationProcessPathData propData = new PropagationProcessPathData();
        propData.setTemperature(15);
        propData.setHumidity(70);

        EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
        splCompare(evaluateAttenuationCnossos.evaluate(propagationPath, propData), "Test T01", new double[]{-54, -54.1, -54.2, -54.5, -54.8, -55.8, -59.3, -72.76}, ERROR_EPSILON_TEST_T);


    }

    /**
     * Sound propagation
     * T02H
     * Horizontal ground with homogeneous properties, road source - Absorbing ground (G=1)
     */
    @Test
    public void T02H() throws LayerDelaunayError {
        List<PointPath> points = new ArrayList<PointPath>();
        List<SegmentPath> segments = new ArrayList<SegmentPath>();
        List<SegmentPath> srPath = new ArrayList<SegmentPath>();
        Vector3D flatTopography = new Vector3D(new Coordinate(0,0,0),new Coordinate(200,0,0));

        points.add(new PointPath(new Coordinate(0, 0, 0.05), 0,  new ArrayList<>(), -1, PointPath.POINT_TYPE.SRCE));
        points.add(new PointPath(new Coordinate(200, 0, 4), 0, new ArrayList<>(), -1, PointPath.POINT_TYPE.RECV));
        srPath.add(new SegmentPath(1,flatTopography,new Coordinate(0,0,0)));

        PropagationPath propagationPath = new PropagationPath(false,points,segments, srPath);
        propagationPath.setGs(0.0);
        PropagationProcessPathData propData = new PropagationProcessPathData();
        propData.setTemperature(15);
        propData.setHumidity(70);

        EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
        splCompare(evaluateAttenuationCnossos.evaluate(propagationPath, propData), "Test T02H", new double[]{-57, -57.1, -57.9, -64.7, -70.6, -76.7, -82.0, -88.0}, ERROR_EPSILON_TEST_T);
    }

    /**
     * Sound propagation
     * T02F
     * Horizontal ground with homogeneous properties, road source - Absorbing ground (G=1)
     */
    @Test
    public void T02F() throws LayerDelaunayError {
        List<PointPath> points = new ArrayList<PointPath>();
        List<SegmentPath> segments = new ArrayList<SegmentPath>();
        List<SegmentPath> srPath = new ArrayList<SegmentPath>();
        Vector3D flatTopography = new Vector3D(new Coordinate(0,0,0),new Coordinate(200,0,0));

        points.add(new PointPath(new Coordinate(0, 0, 0.05), 0, new ArrayList<>(), -1, PointPath.POINT_TYPE.SRCE));
        points.add(new PointPath(new Coordinate(200, 0, 4), 0,  new ArrayList<>(), -1, PointPath.POINT_TYPE.RECV));
        srPath.add(new SegmentPath(1,flatTopography,new Coordinate(0,0,0)));

        PropagationPath propagationPath = new PropagationPath(true,points,segments, srPath);
        propagationPath.setGs(0.0);
        PropagationProcessPathData propData = new PropagationProcessPathData();
        propData.setTemperature(15);
        propData.setHumidity(70);

        EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
        splCompare(evaluateAttenuationCnossos.evaluate(propagationPath, propData), "Test T02F", new double[]{-57, -57.1, -57.2, -58.5, -65.8, -60.4, -62.3, -75.76}, ERROR_EPSILON_TEST_T);
    }

    /**
     * Sound propagation
     * T03F
     * Horizontal ground with homogeneous properties, road source - Absorbing ground (G=0.7)
     */
    @Test
    public void T03F() throws LayerDelaunayError {
        List<PointPath> points = new ArrayList<PointPath>();
        List<SegmentPath> segments = new ArrayList<SegmentPath>();
        List<SegmentPath> srPath = new ArrayList<SegmentPath>();
        Vector3D flatTopography = new Vector3D(new Coordinate(0,0,0),new Coordinate(200,0,0));

        points.add(new PointPath(new Coordinate(0, 0, 0.05), 0.0, new ArrayList<>(), -1, PointPath.POINT_TYPE.SRCE));
        points.add(new PointPath(new Coordinate(200, 0, 4.), 0.,  new ArrayList<>(), -1, PointPath.POINT_TYPE.RECV));
        srPath.add(new SegmentPath(0.7,flatTopography,new Coordinate(0,0,0)));

        PropagationPath propagationPath = new PropagationPath(true,points,segments, srPath);
        propagationPath.setGs(0.0);
        PropagationProcessPathData propData = new PropagationProcessPathData();
        propData.setTemperature(15);
        propData.setHumidity(70);
        propData.setPrime2520(true);

        EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
        splCompare(evaluateAttenuationCnossos.evaluate(propagationPath, propData), "Test T03F", new double[]{-56.1, -56.2, -56.3, -56.6, -61.6, -61.1, -61.4, -74.8}, ERROR_EPSILON_TEST_T);
    }

    /**
     * Sound propagation
     * T03H
     * Horizontal ground with homogeneous properties, road source - Absorbing ground (G=0.7)
     */
    @Test
    public void T03H() throws LayerDelaunayError {
        List<PointPath> points = new ArrayList<PointPath>();
        List<SegmentPath> segments = new ArrayList<SegmentPath>();
        List<SegmentPath> srPath = new ArrayList<SegmentPath>();
        Vector3D flatTopography = new Vector3D(new Coordinate(0,0,0),new Coordinate(200,0,0));

        points.add(new PointPath(new Coordinate(0, 0, 0.05), 0.,  new ArrayList<>(), -1, PointPath.POINT_TYPE.SRCE));
        points.add(new PointPath(new Coordinate(200, 0, 4.), 0.,  new ArrayList<>(), -1, PointPath.POINT_TYPE.RECV));
        srPath.add(new SegmentPath(0.7,flatTopography,new Coordinate(0,0,0)));

        PropagationPath propagationPath = new PropagationPath(false,points,segments, srPath);
        propagationPath.setGs(0.0);
        PropagationProcessPathData propData = new PropagationProcessPathData();
        propData.setTemperature(15);
        propData.setHumidity(70);
        propData.setGDisc(true);

        EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
        splCompare(evaluateAttenuationCnossos.evaluate(propagationPath, propData), "Test T03H", new double[]{-56.1, -56.2, -56.3, -60.6, -66.0, -72.6, -80.8, -88.5}, ERROR_EPSILON_TEST_T);
    }



    /**
     * Sound propagation
     * T06H
     * Horizontal ground with homogeneous properties, road source - Non compacted ground (G=0.7) with a small slope (test relief)
     *
     * @throws LayerDelaunayError
     */
    @Test
    public void T06H() throws LayerDelaunayError {
        List<PointPath> points = new ArrayList<PointPath>();
        List<SegmentPath> segments = new ArrayList<SegmentPath>();
        List<SegmentPath> srPath = new ArrayList<SegmentPath>();
        Vector3D smallSlope = new Vector3D(new Coordinate(0,0,0),new Coordinate(150,0,2));

        points.add(new PointPath(new Coordinate(0, 0, 0.05), 0,  new ArrayList<>(), -1, PointPath.POINT_TYPE.SRCE));
        points.add(new PointPath(new Coordinate(150, 0, 10.0), 0,  new ArrayList<>(), -1, PointPath.POINT_TYPE.RECV));
        // only first and last segment are necessary, even if it is possible to add more.
        srPath.add(new SegmentPath(0.7,smallSlope,new Coordinate(0,0,0)));

        PropagationPath propagationPath = new PropagationPath(false,points,segments, srPath);
        propagationPath.setGs(0.0);
        PropagationProcessPathData propData = new PropagationProcessPathData();
        propData.setTemperature(15);
        propData.setHumidity(70);
        propData.setPrime2520(true);

        EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
        splCompare(evaluateAttenuationCnossos.evaluate(propagationPath, propData), "Test T06H", new double[]{-52.9, -52.9, -53.0, -53.2, -53.5, -54.9, -64.0, -73.6}, ERROR_EPSILON_TEST_T06);
    }

    /**
     * Sound propagation
     * T06H
     * Horizontal ground with homogeneous properties, road source - Non compacted ground (G=0.7) with a small slope (test relief)
     *
     * @throws LayerDelaunayError
     */
    @Test
    public void T06F() throws LayerDelaunayError {
        List<PointPath> points = new ArrayList<PointPath>();
        List<SegmentPath> segments = new ArrayList<SegmentPath>();
        List<SegmentPath> srPath = new ArrayList<SegmentPath>();
        Vector3D smallSlope = new Vector3D(new Coordinate(0,0,0),new Coordinate(150,0,2));

        points.add(new PointPath(new Coordinate(0, 0, 0.05), 0,  new ArrayList<>(), -1, PointPath.POINT_TYPE.SRCE));
        points.add(new PointPath(new Coordinate(150, 0, 10.0), 0,  new ArrayList<>(), -1, PointPath.POINT_TYPE.RECV));
        // only first and last segment are necessary, even if it is possible to add more.
        srPath.add(new SegmentPath(0.7,smallSlope,new Coordinate(0,0,0)));

        PropagationPath propagationPath = new PropagationPath(true,points,segments, srPath);
        propagationPath.setGs(0.0);
        PropagationProcessPathData propData = new PropagationProcessPathData();
        propData.setTemperature(15);
        propData.setHumidity(70);
        propData.setPrime2520(true);

        EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
        splCompare(evaluateAttenuationCnossos.evaluate(propagationPath, propData), "Test T06F", new double[]{-52.9, -52.9, -53.0, -53.2, -53.5, -59.6, -56.8, -67.1}, ERROR_EPSILON_TEST_T);
    }

    /**
     * Sound propagation
     * T08H
     * Reflexion
     *
     * @throws LayerDelaunayError
     */
    @Test
    public void T08H() throws LayerDelaunayError {
        List<PointPath> points = new ArrayList<PointPath>();
        List<SegmentPath> segments = new ArrayList<SegmentPath>();
        List<SegmentPath> srPath = new ArrayList<SegmentPath>();
        Vector3D flatTopography = new Vector3D(new Coordinate(0,0,0),new Coordinate(150,0,0));

        points.add(new PointPath(new Coordinate(0, 0, 4), 0,  new ArrayList<>(), -1, PointPath.POINT_TYPE.SRCE));
        points.add(new PointPath(new Coordinate(75, 20, 4), 0, Collections.nCopies(8, 0.3), -1, PointPath.POINT_TYPE.REFL));
        points.add(new PointPath(new Coordinate(150, 0, 4), 0,  new ArrayList<>(), -1, PointPath.POINT_TYPE.RECV));
        // only first and last segment are necessary, even if it is possible to add more.
        srPath.add(new SegmentPath(0.,flatTopography,new Coordinate(0,0,0)));

        PropagationPath propagationPath = new PropagationPath(false,points,segments, srPath);
        propagationPath.setGs(0.0);
        PropagationProcessPathData propData = new PropagationProcessPathData();
        propData.setTemperature(15);
        propData.setHumidity(70);
        propData.setPrime2520(true);

        EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
        splCompare(evaluateAttenuationCnossos.evaluate(propagationPath, propData), "Test T08H", new double[]{-53.4, -53.4, -53.5, -53.7, -54.0, -54.7, -57.5, -68.1}, ERROR_EPSILON_TEST_T);
    }






    /**
     * Sound propagation
     * TC01
     *
     * @throws LayerDelaunayError
     */
    @Test
    public void TC01H() throws LayerDelaunayError {

        List<PointPath> points = new ArrayList<PointPath>();
        List<SegmentPath> segments = new ArrayList<SegmentPath>();
        List<SegmentPath> srPath = new ArrayList<SegmentPath>();

        Vector3D flatTopography = new Vector3D(new Coordinate(10,10,0),new Coordinate(200,50,0));
        points.add(new PointPath(new Coordinate(10, 10, 1), 0,  new ArrayList<>(), -1, PointPath.POINT_TYPE.SRCE));
        points.add(new PointPath(new Coordinate(200, 50, 4), 0,  new ArrayList<>(), -1, PointPath.POINT_TYPE.RECV));
        segments.add(new SegmentPath(0, flatTopography,new Coordinate(0,0,0)));
        srPath.add(new SegmentPath(0,flatTopography,new Coordinate(0,0,0)));

        PropagationPath propagationPath = new PropagationPath(false,points,segments, srPath);
        propagationPath.setGs(0.0);
        PropagationProcessPathData propData = new PropagationProcessPathData();
        propData.setTemperature(10);
        propData.setHumidity(70);

        splCompare(new EvaluateAttenuationCnossos().evaluate(propagationPath, propData), "Test TC01H", new double[]{39.21-93, 39.16-93, 39.03-93, 38.86-93, 38.53-93, 37.36-93, 32.87-93, 16.54-93}, ERROR_EPSILON_TEST_TC01H);

    }

    /**
     * Sound propagation
     * TC01
     *
     * @throws LayerDelaunayError
     */
    @Test
    public void TC01F() throws LayerDelaunayError {

        List<PointPath> points = new ArrayList<PointPath>();
        List<SegmentPath> segments = new ArrayList<SegmentPath>();
        List<SegmentPath> srPath = new ArrayList<SegmentPath>();
        Vector3D flatTopography = new Vector3D(new Coordinate(10,10,0),new Coordinate(200,50,0));

        points.add(new PointPath(new Coordinate(10, 10, 1), 0,  new ArrayList<>(), -1, PointPath.POINT_TYPE.SRCE));
        points.add(new PointPath(new Coordinate(200, 50, 4), 0,  new ArrayList<>(), -1, PointPath.POINT_TYPE.RECV));
        segments.add(new SegmentPath(0, flatTopography,new Coordinate(0,0,0)));
        srPath.add(new SegmentPath(0,flatTopography,new Coordinate(0,0,0)));

        PropagationPath propagationPath = new PropagationPath(true,points,segments, srPath);
        propagationPath.setGs(0.0);
        PropagationProcessPathData propData = new PropagationProcessPathData();
        propData.setTemperature(10);
        propData.setHumidity(70);

        EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
        splCompare(evaluateAttenuationCnossos.evaluate(propagationPath, propData), "Test TC01F", new double[]{40.58-93, 40.52-93, 40.40-93, 40.23-93, 39.89-93, 38.72-93, 34.24-93, 17.90-93}, ERROR_EPSILON_TEST_TC01F);

    }



    /**
     * Sound propagation
     * T14
     * Site with homogeneous ground properties and a large and tall building
     *
     * @throws LayerDelaunayError
     */
    @Test
    public void T14H() throws LayerDelaunayError {

        List<PointPath> points = new ArrayList<PointPath>();
        List<SegmentPath> segments = new ArrayList<SegmentPath>();
        List<SegmentPath> srPath = new ArrayList<SegmentPath>();

        points.add(new PointPath(new Coordinate(10, 10, 4.0), 0,  new ArrayList<>(), -1, PointPath.POINT_TYPE.SRCE));
        points.add(new PointPath(new Coordinate(175, 50, 4.0), 0,  new ArrayList<>(), -1, PointPath.POINT_TYPE.DIFV));
        points.add(new PointPath(new Coordinate(200, 10, 4.0), 0,  new ArrayList<>(), -1, PointPath.POINT_TYPE.RECV));
        // only first and last segment are necessary, even if it is possible to add more.
        segments.add(new SegmentPath(0.0, new Vector3D(new Coordinate(10,10,0),new Coordinate(175,50,0)),new Coordinate(0,0,0)));
        segments.add(new SegmentPath(0.0, new Vector3D(new Coordinate(175,50,0),new Coordinate(200,10,0)),new Coordinate(0,0,0)));
        srPath.add(new SegmentPath(0.0,new Vector3D(new Coordinate(10,10,0),new Coordinate(200,10,0)),new Coordinate(0,0,0)));

        PropagationPath propagationPath = new PropagationPath(false,points,segments, srPath);
        PropagationProcessPathData propData = new PropagationProcessPathData();
        propData.setTemperature(15);
        propData.setHumidity(70);

        EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
        splCompare(evaluateAttenuationCnossos.evaluate(propagationPath, propData), "Test T14H", new double[]{-76.7, -79.7, -82.8, -86.1, -89.5, -93.5, -100.4, -117.9}, ERROR_EPSILON_TEST_T);

    }

}