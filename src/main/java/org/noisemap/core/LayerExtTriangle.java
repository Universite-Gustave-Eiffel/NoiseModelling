/**
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact on urban mobility plans. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 *
 * This version is developed at French IRSTV Institute and at IFSTTAR
 * (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 *
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
 *
 * Copyright (C) 2011 IFSTTAR
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
 *
 * Noisemap is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Noisemap. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.noisemap.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.LinkedList;
import org.apache.log4j.Logger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;

/**
 * 
 * @author Nicolas Fortin
 */
public class LayerExtTriangle implements LayerDelaunay {

	private static Logger logger = Logger.getLogger(LayerExtTriangle.class
			.getName());
	// Final list
	private ArrayList<Coordinate> vertices = new ArrayList<Coordinate>();
	private LinkedList<Coordinate> holes = new LinkedList<Coordinate>();
	private ArrayList<IntSegment> segments = new ArrayList<IntSegment>();
	private ArrayList<Triangle> neighbors = new ArrayList<Triangle>(); // The
																		// first
																		// neighbor
																		// of
																		// triangle
																		// i is
																		// opposite
																		// the
																		// first
																		// corner
																		// of
																		// triangle
																		// i
	Quadtree ptQuad = new Quadtree();
	ArrayList<Triangle> triangles = new ArrayList<Triangle>();
	private static final String EOL = "\r\n";
	private String TrianglePath = System.getenv("trianglepath");
	private String tempdir = "";
	private String minAngle = "";
	private Coordinate unitize_translation_vec = new Coordinate();
	private final boolean deleteIntermediateFile = true;
	private String maxArea = "";
	private boolean retrieveNeighbords = false;

	// remove debug instr
	// DiskBufferDriver driverDebug;
	/**
	 * @param tmpDir
	 *            Temporary directory of input and output files
	 * @param maxSteinerFactor
	 *            Stop adding steiner point when Steiner point count
	 *            (maxSteiner*input vertice count)
	 */
	public LayerExtTriangle(String tmpDir) { // ,DataSourceFactory dsf remove
												// DataSourceFactory debug
												// parameter
		super();
		this.tempdir = tmpDir;
		if (TrianglePath == null) {
			TrianglePath = "triangle";
		}
		// remove debug instr
		/*
		 * if(dsf != null) { Type
		 * meta_type[]={TypeFactory.createType(Type.GEOMETRY)}; String
		 * meta_name[]={"the_geom"}; DefaultMetadata metadata = new
		 * DefaultMetadata(meta_type,meta_name); try { driverDebug = new
		 * DiskBufferDriver(dsf,metadata ); } catch (DriverException e) {
		 * e.printStackTrace(); return; } }
		 */
	}

	/*
	 * Change coordinate within a local interval
	 */
	private void unitize() {
            if(!this.vertices.isEmpty()) {
                Coordinate min = new Coordinate(this.vertices.get(0)), max = new Coordinate(
                                this.vertices.get(0));
                for (Coordinate pt : this.vertices) {
                        min.x = Math.min(min.x, pt.x);
                        min.y = Math.min(min.y, pt.y);
                        max.x = Math.max(max.x, pt.x);
                        max.y = Math.max(max.y, pt.y);
                }

                // Compute unitize translation vector
                unitize_translation_vec.x = min.x - (max.x - min.x) / 2.;
                unitize_translation_vec.y = min.y - (max.y - min.y) / 2.;

                for (int idv = 0; idv < this.vertices.size(); idv++) {
                        Coordinate v = this.vertices.get(idv);
                        this.vertices.set(idv, new Coordinate(v.x
                                        - unitize_translation_vec.x, v.y
                                        - unitize_translation_vec.y));
                }
                for (int idh = 0; idh < this.holes.size(); idh++) {
                        Coordinate v = this.holes.get(idh);
                        this.holes.set(idh, new Coordinate(v.x - unitize_translation_vec.x,
                                        v.y - unitize_translation_vec.y));
                }                    
            }
	}

	private void cancelUnitize() {
		for (int idv = 0; idv < this.vertices.size(); idv++) {
			Coordinate v = this.vertices.get(idv);
			this.vertices.set(idv, new Coordinate(v.x
					+ unitize_translation_vec.x, v.y
					+ unitize_translation_vec.y));
		}
		for (int idh = 0; idh < this.holes.size(); idh++) {
			Coordinate v = this.holes.get(idh);
			this.holes.set(idh, new Coordinate(v.x + unitize_translation_vec.x,
					v.y + unitize_translation_vec.y));
		}
	}

	private void readNodeFile(final String nodeFileName,
			ArrayList<Coordinate> localVertices) throws LayerDelaunayError {
		this.vertices.clear();
		this.ptQuad = new Quadtree();
		// //////////////////////////////////////////
		// Read model.1.node

		File file = new File(nodeFileName);
		// Initialization
		Scanner in;
		try {
			in = new Scanner(file);
		} catch (FileNotFoundException e) {
			throw new LayerDelaunayError(e);
		}
		in.useLocale(Locale.US); // essential to read float values
		final int vsize = in.nextInt(); // read vertice count
		in.nextInt(); // read dimension
		in.nextInt(); // read third header
		boolean isMarker = in.nextInt() == 1; // Read marker boolean
		localVertices.ensureCapacity(vsize);
		for (int vid = 0; vid < vsize; vid++) {
			in.nextInt(); // vertice ID
			Coordinate vcoord = new Coordinate(in.nextDouble(), in.nextDouble());
			if (isMarker) {
				in.nextInt();
			}
			localVertices.add(vcoord);
			this.getOrAppendVertices(vcoord, this.vertices);
		}
		in.close();
		if (deleteIntermediateFile) {
			file.delete();
		}
	}

	private void skipCommentLines(Scanner in) {
		// while(in.findInLine("#(.*)")!=null)
		while (!in.hasNextInt()) {
			in.nextLine();
		}
	}

	private void readSegsFile(final String polyFileName,
			ArrayList<Coordinate> localVertices) throws LayerDelaunayError {
		this.segments.clear();
		// //////////////////////////////////////////
		// Read model.1.poly

		File file = new File(polyFileName);
		// Initialization
		Scanner in;
		try {
			in = new Scanner(file);
		} catch (FileNotFoundException e) {
			throw new LayerDelaunayError(e.getMessage());
		}
		in.useLocale(Locale.US); // essential to read float values
		skipCommentLines(in);
		final int vsize = in.nextInt(); // read vertice count
		in.nextInt(); // read dimension
		in.nextInt(); // read third header
		boolean isVertexMarker = in.nextInt() == 1; // Read marker boolean
		if (vsize > 0) {
			localVertices.clear();
			localVertices.ensureCapacity(vsize);
		}
		for (int vid = 0; vid < vsize; vid++) {
			in.nextInt(); // vertex ID
			Coordinate vcoord = new Coordinate(in.nextDouble(), in.nextDouble());
			if (isVertexMarker) {
				in.nextInt();
			}
			localVertices.add(vcoord);
			this.getOrAppendVertices(vcoord, this.vertices);
		}

		final int ssize = in.nextInt(); // read seg count
		boolean isMarker = in.nextInt() == 1; // Read marker boolean
		this.segments.ensureCapacity(ssize);
		for (int tid = 0; tid < ssize; tid++) {
			in.nextInt(); // segment ID
			int a = this.getOrAppendVertices(
					localVertices.get(in.nextInt() - 1), this.vertices);
			int b = this.getOrAppendVertices(
					localVertices.get(in.nextInt() - 1), this.vertices);
			IntSegment newseg = new IntSegment(a, b);
			if (isMarker) {
				in.nextInt();
			}
			this.segments.add(newseg);
		}

		final int hsize = in.nextInt(); // read hole count
		if (hsize > 0) {
			holes.clear();
		}
		for (int hid = 0; hid < hsize; hid++) {
			in.nextInt(); // hole ID
			Coordinate hcoord = new Coordinate(in.nextDouble(), in.nextDouble());
			this.holes.add(hcoord);
		}

		in.close();

	}

	private void readNeighFile(final String eleFileName)
			throws LayerDelaunayError {
		this.neighbors.clear();
		// //////////////////////////////////////////
		// Read model.1.ele

		File file = new File(eleFileName);
		// Initialization
		Scanner in;
		try {
			in = new Scanner(file);
		} catch (FileNotFoundException e) {
			throw new LayerDelaunayError(e.getMessage());
		}
		in.useLocale(Locale.US); // essential to read float values
		final int tsize = in.nextInt(); // read triangle count
		in.nextInt(); // read dimension
		this.neighbors.ensureCapacity(tsize);
		for (int tid = 0; tid < tsize; tid++) {
			in.nextInt(); // triangle ID

			int a = in.nextInt() - 1;
			if (a == -2) {
				a = -1;
			}
			int b = in.nextInt() - 1;
			if (b == -2) {
				b = -1;
			}
			int c = in.nextInt() - 1;
			if (c == -2) {
				c = -1;
			}
			Triangle newtri = new Triangle(a, b, c);
			this.neighbors.add(newtri);
		}
		in.close();
		if (deleteIntermediateFile) {
			file.delete();
		}

	}

	private void readEleFile(final String eleFileName,
			ArrayList<Coordinate> localVertices) throws LayerDelaunayError {
		this.triangles.clear();
		// //////////////////////////////////////////
		// Read model.1.ele

		File file = new File(eleFileName);
		// Initialization
		Scanner in;
		try {
			in = new Scanner(file);
		} catch (FileNotFoundException e) {
			throw new LayerDelaunayError(e.getMessage());
		}
		in.useLocale(Locale.US); // essential to read float values
		final int tsize = in.nextInt(); // read triangle count
		in.nextInt(); // read dimension
		boolean isMarker = in.nextInt() == 1; // Read marker boolean
		this.triangles.ensureCapacity(tsize);
		for (int tid = 0; tid < tsize; tid++) {
			in.nextInt(); // triangle ID

			int a = this.getOrAppendVertices(
					localVertices.get(in.nextInt() - 1), this.vertices);
			int b = this.getOrAppendVertices(
					localVertices.get(in.nextInt() - 1), this.vertices);
			int c = this.getOrAppendVertices(
					localVertices.get(in.nextInt() - 1), this.vertices);
			Triangle newtri = new Triangle(a, b, c);
			if (isMarker) {
				in.nextInt();
			}
			this.triangles.add(newtri);
		}
		in.close();
		if (deleteIntermediateFile) {
			file.delete();
		}

	}

	private void readOutputFiles(final String nodeFileName,
			final String eleFileName, final String polyFileName,
			final String neighFileName) throws LayerDelaunayError {
		ArrayList<Coordinate> localVertices = new ArrayList<Coordinate>();
		readNodeFile(nodeFileName, localVertices);
		readEleFile(eleFileName, localVertices);
		readSegsFile(polyFileName, localVertices);
		if (!neighFileName.isEmpty()) {
			readNeighFile(neighFileName);
		}
		localVertices.clear();
		if (deleteIntermediateFile) {
			this.delete(polyFileName);
		}
	}

	private void writePolyFile(final String filepath) throws LayerDelaunayError {
		try {
			File file = new File(filepath);
			// Initialization
			PrintWriter out = new PrintWriter(new FileOutputStream(file));

			// Second loop write header part...
			out.printf("# %s%s", file.getName(), EOL);
			out.printf("#%s", EOL);
			out.printf("# File generated by OrbisGis%s", EOL);
			out.printf("%d 2 0 0%s", vertices.size(), EOL); // <# of vertices>
															// <dimension (must
															// be 2)> <# of
															// attributes> <# of
															// boundary markers
															// (0 or 1)>

			// write vertices
			int coordinateIndex = 1;
			for (Coordinate vertice : vertices) {
				out.printf("%d %s %s%s", coordinateIndex,
						Double.toString(vertice.x), Double.toString(vertice.y),
						EOL); // <vertex #> <x> <y> [attributes] [boundary
								// marker]
				coordinateIndex++;
			}
			// write segments
			out.printf("%d 0%s", segments.size(), EOL); // <# of segments> <# of
														// boundary markers (0
														// or 1)>
			int segmentIndex = 1;
			for (IntSegment segment : segments) {
				out.printf("%d %d %d%s", segmentIndex, segment.getA() + 1,
						segment.getB() + 1, EOL); // <segment #> <endpoint>
													// <endpoint> [boundary
													// marker]
				segmentIndex++;
			}
			// write holes position
			out.printf("%d%s", holes.size(), EOL); // <# of holes>
			int holeIndex = 1;
			for (Coordinate holePosition : holes) {
				out.printf("%d %s %s%s", holeIndex,
						Double.toString(holePosition.x),
						Double.toString(holePosition.y), EOL); // <hole #> <x>
																// <y>
				holeIndex++;
			}
			out.close();
		} catch (FileNotFoundException e) {
			throw new LayerDelaunayError(e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	private int getOrAppendVertices(Coordinate newCoord,
			ArrayList<Coordinate> vertices) {
		// We can obtain the same hash with two different coordinate (4 Bytes or
		// 8 Bytes against 12 or 24 Bytes) , then we use a list as the value of
		// the hashmap
		// First step - Search the vertex parameter within the hashMap
		int newCoordIndex = -1;
		Envelope queryEnv = new Envelope(newCoord);
		queryEnv.expandBy(1.f);
		List<EnvelopeWithIndex<Integer>> result = ptQuad.query(queryEnv);
		for (EnvelopeWithIndex<Integer> envel : result) {
			Coordinate foundCoord = vertices.get(envel.getId());
			if (foundCoord.distance(newCoord) < 0.0001) {
				return envel.getId();
			}
		}
		// Not found then
		// Append to the list and QuadTree
		vertices.add(newCoord);
		newCoordIndex = vertices.size() - 1;
		ptQuad.insert(queryEnv, new EnvelopeWithIndex<Integer>(queryEnv,
				newCoordIndex));
		return newCoordIndex;
	}

	public void loadInputDelaunay(String polyPath) throws LayerDelaunayError {
		ArrayList<Coordinate> localVertices = new ArrayList<Coordinate>();
		this.readSegsFile(polyPath, localVertices);
		localVertices.clear();
	}

	private void delete(String fileName) {
		File nodef = new File(fileName);
		if (nodef.exists()) {
			nodef.delete();
		}
	}
	public static boolean isWindows() {
		 
		String os = System.getProperty("os.name").toLowerCase();
		//windows
	    return (os.indexOf( "win" ) >= 0); 
	}
	/**
	 * @return If savePolyToRefine is true, the path to the polygon file. can be
	 *         used later with the method loadInputDelaunay
	 */
	public String processDelaunay(String prefix, int delaunayIndex,
			int maxSteiner, boolean steinerOnBoundaries,
			boolean savePolyToRefine) throws LayerDelaunayError {

			int refineIndex = 1;
			if (!steinerOnBoundaries) {
				refineIndex++;
			}
			// Convert splitted lineString into segments
			// finalAddLineString();
			// Translate vertices around center for precision
			this.unitize();
			// /////////////////////////////////////////////////
			// Write Polygon File
			String inputroot = this.tempdir + File.separatorChar + prefix
					+ "region" + delaunayIndex;
			String polypath = new String(inputroot);
			String inIndex = "";
			if (refineIndex > 1) {
				inIndex = "." + (refineIndex - 1);
			}
			polypath += inIndex + ".poly";
			// Remove existing files
			this.delete(inputroot + inIndex + ".node");
			this.delete(inputroot + inIndex + ".ele");

			//
			this.writePolyFile(polypath);

			String options = new String("-p");
			if (this.minAngle != "0") {
				options += "q";
				if (this.minAngle != "") {
					options += this.minAngle;
				}
			}
			if (this.maxArea != "") {
				options += "a" + this.maxArea;
			}
			if (maxSteiner != -1) {
				options += "S" + maxSteiner;
			}
			if (!steinerOnBoundaries) {
				options += "YY";
			}
			if (this.retrieveNeighbords) {
				options += "n";
			}
			// /////////////////////////////////////////////////
			// Clear mem & Call Triangle binary
			vertices.clear();
			holes.clear();
			segments.clear();
			neighbors.clear();
			if(isWindows()) {
				options += " \"" + polypath + "\""; // Add polygon file path
			} else {
				options += " " +polypath ; // Add polygon file path
			}
			String line;
			logger.info("Run delaunay triangulation " + TrianglePath + " "
					+ options);

				BufferedReader input=null;
				try {
					Process p = Runtime.getRuntime().exec(TrianglePath + " " + options);
					input = new BufferedReader(new InputStreamReader(
							p.getInputStream()));
				
					while ((line = input.readLine()) != null) {
						System.out.println(line);
					}
				} catch (IOException e) {
					e.printStackTrace();
					throw new LayerDelaunayError(e.getMessage());
				} finally {
					if(input!=null) {
						try {
							input.close();
						} catch (IOException e) {
							e.printStackTrace();
							throw new LayerDelaunayError(e.getMessage());
						}
					}						
				}
			// /////////////////////////////////////////////////
			// Read output files
			if (deleteIntermediateFile) {
				this.delete(polypath);
			}
			String neighFileName = "";
			if (retrieveNeighbords) {
				neighFileName = inputroot + "." + refineIndex + ".neigh";
			}
			readOutputFiles(inputroot + "." + refineIndex + ".node", inputroot
					+ "." + refineIndex + ".ele", inputroot + "." + refineIndex
					+ ".poly", neighFileName);
			logger.info("Size of triangulation :" + triangles.size()
					+ " faces.");
			// Restore original coordinates
			this.cancelUnitize();
			// Save mesh to refine the mesh later
			String finalName = new String();
			if (savePolyToRefine) {
				finalName = this.tempdir + File.separatorChar + "firstPass"
						+ delaunayIndex + ".poly";
				this.writePolyFile(finalName);
			}
			return finalName;
		
	}

	@Override
	public void processDelaunay() throws LayerDelaunayError {
		processDelaunay("", 0, -1, true, false);
	}

	@Override
	public void setMinAngle(Double minAngle) {
		if (minAngle > 0.01) {
			this.minAngle = minAngle.toString();
		} else {
			this.minAngle = "0";
		}
	}

	@Override
	public void setMaxArea(Double maxArea) throws LayerDelaunayError {
		this.maxArea = maxArea.toString();
	}

	@Override
	public void addPolygon(Polygon newPoly, boolean isEmpty)
			throws LayerDelaunayError {

		// remove debug instr
		/*
		 * if(driverDebug != null) { Value[] row=new Value[1];
		 * row[0]=ValueFactory.createValue(newPoly); try {
		 * driverDebug.addValues(row); } catch (DriverException e) { //
		 * Auto-generated catch block e.printStackTrace(); return; } }
		 */
		// Append main polygon
		GeometryFactory factory = new GeometryFactory();
		final Coordinate[] coordinates = newPoly.getExteriorRing()
				.getCoordinates();
		if (coordinates.length > 1) {
			LineString newLineString = factory.createLineString(coordinates);
			this.addLineString(newLineString);
		}
		if (isEmpty) {
			holes.add(newPoly.getInteriorPoint().getCoordinate());
		}
		// Append holes
		final int holeCount = newPoly.getNumInteriorRing();
		for (int holeIndex = 0; holeIndex < holeCount; holeIndex++) {
			LineString holeLine = newPoly.getInteriorRingN(holeIndex);
			// Convert hole into a polygon, then compute an interior point
			Polygon polyBuffnew = factory.createPolygon(
					factory.createLinearRing(holeLine.getCoordinates()), null);
			if (polyBuffnew.getArea() > 0.) {
				Coordinate interiorPoint = polyBuffnew.getInteriorPoint()
						.getCoordinate();
				if (!factory.createPoint(interiorPoint).intersects(holeLine)) {
					if (!isEmpty) {
						holes.add(interiorPoint);
					}
					this.addLineString(holeLine);
				} else {
					logger.info("Warning : hole rejected, can't find interior point.");
				}
			} else {
				logger.info("Warning : hole rejected, area=0");
			}
		}
	}

	@Override
	public void hintInit(Envelope boundingBox, long polygonCount,
			long verticesCount) throws LayerDelaunayError {
		this.vertices.ensureCapacity((int) verticesCount);
		this.segments.ensureCapacity((int) polygonCount * 5);

	}

	@Override
	public List<Coordinate> getVertices() throws LayerDelaunayError {
		return vertices;
	}

	@Override
	public List<Triangle> getTriangles() throws LayerDelaunayError {
		return triangles;
	}

	public List<Triangle> getNeighbors() throws LayerDelaunayError {
		return neighbors;
	}

	@Override
	public void addVertex(Coordinate vertexCoordinate)
			throws LayerDelaunayError {
		getOrAppendVertices(vertexCoordinate, vertices);
	}

	// @SuppressWarnings("unchecked")
	@Override
	public void addLineString(LineString lineToProcess)
			throws LayerDelaunayError {

		Coordinate[] coords = lineToProcess.getCoordinates();
		for (int ind = 1; ind < coords.length; ind++) {
			int firstVertIndex = getOrAppendVertices(coords[ind - 1], vertices);
			int secondVertIndex = getOrAppendVertices(coords[ind], vertices);
			IntSegment newSeg = new IntSegment(firstVertIndex, secondVertIndex);
			segments.add(newSeg);
		}
	}

	public void setRetrieveNeighbors(boolean retrieve) {
		this.retrieveNeighbords = retrieve;
	}

	@Override
	public void reset() {
		this.holes.clear();
		this.unitize_translation_vec = new Coordinate();
		this.segments.clear();
		this.vertices.clear();
		this.ptQuad = new Quadtree();
		this.maxArea = "";
		this.minAngle = "";
	}
}
