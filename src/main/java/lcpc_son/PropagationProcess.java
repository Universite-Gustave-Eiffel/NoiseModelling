package lcpc_son;
/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

import java.util.ArrayList;
import java.util.LinkedList;
/*
import org.gdms.data.metadata.DefaultMetadata;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.driver.DiskBufferDriver;
import org.gdms.driver.DriverException;
*/

import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;

import com.vividsolutions.jts.algorithm.NonRobustLineIntersector;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.quadtree.Quadtree;

public class PropagationProcess implements Runnable {
	  private Thread thread;
	  private PropagationProcessData data;
	  private PropagationProcessOut dataOut;
		/**
		 * Recursive method to feed mirrored receiver position on walls. No obstruction test is done.
		 * @param receiversImage Add receiver image here
		 * @param receiverCoord Receiver coordinate or precedent mirrored coordinate
		 * @param lastResult Last row index. -1 if first reflexion
		 * @param nearBuildingsWalls Walls to be reflected on
		 * @param depth Depth of reflection
		 * TODO Use segment
		 *  orientation to filter wall list
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
	  public PropagationProcess(PropagationProcessData data,PropagationProcessOut dataOut){
	    thread = new Thread(this);
	    this.dataOut=dataOut;
	    this.data=data;
	  }
	  public void start(){
	    thread.start();
	  }
	  public void join(){
	     try {
	      thread.join();
	    } catch (Exception e) {
	    	return;
	    }
	  }
	private double DbaToW(double dBA){
		return Math.pow(10.,dBA/10.);
	}
	private double WToDba(double w){
		return 10*Math.log10(w);
	}
	/**
	 * @param startPt Compute the closest point on lineString with this coordinate, use it as one of the splitted points
	 * @return computed delta
	 */
	private double SplitLineStringIntoPoints(Geometry geom,Coordinate startPt,LinkedList<Coordinate> pts,double minRecDist)
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
			return 1.;
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
			if(pt.distance(closestPt)>delta)
				pts.add(pt);
		}
		return delta;
	}
	/**
	 * ISO-9613 p1 - At 15°C 70% humidity 
	 * @param freq Third octave frequency
	 * @return Attenuation coefficient dB/KM
	 */
	private static double GetAlpha(int freq)
	{
		switch(freq)
		{
			case 100:
				return 0.25;
			case 125:
				return 0.38;
			case 160:
				return 0.57;
			case 200:
				return 0.82;
			case 250:
				return 1.13;
			case 315:
				return 1.51;
			case 400:
				return 1.92;
			case 500:
				return 2.36;
			case 630:
				return 2.84;
			case 800:
				return 3.38;
			case 1000:
				return 4.08;
			case 1250:
				return 5.05;
			case 1600:
				return 6.51;
			case 2000:
				return 8.75;
			case 2500:
				return 12.2;
			case 3150:
				return 17.7;
			case 4000:
				return 26.4;
			case 5000:
				return 39.9;
			default:
				return 0.;
		}
	}
	private int NextFreeFieldNode(ArrayList<Coordinate> nodes,Coordinate startPt,ArrayList<Integer> NodeExceptions,int firstTestNode,FastObstructionTest freeFieldFinder)
	{
		int validNode=firstTestNode;
		while(NodeExceptions.contains(validNode) || (validNode<nodes.size() && !freeFieldFinder.IsFreeField(startPt, nodes.get(validNode))))
		{
			validNode++;
		}
		if(validNode>=nodes.size())
			return -1;		
		return validNode;		
	}
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

	/**
	 * Source-Receiver Direct+Reflection+Diffraction computation
	 * @param[in] srcCoord Coordinate of source
	 * @param[in] receiverCoord Coordinate of receiver
	 * @param[out] energeticSum Energy by frequency band 
	 * @param[in] alpha_atmo Atmospheric absorption by frequency band
	 * @param[in] wj Source sound pressure level dB(A) by frequency band
	 * @param[in] li Coefficient, distance between source discretization
	 * @param[in] mirroredReceiver Receivers mirrored by walls (for reflection)  
	 * @param[in] nearBuildingsWalls Walls within maxsrcdist
	 * @param[in] regionCorners Corners within maxsrcdist
	 * @param[in] regionCornersFreeToReceiver List of index of corners visible from receiver
	 * @param[in] freq_lambda Array of sound wave lambda value by frequency band
	 */
	public void ReceiverSourcePropa(Coordinate srcCoord,Coordinate receiverCoord,double energeticSum[],double[] alpha_atmo,ArrayList<Double> wj,double li,ArrayList<MirrorReceiverResult> mirroredReceiver,ArrayList<LineSegment> nearBuildingsWalls,ArrayList<Coordinate> regionCorners,ArrayList<Integer> regionCornersFreeToReceiver,double[] freq_lambda)
	{
		int nbfreq=data.freq_lvl.size();
		int nb_obstr_test=0;
		double SrcReceiverDistance=srcCoord.distance(receiverCoord);
		if(SrcReceiverDistance<data.maxSrcDist)
		{							
			//Then, check if the source is visible from the receiver (not hidden by a building)
			//Create the direct Line
			long beginBuildingObstructionTest=System.currentTimeMillis();
			boolean somethingHideReceiver=false;
			nb_obstr_test++;
			somethingHideReceiver=!data.freeFieldFinder.IsFreeField(receiverCoord, srcCoord);
			dataOut.appendObstructionTestQueryTime((System.currentTimeMillis()-beginBuildingObstructionTest));
			if(!somethingHideReceiver)
			{
				//Evaluation of energy at receiver
				//add=wj/(4*pi*distance²)
				for(int idfreq=0;idfreq<nbfreq;idfreq++)
				{
					double AttenuatedWj=AttDistW(wj.get(idfreq), SrcReceiverDistance);
					AttenuatedWj=DbaToW(WToDba(AttAtmW(AttenuatedWj, SrcReceiverDistance, alpha_atmo[idfreq]))+10*Math.log10(li));
					energeticSum[idfreq]+=AttenuatedWj;
				}
				
			}
			//
			// Process specular reflection
			if(data.reflexionOrder>0)
			{
				long beginReflexionTest=System.currentTimeMillis();
				NonRobustLineIntersector linters=new NonRobustLineIntersector();
				for( MirrorReceiverResult receiverReflection : mirroredReceiver)
				{
					double ReflectedSrcReceiverDistance=receiverReflection.getReceiverPos().distance(srcCoord);
					if(ReflectedSrcReceiverDistance<data.maxSrcDist)
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
							validReflection=data.freeFieldFinder.IsFreeField(reflectionPt, destinationPt);
							if(validReflection) //Reflection point can see source or its image
							{
								if(receiverReflectionCursor.getMirrorResultId()==-1)
								{   //Direct to the receiver
									nb_obstr_test++;
									validReflection=data.freeFieldFinder.IsFreeField(reflectionPt, receiverCoord);
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
							for(int idfreq=0;idfreq<nbfreq;idfreq++)
							{
								//Geometric dispersion
								double AttenuatedWj=AttDistW(wj.get(idfreq),ReflectedSrcReceiverDistance);
								//Apply wall material attenuation
								AttenuatedWj*=Math.pow((1-data.wallAlpha),reflectionOrderCounter);
								//Apply atmospheric absorption and ground 
								//AttenuatedWj=DbaToW(WToDba(AttenuatedWj)-(alpha_atmo[idfreq]*ReflectedSrcReceiverDistance)/1000.+10*Math.log10(li));
								AttenuatedWj=DbaToW(WToDba(AttAtmW(AttenuatedWj, ReflectedSrcReceiverDistance, alpha_atmo[idfreq]))+10*Math.log10(li));
								energeticSum[idfreq]+=AttenuatedWj;
							}
						}
					}
				}
				dataOut.appendTotalReflexionTime((System.currentTimeMillis()-beginReflexionTest));
				
			} //End reflexion
			/////////////
			// Process diffraction paths
			if(data.diffractionOrder>0 && regionCornersFreeToReceiver.size()>0)
			{
				//Find the first valid source->corner
				int receiverFreeCornerIndex=0;
				int firstCorner=regionCornersFreeToReceiver.get(receiverFreeCornerIndex);
				if(firstCorner!=-1)
				{
					//History of propagation through corners 
					ArrayList<Integer> curCorner=new ArrayList<Integer>();
					curCorner.add(firstCorner);
					while(!curCorner.isEmpty())
					{
						Coordinate lastCorner=regionCorners.get(curCorner.get(curCorner.size()-1));
						//Test Path is free to the source
						if(data.freeFieldFinder.IsFreeField(lastCorner, srcCoord))
						{
							//True then the path is clear
							//Compute attenuation level
							double elength=0;
							for(int ie=1;ie<curCorner.size();ie++)
							{
								elength+=regionCorners.get(curCorner.get(ie-1)).distance(regionCorners.get(curCorner.get(ie)));
							}
							//delta=SO^1+O^nO^(n+1)+O^nnR
							double diffractionFullDistance=srcCoord.distance(regionCorners.get(curCorner.get(0)))+elength+srcCoord.distance(regionCorners.get(curCorner.get(curCorner.size()-1)));
							if(diffractionFullDistance<data.maxSrcDist)
							{
								double delta=diffractionFullDistance-SrcReceiverDistance;
								//Negative if free field
								if(!somethingHideReceiver)
									delta=-delta;
								
								
									
								//double largeAtt=0;//TODO remove
								for(int idfreq=0;idfreq<nbfreq;idfreq++)
								{
									
									double cprime;
									if(curCorner.size()==1)
									{
										cprime=1;
									}else{
										cprime=(75*Math.pow(freq_lambda[idfreq],2)+3*Math.pow(elength,2))/(75*Math.pow(freq_lambda[idfreq],2)+Math.pow(elength,2));
									}
									double testForm=(40/freq_lambda[idfreq])*cprime*delta;
									double DiffractionAttenuation=0;
									if(testForm>=-2)
										DiffractionAttenuation=10*Math.log10(3+testForm);
									//Limit to 0<=DiffractionAttenuation<=25
									DiffractionAttenuation=Math.max(0, DiffractionAttenuation);
									//largeAtt+=DbaToW(DiffractionAttenuation);
									//Geometric dispersion
									double AttenuatedWj=AttDistW(wj.get(idfreq),SrcReceiverDistance);
									//Apply diffraction attenuation
									AttenuatedWj=DbaToW(WToDba(AttenuatedWj)-DiffractionAttenuation);
									//Apply atmospheric absorption and ground 
									//AttenuatedWj=DbaToW(WToDba(AttenuatedWj)-(alpha_atmo[idfreq]*ReflectedSrcReceiverDistance)/1000.+10*Math.log10(li));
									AttenuatedWj=DbaToW(WToDba(AttAtmW(AttenuatedWj, diffractionFullDistance, alpha_atmo[idfreq]))+10*Math.log10(li));
									energeticSum[idfreq]+=AttenuatedWj;
								}
								//TODO removing
								/*
								if(somethingHideReceiver)
								{
									Coordinate[] coordinates=new Coordinate[2+curCorner.size()];
									coordinates[0]=receiverCoord;
									int idvertex=1;
									for(int idcorner : curCorner)
									{
										coordinates[idvertex]=regionCorners.get(idcorner);
										idvertex++;
									}
									coordinates[coordinates.length-1]=srcCoord;
									Value[] row=new Value[3];
									row[0]=ValueFactory.createValue(factory.createLineString(coordinates));
									row[1]=ValueFactory.createValue(idReceiver);
									row[2]=ValueFactory.createValue(WToDba(largeAtt));
									diffId++;
									try {
										driver.addValues(row);
									} catch (DriverException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
										return;
									}
								}
								//END REMOVING
								 */
							}
						}
						//Process to the next corner
						int nextCorner=-1;
						if(data.diffractionOrder>curCorner.size()){
							//Continue to next order valid corner
							nextCorner=NextFreeFieldNode(regionCorners,lastCorner,curCorner,0,data.freeFieldFinder);
							if(nextCorner!=-1)
							{
								curCorner.add(nextCorner);
							}
						}
						while(nextCorner==-1 && !curCorner.isEmpty())
						{
							if(curCorner.size()>1)
							{
								//Next free field corner
								nextCorner=NextFreeFieldNode(regionCorners,regionCorners.get(curCorner.get(curCorner.size()-2)),curCorner,curCorner.get(curCorner.size()-1),data.freeFieldFinder);
							}else{
								//Next receiver-corner tuple
								receiverFreeCornerIndex++;
								if(receiverFreeCornerIndex<regionCornersFreeToReceiver.size())
								{
									nextCorner=regionCornersFreeToReceiver.get(receiverFreeCornerIndex);
								}else{
									nextCorner=-1;
								}
							}
							if(nextCorner!=-1)
							{
								curCorner.set(curCorner.size()-1, nextCorner);
							}else{
								curCorner.remove(curCorner.size()-1);
							}	
						}								
					}
				}
			}
		}
	}
	
	/**
	 * Compute the attenuation of atmospheric absorption
	 * @param Wj Source energy
	 * @param dist Propagation distance
	 * @param alpha_atmo Atmospheric alpha (dB/km)
	 * @return
	 */
	private Double AttAtmW(double Wj,double dist,double alpha_atmo)
	{
		return DbaToW(WToDba(Wj)-(alpha_atmo*dist)/1000.);
	}
	@Override
	public void run() {
		
		GeometryFactory factory=new GeometryFactory();
		
		//TODO comment debugging code
		/*
		int diffId=0;
		Type meta_type[]={TypeFactory.createType(Type.GEOMETRY),TypeFactory.createType(Type.INT),TypeFactory.createType(Type.DOUBLE)};
		String meta_name[]={"the_geom","difid","largebandatt"};
		DefaultMetadata metadata = new DefaultMetadata(meta_type,meta_name);
		DiskBufferDriver driver;
		try {
			driver = new DiskBufferDriver(data.dsf,metadata );
		} catch (DriverException e) {
			e.printStackTrace();
			return;
		}
		*/
		
		long nb_obstr_test=0;
		double verticesSoundLevel[]=new double[data.vertices.size()];				//Computed sound level of vertices

		int nbfreq=data.freq_lvl.size();
		//Init wave length for each frequency
		double cel=344.23935;
		double[] freq_lambda=new double[nbfreq];
		for(int idf=0;idf<nbfreq;idf++)
		{
			if(data.freq_lvl.get(idf)>0)
			{
				freq_lambda[idf]=cel/data.freq_lvl.get(idf);
			}else{
				freq_lambda[idf]=1;
			}
		}
		//Compute atmospheric alpha value by specified frequency band
		double[] alpha_atmo=new double[data.freq_lvl.size()];
		for(int idfreq=0;idfreq<nbfreq;idfreq++)
			alpha_atmo[idfreq]=GetAlpha(data.freq_lvl.get(idfreq));
		///////////////////////////////////////////////
		//Search diffraction corners
		Quadtree cornersQuad=new Quadtree();
		if(data.diffractionOrder>0)
		{
			ArrayList<Coordinate> corners=data.freeFieldFinder.GetWideAnglePoints(Math.PI*(1+1/16.0), Math.PI*(2-(1/16.)));
			//Build Quadtree
			for(Coordinate corner : corners)
			{
				cornersQuad.insert(new Envelope(corner), corner);
			}
		}
		// For each vertices, find sources where the distance is within maxSrcDist meters
		ProgressionProcess propaProcessProgression=data.cellProg;
		int idReceiver=0;
		for(Coordinate receiverCoord : data.vertices)
		{
			propaProcessProgression.nextSubProcessEnd();
			//List of walls within maxReceiverSource distance
			ArrayList<LineSegment> nearBuildingsWalls=null;
			ArrayList<MirrorReceiverResult> mirroredReceiver=null;
			Envelope receiverRegion=new Envelope(receiverCoord.x-data.maxSrcDist,receiverCoord.x+data.maxSrcDist,receiverCoord.y-data.maxSrcDist,receiverCoord.y+data.maxSrcDist);
			if(data.reflexionOrder>0)
			{

				nearBuildingsWalls=new ArrayList<LineSegment>(data.freeFieldFinder.GetLimitsInRange(data.maxSrcDist-1., receiverCoord));
				//Build mirrored receiver list from wall list
				mirroredReceiver=GetMirroredReceiverResults(receiverCoord,nearBuildingsWalls,data.reflexionOrder,data.maxSrcDist);						
			}
			ArrayList<Coordinate> regionCorners=new ArrayList<Coordinate>();
			ArrayList<Integer> regionCornersFreeToReceiver=new ArrayList<Integer>(); //Corners free field with receiver
			if(data.diffractionOrder>0)
			{
				//Query corners in the current zone
				ArrayCoordinateListVisitor cornerQuery=new ArrayCoordinateListVisitor(receiverCoord, data.maxSrcDist);
				cornersQuad.query(receiverRegion, cornerQuery);
				regionCorners=cornerQuery.getItems();
				regionCornersFreeToReceiver.ensureCapacity(regionCorners.size());
				for(int icorner=0;icorner<regionCorners.size();icorner++)
				{
					if(data.freeFieldFinder.IsFreeField(receiverCoord, regionCorners.get(icorner)))
					{
						regionCornersFreeToReceiver.add(icorner);
					}
				}
			}
			double energeticSum[]=new double[data.freq_lvl.size()];
			for(int idfreq=0;idfreq<nbfreq;idfreq++)
				energeticSum[idfreq]=0.0;
			
			long beginQuadQuery=System.currentTimeMillis();
			ArrayList<Integer> regionSourcesLst=data.sourcesIndex.query(receiverRegion);
			dataOut.appendGridIndexQueryTime((System.currentTimeMillis()-beginQuadQuery));
			for(Integer srcIndex : regionSourcesLst)
			{
				Geometry source=data.sourceGeometries.get(srcIndex);
				ArrayList<Double> wj=data.wj_sources.get(srcIndex); //DbaToW(sdsSources.getDouble(srcIndex,dbField ));
				LinkedList<Coordinate> srcPos=new LinkedList<Coordinate>();
				double li=0.;
				if(source instanceof Point)
				{
					Coordinate ptpos=((Point)source).getCoordinate();
					srcPos.add(ptpos);
					//li=Math.min(Math.max(receiverCoord.distance(ptpos),data.minRecDist)/2.,20.0);
					li=1;
					//Compute li to equation  4.1 NMPB 2008 (June 2009)
					//wj+=10*Math.log10(li);
				}else{
					//Discretization of line into multiple point
					//First point is the closest point of the LineString from the receiver
					li=SplitLineStringIntoPoints(source,receiverCoord,srcPos,data.minRecDist);
					//Compute li to equation  4.1 NMPB 2008 (June 2009)
				
				}
				dataOut.appendSourceCount(srcPos.size());
				for(final Coordinate srcCoord : srcPos)
				{
					//For each Pt Source - Pt Receiver
					ReceiverSourcePropa(srcCoord,receiverCoord,energeticSum,alpha_atmo,wj,li,mirroredReceiver,nearBuildingsWalls,regionCorners,regionCornersFreeToReceiver,freq_lambda);
				}
			}
			//Save the sound level at this receiver
			//Do the sum of all frequency bands
			double allfreqlvl=0;
			for(int idfreq=0;idfreq<nbfreq;idfreq++)
			{
				allfreqlvl+=energeticSum[idfreq];
			}
			if(allfreqlvl<DbaToW(0.)) //If sound level<0dB, then set to 0dB
				allfreqlvl=DbaToW(0.);
			verticesSoundLevel[idReceiver]=allfreqlvl;
			idReceiver++;
		}
		//Subdivide each triangle, and apply BiCubic interpolation.
		/*
		ArrayList<Triangle> bicubictri=new ArrayList<Triangle>();
		bicubictri.ensureCapacity(data.triangles.size());
		for(Triangle tri : data.triangles)
		{
			////////////////////////
			//Find the fourth vertex
		}
		*/
		//Now export all triangles with the sound level at each vertices
		int tri_id=0;
		for(Triangle tri : data.triangles)
		{
			Coordinate pverts[]= {data.vertices.get(tri.getA()),data.vertices.get(tri.getB()),data.vertices.get(tri.getC()),data.vertices.get(tri.getA())};
			final Value[] newValues = new Value[6];
			newValues[0]=ValueFactory.createValue(factory.createPolygon(factory.createLinearRing(pverts), null));
			newValues[1]=ValueFactory.createValue(verticesSoundLevel[tri.getA()]);
			newValues[2]=ValueFactory.createValue(verticesSoundLevel[tri.getB()]);
			newValues[3]=ValueFactory.createValue(verticesSoundLevel[tri.getC()]);
			newValues[4]=ValueFactory.createValue(data.cellId);
			newValues[5]=ValueFactory.createValue(tri_id);
			dataOut.addValues(newValues);
			tri_id++;
		}
		dataOut.appendFreeFieldTestCount(nb_obstr_test);
		dataOut.appendCellComputed();
		/*
		try {
			driver.writingFinished();
		} catch (DriverException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
	}
	
}
