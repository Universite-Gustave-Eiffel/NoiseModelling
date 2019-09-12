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
package org.noise_planet.noisemodelling.propagation;


import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.strtree.STRtree;

import java.util.*;


/**
 * MeshBuilder is a Delaunay Structure builder.
 * TODO enable add and query of geometry object (other than
 * fitting elements) into the delaunay triangulation.
 * It can also add the point with Z to complete the mesh with the topography
 *
 * @author Nicolas Fortin
 * @author SU Qi
 */


public class MeshBuilder {
    private List<Triangle> triVertices;
    private List<Coordinate> vertices;
    private List<Triangle> triNeighbors; // Neighbors
    private static final int BUILDING_COUNT_HINT = 1500; // 2-3 km² average buildings
    public static final List<Double> ALPHA_DEFAULT_VALUE = Collections.unmodifiableList(
            Arrays.asList(0.1,0.1,0.1,0.1,0.1,0.1,0.1,0.1));

    private List<PolygonWithHeight> polygonWithHeight = new ArrayList<>(BUILDING_COUNT_HINT);//list polygon with height
    private List<LineString> envelopeSplited = new ArrayList<>();
    private Envelope geometriesBoundingBox = new Envelope();
    private Set<Coordinate> topoPoints = new HashSet<Coordinate>();
    private Set<LineString> topoLines = new HashSet<LineString>();
    private boolean computeNeighbors = true;
    private double maximumArea = 0;
    private GeometryFactory factory = new GeometryFactory();
    private static final int EPSILON_MESH = 2; //Decimal value, Used for merged geometry precision

    public static final class PolygonWithHeight {
        protected final Geometry geo;
        //If we add the topographic, the building height will be the average ToPo Height+ Building Height of all vertices
        private double height;
        private List<Double> alpha = ALPHA_DEFAULT_VALUE;
        private double alphaUniqueValue = Double.NaN;
        private int primaryKey = -1;
        private final boolean hasHeight;

        public PolygonWithHeight(Geometry geo) {
            this.geo = geo;
            this.height = Double.MAX_VALUE;
            this.hasHeight = false;
        }

        public PolygonWithHeight(Geometry geo, double height) {
            this.geo = geo;
            this.height = height;
            this.hasHeight = height < Double.MAX_VALUE;
        }

        public PolygonWithHeight(Geometry geo, double height, double alphaUniqueValue) {
            this.geo = geo;
            this.height = height;
            this.hasHeight = height < Double.MAX_VALUE;
            setAlpha(alphaUniqueValue);
        }

        public PolygonWithHeight copy() {
            PolygonWithHeight copy = new PolygonWithHeight(geo, height, alpha);
            copy.alphaUniqueValue = alphaUniqueValue;
            copy.primaryKey = primaryKey;
            return copy;
        }
        
        public PolygonWithHeight(Geometry geo, double height, List<Double> alpha) {
            this.geo = geo;
            this.height = height;
            this.hasHeight = height < Double.MAX_VALUE;
            this.alpha = new ArrayList<>(alpha);
        }

        /**
         * @return Unique identifier of the building in the database
         */
        public int getPrimaryKey() {
            return primaryKey;
        }

        /**
         * @param primaryKey Unique identifier of the building in the database
         */
        public void setPrimaryKey(int primaryKey) {
            this.primaryKey = primaryKey;
        }

        public Geometry getGeometry() {
            return this.geo;
        }

        /**
         * @return Get absorption coefficient of walls
         */
        public List<Double> getAlpha() {
            return Collections.unmodifiableList(alpha);
        }


        /**
         * @param alpha Set absorption coefficient of walls
         */
        public void setAlpha(List<Double> alpha) {
            this.alpha = Collections.unmodifiableList(new ArrayList<>(alpha));
        }

        /**
         * @param alphaUniqueValue Set absorption coefficient of walls
         */
        public void setAlpha(double alphaUniqueValue) {
            List<Double> newAlpha = new ArrayList<>(PropagationProcessPathData.freq_lvl.size());
            for(double freq : PropagationProcessPathData.freq_lvl_exact) {
                newAlpha.add(getWallAlpha(alphaUniqueValue, freq));
            }
            this.alpha = newAlpha;
        }

        public double getHeight() {
            return this.height;
        }

        public void setHeight(Double height) {
            this.height = height;
        }

        /**
         * @return True if height property has been set
         */
        public boolean hasHeight() {
            return hasHeight;
        }
    }


    /**
     * Get WallAlpha
     */
    public static double getWallAlpha(double wallAlpha, double freq_lvl)
    {
        double value;
        if(wallAlpha >= 0 && wallAlpha <= 1) {
            // todo let the user choose if he wants to convert G to Sigma
            //value = GetWallImpedance(20000 * Math.pow (10., -2 * Math.pow (wallAlpha, 3./5.)),freq_lvl);
            value= wallAlpha;
        } else {
            value = GetWallImpedance(Math.min(20000, Math.max(20, wallAlpha)),freq_lvl);
        }
        return value;
    }

    public static double GetWallImpedance(double sigma, double freq_l)
    {
        double s = Math.log(freq_l / sigma);
        double x = 1. + 9.08 * Math.exp(-.75 * s);
        double y = 11.9 * Math.exp(-0.73 * s);
        ComplexNumber Z = new ComplexNumber(x, y);

        // Delany-Bazley method, not used in NoiseModelling for the moment
        /*double layer = 0.05; // Let user Choose
        if (layer > 0 && sigma < 1000)
        {
            s = 1000 * sigma / freq;
            double c = 340;
            double RealK= 2 * Math.PI * freq / c *(1 + 0.0858 * Math.pow(s, 0.70));
            double ImgK=2 * Math.PI * freq / c *(0.175 * Math.pow(s, 0.59));
            ComplexNumber k = ComplexNumber.multiply(new ComplexNumber(2 * Math.PI * freq / c,0) , new ComplexNumber(1 + 0.0858 * Math.pow(s, 0.70),0.175 * Math.pow(s, 0.59)));
            ComplexNumber j = new ComplexNumber(-0, -1);
            ComplexNumber m = ComplexNumber.multiply(j,k);
            Z[i] = ComplexNumber.divide(Z[i], (ComplexNumber.exp(m)));
        }*/

        return GetTrueWallAlpha(Z);
    }

    static double GetTrueWallAlpha(ComplexNumber impedance)         // TODO convert impedance to alpha
    {
        double alpha ;
        ComplexNumber z = ComplexNumber.divide(new ComplexNumber(1.0,0), impedance) ;
        double x = z.getRe();
        double y = z.getIm();
        double a1 = (x * x - y * y) / y ;
        double a2 = y / (x * x + y * y + x) ;
        double a3 = ((x + 1) *(x + 1) + y * y) / (x * x + y * y) ;
        alpha = 8 * x * (1 + a1 * Math.atan(a2) - x * Math.log(a3)) ;
        return alpha ;
    }


    public MeshBuilder() {
        super();
    }

    /**
     * Retrieve triangle list
     *
     * @return
     */
    public List<Triangle> getTriangles() {
        return triVertices;
    }

    /**
     * @return Envelope of buildings
     */
    public Envelope getGeometriesBoundingBox() {
        return new Envelope(geometriesBoundingBox);
    }

    /**
     * Retrieve neighbors triangle list
     *
     * @return
     */
    public List<Triangle> getTriNeighbors() {
        return triNeighbors;
    }

    /**
     * @return vertices list
     */
    public List<Coordinate> getVertices() {
        return vertices;
    }


    /**
     * @return Envelope
     */
    public Envelope getEnvelope() {
        return geometriesBoundingBox;
    }


    /**
     * Retrieve Buildings polygon with the height
     * @return the polygons(merged)  with a height "without" the effect Topographic.
     */
    public List<PolygonWithHeight> getPolygonWithHeight() {
        return polygonWithHeight;

    }

    public List<Coordinate> getBuildingCoordinates() {
        List<Coordinate>  coordinates = new ArrayList<>();
        for (int i=0;i<polygonWithHeight.size();i++){
            coordinates.addAll(Arrays.asList(polygonWithHeight.get(i).geo.getCoordinates()));
        }
        return coordinates;
    }

    public void addGeometry(Geometry obstructionPoly) {
        addGeometry(new PolygonWithHeight(obstructionPoly));
    }

    private void addGeometry(PolygonWithHeight poly) {
        this.geometriesBoundingBox.expandToInclude(poly.getGeometry().getEnvelopeInternal());
        polygonWithHeight.add(poly);
    }


    /**
     * Add a new building with height and merge this new building with existing buildings if they have intersections
     * When we merge the buildings, we will use The shortest height to new building
     *
     * @param obstructionPoly  building's Geometry
     * @param heightofBuilding building's Height
     */
    public void addGeometry(Geometry obstructionPoly, double heightofBuilding) {
        addGeometry(new PolygonWithHeight(obstructionPoly, heightofBuilding));
    }

    /**
     * Add a new building with height and merge this new building with existing buildings if they have intersections
     * When we merge the buildings, we will use The shortest height to new building
     *
     * @param obstructionPoly  building's Geometry
     * @param heightofBuilding building's Height
     * @param alpha Wall absorption coefficient
     */
    public void addGeometry(Geometry obstructionPoly, double heightofBuilding, List<Double> alpha) {
        addGeometry(new PolygonWithHeight(obstructionPoly, heightofBuilding, alpha));
    }

    /**
     * Add a new building with height and merge this new building with existing buildings if they have intersections
     * When we merge the buildings, we will use The shortest height to new building
     *
     * @param obstructionPoly  building's Geometry
     * @param heightofBuilding building's Height
     * @param alpha Wall absorption coefficient
     */
    public void addGeometry(Geometry obstructionPoly, double heightofBuilding, double[] alpha) {
        List<Double> alphaw = new ArrayList<>(alpha.length);
        for(double a : alpha) {
            alphaw.add(a);
        }
        addGeometry(new PolygonWithHeight(obstructionPoly, heightofBuilding, alphaw));
    }

    /**
     * Add a new building with height and merge this new building with existing buildings if they have intersections
     * When we merge the buildings, we will use The shortest height to new building
     *
     * @param obstructionPoly  building's Geometry
     * @param heightofBuilding building's Height
     * @param alpha Wall absorption coefficient
     */
    public PolygonWithHeight addGeometry(Geometry obstructionPoly, double heightofBuilding, double alpha) {
        PolygonWithHeight poly = new PolygonWithHeight(obstructionPoly, heightofBuilding, alpha);
        addGeometry(poly);
        return poly;
    }

    public void mergeBuildings(Geometry boundingBoxGeom) {
        // Delaunay triangulation request good quality input data
        // We have to merge buildings that may overlap
        Geometry[] toUnion = new Geometry[polygonWithHeight.size() + 1];
        STRtree buildingsRtree;
        if(toUnion.length > 10) {
            buildingsRtree = new STRtree(toUnion.length);
        } else {
            buildingsRtree = new STRtree();
        }
        int i = 0;
        for(PolygonWithHeight poly : polygonWithHeight) {
            toUnion[i] = poly.getGeometry();
            buildingsRtree.insert(poly.getGeometry().getEnvelopeInternal(), i);
            i++;
        }
        if(boundingBoxGeom instanceof Polygon) {
          // Add envelope to union of geometry
          toUnion[i] = ((Polygon)(boundingBoxGeom)).getExteriorRing();
        } else {
          toUnion[i] = factory.createPolygon(new Coordinate[0]);
        }
        Geometry geomCollection = factory.createGeometryCollection(toUnion);
        geomCollection = geomCollection.union();
        List<PolygonWithHeight> mergedPolygonWithHeight = new ArrayList<>(geomCollection.getNumGeometries());
        // For each merged buildings fetch all contained buildings and take the minimal height then insert into mergedPolygonWithHeight
        for(int idGeom = 0; idGeom < geomCollection.getNumGeometries(); idGeom++) {
            //fetch all contained buildings
            Geometry geometryN = geomCollection.getGeometryN(idGeom);
            if(geometryN instanceof Polygon) {
                List polyInters = buildingsRtree.query(geometryN.getEnvelopeInternal());
                double minHeight = Double.MAX_VALUE;
                List<Double> minAlpha = new ArrayList<>(ALPHA_DEFAULT_VALUE);
                int primaryKey = -1;
                for (Object id : polyInters) {
                    if (id instanceof Integer) {
                        PolygonWithHeight inPoly = polygonWithHeight.get((int) id);
                        if (inPoly.getGeometry().intersects(geometryN)) {
                            if(inPoly.getPrimaryKey() > -1) {
                                primaryKey = inPoly.getPrimaryKey();
                            }
                            if(inPoly.hasHeight) {
                                minHeight = Math.min(minHeight, inPoly.getHeight());
                            }
                            minAlpha = inPoly.getAlpha();
                            break;
                        }
                    }
                }
                PolygonWithHeight reconstructedBuilding = new PolygonWithHeight(geometryN, minHeight, minAlpha);
                reconstructedBuilding.setPrimaryKey(primaryKey);
                mergedPolygonWithHeight.add(reconstructedBuilding);
            } else if(geometryN instanceof LineString) {
              // Exterior envelope
              envelopeSplited.add((LineString)geometryN);
            }
        }
        polygonWithHeight = mergedPolygonWithHeight;
    }

    /**
     * Add the Topographic Point in the mesh data, to complete the topographic data.
     *
     * @param point Topographic Point
     */
    public void addTopographicPoint(Coordinate point) {
        if (Double.isNaN(point.z)) {
            point.setCoordinate(new Coordinate(point.x, point.y, 0.));
        }
        geometriesBoundingBox.expandToInclude(point);
        this.topoPoints.add(point);
    }

    public void addTopographicLine(LineString lineSegment) {
        geometriesBoundingBox.expandToInclude(lineSegment.getEnvelopeInternal());
        this.topoLines.add(lineSegment);
    }

    private void addPolygon(Polygon newpoly, LayerDelaunay delaunayTool,
                            int buildingID) throws LayerDelaunayError {
        // Fix clock wise orientation of the polygon and inner holes
        newpoly.normalize();
        delaunayTool.addPolygon(newpoly, buildingID);
    }

    private void explodeAndAddPolygon(Geometry intersectedGeometry,
                                      LayerDelaunay delaunayTool, int buildingID)
            throws LayerDelaunayError {

        if (intersectedGeometry instanceof GeometryCollection) {
            for (int j = 0; j < intersectedGeometry.getNumGeometries(); j++) {
                Geometry subGeom = intersectedGeometry.getGeometryN(j);
                explodeAndAddPolygon(subGeom, delaunayTool, buildingID);
            }
        } else if (intersectedGeometry instanceof Polygon) {
            addPolygon((Polygon) intersectedGeometry, delaunayTool, buildingID);
        } else if (intersectedGeometry instanceof LineString) {
            delaunayTool.addLineString((LineString) intersectedGeometry, buildingID);
        }
    }

    public void finishPolygonFeeding(Envelope boundingBoxFilter) throws LayerDelaunayError {
        finishPolygonFeeding(new GeometryFactory().toGeometry(boundingBoxFilter));
    }

    public void finishPolygonFeeding(Geometry boundingBoxGeom) throws LayerDelaunayError {
        // Insert the main rectangle
        if (!(boundingBoxGeom instanceof Polygon)) {
            return;
        }
        if (boundingBoxGeom != null) {
            this.geometriesBoundingBox = boundingBoxGeom.getEnvelopeInternal();
        }

        LayerDelaunay delaunayTool = new LayerPoly2Tri();


        //merge buildings
        mergeBuildings(boundingBoxGeom);


        for (LineString lineString : envelopeSplited) {
            delaunayTool.addLineString(lineString, -1);
        }

        //add topoPoints to delaunay
        if (!topoPoints.isEmpty() || !topoLines.isEmpty()) {
            for (Coordinate topoPoint : topoPoints) {
                delaunayTool.addVertex(topoPoint);
            }
            for(LineString topoLine : topoLines) {
                explodeAndAddPolygon(topoLine, delaunayTool, -1);
            }
        }

        //computeNeighbors
        delaunayTool.setRetrieveNeighbors(false);
        delaunayTool.processDelaunay();
        FastObstructionTest fastObstructionTest = new FastObstructionTest(new ArrayList<>(Collections.EMPTY_LIST), delaunayTool.getTriangles(),null,delaunayTool.getVertices());
        ComputeRays.AbsoluteCoordinateSequenceFilter absoluteCoordinateSequenceFilter = new ComputeRays.AbsoluteCoordinateSequenceFilter(fastObstructionTest, false);

        //add buildings to delaunay triangulation
        int i = 1;
        for (PolygonWithHeight polygon : polygonWithHeight) {
            Geometry geometry = polygon.getGeometry();
            geometry.apply(absoluteCoordinateSequenceFilter);
            explodeAndAddPolygon(geometry, delaunayTool, i);
            i++;
        }

        //Process delaunay Triangulation
        delaunayTool.setMinAngle(0.);
        //computeNeighbors
        delaunayTool.setRetrieveNeighbors(computeNeighbors);
        ////////////////////
        // Refine result
        // Triangle area
        if(maximumArea > 0) {
            delaunayTool.setMaxArea(maximumArea);
        }
        delaunayTool.processDelaunay();
        // Get results
        this.triVertices = delaunayTool.getTriangles();
        this.vertices = delaunayTool.getVertices();

        if(computeNeighbors) {
            this.triNeighbors = delaunayTool.getNeighbors();
        }
    }

    /**
     * Add a constraint on maximum triangle area.
     * @param maximumArea Value in square meter.
     */
    public void setMaximumArea(double maximumArea) {
        this.maximumArea = Math.max(0, maximumArea);
    }

    public void setComputeNeighbors(boolean computeNeighbors) {
        this.computeNeighbors = computeNeighbors;
    }

    //function just for test MergePolygon
    public void testMergeGetPolygonWithHeight() {

        for (PolygonWithHeight polygon : polygonWithHeight) {
            System.out.println("Polygon is:" + polygon.getGeometry().toString());
            System.out.println("Building height is:" + polygon.getHeight());
        }
    }

}