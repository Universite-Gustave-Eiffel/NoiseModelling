package lcpc_son;

/***********************************
 * ANR EvalPDU
 * Lcpc 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

import java.util.ArrayList;
import java.util.List;
//import java.util.HashMap;
import java.util.LinkedList;
import org.apache.log4j.Logger;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;

public class LayerCTriangle implements LayerDelaunay {
	
	private static Logger logger = Logger.getLogger(LayerCTriangle.class.getName());
	private ArrayList<Coordinate> vertices = new ArrayList<Coordinate>();
	private LinkedList<Coordinate> holes = new LinkedList<Coordinate>();
	private ArrayList<IntSegment> segments=new ArrayList<IntSegment>();
	//HashMap<Integer,LinkedList<Integer>> hashOfArrayIndex=new HashMap<Integer,LinkedList<Integer>>();
	Quadtree ptQuad=new Quadtree();
	ArrayList<Triangle> triangles=new ArrayList<Triangle>();
	// Header generated thanks to the following command
	// "javah -jni -classpath ~/workspace/AcousMap/target/classes/ lcpc_son.LayerCTriangle"
    ///Native declaration of the C library
    private native void triangulateC(String options, CTriangleStruct structIn, CTriangleStruct structOut, CTriangleStruct structVeronoiOut);
    static {
    		System.load("/home/fortin/workspace/AcousMap/src/main/java/lcpc_son/libtrianglelib.so");
    		//System.load("C:/module_bruit/workspace/AcousMap/src/main/java/lcpc_son/trianglelib/bin/Release/libtrianglelib.dll");
    		//System.loadLibrary("trianglelib");
    	}
  
    private void initJniStruct(CTriangleStruct objToInit)
    {
    	objToInit.numberofpoints=this.vertices.size();
    	objToInit.numberofcorners=3;
    	objToInit.numberofedges=0;
    	objToInit.numberofholes=this.holes.size();
    	objToInit.numberofpointattributes=0;
    	objToInit.numberofregions=0;
    	objToInit.numberofsegments=this.segments.size();
    	objToInit.numberoftriangleattributes=0;
    	objToInit.numberoftriangles=0;
    	
    	objToInit.initArrays(true, false);
    	
    	for(int ivert=0;ivert<this.vertices.size();ivert++)
    	{
    		objToInit.pointlist[2*ivert]=this.vertices.get(ivert).x;
    		objToInit.pointlist[2*ivert+1]=this.vertices.get(ivert).y;
    	}
    	int ihole=0;
    	for(Coordinate hole : holes)
    	{
    		objToInit.holelist[2*ihole]=hole.x;
    		objToInit.holelist[2*ihole+1]=hole.y;
    		ihole++;
    	}
    	for(int iseg=0;iseg<this.segments.size();iseg++)
    	{
    		objToInit.segmentlist[2*iseg]=this.segments.get(iseg).getA();
    		objToInit.segmentlist[2*iseg+1]=this.segments.get(iseg).getB();
    	}
    	
    	logger.info("objToInit.numberofpoints="+objToInit.numberofpoints);
    }
    
    
	@SuppressWarnings("unchecked")
	private int getOrAppendVertices(Coordinate newCoord,ArrayList<Coordinate> vertices)
	{
		//We can obtain the same hash with two different coordinate (4 Bytes or 8 Bytes against 12 or 24 Bytes) , then we use a list as the value of the hashmap
		//First step - Search the vertice parameter within the hashMap
		int newCoordIndex=-1;
		Envelope queryEnv=new Envelope(newCoord);
		queryEnv.expandBy(1.f);
		List<EnvelopeWithIndex<Integer>> result=ptQuad.query(queryEnv);
		for(EnvelopeWithIndex<Integer> envel : result)
		{
			Coordinate foundCoord=vertices.get((int)envel.getId());
			if(foundCoord.distance(newCoord)<0.01)
			{
				return (int)envel.getId();
			}
		}
		//Not found then
		//Append to the list and QuadTree
		vertices.add(newCoord);
		newCoordIndex=vertices.size()-1;
		ptQuad.insert(queryEnv, new EnvelopeWithIndex<Integer>(queryEnv,newCoordIndex ));
		return newCoordIndex;
	}
    
	@Override
	public void processDelaunay() throws LayerDelaunayError {
		try {

			CTriangleStruct thisStructOut=new CTriangleStruct();
			CTriangleStruct thisStructVeronoiOut=new CTriangleStruct();
			CTriangleStruct thisStructIn=new CTriangleStruct();
			initJniStruct(thisStructIn);
			vertices.clear();
			String options= new String("zqY");
			triangulateC(options,thisStructIn,thisStructOut,thisStructVeronoiOut);
			logger.info("Size of triangulation :"+thisStructOut.numberoftriangles+" faces.");
			vertices.ensureCapacity(thisStructOut.numberofpoints);
			thisStructIn=null; //free memory
			for(int ivert=0;ivert<thisStructOut.numberofpoints;ivert++)
			{
				vertices.add(new Coordinate(thisStructOut.pointlist[2*ivert],thisStructOut.pointlist[2*ivert+1]));
			}
			triangles.ensureCapacity(thisStructOut.numberoftriangles);
			for(int itri=0;itri<thisStructOut.numberoftriangles;itri++)
			{
				triangles.add(new Triangle(thisStructOut.trianglelist[3*itri],thisStructOut.trianglelist[3*itri+1],thisStructOut.trianglelist[3*itri+2]));
			}			
		} catch (Exception e) {
			throw new LayerDelaunayError(e.getMessage());
		}
	}

	@Override
	public void setMinAngle(Double minAngle) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addPolygon(Polygon newPoly, boolean isEmpty)
			throws LayerDelaunayError {
		// Append main polygon
		final Coordinate[] coordinates = newPoly.getExteriorRing().getCoordinates();
		if(coordinates.length>1)
		{
			int firstVertIndex=getOrAppendVertices(coordinates[0], vertices);
			for (int i = 0; i < coordinates.length - 1; i++) {
				int secondVertIndex=getOrAppendVertices(coordinates[i+1], vertices);
				IntSegment newSeg=new IntSegment(firstVertIndex,secondVertIndex);
				segments.add(newSeg);				
				firstVertIndex=secondVertIndex;
			}
		}
		if(isEmpty)
		{
			holes.add(newPoly.getInteriorPoint().getCoordinate());
		}
		// Append holes
		final int holeCount = newPoly.getNumInteriorRing();
		GeometryFactory factory=new GeometryFactory();
		for(int holeIndex= 0;holeIndex < holeCount;holeIndex++)
		{
			LineString holeLine=newPoly.getInteriorRingN(holeIndex);
			//Convert hole into a polygon, then compute an interior point
			Polygon polyBuffnew=factory.createPolygon(factory.createLinearRing(holeLine.getCoordinates()),null);
			if(polyBuffnew.getArea()>0.)
			{
				Coordinate interiorPoint=polyBuffnew.getInteriorPoint().getCoordinate();
				if(!factory.createPoint(interiorPoint).intersects(holeLine))
				{
					if(!isEmpty)
						holes.add(interiorPoint);
					final Coordinate[] holeCoordinates = holeLine.getCoordinates();
					int firstVertIndex=getOrAppendVertices(holeCoordinates[0], vertices);
					for (int i = 0; i < holeCoordinates.length - 1; i++) {
						int secondVertIndex=getOrAppendVertices(holeCoordinates[i+1], vertices);
						IntSegment newSeg=new IntSegment(firstVertIndex,secondVertIndex);
						segments.add(newSeg);				
						firstVertIndex=secondVertIndex;
					}
				}else{
					logger.info("Warning : hole rejected, can't find interior point.");
				}
			}else{
				logger.info("Warning : hole rejected, area=0");
			}
		}
	}


	@Override
	public void HintInit(Envelope boundingBox, long polygonCount,
			long verticesCount) throws LayerDelaunayError {
		this.vertices.ensureCapacity((int)verticesCount);
		this.segments.ensureCapacity((int)polygonCount*5);
		
		
	}

	@Override
	public ArrayList<Coordinate> getVertices() throws LayerDelaunayError {
		return vertices;
	}

	@Override
	public ArrayList<Triangle> getTriangles() throws LayerDelaunayError {
		return triangles;
	}


	@Override
	public void addVertex(Coordinate vertexCoordinate)
			throws LayerDelaunayError {
		getOrAppendVertices(vertexCoordinate, vertices);		
	}




	@Override
	public void setMaxArea(Double maxArea) throws LayerDelaunayError {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void addLineString(LineString line) throws LayerDelaunayError {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void reset() {
		this.holes.clear();
		this.segments.clear();
		this.ptQuad= new Quadtree();
		this.triangles.clear();
		this.vertices.clear();
	}


}
