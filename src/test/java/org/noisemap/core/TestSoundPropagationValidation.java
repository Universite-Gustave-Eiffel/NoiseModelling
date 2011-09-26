package org.noisemap.core;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
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
	public static boolean isSameDbValues(double dba,double dba2) {
		return Math.abs(dba-dba2)<splEpsilon || (Double.isInfinite(dba) && Double.isInfinite(dba2));
	}
	private void splCompare(double dba,String testName,double goodSpl) {

		System.out.println(testName+" "+dba+" dB(A)");
		assertTrue(goodSpl+"!="+dba+" (right ref)Sound level computation error @ "+testName,isSameDbValues(dba,goodSpl));		
	}
	public void testScene1() throws LayerDelaunayError {
		System.out.println("________________________________________________");
		System.out.println("Scene 1 :");
		long startMakeScene=System.currentTimeMillis();
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
		Envelope cellEnvelope=new Envelope(new Coordinate(-170., -170.,0.),new Coordinate(170, 170,0.));
		//Add source sound level
		List<ArrayList<Double>> srcSpectrum=new ArrayList<ArrayList<Double>>();
		srcSpectrum.add(new ArrayList<Double>());
		srcSpectrum.get(0).add(PropagationProcess.dbaToW(100.)); // 100 dB(A) @ 125 Hz
		List<Integer> freqLvl=new ArrayList<Integer>();
		freqLvl.add(125);
		//Build query structure for sources
		QueryGeometryStructure<Integer> sourcesIndex = new QueryGridIndex<Integer>(
				cellEnvelope, 8, 8);
		int idsrc=0;
		for(Geometry src : srclst) {
			sourcesIndex.appendGeometry(src, idsrc);
			idsrc++;
		}
		System.out.println("Construction scene in "+(System.currentTimeMillis()-startMakeScene)+"ms");
		long startObstructionTest=System.currentTimeMillis();
		//Create obstruction test object
		FastObstructionTest manager = new FastObstructionTest();
		manager.addGeometry(building1);
		manager.finishPolygonFeeding(cellEnvelope);	
		
		//Retrieve Delaunay triangulation of scene
		List<Triangle> tri=manager.getTriangles();
		List<Coordinate> vert=manager.getVertices();

		Stack<PropagationResultRecord> dataStack=new Stack<PropagationResultRecord>();
		PropagationProcessData propData=new PropagationProcessData(vert, tri, manager, sourcesIndex, srclst, srcSpectrum, freqLvl, 0, 2, 80.,50., 1., 0., 0l, null, null);
		PropagationProcessOut propDataOut=new PropagationProcessOut(dataStack);
		PropagationProcess propManager=new PropagationProcess(propData, propDataOut);
		propManager.initStructures();

		System.out.println("Propagation initialisation in "+(System.currentTimeMillis()-startObstructionTest)+"ms");
		long startSimulation=System.currentTimeMillis();
		//Run test
		/////////////////////////////////////////////////////////////////////////
		// 					   Single diffraction test
		propData.diffractionOrder=1;
		propData.reflexionOrder=0;
		splCompare(splCompute(propManager, new Coordinate(15,40,0)), "Scene 1 R4_S1", 46.81);
		/////////////////////////////////////////////////////////////////////////
		// 					   Dual diffraction test
		propData.diffractionOrder=2;
		propData.reflexionOrder=0;
		splCompare(splCompute(propManager, new Coordinate(5,15,0)), "Scene 1 R1_S1", 34.97);
		/////////////////////////////////////////////////////////////////////////
		// 					   Geometric dispersion test
		//Get reference spl value at 5m
		propData.reflexionOrder=0;
		propData.diffractionOrder=0;
		double dbaRef=splCompute(propManager, new Coordinate(40,20,0));
		//spl value at 10m
		double dbaReduced=splCompute(propManager, new Coordinate(40,25,0));
		splCompare(dbaReduced, "Scene 1 R2_S1", dbaRef-6.); //Double distance, then loss 6 dB. Atmospheric attenuation is not significant at 125Hz and  5 m distance
		/////////////////////////////////////////////////////////////////////////
		// 					   First reflection test
		dbaRef=splCompute(propManager, new Coordinate(35,15,0));           //Ref, at 5m of src, at 5m of wall
		double dbaRef2=splCompute(propManager, new Coordinate(40,15+15,0));//Ref2, at 15m of src (Src->Receiver->Wall->Receiver : 5+5+5)
		propData.reflexionOrder=1;
		propData.wallAlpha=0.2;
		double dbaReflection=splCompute(propManager, new Coordinate(35,15,0)); //dbaReflection must be equal to the energetic sum of Ref&Ref2, with Ref2 attenuated by wall alpha.
		splCompare(dbaReflection, "Scene 1 R3_S1",PropagationProcess.wToDba( PropagationProcess.dbaToW(dbaRef)+PropagationProcess.dbaToW(dbaRef2)*(1-propData.wallAlpha)));
		/////////////////////////////////////////////////////////////////////////
		// 					   Energetic sum of source test
		// The same source duplicated at the same position must be equal to a single point source with an energetic sum of SPL.
		// Get reference spl value
		propData.reflexionOrder=0;
		propData.diffractionOrder=0;
		srcSpectrum.get(0).set(0,PropagationProcess.dbaToW(100.)+PropagationProcess.dbaToW(100.));
		double dbaSingleSource=splCompute(propManager, new Coordinate(40,20,0));
		//spl value
		srcSpectrum.add(new ArrayList<Double>());
		srcSpectrum.get(0).set(0,PropagationProcess.dbaToW(100.));
		srcSpectrum.get(1).add(PropagationProcess.dbaToW(100.)); // 100 dB(A) @ 125 Hz
		srclst.add(factory.createPoint(new Coordinate(40,15,0)));
		sourcesIndex.appendGeometry(srclst.get(1), idsrc);
		idsrc++;
		double dbaDupp=splCompute(propManager, new Coordinate(40,20,0));
		splCompare(dbaSingleSource, "Scene 1 R3_S2",dbaDupp);

		
		System.out.println("Simulation done in "+(System.currentTimeMillis()-startSimulation)+"ms");
		System.out.println(manager.getNbObstructionTest()+" obstruction test has been done..");
		System.out.println("testScene1 done in "+(System.currentTimeMillis()-startMakeScene)+"ms");
	}
	/**
	 * Build a scene with two line source at the same position
	 * @throws LayerDelaunayError
	 */
	public void testMergeSources() throws LayerDelaunayError {
		System.out.println("________________________________________________");
		System.out.println("Scene 2 :");
		long startMakeScene=System.currentTimeMillis();
		////////////////////////////////////////////////////////////////////////////
		//Build Scene with One Building
		GeometryFactory factory = new GeometryFactory();
		Coordinate[] building1Coords = { new Coordinate(6., 2.,0.),new Coordinate(18., 2.,0.),new Coordinate(18., 6.,0.),new Coordinate(6., 6.,0.),new Coordinate(6., 2.,0.)};
		Polygon building1 = factory.createPolygon(
				factory.createLinearRing(building1Coords), null);
		Coordinate[] building2Coords = { new Coordinate(24., 2.,0.),new Coordinate(28., 2.,0.),new Coordinate(28., 6.,0.),new Coordinate(24., 6.,0.),new Coordinate(24., 2.,0.)};
		Polygon building2 = factory.createPolygon(
				factory.createLinearRing(building2Coords), null);
		Coordinate[] building3Coords = { new Coordinate(6., 10.,0.),new Coordinate(24., 10.,0.),new Coordinate(24.,18.,0.),new Coordinate(6., 18.,0.),new Coordinate(6., 10.,0.)};
		Polygon building3 = factory.createPolygon(
				factory.createLinearRing(building3Coords), null);

		////////////////////////////////////////////////////////////////////////////
		//Add road source as one point
		List<Geometry> srclst=new ArrayList<Geometry>();
		Coordinate[] way1={new Coordinate(2,8,0),new Coordinate(24,8,0),new Coordinate(30,14,0)};
		LineString road1=factory.createLineString(way1);
		srclst.add(road1);
		srclst.add(road1);
		//Scene dimension
		Envelope cellEnvelope=new Envelope(new Coordinate(-500., -500.,0.),new Coordinate(500, 500,0.));
		//Add source sound level
		List<ArrayList<Double>> srcSpectrum=new ArrayList<ArrayList<Double>>();
		srcSpectrum.add(new ArrayList<Double>());
		srcSpectrum.add(new ArrayList<Double>());
		srcSpectrum.get(0).add(PropagationProcess.dbaToW(100.)); // 100 dB(A) @ 125 Hz
		srcSpectrum.get(1).add(PropagationProcess.dbaToW(100.)); // 100 dB(A) @ 125 Hz
		List<Integer> freqLvl=new ArrayList<Integer>();
		freqLvl.add(125);
		//Build query structure for sources
		QueryGeometryStructure<Integer> sourcesIndex = new QueryGridIndex<Integer>(
				cellEnvelope, 8, 8);
		int idsrc=0;
		for(Geometry src : srclst) {
			sourcesIndex.appendGeometry(src, idsrc);
			idsrc++;
		}
		System.out.println("Construction scene in "+(System.currentTimeMillis()-startMakeScene)+"ms");
		long startObstructionTest=System.currentTimeMillis();
		//Create obstruction test object
		FastObstructionTest manager = new FastObstructionTest();
		manager.addGeometry(building1);
		manager.addGeometry(building2);
		manager.addGeometry(building3);
		manager.finishPolygonFeeding(cellEnvelope);	
		
		//Retrieve Delaunay triangulation of scene
		List<Triangle> tri=manager.getTriangles();
		List<Coordinate> vert=manager.getVertices();

		Stack<PropagationResultRecord> dataStack=new Stack<PropagationResultRecord>();
		PropagationProcessData propData=new PropagationProcessData(vert, tri, manager, sourcesIndex, srclst, srcSpectrum, freqLvl, 0, 2, 80.,50., 1., 0., 0l, null, null);
		PropagationProcessOut propDataOut=new PropagationProcessOut(dataStack);
		PropagationProcess propManager=new PropagationProcess(propData, propDataOut);
		propManager.initStructures();

		System.out.println("Propagation initialisation in "+(System.currentTimeMillis()-startObstructionTest)+"ms");
		long startSimulation=System.currentTimeMillis();
		//Run test
		//System.out.println(manager.getDelaunayGeoms());
		/////////////////////////////////////////////////////////////////////////
		// 					   Geometric dispersion test
		propData.reflexionOrder=3;
		propData.diffractionOrder=0;
		double dbaRef=splCompute(propManager, new Coordinate(20,4,0));
		System.out.println("Simulation done in "+(System.currentTimeMillis()-startSimulation)+"ms");
		System.out.println(manager.getNbObstructionTest()+" obstruction test has been done..");
		System.out.println(propDataOut.getNb_couple_receiver_src()+" point source created..");
		System.out.println(propDataOut.getNb_image_receiver()+" receiver image found..");
		System.out.println(propDataOut.getNb_reflexion_path()+" reflection path found..");
		System.out.println(propDataOut.getTotalReflexionTime()+" ms reflexion time");
		splCompare(dbaRef, "Scene 2 (20,4)",91.916);
		System.out.println("testScene1 done in "+(System.currentTimeMillis()-startMakeScene)+"ms");
	}
}
