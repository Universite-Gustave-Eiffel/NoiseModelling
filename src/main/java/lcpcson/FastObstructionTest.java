package lcpcson;
/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Stack;
import org.grap.utilities.EnvelopeUtil;

import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.algorithm.NonRobustLineIntersector;
import com.vividsolutions.jts.algorithm.VectorMath;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.buffer.BufferParameters;

public class FastObstructionTest {
	double epsilon = 1e-12;
	private String tmpdir;
	private ArrayList<Triangle> triVertices;
	private ArrayList<Coordinate> vertices;
	private ArrayList<Triangle> triNeighbors; 	//Neighbors
	private LinkedList<Geometry> toUnite=new LinkedList<Geometry>(); //Polygon union
	private QueryGridIndex<Integer> triIndex=null;
	private int lastFountPointTriTest=0;
	private ArrayList<Double> verticesOpenAngle=null;
 
	
	
	public FastObstructionTest(String tmpdir) {
		super();
		this.tmpdir = tmpdir;
	}

	public void AddGeometry(Geometry obstructionPoly)
	{
		toUnite.add(obstructionPoly);		
	}
	private Geometry Merge(LinkedList<Geometry> toUnite, double bufferSize)
	{
		GeometryFactory geometryFactory = new GeometryFactory();
		Geometry geoArray[]=new Geometry[toUnite.size()];
		toUnite.toArray(geoArray);
		GeometryCollection polygonCollection = geometryFactory.createGeometryCollection(geoArray);
		return polygonCollection.buffer(bufferSize,0,BufferParameters.CAP_SQUARE);
	}
	private void AddPolygon(Polygon newpoly,LayerDelaunay delaunayTool,Geometry boundingBox) throws LayerDelaunayError
	{
		delaunayTool.addPolygon(newpoly,true);		
	}
	private void ExplodeAndAddPolygon(Geometry intersectedGeometry,LayerDelaunay delaunayTool,Geometry boundingBox) throws LayerDelaunayError
	{
		if(intersectedGeometry instanceof MultiPolygon || intersectedGeometry instanceof GeometryCollection )
		{
			for (int j = 0; j < intersectedGeometry.getNumGeometries(); j++)
			{
				Geometry subGeom = intersectedGeometry.getGeometryN(j);
				ExplodeAndAddPolygon(subGeom,delaunayTool,boundingBox);
			}
		}else if(intersectedGeometry instanceof Polygon)
		{
			AddPolygon((Polygon)intersectedGeometry,delaunayTool,boundingBox);
		}else if(intersectedGeometry instanceof LineString)
		{
			delaunayTool.addLineString((LineString)intersectedGeometry);
		}
	}
	//feeding
	public void FinishPolygonFeeding(Envelope boundingBoxFilter) throws LayerDelaunayError
	{
		verticesOpenAngle=null;
		LayerExtTriangle delaunayTool = new LayerExtTriangle(tmpdir);
		//Insert the main rectangle
		Geometry linearRing=EnvelopeUtil.toGeometry(boundingBoxFilter);
		if ( !(linearRing instanceof LinearRing))
			return;
		GeometryFactory factory = new  GeometryFactory();
		Polygon boundingBox=new Polygon((LinearRing)linearRing, null, factory);
		delaunayTool.addPolygon(boundingBox, false);
		
		//Merge polygon
		Geometry allbuilds=Merge(toUnite,0.);
		toUnite.clear();
		//Remove geometries out of the bounding box
		allbuilds=allbuilds.intersection(boundingBox);
		ExplodeAndAddPolygon(allbuilds,delaunayTool,boundingBox);
		//Process delaunay Triangulation
		delaunayTool.setMinAngle(0.);
		delaunayTool.setRetrieveNeighbors(true);
		delaunayTool.processDelaunay();
		
		//Get results
		this.triVertices=delaunayTool.getTriangles();
		this.vertices=delaunayTool.getVertices();
		this.triNeighbors=delaunayTool.getNeighbors();
		///////////////////////////////////
		//Feed GridIndex
		
		//int gridsize=(int)Math.pow(2, Math.log10(Math.pow(this.triVertices.size()+1,2)));
		int gridsize=64;
		triIndex=new QueryGridIndex<Integer>(boundingBoxFilter,gridsize ,gridsize);
		int triind=0;
		for(Triangle tri : this.triVertices)
		{
			final Coordinate[] triCoords={vertices.get(tri.getA()),vertices.get(tri.getB()),vertices.get(tri.getC()),vertices.get(tri.getA())};
			Polygon newpoly=factory.createPolygon(factory.createLinearRing(triCoords), null);
			triIndex.AppendGeometry(newpoly,triind);
			triind++;
		}
	}

	/**
	 * Compute the next triangle index.Find the shortest intersection point of triIndex segments to the p1 coordinate
	 * @param triIndex Triangle index
	 * @param propagationLine Propagation line
	 * @return Next triangle to the specified direction, -1 if there is no triangle neighbor.
	 */
	private int GetNextTri(final int triIndex, final LineSegment propagationLine, HashSet<Integer> navigationHistory)
	{
		NonRobustLineIntersector linters=new NonRobustLineIntersector();
		final Triangle tri=this.triVertices.get(triIndex);
		int nearestIntersectionSide=-1;
		double nearestIntersectionPtDist=Double.MAX_VALUE;
		//Find intersection pt
		final Coordinate aTri=this.vertices.get(tri.getA());
		final Coordinate bTri=this.vertices.get(tri.getB());
		final Coordinate cTri=this.vertices.get(tri.getC());
		
		//Intersection First Side
		linters.computeIntersection(propagationLine.p0, propagationLine.p1, aTri,bTri);

		
		if(linters.hasIntersection())
		{
			Coordinate i0=linters.getIntersection(0);
			if(linters.getIntersectionNum()==1)
			{
				double dist=i0.distance(propagationLine.p1);
				if(dist<nearestIntersectionPtDist && !navigationHistory.contains(this.triNeighbors.get(triIndex).get(2)))
				{
					nearestIntersectionPtDist=dist;
					nearestIntersectionSide=2;
				}
			}else{
				//Colinear intersection
				Coordinate i1=linters.getIntersection(1);
				double dist0=i0.distance(propagationLine.p1);
				double dist1=i1.distance(propagationLine.p1);
				Coordinate nearestPt;
				if(dist0<dist1)
				{
					nearestPt=i0;
				}else{
					nearestPt=i1;
				}
				if(nearestPt==aTri)
				{
					return this.triNeighbors.get(triIndex).get(1);
				}else{
					return this.triNeighbors.get(triIndex).get(0);
				}
				
			}
		}
		
		//Intersection Second Side		
		linters.computeIntersection(propagationLine.p0, propagationLine.p1, bTri,cTri);
		if(linters.hasIntersection())
		{
			Coordinate i0=linters.getIntersection(0);
			if(linters.getIntersectionNum()==1)
			{
				double dist=i0.distance(propagationLine.p1);
				if(dist<nearestIntersectionPtDist && !navigationHistory.contains(this.triNeighbors.get(triIndex).get(0)))
				{
					nearestIntersectionPtDist=dist;
					nearestIntersectionSide=0;
				}
			}else{
				//Colinear intersection
				Coordinate i1=linters.getIntersection(1);
				double dist0=i0.distance(propagationLine.p1);
				double dist1=i1.distance(propagationLine.p1);
				Coordinate nearestPt;
				if(dist0<dist1)
				{
					nearestPt=i0;
				}else{
					nearestPt=i1;
				}
				if(nearestPt==bTri)
				{
					return this.triNeighbors.get(triIndex).get(2);
				}else{
					return this.triNeighbors.get(triIndex).get(1);
				}				
			}
		}
		
		//Intersection Third Side
		linters.computeIntersection(propagationLine.p0, propagationLine.p1, cTri,aTri);		
		if(linters.hasIntersection())
		{
			Coordinate i0=linters.getIntersection(0);
			if(linters.getIntersectionNum()==1)
			{
				double dist=i0.distance(propagationLine.p1);
				if(dist<nearestIntersectionPtDist && !navigationHistory.contains(this.triNeighbors.get(triIndex).get(1)))
				{
					nearestIntersectionPtDist=dist;
					nearestIntersectionSide=1;
				}
			}else{
				//Colinear intersection
				Coordinate i1=linters.getIntersection(1);
				double dist0=i0.distance(propagationLine.p1);
				double dist1=i1.distance(propagationLine.p1);
				Coordinate nearestPt;
				if(dist0<dist1)
				{
					nearestPt=i0;
				}else{
					nearestPt=i1;
				}
				if(nearestPt==cTri)
				{
					return this.triNeighbors.get(triIndex).get(0);
				}else{
					return this.triNeighbors.get(triIndex).get(2);
				}
				
			}
		}	
		
		if(nearestIntersectionSide!=-1)
			return this.triNeighbors.get(triIndex).get(nearestIntersectionSide);
		else
			return -1;
	}

	/**
	 * Fast dot in triangle test
	 * @see http://www.blackpawn.com/texts/pointinpoly/default.html
	 * @param p Coordinate of the point
	 * @param a Coordinate of the A vertex of triangle
	 * @param b Coordinate of the B vertex of triangle
	 * @param c Coordinate of the C vertex of triangle
	 * @return
	 */
	private boolean dotInTri(Coordinate p,Coordinate a,Coordinate b,Coordinate c)
	{
		Coordinate v0 = new Coordinate(c.x-a.x,c.y-a.y,0.);
		Coordinate v1 = new Coordinate(b.x-a.x,b.y-a.y,0.);
		Coordinate v2 = new Coordinate(p.x-a.x,p.y-a.y,0.);

		// Compute dot products
		double dot00 = VectorMath.dotProduct(v0, v0);
		double dot01 = VectorMath.dotProduct(v0, v1);
		double dot02 = VectorMath.dotProduct(v0, v2);
		double dot11 = VectorMath.dotProduct(v1, v1);
		double dot12 = VectorMath.dotProduct(v1, v2);

		// Compute barycentric coordinates
		double invDenom = 1 / (dot00 * dot11 - dot01 * dot01);
		double u = (dot11 * dot02 - dot01 * dot12) * invDenom;
		double v = (dot00 * dot12 - dot01 * dot02) * invDenom;

		// Check if point is in triangle
		return (u > (0.-epsilon)) && (v > (0.-epsilon)) && (u + v < (1.+epsilon));

	}
	Coordinate[] GetTriangle(int triIndex)
	{
		final Triangle tri=this.triVertices.get(triIndex);
		Coordinate[] coords={this.vertices.get(tri.getA()),this.vertices.get(tri.getB()),this.vertices.get(tri.getC())};
		return coords;
	}
	/**
	 * Return the triangle id from a point coordinate inside the triangle
	 * @param pt
	 * @return Triangle Id, Or -1 if no triangle has been found
	 */
	

	private int GetTriangleIdByCoordinate(Coordinate pt)
	{
		//Shortcut, test if the last found triangle contain this point, if not use the quadtree
		Coordinate[] trit=GetTriangle(lastFountPointTriTest);
		if(dotInTri(pt,trit[0],trit[1],trit[2]))
			return lastFountPointTriTest;

		Envelope ptEnv=new Envelope(pt);
		ArrayList<Integer> res=triIndex.query(new Envelope(ptEnv));
		for(int triIndex : res)
		{
			Coordinate[] tri=GetTriangle(triIndex);
			if(dotInTri(pt,tri[0],tri[1],tri[2]))
			{
				lastFountPointTriTest=triIndex;
				return triIndex;
			}
		}
			
		return -1;
	}
	/**
	 * 
	 * @param minAngle Minimum angle [0-2Pi]
	 * @param maxAngle Maximum angle [0-2Pi]
	 * @return List of corners within parameters range
	 */
	public ArrayList<Coordinate> GetWideAnglePoints(double minAngle, double maxAngle)
	{
		ArrayList<Coordinate> wideAnglePts=new ArrayList<Coordinate>(vertices.size());
		if(verticesOpenAngle==null)
		{
			verticesOpenAngle=new ArrayList<Double>(vertices.size()); //Reserve size
			for(int idvert=0;idvert<vertices.size();idvert++)
			{
				verticesOpenAngle.add(0.);
			}
			for(Triangle tri : this.triVertices)
			{
				//Compute angle at each corner, then add to vertices angle array
				Coordinate triA=vertices.get(tri.getA());
				Coordinate triB=vertices.get(tri.getB());
				Coordinate triC=vertices.get(tri.getC());
				//Add A vertex angle
				verticesOpenAngle.set(tri.getA(),verticesOpenAngle.get(tri.getA())+Angle.angleBetween(triB,triA,triC));
				//Add B vertex angle
				verticesOpenAngle.set(tri.getB(),verticesOpenAngle.get(tri.getB())+Angle.angleBetween(triA,triB,triC));
				//Add A vertex angle
				verticesOpenAngle.set(tri.getC(),verticesOpenAngle.get(tri.getC())+Angle.angleBetween(triB,triC,triA));
			}
		}
		int idvert=0;
		for(Double angleVertex : verticesOpenAngle)
		{
			if(angleVertex>=minAngle && angleVertex<=maxAngle)
			{
				wideAnglePts.add(vertices.get(idvert));
			}	
			idvert++;
		}		
		return wideAnglePts;
	}
	/**
	 * Compute the list of segments corresponding to holes and domain limitation
	 * @param maxDist Maximum distance from origin to segments
	 * @param p1 Origin of search
	 * @return List of segment
	 * TODO return segment normal
	 */
	public LinkedList<LineSegment> GetLimitsInRange(double maxDist,Coordinate p1)
	{
		LinkedList<LineSegment> walls=new LinkedList<LineSegment>();
		int curTri=GetTriangleIdByCoordinate(p1);
		int nextTri=-1;
		HashSet<Integer> navigationHistory=new HashSet<Integer>();	//List all triangles already processed
		Stack<Integer> navigationNodes=new Stack<Integer>(); 		//List the current queue of triangles the process go through
		while(curTri!=-1)
		{
			navigationHistory.add(curTri);
			//for each side of the triangle
			Triangle neighboors=this.triNeighbors.get(curTri);
			nextTri=-1;
			for(short idside=0;idside<3;idside++)
			{
				if(!navigationHistory.contains(neighboors.get(idside)))
				{
					IntSegment segVerticesIndex=this.triVertices.get(curTri).GetSegment(idside);
					LineSegment side=new LineSegment(this.vertices.get(segVerticesIndex.getA()),this.vertices.get(segVerticesIndex.getB()));
					Coordinate closestPoint=side.closestPoint(p1);
					if(closestPoint.distance(p1)<=maxDist)
					{
						//In this direction there is a hole or this is outside of the geometry
						if(neighboors.get(idside)==-1)
						{
							walls.add(side);
						}else
						{
							//Store currentTriangle Id. This is where to go back when there is no more navigable neighbors at the next triangle
							navigationNodes.add(curTri);
							nextTri=neighboors.get(idside);
							break; //Next triangle
						} 
					}
				}
			}
			if(nextTri==-1 && !navigationNodes.empty())
			{
				//All the side have been rejected, go back by one on the navigation
				nextTri=navigationNodes.pop();
			}
			curTri=nextTri;
		}	
		return walls;		
	}
	public boolean IsFreeField(Coordinate p1,Coordinate p2)
	{
		LineSegment propaLine=new LineSegment(p1,p2);
		int curTri=GetTriangleIdByCoordinate(p1);
		HashSet<Integer> navigationHistory=new HashSet<Integer>();
		while(curTri!=-1)
		{
			navigationHistory.add(curTri);
			Coordinate[] tri=GetTriangle(curTri);
			if(dotInTri(p2,tri[0],tri[1],tri[2]))
				return true;
			curTri=this.GetNextTri(curTri, propaLine,navigationHistory);
		}
		return false;
	}
}
