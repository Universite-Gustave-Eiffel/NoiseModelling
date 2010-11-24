package lcpc_son;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.jdelaunay.delaunay.DelaunayError;
import org.jdelaunay.delaunay.MyBox;
import org.jdelaunay.delaunay.MyMesh;
import org.jdelaunay.delaunay.MyPoint;
import org.jdelaunay.delaunay.MyPolygon;
import org.jdelaunay.delaunay.MyTriangle;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFilter;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;


public class LayerJDelaunay implements LayerDelaunay {
	private ArrayList<Coordinate> vertices = new ArrayList<Coordinate>();
	ArrayList<Triangle> triangles=new ArrayList<Triangle>();
	HashMap<Integer,LinkedList<Integer>> hashOfArrayIndex=new HashMap<Integer,LinkedList<Integer>>();
	LinkedList<MyPoint> ptToInsert=new LinkedList<MyPoint>();

	private static class SetZFilter implements CoordinateSequenceFilter {
		private boolean done = false;

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

		public boolean isDone() {
			return done;
		}

		public boolean isGeometryChanged() {
			return true;
		}
	}
	private int getOrAppendVertices(Coordinate newCoord,ArrayList<Coordinate> vertices,HashMap<Integer,LinkedList<Integer>> hashOfArrayIndex)
	{
		//We can obtain the same hash with two different coordinate (4 Bytes or 8 Bytes against 12 or 24 Bytes) , then we use a list as the value of the hashmap
		//First step - Search the vertice parameter within the hashMap
		int newCoordIndex=-1;
		Integer coordinateHash=newCoord.hashCode();
		LinkedList<Integer> listOfIndex=hashOfArrayIndex.get(coordinateHash);
		if(listOfIndex != null) //There are the same hash value
		{
			for(int vind : listOfIndex) //Loop inside the coordinate index
			{
				if(newCoord.equals3D(vertices.get(vind))) //the coordinate is equal to the existing coordinate
				{
					newCoordIndex=vind;
					break; //Exit for loop
				}
			}
			if(newCoordIndex==-1)
			{
				//No vertices has been found, we push the new coordinate into the existing linked list
				newCoordIndex=vertices.size();
				listOfIndex.add(newCoordIndex);
				vertices.add(newCoord);
			}
		}else{
			//Push a new hash element
			listOfIndex =  new LinkedList<Integer>();
			newCoordIndex=vertices.size();
			listOfIndex.add(newCoordIndex);
			vertices.add(newCoord);
			hashOfArrayIndex.put(coordinateHash,listOfIndex);
		}
		return newCoordIndex;
	}
	private MyMesh delaunayTool = null;

	@Override
	public void processDelaunay() throws LayerDelaunayError {
		if(delaunayTool!=null)
		{
			try {
				delaunayTool.processDelaunay();
				//Add pts
				while(!this.ptToInsert.isEmpty())
					this.delaunayTool.addPoint(this.ptToInsert.pop());				
				ArrayList<MyTriangle> trianglesDelaunay=delaunayTool.getTriangles();
				triangles.ensureCapacity(trianglesDelaunay.size());//reserve memory
				for(MyTriangle triangle : trianglesDelaunay)
				{
					int a=getOrAppendVertices(triangle.getPoint(0).getCoordinate(),vertices,hashOfArrayIndex);	
					int b=getOrAppendVertices(triangle.getPoint(1).getCoordinate(),vertices,hashOfArrayIndex);
					int c=getOrAppendVertices(triangle.getPoint(2).getCoordinate(),vertices,hashOfArrayIndex);				
					triangles.add(new Triangle(a,b,c));
				}
				delaunayTool=null;

			} catch (DelaunayError e) {
				throw new LayerDelaunayError(e.getMessage());
			}
		}		
	}



	@Override
	public void addPolygon(Polygon newPoly,boolean isEmpty) throws LayerDelaunayError {

		if(delaunayTool==null)
			delaunayTool=new MyMesh();
		try {
			//To avoid errors we set the Z coordinate to 0.
			SetZFilter zFilter = new SetZFilter();
			newPoly.apply(zFilter);
			if(newPoly.getNumInteriorRing()==0)
			{
				delaunayTool.addPolygon(new MyPolygon(newPoly,isEmpty));
			}else{
				GeometryFactory factory = new  GeometryFactory();
				Polygon extPoly=new Polygon(factory.createLinearRing(newPoly.getExteriorRing().getCoordinates()),null,factory);
				MyPolygon internalPoly=new MyPolygon(extPoly);
				internalPoly.setEmpty(isEmpty);
				internalPoly.setUsePolygonZ(false);
				delaunayTool.addPolygon(internalPoly);
				//Append the interior polygons
				final int holeCount = newPoly.getNumInteriorRing();
				for(int holeIndex= 0;holeIndex < holeCount;holeIndex++)
				{
					LineString holeLine=newPoly.getInteriorRingN(holeIndex);
					//Convert hole into a polygon
					Polygon polyBuffnew=factory.createPolygon(factory.createLinearRing(holeLine.getCoordinates()),null);
					MyPolygon internalHolePoly=new MyPolygon(polyBuffnew);
					internalHolePoly.setEmpty(!isEmpty);
					internalHolePoly.setUsePolygonZ(false);
					delaunayTool.addPolygon(internalHolePoly);
				}
			}
		} catch (DelaunayError e) {
			throw new LayerDelaunayError(e.getMessage());
		}
	}

	@Override
	public void setMinAngle(Double minAngle) {
		// TODO Auto-generated method stub
		if(delaunayTool!=null)
		{
			delaunayTool.setMinAngle(minAngle);
		}
	}



	@Override
	public void HintInit(Envelope bBox, long polygonCount,
			long verticesCount) throws LayerDelaunayError {
		if(delaunayTool==null)
			delaunayTool=new MyMesh();
		MyBox boundingBox=new MyBox(bBox.getMinX(),bBox.getMaxX(),bBox.getMinY(),bBox.getMaxY(),0.,0.);
		try {
			delaunayTool.init(boundingBox);
		} catch (DelaunayError e) {
			throw new LayerDelaunayError(e.getMessage());
		}
	}



	@Override
	public ArrayList<Coordinate> getVertices() throws LayerDelaunayError {
		return this.vertices;
	}



	@Override
	public ArrayList<Triangle> getTriangles() throws LayerDelaunayError {
		return this.triangles;
	}



	@Override
	public void addVertex(Coordinate vertexCoordinate)
			throws LayerDelaunayError {
		/*
		try {
			MyPoint newpt=new MyPoint(vertexCoordinate.x,vertexCoordinate.y,0.);
			newpt.setUseZ(false);
			this.ptToInsert.add(newpt);
		} catch (DelaunayError e) {
			throw new LayerDelaunayError(e.getMessage());
		}	
		*/	
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
		// TODO Auto-generated method stub
		
	}



}
