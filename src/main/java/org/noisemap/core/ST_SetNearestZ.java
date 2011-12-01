/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

package org.noisemap.core;

import java.util.ArrayList;
import java.util.List;

import org.gdms.data.SQLDataSourceFactory;
import org.gdms.data.schema.DefaultMetadata;
import org.gdms.data.schema.Metadata;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DriverException;
import org.gdms.driver.DataSet;
import org.gdms.driver.driverManager.DriverLoadException;
import org.gdms.sql.function.FunctionException;
import org.gdms.sql.function.FunctionSignature;
import org.gdms.sql.function.ScalarArgument;
import org.gdms.sql.function.table.AbstractTableFunction;
import org.gdms.sql.function.table.TableArgument;
import org.gdms.sql.function.table.TableDefinition;
import org.gdms.sql.function.table.TableFunctionSignature;
import org.gdms.driver.DiskBufferDriver;
import org.orbisgis.progress.ProgressMonitor;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFilter;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import org.gdms.data.schema.MetadataUtilities;

public class ST_SetNearestZ extends AbstractTableFunction {
	private class QuadtreeZFilter implements CoordinateSequenceFilter {
		private boolean done = false;
		private final double maxDist;
		private boolean outOfBoundsDestinationGeomtry = false; // If one
																// coordinates
																// of the
																// geometry did
																// not find a
																// source Z
																// coordinate
		private ArrayList<Coordinate> nearestCoordinates = new ArrayList<Coordinate>();

		@SuppressWarnings("unchecked")
		public QuadtreeZFilter(Quadtree quadtree, final double maxDist,
				DataSet sdsSource,int geoFieldIndex, Envelope geomArea) {
			super();
			this.maxDist = maxDist;
			// Find coordinates under the distance of the geom
			geomArea.expandBy(maxDist);
			List<EnvelopeWithIndex<Long>> list = quadtree.query(geomArea);
			for (EnvelopeWithIndex<Long> env_with_index : list) {

				try {
					if ((env_with_index.intersects(geomArea))) {
						Geometry geometry = sdsSource
								.getFieldValue(env_with_index.getId(),geoFieldIndex).getAsGeometry();
						Coordinate[] points = geometry.getCoordinates();
						for (int ptindex = 0; ptindex < points.length; ptindex++) {
							Coordinate coord = points[ptindex];
							if (geomArea.contains(coord)) {
								nearestCoordinates.add(coord);
							}
						}
					}
				} catch (DriverException e) {
					e.printStackTrace();
					done = true;
				}
			}
			if (list.isEmpty() || nearestCoordinates.isEmpty()) {
				// There is no Z information in this area. We skip this line
				outOfBoundsDestinationGeomtry = true;
				done = true;
			}
		}

		/**
		 * If one of the coordinate to update did not find the nearest Z
		 * coordinate this method return True
		 */
		public boolean isOneCoordOutOfSource() {
			return outOfBoundsDestinationGeomtry;
		}

		@Override
		public void filter(CoordinateSequence seq, int i) {
			double x = seq.getX(i);
			double y = seq.getY(i);
			Coordinate seq_pt = seq.getCoordinate(i);
			seq.setOrdinate(i, 0, x);
			seq.setOrdinate(i, 1, y);

			// Keep only nodes that are inside the area
			// and keep the nearest
			Envelope area = new Envelope(x - maxDist, x + maxDist, y - maxDist,
					y + maxDist);
			boolean isFound = false;
			double nearest_z = 0;
			double nearest_dist = 2 * maxDist;
			for (Coordinate coord : nearestCoordinates) {
				if (area.contains(coord)) {
					double curDist = coord.distance(seq_pt);
					if (curDist < nearest_dist && curDist < maxDist
							&& coord.z > -99) {
						nearest_dist = curDist;
						nearest_z = coord.z;
						isFound = true;
					}
				}

			}
			if (isFound) {
				seq.setOrdinate(i, 2, nearest_z);
			} else {
				outOfBoundsDestinationGeomtry = true;
			}

			if (i == seq.size() - 1) {
				done = true;
			}
		}

		@Override
		public boolean isDone() {
			return done;
		}

		@Override
		public boolean isGeometryChanged() {
			return done;
		}
	}
	@Override
	public String getName() {
		return "ST_SetNearestZ";
	}

	@Override
	public String getSqlOrder() {
		return "ST_SetNearestZ( left_table,right_table,'the_geom','the_geom', MaximumDistance )";
	}

	@Override
	public String getDescription() {
		return "Read the Z information from the right table, then apply to the left table nearest geometry, destroy the geom line if there is no Z information in the maximum distance parameter.";
	}

	@Override
	public DataSet evaluate(SQLDataSourceFactory dsf, DataSet[] tables,
            Value[] values, ProgressMonitor pm) throws FunctionException {
                ProgressionOrbisGisManager progManager=new ProgressionOrbisGisManager(2, pm);
		try {
			
			final double maxDist;
			// Declare source and Destination tables
			final DataSet sds = tables[0];
			final DataSet sdsSource = tables[1];
			// Open source and Destination tables

                        int spatialUpdateFieldIndex,spatialSourceFieldIndex;

			// Set defaultGeom as the geom set by the user
                        if(values.length==3) {
                            final String spatialUpdateFieldName = values[0].toString();
                            final String spatialSourceFieldName = values[1].toString();
                            spatialUpdateFieldIndex = sds.getMetadata().getFieldIndex(spatialUpdateFieldName);
                            spatialSourceFieldIndex = sdsSource.getMetadata().getFieldIndex(spatialSourceFieldName);
                            maxDist = values[2].getAsDouble();
                        } else  {
                            spatialUpdateFieldIndex = MetadataUtilities.getSpatialFieldIndex(sds.getMetadata());
                            spatialSourceFieldIndex = MetadataUtilities.getSpatialFieldIndex(sdsSource.getMetadata());
                            maxDist = values[0].getAsDouble();
                        }


			final DiskBufferDriver driver = new DiskBufferDriver(dsf,
					sds.getMetadata());

			final long rowCount = sds.getRowCount();
			final long rowSourceCount = sdsSource.getRowCount();
			// First Loop
			// Build QuadTree from Source Geometry
			Quadtree quadtree = new Quadtree();
			ProgressionProcess quadprog=progManager.nextSubProcess(rowSourceCount);
			for (long rowIndex = 0; rowIndex < rowSourceCount; rowIndex++) {
				quadprog.nextSubProcessEnd();
				if (pm.isCancelled()) {
					break;
				}
				final Geometry geometry = sdsSource.getFieldValue(rowIndex,spatialSourceFieldIndex).getAsGeometry();
				quadtree.insert(
						geometry.getEnvelopeInternal(),
						new EnvelopeWithIndex<Long>(geometry
								.getEnvelopeInternal(), rowIndex));
			}

			// Second Loop
			// Appends Rows With modified Z values
			ProgressionProcess queryprog=progManager.nextSubProcess(rowCount);
			for (long rowIndex = 0; rowIndex < rowCount; rowIndex++) {

				queryprog.nextSubProcessEnd();
				if (pm.isCancelled()) {
					break;
				}

				// Find the nearest Z information within the maxDist
				final Geometry geometry = sds.getFieldValue(rowIndex,spatialUpdateFieldIndex).getAsGeometry();
				QuadtreeZFilter zFilter = new QuadtreeZFilter(quadtree,
						maxDist, sdsSource,spatialSourceFieldIndex, geometry.getEnvelopeInternal());
				geometry.apply(zFilter);

				if (!zFilter.isOneCoordOutOfSource()) // We skip this line if
														// there is no
														// information for at
														// least one of the
														// destination
														// coordinates
				{
					//final Value[] fieldsValues = sds. getRow(rowIndex);
					// If we found something within MaximumDistance units.
					int fieldCount = sds.getMetadata().getFieldCount();
					final Value[] newValues = new Value[fieldCount];
					
                    for (int j = 0; j < fieldCount; j++) {
                    	newValues[j] = sds.getFieldValue(rowIndex, j);
                    }
					// Update the geom
					newValues[spatialUpdateFieldIndex] = ValueFactory
							.createValue(geometry);
					// Append row
					driver.addValues(newValues);
				}
			}
			driver.writingFinished();
                        driver.start();
			return driver.getTable("main");
		} catch (DriverLoadException e) {
                        progManager.stop();
			throw new FunctionException(e);
		} catch (DriverException e) {
                        progManager.stop();
			throw new FunctionException(e);
		}
	}

	

	@Override
	public Metadata getMetadata(Metadata[] tables) throws DriverException {
		final Metadata metadata = tables[0];
		// we don't want the resulting Metadata to be constrained !
		final int fieldCount = metadata.getFieldCount();
		final Type[] fieldsTypes = new Type[fieldCount];
		final String[] fieldsNames = new String[fieldCount];

		for (int fieldId = 0; fieldId < fieldCount; fieldId++) {
			fieldsNames[fieldId] = metadata.getFieldName(fieldId);
			final Type tmp = metadata.getFieldType(fieldId);
			fieldsTypes[fieldId] = TypeFactory.createType(tmp.getTypeCode());
		}
		return new DefaultMetadata(fieldsTypes, fieldsNames);
	}


    @Override
    public FunctionSignature[] getFunctionSignatures() {
            return new FunctionSignature[]{
                            new TableFunctionSignature(TableDefinition.GEOMETRY,
                            new TableArgument(TableDefinition.GEOMETRY),
                            new TableArgument(TableDefinition.GEOMETRY),
                            ScalarArgument.DOUBLE),
                            new TableFunctionSignature(TableDefinition.GEOMETRY,
                            new TableArgument(TableDefinition.GEOMETRY),
                            new TableArgument(TableDefinition.GEOMETRY),
                            ScalarArgument.STRING, //'the_geom'
                            ScalarArgument.STRING, //'the_geom'
                            ScalarArgument.DOUBLE)
                    };
    }

}