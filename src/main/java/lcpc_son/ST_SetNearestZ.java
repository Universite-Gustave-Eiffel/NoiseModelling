/***********************************
 * ANR EvalPDU
 * Lcpc 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

package lcpc_son;

import java.util.ArrayList;
import java.util.List;

import org.gdms.data.DataSource;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.ExecutionException;
import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.data.metadata.DefaultMetadata;
import org.gdms.data.metadata.Metadata;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DriverException;
import org.gdms.driver.ObjectDriver;
import org.gdms.driver.driverManager.DriverLoadException;
import org.gdms.sql.customQuery.CustomQuery;
import org.gdms.sql.customQuery.TableDefinition;
import org.gdms.sql.function.Argument;
import org.gdms.sql.function.Arguments;
import org.gdms.driver.DiskBufferDriver;
import org.orbisgis.progress.IProgressMonitor;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFilter;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;


public class ST_SetNearestZ implements CustomQuery {

	public String getName() {
		return "ST_SetNearestZ";
	}

	public String getSqlOrder() {
		return "select ST_SetNearestZ( left_table.geomToUpdate, right_table.geomSource, MaximumDistance ) from left_table,right_table;";
	}

	public String getDescription() {
		return "Add or Update the Z information from the nearest geometry, destroy the geom line if there is no source information under the maximum distance parameter.";
	}

	public ObjectDriver evaluate(DataSourceFactory dsf, DataSource[] tables,
			Value[] values, IProgressMonitor pm) throws ExecutionException {
		try {
			final double maxDist=values[2].getAsDouble();
			//Declare source and Destination tables
			final SpatialDataSourceDecorator sds = new SpatialDataSourceDecorator(
					tables[0]);
			final SpatialDataSourceDecorator sdsSource = new SpatialDataSourceDecorator(
					tables[1]);
			//Open source and Destination tables			
			sds.open();
			sdsSource.open();

			//Set defaultGeom as the geom set by the user
			final String spatialUpdateFieldName = values[0].toString();
			final String spatialSourceFieldName = values[1].toString();
			sds.setDefaultGeometry(spatialUpdateFieldName);
			sdsSource.setDefaultGeometry(spatialSourceFieldName);
			final int spatialFieldIndex = sds.getSpatialFieldIndex();
			
			
			final DiskBufferDriver driver = new DiskBufferDriver(dsf, sds.getMetadata());

			final long rowCount = sds.getRowCount();
			final long rowSourceCount = sdsSource.getRowCount();
			//First Loop
			//Build QuadTree from Source Geometry
			Quadtree quadtree= new Quadtree();
			for (long rowIndex = 0; rowIndex < rowSourceCount; rowIndex++) {

				if (rowIndex / 50 == rowIndex / 50.0) {
					if (pm.isCancelled()) {
						break;
					} else {
						pm.progressTo((int) (50 * rowIndex / rowSourceCount));
					}
				}
				final Geometry geometry = sdsSource.getGeometry(rowIndex);
				quadtree.insert(geometry.getEnvelopeInternal(),new EnvelopeWithIndex<Long>(geometry.getEnvelopeInternal(),rowIndex));
			}

			//Second Loop
			//Appends Rows With modified Z values
			for (long rowIndex = 0; rowIndex < rowCount; rowIndex++) {

				if (rowIndex / 50 == rowIndex / 50.0) {
					if (pm.isCancelled()) {
						break;
					} else {
						pm.progressTo((int) (50 + 50 * rowIndex / rowCount));
					}
				}

				//Find the nearest Z information within the maxDist
				final Geometry geometry = sds.getGeometry(rowIndex);
				QuadtreeZFilter zFilter = new QuadtreeZFilter(quadtree,maxDist,sdsSource,geometry.getEnvelopeInternal());
				geometry.apply(zFilter);
			
				
				if(!zFilter.isOneCoordOutOfSource()) //We skip this line if there is no information for at least one of the destination coordinates
				{
					final Value[] fieldsValues = sds.getRow(rowIndex);
					//If we found something within MaximumDistance units.
					final Value[] newValues = new Value[fieldsValues.length];
					System.arraycopy(fieldsValues, 0, newValues, 0,
							fieldsValues.length);
					//Update the geom
					newValues[spatialFieldIndex]=ValueFactory.createValue(geometry);
					//Append row
					driver.addValues(newValues);
				}
			}
			driver.writingFinished();
			sds.close();
			sdsSource.close();
			return driver;
		} catch (DriverLoadException e) {
			throw new ExecutionException(e);
		} catch (DriverException e) {
			throw new ExecutionException(e);
		}
	}
	private class QuadtreeZFilter implements CoordinateSequenceFilter {
		private boolean done = false;
		private final double maxDist;
		private boolean outOfBoundsDestinationGeomtry = false; //If one coordinates of the geometry did not find a source Z coordinate
		private ArrayList<Coordinate> nearestCoordinates=new ArrayList<Coordinate>();
		
		@SuppressWarnings("unchecked")
		public QuadtreeZFilter(Quadtree quadtree,final double maxDist,SpatialDataSourceDecorator sdsSource,Envelope geomArea) {
			super();
			this.maxDist=maxDist;
			//Find coordinates under the distance of the geom
			geomArea.expandBy(maxDist);
			List<EnvelopeWithIndex<Long>> list = quadtree.query(geomArea);
			for (EnvelopeWithIndex<Long> env_with_index : list) {

				try {
					if ((env_with_index.intersects(geomArea))) {
						Geometry geometry = sdsSource.getGeometry(env_with_index.getId());
						Coordinate[] points=geometry.getCoordinates();
						for (int ptindex = 0; ptindex < points.length; ptindex++) {
							Coordinate coord=points[ptindex];
							if(geomArea.contains(coord))
								nearestCoordinates.add(coord);
						}
					}
				} catch (DriverException e) {
					e.printStackTrace();
					done=true;
				}
			}
			if(list.isEmpty() || nearestCoordinates.isEmpty()){
				//There is no Z information in this area. We skip this line
				outOfBoundsDestinationGeomtry=true;
				done=true;
			}
		}
		
		/**
		 * If one of the coordinate to update did not find the nearest Z coordinate 
		 * this method return True
		 */
		public boolean isOneCoordOutOfSource()
		{
			return outOfBoundsDestinationGeomtry;
		}


		public void filter(CoordinateSequence seq, int i) {
			double x = seq.getX(i);
			double y = seq.getY(i);
			Coordinate seq_pt=seq.getCoordinate(i);
			seq.setOrdinate(i, 0, x);
			seq.setOrdinate(i, 1, y);

			//Keep only nodes that are inside the area
			//and keep the nearest
			Envelope area = new Envelope(x-maxDist,x+maxDist,y-maxDist,y+maxDist);
			boolean isFound=false;
			double nearest_z=0;
			double nearest_dist=2*maxDist;
			for (Coordinate coord : nearestCoordinates) {
					if (area.contains(coord)) {
						double curDist=coord.distance(seq_pt);
						if(curDist<nearest_dist && curDist<maxDist && coord.z>-99)
						{
							nearest_dist=curDist;
							nearest_z=coord.z;
							isFound=true;
						}
					}

			}
			if(isFound)
				seq.setOrdinate(i, 2, nearest_z);
			else
				outOfBoundsDestinationGeomtry=true;

			if (i == seq.size()-1) {
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

	public TableDefinition[] getTablesDefinitions() {
		return new TableDefinition[] { TableDefinition.GEOMETRY,TableDefinition.GEOMETRY };
	}

	public Arguments[] getFunctionArguments() {
		return new Arguments[] { new Arguments(Argument.GEOMETRY,Argument.GEOMETRY,Argument.NUMERIC) };
	}
}