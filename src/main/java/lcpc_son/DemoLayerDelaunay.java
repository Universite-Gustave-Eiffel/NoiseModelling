package lcpc_son;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


import org.gdms.data.DataSource;
import org.gdms.data.DataSourceCreationException;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.driver.DriverException;
import org.gdms.driver.driverManager.DriverLoadException;
import org.grap.utilities.EnvelopeUtil;
import org.jdelaunay.delaunay.ConstraintPolygon;
import org.jdelaunay.delaunay.DelaunayError;
import org.jdelaunay.delaunay.MyDrawing;
import org.jdelaunay.delaunay.MyMesh;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFilter;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;

/**
 * Demo of JDelaunay library.
 * @author Adelin PIAU
 * @date 2010-07-27
 * @version 1.3
 */
class SetZFilter implements CoordinateSequenceFilter {
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
public class DemoLayerDelaunay {
	public static DataSourceFactory dsf = new DataSourceFactory();

	/*
	public static void AddRoadsPts(MyMesh aMesh) throws DriverLoadException, DataSourceCreationException, DriverException, DelaunayError
	{

		DataSource mydata;
		SpatialDataSourceDecorator sds;
		mydata = dsf.getDataSource(new File("/home/fortin/Modèles/roadspt.gdms"));
		sds = new SpatialDataSourceDecorator(mydata);
		sds.open();

		long max=sds.getRowCount();
		for (long i = 0; i < max; i++) 
		{
			Geometry geom=sds.getGeometry(i);
			for (int j = 0; j < geom.getNumGeometries(); j++)
			{
				Geometry subGeom = geom.getGeometryN(j);
				if (subGeom instanceof Point)
				{
					aMesh.addPoint(new MyPoint(((Point) subGeom).getCoordinate()));
				}
			}
		}
		// "/home/fortin/Modèles/roadspt.gdms"
		sds.close();
	}
*/

	public static void main(String[] args) throws DriverLoadException,
			DataSourceCreationException, DriverException, LayerDelaunayError,
			ParseException, IOException {
		for(int i=0;i<1;i++)
		{
			mainloop(args);
		}
	
	}
	public static void mainloop(String[] args) throws DriverLoadException,
			DataSourceCreationException, DriverException, LayerDelaunayError,
			ParseException, IOException {

		
	String help="Demo 1.3\n" +
			"Parameter : [-v] [-c <path level edges>] [-p <path buildings>]\n" +
			"-v : verbose\n" +
			"-e : level edges\n" +
			"-p : polygons\n" +
			"example :\n" +
			"-v -e courbes_niveaux.shp -p bati_extrait.shp";
		
		
		if(args.length==0)
		{
			System.out.println(help);
		}
		else
		{
	
			String path="", pathBuilding="";
			LayerDelaunay aMesh = new LayerCTriangle();
			boolean callhelp =false;
			
			if(args.length==2)
			{
				if(args[0].equals("-e"))
				{
					path = args[1];
				}
				else
				if(args[0].equals("-p"))
				{
					pathBuilding=args[1];
				}
				else
					callhelp=true;
			}
			else
			if(args.length>=3)
			{
				
				if(args[0].equals("-v"))
				{
					//aMesh.setVerbose(true);
				}
				else
					callhelp=true;
				
				if(args[1].equals("-e"))
				{
					path = args[2];
					
					if(args.length==5)
					{
						if(args[3].equals("-p"))
						{
							pathBuilding=args[4];
						}
						else
							callhelp=true;
					}
				}
				else
				if(args[1].equals("-p"))
				{
					pathBuilding=args[2];
				}
				else
					callhelp=true;
			}
			
			
			
			
			if(callhelp==true)
			{
				System.out.println(help);
			}
			else
			{
				
				long start = System.currentTimeMillis();
				
	
				DataSource mydata;
				SpatialDataSourceDecorator sds;
				
				if(!path.equals(""))// level edges
				{
	
					
					
					mydata = dsf.getDataSource(new File(path));	
					sds = new SpatialDataSourceDecorator(mydata);
					sds.open();
			
					
					Envelope env = sds.getFullExtent();

					aMesh.HintInit(env, sds.getRowCount(), sds.getRowCount()*5);
					
					int z;
					for (long i = 0; i < sds.getRowCount(); i++) {
						Geometry geom = sds.getGeometry(i);
						z = sds.getFieldValue(i, 2).getAsInt();
						for (int j = 0; j < geom.getNumGeometries(); j++) {
							Geometry subGeom = geom.getGeometryN(j);
							if (subGeom instanceof LineString) {
								Coordinate c1 = subGeom.getCoordinates()[0];
								Coordinate c2;
								c1.z = z;
								for (int k = 1; k < subGeom.getCoordinates().length; k++) {
									c2 = subGeom.getCoordinates()[k];
									c2.z = z;
									/*
									aMesh.addLevelEdge(new MyEdge(new MyPoint(c1),
											new MyPoint(c2)));
											*/
									c1 = c2;
								}
							}
						}
					}
			
					sds.close();
					
					// Uncomment it for adding polygons after triangularization.
					// (Don't forget to comment the same function after adding polygons!)

					//aMesh.refineMesh();
				}

				Envelope env=new Envelope();
				
				if(!pathBuilding.equals(""))// polygones
				{
					// adding polygons
					mydata = dsf.getDataSource(new File(pathBuilding));
					sds = new SpatialDataSourceDecorator(mydata);
					sds.open();
					
					
					if(path.equals(""))
					{
						env = sds.getFullExtent();
						aMesh.HintInit(env, sds.getRowCount(), sds.getRowCount()*5);
						Geometry linearRing=EnvelopeUtil.toGeometry(env);
						if ( !(linearRing instanceof LinearRing))
							return;
						GeometryFactory factory = new  GeometryFactory();
						Polygon boundingBox=new Polygon((LinearRing)linearRing, null, factory);

						//Insert the main rectangle
						aMesh.addPolygon(boundingBox, false);

					}
					System.out.println("\nadding polygon :\n");
					long max;
					max=sds.getRowCount();// can take more (or lot of more) than 2 minutes
					for (long i = 0; i < max; i++) {// can take more (or lot of more) than 2 minutes
						Geometry geom = sds.getGeometry(i);
						for (int j = 0; j < geom.getNumGeometries(); j++) {
							Geometry subGeom = geom.getGeometryN(j);
							if (subGeom instanceof Polygon) {
								SetZFilter zFilter = new SetZFilter();
								subGeom.apply(zFilter);
								//					aPolygon.setMustBeTriangulated(true);
								System.out.print("\r"+i+" / "+(max-1));
								//aMesh.addPolygon(aPolygon);
								aMesh.addPolygon((Polygon) subGeom, true);
							}
						}
					}
					sds.close();
					
					if(path.equals(""))
					{
						//Test
						
						//Add pts near roads					
						aMesh.processDelaunay();
						//aMesh.enforceQuality();
						//AddRoadsPts(aMesh);	
					}
				}
				
				
				// Uncomment it for triangulate polygons and level edge in the same time.
				// (Don't forget to comment the same function befor adding polygons!)
		//		aMesh.processDelaunay();
		
				//System.out.println("\npoint : "+aMesh.getNbPoints()+"\nedges : "+aMesh.getNbEdges()+"\ntriangles : "+aMesh.getNbTriangles());
				
				long end = System.currentTimeMillis();
				System.out.println("Duration " + (end-start)+"ms ==> ~ "+((end-start)/60000)+"min");
				
				//retrieve delaunay
				ArrayList<Triangle> triArray=aMesh.getTriangles();
				ArrayList<Coordinate> verts=aMesh.getVertices();
				MyMesh showMesh=new MyMesh();
				Envelope meshEnv=new Envelope();
				int lastshow=0;
				try {
					int tric=0;
					int maxtri=triArray.size();
					if(!verts.isEmpty())
					{
						meshEnv=new Envelope(verts.get(0));
						for(Triangle tri : triArray)
						{
							Coordinate a=verts.get(tri.getA());
							Coordinate b=verts.get(tri.getB());
							Coordinate c=verts.get(tri.getC());
							meshEnv.expandToInclude(a);
							meshEnv.expandToInclude(b);
							meshEnv.expandToInclude(c);
							/*
							showMesh.addLevelEdge(new MyPoint(a.x,a.y,0),new MyPoint(b.x,b.y,0));
							showMesh.addLevelEdge(new MyPoint(b.x,b.y,0),new MyPoint(c.x,c.y,0));
							showMesh.addLevelEdge(new MyPoint(c.x,c.y,0),new MyPoint(a.x,a.y,0));
							*/
							GeometryFactory factory=new GeometryFactory();
							Coordinate[] coordinates={a,b,c,a};
							Polygon tripoly=factory.createPolygon(factory.createLinearRing(coordinates), null);
							tripoly.apply(new SetZFilter());
							ConstraintPolygon newPoly=new ConstraintPolygon(tripoly);
							showMesh.addPolygon(newPoly);
							tric++;
							if((int)((float)tric/(float)maxtri*100) != lastshow)
							{
								System.out.println("Progression "+(int)((float)tric/(float)maxtri*100));
								lastshow=(int)((float)tric/(float)maxtri*100);
							}
						}
					}
				} catch (DelaunayError e) {
					e.printStackTrace();
				}
				//showMesh.setBoudingBox(meshEnv);
				MyDrawing aff2 = new MyDrawing();
				aff2.add(showMesh);
				showMesh.setAffiche(aff2);
				
				System.out.println("Save in Mesh.wrl ...");
				//aMesh.VRMLexport();// save mesh in Mesh.wrl
				
				System.out.println("Save in mesh*.gdms");
				//JdelaunayExport.exportGDMS(aMesh, "/home/fortin/mesh.gdms");
				
				
				System.out.println("Check triangularization...");
				//aMesh.checkTriangularization();
		
				
				end = System.currentTimeMillis();
				System.out.println("Duration " + (end-start)+"ms ==> ~ "+((end-start)/60000)+"min");
				System.out.println("Finish");
				
				
				
				
			}
		}
	}

}
