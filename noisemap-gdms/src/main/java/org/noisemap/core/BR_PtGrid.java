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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.NoSuchTableException;
import org.gdms.data.indexes.DefaultSpatialIndexQuery;
import org.gdms.data.indexes.IndexException;
import org.gdms.data.indexes.IndexManager;
import org.gdms.data.indexes.IndexQueryException;
import org.gdms.data.indexes.tree.IndexVisitor;
import org.gdms.data.schema.DefaultMetadata;
import org.gdms.data.schema.Metadata;
import org.gdms.data.schema.MetadataUtilities;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.driver.DataSet;
import org.gdms.driver.DiskBufferDriver;
import org.gdms.driver.DriverException;
import org.gdms.driver.driverManager.DriverLoadException;
import org.gdms.sql.function.FunctionException;
import org.gdms.sql.function.FunctionSignature;
import org.gdms.sql.function.ScalarArgument;
import org.gdms.sql.function.table.AbstractTableFunction;
import org.gdms.sql.function.table.TableArgument;
import org.gdms.sql.function.table.TableDefinition;
import org.gdms.sql.function.table.TableFunctionSignature;
import org.orbisgis.noisemap.core.FastObstructionTest;
import org.orbisgis.noisemap.core.LayerDelaunayError;
import org.orbisgis.noisemap.core.MeshBuilder;
import org.orbisgis.noisemap.core.PropagationProcess;
import org.orbisgis.noisemap.core.PropagationProcessData;
import org.orbisgis.noisemap.core.PropagationProcessOut;
import org.orbisgis.noisemap.core.PropagationResultPtRecord;
import org.orbisgis.noisemap.core.QueryGeometryStructure;
import org.orbisgis.noisemap.core.QueryQuadTree;
import org.orbisgis.noisemap.core.RowsUnionClassification;
import org.orbisgis.progress.ProgressMonitor;

/**
 * Evaluate the sound level at each coordinate specified in parameters.
 * This function doesn't make a noise map but is useful to get the
 * sound level at some coordinates.
 * TODO subdivision level is useless, subdivision level can be computed from 
 * the maximum propagation distance and computation bounding box
 * 
 * @author Nicolas Fortin
 */
public class BR_PtGrid extends AbstractTableFunction {
        private Logger logger = Logger.getLogger(BR_TriGrid.class.getName());
	@Override
	public Metadata getMetadata(Metadata[] tables) throws DriverException {
		final Metadata metadata = tables[2];
		// we don't want the resulting Metadata to be constrained !
		final int fieldCount = metadata.getFieldCount();
		final Type[] fieldsTypes = new Type[fieldCount+2];
		final String[] fieldsNames = new String[fieldCount+2];

		for (int fieldId = 0; fieldId < fieldCount; fieldId++) {
			fieldsNames[fieldId] = metadata.getFieldName(fieldId);
			final Type tmp = metadata.getFieldType(fieldId);
			fieldsTypes[fieldId] = TypeFactory.createType(tmp.getTypeCode());
		}
		fieldsNames[fieldCount]="db_m";
		fieldsTypes[fieldCount]=TypeFactory.createType(Type.DOUBLE);
		fieldsNames[fieldCount+1]="cellid";
		fieldsTypes[fieldCount+1]=TypeFactory.createType(Type.INT);
		return new DefaultMetadata(fieldsTypes, fieldsNames);
	}

    @Override
    public FunctionSignature[] getFunctionSignatures() {
		// Builds geom , sources.the_geom, Receivers.the_geom, sources.db_m ,max propa dist , subdiv
		// lev ,roads width , receiv road dis,max tri area ,sound refl o,sound
		// dif order,wall alpha
            return new FunctionSignature[]{
                            new TableFunctionSignature(TableDefinition.GEOMETRY,
                            new TableArgument(TableDefinition.GEOMETRY), //Buildings
                            new TableArgument(TableDefinition.GEOMETRY), //Sources
                            new TableArgument(TableDefinition.GEOMETRY), //Receivers
                            ScalarArgument.STRING, //db_m field source name
                            ScalarArgument.DOUBLE, //maximum propagation distance
                            ScalarArgument.DOUBLE, //Wall dist seek
                            ScalarArgument.INT,    //Subdivision level
                            ScalarArgument.INT,    //Sound reflection order
                            ScalarArgument.INT,    //Sound diffraction order
                            ScalarArgument.DOUBLE) //alpha of wall
                    };
    }

    @Override
    public String getName() {
            return "BR_PtGrid";
    }

    @Override
    public String getSqlOrder() {
            return "select * from BR_PtGrid(buildings table(polygons),sources table(points),receivers table(points),sound lvl field name(string),maximum propagation distance (double meter),maximum wall seeking distance (double meter),subdivision level 4^n cells(int), sound reflection order(int, recommended 2), sound diffraction order(int, recommended 1), absorption alpha of walls (double));";
    }

    @Override
    public String getDescription() {
            return "BR_PtGrid(buildings(polygons),sources(points),receivers(points),sound lvl field name(string),maximum propagation distance (double meter),maximum wall seeking distance (double meter),subdivision level 4^n cells(int), sound reflection order, sound diffraction order, alpha of walls ) Sound propagation from ponctual sound sources to specified ponctual receivers .";
    }
    /**
     * Set the logger for object message
     * @param logger
     */
    public void setLogger(Logger logger) {
        this.logger = logger;
    }
    @Override
    public DataSet evaluate(DataSourceFactory sqldsf, DataSet[] tables, Value[] values, ProgressMonitor pm) throws FunctionException {
                boolean useGeometryIndex = false; //Use gdms geometry index for source and buildings parsing
                IndexManager im =null;
                if(useGeometryIndex) {
                    im = sqldsf.getIndexManager();
                }
                
                if(values.length<7) {
                    throw new FunctionException("Not enough parameters !");
                }else if(values.length>7){
                    throw new FunctionException("Too many parameters !");
                }
		String dbField = values[0].toString();
		double maxSrcDist = values[1].getAsDouble();
                double maxRefDist = values[2].getAsDouble();
		int subdivLvl = values[3].getAsInt();
		int reflexionOrder = values[4].getAsInt();
		int diffractionOrder = values[5].getAsInt();
		double wallAlpha = values[6].getAsDouble();
		boolean doMultiThreading = true;
                assert(maxSrcDist>maxRefDist); //Maximum Source-Receiver
                                               //distance must be superior than
                                               //maximum Receiver-Wall distance
                ThreadPool threadManager = null;
                ProgressionOrbisGisManager pmManager=null;
                PropagationProcessDiskWriter driverManager=null;
		try {
			// Steps of execution
			// Evaluation of the main bounding box (receivers+max dist propagation)
			// Split domain into 4^subdiv cells
			// For each cell :
			// Expand bounding box cell by maxSrcDist
			// Save the list of sources index inside the extended bounding box
			// Save the list of buildings index inside the extended bounding box
			// Find all sources within maxSrcDist
			int tableBuildings = 0;
			int tableReceivers = 2;
			int tableSources = 1;

			// Load Sources and Buildings table drivers
			final DataSet sds = tables[tableBuildings];
			final DataSet sdsSources = tables[tableSources];
                        final DataSet sdsReceivers = tables[tableReceivers];

			long nbreceivers = sdsReceivers.getRowCount();
			// Set defaultGeom as the geom set by the user
			int spatialBuildingsFieldIndex = MetadataUtilities.getSpatialFieldIndex(sds.getMetadata());
			int spatialSourceFieldIndex = MetadataUtilities.getSpatialFieldIndex(sdsSources.getMetadata());
                        int spatialReceiversFieldIndex= MetadataUtilities.getSpatialFieldIndex(sdsReceivers.getMetadata());
                        String spatialSourceFieldName =sdsSources.getMetadata().getFieldName(spatialSourceFieldIndex);
                        String spatialBuildingsFieldName = sds.getMetadata().getFieldName(spatialBuildingsFieldIndex);                        
                        
                        //Initialize geometry index
                        if(useGeometryIndex) {
                            //Index of Sound Sources Table
                            if(!im.isIndexed(sdsSources,spatialSourceFieldName)) {
                                im.buildIndex(sdsSources, spatialSourceFieldName, null);
                            }
                            //Index of Buildings Table
                            if(!im.isIndexed(sds,spatialBuildingsFieldName)) {
                                im.buildIndex(sds,spatialBuildingsFieldName,null);
                            }
                        }
			// 1 Step - Evaluation of the main bounding box (receivers)
			Envelope mainEnvelope = TriGrid.GetGlobalEnvelope(sdsReceivers, pm);
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

			DiskBufferDriver driver = new DiskBufferDriver(sqldsf, this.getMetadata(new Metadata[] {sds.getMetadata(),sdsSources.getMetadata(), sdsReceivers.getMetadata() }));

			int nbcell = gridDim * gridDim;
			if (nbcell == 1) {
                		doMultiThreading = false;
			}

			Runtime runtime = Runtime.getRuntime();
			threadManager = new ThreadPool(
					runtime.availableProcessors(),
					runtime.availableProcessors() + 1, Long.MAX_VALUE,
                    			TimeUnit.SECONDS);

			pmManager = new ProgressionOrbisGisManager(
					nbreceivers, pm);
			Stack<PropagationResultPtRecord> toDriver = new Stack<PropagationResultPtRecord>();
			driverManager = new PropagationProcessDiskWriter(
					null,toDriver, driver,sdsReceivers);
			driverManager.start();
			pmManager.start();
			PropagationProcessOut threadDataOut = new PropagationProcessOut(
					null,toDriver);

			for (int cellI = 0; cellI < gridDim; cellI++) {
				for (int cellJ = 0; cellJ < gridDim; cellJ++) {
					Envelope cellEnvelope = TriGrid.getCellEnv(mainEnvelope, cellI,
							cellJ, gridDim, gridDim, cellWidth, cellHeight);// new
																			// Envelope(mainEnvelope.getMinX()+cellI*cellWidth,
					//Collect the list and index of receivers on the cell
                                        List<Coordinate> cellReceivers = new ArrayList<Coordinate>();
                                        List<Long> cellReceiversRowId = new ArrayList<Long>();

                                        long receiversRowCount = sdsReceivers.getRowCount();
                                        for (long rowIndex = 0; rowIndex < receiversRowCount; rowIndex++) {

                                                Geometry geo = sdsReceivers.getFieldValue(rowIndex,spatialReceiversFieldIndex).getAsGeometry();
                                                Envelope ptEnv = geo.getEnvelopeInternal();
                                                if (ptEnv.intersects(cellEnvelope)) {
                                                    cellReceivers.add(geo.getCoordinate());
                                                    cellReceiversRowId.add(rowIndex);
                                                }
                                        }
                                        if(!cellReceivers.isEmpty()) {
                                            MeshBuilder mesh = new MeshBuilder();
                                            int ij = cellI * gridDim + cellJ;
                                            logger.info("Begin processing of cell " + (cellI+1) + ","
                                                            + (cellJ+1) + " of the " + gridDim + "x" + gridDim
                                                            + "  grid..");
                                            if (pm!=null && pm.isCancelled()) {
                                                    driver.writingFinished();
                                                    return driver.getTable("main");
                                            }
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
                                            //Make the Geometry Index request of Buildings
                                            RowsUnionClassification buildingsRowsToFetch;
                                            if(useGeometryIndex) {
                                                DefaultSpatialIndexQuery buildingsSpatialIndexQuery = new DefaultSpatialIndexQuery(spatialBuildingsFieldName,expandedCellEnvelop);
                                                RowVisitor visitor = new RowVisitor();
                                                im.queryIndex(sds, buildingsSpatialIndexQuery,visitor);
                                                buildingsRowsToFetch = visitor.getVisitedRows();
                                            } else {
                                                buildingsRowsToFetch = new RowsUnionClassification(0,(int)sds.getRowCount()-1);
                                            }
                                            //Make the Geometry Index request of Sound Sources
                                            RowsUnionClassification sourcesRowsToFetch;
                                            if(useGeometryIndex) {
                                                DefaultSpatialIndexQuery sourcesSpatialIndexQuery = new DefaultSpatialIndexQuery(spatialSourceFieldName,expandedCellEnvelop);
                                                RowVisitor visitor = new RowVisitor();
                                                im.queryIndex(sdsSources, sourcesSpatialIndexQuery,visitor);
                                                sourcesRowsToFetch = visitor.getVisitedRows();
                                            } else {
                                                sourcesRowsToFetch = new RowsUnionClassification(0,(int)sdsSources.getRowCount()-1);
                                            }
                                            
                                            
                                            
                                            Iterator<Integer> itSrcRows = sourcesRowsToFetch.getRowRanges();
                                            while(itSrcRows.hasNext()) {
                                                int rbegin=itSrcRows.next();
                                                int rend=itSrcRows.next();
                                                Integer idsource = 0;
                                                for (long rowIndex = rbegin; rowIndex <= rend; rowIndex++) {

                                                        final Value[] row =sdsSources.getRow(rowIndex);
                                                        Geometry geo = row[spatialSourceFieldIndex].getAsGeometry();
                                                        Envelope ptEnv = geo.getEnvelopeInternal();
                                                        if (ptEnv.intersects(expandedCellEnvelop)) {
                                                                sourcesIndex.appendGeometry(geo, idsource);
                                                                ArrayList<Double> wj_spectrum = new ArrayList<Double>();
                                                                wj_spectrum.ensureCapacity(db_field_ids.size());
                                                                for (Integer idcol : db_field_ids) {
                                                                        wj_spectrum.add(TriGrid.DbaToW(row[idcol].getAsDouble()));
                                                                }
                                                                wj_sources.add(wj_spectrum);
                                                                sourceGeometries.add(geo);
                                                                idsource++;
                                                        }
                                                }
                                            }

                                            // //////////////////////////////////////////////////////
                                            // feed freeFieldFinder for fast intersection query
                                            // optimization
                                            Iterator<Integer> itBuildingsRows = buildingsRowsToFetch.getRowRanges();
                                            while(itBuildingsRows.hasNext()) {
                                                int rbegin=itBuildingsRows.next();
                                                int rend=itBuildingsRows.next();
                                                for (long rowIndex = rbegin; rowIndex <= rend; rowIndex++) {
                                                        final Geometry geometry = sds.getFieldValue(rowIndex, spatialBuildingsFieldIndex).getAsGeometry();
                                                        Envelope geomEnv = geometry.getEnvelopeInternal();
                                                        if (expandedCellEnvelop.intersects(geomEnv)) {
                                                                mesh.addGeometry(geometry);
                                                        }
                                                }
                                            }
                                            mesh.finishPolygonFeeding(expandedCellEnvelop);
                                            FastObstructionTest freeFieldFinder=new FastObstructionTest(mesh.getPolygonWithHeight(),mesh.getTriangles(),mesh.getTriNeighbors(),mesh.getVertices());
                                            
                                            PropagationProcessData threadData = new PropagationProcessData(
                                                            cellReceivers,cellReceiversRowId, null, freeFieldFinder, sourcesIndex,
                                                            sourceGeometries, wj_sources, db_field_freq,
                                                            reflexionOrder, diffractionOrder, maxSrcDist,maxRefDist,
                                                            1., wallAlpha, ij,
                                                            pmManager.getRootProgress(),null);
                                            PropagationProcess propaProcess = new PropagationProcess(
                                                            threadData, threadDataOut);

                                            if (doMultiThreading) {
                                                    if(!threadManager.hasAvaibleQueueSlot()) {
                                                        logger.info("Wait for free Thread to begin propagation of cell "
                                                                        + (cellI + 1)
                                                                        + ","
                                                                        + (cellJ + 1)
                                                                        + " of the "
                                                                        + gridDim
                                                                        + "x" + gridDim + "  grid..");
                                                    }
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
			}
			// Wait termination of processes
			Thread.sleep(100);
                        if(threadManager.getRemainingTasks() > 0) {
                            logger.info("Wait for termination of the lasts propagation process..");
                        }
			while (threadManager.getRemainingTasks() > 0) {
				if (pm!=null && pm.isCancelled()) {
					driver.writingFinished();
					return driver.getTable("main");
				}
				Thread.sleep(100);
			}
			Thread.sleep(100);
			// Wait for rows stack to be empty
			driverManager.stopWatchingStack();
			pmManager.stop();
                        if(driverManager.isRunning()) {
                            logger.info("Wait for termination of writing to the driver..");
                        }
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
                } catch (NoSuchTableException e)  {
			throw new FunctionException(e);   
                } catch (IndexException e)  {
			throw new FunctionException(e); 
                } catch (IndexQueryException e)  {
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
    private class RowVisitor implements IndexVisitor {
        RowsUnionClassification visitedRows = new RowsUnionClassification();
        @Override
        public void visitElement(int row, Object env) {
            visitedRows.addRow(row);
        }
        /**
         * 
         * @return Query result
         */
        public RowsUnionClassification getVisitedRows() {
            return visitedRows;
        }
        
    }
}
