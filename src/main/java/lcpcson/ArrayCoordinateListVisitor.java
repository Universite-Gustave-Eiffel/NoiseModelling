package lcpcson;
/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/


import java.util.*;
import com.vividsolutions.jts.index.ItemVisitor;
import com.vividsolutions.jts.geom.Coordinate;

public class ArrayCoordinateListVisitor
    implements ItemVisitor
{

  private ArrayList<Coordinate> items = new ArrayList<Coordinate>();
  private Coordinate destinationCoordinate;
  private double maxDist;
  
  public ArrayCoordinateListVisitor(Coordinate _destinationCoordinate,double _maxDist) {
	  destinationCoordinate=_destinationCoordinate;
	  maxDist=_maxDist;
  }

  public void visitItem(Object item)
  {
	if(destinationCoordinate.distance((Coordinate)item)<=maxDist)
		items.add((Coordinate)item);
  }

  public ArrayList<Coordinate> getItems() { return items; }

}