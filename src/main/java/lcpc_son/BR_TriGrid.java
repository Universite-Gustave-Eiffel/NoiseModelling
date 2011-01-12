/***********************************
 * ANR EvalPDU
 * Lcpc 30_08_2010
 * @author Nicolas Fortin
 ***********************************/
// TODO Calcul de correction de niveau sonore en fonction de la distance entre chaque source. Utiliser ce paramètre lors de la somme de chaque energie source
// +10log10(PasSource)
package lcpc_son;


import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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
import org.gdms.data.values.ValueFactory;
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

import com.vividsolutions.jts.algorithm.NonRobustLineIntersector;
import com.vividsolutions.jts.densify.Densifier;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import com.vividsolutions.jts.index.quadtree.Quadtree;
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
	private long totalBuildingObstructionTest = 0;
	private long totalQuadtreeQuery = 0;


	int GetCellId(int row,int col,int cols)
	{
		return row*cols+col;
	}
	
	private Double DbaToW(Double dBA){
		return Math.pow(10.,dBA/10.);
	}
	/*
	private Double WToDba(Double W){
		return 10*Math.log10(W);
	}
	*/
	/**
	 * Compute attenuation of sound energy by distance. Minimum distance is one meter.
	 * @param Wj Source level
	 * @param distance Distance in meter
	 * @return Attenuated sound level. Take only account of geometric dispersion of sound wave.
	 */
	private Double AttDistW(double Wj,double distance)
	{
		if(distance<1.) //No infinite sound level
			distance=1.;
		return Wj/(4*Math.PI*distance*distance);
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
	/**
	 * @param startPt Compute the closest point on lineString with this coordinate, use it as one of the splitted points
	 */
	private void SplitLineStringIntoPoints(Geometry geom,Coordinate startPt,LinkedList<Coordinate> pts,double minRecDist)
	{
		//Find the position of the closest point
		Coordinate[] points=geom.getCoordinates();
		//For each segments
		Double closestPtDist=Double.MAX_VALUE;
		Coordinate closestPt=null;
		for(int i=1;i<points.length;i++)
		{
			LineSegment seg=new LineSegment(points[i-1],points[i]);
			Coordinate SegClosest=seg.closestPoint(startPt);
			double segcdist=SegClosest.distance(startPt);
			if(segcdist<closestPtDist)
			{
				closestPtDist=segcdist;
				closestPt=SegClosest;
			}
		}	
		if(closestPt==null)
			return;
		double delta=20.;
		// If the minimum effective distance between the line source and the receiver is smaller than the minimum distance constraint then the discretisation parameter is changed
		// Delta must not not too small to avoid memory overhead.
		if(closestPtDist<minRecDist)
			closestPtDist=minRecDist;
		if(closestPtDist/2<delta)
			delta=closestPtDist/2;
		pts.add(closestPt);
		Coordinate[] splitedPts=ST_SplitLineInPoints.SplitMultiPointsInRegularPoints(points, delta);
		for(Coordinate pt : splitedPts)
		{
			pts.add(pt);
		}
		
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
			int maxSteiner=cellMesh.getVertices().size()*5;
			if(maxSteiner<10000)
				maxSteiner=10000;
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
	@SuppressWarnings("unchecked")
	private boolean QuadIsFreeField(Quadtree buildingsQuadtree,Coordinate receiverCoord,Coordinate srcCoord,final SpatialDataSourceDecorator sds) throws DriverException
	{
		GeometryFactory factory = new  GeometryFactory();
		Coordinate pverts[]= {receiverCoord,srcCoord};
		LineString freeFieldLine=factory.createLineString(pverts);
		Envelope regionIntersection=freeFieldLine.getEnvelopeInternal();
		regionIntersection.expandBy(1.); //expand by 1 meter
		long beginQuadQuery=System.currentTimeMillis();
	 	List<EnvelopeWithIndex<Long>> buildingsInRegion=buildingsQuadtree.query(regionIntersection);
	 	totalQuadtreeQuery+=(System.currentTimeMillis()-beginQuadQuery);
		for(EnvelopeWithIndex<Long> buildEnv : buildingsInRegion)
		{
			if(buildEnv.intersects(regionIntersection))
			{
				//Read the geometry
				Geometry building=sds.getGeometry(buildEnv.getId());
				if(building.intersects(freeFieldLine))
				{
					Geometry intersectsPts=building.intersection(freeFieldLine);
					if(intersectsPts.getNumPoints()>1)
					{
						// The building geometry intersect with the line string that is between the source and the receiver
						return false;
					}
				}
			}
		}
		return true;
	}
	/**
	 * Recursive method to feed mirrored receiver position on walls. No obstruction test is done.
	 * @param receiversImage Add receiver image here
	 * @param receiverCoord Receiver coordinate or precedent mirrored coordinate
	 * @param lastResult Last row index. -1 if first reflexion
	 * @param nearBuildingsWalls Walls to be reflected on
	 * @param depth Depth of reflection
	 */
	private void feedMirroredReceiverResults( ArrayList<MirrorReceiverResult> receiversImage, Coordinate receiverCoord, int lastResult,ArrayList<LineSegment> nearBuildingsWalls,int depth,double distanceLimitation)
	{
		//For each wall (except parent wall) compute the mirrored coordinate
		int exceptionWallId=-1;
		if(lastResult!=-1)
		{
			exceptionWallId=receiversImage.get(lastResult).getWallId();
		}
		int wallId=0;
		for(LineSegment wall : nearBuildingsWalls)
		{
			if(wallId!=exceptionWallId)
			{
				Coordinate intersectionPt=wall.project(receiverCoord);
				if(wall.distance(receiverCoord)<distanceLimitation) //Test maximum distance constraint
				{
					Coordinate mirrored=new Coordinate(2*intersectionPt.x-receiverCoord.x,2*intersectionPt.y-receiverCoord.y);
					receiversImage.add(new MirrorReceiverResult(mirrored, lastResult, wallId));
					if(depth>0)
					{
						feedMirroredReceiverResults(receiversImage, mirrored, receiversImage.size()-1, nearBuildingsWalls, depth-1,distanceLimitation);
					}
				}
			}
			wallId++;
		}
	}

	/**
	 * Compute all receiver position mirrored by specified segments
	 * @param receiverCoord Position of the original receiver
	 * @param nearBuildingsWalls Segments to mirror to
	 * @param order Order of reflections 1 to a limited number
	 * @return List of possible reflections
	 */
	 private ArrayList<MirrorReceiverResult> GetMirroredReceiverResults(Coordinate receiverCoord,ArrayList<LineSegment> nearBuildingsWalls,int order,double distanceLimitation)
	 {
		 ArrayList<MirrorReceiverResult> receiversImage=new ArrayList<MirrorReceiverResult>();
		 feedMirroredReceiverResults(receiversImage,receiverCoord,-1,nearBuildingsWalls,order-1,distanceLimitation);
		 return receiversImage;
	 }
	@SuppressWarnings("unchecked")
	public ObjectDriver evaluate(DataSourceFactory dsf, DataSource[] tables,
			Value[] values, IProgressMonitor pm) throws ExecutionException {
		String tmpdir=dsf.getTempDir().getAbsolutePath();
		String dbField = values[2].toString();
		boolean useFastObstructionTest=true;
		double maxSrcDist = values[3].getAsDouble();
		int subdivLvl = values[4].getAsInt();
		double minRecDist = values[5].getAsDouble(); /*<! Minimum distance between source and receiver*/
		double srcPtDist = values[6].getAsDouble(); /*<! Complexity distance of roads */
		double maximumArea = values[7].getAsDouble();
		int reflexionOrder = values[8].getAsInt();
		double wallAlpha = values[9].getAsDouble();
		boolean forceSinglePass=false;
		
		
		GeometryFactory factory = new  GeometryFactory();
		
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
			long nb_couple_receiver_src=0;
			long nb_obstr_test=0;
			
			double cellWidth=mainEnvelope.getWidth()/gridDim;	
			double cellHeight=mainEnvelope.getHeight()/gridDim;	
			
			String[] firstPassResults= new String[gridDim*gridDim];
			NodeList[] neighborsBorderVertices=new NodeList[gridDim*gridDim];
			Type meta_type[]={TypeFactory.createType(Type.GEOMETRY),TypeFactory.createType(Type.FLOAT),TypeFactory.createType(Type.FLOAT),TypeFactory.createType(Type.FLOAT),TypeFactory.createType(Type.INT),TypeFactory.createType(Type.INT)};
			String meta_name[]={"the_geom","db_v1","db_v2","db_v3","cellid","triid"};
			DefaultMetadata metadata = new DefaultMetadata(meta_type,meta_name);
			DiskBufferDriver driver = new DiskBufferDriver(dsf,metadata );
			
			int nbcell=gridDim*gridDim;
			for (int cellI = 0; cellI < gridDim; cellI++) {
				for (int cellJ = 0; cellJ < gridDim; cellJ++) {
					FastObstructionTest freeFieldFinder = new FastObstructionTest(tmpdir);
					int ij=cellI*gridDim+cellJ;
					logger.info("Begin processing of cell "+cellI+","+cellJ+" of the "+gridDim+"x"+gridDim+"  grid..");
					if (pm.isCancelled()) {
						driver.writingFinished();
						return driver;
					} else {
						pm.progressTo((int) (100*ij/nbcell));
					}			
					Envelope cellEnvelope=getCellEnv(mainEnvelope, cellI, cellJ, gridDim, gridDim, cellWidth, cellHeight);//new Envelope(mainEnvelope.getMinX()+cellI*cellWidth, mainEnvelope.getMinX()+cellI*cellWidth+cellWidth, mainEnvelope.getMinY()+cellHeight*cellJ, mainEnvelope.getMinY()+cellHeight*cellJ+cellHeight);
					Envelope expandedCellEnvelop=new Envelope(cellEnvelope);
					expandedCellEnvelop.expandBy(maxSrcDist*2.);
					// Build delaunay triangulation from buildings inside the extended bounding box

					
					final SpatialDataSourceDecorator sds = new SpatialDataSourceDecorator(tables[tableBuildings]);
					final SpatialDataSourceDecorator sdsSources = new SpatialDataSourceDecorator(tables[tableSources]);
					////////////////////////////////////////////////////////
					// Make source index for optimization
					GridIndex<Long> sourcesQuad=new GridIndex<Long>(expandedCellEnvelop,64,64);
					sdsSources.open();
					long rowCount = sdsSources.getRowCount();
					for (long rowIndex = 0; rowIndex < rowCount; rowIndex++)
					{
						Geometry pt=sdsSources.getGeometry(rowIndex);
						Envelope ptEnv=pt.getEnvelopeInternal();
						if(ptEnv.intersects(expandedCellEnvelop))
						{
							sourcesQuad.AppendGeometry(pt, rowIndex);
						}
					}
					
					
					////////////////////////////////////////////////////////
					// Make buildings QuadTree or feed freeFieldFinder for fast intersection query optimization
					
					Quadtree buildingsQuadtree=new Quadtree();
					sds.open();
					rowCount = sds.getRowCount();
					for (long rowIndex = 0; rowIndex < rowCount; rowIndex++)
					{
						final Geometry geometry = sds.getGeometry(rowIndex);
						Envelope geomEnv=geometry.getEnvelopeInternal();
						if (expandedCellEnvelop.intersects(geomEnv)) 
						{
							if(!useFastObstructionTest)
							{
								buildingsQuadtree.insert(geomEnv, new EnvelopeWithIndex<Long>(geomEnv, rowIndex));
							}else{
								freeFieldFinder.AddGeometry(geometry);									
							}
						}
					}
					if(useFastObstructionTest)
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

					double verticesSoundLevel[]=new double[vertices.size()];

					// For each vertices, find sources where the distance is within maxSrcDist meters
					int idReceiver=0;
					long propaPerc=System.currentTimeMillis();
					for(Coordinate receiverCoord : vertices)
					{
						nbreceivers++;
						int rayid=0;
						if(System.currentTimeMillis()-propaPerc>500)
						{
							logger.info("Sound propagation "+idReceiver+"/"+vertices.size());
							propaPerc=System.currentTimeMillis();
							if(pm.isCancelled())
							{
								driver.writingFinished();
								return driver;
							}
						}
						//List of walls within maxReceiverSource distance
						ArrayList<LineSegment> nearBuildingsWalls=null;
						ArrayList<MirrorReceiverResult> mirroredReceiver=null;
						if(reflexionOrder>0)
						{
							if(useFastObstructionTest)
							{
								nearBuildingsWalls=new ArrayList<LineSegment>(freeFieldFinder.GetLimitsInRange(maxSrcDist-1., receiverCoord));
							}else{
								LinkedList<LineSegment> tmpWalls=new LinkedList<LineSegment>();
								long beginQuadQuery=System.currentTimeMillis();
								Envelope maxBuildingsRegion=new Envelope(receiverCoord);
								maxBuildingsRegion.expandBy(maxSrcDist-1.);
							 	List<EnvelopeWithIndex<Long>> buildingsInRegion=buildingsQuadtree.query(maxBuildingsRegion);
							 	totalQuadtreeQuery+=(System.currentTimeMillis()-beginQuadQuery);
								for(EnvelopeWithIndex<Long> buildEnv : buildingsInRegion)
								{
									if(buildEnv.intersects(maxBuildingsRegion))
									{
										//Read the geometry
										Geometry building=sds.getGeometry(buildEnv.getId());
										//Append segments to neadBuildingWalls
										Coordinate[] buildsCoords=building.getCoordinates();
										for(int i=1;i<buildsCoords.length;i++)
										{
											tmpWalls.add(new LineSegment(buildsCoords[i-1],buildsCoords[i]));
										}
									}
								}
								nearBuildingsWalls=new ArrayList<LineSegment>(tmpWalls);
							}
							//Build mirrored receiver list from wall list
							mirroredReceiver=GetMirroredReceiverResults(receiverCoord,nearBuildingsWalls,reflexionOrder,maxSrcDist);						
						}
						double energeticSum=0;
						Envelope receiverRegion=new Envelope(receiverCoord.x-maxSrcDist,receiverCoord.x+maxSrcDist,receiverCoord.y-maxSrcDist,receiverCoord.y+maxSrcDist);
						long beginQuadQuery=System.currentTimeMillis();
						ArrayList<Long> regionSourcesLst=sourcesQuad.query(receiverRegion);
						totalQuadtreeQuery+=(System.currentTimeMillis()-beginQuadQuery);
						for(Long srcIndex : regionSourcesLst)
						{
							Geometry source=sdsSources.getGeometry(srcIndex);
							double Wj=DbaToW(sdsSources.getDouble(srcIndex,dbField ));
							LinkedList<Coordinate> srcPos=new LinkedList<Coordinate>();
							if(source instanceof Point)
							{
								srcPos.add(((Point)source).getCoordinate());									
							}else{
								//Discretization of line into multiple point
								//First point is the closest point of the LineString from the receiver
								SplitLineStringIntoPoints(source,receiverCoord,srcPos,minRecDist);
							}
							Coordinate lastSourceCoord=null;
							boolean lasthidingfound=false;
							nb_couple_receiver_src+=srcPos.size();
							for(final Coordinate srcCoord : srcPos)
							{
								double SrcReceiverDistance=srcCoord.distance(receiverCoord);
								if(SrcReceiverDistance<maxSrcDist)
								{							
									//Then, check if the source is visible from the receiver (not hidden by a building)
									//Create the direct Line
									long beginBuildingObstructionTest=System.currentTimeMillis();
									boolean somethingHideReceiver=false;

									if(lastSourceCoord!=null && lastSourceCoord.equals2D(srcCoord)) //If the srcPos is the same than the last one
									{
										somethingHideReceiver=lasthidingfound;											
									}else{		
										nb_obstr_test++;
										if(!useFastObstructionTest)
										{
											somethingHideReceiver=!QuadIsFreeField(buildingsQuadtree,receiverCoord, srcCoord,sds);
										}else{
											somethingHideReceiver=!freeFieldFinder.IsFreeField(receiverCoord, srcCoord);
										}
									}
									this.totalBuildingObstructionTest+=(System.currentTimeMillis()-beginBuildingObstructionTest);

									lastSourceCoord=srcCoord;
									lasthidingfound=somethingHideReceiver;
									if(!somethingHideReceiver)
									{
										//Evaluation of energy at receiver
										//add=wj/(4*pi*distance²)
										energeticSum+=AttDistW(Wj, SrcReceiverDistance);
										
									}
									//
									// Process specular reflection
									if(reflexionOrder>0)
									{
										NonRobustLineIntersector linters=new NonRobustLineIntersector();
										for( MirrorReceiverResult receiverReflection : mirroredReceiver)
										{
											double ReflectedSrcReceiverDistance=receiverReflection.getReceiverPos().distance(srcCoord);
											if(ReflectedSrcReceiverDistance<maxSrcDist)
											{
												boolean validReflection=false;
												int reflectionOrderCounter=0;
												MirrorReceiverResult receiverReflectionCursor=receiverReflection;
												//Test whether intersection point is on the wall segment or not
												Coordinate destinationPt=new Coordinate(srcCoord);
												LineSegment seg=nearBuildingsWalls.get(receiverReflection.getWallId());
												linters.computeIntersection(seg.p0, seg.p1, receiverReflection.getReceiverPos(),destinationPt);
												while(linters.hasIntersection()) //While there is a reflection point on another wall
												{
													reflectionOrderCounter++;
													//There are a probable reflection point on the segment
													Coordinate reflectionPt=new Coordinate(linters.getIntersection(0));
													//Translate reflection point by epsilon value to increase computation robustness
													Coordinate vec_epsilon=new Coordinate(reflectionPt.x-destinationPt.x,reflectionPt.y-destinationPt.y);
													double length=vec_epsilon.distance(new Coordinate(0.,0.,0.));
													//Normalize vector
													vec_epsilon.x/=length;
													vec_epsilon.y/=length;
													//Multiply by epsilon in meter
													vec_epsilon.x*=0.01;
													vec_epsilon.y*=0.01;
													//Translate reflection pt by epsilon to get outside the wall
													reflectionPt.x-=vec_epsilon.x;
													reflectionPt.y-=vec_epsilon.y;
													//Test if there is no obstacles between the reflection point and old reflection pt (or source position)
													nb_obstr_test++;
													if(!useFastObstructionTest)
													{
														validReflection=QuadIsFreeField(buildingsQuadtree,reflectionPt, destinationPt,sds);
													}else{
														validReflection=freeFieldFinder.IsFreeField(reflectionPt, destinationPt);
													}
													if(validReflection) //Reflection point can see source or its image
													{
														if(receiverReflectionCursor.getMirrorResultId()==-1)
														{   //Direct to the receiver
															nb_obstr_test++;
															if(!useFastObstructionTest)
															{
																validReflection=QuadIsFreeField(buildingsQuadtree,reflectionPt, receiverCoord,sds);
															}else{
																validReflection=freeFieldFinder.IsFreeField(reflectionPt, receiverCoord);
															}
															break; //That was the last reflection
														}else{
															//There is another reflection
															destinationPt.setCoordinate(reflectionPt);
															//Move reflection information cursor to a reflection closer 
															receiverReflectionCursor=mirroredReceiver.get(receiverReflectionCursor.getMirrorResultId());
															//Update intersection data
															seg=nearBuildingsWalls.get(receiverReflectionCursor.getWallId());
															linters.computeIntersection(seg.p0, seg.p1, receiverReflectionCursor.getReceiverPos(),destinationPt);
															validReflection=false;
														}
													}else{
														break;
													}
												}
												if(validReflection)
												{
													//A path has been found
													double geometricAtteuatedWj=AttDistW(Wj,ReflectedSrcReceiverDistance);
													//Apply wall material attenuation
													geometricAtteuatedWj*=Math.pow((1-wallAlpha),reflectionOrderCounter);
													energeticSum+=geometricAtteuatedWj;
												}
											}
											rayid++;
										}
									}
								}
								
							}
						}
						//Save the sound level at this receiver
						if(energeticSum<DbaToW(0.)) //If sound level<0dB, then set to 0dB
							energeticSum=DbaToW(0.);
						verticesSoundLevel[idReceiver]=energeticSum;
						idReceiver++;
					}
					sdsSources.close();
					sds.close();
					logger.info("Save cell's triangles..");
					//Now export all triangles with the sound level at each vertices
					int tri_id=0;
					for(Triangle tri : triangles)
					{
						Coordinate pverts[]= {vertices.get(tri.getA()),vertices.get(tri.getB()),vertices.get(tri.getC()),vertices.get(tri.getA())};
						final Value[] newValues = new Value[6];
						newValues[0]=ValueFactory.createValue(factory.createPolygon(factory.createLinearRing(pverts), null));
						newValues[1]=ValueFactory.createValue(verticesSoundLevel[tri.getA()]);
						newValues[2]=ValueFactory.createValue(verticesSoundLevel[tri.getB()]);
						newValues[3]=ValueFactory.createValue(verticesSoundLevel[tri.getC()]);
						newValues[4]=ValueFactory.createValue(ij);
						newValues[5]=ValueFactory.createValue(tri_id);
						driver.addValues(newValues);
						tri_id++;
					}
					logger.info("Cell's triangles saved..");
				}
			}
			driver.writingFinished();
			logger.info("Parse polygons time:" + this.totalParseBuildings);
			logger.info("Delaunay time:" + this.totalDelaunay);
			logger.info("Building source-receiver obstruction test time:" + this.totalBuildingObstructionTest);
			logger.info("Quadtree query time:" + totalQuadtreeQuery);
			logger.info("Receiver count:" + nbreceivers);
			logger.info("Receiver-Source count:" + nb_couple_receiver_src);
			logger.info("Buildings obstruction test count:" + nb_obstr_test);
			return driver;
		} catch (DriverLoadException e) {
			throw new ExecutionException(e);
		} catch (DriverException e) {
			throw new ExecutionException(e);
		} catch (LayerDelaunayError e) {
			throw new ExecutionException(e);			
		}
	}

	
	public Metadata getMetadata(Metadata[] tables) throws DriverException {

		return new DefaultMetadata();
	}

	public TableDefinition[] geTablesDefinitions() {
		return new TableDefinition[] { TableDefinition.GEOMETRY,TableDefinition.GEOMETRY };
	}

	public Arguments[] getFunctionArguments() {
		return new Arguments[] { new Arguments(Argument.GEOMETRY,Argument.GEOMETRY,Argument.STRING,Argument.NUMERIC,Argument.INT,Argument.NUMERIC,Argument.NUMERIC,Argument.NUMERIC,Argument.INT,Argument.NUMERIC) };
	}
	public String getName() {
		return "BR_TriGrid";
	}

	public String getSqlOrder() {
		return "select BR_TriGrid( objects_table.the_geom, sound_sources_table.the_geom,sound_sources_table.db_m,50,3,2.5,5.0,300,1,0.1 ) from objects_table,sound_sources_table;";
	}

	public String getDescription() {
		return "BR_TriGrid(buildings(polygons),sources(points),sound lvl(double),maximum propagation distance (double meter),subdivision level 4^n cells(int), roads width (meter), densification of receivers near roads and buildings (meter), maximum area of triangle, sound reflection order, alpha of walls ) Sound propagation from ponctual sound sources to ponctual receivers created by a delaunay triangulation of specified buildings geometry.";
	}

}