/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

package org.noisemap.core;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Coordinate;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

import org.gdms.data.SQLDataSourceFactory;
import org.gdms.data.schema.DefaultMetadata;
import org.gdms.data.schema.Metadata;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.schema.MetadataUtilities;
import org.gdms.driver.DiskBufferDriver;
import org.gdms.driver.DataSet;
import org.gdms.driver.DriverException;
import org.gdms.driver.driverManager.DriverLoadException;
import org.gdms.sql.function.FunctionException;
import org.gdms.sql.function.FunctionSignature;
import org.gdms.sql.function.ScalarArgument;
import org.gdms.sql.function.table.AbstractTableFunction;
import org.gdms.sql.function.table.TableArgument;
import org.gdms.sql.function.table.TableDefinition;
import org.gdms.sql.function.table.TableFunctionSignature;
import org.orbisgis.progress.ProgressMonitor;

import org.apache.log4j.Logger;

/**
 * Evaluate the sound level at each coordinate specified in parameters.
 * This function doesn't make a noise map but is usefull to quickely get the
 * sound level at some coordinates.
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
    public DataSet evaluate(SQLDataSourceFactory sqldsf, DataSet[] tables, Value[] values, ProgressMonitor pm) throws FunctionException {

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


			// 1 Step - Evaluation of the main bounding box (receivers)
			Envelope mainEnvelope = BR_TriGrid.GetGlobalEnvelope(sdsReceivers, pm);
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
			ThreadPool threadManager = new ThreadPool(
					runtime.availableProcessors(),
					runtime.availableProcessors() + 1, Long.MAX_VALUE,
                    			TimeUnit.SECONDS);

			ProgressionOrbisGisManager pmManager = new ProgressionOrbisGisManager(
					nbreceivers, pm);
			Stack<PropagationResultPtRecord> toDriver = new Stack<PropagationResultPtRecord>();
			PropagationProcessDiskWriter driverManager = new PropagationProcessDiskWriter(
					null,toDriver, driver,sdsReceivers);
			driverManager.start();
			pmManager.start();
			PropagationProcessOut threadDataOut = new PropagationProcessOut(
					null,toDriver);

			for (int cellI = 0; cellI < gridDim; cellI++) {
				for (int cellJ = 0; cellJ < gridDim; cellJ++) {
					Envelope cellEnvelope = BR_TriGrid.getCellEnv(mainEnvelope, cellI,
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
                                            FastObstructionTest freeFieldFinder = new FastObstructionTest();
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
                                            QueryGeometryStructure<Integer> sourcesIndex = new QueryGridIndex<Integer>(
                                                            expandedCellEnvelop, 16, 16);
                                            long rowCount = sdsSources.getRowCount();
                                            int fieldCount = sdsSources.getMetadata().getFieldCount();
                                            Integer idsource = 0;
                                            for (long rowIndex = 0; rowIndex < rowCount; rowIndex++) {

                                                    final Value[] row =sdsSources.getRow(rowIndex);
                                                    Geometry geo = row[spatialSourceFieldIndex].getAsGeometry();
                                                    Envelope ptEnv = geo.getEnvelopeInternal();
                                                    if (ptEnv.intersects(expandedCellEnvelop)) {
                                                            sourcesIndex.appendGeometry(geo, idsource);
                                                            ArrayList<Double> wj_spectrum = new ArrayList<Double>();
                                                            wj_spectrum.ensureCapacity(db_field_ids.size());
                                                            for (Integer idcol : db_field_ids) {
                                                                    wj_spectrum.add(BR_TriGrid.DbaToW(row[idcol].getAsDouble()));
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
                                                    Envelope geomEnv = geometry.getEnvelopeInternal();
                                                    if (expandedCellEnvelop.intersects(geomEnv)) {
                                                            freeFieldFinder.addGeometry(geometry);
                                                    }
                                            }

                                            freeFieldFinder.finishPolygonFeeding(expandedCellEnvelop);

                                            PropagationProcessData threadData = new PropagationProcessData(
                                                            cellReceivers,cellReceiversRowId, null, freeFieldFinder, sourcesIndex,
                                                            sourceGeometries, wj_sources, db_field_freq,
                                                            reflexionOrder, diffractionOrder, maxSrcDist,maxRefDist,
                                                            1., wallAlpha, ij, sqldsf,
                                                            pmManager.getRootProgress());
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
                                                    while (!threadManager.isAvaibleQueueSlot()) {
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
                        driver.start();
			logger.info("Building source-receiver obstruction test time:"
					+ threadDataOut.getTotalBuildingObstructionTest() + " ms");
			logger.info("Reflexion computing time:"
					+ threadDataOut.getTotalReflexionTime() + " ms");
			logger.info("Source query and optimisation time:"
					+ threadDataOut.getSourceSplittingTime() + " ms");
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
		}
    }

}
