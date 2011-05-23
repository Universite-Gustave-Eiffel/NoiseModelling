package lcpc_son;
/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Triangle;

public class TriMarkers extends Triangle {

	public TriMarkers()
	{
		super(new Coordinate(),new Coordinate(),new Coordinate());
		this.m1 = 0;
		this.m2 = 0;
		this.m3 = 0;		
	}
	public TriMarkers(Coordinate p0, Coordinate p1, Coordinate p2, double m1,
			double m2, double m3) {
		super(p0, p1, p2);

		if(!CGAlgorithms.isCCW(this.GetRing()))
			this.SetCoordinates(p2,p1,p0);
		this.m1 = m1;
		this.m2 = m2;
		this.m3 = m3;
	}

	public double m1,m2,m3;

	public void setMarkers(double m1,
			double m2, double m3)
	{
		this.m1 = m1;
		this.m2 = m2;
		this.m3 = m3;		
	}
	public void SetAll(Coordinate p0, Coordinate p1, Coordinate p2, double m1,
			double m2, double m3)
	{
		SetCoordinates(p0,p1,p2);
		setMarkers(m1, m2, m3);
		if(!CGAlgorithms.isCCW(this.GetRing()))
			this.SetCoordinates(p2,p1,p0);
	}
	public double GetMinMarker()
	{
		return GetMinMarker((short)-1);
	}
	public double GetMinMarker(short exception)
	{
		double minval=Double.POSITIVE_INFINITY;
		if(exception!=0)
			minval=Math.min(minval, this.m1);
		if(exception!=1)
			minval=Math.min(minval, this.m2);
		if(exception!=2)
			minval=Math.min(minval, this.m3);
		return minval;
	}
	public double GetMaxMarker()
	{
		return GetMaxMarker((short)-1);
	}
	public double GetMaxMarker(short exception)
	{
		double maxval=Double.NEGATIVE_INFINITY;
		if(exception!=0)
			maxval=Math.max(maxval, this.m1);
		if(exception!=1)
			maxval=Math.max(maxval, this.m2);
		if(exception!=2)
			maxval=Math.max(maxval, this.m3);
		return maxval;
	}
	public void SetCoordinates(Coordinate p0, Coordinate p1, Coordinate p2)
	{
		this.p0=p0;
		this.p1=p1;
		this.p2=p2;
	}
	public Coordinate[] GetRing()
	{
		Coordinate[] ring={p0,p1,p2,p0};
		return ring;
	}
	Coordinate getVertice(short idvert)
	{
		if(idvert==0)
			return this.p0;
		else if(idvert==1)
			return this.p1;
		else
			return this.p2;
	}
	double getMarker(short idvert)
	{
		if(idvert==0)
			return this.m1;
		else if(idvert==1)
			return this.m2;
		else
			return this.m3;
	}
}
