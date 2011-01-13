package lcpc_son;

import java.util.ArrayList;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;


/**
 * Data input for a propagation process (SubEnveloppe of BR_TriGrid)
 * @author fortin
 *
 */
public class PropagationProcessData {
	public ArrayList<Coordinate> vertices;			//Coordinate of receivers
	public ArrayList<Triangle> triangles;			//Index of vertices of triangles
	public FastObstructionTest freeFieldFinder;		//FreeField test
	public GridIndex<Integer> sourcesIndex;			//Source Index
	public ArrayList<Geometry> sourceGeometries;	//Sources geometries. Can be LINESTRING or POINT
	public ArrayList<Double> wj_sources;			//Sound level of source. Size is nbsource*freq_count
	public int[] freq_lvl;							//Frequency bands
	public int reflexionOrder;						//reflexionOrder
	public double maxSrcDist;						//Maximum source distance
	public double minRecDist;						//Minimum distance between source and receiver
	public double wallAlpha;						//Wall alpha [0-1]
	public Long cellId;								//cell id
	public PropagationProcessData(ArrayList<Coordinate> vertices,
			ArrayList<Triangle> triangles, FastObstructionTest freeFieldFinder,
			GridIndex<Integer> sourcesIndex,
			ArrayList<Geometry> sourceGeometries, ArrayList<Double> wj_sources,
			int[] freq_lvl, int reflexionOrder,
			double maxSrcDist, double minRecDist, double wallAlpha, Long cellId) {
		super();
		this.vertices = vertices;
		this.triangles = triangles;
		this.freeFieldFinder = freeFieldFinder;
		this.sourcesIndex = sourcesIndex;
		this.sourceGeometries = sourceGeometries;
		this.wj_sources = wj_sources;
		this.freq_lvl = freq_lvl;
		this.reflexionOrder = reflexionOrder;
		this.maxSrcDist = maxSrcDist;
		this.minRecDist = minRecDist;
		this.wallAlpha = wallAlpha;
		this.cellId = cellId;
	}
	
	
}
