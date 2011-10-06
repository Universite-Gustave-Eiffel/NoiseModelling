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
import org.gdms.data.schema.MetadataUtilities;
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
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Set the right table row id to each left table rows from the nearest geometry,
 * add also the column AvgDist corresponding to the average distance between the
 * left and the right's nearest geometry found. -1 if nothing has been found in
 * the region of the left geometry.
 */

public class ST_SetNearestGeometryId extends AbstractTableFunction {

	@Override
	public String getName() {
		return "ST_SetNearestGeometryId";
	}

	@Override
	public String getSqlOrder() {
		return "select * from ST_SetNearestGeometryId( left_table,right_table, left_table.geomToUpdate, right_table.geomSource, \"right_table_rowId_Label\" );";
	}

	@Override
	public String getDescription() {
		return "Set the right table row id to each left table rows from the nearest geometry, add also the column AvgDist corresponding to the average distance between the left and the right's nearest geometry found. -1 if nothing has been found in the region of the left geometry.";
	}

	@Override
	public DataSet evaluate(SQLDataSourceFactory dsf, DataSet[] tables,
            Value[] values, ProgressMonitor pm) throws FunctionException {
		try {
			ProgressionOrbisGisManager progManager=new ProgressionOrbisGisManager(2, pm);
			// Declare source and Destination tables
			final DataSet sds = tables[0];
			final DataSet sdsSource = tables[1];

			// Set defaultGeom as the geom set by the user
			String spatialUpdateFieldName = values[0].toString();
			String spatialSourceFieldName = values[1].toString();
			String idSourceFieldName = values[2].toString();

			int spatialUpdateFieldIndex = sds.getMetadata().getFieldIndex(spatialUpdateFieldName);
			int spatialSourceFieldIndex = sdsSource.getMetadata().getFieldIndex(spatialSourceFieldName);
			final int idSourceNum = sdsSource.getMetadata().getFieldIndex(idSourceFieldName);

			DefaultMetadata metadata = new DefaultMetadata(sds.getMetadata());
			String field = MetadataUtilities.getUniqueFieldName(metadata,
					idSourceFieldName);
			metadata.addField(field,
					sdsSource.getMetadata().getFieldType(idSourceNum));
			String fieldDist = MetadataUtilities.getUniqueFieldName(metadata,
					"avgDist");
			metadata.addField(fieldDist, TypeFactory.createType(Type.FLOAT));
			final DiskBufferDriver driver = new DiskBufferDriver(dsf, metadata);

			final long rowCount = sds.getRowCount();
			final long rowSourceCount = sdsSource.getRowCount();
			// First Loop
			// Build QuadTree from Source Geometry
			Quadtree quadtree = new Quadtree();
			ProgressionProcess quadprocess=progManager.nextSubProcess(rowSourceCount);
			for (long rowIndex = 0; rowIndex < rowSourceCount; rowIndex++) {
				quadprocess.nextSubProcessEnd();
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
			// Appends Rows With nearest right table row index
			ProgressionProcess queryprocess=progManager.nextSubProcess(rowSourceCount);
			for (long rowIndex = 0; rowIndex < rowCount; rowIndex++) {
				queryprocess.nextSubProcessEnd();

				if (pm.isCancelled()) {
					break;
				}

				// Find the nearest row id information with the min avg dist
				final Geometry geometry = sds.getFieldValue(rowIndex,spatialUpdateFieldIndex).getAsGeometry();
				QuadtreeNearestFilter zFilter = new QuadtreeNearestFilter(
						quadtree, sdsSource, spatialSourceFieldIndex, geometry.getEnvelopeInternal());
				geometry.apply(zFilter);

				int fieldCount = sds.getMetadata().getFieldCount();
				final Value[] newValues = new Value[fieldCount + 2];
				
                for (int j = 0; j < fieldCount; j++) {
                	newValues[j] = sds.getFieldValue(rowIndex, j);
                }
	
				
				// Set the two new columns values
				long nearest_id = zFilter.getNearestId();
				if (nearest_id != -1) {
					newValues[newValues.length - 2] = ValueFactory
							.createValue(sdsSource.getFieldValue(nearest_id,
									idSourceNum).getAsLong());
					newValues[newValues.length - 1] = ValueFactory
							.createValue(zFilter.getAvgDist());
				} else {
					newValues[newValues.length - 2] = ValueFactory
							.createValue(-1);
					newValues[newValues.length - 1] = ValueFactory
							.createValue(0.);
				}
				// Append row
				driver.addValues(newValues);

			}
			driver.writingFinished();
                           driver.start();
			return driver.getTable("main");
		} catch (DriverLoadException e) {
			throw new FunctionException(e);
		} catch (DriverException e) {
			throw new FunctionException(e);
		}
	}

	private class QuadtreeNearestFilter implements CoordinateSequenceFilter {
		private boolean done = false;
		private ArrayList<Geometry> nearestGeometry = new ArrayList<Geometry>();
		private ArrayList<Long> geometryScoreCount = new ArrayList<Long>();
		private ArrayList<Double> geometryScoreSumDist = new ArrayList<Double>();

		@SuppressWarnings("unchecked")
		public QuadtreeNearestFilter(Quadtree quadtree,
				DataSet sdsSource,int geoFieldIndex,Envelope geomArea) {
			super();
			// Find coordinates under the distance of the geom

			List<EnvelopeWithIndex<Long>> list = quadtree.query(geomArea);
			for (EnvelopeWithIndex<Long> env_with_index : list) {

				try {
					if ((env_with_index.intersects(geomArea))) {
						Geometry geometry = sdsSource
								.getFieldValue(env_with_index.getId(),geoFieldIndex).getAsGeometry();
						geometry.setUserData(Long.valueOf(env_with_index
								.getId()));
						nearestGeometry.add(geometry);
						geometryScoreCount.add(0L);
						geometryScoreSumDist.add(0.);
					}
				} catch (DriverException e) {
					e.printStackTrace();
					done = true;
				}
			}
			if (list.isEmpty() || nearestGeometry.isEmpty()) {
				done = true;
			}
		}

		private int getNearestGeometryLocalId() {
			int nearest_id = -1;
			double nearest_dist = Double.MAX_VALUE;
			for (int localIndex = 0; localIndex < nearestGeometry.size(); localIndex++) {
				double curdist = geometryScoreSumDist.get(localIndex);
				long curcount = geometryScoreCount.get(localIndex);
				if (curcount > 0 && curdist / curcount < nearest_dist) {
					nearest_dist = curdist / curcount;
					nearest_id = localIndex;
				}
			}
			return nearest_id;
		}

		public long getNearestId() {
			int nearestLocaleId = getNearestGeometryLocalId();
			if (nearestLocaleId >= 0) {
				return (Long) nearestGeometry.get(nearestLocaleId)
						.getUserData();
			} else {
				return -1;
			}
		}

		public double getAvgDist() {
			int nearestLocaleId = getNearestGeometryLocalId();
			if (nearestLocaleId >= 0) {
				double curdist = geometryScoreSumDist.get(nearestLocaleId);
				long curcount = geometryScoreCount.get(nearestLocaleId);
				return curdist / curcount;
			} else {
				return Double.MAX_VALUE;
			}
		}

		@Override
		public void filter(CoordinateSequence seq, int i) {
			Coordinate seq_pt = seq.getCoordinate(i);
			// Keep only nodes that are inside the area
			// and keep the nearest
			for (int localIndex = 0; localIndex < nearestGeometry.size(); localIndex++) {
				Geometry geo = nearestGeometry.get(localIndex);
				double curDist = geo.distance(GeometryFactory
						.createPointFromInternalCoord(seq_pt, geo));
				geometryScoreCount.set(localIndex,
						geometryScoreCount.get(localIndex) + 1);
				geometryScoreSumDist.set(localIndex,
						geometryScoreSumDist.get(localIndex) + curDist);
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
                            ScalarArgument.STRING)
                    };
    }
}