/*
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact in urban areas. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 *
 * This plugin is currently developed by the Environmental Acoustics Laboratory (LAE) of Ifsttar
 * (http://wwww.lae.ifsttar.fr/) in collaboration with the Lab-STICC CNRS laboratory.
 * It was initially developed as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 *
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * <nicolas.fortin@ifsttar.fr>
 *
 * Copyright (C) 2011-2016 IFSTTAR-CNRS
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
 * For more information concerning NoiseM@p, please consult: <http://noisemap.orbisgis.org/>
 *
 * For more information concerning OrbisGis, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 *
 * info_at_ orbisgis.org
 */

package org.orbisgis.noisemap.core;

import org.h2gis.functions.spatial.properties.ST_Extent;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.orbisgis.noisemap.core.jdbc.JdbcNoiseMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.assertEquals;

public class CnossosTest {
  private static final List<Integer> freqLvl= Collections.unmodifiableList(Arrays.asList(63 ,   125 ,   250 ,   500 ,  1000 ,  2000 ,  4000 ,  8000));
  // A weighting
  private static final double[] dba = new double[] { -25.2 , -15.6 ,  -8.4 ,  -3.1 ,   0.0 ,   1.2 ,   0.9 ,  -2.4};

  private double[] splCompute(ComputeRays propManager,Coordinate receiverPosition) {
    double energeticSum[] = new double[freqLvl.size()];
    List<PropagationDebugInfo> debug = new ArrayList<>();
    //propManager.(receiverPosition, energeticSum, debug);
    return energeticSum;
  }

  private void splCompare(double[] resultW,String testName,double[] expectedLevel, double splEpsilon) {
    for(int i=0; i<resultW.length; i++) {
      double dba = ComputeRays.wToDba(resultW[i]);
      double expected = expectedLevel[i];
      assertEquals("Unit test "+testName+" failed at "+freqLvl.get(i)+" Hz",expected, dba,splEpsilon);
    }
  }

  private static ArrayList<Double> asW(double... dbValues) {
    assertEquals(dba.length, dbValues.length);
    ArrayList<Double> ret = new ArrayList<>(dbValues.length);
    for(int idFreq = 0; idFreq < freqLvl.size(); idFreq++) {
      ret.add(ComputeRays.dbaToW(dbValues[idFreq] + dba[idFreq]));
    }
    return ret;
  }

  /**
   * Sound propagation
   * One source, One receiver, no buildings, two ground area and no topography.
   * TestCnossos -i="flat ground - 10m.xml" -m="JRC-2012"
   * @throws LayerDelaunayError
   */
  @Test
  public void test_flat_ground_10m() throws LayerDelaunayError {
    double humidity = 70;
    double temperature = 15;
    final double[] WIND_ROSE = new double[]{0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5};
    final double[] LW = new double[]{80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0};
    Coordinate receiver = new Coordinate(10, 0, 2.5);
    GeometryFactory factory = new GeometryFactory();
    ////////////////////////////////////////////////////////////////////////////
    //Add road source as one point
    List<Geometry> srclst = new ArrayList<Geometry>();
    srclst.add(factory.createPoint(new Coordinate(0, 0, 0.5)));
    //Scene dimension
    Envelope cellEnvelope = new Envelope(new Coordinate(-170., -170., 0.), new Coordinate(170, 170, 0.));
    //Add source sound level
    List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
    srcSpectrum.add(asW(LW));
    //Build query structure for sources
    QueryGeometryStructure sourcesIndex = new QueryQuadTree();
    int idsrc = 0;
    for (Geometry src : srclst) {
      sourcesIndex.appendGeometry(src, idsrc);
      idsrc++;
    }
    //Create obstruction test object
    MeshBuilder mesh = new MeshBuilder();
    mesh.finishPolygonFeeding(cellEnvelope);

    // Ground types
    List<GeoWithSoilType> soil = new ArrayList<>();

    soil.add(new GeoWithSoilType(factory.toGeometry(srclst.get(0).buffer(5).getEnvelopeInternal()), 0));
    soil.add(new GeoWithSoilType(factory.toGeometry(factory.createPoint(receiver).buffer(5).getEnvelopeInternal()), 1));

    //Retrieve Delaunay triangulation of scene
    List<Coordinate> vert = mesh.getVertices();
    FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(), mesh.getTriNeighbors(), mesh.getVertices());

    PropagationProcessData propData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum, freqLvl, 0, false, 200, 200, 1., 0., WIND_ROSE,0, 0, null, soil, false);
    propData.setTemperature(temperature);
    propData.setHumidity(humidity);

    ComputeRays propManager = new ComputeRays(propData);
    propManager.initStructures();

    //Run test
    splCompare(splCompute(propManager, receiver), "flat_ground_10m", new double[]{25.8, 46.1, 58.3, 68.5, 71.6, 72.8, 67.3, 58.3}, 0.1);
  }

  /**
   * Sound propagation
   * One source, One receiver, no buildings, two ground area and no topography.
   * TestCnossos -i="flat ground - 20m.xml" -m="JRC-2012"
   * @throws LayerDelaunayError
   */
  @Test
  public void test_flat_ground_20m() throws LayerDelaunayError {
    double humidity = 70;
    double temperature = 15;
    final double[] WIND_ROSE = new double[]{0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5};
    final double[] LW = new double[]{80.0, 90.0, 95.0, 100.0, 100.0, 100.0, 95.0, 90.0};
    Coordinate receiver = new Coordinate(20, 0, 2.5);
    GeometryFactory factory = new GeometryFactory();
    ////////////////////////////////////////////////////////////////////////////
    //Add road source as one point
    List<Geometry> srclst = new ArrayList<Geometry>();
    srclst.add(factory.createPoint(new Coordinate(0, 0, 0.5)));
    //Scene dimension
    Envelope cellEnvelope = new Envelope(new Coordinate(-170., -170., 0.), new Coordinate(170, 170, 0.));
    //Add source sound level
    List<ArrayList<Double>> srcSpectrum = new ArrayList<ArrayList<Double>>();
    srcSpectrum.add(asW(LW));
    //Build query structure for sources
    QueryGeometryStructure sourcesIndex = new QueryQuadTree();
    int idsrc = 0;
    for (Geometry src : srclst) {
      sourcesIndex.appendGeometry(src, idsrc);
      idsrc++;
    }
    //Create obstruction test object
    MeshBuilder mesh = new MeshBuilder();
    mesh.finishPolygonFeeding(cellEnvelope);

    // Ground types
    List<GeoWithSoilType> soil = new ArrayList<>();

    soil.add(new GeoWithSoilType(factory.toGeometry(srclst.get(0).buffer(5).getEnvelopeInternal()), 0));
    soil.add(new GeoWithSoilType(factory.toGeometry(factory.createPoint(receiver).buffer(15).getEnvelopeInternal()), 1));

    //Retrieve Delaunay triangulation of scene
    List<Coordinate> vert = mesh.getVertices();
    FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(), mesh.getTriNeighbors(), mesh.getVertices());

    PropagationProcessData propData = new PropagationProcessData(vert, manager, sourcesIndex, srclst, srcSpectrum, freqLvl, 0, false, 200, 200, 1., 0., WIND_ROSE, 0,0, null, soil, false);
    propData.setTemperature(temperature);
    propData.setHumidity(humidity);
    ComputeRays propManager = new ComputeRays(propData);
    propManager.initStructures();

    //Run test
    splCompare(splCompute(propManager, receiver), "flat_ground_20m", new double[]{20.1 ,  39.6 ,  52.0 ,  62.3 ,  65.4 ,  66.5 ,  60.8 ,  51.1}, 0.1);
  }
}
