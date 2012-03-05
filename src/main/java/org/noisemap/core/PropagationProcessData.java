package org.noisemap.core;

/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import java.util.ArrayList;
import java.util.List;
import org.gdms.data.DataSourceFactory;

/**
 * Data input for a propagation process (SubEnveloppe of BR_TriGrid)
 * 
 * @author fortin
 * 
 */
public class PropagationProcessData {
	public List<Coordinate> vertices; // Coordinate of receivers
        public List<Long> receiverRowId;  //Row id of receivers, only for BR_PtGrid
	public List<Triangle> triangles; // Index of vertices of triangles
	public FastObstructionTest freeFieldFinder; // FreeField test
	public QueryGeometryStructure sourcesIndex; // Source Index
	public List<Geometry> sourceGeometries; // Sources geometries. Can be
											// LINESTRING or POINT
	public List<ArrayList<Double>> wj_sources; // Sound level of source. By
												// frequency band, energetic
	public List<Integer> freq_lvl; // Frequency bands values, by third octave
	public int reflexionOrder; // reflexionOrder
	public int diffractionOrder; // diffractionOrder
	public double maxSrcDist; // Maximum source distance
        public double maxRefDist; // Maximum reflection wall distance
	public double minRecDist; // Minimum distance between source and receiver
	public double wallAlpha; // Wall alpha [0-1]
	public int cellId; // cell id
	public DataSourceFactory dsf; // Debug purpose
	public ProgressionProcess cellProg; // Progression information

    public PropagationProcessData(List<Coordinate> vertices, List<Long> receiverRowId, List<Triangle> triangles, FastObstructionTest freeFieldFinder, QueryGeometryStructure sourcesIndex, List<Geometry> sourceGeometries, List<ArrayList<Double>> wj_sources, List<Integer> freq_lvl, int reflexionOrder, int diffractionOrder, double maxSrcDist, double maxRefDist, double minRecDist, double wallAlpha, int cellId, DataSourceFactory dsf, ProgressionProcess cellProg) {
        this.vertices = vertices;
        this.receiverRowId = receiverRowId;
        this.triangles = triangles;
        this.freeFieldFinder = freeFieldFinder;
        this.sourcesIndex = sourcesIndex;
        this.sourceGeometries = sourceGeometries;
        this.wj_sources = wj_sources;
        this.freq_lvl = freq_lvl;
        this.reflexionOrder = reflexionOrder;
        this.diffractionOrder = diffractionOrder;
        this.maxSrcDist = maxSrcDist;
        this.maxRefDist = maxRefDist;
        this.minRecDist = minRecDist;
        this.wallAlpha = wallAlpha;
        this.cellId = cellId;
        this.dsf = dsf;
        this.cellProg = cellProg;
    }


	

}
