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

import com.vividsolutions.jts.geom.*;

import org.poly2tri.Poly2Tri;
import org.poly2tri.geometry.polygon.PolygonPoint;
import org.poly2tri.triangulation.Triangulatable;
import org.poly2tri.triangulation.TriangulationAlgorithm;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;
import org.poly2tri.triangulation.point.TPoint;
import org.poly2tri.triangulation.sets.ConstrainedPointSet;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class LayerPoly2Tri implements LayerDelaunay {
    // Precision
    private MathContext mathContext = MathContext.DECIMAL64;
    private GeometryFactory gf;
    private Triangulatable convertedInput = null;


    private org.poly2tri.geometry.polygon.Polygon makePolygon(LineString lineString) {
        PolygonPoint[] points = new PolygonPoint[lineString.getNumPoints() - 1];
        for(int idPoint=0; idPoint < points.length; idPoint++) {
            Coordinate point = lineString.getCoordinateN(idPoint);
            points[idPoint] = new PolygonPoint(r(point.x), r(point.y), Double.isNaN(point.z) ? 0 : r(point.z));
        }
        return new org.poly2tri.geometry.polygon.Polygon(points);
    }

    private org.poly2tri.geometry.polygon.Polygon makePolygon(Polygon polygon) {
        org.poly2tri.geometry.polygon.Polygon poly = makePolygon(polygon.getExteriorRing());
        // Add holes
        for(int idHole = 0; idHole < polygon.getNumInteriorRing(); idHole++) {
            poly.addHole(makePolygon(polygon.getInteriorRingN(idHole)));
        }
        return poly;
    }

    private static Coordinate toJts(boolean is2d, org.poly2tri.geometry.primitives.Point pt) {
        if(is2d) {
            return new Coordinate(pt.getX(), pt.getY());
        } else {
            return new Coordinate(pt.getX(), pt.getY(), pt.getZ());
        }
    }

    private int getMinDimension(GeometryCollection geometries) {
        int dimension = Integer.MAX_VALUE;
        for (int i = 0; i < geometries.getNumGeometries(); i++) {
            dimension = Math.min(dimension, geometries.getGeometryN(i).getDimension());
        }
        if(dimension == Integer.MAX_VALUE) {
            dimension = -1;
        }
        return dimension;
    }

    private double r(double v) {
        return new BigDecimal(v).round(mathContext).doubleValue();
    }


    @Override
    public void hintInit(Envelope boundingBox, long polygonCount, long verticesCount) throws LayerDelaunayError {

    }

    @Override
    public void addPolygon(Polygon newPoly, boolean isEmpty) throws LayerDelaunayError {
        org.poly2tri.geometry.polygon.Polygon
    }

    @Override
    public void addPolygon(Polygon newPoly, boolean isEmpty, int attribute) throws LayerDelaunayError {

    }

    @Override
    public void addVertex(Coordinate vertexCoordinate) throws LayerDelaunayError {

    }

    @Override
    public void addLineString(LineString line) throws LayerDelaunayError {

    }

    @Override
    public void setMinAngle(Double minAngle) throws LayerDelaunayError {

    }

    @Override
    public void setMaxArea(Double maxArea) throws LayerDelaunayError {

    }

    @Override
    public void processDelaunay() throws LayerDelaunayError {

    }

    @Override
    public List<Coordinate> getVertices() throws LayerDelaunayError {
        return null;
    }

    @Override
    public List<Triangle> getTriangles() throws LayerDelaunayError {
        return null;
    }

    @Override
    public List<Triangle> getNeighbors() throws LayerDelaunayError {
        return null;
    }

    @Override
    public void reset() {

    }

    @Override
    public void setRetrieveNeighbors(boolean retrieve) {

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

        protected int addPt(Coordinate coordinate) {
            TPoint pt = new TPoint(delaunayData.r(coordinate.x), delaunayData.r(coordinate.y),
                    Double.isNaN(coordinate.z) ? 0 : delaunayData.r(coordinate.z));
            Integer index = pts.get(pt);
            if(index == null) {
                index = maxIndex.getAndAdd(1);
                pts.put(pt, index);
            }
            return index;
        }

        @Override
        public void filter(Coordinate pt) {
            addPt(pt);
        }
    }

    private static class LineStringHandler extends PointHandler {
        private List<Integer> segments;
        private int firstPtIndex = -1;

        public LineStringHandler(LayerPoly2Tri delaunayData, Map<TriangulationPoint, Integer> pts,
                                 AtomicInteger maxIndex, List<Integer> segments) {
            super(delaunayData, pts, maxIndex);
            this.segments = segments;
        }

        /**
         * New line string
         */
        public void reset() {
            firstPtIndex = -1;
        }

        @Override
        public void filter(Coordinate pt) {
            if (firstPtIndex == -1) {
                firstPtIndex = addPt(pt);
            } else {
                int secondPt = addPt(pt);
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
