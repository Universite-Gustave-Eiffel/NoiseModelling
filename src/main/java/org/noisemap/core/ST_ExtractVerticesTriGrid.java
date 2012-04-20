/***********************************
 * ANR EvalPDU
 * IFSTTAR 16_11_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/
package org.noisemap.core;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import java.util.HashMap;
import java.util.Iterator;
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
import org.gdms.driver.DiskBufferDriver;
import org.gdms.driver.DriverException;
import org.gdms.sql.function.FunctionException;
import org.gdms.sql.function.FunctionSignature;
import org.gdms.sql.function.table.AbstractTableFunction;
import org.gdms.sql.function.table.TableArgument;
import org.gdms.sql.function.table.TableDefinition;
import org.gdms.sql.function.table.TableFunctionSignature;
import org.orbisgis.progress.ProgressMonitor;



/**
 * This function is the post process of ST_BrTriGrid function. It will extract and merge the vertices and corresponding values of triangles.
 */
public class ST_ExtractVerticesTriGrid extends AbstractTableFunction {

    private static void registerNewVertex(GeometryFactory geometryFactory,final DiskBufferDriver driver, Coordinate pt, double value) throws DriverException {
        Value[] row = new Value[2];
        row[0] = ValueFactory.createValue(geometryFactory.createPoint(pt));
        row[1] = ValueFactory.createValue(value);
        driver.addValues(row);
    }
    @Override
    public DataSet evaluate(SQLDataSourceFactory sqldsf, DataSet[] dss, Value[] values, ProgressMonitor pm) throws FunctionException {
        //First pass
        //Aggregation of row line number corresponding to groups.
        try {

            pm.startTask("Grouping and Fusion of geometries", 100);
            int spatialFieldIndex;
            final DataSet sds = dss[0];
            int cellidFieldIndex=sds.getMetadata().getFieldIndex("cellid");
            int dbv1FieldIndex=sds.getMetadata().getFieldIndex("db_v1");
            int dbv2FieldIndex=sds.getMetadata().getFieldIndex("db_v2");
            int dbv3FieldIndex=sds.getMetadata().getFieldIndex("db_v3");
            if (1 == values.length) {
                    // if no spatial's field's name is provided, the default (first)
                    // one is arbitrarily chosen.
                   spatialFieldIndex = sds.getMetadata().getFieldIndex(values[0].toString());
            } else {
                   spatialFieldIndex = MetadataUtilities.getSpatialFieldIndex(sds.getMetadata());
            }
            final DiskBufferDriver driver = new DiskBufferDriver(sqldsf,
					getMetadata(null));

            GeometryFactory geometryFactory = new GeometryFactory();
            //Declaration of the HashMap that will keep the lines number for each geometries of the same category.
            HashMap<Integer,RowsUnionClassification> groups=new HashMap<Integer,RowsUnionClassification>();
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
                Integer cellid=(Integer)sds.getFieldValue(i,cellidFieldIndex).getAsInt();
                RowsUnionClassification res=groups.get(cellid);
                if(res==null) {
                    groups.put(cellid, new RowsUnionClassification((int) i));
                } else {
                    res.addRow((int) i);
                }
                progressionInfo.nextSubProcessEnd();
            }


            //Step 2, Union of vertices
            ProgressionProcess progressionInfoUnion=pmManager.nextSubProcess(groups.size());
            Iterator<Entry<Integer,RowsUnionClassification>> it = groups.entrySet().iterator();
            //For each distinct group
            for(Map.Entry<Integer,RowsUnionClassification> pairs : groups.entrySet()) {
                if(pm.isCancelled()) {
                    throw new FunctionException("Canceled by user");
                }
                Iterator<Integer> ranges=pairs.getValue().getRowRanges();
                PointsMerge verticesMergeTool=new PointsMerge(0.1);
                int cpt=0;
                while(ranges.hasNext()) {
                    int begin=ranges.next();
                    int end=ranges.next();
                    for(int rowid=begin;rowid<=end;rowid++) {
                        if(pm.isCancelled()) {
                            throw new FunctionException("Canceled by user");
                        }
                        Coordinate [] coords = sds.getFieldValue(rowid,spatialFieldIndex).getAsGeometry().getCoordinates();
                        if(verticesMergeTool.getOrAppendVertex(coords[0])==cpt) {
                            registerNewVertex(geometryFactory,driver,coords[0],sds.getFieldValue(rowid,dbv1FieldIndex).getAsDouble());
                            cpt++;
                        }
                        if(verticesMergeTool.getOrAppendVertex(coords[1])==cpt) {
                            registerNewVertex(geometryFactory,driver,coords[1],sds.getFieldValue(rowid,dbv2FieldIndex).getAsDouble());
                            cpt++;
                        }
                        if(verticesMergeTool.getOrAppendVertex(coords[2])==cpt) {
                            registerNewVertex(geometryFactory,driver,coords[2],sds.getFieldValue(rowid,dbv3FieldIndex).getAsDouble());
                            cpt++;
                        }
                    }
                }
                progressionInfoUnion.nextSubProcessEnd();
            }
            //Close all threads & files
            pmManager.stop();
            pm.endTask();
            driver.writingFinished();
            return driver.getTable("main");
            } catch (DriverException e) {
                    throw new FunctionException(e);
            }
    }

    @Override
    public Metadata getMetadata(Metadata[] mtdts) throws DriverException {
		return new DefaultMetadata(new Type[] {
				TypeFactory.createType(Type.GEOMETRY),
				TypeFactory.createType(Type.DOUBLE)},
                                new String[] { "the_geom",
				"db_v"});
    }

    @Override
    public String getDescription() {
        return "This function is the post process of ST_BrTriGrid function. It will extract and merge the vertices and corresponding values of triangles.";
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
        return "ST_ExtractVerticesTriGrid";
    }

    @Override
    public String getSqlOrder() {
        return "select * from ST_ExtractVerticesTriGrid(tri_table);";
    }

}
