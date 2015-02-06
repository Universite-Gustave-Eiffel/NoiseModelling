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

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import junit.framework.TestCase;
import org.gdms.data.DataSource;
import org.gdms.data.DataSourceCreationException;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.NoSuchTableException;
import org.gdms.data.indexes.DefaultSpatialIndexQuery;
import org.gdms.data.indexes.IndexException;
import org.gdms.data.indexes.IndexManager;
import org.gdms.data.indexes.IndexQueryException;
import org.gdms.data.indexes.tree.IndexVisitor;
import org.gdms.data.schema.MetadataUtilities;
import org.gdms.driver.DataSet;
import org.gdms.driver.DriverException;
import org.grap.utilities.EnvelopeUtil;
import org.junit.Before;

/**
 * Unit test & quick benchmark of implemented GeometryStructure.
 */
public class QueryGeometryStructureTest extends TestCase {
    private DataSourceFactory dsf;
    
    @Before@Override
    public void setUp() throws Exception {
            //Init datasource folder
            String targetPath="target"+File.separatorChar;
            File targetDir = new File(targetPath);
            File sourceDir = new File(targetPath+"sources"+File.separatorChar);
            dsf = new DataSourceFactory(sourceDir.getAbsolutePath(),
                    targetDir.getAbsolutePath());
    }
    
    
    
    public void testGdmsIndex() throws DataSourceCreationException, DriverException, NoSuchTableException, IndexException, IndexQueryException {
        //Register gdms file
        File sourcesGdmsFile = new File("src"+File.separatorChar+
                "test"+File.separatorChar+
                "resource"+File.separatorChar+
                "org"+File.separatorChar+
                "noisemap"+File.separatorChar+
                "core"+File.separatorChar+
                "osm_bench_lines.gdms"
                );
        dsf.getSourceManager().register("soundSources", 
                sourcesGdmsFile);
        
        DataSource sourcefil = dsf.getDataSource(sourcesGdmsFile);
        sourcefil.open();
        
        DataSet sdsSources = sourcefil.getDriverTable();
        int spatialSourceFieldIndex = MetadataUtilities.getSpatialFieldIndex(sdsSources.getMetadata());
        String spatialSourceFieldName = sdsSources.getMetadata().getFieldName(spatialSourceFieldIndex);
        long rowCount = sdsSources.getRowCount();
        IndexManager im = dsf.getIndexManager();
        //Index of Sound Sources Table
        if(!im.isIndexed(sdsSources,spatialSourceFieldName)) {
            im.buildIndex(sdsSources, spatialSourceFieldName, null);
        }

        Envelope testExtract = new Envelope(new Coordinate(-1.5552300630926283,47.24373163594368),
                                            new Coordinate(-1.5516724508251067,47.246733371294404));
        //compute expected Query Values
        
        Set<Integer> expectedQueryValue = new HashSet<Integer>();
        Geometry env = EnvelopeUtil.toGeometry(testExtract);
        
        for (Integer rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            Geometry sourceGeom = sdsSources.getFieldValue(rowIndex, spatialSourceFieldIndex).getAsGeometry();
            if(sourceGeom.getEnvelopeInternal().intersects(testExtract)) {
                expectedQueryValue.add(rowIndex);
            }
        }
        int foundRows = expectedQueryValue.size();
        //Query with rTree
        DefaultSpatialIndexQuery sourceSpatialIndexQuery = new DefaultSpatialIndexQuery(spatialSourceFieldName,testExtract);
        RowVisitor visitor = new RowVisitor();
        im.queryIndex(sdsSources, sourceSpatialIndexQuery, visitor);
        int foundRowsByIndex = visitor.getVisitedRows().size();
        for(int foundIndex : visitor.getVisitedRows()) {
            expectedQueryValue.remove(foundIndex);
        }
        System.out.println("GDMS RIndex found "+foundRowsByIndex+" rows and missed "+expectedQueryValue.size()+" / "+foundRows+" rows"); 
    }
    
    private class RowVisitor implements IndexVisitor {
        List<Integer> visitedRows = new ArrayList<Integer>();
        @Override
        public void visitElement(int row, Object env) {
            visitedRows.add(row);
        }
        /**
         * 
         * @return Query result
         */
        public List<Integer> getVisitedRows() {
            return visitedRows;
        }
        
    }
    
    /**
     * Dummy test
     */
    public void testVoid() {
        
    }
    /**
     * This function does not assert,
     * but keep track of the evolution of geometry structures optimisations
     */
    public void testBenchQueryGeometryStructure() throws DataSourceCreationException, DriverException {
        
        System.out.println("________________________________________________");
        System.out.println("QueryGeometryStructure Bench :");

        //Register gdms file
        File sourcesGdmsFile = new File("src"+File.separatorChar+
                "test"+File.separatorChar+
                "resource"+File.separatorChar+
                "org"+File.separatorChar+
                "noisemap"+File.separatorChar+
                "core"+File.separatorChar+
                "osm_bench_lines.gdms"
                );
        dsf.getSourceManager().register("soundSources", 
                sourcesGdmsFile);
        DataSource sdsSources = dsf.getDataSource(sourcesGdmsFile);
        sdsSources.open();
        int spatialSourceFieldIndex = MetadataUtilities.getSpatialFieldIndex(sdsSources.getMetadata());
        long rowCount = sdsSources.getRowCount();
        
        
        //Init quadtree structures
        long startFeedQuadree=System.currentTimeMillis();
        QueryQuadTree quadIndex = new QueryQuadTree();        
        for (Integer rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            Geometry sourceGeom = sdsSources.getFieldValue(rowIndex, spatialSourceFieldIndex).getAsGeometry();
            quadIndex.appendGeometry(sourceGeom, rowIndex);
        }
        long feedQuadtreeTime = System.currentTimeMillis() - startFeedQuadree;
        //Init grid structure
        long startFeedGrid=System.currentTimeMillis();
        QueryGridIndex gridIndex = new QueryGridIndex(sdsSources.getFullExtent(),8,8);
        for (Integer rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            Geometry sourceGeom = sdsSources.getFieldValue(rowIndex, spatialSourceFieldIndex).getAsGeometry();
            gridIndex.appendGeometry(sourceGeom, rowIndex);
        }
        long feedGridTime = System.currentTimeMillis() - startFeedGrid;
        //Init rtree structure
        long startFeedRtree=System.currentTimeMillis();
        QueryRTree rTreeIndex = new QueryRTree();
        for (Integer rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            Geometry sourceGeom = sdsSources.getFieldValue(rowIndex, spatialSourceFieldIndex).getAsGeometry();
            rTreeIndex.appendGeometry(sourceGeom, rowIndex);
        }
        long feedRTreeTime = System.currentTimeMillis() - startFeedRtree;
        
        
        
        System.out.println("Feed structures with "+rowCount+" items..");
        System.out.println("Feed QueryQuadTree in "+feedQuadtreeTime+" ms");
        System.out.println("Feed QueryGridIndex in "+feedGridTime+" ms");
        System.out.println("Feed RTreeIndex in "+feedRTreeTime+" ms");
        
       Envelope testExtract = new Envelope(new Coordinate(-1.5552300630926283,47.24373163594368),
                                            new Coordinate(-1.5516724508251067,47.246733371294404));
       //compute expected Query Values
        
        ArrayList<Integer> expectedQueryValue = new ArrayList<Integer>();
        Geometry env = EnvelopeUtil.toGeometry(testExtract);
        
        for (Integer rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            Geometry sourceGeom = sdsSources.getFieldValue(rowIndex, spatialSourceFieldIndex).getAsGeometry();
            if(env.intersects(sourceGeom)) {
                expectedQueryValue.add(rowIndex);
            }
        }
        
        //Request for each implementation of QueryGeometryStructure
        System.out.println("Expected result items : "+expectedQueryValue.size()+" items");
        long debQuery = System.nanoTime();
        long nbItemsReturned = countResult(quadIndex.query(testExtract));
        System.out.println("QueryQuadTree query time in "+(System.nanoTime() - debQuery)/1e6+" ms with "+nbItemsReturned+" items returned.");
        System.out.println("QueryQuadTree item count "+quadIndex.size());
        debQuery = System.nanoTime();
        nbItemsReturned = countResult(gridIndex.query(testExtract));
        System.out.println("QueryGridIndex query time in "+(System.nanoTime() - debQuery)/1e6+" ms with "+nbItemsReturned+" items returned.");
        System.out.println("QueryGridIndex item count "+gridIndex.size());
        debQuery = System.nanoTime();
        nbItemsReturned = countResult(rTreeIndex.query(testExtract));
        System.out.println("RTreeIndex query time in "+(System.nanoTime() - debQuery)/1e6+" ms with "+nbItemsReturned+" items returned.");

        
        
        
        //Check items returned by GridIndex
        queryAssert(expectedQueryValue,gridIndex.query(testExtract));
        //queryAssert(expectedQueryValue,quadIndex.query(testExtract));
    }
    private Integer countResult(Iterator<Integer> result) {
        Integer counter=0;
        while(result.hasNext()) {
            result.next();
            counter++;
        }        
        return counter;
    }
    /**
     * queryResult must contains all items of expectedResult
     * @param expectedResult
     * @param queryResult 
     */
    private void queryAssert(List<Integer> expectedResult,Iterator<Integer> queryResult) {
        List<Integer> remainingExpectedResult = new ArrayList<Integer>();
        remainingExpectedResult.addAll(expectedResult);
        while(queryResult.hasNext()) {
            Integer geoIndex = queryResult.next();
            if(remainingExpectedResult.contains(geoIndex)) {
                remainingExpectedResult.remove(geoIndex);
            }
        }
        if(!remainingExpectedResult.isEmpty()) {
            assertTrue("QueryGeometryStructure does not return the expected row index (first :"+remainingExpectedResult.get(0)+" )",remainingExpectedResult.isEmpty());
        }
    }
    /**
     * Add lines in a geometry query structure
     */
    /*
    public void testLineSegmentsIntersect() {
        GeometryFactory fact = new GeometryFactory();
        //Init segments
        LineSegment[] segments= new LineSegment[] {
            new LineSegment(new Coordinate(2,1),new Coordinate(7,3)),  //A
            new LineSegment(new Coordinate(8,3),new Coordinate(10,1)), //B
            new LineSegment(new Coordinate(2,6),new Coordinate(7,6))   //C
        };
        //Init scene envelope
        Envelope envTest = new Envelope(new Coordinate(0,0),new Coordinate(11,11));
        
        //Feed grid structure
        QueryGridIndex gridIndex = new QueryGridIndex(envTest,4,4);
        for(Integer idseg = 0;idseg < segments.length ; idseg++) {
            gridIndex.appendGeometry(segments[idseg].toGeometry(fact),idseg);
        }
        
        //Feed JTS Quadtree structure
        QueryQuadTree quadIndex = new QueryQuadTree();
        for(Integer idseg = 0;idseg < segments.length ; idseg++) {
            quadIndex.appendGeometry(segments[idseg].toGeometry(fact),idseg);
        }
        
        
        //Intersection test
        List<Integer> expectedIndex;
        expectedIndex=new ArrayList<Integer>();
        expectedIndex.add(0);
        expectedIndex.add(1);
        Envelope queryEnv = new Envelope(new Coordinate(7,2),new Coordinate(8,3));
        queryAssert(expectedIndex, quadIndex.query(queryEnv));
        queryAssert(expectedIndex, gridIndex.query(queryEnv));
        
        //Other one        
        expectedIndex.add(2);
        queryEnv = new Envelope(new Coordinate(7,2),new Coordinate(8,6));
        queryAssert(expectedIndex, quadIndex.query(queryEnv));
        queryAssert(expectedIndex, gridIndex.query(queryEnv));
    }
    */
}
