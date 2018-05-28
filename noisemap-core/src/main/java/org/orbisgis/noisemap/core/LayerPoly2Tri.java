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

import org.locationtech.jts.algorithm.CGAlgorithms;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateFilter;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFilter;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.strtree.*;
import org.poly2tri.Poly2Tri;
import org.poly2tri.geometry.polygon.PolygonPoint;
import org.poly2tri.triangulation.Triangulatable;
import org.poly2tri.triangulation.TriangulationAlgorithm;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;
import org.poly2tri.triangulation.point.TPoint;
import org.poly2tri.triangulation.sets.ConstrainedPointSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class LayerPoly2Tri implements LayerDelaunay {
  // Precision
  private MathContext mathContext = MathContext.DECIMAL64;
  private static final Logger LOGGER = LoggerFactory.getLogger(LayerPoly2Tri.class);

  private double r(double v) {
    return new BigDecimal(v).round(mathContext).doubleValue();
  }

  private Map<TriangulationPoint, Integer> pts = new HashMap<TriangulationPoint, Integer>();
  private List<Integer> segments = new ArrayList<Integer>(pts.size());
  private AtomicInteger pointsCount = new AtomicInteger(0);
  private PointHandler pointHandler = new PointHandler(this, pts, pointsCount);
  private LineStringHandler lineStringHandler = new LineStringHandler(this, pts, pointsCount, segments);
  private List<Coordinate> vertices = new ArrayList<Coordinate>();
  private HashMap<Integer, BuildingWithID> buildingWithID = new HashMap<Integer, BuildingWithID>();

  private boolean computeNeighbors = false;
  private List<Triangle> triangles = new ArrayList<Triangle>();
  private List<Triangle> neighbors = new ArrayList<Triangle>(); // The first neighbor triangle is opposite the first corner of triangle  i
  private HashMap<Integer, LinkedList<Integer>> hashOfArrayIndex = new HashMap<Integer, LinkedList<Integer>>();
  private double maxArea = 0;

  private static GeometryFactory FACTORY = new GeometryFactory();

  private static Coordinate TPointToCoordinate(TriangulationPoint tPoint) {
    return new Coordinate(tPoint.getX(), tPoint.getY(), tPoint.getZ());
  }

  private static TPoint CoordinateToTPoint(Coordinate coordinate) {
    return new PointWithAttribute(coordinate.x, coordinate.y, coordinate.z, -1);
  }

  private static TPoint CoordinateToTPoint(Coordinate coordinate, int attribute) {
    return new PointWithAttribute(coordinate.x, coordinate.y, coordinate.z, attribute);
  }

  private static class BuildingWithID {
    private Polygon building;


    public BuildingWithID(Polygon building) {
      this.building = building;

    }


    public boolean isTriangleInBuilding(TPoint point) {
      return this.building.intersects(FACTORY.createPoint(TPointToCoordinate(point)));
    }


  }

  private int getOrAppendVertices(Coordinate newCoord, List<Coordinate> vertices, HashMap<Integer, LinkedList<Integer>> hashOfArrayIndex) {
    // We can obtain the same hash with two different coordinate (4 Bytes or
    // 8 Bytes against 12 or 24 Bytes) , then we use a list as the value of
    // the hashmap
    // First step - Search the vertice parameter within the hashMap
    int newCoordIndex = -1;
    Integer coordinateHash = newCoord.hashCode();
    LinkedList<Integer> listOfIndex = hashOfArrayIndex.get(coordinateHash);
    if (listOfIndex != null) // There are the same hash value
    {
      for (int vind : listOfIndex) // Loop inside the coordinate index
      {
        if (newCoord.equals3D(vertices.get(vind))) // the coordinate is
        // equal to the
        // existing
        // coordinate
        {
          newCoordIndex = vind;
          break; // Exit for loop
        }
      }
      if (newCoordIndex == -1) {
        // No vertices has been found, we push the new coordinate into
        // the existing linked list
        newCoordIndex = vertices.size();
        listOfIndex.add(newCoordIndex);
        vertices.add(newCoord);
      }
    } else {
      // Push a new hash element
      listOfIndex = new LinkedList<Integer>();
      newCoordIndex = vertices.size();
      listOfIndex.add(newCoordIndex);
      vertices.add(newCoord);
      hashOfArrayIndex.put(coordinateHash, listOfIndex);
    }
    return newCoordIndex;
  }

  private static int getPointAttribute(TriangulationPoint p) {
    if (p instanceof PointWithAttribute) {
      return ((PointWithAttribute) p).getAttribute();
    } else {
      return -1;
    }
  }

  private static int getTriangleIndex(Map<DelaunayTriangle, Integer> index, DelaunayTriangle instance) {
    if (instance == null) {
      return -1;
    } else {
      Integer res = index.get(instance);
      if (res == null) {
        return -1;
      } else {
        return res;
      }
    }
  }

  @Override
  public void processDelaunay() throws LayerDelaunayError {
    // Create input data for Poly2Tri
    int[] index = new int[segments.size()];
    for (int i = 0; i < index.length; i++) {
      index[i] = segments.get(i);
    }
    // Construct final points array by reversing key,value of hash map
    TriangulationPoint[] ptsArray = new TriangulationPoint[pointsCount.get()];
    for(Map.Entry<TriangulationPoint, Integer> entry : pts.entrySet()) {
      ptsArray[entry.getValue()] = entry.getKey();
    }
    pts.clear();
    List<TriangulationPoint> meshPoints = new ArrayList<>(Arrays.asList(ptsArray));
    ConstrainedPointSet convertedInput = new ConstrainedPointSet(meshPoints, index);


    STRtree buildingsRtree = null;
    if(maxArea > 0) {
      // STRtree minimal size is 2
      buildingsRtree = new STRtree(Math.max(10, buildingWithID.size()));
      for (Map.Entry<Integer, BuildingWithID> buildingWithIDEntry : buildingWithID.entrySet()) {
        buildingsRtree.insert(buildingWithIDEntry.getValue().building.getEnvelopeInternal(), buildingWithIDEntry.getKey());
      }
    }

    boolean refine;
    do {
      // Triangulate
      Poly2Tri.triangulate(TriangulationAlgorithm.DTSweep, convertedInput);
      refine = false;

      // Will triangulate multiple time if refinement is necessary
      if(buildingsRtree != null && maxArea > 0) {
        List<DelaunayTriangle> trianglesDelaunay = convertedInput.getTriangles();
        for (DelaunayTriangle triangle : trianglesDelaunay) {
          if(triangle.area() > maxArea) {
            // Insert steiner point in centroid
            TPoint centroid = triangle.centroid();
            // Do not add steiner points into buildings
            Envelope searchEnvelope = new Envelope(TPointToCoordinate(centroid));
            searchEnvelope.expandBy(1.);
            List polyInters = buildingsRtree.query(searchEnvelope);
            boolean inBuilding = false;
            for (Object id : polyInters) {
              if (id instanceof Integer) {
                BuildingWithID inPoly = buildingWithID.get(id);
                if (inPoly.building.contains(FACTORY.createPoint(TPointToCoordinate(centroid)))) {
                    inBuilding = true;
                    break;
                }
              }
            }
            if(!inBuilding) {
              meshPoints.add(centroid);
              refine = true;
            }
          }
        }
        if(refine) {
          convertedInput = new ConstrainedPointSet(meshPoints, index);
        }
      }
    } while (refine);


    List<DelaunayTriangle> trianglesDelaunay = convertedInput.getTriangles();
    List<Integer> triangleAttribute = Arrays.asList(new Integer[trianglesDelaunay.size()]);
    // Create an index of triangles instance for fast neighbors search
    Map<DelaunayTriangle, Integer> triangleSearch = new HashMap<>(trianglesDelaunay.size());
    int triangleIndex = 0;
    if (computeNeighbors) {
      for (DelaunayTriangle triangle : trianglesDelaunay) {
        triangleSearch.put(triangle, triangleIndex);
        triangleIndex++;
      }
    }

    //Build ArrayList for binary search
    //test add height
    int triangleId = 0;
    for (DelaunayTriangle triangle : trianglesDelaunay) {
      Coordinate[] ring = new Coordinate[]{TPointToCoordinate(triangle.points[0]), TPointToCoordinate(triangle.points[1]), TPointToCoordinate(triangle.points[2]), TPointToCoordinate(triangle.points[0])};
      //if one of three vertices have buildingID and buildingID>=1
      if (getPointAttribute(triangle.points[0]) >= 1 || getPointAttribute(triangle.points[1]) >= 1 || getPointAttribute(triangle.points[2]) >= 1) {
        int propertyBulidingID = 0;
        for (int i = 0; i <= 2; i++) {
          int potentialBuildingID = getPointAttribute(triangle.points[i]);
          if (potentialBuildingID >= 1) {
            //get the Barycenter of the triangle so we can sure this point is in this triangle and we will check if the building contain this point
            if (this.buildingWithID.get(potentialBuildingID).isTriangleInBuilding(triangle.centroid())) {
              propertyBulidingID = potentialBuildingID;
              break;
            }
          }
        }
        triangleAttribute.set(triangleId, propertyBulidingID);
      } else {
        //if there are less than 3 points have buildingID this triangle is out of building
        triangleAttribute.set(triangleId, 0);
      }

      if (!CGAlgorithms.isCCW(ring)) {
        Coordinate tmp = new Coordinate(ring[0]);
        ring[0] = ring[2];
        ring[2] = tmp;
      }

      int a = getOrAppendVertices(ring[0], vertices, hashOfArrayIndex);
      int b = getOrAppendVertices(ring[1], vertices, hashOfArrayIndex);
      int c = getOrAppendVertices(ring[2], vertices, hashOfArrayIndex);
      triangles.add(new Triangle(a, b, c, triangleAttribute.get(triangleId)));
      if (computeNeighbors) {
        // Compute neighbors index
        neighbors.add(new Triangle(getTriangleIndex(triangleSearch, triangle.neighborAcross(triangle.points[0])), getTriangleIndex(triangleSearch, triangle.neighborAcross(triangle.points[1])), getTriangleIndex(triangleSearch, triangle.neighborAcross(triangle.points[2]))));
      }
      triangleId++;
    }
  }

  private static class SetZFilter implements CoordinateSequenceFilter {
    private boolean done = false;

    @Override
    public void filter(CoordinateSequence seq, int i) {
      double x = seq.getX(i);
      double y = seq.getY(i);
      double z = seq.getOrdinate(i, 2);
      seq.setOrdinate(i, 0, x);
      seq.setOrdinate(i, 1, y);
      if (Double.isNaN(z)) {
        seq.setOrdinate(i, 2, 0);
      } else {
        seq.setOrdinate(i, 2, z);
      }
      if (i == seq.size()) {
        done = true;
      }
    }

    @Override
    public boolean isDone() {
      return done;
    }

    @Override
    public boolean isGeometryChanged() {
      return true;
    }
  }

  /**
   * Add height of building
   *
   * @return
   */
  @Override
  public void addPolygon(Polygon newPoly, int buildingId) throws LayerDelaunayError {

    //// To avoid errors we set the Z coordinate to 0.
    SetZFilter zFilter = new SetZFilter();
    newPoly.apply(zFilter);
    GeometryFactory factory = new GeometryFactory();
    final Coordinate[] coordinates = newPoly.getExteriorRing().getCoordinates();
    if (coordinates.length > 1) {
      LineString newLineString = factory.createLineString(coordinates);
      this.addLineString(newLineString, buildingId);
      this.buildingWithID.put(buildingId, new BuildingWithID(newPoly));
    }
    // Append holes
    final int holeCount = newPoly.getNumInteriorRing();
    for (int holeIndex = 0; holeIndex < holeCount; holeIndex++) {
      LineString holeLine = newPoly.getInteriorRingN(holeIndex);
      // Convert hole into a polygon, then compute an interior point
      Polygon polyBuffnew = factory.createPolygon(factory.createLinearRing(holeLine.getCoordinates()), null);
      if (polyBuffnew.getArea() > 0.) {
        Coordinate interiorPoint = polyBuffnew.getInteriorPoint().getCoordinate();
        if (!factory.createPoint(interiorPoint).intersects(holeLine)) {
          this.addLineString(holeLine, buildingId);
        } else {
          LOGGER.info("Warning : hole rejected, can't find interior point.");
        }
      } else {
        LOGGER.info("Warning : hole rejected, area=0");
      }
    }
  }

  @Override
  public void setMinAngle(Double minAngle) {
    // TODO Auto-generated method stub

  }

  @Override
  public void hintInit(Envelope bBox, long polygonCount, long verticesCount) throws LayerDelaunayError {
  }

  @Override
  public List<Coordinate> getVertices() throws LayerDelaunayError {
    return this.vertices;
  }

  @Override
  public List<Triangle> getTriangles() throws LayerDelaunayError {
    return this.triangles;
  }

  @Override
  public void addVertex(Coordinate vertexCoordinate) throws LayerDelaunayError {
    pointHandler.addPt(vertexCoordinate, -1);
  }

  @Override
  public void setMaxArea(Double maxArea) throws LayerDelaunayError {
      this.maxArea = Math.max(0, maxArea);
  }

  //add buildingID to edge property and to points property
  public void addLineString(LineString lineToProcess, int buildingID) throws LayerDelaunayError {
    lineStringHandler.reset();
    lineStringHandler.setAttribute(buildingID);
    lineToProcess.apply(lineStringHandler);
  }
  //add buildingID to edge property and to points property

  @Override
  public void reset() {
    // TODO Auto-generated method stub

  }

  @Override
  public List<Triangle> getNeighbors() throws LayerDelaunayError {
    if (computeNeighbors) {
      return neighbors;
    } else {
      throw new LayerDelaunayError("You must call setRetrieveNeighbors(True) before process delaunay triangulation");
    }
  }

  @Override
  public void setRetrieveNeighbors(boolean retrieve) {
    this.computeNeighbors = retrieve;

  }


  private static class PointHandler implements CoordinateFilter {
    private LayerPoly2Tri delaunayData;
    private Map<TriangulationPoint, Integer> pts;
    private AtomicInteger maxIndex;

    public PointHandler(LayerPoly2Tri delaunayData, Map<TriangulationPoint, Integer> pts, AtomicInteger maxIndex) {
      this.delaunayData = delaunayData;
      this.pts = pts;
      this.maxIndex = maxIndex;
    }

    public Coordinate[] getPoints() {
      Coordinate[] ret = new Coordinate[pts.size()];
      int i = 0;
      for (TriangulationPoint pt : pts.keySet()) {
        ret[i] = TPointToCoordinate(pt);
        i++;
      }
      return ret;
    }

    protected int addPt(Coordinate coordinate, int attribute) {
      TPoint pt = new PointWithAttribute(delaunayData.r(coordinate.x), delaunayData.r(coordinate.y), Double.isNaN(coordinate.z) ? 0 : delaunayData.r(coordinate.z), attribute);
      Integer index = pts.get(pt);
      if (index == null) {
        index = maxIndex.getAndAdd(1);
        pts.put(pt, index);
      }
      return index;
    }

    @Override
    public void filter(Coordinate pt) {
      addPt(pt, -1);
    }
  }

  private static class LineStringHandler extends PointHandler {
    private List<Integer> segments;
    private int firstPtIndex = -1;
    private int attribute = -1;

    public LineStringHandler(LayerPoly2Tri delaunayData, Map<TriangulationPoint, Integer> pts, AtomicInteger maxIndex, List<Integer> segments) {
      super(delaunayData, pts, maxIndex);
      this.segments = segments;
    }

    /**
     * New line string
     */
    public void reset() {
      firstPtIndex = -1;
      attribute = -1;
    }

    public void setAttribute(int attribute) {
      this.attribute = attribute;
    }

    @Override
    public void filter(Coordinate pt) {
      if (firstPtIndex == -1) {
        firstPtIndex = addPt(pt, attribute);
      } else {
        int secondPt = addPt(pt, attribute);
        if (secondPt != firstPtIndex) {
          segments.add(firstPtIndex);
          segments.add(secondPt);
          firstPtIndex = secondPt;
        }
      }
    }
  }

  /**
   * Points instance are kept by poly2tri, so define our own point instance in order to retrieve attributes
   */
  private static class PointWithAttribute extends PolygonPoint {
    int attribute;

    public PointWithAttribute(double x, double y, int attribute) {
      super(x, y);
      this.attribute = attribute;
    }

    public PointWithAttribute(double x, double y, double z, int attribute) {
      super(x, y, z);
      this.attribute = attribute;
    }

    public int getAttribute() {
      return attribute;
    }

    public void setAttribute(int attribute) {
      this.attribute = attribute;
    }
  }
}
