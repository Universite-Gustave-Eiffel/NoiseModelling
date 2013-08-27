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
package org.noisemap.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;

import org.jdelaunay.delaunay.ConstrainedMesh;
import org.jdelaunay.delaunay.geometries.DEdge;
import org.jdelaunay.delaunay.geometries.DPoint;
import org.jdelaunay.delaunay.geometries.DTriangle;
import org.jdelaunay.delaunay.error.DelaunayError;
import org.jdelaunay.delaunay.geometries.Element;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFilter;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Point;
/**
 * 
 * @author Nicolas Fortin
 */
public class LayerJDelaunay implements LayerDelaunay {
	private static Logger logger = Logger.getLogger(LayerJDelaunay.class
			.getName());
	private List<Coordinate> vertices = new ArrayList<Coordinate>();
	private ArrayList<DEdge> constraintEdge = new ArrayList<DEdge>();
	private LinkedList<DPoint> ptToInsert = new LinkedList<DPoint>();
        private HashMap<Integer,BuildingWithID> buildingWithID=new HashMap<Integer,BuildingWithID>();
	private List<Coordinate> holes = new LinkedList<Coordinate>();
	private boolean debugMode=false; //output primitives in a text file
	private boolean computeNeighbors=false;
	List<Triangle> triangles = new ArrayList<Triangle>();
	private List<Triangle> neighbors = new ArrayList<Triangle>(); // The first neighbor triangle is opposite the first corner of triangle  i
	HashMap<Integer, LinkedList<Integer>> hashOfArrayIndex = new HashMap<Integer, LinkedList<Integer>>();
        //this value is to check if the polygon is counter-clockwise when polygon's have 3 or more coordiantes. 
        //-1 there are no polygon have 3 or more coordiantes
        //0 polygons are oriented counter-clockwise
        //1 polygons are oriented clockwise
        //2 warning
        private int checkisCCW=-1;
        //triangletest is for JDeLaunayTriangleDirectionChange to test triangle direction
        private List<DTriangle> triangletest=new ArrayList<DTriangle>();
        
        
	private static DTriangle findTriByCoordinate(Coordinate pos,List<DTriangle> trilst) throws DelaunayError {
		DPoint pt;
		if(Double.isNaN(pos.z)) {
			pt=new DPoint(pos.x,pos.y,0);
		} else {
			pt=new DPoint(pos);
		}
		Element foundEl=trilst.get(trilst.size()/2).searchPointContainer(pt);
		if(foundEl instanceof DTriangle) {
			return (DTriangle)foundEl;
		}else{
			for(DTriangle tri : trilst) {
				if(tri.contains(pt)) {
					return tri;
				}
			}
			return null;
		}
	}
	private static class SetZFilter implements CoordinateSequenceFilter {
		private boolean done = false;

		@Override
		public void filter(CoordinateSequence seq, int i) {
			double x = seq.getX(i);
			double y = seq.getY(i);
			seq.setOrdinate(i, 0, x);
			seq.setOrdinate(i, 1, y);
			seq.setOrdinate(i, 2, 0);
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

        private static class BuildingWithID{
                private Polygon building;
                
                
                public BuildingWithID(Polygon building) {
                    this.building=building;
                    
                }

                public boolean isTriangleInBuilding(DPoint point)
                {
                    GeometryFactory factory=new GeometryFactory();
                    Point middlepoint=factory.createPoint(point.getCoordinate());
                    return this.building.contains(middlepoint);
                }

              
        }
	private int getOrAppendVertices(Coordinate newCoord,
			List<Coordinate> vertices,
			HashMap<Integer, LinkedList<Integer>> hashOfArrayIndex) {
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

	private ConstrainedMesh delaunayTool = null;

	@Override
	public void processDelaunay() throws LayerDelaunayError {
		if (delaunayTool != null) {
			try {
				// Push segments
				delaunayTool.setPoints(ptToInsert);
				delaunayTool.setConstraintEdges(constraintEdge);
			
				if(debugMode) {
					try
					{
					//Debug mode write input & output data to files
					File file = new File("./layerjdlaunaydebug"+System.currentTimeMillis()+".txt");	
					// Initialization
					PrintWriter out = new PrintWriter(new FileOutputStream(file));
						
					out.printf("DPoint pts[]={");
					// write pts
					for(DPoint pt : ptToInsert) {
						out.printf("new DPoint(%s, %s, %s),\r\n",Double.toString(pt.getX()),Double.toString(pt.getY()),Double.toString(pt.getZ()));
					}
					out.printf("};\r\n");
					//write segments
					out.printf("DEdge edges[]={");
					// write pts

					for(DEdge edge : constraintEdge) {
						DPoint pt=edge.getStartPoint();
						DPoint pt2=edge.getEndPoint();
						out.printf("new DEdge(%s, %s, %s,%s, %s, %s),\r\n",Double.toString(pt.getX()),Double.toString(pt.getY()),Double.toString(pt.getZ()),Double.toString(pt2.getX()),Double.toString(pt2.getY()),Double.toString(pt2.getZ()));
					}
					out.printf("};\r\n");					
					out.close();
					} catch (FileNotFoundException e) {
						throw new LayerDelaunayError(e.getMessage());
					}
				}
				
				
				delaunayTool.forceConstraintIntegrity();
				delaunayTool.processDelaunay();
				constraintEdge.clear();
				ptToInsert.clear();
				List<DTriangle> trianglesDelaunay = delaunayTool
						.getTriangleList();
                                //this value is for the unit test
                                triangletest=delaunayTool.getTriangleList();
				//triangles.ensureCapacity(trianglesDelaunay.size());// reserve
																	// memory
				HashMap<Integer, Integer> gidToIndex = new HashMap<Integer, Integer>();
				ArrayList<Triangle> gidTriangle=new ArrayList<Triangle>(trianglesDelaunay.size());
				
				//Build ArrayList for binary search
                                //test add height
            
                                
                                
                                for (DTriangle triangle : trianglesDelaunay) {
                                                Coordinate [] ring = new Coordinate [] {triangle.getPoint(0).getCoordinate(),triangle.getPoint(1).getCoordinate(),triangle.getPoint(2).getCoordinate(),triangle.getPoint(0).getCoordinate()};
						boolean orientationReversed=false;
                                                //if 3 points have buildingID and buildingID>=1
                                                if(triangle.getPoint(0).getProperty()==triangle.getPoint(1).getProperty()&&triangle.getPoint(0).getProperty()==triangle.getPoint(2).getProperty()&&triangle.getPoint(0).getProperty()>=1){
                                                    //get the Barycenter of the triangle so we can sure this point is in this triangle and we will check if the building contain this point
                                                    int propertyBuildingID=triangle.getPoint(0).getProperty();
                                                    if(this.buildingWithID.get(propertyBuildingID).isTriangleInBuilding(triangle.getBarycenter())){
                                                        triangle.setProperty(propertyBuildingID);
                                                    }
                                                    else{
                                                        triangle.setProperty(0);
                                                    }
                                                
                                                
                                                }
                                                //if there are less than 3 points have buildingID this triangle is out of building
                                                else{
                                                    triangle.setProperty(0);
                                                }
                                                                                                         
                                                              
                                                              if(!CGAlgorithms.isCCW(ring)) {
                                                                  Coordinate tmp= new Coordinate(ring[0]);
                                                                  ring[0]=ring[2];
                                                                  ring[2]=tmp;
                                                                  orientationReversed=true;
                                                                }
							
                                                              int a = getOrAppendVertices(ring[0], vertices, hashOfArrayIndex);
                                                              int b = getOrAppendVertices(ring[1], vertices, hashOfArrayIndex);
                                                              int c = getOrAppendVertices(ring[2], vertices, hashOfArrayIndex);
                                                              int buildingID=triangle.getProperty();
                                                              triangles.add(new Triangle(a, b, c,buildingID));
                                                              if(this.computeNeighbors) {
                                                                    Triangle gidTri=new Triangle(-1,-1,-1,0);
                                                                    for(int i=0;i<3;i++) {
                                                                            DTriangle neighTriangle = triangle.getOppositeEdge(triangle.getPoint(i)).getOtherTriangle(triangle);
                                                                            if(neighTriangle!=null&& neighTriangle.getExternalGID()!=0) {
                                                                                //if neighbor is in building
                                                                                    int neighBuildingID=0;
                                                                                    if(neighTriangle.getPoint(0).getProperty()==neighTriangle.getPoint(1).getProperty()&&neighTriangle.getPoint(0).getProperty()==neighTriangle.getPoint(2).getProperty()&&(neighTriangle.getPoint(0).getProperty()>=1)){
                                                                                        neighBuildingID=neighTriangle.getPoint(0).getProperty();
                                                                                        if(this.buildingWithID.get(neighBuildingID).isTriangleInBuilding(neighTriangle.getBarycenter())){
                                                                                            neighTriangle.setProperty(neighBuildingID);
                                                                                        }
                                                                                        else{
                                                                                            neighTriangle.setProperty(0);
                                                                                        }
                                                                                    
                                                                                    }    
                                                                                    gidTri.set(i,neighTriangle.getGID());
                                                                                    
                                                                            }
                                                                    }
                                                                    gidTri.setBuidlingID(buildingID);
                                                                    if(!orientationReversed) {
                                                                            gidTriangle.add(gidTri);
                                                                    } else {
                                                                            gidTriangle.add(new Triangle(gidTri.getC(),gidTri.getB(),gidTri.getA(),buildingID));
                                                                    }
                                                                    gidToIndex.put(triangle.getGID(),gidTriangle.size()-1);
                                                            }    
                                                
                                                
						
                                                
                                                
                                                
                                                
					}
				
                                /*
				for (DTriangle triangle : trianglesDelaunay) {
					if(triangle.getExternalGID()!=0) //Not a hole
					{
						boolean orientationReversed=false;
						Coordinate [] ring = new Coordinate [] {triangle.getPoint(0).getCoordinate(),triangle.getPoint(1).getCoordinate(),triangle.getPoint(2).getCoordinate(),triangle.getPoint(0).getCoordinate()};
						if(!CGAlgorithms.isCCW(ring)) {
							Coordinate tmp= new Coordinate(ring[0]);
							ring[0]=ring[2];
							ring[2]=tmp;
							orientationReversed=true;
						}
							
						int a = getOrAppendVertices(ring[0], vertices, hashOfArrayIndex);
						int b = getOrAppendVertices(ring[1], vertices, hashOfArrayIndex);
						int c = getOrAppendVertices(ring[2], vertices, hashOfArrayIndex);
                                                double height=triangle.getHeight();
						triangles.add(new Triangle(a, b, c,height));
						if(this.computeNeighbors) {
							Triangle gidTri=new Triangle(-1,-1,-1,0);
							for(int i=0;i<3;i++) {
								DTriangle neighTriangle = triangle.getOppositeEdge(triangle.getPoint(i)).getOtherTriangle(triangle);
								if(neighTriangle!=null && neighTriangle.getExternalGID()!=0) {
									gidTri.set(i,neighTriangle.getGID());
                                                                        gidTri.setHeight(neighTriangle.getHeight());//set height
								}
							}
							if(!orientationReversed) {
								gidTriangle.add(gidTri);
							} else {
								gidTriangle.add(new Triangle(gidTri.getC(),gidTri.getB(),gidTri.getA(),gidTri.getHeight()));
							}
							gidToIndex.put(triangle.getGID(),gidTriangle.size()-1);
						}
					}
				}
                                */
				if(this.computeNeighbors) {
					//Translate GID to local index
					for(Triangle tri : gidTriangle) {
						Triangle localTri=new Triangle(-1,-1,-1,0);
						for(int i=0;i<3;i++) {
							int index=tri.get(i);
							if(index!=-1){
								localTri.set(i, gidToIndex.get(index));
                                                        }
                                                        
						}
                                                localTri.setBuidlingID(tri.getBuidlingID());
						neighbors.add(localTri);
					}
				}
				delaunayTool = null;

			} catch (DelaunayError e) {
				String msgStack=new String();
				for(StackTraceElement lign : e.getStackTrace()) {
					msgStack.concat(lign.toString()+"\n");
				}
				throw new LayerDelaunayError(e.getMessage()+msgStack);
			}
		}
	}
	private void addHole(Coordinate holePosition) throws LayerDelaunayError
	{
		holes.add(holePosition);
	}
	@Override
	public void addPolygon(Polygon newPoly, boolean isEmpty)
			throws LayerDelaunayError {

		if (delaunayTool == null) {
			delaunayTool = new ConstrainedMesh();
		}

		// To avoid errors we set the Z coordinate to 0.
		SetZFilter zFilter = new SetZFilter();
		newPoly.apply(zFilter);
		GeometryFactory factory = new GeometryFactory();
		final Coordinate[] coordinates = newPoly.getExteriorRing()
				.getCoordinates();
		if (coordinates.length > 1) {
			LineString newLineString = factory.createLineString(coordinates);
			this.addLineString(newLineString);
		}
		if (isEmpty) {
			addHole(newPoly.getInteriorPoint().getCoordinate());
		}
		// Append holes
		final int holeCount = newPoly.getNumInteriorRing();
		for (int holeIndex = 0; holeIndex < holeCount; holeIndex++) {
			LineString holeLine = newPoly.getInteriorRingN(holeIndex);
			// Convert hole into a polygon, then compute an interior point
			Polygon polyBuffnew = factory.createPolygon(
					factory.createLinearRing(holeLine.getCoordinates()), null);
			if (polyBuffnew.getArea() > 0.) {
				Coordinate interiorPoint = polyBuffnew.getInteriorPoint()
						.getCoordinate();
				if (!factory.createPoint(interiorPoint).intersects(holeLine)) {
					if(!isEmpty) {
						addHole(interiorPoint);
					}
					this.addLineString(holeLine);
				} else {
					logger.info("Warning : hole rejected, can't find interior point.");
				}
			} else {
				logger.info("Warning : hole rejected, area=0");
			}
		}
	}

         /**
         * Add height of building 
         * @return
         */
        public void addPolygon(Polygon newPoly, boolean isEmpty,int biudlingID)
			throws LayerDelaunayError {

		if (delaunayTool == null) {
			delaunayTool = new ConstrainedMesh();
		}

		// To avoid errors we set the Z coordinate to 0.
		SetZFilter zFilter = new SetZFilter();
		newPoly.apply(zFilter);
		GeometryFactory factory = new GeometryFactory();
		final Coordinate[] coordinates = newPoly.getExteriorRing()
				.getCoordinates();
		if (coordinates.length > 1) {
			LineString newLineString = factory.createLineString(coordinates);
			this.addLineString(newLineString,biudlingID);
                        this.buildingWithID.put(biudlingID, new BuildingWithID(newPoly));
                }
		if (isEmpty) {
			addHole(newPoly.getInteriorPoint().getCoordinate());
		}
		// Append holes
		final int holeCount = newPoly.getNumInteriorRing();
		for (int holeIndex = 0; holeIndex < holeCount; holeIndex++) {
			LineString holeLine = newPoly.getInteriorRingN(holeIndex);
			// Convert hole into a polygon, then compute an interior point
			Polygon polyBuffnew = factory.createPolygon(
					factory.createLinearRing(holeLine.getCoordinates()), null);
			if (polyBuffnew.getArea() > 0.) {
				Coordinate interiorPoint = polyBuffnew.getInteriorPoint()
						.getCoordinate();
				if (!factory.createPoint(interiorPoint).intersects(holeLine)) {
					if(!isEmpty) {
						addHole(interiorPoint);
					}
					this.addLineString(holeLine);
				} else {
					logger.info("Warning : hole rejected, can't find interior point.");
				}
			} else {
				logger.info("Warning : hole rejected, area=0");
			}
		}
	}
	@Override
	public void setMinAngle(Double minAngle) {
		// TODO Auto-generated method stub

	}

	@Override
	public void hintInit(Envelope bBox, long polygonCount, long verticesCount)
			throws LayerDelaunayError {
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
	public void addVertex(Coordinate vertexCoordinate)
			throws LayerDelaunayError {
		this.getOrAppendVertices(vertexCoordinate, vertices, hashOfArrayIndex);
	}

	@Override
	public void setMaxArea(Double maxArea) throws LayerDelaunayError {
		// TODO Auto-generated method stub

	}

	@Override
	public void addLineString(LineString lineToProcess)
			throws LayerDelaunayError {
		Coordinate[] coords = lineToProcess.getCoordinates();
		try {
			for (int ind = 1; ind < coords.length; ind++) {
				this.constraintEdge.add(new DEdge(new DPoint(coords[ind - 1]),
						new DPoint(coords[ind])));
			}
		} catch (DelaunayError e) {
			throw new LayerDelaunayError(e.getMessage());
		}
	}

        //add buildingID to edge property and to points property
	public void addLineString(LineString lineToProcess,int buildingID)
			throws LayerDelaunayError {
		Coordinate[] coords = lineToProcess.getCoordinates();
		try {
			for (int ind = 1; ind < coords.length; ind++) {
                                DPoint point1=new DPoint(coords[ind - 1]);
                                DPoint point2=new DPoint(coords[ind]);
                                point1.setProperty(buildingID);
                                point2.setProperty(buildingID);
                                DEdge edge=new DEdge(point1,
						point2); 
                                edge.setProperty(buildingID);
				this.constraintEdge.add(edge);
			}
		} catch (DelaunayError e) {
			throw new LayerDelaunayError(e.getMessage());
		}
	}
        
	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Triangle> getNeighbors() throws LayerDelaunayError {
		if(computeNeighbors) {
			return neighbors;
		} else {
			throw new LayerDelaunayError("You must call setRetrieveNeighbors(True) before process delaunay triangulation");
		}
	}

	@Override
	public void setRetrieveNeighbors(boolean retrieve) {
		this.computeNeighbors=retrieve;
		
	}
        
              

}
