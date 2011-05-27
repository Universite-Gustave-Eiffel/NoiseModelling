package lcpcson;
/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

import java.util.ArrayList;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

/*
 * This interface aims to link the acoustic module with many delaunay library,
 * to easy switch between libraries
 * 
 */
public interface LayerDelaunay {
	/**
	 * This optional method give an hint of the size of the delaunay process. Call this method before the first call of addPolygon
	 * This method is used only for optimization.
	 * @param[in] boundingBox Bounding box of the delaunay mesh
	 * @param[in] polygonCount Size of the polygon count
	 * @warning The precision of the parameters value is not required, this is only an hint.
	 */
	public void HintInit(Envelope boundingBox,long polygonCount,long verticesCount)  throws LayerDelaunayError ;
	/**
	 * Append a polygon into the triangulation
	 * @param[in] newPoly Polygon to append into the mesh, internal rings will be inserted as holes.
	 * @param[in] isEmpty This polygon is a hole. If yes, only the external ring is used.
	 */
	public void addPolygon(Polygon newPoly,boolean isEmpty) throws LayerDelaunayError;
	/**
	 * Append a vertex into the triangulation
	 * @param[in] vertexCoordinate Coordinate of the new vertex
	 */
	public void addVertex(Coordinate vertexCoordinate) throws LayerDelaunayError;
	/**
	 * Append a LineString into the triangulation
	 * @param[in] a Coordinate of the segment start
	 * @param[in] b Coordinate of the segment end
	 */
	public void addLineString(LineString line) throws LayerDelaunayError;
	/**
	 *  Set the minimum angle, if you wish to enforce the quality of the delaunay
	 *  Call processDelauney after to take account of this method.
	 *  @param[in] minAngle Minimum angle in radiant
	 */
	public void setMinAngle(Double minAngle) throws LayerDelaunayError;
	/**
	 *  Set the maximum area in m²
	 *  Call processDelauney after to take account of this method.
	 *  @param[in] maxArea Maximum area in m²
	 */
	public void setMaxArea(Double maxArea) throws LayerDelaunayError;
	/**
	 * Launch delaunay process
	 */
	public void processDelaunay()  throws LayerDelaunayError;
	/**
	 * When the processDelaunay has been called, retrieve results vertices
	 */
	public ArrayList<Coordinate> getVertices()  throws LayerDelaunayError;
	/**
	 * When the processDelaunay has been called, retrieve results
	 * Triangle link unique vertices by their index.
	 */
	public ArrayList<Triangle> getTriangles()  throws LayerDelaunayError;
	/*
	 * Remove all data, come back to the constructor state
	 */
	public void reset();
}
