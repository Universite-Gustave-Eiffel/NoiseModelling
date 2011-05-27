package lcpcson;

import lcpcson.QueryGridIndex;
import lcpcson.EnvelopeWithIndex;
import lcpcson.LayerDelaunayError;
import lcpcson.LayerExtTriangle;
import lcpcson.Triangle;
import java.util.ArrayList;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;

public class GridIndexDebug {

	public static void main(String[] args) 
	{
		LayerExtTriangle delaun=new LayerExtTriangle("/home/fortin/OrbisGIS/temp");
				
		try {
			Envelope env=new Envelope(-10,30,-10,30);
			delaun.hintInit(env, 5, 10);
			delaun.setMaxArea(0.1);
			final Coordinate[] polycontour={new Coordinate(14,0,0),new Coordinate(23,0,0),new Coordinate(23,6,0),new Coordinate(14,6,0),new Coordinate(14,4,0),new Coordinate(21,4,0),new Coordinate(21,2,0),new Coordinate(14,2,0),new Coordinate(14,0,0)};
			GeometryFactory factory=new GeometryFactory();
			delaun.addPolygon(factory.createPolygon(factory.createLinearRing(polycontour), null), false);
			
			delaun.setMinAngle(20.);
			delaun.processDelaunay();
			
			ArrayList<Coordinate> verts=delaun.getVertices();
			
			ArrayList<Integer> testId=new ArrayList<Integer>();
			ArrayList<Coordinate> testCoord=new ArrayList<Coordinate>();
			int geoid=0;
			for(Triangle tri : delaun.getTriangles())
			{
				final Coordinate[] triCoords={verts.get(tri.getA()),verts.get(tri.getB()),verts.get(tri.getC()),verts.get(tri.getA())};
				Polygon newpoly=factory.createPolygon(factory.createLinearRing(triCoords), null);
				testId.add(geoid);
				testCoord.add(newpoly.getInteriorPoint().getCoordinate());				
				geoid++;
			}
			
			long feedStart=System.currentTimeMillis();
			QueryGridIndex<Integer> tool=new QueryGridIndex<Integer>(env, 64, 64);
			geoid=0;
			for(Triangle tri : delaun.getTriangles())
			{
				final Coordinate[] triCoords={verts.get(tri.getA()),verts.get(tri.getB()),verts.get(tri.getC()),verts.get(tri.getA())};
				Polygon newpoly=factory.createPolygon(factory.createLinearRing(triCoords), null);
				tool.AppendGeometry(newpoly,geoid);			
				geoid++;
			}
			System.out.println("GridIndex FeedTime :"+(System.currentTimeMillis()-feedStart)+" ms");
			long QuadfeedStart=System.currentTimeMillis();
			Quadtree quad=new Quadtree();
			geoid=0;
			for(Triangle tri : delaun.getTriangles())
			{
				final Coordinate[] triCoords={verts.get(tri.getA()),verts.get(tri.getB()),verts.get(tri.getC()),verts.get(tri.getA())};
				Polygon newpoly=factory.createPolygon(factory.createLinearRing(triCoords), null);
				quad.insert(newpoly.getEnvelopeInternal(), new EnvelopeWithIndex<Integer>(newpoly.getEnvelopeInternal(), geoid));
				geoid++;
			}
			System.out.println("Quad FeedTime :"+(System.currentTimeMillis()-QuadfeedStart)+" ms");
			//Launch a test for each point
			int meanResCount=0;
			int maxres=0;
			int minres=Integer.MAX_VALUE;
			long GridfeedStart=System.currentTimeMillis();
			for(Integer idGeo : testId)
			{
				Coordinate ptInside=testCoord.get(idGeo);
				Envelope envQuery=new Envelope(ptInside);
				envQuery.expandBy(0.01);
				ArrayList<Integer> geoIds=tool.query(envQuery);
				meanResCount+=geoIds.size();
				if(geoIds.size()>maxres)
					maxres=geoIds.size();
				if(geoIds.size()<minres)
					minres=geoIds.size();
				if(!geoIds.contains(idGeo))
				{
					System.err.println("Unable to retrieve idgeo :"+idGeo);
				}
			}
			System.out.println("Grid Query :"+(System.currentTimeMillis()-GridfeedStart)+" ms");
			long QuadqueryStart=System.currentTimeMillis();
			int quad_meanResCount=0;
			int quad_maxres=0;
			int quad_minres=Integer.MAX_VALUE;
			for(Integer idGeo : testId)
			{
				Coordinate ptInside=testCoord.get(idGeo);
				Envelope envQuery=new Envelope(ptInside);
				envQuery.expandBy(0.1);
				@SuppressWarnings("unchecked")
				ArrayList<EnvelopeWithIndex<Integer>> geoIds=(ArrayList<EnvelopeWithIndex<Integer>>)quad.query(envQuery);
				quad_meanResCount+=geoIds.size();
				if(geoIds.size()>quad_maxres)
					quad_maxres=geoIds.size();
				if(geoIds.size()<quad_minres)
					quad_minres=geoIds.size();
				ArrayList<Integer> ids=new ArrayList<Integer>();
				for(EnvelopeWithIndex<Integer> cid : geoIds)
				{
					ids.add(cid.getId());
				}
				if(!ids.contains(idGeo))
				{
					System.err.println("Unable to retrieve idgeo :"+idGeo);
				}
			}
			System.out.println("Quad Query :"+(System.currentTimeMillis()-QuadqueryStart)+"ms");
			System.out.println("Retrieve idgeo mean count :"+meanResCount/geoid);
			System.out.println("Retrieve idgeo min count :"+minres);
			System.out.println("Retrieve idgeo max count :"+maxres);
			System.out.println("Quad Retrieve idgeo mean count :"+quad_meanResCount/geoid);
			System.out.println("Quad Retrieve idgeo min count :"+quad_minres);
			System.out.println("Quad Retrieve idgeo max count :"+quad_maxres);
			System.out.println("Primitives count :"+geoid);
		} catch (LayerDelaunayError e) {

			e.printStackTrace();
		}
		
		
		 
	}
}
