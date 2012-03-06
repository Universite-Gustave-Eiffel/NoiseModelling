package org.noisemap.core;
/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, JudicaÃ«l PICAUT
 ***********************************/
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.operation.predicate.RectangleIntersects;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import org.grap.utilities.EnvelopeUtil;
/**
 * GridIndex is a class to speed up the query of a geometry collection inside a
 * region envelope
 *
 * @author N.Fortin J.Picaut (IFSTTAR 2011)
 */
public class QueryGridIndex implements QueryGeometryStructure {
        private int[] grid = null;
        private int nbI = 0;
        private int nbJ = 0;
        private double cellSizeI;
        private double cellSizeJ;
        private ArrayList<ArrayList<Integer>> gridContent = new ArrayList<ArrayList<Integer>>();
        private Envelope mainEnv;
        public QueryGridIndex(final Envelope gridEnv, int xsubdiv, int ysubdiv) {
                super();
                grid = new int[xsubdiv * ysubdiv];
                for (int i = 0; i < grid.length; i++) {
                        grid[i] = -1;
                }
                mainEnv = gridEnv;
                nbJ = xsubdiv;
                nbI = ysubdiv;
                cellSizeI = mainEnv.getHeight() / nbI;
                cellSizeJ = mainEnv.getWidth() / nbJ;
        }
        private Envelope getCellEnv(int i, int j) {
                final double minx = mainEnv.getMinX() + cellSizeJ * j;
                final double miny = mainEnv.getMinY() + cellSizeI * i;
                return new Envelope(minx, minx + cellSizeJ, miny, miny + cellSizeI);
        }
        private void addItem(int i, int j, Integer content) {
                int idcontent = grid[j + i * nbJ];
                if (idcontent == -1) {
                        idcontent = gridContent.size();
                        gridContent.add(new ArrayList<Integer>());
                        grid[j + i * nbJ] = idcontent;
                }
                gridContent.get(idcontent).add(content);
        }
        /**
         * Convert coordinate to i,j index
         * @param coord Coordinate to convert
         * @return [i,j] array
         */
        private int[] getIndexByCoordinate(Coordinate coord) {
            Coordinate norm = new Coordinate((coord.x - mainEnv.getMinX()) / mainEnv.getWidth(),
                                             (coord.y - mainEnv.getMinY()) / mainEnv.getHeight());
           return new int[] {(int)Math.floor(norm.x*nbJ),(int)Math.floor(norm.y*nbI)};
        }
        private int[] getRange(Envelope geoEnv) {
                // Compute index intervals from envelopes
                int[] minIndex = getIndexByCoordinate(new Coordinate(geoEnv.getMinX(),geoEnv.getMinY()));
                int[] maxIndex = getIndexByCoordinate(new Coordinate(geoEnv.getMaxX(),geoEnv.getMaxY()));
                //Retrieve values and limit to boundary
                int minJ = Math.max(minIndex [0],0);
                int minI = Math.max(minIndex [1],0);
                int maxJ = Math.min(maxIndex [0] + 1,nbJ);
                int maxI = Math.min(maxIndex [1] + 1,nbI);
                int[] range = { minI, maxI, minJ, maxJ };
                return range;
        }
        @Override
        public void appendGeometry(final Geometry newGeom, final Integer externalId) {
                // Compute index intervals from envelopes
                int[] ranges = getRange(newGeom.getEnvelopeInternal());
                int minI = ranges[0], maxI = ranges[1], minJ = ranges[2], maxJ = ranges[3];
                GeometryFactory factory = new GeometryFactory();
                for (int i = minI; i < maxI; i++) {
                        for (int j = minJ; j < maxJ; j++) {
                                Envelope cellEnv = getCellEnv(i, j);
                                Polygon square = factory.createPolygon(
                                                (LinearRing) EnvelopeUtil.toGeometry(cellEnv), null);
                                RectangleIntersects inter = new RectangleIntersects(square);
                                if (inter.intersects(newGeom)) {
                                        addItem(i, j, externalId);
                                }
                        }
                }
        }
        @Override
        public Iterator<Integer> query(Envelope queryEnv) {
                int[] ranges = getRange(queryEnv);
                int minI = ranges[0], maxI = ranges[1], minJ = ranges[2], maxJ = ranges[3];
                ArrayList<Integer> querySet = new ArrayList<Integer>();
                int cellsParsed = 0;
                for (int i = minI; i < maxI; i++) {
                        for (int j = minJ; j < maxJ; j++) {
                                int contentId = grid[j + i * nbJ];
                                if (contentId != -1) {
                                        querySet.addAll(gridContent.get(contentId));
                                        cellsParsed++;
                                }
                        }
                }
                if (cellsParsed > 1) {
                        HashSet<Integer> h = new HashSet<Integer>(querySet);
                        querySet.clear();
                        querySet.addAll(h);
                }
                return querySet.iterator();
        }
}