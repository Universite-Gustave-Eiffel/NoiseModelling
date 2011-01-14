package lcpc_son;

import java.util.ArrayList;

import com.vividsolutions.jts.algorithm.NonRobustLineIntersector;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;

/**
 * Debug class for tri grid
 * @author fortin
 *
 */
public class TriGridDebug {
	/**
	 * Recursive method to feed mirrored receiver position on walls. No obstruction test is done.
	 * @param receiversImage Add receiver image here
	 * @param receiverCoord Receiver coordinate or precedent mirrored coordinate
	 * @param lastResult Last row index. -1 if first reflexion
	 * @param nearBuildingsWalls Walls to be reflected on
	 * @param depth Depth of reflection
	 */
	private static void feedMirroredReceiverResults( ArrayList<MirrorReceiverResult> receiversImage, Coordinate receiverCoord, int lastResult,ArrayList<LineSegment> nearBuildingsWalls,int depth,double distanceLimitation)
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
	 private static ArrayList<MirrorReceiverResult> GetMirroredReceiverResults(Coordinate receiverCoord,ArrayList<LineSegment> nearBuildingsWalls,int order,double distanceLimitation)
	 {
		 ArrayList<MirrorReceiverResult> receiversImage=new ArrayList<MirrorReceiverResult>();
		 feedMirroredReceiverResults(receiversImage,receiverCoord,-1,nearBuildingsWalls,order-1,distanceLimitation);
		 return receiversImage;
	 }
	 public static void main(String[] args) throws LayerDelaunayError 
	{
		double maxSrcDist=30;
		Coordinate receiverCoord=new Coordinate(20,3);
		Coordinate srcCoord=new Coordinate(12,4);
		
		FastObstructionTest freeFieldFinder = new FastObstructionTest("/home/fortin/OrbisGIS/temp");
		Coordinate[] polycontour={new Coordinate(14,0,0),new Coordinate(23,0,0),new Coordinate(23,6,0),new Coordinate(14,6,0),new Coordinate(14,4,0),new Coordinate(21,4,0),new Coordinate(21,2,0),new Coordinate(14,2,0),new Coordinate(14,0,0)};
		GeometryFactory factory=new GeometryFactory();
		freeFieldFinder.AddGeometry(factory.createPolygon(factory.createLinearRing(polycontour), null));
		freeFieldFinder.FinishPolygonFeeding(new Envelope(-10,30,-10,30));
		ArrayList<LineSegment> nearBuildingsWalls=new ArrayList<LineSegment>(freeFieldFinder.GetLimitsInRange(2, receiverCoord));

		ArrayList<MirrorReceiverResult> mirroredReceiver=GetMirroredReceiverResults(receiverCoord,nearBuildingsWalls,2,maxSrcDist);
		NonRobustLineIntersector linters=new NonRobustLineIntersector();
		int rayid=0;
		for( MirrorReceiverResult receiverReflection : mirroredReceiver)
		{
			ArrayList<LineSegment> debug_rays=new ArrayList<LineSegment>();
			double ReflectedSrcReceiverDistance=receiverReflection.getReceiverPos().distance(srcCoord);
			if(ReflectedSrcReceiverDistance<maxSrcDist)
			{
				boolean validReflection=false;
				int reflectionOrderCounter=0;
				MirrorReceiverResult receiverReflectionCursor=receiverReflection;
				//Test whether intersection point is on the wall segment or not
				Coordinate destinationPt=srcCoord;
				LineSegment seg=nearBuildingsWalls.get(receiverReflection.getWallId());
				linters.computeIntersection(seg.p0, seg.p1, receiverReflection.getReceiverPos(),destinationPt);
				while(linters.hasIntersection()) //While there is a reflection point on another wall
				{
					reflectionOrderCounter++;
					//There are a probable reflection point on the segment
					Coordinate reflectionPt=linters.getIntersection(0); //TODO maybe translate pt by epsilon to old destinationPt
					//Translate reflection point by epsilon value
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
					
					validReflection=freeFieldFinder.IsFreeField(reflectionPt, destinationPt);
					if(validReflection) //Reflection point can see source or its image
					{
						debug_rays.add(new LineSegment(reflectionPt,destinationPt));//TODO remove debug
						//Move to the next reflection pt. If there is no more reflection test freeField to source
						if(receiverReflectionCursor.getMirrorResultId()==-1)
						{   //Direct to the receiver
							debug_rays.add(new LineSegment(reflectionPt,receiverCoord)); //TODO remove debug instru
							validReflection=freeFieldFinder.IsFreeField(reflectionPt, receiverCoord);
							break; //That was the last reflection
						}else{
							//There is another reflection
							destinationPt.setCoordinate(reflectionPt);
							//Move reflection information cursor to a reflection closer 
							receiverReflectionCursor=mirroredReceiver.get(receiverReflectionCursor.getMirrorResultId());
							//Update intersection data
							seg=nearBuildingsWalls.get(receiverReflectionCursor.getWallId());
							linters.computeIntersection(seg.p0, seg.p1, receiverReflectionCursor.getReceiverPos(),destinationPt);
						}
					}else{
						break;
					}
					validReflection=false;
				}
				if(validReflection)
				{
					System.out.println("Reflection OK !");
				}
			}
			rayid++;
		}
	}
}
