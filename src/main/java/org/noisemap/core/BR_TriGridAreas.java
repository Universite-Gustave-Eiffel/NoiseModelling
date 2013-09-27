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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.values.Value;
import org.gdms.driver.DriverException;
import org.gdms.driver.DataSet;
import org.gdms.driver.driverManager.DriverLoadException;
import org.gdms.sql.function.FunctionException;
import org.gdms.sql.function.FunctionSignature;
import org.gdms.sql.function.ScalarArgument;
import org.gdms.sql.function.table.TableArgument;
import org.gdms.sql.function.table.TableDefinition;
import org.gdms.sql.function.table.TableFunctionSignature;
import org.gdms.driver.DiskBufferDriver;
import org.orbisgis.progress.ProgressMonitor;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import org.gdms.data.schema.MetadataUtilities;

/**
 *
 * @author SU Qi
 */
public class BR_TriGridAreas extends BR_TriGrid{
        private Logger logger = Logger.getLogger(BR_TriGridAreas.class.getName());
	// _________ ^
	// | | | | | | Y or J (bottom to top)
	// | | | | |
	// |_|_|_|_|
	// -> X or I (left to right)
	private static short nRight = 0, nLeft = 1, nBottom = 2, nTop = 3; // neighbor
																		// relative
																		// positions
																		// index
	private static short[][] neighboor = { { 1, 0 }, { -1, 0 }, { 0, -1 },
			{ 0, 1 } }; // neighbor relative positions
	// Timing sum in millisec
	private long totalParseBuildings = 0;
	private long totalDelaunay = 0;
        private static final String heightField = "height";
        
        @Override
        public FunctionSignature[] getFunctionSignatures() {
		// Builds geom , sources.the_geom, soil areas, sources.db_m ,max propa dist , subdiv
		// lev ,roads width , receiv road dis,max tri area ,sound refl o,sound
		// dif order,wall alpha
            return new FunctionSignature[]{
                            new TableFunctionSignature(TableDefinition.GEOMETRY,
                            new TableArgument(TableDefinition.GEOMETRY),//buildings
                            new TableArgument(TableDefinition.GEOMETRY),//src
                            new TableArgument(TableDefinition.GEOMETRY),//soil areas
                            ScalarArgument.STRING,
                            ScalarArgument.DOUBLE,
                            ScalarArgument.DOUBLE,
                            ScalarArgument.INT,
                            ScalarArgument.DOUBLE,
                            ScalarArgument.DOUBLE,
                            ScalarArgument.DOUBLE,
                            ScalarArgument.INT,
                            ScalarArgument.INT,
                            ScalarArgument.DOUBLE                            
                            )
                    };
        }
        
        @Override
        public DataSet evaluate(DataSourceFactory dsf, DataSet[] tables,
            Value[] values, ProgressMonitor pm) throws FunctionException {
		String tmpdir = dsf.getTempDir().getAbsolutePath();
                if(values.length<10) {
                    throw new FunctionException("Not enough parameters !");
                }else if(values.length>10){
                    throw new FunctionException("Too many parameters !");
                }
		String dbField = values[0].toString();
		double maxSrcDist = values[1].getAsDouble();
                double maxRefDist = values[2].getAsDouble();
		int subdivLvl = values[3].getAsInt();
		double minRecDist = values[4].getAsDouble(); /*
													 * <! Minimum distance
													 * between source and
													 * receiver
													 */
		double srcPtDist = values[5].getAsDouble(); /*
													 * <! Complexity distance of
													 * roads
													 */
		double maximumArea = values[6].getAsDouble();
		int reflexionOrder = values[7].getAsInt();
		int diffractionOrder = values[8].getAsInt();
		double wallAlpha = values[9].getAsDouble();
		boolean forceSinglePass = false;
		boolean doMultiThreading = true;
                assert(maxSrcDist>maxRefDist); //Maximum Source-Receiver
                                               //distance must be superior than
                                               //maximum Receiver-Wall distance
                DiskBufferDriver driver=null;
                ThreadPool threadManager=null;
                ProgressionOrbisGisManager pmManager=null;
                PropagationProcessDiskWriter driverManager=null;
		try {
			// Steps of execution
			// Evaluation of the main bounding box (sources+buildings)
			// Split domain into 4^subdiv cells
			// For each cell :
			// Expand bounding box cell by maxSrcDist
			// Build delaunay triangulation from buildings polygon processed by
			// intersection with non extended bounding box
			// Save the list of sources index inside the extended bounding box
			// Save the list of buildings index inside the extended bounding box
			// Make a structure to keep the following information
			// Triangle list with the 3 vertices index
			// Vertices list (as receivers)
			// For each vertices within the cell bounding box (not the extended
			// one)
			// Find all sources within maxSrcDist
			// For All found sources
			// Test if there is a gap(no building) between source and receiver
			// if not then append the distance attenuated sound level to the
			// receiver
			// Save the triangle geometry with the db_m value of the 3 vertices
			
			int tableBuildings = 0;
			int tableSources = 1;
                        int tableSoilAreas = 2; 
			long nbreceivers = 0;

                            
                           
			// Load Sources, Buildings, Topo Points and Soil Areas table drivers
                        if(tables.length!=3){
                            throw new FunctionException("Tables length must be 3 !");
                        }
			final DataSet sds = tables[tableBuildings];
			final DataSet sdsSources = tables[tableSources];
                        final DataSet sdsSoilAreas = tables[tableSoilAreas];  
                       

                        // extract spatial field index of four input tables
			int spatialBuildingsFieldIndex = MetadataUtilities.getSpatialFieldIndex(sds.getMetadata());
			int spatialSourceFieldIndex = MetadataUtilities.getSpatialFieldIndex(sdsSources.getMetadata());
                        int spatialsdsSoilAreasFieldIndex = MetadataUtilities.getSpatialFieldIndex(sdsSoilAreas.getMetadata());

			
			
			// 1 Step - Evaluation of the main bounding box (sources)
			Envelope mainEnvelope = GetGlobalEnvelope(sdsSources, pm);
                        // Split domain into 4^subdiv cells

			int gridDim = (int) Math.pow(2, subdivLvl);

			// Initialization frequency declared in source Table
			ArrayList<Integer> db_field_ids = new ArrayList<Integer>();
			ArrayList<Integer> db_field_freq = new ArrayList<Integer>();
			int fieldid = 0;
			for (String fieldName : sdsSources.getMetadata().getFieldNames()) {
				if (fieldName.startsWith(dbField)) {
					String sub = fieldName.substring(dbField.length());
					if (sub.length() > 0) {
						int freq = Integer.parseInt(sub);
						db_field_ids.add(fieldid);
						db_field_freq.add(freq);
					} else {
						db_field_ids.add(fieldid);
						db_field_freq.add(0);
					}
				}
				fieldid++;
			}

			double cellWidth = mainEnvelope.getWidth() / gridDim;
			double cellHeight = mainEnvelope.getHeight() / gridDim;

			String[] firstPassResults = new String[gridDim * gridDim];
			NodeList[] neighborsBorderVertices = new NodeList[gridDim * gridDim];

			driver = new DiskBufferDriver(dsf, getMetadata(null));

			int nbcell = gridDim * gridDim;
			if (nbcell == 1) {
				doMultiThreading = false;
				forceSinglePass = true;
			}

			Runtime runtime = Runtime.getRuntime();
			threadManager = new ThreadPool(
					runtime.availableProcessors(),
					runtime.availableProcessors() + 1, Long.MAX_VALUE,
					TimeUnit.SECONDS);

			pmManager = new ProgressionOrbisGisManager(
					nbcell, pm);
			Stack<PropagationResultTriRecord> toDriver = new Stack<PropagationResultTriRecord>();
			driverManager = new PropagationProcessDiskWriter(
					toDriver,null, driver,null);
			driverManager.start();
			pmManager.start();
			PropagationProcessOut threadDataOut = new PropagationProcessOut(
					toDriver,null);

			for (int cellI = 0; cellI < gridDim; cellI++) {
				for (int cellJ = 0; cellJ < gridDim; cellJ++) {
					MeshBuilder mesh = new MeshBuilder();
					int ij = cellI * gridDim + cellJ;
					logger.info("Begin processing of cell " + (cellI+1) + ","
							+ (cellJ+1) + " of the " + gridDim + "x" + gridDim
							+ "  grid..");
					if (pm!=null && pm.isCancelled()) {
						driver.writingFinished();
						return driver.getTable("main");
					}
					Envelope cellEnvelope = getCellEnv(mainEnvelope, cellI,
							cellJ, gridDim, gridDim, cellWidth, cellHeight);// new
																			// Envelope(mainEnvelope.getMinX()+cellI*cellWidth,
																			// mainEnvelope.getMinX()+cellI*cellWidth+cellWidth,
																			// mainEnvelope.getMinY()+cellHeight*cellJ,
																			// mainEnvelope.getMinY()+cellHeight*cellJ+cellHeight);
					Envelope expandedCellEnvelop = new Envelope(cellEnvelope);
					expandedCellEnvelop.expandBy(maxSrcDist);
					// Build delaunay triangulation from buildings inside the
					// extended bounding box

					// //////////////////////////////////////////////////////
					// Make source index for optimization
					ArrayList<Geometry> sourceGeometries = new ArrayList<Geometry>();
					ArrayList<ArrayList<Double>> wj_sources = new ArrayList<ArrayList<Double>>();
					QueryGeometryStructure sourcesIndex = new QueryQuadTree();
					// QueryGeometryStructure<Integer> sourcesIndex=new
					// QueryQuadTree<Integer>();

					long rowCount = sdsSources.getRowCount();
					int fieldCount = sdsSources.getMetadata().getFieldCount();
					Integer idsource = 0;
					for (long rowIndex = 0; rowIndex < rowCount; rowIndex++) {
						
						//Value[] row = sdsSources.getRow(rowIndex);
						//Geometry geo = sdsSources.getGeometry(rowIndex);
                                                final Value[] row = new Value[fieldCount];
                                                for (int j = 0; j < fieldCount; j++) {
                                                        row[j] = sdsSources.getFieldValue(rowIndex, j);
                                                }
                                                Geometry geo = row[spatialSourceFieldIndex].getAsGeometry();

						Envelope ptEnv = geo.getEnvelopeInternal();
						if (ptEnv.intersects(expandedCellEnvelop)) {
							sourcesIndex.appendGeometry(geo, idsource);
							ArrayList<Double> wj_spectrum = new ArrayList<Double>();
							wj_spectrum.ensureCapacity(db_field_ids.size());
							for (Integer idcol : db_field_ids) {
								wj_spectrum
										.add(DbaToW(row[idcol].getAsDouble()));
							}
							wj_sources.add(wj_spectrum);
							sourceGeometries.add(geo);
							idsource++;
						}
					}

					// //////////////////////////////////////////////////////
					// feed freeFieldFinder for fast intersection query
					// optimization


					rowCount = sds.getRowCount();
					for (long rowIndex = 0; rowIndex < rowCount; rowIndex++) {
						final Geometry geometry = sds.getFieldValue(rowIndex, spatialBuildingsFieldIndex).getAsGeometry();
                                                double height = 0.;
                                                
                                                //if exist height
                                               if(sds.getMetadata().getFieldIndex(heightField)!=-1){
                                                  height=sds.getFieldValue(rowIndex, sds.getMetadata().getFieldIndex(heightField)).getAsDouble();
                                               }
						Envelope geomEnv = geometry.getEnvelopeInternal();
						if (expandedCellEnvelop.intersects(geomEnv)) {
                                                    //if we dont have height of building
                                                    if(Double.compare(height, 0.)==0){
                                                        mesh.addGeometry(geometry);
                                                    }
                                                    else{
                                                        mesh.addGeometry(geometry,height);
                                                    }
						}
					}

					mesh.finishPolygonFeeding(expandedCellEnvelop);
                                        FastObstructionTest freeFieldFinder= new FastObstructionTest(mesh.getPolygonWithHeight(), 
                                                                             mesh.getTriangles(), mesh.getTriNeighbors(), mesh.getVertices());
					// Compute the first pass delaunay mesh
					// The first pass doesn't take account of additional
					// vertices of neighbor cells at the borders
					// then, there are discontinuities in iso surfaces at each
					// border of cell
					LayerDelaunay cellMesh = new LayerExtTriangle(tmpdir);// new
																			// LayerCTriangle();
																			// //new
																			// LayerJDelaunay();

					if (cellMesh instanceof LayerExtTriangle
							&& !forceSinglePass) {
						for (short[] ijneighoffset : neighboor) {
							int[] ijneigh = { cellI + ijneighoffset[0],
									cellJ + ijneighoffset[1] };
							if (ijneigh[0] >= 0 && ijneigh[0] < gridDim
									&& ijneigh[1] >= 0 && ijneigh[1] < gridDim) {
								if (firstPassResults[getCellId(ijneigh[0],
										ijneigh[1], gridDim)] == null) {
									cellMesh.reset();
									computeFirstPassDelaunay(cellMesh,
											mainEnvelope, ijneigh[0],
											ijneigh[1], gridDim, gridDim,
											cellWidth, cellHeight, maxSrcDist,
											sds, sdsSources,spatialBuildingsFieldIndex,spatialSourceFieldIndex, minRecDist,
											srcPtDist, firstPassResults,
											neighborsBorderVertices,
											maximumArea);
								}
							}
						}
						// Compute the first pass of the 5 neighbor cells if
						// this is not already done
						if (firstPassResults[getCellId(cellI, cellJ, gridDim)] == null) {
							cellMesh.reset();
							computeFirstPassDelaunay(cellMesh, mainEnvelope,
									cellI, cellJ, gridDim, gridDim, cellWidth,
									cellHeight, maxSrcDist, sds, sdsSources, spatialBuildingsFieldIndex,spatialSourceFieldIndex,
									minRecDist, srcPtDist, firstPassResults,
									neighborsBorderVertices, maximumArea);
						}

						// Compute second pass of the current cell
						cellMesh.reset();
						computeSecondPassDelaunay(
								(LayerExtTriangle) cellMesh,
								mainEnvelope,
								cellI,
								cellJ,
								gridDim,
								gridDim,
								cellWidth,
								cellHeight,
								firstPassResults[getCellId(cellI, cellJ,
										gridDim)],
								neighborsBorderVertices[getCellId(cellI, cellJ,
										gridDim)]);
					} else {
						computeFirstPassDelaunay(cellMesh, mainEnvelope, cellI,
								cellJ, gridDim, gridDim, cellWidth, cellHeight,
								maxSrcDist, sds, sdsSources, spatialBuildingsFieldIndex, spatialSourceFieldIndex, minRecDist,
								srcPtDist, firstPassResults,
								neighborsBorderVertices, maximumArea);
					}
					// Make a structure to keep the following information
					// Triangle list with 3 vertices(int), and 3 neighbor
					// triangle ID
					// Vertices list

					// The evaluation of sound level must be done where the
					// following vertices are
					List<Coordinate> vertices = cellMesh.getVertices();
					List<Triangle> triangles = cellMesh.getTriangles();
					nbreceivers += vertices.size();
                                        List<GeoWithSoilType> geoWithSoil = new LinkedList<GeoWithSoilType>();
                                        if(spatialsdsSoilAreasFieldIndex!=-1&&sdsSoilAreas!=null){
                                            rowCount = sdsSoilAreas.getRowCount();
                                            
                                            for(int i=0;i<rowCount;i++){
                                                Geometry soilGeo = sdsSoilAreas.getFieldValue(i, spatialsdsSoilAreasFieldIndex).getAsGeometry();
                                                double soilType = 0.;
                                                if (sdsSoilAreas.getMetadata().getFieldIndex("G")!=-1){
                                                    sdsSoilAreas.getFieldValue(i, sdsSoilAreas.getMetadata().getFieldIndex("G")).getAsDouble();
                                                }
                                                if(expandedCellEnvelop.intersects(soilGeo.getEnvelopeInternal())){
                                                    geoWithSoil.add(new GeoWithSoilType(soilGeo, soilType));
                                                }
                                            
                                            }
                                        
                                        }
                                        
					PropagationProcessData threadData = new PropagationProcessData(
							vertices,null, triangles, freeFieldFinder, sourcesIndex,
							sourceGeometries, wj_sources, db_field_freq,
							reflexionOrder, diffractionOrder, maxSrcDist,maxRefDist,
							minRecDist, wallAlpha, ij, dsf,
							pmManager.nextSubProcess(vertices.size()), geoWithSoil);
					PropagationProcess propaProcess = new PropagationProcess(
							threadData, threadDataOut);

					if (doMultiThreading) {
						logger.info("Wait for free Thread to begin propagation of cell "
								+ (cellI + 1)
								+ ","
								+ (cellJ + 1)
								+ " of the "
								+ gridDim
								+ "x" + gridDim + "  grid..");
						// threadManager.executeBlocking(propaProcess);
						while (!threadManager.hasAvaibleQueueSlot()) {
							if (pm!=null && pm.isCancelled()) {
								driver.writingFinished();
								return driver.getTable("main");
							}
							Thread.sleep(100);
						}
						threadManager.execute(propaProcess);
						logger.info("Processing enqueued"); // enqueued
					} else {
						propaProcess.run();
					}
				}
			}
			// Wait termination of processes
			logger.info("Wait for termination of the lasts propagation process..");
			// threadManager.getRemainingTasks()>0
			Thread.sleep(100);
			while (threadDataOut.getCellComputed() < nbcell && doMultiThreading) {
				if (pm!=null && pm.isCancelled()) {
					driver.writingFinished();
					return driver.getTable("main");
				}
				Thread.sleep(100);
			}
			// Wait for rows stack to be empty
			driverManager.stopWatchingStack();
			pmManager.stop();
			logger.info("Wait for termination of writing to the driver..");
			while (driverManager.isRunning()) {
				if (pm!=null && pm.isCancelled()) {
					driver.writingFinished();
					return driver.getTable("main");
				}
				Thread.sleep(10);
			}
                        threadManager.shutdown();
			driver.writingFinished();
                        driver.open();
			logger.info("Parse polygons time:" + this.totalParseBuildings
					+ " ms");
			logger.info("Delaunay time:" + this.totalDelaunay + " ms");
                        logger.info("Min Max Avg computation time by receiver : "+ (threadDataOut.getMinimalReceiverComputationTime()/1e6) +" ms to "+ (threadDataOut.getMaximalReceiverComputationTime()/1e6)+" ms. Avg :"+(threadDataOut.getSumReceiverComputationTime()/(nbreceivers*1e6))+" ms.");
			logger.info("Receiver count:" + nbreceivers);
			logger.info("Receiver-Source count:"
					+ threadDataOut.getNb_couple_receiver_src());
                        logger.info("Receiver image (reflections):"
                                        + threadDataOut.getNb_image_receiver());
                        logger.info("Receiver-Sources specular reflection path count:"
                                        + threadDataOut.getNb_reflexion_path());
                        logger.info("Receiver-Source diffraction path count:"+threadDataOut.getNb_diffraction_path());
			logger.info("Buildings obstruction test count:"
					+ threadDataOut.getNb_obstr_test());
			return driver.getTable("main");
		} catch (DriverLoadException e) {
			throw new FunctionException(e);
		} catch (DriverException e) {
			throw new FunctionException(e);
		} catch (LayerDelaunayError e) {
			throw new FunctionException(e);
		} catch (InterruptedException e) {
			throw new FunctionException(e);
		} finally {
                    //Stop threads if there are not stoped
                    if(pmManager!=null) {
                        pmManager.stop();
                    }
                    if(threadManager!=null) {
                        threadManager.shutdown();
                    }
                    if(driverManager!=null) {
                        driverManager.stopWatchingStack();
                    }
                }
	}
        
        @Override
	public String getName() {
		return "BR_TriGridAreas";
	}
}
