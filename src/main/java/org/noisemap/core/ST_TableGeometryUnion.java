/***********************************
 * ANR EvalPDU
 * IFSTTAR 05_10_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/
package org.noisemap.core;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import org.gdms.data.SQLDataSourceFactory;
import org.gdms.data.schema.DefaultMetadata;
import org.gdms.data.schema.Metadata;
import org.gdms.data.schema.MetadataUtilities;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DataSet;
import org.gdms.driver.DriverException;
import org.gdms.driver.memory.MemoryDataSetDriver;
import org.gdms.sql.function.FunctionException;
import org.gdms.sql.function.FunctionSignature;
import org.gdms.sql.function.table.AbstractTableFunction;
import org.gdms.sql.function.table.TableArgument;
import org.gdms.sql.function.table.TableDefinition;
import org.gdms.sql.function.table.TableFunctionSignature;
import org.orbisgis.progress.ProgressMonitor;

class GroupKey {
    private int cellId;
    private short idIso;

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + this.cellId;
        hash = 13 * hash + this.idIso;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GroupKey other = (GroupKey) obj;
        if (this.cellId != other.getCellId()) {
            return false;
        }
        if (this.idIso != other.getIdIso()) {
            return false;
        }
        return true;
    }

    public int getCellId() {
        return cellId;
    }

    public void setCellId(int cellId) {
        this.cellId = cellId;
    }

    public short getIdIso() {
        return idIso;
    }

    public void setIdIso(short idIso) {
        this.idIso = idIso;
    }

    public GroupKey(int cellId, short idIso) {
        this.cellId = cellId;
        this.idIso = idIso;
    }
}

/**
 * This function is the post process of ST_TriangleContouring function. It will merge geometry of the same category.
 */
public class ST_TableGeometryUnion extends AbstractTableFunction {

    @Override
    public DataSet evaluate(SQLDataSourceFactory sqldsf, DataSet[] dss, Value[] values, ProgressMonitor pm) throws FunctionException {
        //First pass
        //Aggregation of row line number corresponding to groups.
        try {

            pm.startTask("Grouping and Fusion of geometries", 100);
            int spatialFieldIndex;
            final DataSet sds = dss[0];
            int cellidFieldIndex=sds.getMetadata().getFieldIndex("cellid");
            int idisoFieldIndex=sds.getMetadata().getFieldIndex("idiso");
            if (1 == values.length) {
                    // if no spatial's field's name is provided, the default (first)
                    // one is arbitrarily chosen.
                   spatialFieldIndex = sds.getMetadata().getFieldIndex(values[0].toString());
            } else {
                   spatialFieldIndex = MetadataUtilities.getSpatialFieldIndex(sds.getMetadata());
            }
            final MemoryDataSetDriver driver = new MemoryDataSetDriver(
					getMetadata(null));


            //Declaration of the HashMap that will keep the lines number for each geometries of the same category.
            HashMap<GroupKey,RowsUnionClassification> groups=new HashMap<GroupKey,RowsUnionClassification>();
            long rowCount = sds.getRowCount();
            //Instanciate the progression manager
            ProgressionOrbisGisManager pmManager = new ProgressionOrbisGisManager(
					2, pm);
            ProgressionProcess progressionInfo=pmManager.nextSubProcess(rowCount);
            pmManager.start();
            // Fill the HashMap with rowCount line numbers
            for (long i = 0; i < rowCount; i++) {
                if(pm.isCancelled()) {
                    throw new FunctionException("Canceled by user");
                }
                int cellid=sds.getFieldValue(i,cellidFieldIndex).getAsInt();
                short idiso=sds.getFieldValue(i,idisoFieldIndex).getAsShort();
                GroupKey thekey=new GroupKey(cellid,idiso);
                RowsUnionClassification res=groups.get(thekey);
                if(res==null) {
                    groups.put(thekey, new RowsUnionClassification((int) i));
                } else {
                    res.addRow((int) i);
                }
                progressionInfo.nextSubProcessEnd();
            }


            //Step 2, Union of geometries
            ProgressionProcess progressionInfoUnion=pmManager.nextSubProcess(groups.size());
            Iterator<Entry<GroupKey,RowsUnionClassification>> it = groups.entrySet().iterator();
            //For each distinct group
            GeometryFactory geometryFactory = new GeometryFactory();
            for(Map.Entry<GroupKey,RowsUnionClassification> pairs : groups.entrySet()) {
                if(pm.isCancelled()) {
                    throw new FunctionException("Canceled by user");
                }
                Iterator<Integer> ranges=pairs.getValue().getRowRanges();
                LinkedList<Geometry> toUnite=new LinkedList<Geometry>();
                while(ranges.hasNext()) {
                    int begin=ranges.next();
                    int end=ranges.next();
                    for(int rowid=begin;rowid<=end;rowid++) {
                        if(pm.isCancelled()) {
                            throw new FunctionException("Canceled by user");
                        }
                        toUnite.add(sds.getFieldValue(rowid,spatialFieldIndex).getAsGeometry());
                    }
                }
                //Merge geometries
                Geometry geoArray[] = new Geometry[toUnite.size()];
		toUnite.toArray(geoArray);
                toUnite.clear();
		GeometryCollection polygonCollection = geometryFactory
				.createGeometryCollection(geoArray);
                Geometry mergedGeom=polygonCollection.union();
                //Save the merged geometries into the driver
                Value[] row = new Value[3];
                row[0] = ValueFactory.createValue(mergedGeom);
                row[1] = ValueFactory.createValue(pairs.getKey().getCellId());
                row[2] = ValueFactory.createValue(pairs.getKey().getIdIso());
                driver.addValues(row);
                progressionInfoUnion.nextSubProcessEnd();
            }
            
            //Close all threads & files
            pmManager.stop();
            pm.endTask();
            driver.start();
            return driver.getTable("main");
            } catch (DriverException e) {
                    throw new FunctionException(e);
            }
    }

    @Override
    public Metadata getMetadata(Metadata[] mtdts) throws DriverException {
		return new DefaultMetadata(new Type[] {
				TypeFactory.createType(Type.GEOMETRY),
				TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.SHORT)},
                                new String[] { "the_geom",
				"cellid",
                                "idiso"});
    }

    @Override
    public String getDescription() {
        return "This function is not generic,it post process the ST_TriangleContouring function. It will merge geometry of the same category (cellid and idiso).";
    }

    @Override
    public FunctionSignature[] getFunctionSignatures() {
        return new FunctionSignature[]{
                new TableFunctionSignature(TableDefinition.GEOMETRY,
                new TableArgument(TableDefinition.GEOMETRY))
        };
    }

    @Override
    public String getName() {
        return "ST_TableGeometryUnion";
    }

    @Override
    public String getSqlOrder() {
        return "select * from ST_TableGeometryUnion(contouring_table);";
    }

}
