/**
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact on urban mobility plans. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 *
 * This version is developed at French IRSTV Institute and at IFSTTAR
 * (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 *
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
 *
 * Copyright (C) 2011 IFSTTAR
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
 *
 * Noisemap is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Noisemap. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.orbisgis.noisemap.core;

import junit.framework.TestCase;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/***
 * Sound propagation evaluation using NMPB validation scenarios Doesn't work !!
 * @author Pierre Aumond, UMRAE
 */
public class TestISO17534 extends TestCase {
  private static final List<Integer> freqLvl= Collections.unmodifiableList(Arrays.asList(63, 125, 250, 500,1000, 2000,
          4000, 8000));
  private static final double ERROR_EPSILON_TEST_T = 0.2;

  private double[] splCompute(PropagationProcess propManager,Coordinate receiverPosition) {
    double energeticSum[] = new double[freqLvl.size()];
    List<PropagationDebugInfo> debug = new ArrayList<>();
    propManager.computeSoundLevelAtPosition(receiverPosition, energeticSum, debug);
    return energeticSum;
  }

  private void splCompare(double[] resultW,String testName,double[] expectedLevel, double splEpsilon) {
    for(int i=0; i<resultW.length; i++) {
      double dba = PropagationProcess.wToDba(resultW[i]);
      double expected = expectedLevel[i];
      assertEquals("Unit test "+testName+" failed at "+freqLvl.get(i)+" Hz",expected, dba,splEpsilon);
    }
  }

  private static ArrayList<Double> asW(double... dbValues) {
    ArrayList<Double> ret = new ArrayList<>(dbValues.length);
    for(double db_m : dbValues) {
      ret.add(PropagationProcess.dbaToW(db_m));
    }
    return ret;
  }

  /**
   * Sound propagation
   * T01
   * Horizontal ground with homogeneous properties, close receiver - Reflective ground (G=0)
   * @throws LayerDelaunayError
   */
  public void testT01() throws LayerDelaunayError {
    GeometryFactory factory = new GeometryFactory();
    ////////////////////////////////////////////////////////////////////////////
    //Add road source as one point
    List<Geometry> srclst=new ArrayList<Geometry>();
    srclst.add(factory.createPoint(new Coordinate(0, 0, 1)));
    //Scene dimension
    Envelope cellEnvelope=new Envelope(new Coordinate(-250., -250.,0.),new Coordinate(250, 250,0.));
    //Add source sound level
    List<ArrayList<Double>> srcSpectrum=new ArrayList<ArrayList<Double>>();
    srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0 ,90.0 ));
    // GeometrySoilType
    List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
    geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-50,250,-50,50)),0.));
    //Build query structure for sources
    QueryGeometryStructure sourcesIndex = new QueryQuadTree();
    int idsrc=0;
    for(Geometry src : srclst) {
      sourcesIndex.appendGeometry(src, idsrc);
      idsrc++;
    }
    //Create obstruction test object
    MeshBuilder mesh = new MeshBuilder();
    mesh.finishPolygonFeeding(cellEnvelope);

    //Retrieve Delaunay triangulation of scene
    List<Coordinate> vert=mesh.getVertices();
    FastObstructionTest manager=new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
            mesh.getTriNeighbors(), mesh.getVertices());
    // rose of favourable conditions
    double[] favrose = new double[]{0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25};

    PropagationProcessData propData=new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
            freqLvl, 0, 0, 250,250, 1., 0.,favrose, 0, null,geoWithSoilTypeList, true);
    propData.setTemperature(15);
    propData.setHumidity(70);
    PropagationProcessOut propDataOut=new PropagationProcessOut();
    PropagationProcess propManager=new PropagationProcess(propData, propDataOut);
    propManager.initStructures();

    //Run test
    splCompare(splCompute(propManager, new Coordinate(200, 0, 4)), "Test T01", new double[]{26.0,35.9,40.8,45.5,45.2,44.2,35.7,17.0}, ERROR_EPSILON_TEST_T);
  }


  /**
   * Sound propagation
   * T02
   * Horizontal ground with homogeneous properties, road source - Absorbing ground (G=1)
   * @throws LayerDelaunayError
   */
  public void testT02() throws LayerDelaunayError {
    GeometryFactory factory = new GeometryFactory();
    ////////////////////////////////////////////////////////////////////////////
    //Add road source as one point
    List<Geometry> srclst=new ArrayList<Geometry>();
    srclst.add(factory.createPoint(new Coordinate(0, 0, 0.05)));
    //Scene dimension
    Envelope cellEnvelope=new Envelope(new Coordinate(-250., -250.,0.),new Coordinate(250, 250,0.));
    //Add source sound level
    List<ArrayList<Double>> srcSpectrum=new ArrayList<ArrayList<Double>>();
    srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0 ,90.0));
    // GeometrySoilType
    List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
    geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-50,250,-50,50)),1.));
    //Build query structure for sources
    QueryGeometryStructure sourcesIndex = new QueryQuadTree();
    int idsrc=0;
    for(Geometry src : srclst) {
      sourcesIndex.appendGeometry(src, idsrc);
      idsrc++;
    }
    //Create obstruction test object
    MeshBuilder mesh = new MeshBuilder();
    mesh.finishPolygonFeeding(cellEnvelope);

    //Retrieve Delaunay triangulation of scene
    List<Coordinate> vert=mesh.getVertices();
    FastObstructionTest manager=new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
            mesh.getTriNeighbors(), mesh.getVertices());
    // rose of favourable conditions
    double[] favrose = new double[]{0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5};

    PropagationProcessData propData=new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
            freqLvl, 0, 0, 250,250, 1., 0., favrose,0, null,geoWithSoilTypeList, true);
    PropagationProcessOut propDataOut=new PropagationProcessOut();
    PropagationProcess propManager=new PropagationProcess(propData, propDataOut);
    propManager.initStructures();

    //Run test
    splCompare(splCompute(propManager, new Coordinate(200, 0, 4)), "Test T02", new double[]{23.0,32.9,37.4,39.4,32.5,36.7,29.7,11.2}, ERROR_EPSILON_TEST_T);
  }

  /**
   * Sound propagation
   * T03
   * Horizontal ground with homogeneous properties, road source - Non compacted ground (G=0.7)
   * @throws LayerDelaunayError
   */
  public void testT03() throws LayerDelaunayError {
    GeometryFactory factory = new GeometryFactory();
    ////////////////////////////////////////////////////////////////////////////
    //Add road source as one point
    List<Geometry> srclst=new ArrayList<Geometry>();
    srclst.add(factory.createPoint(new Coordinate(0, 0, 0.05)));
    //Scene dimension
    Envelope cellEnvelope=new Envelope(new Coordinate(-250., -250.,0.),new Coordinate(250, 250,0.));
    //Add source sound level
    List<ArrayList<Double>> srcSpectrum=new ArrayList<ArrayList<Double>>();
    srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0 ,90.0));
    // GeometrySoilType
    List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
    geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-50,250,-50,50)),0.7));
    //Build query structure for sources
    QueryGeometryStructure sourcesIndex = new QueryQuadTree();
    int idsrc=0;
    for(Geometry src : srclst) {
      sourcesIndex.appendGeometry(src, idsrc);
      idsrc++;
    }
    //Create obstruction test object
    MeshBuilder mesh = new MeshBuilder();
    mesh.finishPolygonFeeding(cellEnvelope);

    //Retrieve Delaunay triangulation of scene
    List<Coordinate> vert=mesh.getVertices();
    FastObstructionTest manager=new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
            mesh.getTriNeighbors(), mesh.getVertices());
    // rose of favourable conditions
    double[] favrose = new double[]{0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5};

    PropagationProcessData propData=new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
            freqLvl, 0, 0, 250,250, 1., 0., favrose,0, null,geoWithSoilTypeList, true);
    PropagationProcessOut propDataOut=new PropagationProcessOut();
    PropagationProcess propManager=new PropagationProcess(propData, propDataOut);
    propManager.initStructures();

    //Run test
    splCompare(splCompute(propManager, new Coordinate(7.5, 0, 4)), "Test T03", new double[]{53.3,63.3,68.3,73.3,73.3,73.3,68.1,62.5}, ERROR_EPSILON_TEST_T);
  }

  /**
   * Sound propagation
   * T04H
   * Horizontal ground with spatially varying acoustic properties Homogeneous Meteo
   * @throws LayerDelaunayError
   */
  public void testT04H() throws LayerDelaunayError {
    GeometryFactory factory = new GeometryFactory();
    ////////////////////////////////////////////////////////////////////////////
    //Add road source as one point
    List<Geometry> srclst=new ArrayList<Geometry>();
    srclst.add(factory.createPoint(new Coordinate(0, 0, 0.05)));
    //Scene dimension
    Envelope cellEnvelope=new Envelope(new Coordinate(-250., -250.,0.),new Coordinate(250, 250,0.));
    //Add source sound level
    List<ArrayList<Double>> srcSpectrum=new ArrayList<ArrayList<Double>>();
    srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0 ,90.0));
    // GeometrySoilType
    List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
    geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(-20,20,-20,40)),0.));
    geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(20,40,-20,40)),1.));
    geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(40,60,-20,40)),0.3));
    //Build query structure for sources
    QueryGeometryStructure sourcesIndex = new QueryQuadTree();
    int idsrc=0;
    for(Geometry src : srclst) {
      sourcesIndex.appendGeometry(src, idsrc);
      idsrc++;
    }
    //Create obstruction test object
    MeshBuilder mesh = new MeshBuilder();
    mesh.finishPolygonFeeding(cellEnvelope);

    //Retrieve Delaunay triangulation of scene
    List<Coordinate> vert=mesh.getVertices();
    FastObstructionTest manager=new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
            mesh.getTriNeighbors(), mesh.getVertices());
    // rose of favourable conditions
    double[] favrose = new double[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};

    PropagationProcessData propData=new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
            freqLvl, 0, 0, 250,250, 1., 0., favrose,0, null,geoWithSoilTypeList, true);
    propData.setTemperature(15);
    propData.setHumidity(70);
    PropagationProcessOut propDataOut=new PropagationProcessOut();
    PropagationProcess propManager=new PropagationProcess(propData, propDataOut);
    propManager.initStructures();

    //Run test
    splCompare(splCompute(propManager, new Coordinate(50, 0, 2)), "Test T04H", new double[]{36.9 ,  46.9 ,  51.8 ,  56.8 ,  56.7 ,  56.3 ,  42.8 ,  32.6}, 0.1);
  }
  /**
   * Sound propagation
   * T04F
   * Horizontal ground with spatially varying acoustic properties Favourable Meteo
   * @throws LayerDelaunayError
   */
  public void testT04F() throws LayerDelaunayError {
    GeometryFactory factory = new GeometryFactory();
    ////////////////////////////////////////////////////////////////////////////
    //Add road source as one point
    List<Geometry> srclst=new ArrayList<Geometry>();
    srclst.add(factory.createPoint(new Coordinate(0, 0, 0.05)));
    //Scene dimension
    Envelope cellEnvelope=new Envelope(new Coordinate(-250., -250.,0.),new Coordinate(250, 250,0.));
    //Add source sound level
    List<ArrayList<Double>> srcSpectrum=new ArrayList<ArrayList<Double>>();
    srcSpectrum.add(asW(80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0 ,90.0));
    // GeometrySoilType
    List<GeoWithSoilType> geoWithSoilTypeList = new ArrayList<>();
    geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(0,20,-20,40)),0.));
    geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(20,40,-20,40)),1.));
    geoWithSoilTypeList.add(new GeoWithSoilType(factory.toGeometry(new Envelope(40,60,-20,40)),0.3));
    //Build query structure for sources
    QueryGeometryStructure sourcesIndex = new QueryQuadTree();
    int idsrc=0;
    for(Geometry src : srclst) {
      sourcesIndex.appendGeometry(src, idsrc);
      idsrc++;
    }
    //Create obstruction test object
    MeshBuilder mesh = new MeshBuilder();
    mesh.finishPolygonFeeding(cellEnvelope);

    //Retrieve Delaunay triangulation of scene
    List<Coordinate> vert=mesh.getVertices();
    FastObstructionTest manager=new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
            mesh.getTriNeighbors(), mesh.getVertices());
    // rose of favourable conditions
    double[] favrose = new double[]{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1};

    PropagationProcessData propData=new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum,
            freqLvl, 0, 0, 250,250, 1., 0., favrose,0, null,geoWithSoilTypeList, true);
    propData.setTemperature(15);
    propData.setHumidity(70);
    PropagationProcessOut propDataOut=new PropagationProcessOut();
    PropagationProcess propManager=new PropagationProcess(propData, propDataOut);
    propManager.initStructures();

    //Run test
    splCompare(splCompute(propManager, new Coordinate(50, 0, 2)), "Test T04F", new double[]{36.9, 46.9, 51.8, 56.8, 56.7, 53.3, 48.8, 42.1}, ERROR_EPSILON_TEST_T);
  }

}
