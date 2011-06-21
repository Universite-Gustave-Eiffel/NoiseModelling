package org.noisemap.core;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.gdms.data.values.Value;
import junit.framework.TestCase;

public class TestSoundPropagationValidation extends TestCase {
	public static double splEpsilon=0.05;
	private double splCompute(PropagationProcess propManager,Coordinate receiverPosition) {
		double energeticSum[]={0.};
		propManager.computeSoundLevelAtPosition(receiverPosition, energeticSum);
		return PropagationProcess.wToDba(energeticSum[0]);		
	}
	private void splCompare(double dba,String testName,double goodSpl) {

		System.out.println(testName+" "+dba+" dB(A)");
		assertTrue(goodSpl+"!="+dba+" (right ref)Sound level computation error @ "+testName,Math.abs(dba-goodSpl)<splEpsilon);		
	}
	public void testScene1() throws LayerDelaunayError {
		////////////////////////////////////////////////////////////////////////////
		//Build Scene with One Building
		GeometryFactory factory = new GeometryFactory();
		Coordinate[] building1Coords = { new Coordinate(15., 5.,0.),
				new Coordinate(30., 5.,0.), new Coordinate(30., 30.,0.),
				new Coordinate(15., 30.,0.), new Coordinate(15., 5.,0.) };
		Polygon building1 = factory.createPolygon(
				factory.createLinearRing(building1Coords), null);
		////////////////////////////////////////////////////////////////////////////
		//Add road source as one point
		List<Geometry> srclst=new ArrayList<Geometry>(); 
		srclst.add(factory.createPoint(new Coordinate(40,15,0)));
		//Scene dimension
		Envelope cellEnvelope=new Envelope(new Coordinate(0., 0.,0.),new Coordinate(45., 45.,0.));
		//Add source sound level
		List<ArrayList<Double>> srcSpectrum=new ArrayList<ArrayList<Double>>();
		srcSpectrum.add(new ArrayList<Double>());
		srcSpectrum.get(0).add(PropagationProcess.dbaToW(100.)); // 100 dB(A) @ 125 Hz
		List<Integer> freqLvl=new ArrayList<Integer>();
		freqLvl.add(125);
		//Build query structure for sources
		QueryGeometryStructure<Integer> sourcesIndex = new QueryGridIndex<Integer>(
				cellEnvelope, 16, 16);
		int idsrc=0;
		for(Geometry src : srclst) {
			sourcesIndex.appendGeometry(src, idsrc);
			idsrc++;
		}
		
		//Create obstruction test object
		FastObstructionTest manager = new FastObstructionTest();
		manager.addGeometry(building1);
		manager.finishPolygonFeeding(cellEnvelope);	
		//Retrieve Delaunay triangulation of scene
		List<Triangle> tri=manager.getTriangles();
		List<Coordinate> vert=manager.getVertices();

		Stack<ArrayList<Value>> dataStack=new Stack<ArrayList<Value>>();		
		PropagationProcessData propData=new PropagationProcessData(vert, tri, manager, sourcesIndex, srclst, srcSpectrum, freqLvl, 0, 2, 80., 1., 0., 0l, null, null);
		PropagationProcessOut propDataOut=new PropagationProcessOut(dataStack);
		PropagationProcess propManager=new PropagationProcess(propData, propDataOut);
		propManager.initStructures();
		
		//Run test
		splCompare(splCompute(propManager, new Coordinate(5,15,0)), "Scene 1 R1_S1", 34.97106);
		//Get reference spl value at 5m
		propData.diffractionOrder=0;
		double dbaRef=splCompute(propManager, new Coordinate(40,20,0));
		//spl value at 10m
		double dbaReduced=splCompute(propManager, new Coordinate(40,25,0));
		splCompare(dbaReduced, "Scene 1 R2_S1", dbaRef-6.); //Double distance, then loss 6 dB. Atmospheric attenuation is not significant at 125Hz and  5 m distance
	}
}
