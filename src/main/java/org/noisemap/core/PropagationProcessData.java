package org.noisemap.core;

/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

import java.util.ArrayList;
import java.util.List;

import org.gdms.data.DataSourceFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Data input for a propagation process (SubEnveloppe of BR_TriGrid)
 * 
 * @author fortin
 * 
 */
public class PropagationProcessData {
	public List<Coordinate> vertices; // Coordinate of receivers
	public List<Triangle> triangles; // Index of vertices of triangles
	public FastObstructionTest freeFieldFinder; // FreeField test
	public QueryGeometryStructure<Integer> sourcesIndex; // Source Index
	public List<Geometry> sourceGeometries; // Sources geometries. Can be
											// LINESTRING or POINT
	public List<ArrayList<Double>> wj_sources; // Sound level of source. By
												// frequency band, energetic
	public List<Integer> freq_lvl; // Frequency bands values, by third octave
	public int reflexionOrder; // reflexionOrder
	public int diffractionOrder; // diffractionOrder
	public double maxSrcDist; // Maximum source distance
	public double minRecDist; // Minimum distance between source and receiver
	public double wallAlpha; // Wall alpha [0-1]
	public Long cellId; // cell id
	public DataSourceFactory dsf; // Debug purpose
	ProgressionProcess cellProg; // Progression information

	public PropagationProcessData(List<Coordinate> vertices,
			List<Triangle> triangles, FastObstructionTest freeFieldFinder,
			QueryGeometryStructure<Integer> sourcesIndex,
			List<Geometry> sourceGeometries,
			List<ArrayList<Double>> wj_sources, List<Integer> freq_lvl,
			int reflexionOrder, int diffractionOrder, double maxSrcDist,
			double minRecDist, double wallAlpha, Long cellId,
			DataSourceFactory dsf, ProgressionProcess cellProg) {
		super();
		this.vertices = vertices;
		this.triangles = triangles;
		this.freeFieldFinder = freeFieldFinder;
		this.sourcesIndex = sourcesIndex;
		this.sourceGeometries = sourceGeometries;
		this.wj_sources = wj_sources;
		this.freq_lvl = freq_lvl;
		this.reflexionOrder = reflexionOrder;
		this.diffractionOrder = diffractionOrder;
		this.maxSrcDist = maxSrcDist;
		this.minRecDist = minRecDist;
		this.wallAlpha = wallAlpha;
		this.cellId = cellId;
		this.dsf = dsf;
		this.cellProg = cellProg;
	}

}
