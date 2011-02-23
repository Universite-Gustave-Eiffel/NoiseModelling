/***********************************
 * ANR EvalPDU
 * Lcpc 30_08_2010
 * @author Nicolas Fortin
 ***********************************/

package lcpc_son;


import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.gdms.data.DataSource;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.ExecutionException;
import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.data.metadata.DefaultMetadata;
import org.gdms.data.metadata.Metadata;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.driver.DriverException;
import org.gdms.driver.ObjectDriver;
import org.gdms.driver.driverManager.DriverLoadException;
import org.gdms.sql.customQuery.CustomQuery;
import org.gdms.sql.customQuery.TableDefinition;
import org.gdms.sql.function.Argument;
import org.gdms.sql.function.Arguments;
import org.gdms.driver.DiskBufferDriver;
import org.grap.utilities.EnvelopeUtil;
import org.orbisgis.progress.IProgressMonitor;
import com.vividsolutions.jts.densify.Densifier;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.buffer.BufferParameters;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;

class NodeList { public final LinkedList< Coordinate > nodes=new LinkedList< Coordinate >(); }
/**
 * Set the right table row id to each left table rows from the nearest geometry, add also the column AvgDist corresponding to the average distance between the left and the right's nearest geometry found. -1 if nothing has been found in the region of the left geometry. 
 */

public class BR_TriGrid implements CustomQuery {

	private static Logger logger = Logger.getLogger(BR_TriGrid.class.getName());
	// _________  ^
	// | | | | |  | Y or J (bottom to top)
	// | | | | |
	// |_|_|_|_|
	// -> X or I (left to right)
	private static short nRight=0,nLeft=1,nBottom=2,nTop=3;			// neighbor relative positions index
	private static short[][] neighboor={{1,0},{-1,0},{0,-1},{0,1}}; // neighbor relative positions
	//Timing sum in millisec
	private long totalParseBuildings= 0;
	private long totalDelaunay= 0;
	
	int GetCellId(int row,int col,int cols)
	{
		return row*cols+col;
	}
	
	private Envelope GetGlobalEnvelope(DataSourceFactory dsf, DataSource[] tables, IProgressMonitor pm)  throws ExecutionException
	{
		//The region of interest is only where we can find sources
		//Then we keep only the region where the area is covered by sources
		Envelope mainEnvelope=new Envelope();

		final SpatialDataSourceDecorator sdsSource = new SpatialDataSourceDecorator(tables[1]);
		try {
			sdsSource.open();
			mainEnvelope  = sdsSource.getFullExtent();
			sdsSource.close();
		} catch (DriverException e) {
			throw new ExecutionException(e);
		}
		return mainEnvelope;

	}
	private void AddPolygon(Polygon newpoly,LayerDelaunay delaunayTool,Geometry boundingBox) throws DriverException, LayerDelaunayError
	{
		delaunayTool.addPolygon(newpoly,true);		
	}
	private void ExplodeAndAddPolygon(Geometry intersectedGeometry,LayerDelaunay delaunayTool,Geometry boundingBox) throws DriverException, LayerDelaunayError
	{
		long beginAppendPolygons=System.currentTimeMillis();
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
		totalDelaunay+=System.currentTimeMillis()-beginAppendPolygons;
	}
	
	private Geometry Merge(LinkedList<Geometry> toUnite, double bufferSize)
	{
		GeometryFactory geometryFactory = new GeometryFactory();
		Geometry geoArray[]=new Geometry[toUnite.size()];
		toUnite.toArray(geoArray);
		GeometryCollection polygonCollection = geometryFactory.createGeometryCollection(geoArray);
		return polygonCollection.buffer(bufferSize,0,BufferParameters.CAP_SQUARE);
	}
	private Envelope getCellEnv(Envelope mainEnvelope,int cellI,int cellJ,int cellIMax,int cellJMax,double cellWidth,double cellHeight)
	{
		return new Envelope(mainEnvelope.getMinX()+cellI*cellWidth, mainEnvelope.getMinX()+cellI*cellWidth+cellWidth, mainEnvelope.getMinY()+cellHeight*cellJ, mainEnvelope.getMinY()+cellHeight*cellJ+cellHeight);
	}
	private void FeedDelaunay(SpatialDataSourceDecorator polygonDatabase,LayerDelaunay delaunayTool,Envelope boundingBoxFilter,double srcDistance,LinkedList<LineString> delaunaySegments,double minRecDist,double srcPtDist ) throws  DriverException, LayerDelaunayError
	{
		Envelope extendedEnvelope=new Envelope(boundingBoxFilter);
		extendedEnvelope.expandBy(srcDistance*2.);
		long oldtotalDelaunay=totalDelaunay;
		long beginfeed=System.currentTimeMillis();
		Geometry linearRing=EnvelopeUtil.toGeometry(boundingBoxFilter);
		if ( !(linearRing instanceof LinearRing))
			return;
		GeometryFactory factory = new  GeometryFactory();
		Polygon boundingBox=new Polygon((LinearRing)linearRing, null, factory);

		//Insert the main rectangle
		delaunayTool.addPolygon(boundingBox, false);
		

		LinkedList<Geometry> toUnite = new LinkedList<Geometry>();
		final long rowCount = polygonDatabase.getRowCount();
		for (long rowIndex = 0; rowIndex < rowCount; rowIndex++)
		{
			final Geometry geometry = polygonDatabase.getGeometry(rowIndex);
			Envelope geomEnv=geometry.getEnvelopeInternal();
			geomEnv.expandBy(0.5);
			if (boundingBoxFilter.intersects(geomEnv)) 
			{
				//Add polygon to union array
				toUnite.add(geometry);
			}
		}
		//Merge buildings

		LinkedList<Geometry> toUniteRoads = new LinkedList<Geometry>();
		for(LineString road : delaunaySegments)
		{
			toUniteRoads.add(road);				
		}
		//Reduce small artifacts to avoid, shortest geometry to be over-triangulated
		LinkedList<Geometry> toUniteFinal= new LinkedList<Geometry>();
		if(!toUnite.isEmpty())
		{
			Geometry bufferBuildings=Merge(toUnite,0.5);
			//Remove small artifacts due to buildings buffer
			bufferBuildings=TopologyPreservingSimplifier.simplify(bufferBuildings, .1);
			//Densify receiver near buildings
			//bufferBuildings=Densifier.densify(bufferBuildings,srcPtDist);
			
			toUniteFinal.add(bufferBuildings);		//Add buildings to triangulation
		}
		
		if(!toUniteRoads.isEmpty() && minRecDist>0.01)
		{
			//Build Polygons buffer from roads lines
			Geometry bufferRoads = Merge(toUniteRoads,minRecDist);
			//Remove small artifacts due to multiple buffer crosses
			bufferRoads = TopologyPreservingSimplifier.simplify(bufferRoads, minRecDist/2);
			//Densify roads to set more receiver near roads.
			bufferRoads=Densifier.densify(bufferRoads,srcPtDist);
			toUniteFinal.add(bufferRoads);	//Merge roads with minRecDist m buffer
		}
		Geometry union=Merge(toUniteFinal,0.);	//Merge roads and buildings together
		//Remove geometries out of the bounding box
		union=union.intersection(boundingBox);
		ExplodeAndAddPolygon(union,delaunayTool,boundingBox);
		totalParseBuildings+=System.currentTimeMillis()-beginfeed-(totalDelaunay-oldtotalDelaunay);
	}
	private void computeSecondPassDelaunay(LayerExtTriangle cellMesh,Envelope mainEnvelope,int cellI,int cellJ,int cellIMax,int cellJMax,double cellWidth,double cellHeight,String firstPassResult,NodeList neighborsBorderVertices) throws LayerDelaunayError
	{
		 long beginDelaunay=System.currentTimeMillis();
		 cellMesh.loadInputDelaunay(firstPassResult);
		 File file=new File(firstPassResult);
		 file.delete();
		 if(neighborsBorderVertices!=null)
		 {
			 for(Coordinate neighCoord : neighborsBorderVertices.nodes)
			 {
				 cellMesh.addVertex(neighCoord);
			 }
		 }
		 cellMesh.setMinAngle(0.);
		 cellMesh.processDelaunay("second_",GetCellId(cellI, cellJ, cellJMax), -1, false, false);
		 if(neighborsBorderVertices!=null)
		 {
			 neighborsBorderVertices.nodes.clear();
		 }
		 totalDelaunay+=System.currentTimeMillis()-beginDelaunay;
	}
	
	/**
	 * Delaunay triangulation of Sub-Domain
	 * @param cellMesh
	 * @param mainEnvelope
	 * @param cellI
	 * @param cellJ
	 * @param cellIMax
	 * @param cellJMax
	 * @param cellWidth
	 * @param cellHeight
	 * @param maxSrcDist
	 * @param sds
	 * @param sdsSources
	 * @param minRecDist
	 * @param srcPtDist
	 * @param firstPassResults
	 * @param neighborsBorderVertices
	 * @param maximumArea
	 * @throws DriverException
	 * @throws LayerDelaunayError
	 */
	private void computeFirstPassDelaunay(LayerDelaunay cellMesh,Envelope mainEnvelope,int cellI,int cellJ,int cellIMax,int cellJMax,double cellWidth,double cellHeight,double maxSrcDist,SpatialDataSourceDecorator sds,SpatialDataSourceDecorator sdsSources,double minRecDist,double srcPtDist,String[] firstPassResults,NodeList[] neighborsBorderVertices,double maximumArea) throws DriverException, LayerDelaunayError
	{
		
		Envelope cellEnvelope=getCellEnv(mainEnvelope, cellI, cellJ, cellIMax, cellJMax, cellWidth, cellHeight);//new Envelope(mainEnvelope.getMinX()+cellI*cellWidth, mainEnvelope.getMinX()+cellI*cellWidth+cellWidth, mainEnvelope.getMinY()+cellHeight*cellJ, mainEnvelope.getMinY()+cellHeight*cellJ+cellHeight);
		Envelope expandedCellEnvelop=new Envelope(cellEnvelope);
		expandedCellEnvelop.expandBy(maxSrcDist);
		
		// Build delaunay triangulation from buildings inside the extended bounding box

		cellMesh.HintInit(cellEnvelope, 1500, 5000);
		///////////////////////////////////////////////////
		//Add roads into delaunay tool
		sds.open();
		sdsSources.open();
		long rowCount = sdsSources.getRowCount();
		final double firstPtAng=(Math.PI)/4.;
		final double secondPtAng=(Math.PI)-firstPtAng;
		final double thirdPtAng=Math.PI+firstPtAng;
		final double fourPtAng=-firstPtAng;
		LinkedList<LineString> delaunaySegments=new LinkedList<LineString>();
		for (long rowIndex = 0; rowIndex < rowCount; rowIndex++)
		{
			Geometry pt=sdsSources.getGeometry(rowIndex);
			Envelope ptEnv=pt.getEnvelopeInternal();
			if(ptEnv.intersects(expandedCellEnvelop))
			{
				if(pt instanceof Point )
				{
					Coordinate ptcoord=((Point)pt).getCoordinate();
					//Add 4 pts
					Coordinate pt1=new Coordinate(Math.cos(firstPtAng)*minRecDist+ptcoord.x, Math.sin(firstPtAng)*minRecDist+ptcoord.y);
					Coordinate pt2=new Coordinate(Math.cos(secondPtAng)*minRecDist*2+ptcoord.x, Math.sin(secondPtAng)*minRecDist*2+ptcoord.y);
					Coordinate pt3=new Coordinate(Math.cos(thirdPtAng)*minRecDist+ptcoord.x, Math.sin(thirdPtAng)*minRecDist+ptcoord.y);
					Coordinate pt4=new Coordinate(Math.cos(fourPtAng)*minRecDist*2+ptcoord.x, Math.sin(fourPtAng)*minRecDist*2+ptcoord.y);
					if(cellEnvelope.contains(pt1))
						cellMesh.addVertex(pt1);
					if(cellEnvelope.contains(pt2))
						cellMesh.addVertex(pt2);
					if(cellEnvelope.contains(pt3))
						cellMesh.addVertex(pt3);
					if(cellEnvelope.contains(pt4))
						cellMesh.addVertex(pt4);
				}else{

					if(pt instanceof LineString)
					{
						delaunaySegments.add((LineString)(pt));
					}else if(pt instanceof MultiLineString)
					{
						int nblinestring=((MultiLineString)pt).getNumGeometries();
						for(int idlinestring=0;idlinestring<nblinestring;idlinestring++)
						{
							delaunaySegments.add((LineString)(pt.getGeometryN(idlinestring)));
						}
					}
				}
			}						
		}
		FeedDelaunay(sds,cellMesh,cellEnvelope,maxSrcDist,delaunaySegments,minRecDist,srcPtDist);
		
		//Process delaunay
		
		long beginDelaunay=System.currentTimeMillis();
		logger.info("Begin delaunay");
		cellMesh.setMaxArea(maximumArea); // Maximum area
		//Maximum 5x steinerpt than input point, this limits avoid infinite loop, or memory consuming triangulation
		if(!(cellMesh instanceof LayerExtTriangle))
		{
			cellMesh.processDelaunay();
		}else{
			int maxSteiner=Math.max(cellMesh.getVertices().size()*5,50000);
			cellMesh.setMinAngle(20.);
			String firstPathFileName=((LayerExtTriangle)cellMesh).processDelaunay("first_",GetCellId(cellI, cellJ, cellJMax), maxSteiner, true, true);
			firstPassResults[GetCellId(cellI, cellJ, cellJMax)]=firstPathFileName;
			
			ArrayList<Coordinate> vertices = cellMesh.getVertices();
			boolean isLeft=cellI>0;
			boolean isRight=cellI<cellIMax-1;
			boolean isTop=cellJ<cellJMax-1;
			boolean isBottom=cellJ>0;
			int leftCellId=GetCellId(cellI+BR_TriGrid.neighboor[BR_TriGrid.nLeft][0], cellJ+BR_TriGrid.neighboor[BR_TriGrid.nLeft][1], cellJMax);
			int rightCellId=GetCellId(cellI+BR_TriGrid.neighboor[BR_TriGrid.nRight][0], cellJ+BR_TriGrid.neighboor[BR_TriGrid.nRight][1], cellJMax);
			int topCellId=GetCellId(cellI+BR_TriGrid.neighboor[BR_TriGrid.nTop][0], cellJ+BR_TriGrid.neighboor[BR_TriGrid.nTop][1], cellJMax);
			int bottomCellId=GetCellId(cellI+BR_TriGrid.neighboor[BR_TriGrid.nBottom][0], cellJ+BR_TriGrid.neighboor[BR_TriGrid.nBottom][1], cellJMax);
			//Initialization of cell array object
			Envelope leftEnv=null,rightEnv=null,topEnv=null,bottomEnv=null;
			if(isLeft)
			{
				if(neighborsBorderVertices[leftCellId]==null)
					neighborsBorderVertices[leftCellId]=new NodeList();
				leftEnv=this.getCellEnv(mainEnvelope, cellI+BR_TriGrid.neighboor[BR_TriGrid.nLeft][0], cellJ+BR_TriGrid.neighboor[BR_TriGrid.nLeft][1], cellIMax, cellJMax, cellWidth, cellHeight);
			}	
			if(isRight)
			{
				if(neighborsBorderVertices[rightCellId]==null)
					neighborsBorderVertices[rightCellId]=new NodeList();
				rightEnv=this.getCellEnv(mainEnvelope, cellI+BR_TriGrid.neighboor[BR_TriGrid.nRight][0], cellJ+BR_TriGrid.neighboor[BR_TriGrid.nRight][1], cellIMax, cellJMax, cellWidth, cellHeight);
			}
			if(isBottom)
			{
				if(neighborsBorderVertices[bottomCellId]==null)
					neighborsBorderVertices[bottomCellId]=new NodeList();
				bottomEnv=this.getCellEnv(mainEnvelope, cellI+BR_TriGrid.neighboor[BR_TriGrid.nBottom][0], cellJ+BR_TriGrid.neighboor[BR_TriGrid.nBottom][1], cellIMax, cellJMax, cellWidth, cellHeight);
			}
			if(isTop)
			{
				if(neighborsBorderVertices[topCellId]==null)
					neighborsBorderVertices[topCellId]=new NodeList();
				topEnv=this.getCellEnv(mainEnvelope, cellI+BR_TriGrid.neighboor[BR_TriGrid.nTop][0], cellJ+BR_TriGrid.neighboor[BR_TriGrid.nTop][1], cellIMax, cellJMax, cellWidth, cellHeight);
			}
			
			//Distribute border's vertices to neighbor second pass triangulation
			for(Coordinate vertex : vertices)
			{
				Envelope ptEnv=new Envelope(vertex);
				if(isLeft && leftEnv.distance(ptEnv)<0.0001) //leftEnv.intersects(vertex))
				{
					//Left
					//Translate to the exact position of the border
					vertex.x=leftEnv.getMaxX();
					neighborsBorderVertices[leftCellId].nodes.add(vertex);
				}else if(isRight && rightEnv.distance(ptEnv)<0.0001)
				{
					//Right
					vertex.x=rightEnv.getMinX();
					neighborsBorderVertices[rightCellId].nodes.add(vertex);
				}else if(isBottom && bottomEnv.distance(ptEnv)<0.0001)
				{
					//Bottom
					vertex.y=bottomEnv.getMaxY();
					neighborsBorderVertices[bottomCellId].nodes.add(vertex);
				}else if(isTop && topEnv.distance(ptEnv)<0.0001)
				{
					//Top
					vertex.y=topEnv.getMinY();
					neighborsBorderVertices[topCellId].nodes.add(vertex);
				}
			}
			
		}
		logger.info("End delaunay");
		totalDelaunay+=System.currentTimeMillis()-beginDelaunay;
		sdsSources.close();
		sds.close();
	}

	private Double DbaToW(Double dBA){
		return Math.pow(10.,dBA/10.);
	}
	
	public ObjectDriver evaluate(DataSourceFactory dsf, DataSource[] tables,
			Value[] values, IProgressMonitor pm) throws ExecutionException {
		String tmpdir=dsf.getTempDir().getAbsolutePath();
		String dbField = values[2].toString();
		double maxSrcDist = values[3].getAsDouble();
		int subdivLvl = values[4].getAsInt();
		double minRecDist = values[5].getAsDouble(); /*<! Minimum distance between source and receiver*/
		double srcPtDist = values[6].getAsDouble(); /*<! Complexity distance of roads */
		double maximumArea = values[7].getAsDouble();
		int reflexionOrder = values[8].getAsInt();
		int diffractionOrder = values[9].getAsInt();
		double wallAlpha = values[10].getAsDouble();
		boolean forceSinglePass=false;
		boolean doMultiThreading=true;
		
		try {
			// Steps of execution
			// Evaluation of the main bounding box (sources+buildings)
			// Split domain into 4^subdiv cells
			// For each cell :
			//  Expand bounding box cell by maxSrcDist
			// 	Build delaunay triangulation from buildings polygon processed by intersection with non extended bounding box
			//  Save the list of sources index inside the extended bounding box
			//  Save the list of buildings index inside the extended bounding box
			// 	Make a structure to keep the following information
			// 	Triangle list with the 3 vertices index
			// 	Vertices list (as receivers)
			// 	For each vertices within the cell bounding box (not the extended one)
			// 		Find all sources within maxSrcDist
			//		For All found sources
			//	      Test if there is a gap(no building) between source and receiver
			//        if not then append the distance attenuated sound level to the receiver
			//  Save the triangle geometry with the db_m value of the 3 vertices
			
			//1 Step - Evaluation of the main bounding box (sources)
			Envelope mainEnvelope=GetGlobalEnvelope(dsf,tables,pm);
			//Reduce by the distance of Sources distance
			mainEnvelope = new Envelope(mainEnvelope.getMinX()+maxSrcDist,mainEnvelope.getMaxX()-maxSrcDist,mainEnvelope.getMinY()+maxSrcDist,mainEnvelope.getMaxY()-maxSrcDist);
			// Split domain into 4^subdiv cells
			
			int gridDim=(int) Math.pow(2,subdivLvl);
			int tableBuildings=0;
			int tableSources=1;
			long nbreceivers=0;

			//Load Sources and Buildings table drivers
			final SpatialDataSourceDecorator sds = new SpatialDataSourceDecorator(tables[tableBuildings]);
			final SpatialDataSourceDecorator sdsSources = new SpatialDataSourceDecorator(tables[tableSources]);
			
			sdsSources.open();
			//Initialization frequency declared in source Table
			ArrayList<Integer> db_field_ids=new ArrayList<Integer>();
			ArrayList<Integer> db_field_freq=new ArrayList<Integer>();
			int fieldid=0;
			for(String fieldName  : sdsSources.getFieldNames())
			{
				if(fieldName.startsWith(dbField))
				{
					String sub=fieldName.substring(dbField.length());
					if(sub.length()>0)
					{
						int freq=Integer.parseInt(sub);
						db_field_ids.add(fieldid);
						db_field_freq.add(freq);
					}else{
						db_field_ids.add(fieldid);
						db_field_freq.add(0);
					}
				}
				fieldid++;
			}
			sdsSources.close();
			
			double cellWidth=mainEnvelope.getWidth()/gridDim;	
			double cellHeight=mainEnvelope.getHeight()/gridDim;	
			
			String[] firstPassResults= new String[gridDim*gridDim];
			NodeList[] neighborsBorderVertices=new NodeList[gridDim*gridDim];
			
			
			Type meta_type[]={TypeFactory.createType(Type.GEOMETRY),TypeFactory.createType(Type.FLOAT),TypeFactory.createType(Type.FLOAT),TypeFactory.createType(Type.FLOAT),TypeFactory.createType(Type.INT),TypeFactory.createType(Type.INT)};
			String meta_name[]={"the_geom","db_v1","db_v2","db_v3","cellid","triid"};
			DefaultMetadata metadata = new DefaultMetadata(meta_type,meta_name);
			DiskBufferDriver driver = new DiskBufferDriver(dsf,metadata );
			
			
			int nbcell=gridDim*gridDim;
			if(nbcell==1)
			{
				doMultiThreading=false;
				forceSinglePass=true;
			}
			
			Runtime runtime = Runtime.getRuntime();
			ThreadPool threadManager=new ThreadPool(runtime.availableProcessors(),runtime.availableProcessors()+1,Long.MAX_VALUE,TimeUnit.SECONDS);
			
			ProgressionOrbisGisManager pmManager=new ProgressionOrbisGisManager(nbcell, pm);
			Stack<ArrayList<Value>> toDriver=new Stack<ArrayList<Value>>();
			PropagationProcessDiskWriter driverManager=new PropagationProcessDiskWriter(toDriver, driver);
			driverManager.start();
			pmManager.start();
			PropagationProcessOut threadDataOut=new PropagationProcessOut(toDriver);

			for (int cellI = 0; cellI < gridDim; cellI++) {
				for (int cellJ = 0; cellJ < gridDim; cellJ++) {
					FastObstructionTest freeFieldFinder = new FastObstructionTest(tmpdir);
					int ij=cellI*gridDim+cellJ;
					logger.info("Begin processing of cell "+cellI+","+cellJ+" of the "+gridDim+"x"+gridDim+"  grid..");
					if (pm.isCancelled()) {
						driver.writingFinished();
						return driver;
					}			
					Envelope cellEnvelope=getCellEnv(mainEnvelope, cellI, cellJ, gridDim, gridDim, cellWidth, cellHeight);//new Envelope(mainEnvelope.getMinX()+cellI*cellWidth, mainEnvelope.getMinX()+cellI*cellWidth+cellWidth, mainEnvelope.getMinY()+cellHeight*cellJ, mainEnvelope.getMinY()+cellHeight*cellJ+cellHeight);
					Envelope expandedCellEnvelop=new Envelope(cellEnvelope);
					expandedCellEnvelop.expandBy(maxSrcDist*2.);
					// Build delaunay triangulation from buildings inside the extended bounding box

					


					////////////////////////////////////////////////////////
					// Make source index for optimization
					ArrayList<Geometry> sourceGeometries=new ArrayList<Geometry>();
					ArrayList<ArrayList<Double>> wj_sources=new ArrayList<ArrayList<Double>>();
					QueryGeometryStructure<Integer> sourcesIndex=new QueryGridIndex<Integer>(expandedCellEnvelop,64,64);
					//QueryGeometryStructure<Integer> sourcesIndex=new QueryQuadTree<Integer>();
					sdsSources.open();
					long rowCount = sdsSources.getRowCount();
					Integer idsource=0;
					for (long rowIndex = 0; rowIndex < rowCount; rowIndex++)
					{
						Value[] row=sdsSources.getRow(rowIndex);
						Geometry geo=sdsSources.getGeometry(rowIndex);
						Envelope ptEnv=geo.getEnvelopeInternal();
						if(ptEnv.intersects(expandedCellEnvelop))
						{
							sourcesIndex.AppendGeometry(geo, idsource);
							ArrayList<Double> wj_spectrum=new ArrayList<Double>();
							wj_spectrum.ensureCapacity(db_field_ids.size());
							for(Integer idcol : db_field_ids)
							{
								wj_spectrum.add(DbaToW(row[idcol].getAsDouble()));								
							}
							wj_sources.add(wj_spectrum);
							sourceGeometries.add(geo);
							idsource++;
						}
					}
					
					
					////////////////////////////////////////////////////////
					// feed freeFieldFinder for fast intersection query optimization
					
					sds.open();
					rowCount = sds.getRowCount();
					for (long rowIndex = 0; rowIndex < rowCount; rowIndex++)
					{
						final Geometry geometry = sds.getGeometry(rowIndex);
						Envelope geomEnv=geometry.getEnvelopeInternal();
						if (expandedCellEnvelop.intersects(geomEnv)) 
						{
							freeFieldFinder.AddGeometry(geometry);
						}
					}
					sds.close();
					freeFieldFinder.FinishPolygonFeeding(expandedCellEnvelop);
					
					//Compute the first pass delaunay mesh
					//The first pass doesn't take account of additional vertices of neighbor cells at the borders
					//then, there are discontinuities in iso surfaces at each border of cell
					LayerDelaunay cellMesh = new LayerExtTriangle(tmpdir);//new LayerCTriangle(); //new LayerJDelaunay();
				
					if(cellMesh instanceof LayerExtTriangle && !forceSinglePass)
					{
						for(short[] ijneighoffset : BR_TriGrid.neighboor)
						{
							int[] ijneigh={cellI+ijneighoffset[0],cellJ+ijneighoffset[1]};
							if(ijneigh[0]>=0 && ijneigh[0]<gridDim && ijneigh[1]>=0 && ijneigh[1]<gridDim)
							{
								if(firstPassResults[GetCellId(ijneigh[0],ijneigh[1],gridDim)]==null)
								{
									cellMesh.reset();
									computeFirstPassDelaunay(cellMesh,mainEnvelope,ijneigh[0],ijneigh[1],gridDim,gridDim,cellWidth,cellHeight,maxSrcDist,sds,sdsSources,minRecDist,srcPtDist,firstPassResults,neighborsBorderVertices,maximumArea);
								}
							}
						}
						//Compute the first pass of the 5 neighbor cells if this is not already done
						if(firstPassResults[GetCellId(cellI,cellJ,gridDim)]==null)
						{
							cellMesh.reset();
							computeFirstPassDelaunay(cellMesh,mainEnvelope,cellI,cellJ,gridDim,gridDim,cellWidth,cellHeight,maxSrcDist,sds,sdsSources,minRecDist,srcPtDist,firstPassResults,neighborsBorderVertices,maximumArea);
						}
						
						//Compute second pass of the current cell
						cellMesh.reset();
						computeSecondPassDelaunay((LayerExtTriangle)cellMesh,mainEnvelope,cellI,cellJ,gridDim,gridDim,cellWidth,cellHeight, firstPassResults[GetCellId(cellI,cellJ,gridDim)], neighborsBorderVertices[GetCellId(cellI,cellJ,gridDim)]);
					}else{
						computeFirstPassDelaunay(cellMesh,mainEnvelope,cellI,cellJ,gridDim,gridDim,cellWidth,cellHeight,maxSrcDist,sds,sdsSources,minRecDist,srcPtDist,firstPassResults,neighborsBorderVertices,maximumArea);
					}
					// 	Make a structure to keep the following information
					// 	Triangle list with 3 vertices(int), and 3 neighbor triangle ID
					// 	Vertices list
					
					//The evaluation of sound level must be done where the following vertices are
					ArrayList<Coordinate> vertices=cellMesh.getVertices();
					ArrayList<Triangle> triangles=cellMesh.getTriangles();
					nbreceivers+=vertices.size();
					PropagationProcessData threadData=new PropagationProcessData(vertices, triangles, freeFieldFinder, sourcesIndex, sourceGeometries, wj_sources, db_field_freq, reflexionOrder,diffractionOrder, maxSrcDist, minRecDist, wallAlpha, (long)ij,dsf,pmManager.NextSubProcess(vertices.size()));
					PropagationProcess propaProcess=new PropagationProcess(threadData, threadDataOut);
					
					if(doMultiThreading)
					{
						logger.info("Wait for free Thread to begin propagation of cell "+cellI+","+cellJ+" of the "+gridDim+"x"+gridDim+"  grid..");
						//threadManager.executeBlocking(propaProcess);
						while(!threadManager.isAvaibleQueueSlot())
						{
							if(pm.isCancelled())
							{
								driver.writingFinished();
								return driver;
							}
							Thread.sleep(100);
						}
						threadManager.execute(propaProcess);
						logger.info("Processing enqueued"); //enqueued
					}else{
						propaProcess.run();
					}
				}
			}
			//Wait termination of processes
			logger.info("Wait for termination of the lasts propagation process..");
			//threadManager.GetRemainingTasks()>0
			Thread.sleep(100);
			while(threadDataOut.getCellComputed()<nbcell && doMultiThreading)
			{
				if(pm.isCancelled())
				{
					driver.writingFinished();
					return driver;
				}
				Thread.sleep(100);
			}
			//Wait for rows stack to be empty
			driverManager.StopWatchingStack();
			pmManager.stop();
			logger.info("Wait for termination of writing to the driver..");
			while(driverManager.isRunning())
			{
				if(pm.isCancelled())
				{
					driver.writingFinished();
					return driver;
				}
				Thread.sleep(10);
			}
			driver.writingFinished();
			logger.info("Parse polygons time:" + this.totalParseBuildings + " ms");
			logger.info("Delaunay time:" + this.totalDelaunay + " ms");
			logger.info("Building source-receiver obstruction test time:" + threadDataOut.getTotalBuildingObstructionTest() + " ms");
			logger.info("Reflexion computing time:" + threadDataOut.getTotalReflexionTime() + " ms");
			logger.info("Quadtree query time:" + threadDataOut.getTotalGridIndexQuery());
			logger.info("Receiver count:" + nbreceivers);
			logger.info("Receiver-Source count:" + threadDataOut.getNb_couple_receiver_src());
			logger.info("Buildings obstruction test count:" + threadDataOut.getNb_obstr_test());
			return driver;
		} catch (DriverLoadException e) {
			throw new ExecutionException(e);
		} catch (DriverException e) {
			throw new ExecutionException(e);
		} catch (LayerDelaunayError e) {
			throw new ExecutionException(e);			
		} catch (InterruptedException e) {
			throw new ExecutionException(e);	
		}
	}

	
	public Metadata getMetadata(Metadata[] tables) throws DriverException {

		return new DefaultMetadata();
	}

	public Arguments[] getFunctionArguments() {
											  // Builds geom    , sources.the_geom, sources.db_m  ,max propa dist  , subdiv lev ,roads width     , receiv road dis,max tri area    ,sound refl o,sound dif order,wall alpha
		return new Arguments[] { new Arguments(Argument.GEOMETRY,Argument.GEOMETRY,Argument.STRING,Argument.NUMERIC,Argument.INT,Argument.NUMERIC,Argument.NUMERIC,Argument.NUMERIC,Argument.INT,Argument.INT   ,Argument.NUMERIC) };
	}
	public String getName() {
		return "BR_TriGrid";
	}

	public String getSqlOrder() {
		return "select BR_TriGrid( objects_table.the_geom, sound_sources_table.the_geom,sound_sources_table.db_m,50,3,2.5,5.0,300,1,0.1 ) from objects_table,sound_sources_table;";
	}

	public String getDescription() {
		return "BR_TriGrid(buildings(polygons),sources(points),sound lvl field name(string),maximum propagation distance (double meter),subdivision level 4^n cells(int), roads width (meter), densification of receivers near roads and buildings (meter), maximum area of triangle, sound reflection order, sound diffraction order, alpha of walls ) Sound propagation from ponctual sound sources to ponctual receivers created by a delaunay triangulation of specified buildings geometry.";
	}

	@Override
	public TableDefinition[] getTablesDefinitions() {
		return new TableDefinition[] { TableDefinition.GEOMETRY,TableDefinition.GEOMETRY };
	}

}