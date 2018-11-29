package org.orbisgis.noisemap.core;

import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class PropagationPathTest {
    private static final List<Integer> freqLvl = Collections.unmodifiableList(Arrays.asList(63, 125, 250, 500, 1000, 2000,
            4000, 8000));
    @Test
    public void TestPropagationPath(){
        boolean favorable = true;
        List<PropagationPath.PointPath> points = new ArrayList<PropagationPath.PointPath>();
        List<PropagationPath.SegmentPath>  segments = new ArrayList<PropagationPath.SegmentPath>();

        points.add(new PropagationPath.PointPath(new Coordinate(0,0,0),1,0,Double.NaN,true));
        points.add(new PropagationPath.PointPath(new Coordinate(10,0,0),1,Double.NaN,0.5,true));
        points.add(new PropagationPath.PointPath(new Coordinate(20,0,0),1,Double.NaN,0.5,true));
        points.add(new PropagationPath.PointPath(new Coordinate(30,30,0),1,0,Double.NaN,true));
        segments.add(new PropagationPath.SegmentPath(1,0));
        segments.add(new PropagationPath.SegmentPath(1,0));


        PropagationPath propagationPath = new PropagationPath(favorable,points,segments);
        PropagationProcessPathData propData = new PropagationProcessPathData(freqLvl);
        propData.setTemperature(15);
        propData.setHumidity(70);



        EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
        evaluateAttenuationCnossos.evaluate(propagationPath, propData);





    }



}