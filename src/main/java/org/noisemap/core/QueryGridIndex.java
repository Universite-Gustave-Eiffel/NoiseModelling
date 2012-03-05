package org.noisemap.core;

/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.operation.predicate.RectangleIntersects;
import java.util.*;
import org.grap.utilities.EnvelopeUtil;

/**
 * GridIndex is a class to speed up the query of a geometry collection inside a
 * region envelope
 * 
 * @author N.Fortin J.Picaut (IFSTTAR 2011)
 */

public class QueryGridIndex implements QueryGeometryStructure {
	//private int[] grid = null;
	private int nbI = 0;
	private int nbJ = 0;
	private double cellSizeI;
	private double cellSizeJ;
        private Map<Integer,RowsUnionClassification> gridContent = new HashMap<Integer,RowsUnionClassification>();
	//private ArrayList<ArrayList<Integer>> gridContent = new ArrayList<ArrayList<Integer>>();
	private Envelope mainEnv;

	public QueryGridIndex(final Envelope gridEnv, int xsubdiv, int ysubdiv) {
		super();
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
        /**
         * Compute the 1 dimensional index from i,j
         * @param i Row
         * @param j Column
         * @return The 1 dimensional index
         */
        private int getFlatIndex(int i, int j) {
            return j + i * nbJ;
        }
	private void addItem(int i, int j, Integer content) {
            Integer flatIndex = getFlatIndex(i,j);
            if(!gridContent.containsKey(flatIndex)) {
                gridContent.put(flatIndex, new RowsUnionClassification(content));
            } else {
                gridContent.get(flatIndex).addRow(content);
            }
	}

	private int[] getRange(Envelope geoEnv) {
		// Compute index intervals from envelopes
		Coordinate mainCenter = mainEnv.centre();
		Coordinate tmpvec = new Coordinate((geoEnv.getMinX() - mainCenter.x)
				/ cellSizeJ, (geoEnv.getMinY() - mainCenter.y) / cellSizeI);
		int halfCellCountI = nbI / 2;
		int halfCellCountJ = nbJ / 2;
		int minI = (int) (Math.floor(tmpvec.y)) + halfCellCountI;
		int minJ = (int) (Math.floor(tmpvec.x)) + halfCellCountJ;
		tmpvec = new Coordinate((geoEnv.getMaxX() - mainCenter.x) / cellSizeJ,
				(geoEnv.getMaxY() - mainCenter.y) / cellSizeI);
		int maxI = (int) (Math.ceil(tmpvec.y)) + halfCellCountI;
		int maxJ = (int) (Math.ceil(tmpvec.x)) + halfCellCountJ;
		if (minI == maxI) {
			maxI += 1;
		}
		if (minJ == maxJ) {
			maxJ += 1;
		}
		if (minI < 0) {
			minI = 0;
		}
		if (minJ < 0) {
			minJ = 0;
		}
		if (maxI > nbI) {
			maxI = nbI;
		}
		if (maxJ > nbJ) {
			maxJ = nbJ;
		}
		int[] range = { minI, maxI, minJ, maxJ };
		return range;
	}

	@Override
	public void appendGeometry(final Geometry newGeom, final Integer externalId) {
		// Compute index intervals from envelopes

		int[] ranges = getRange(newGeom.getEnvelopeInternal());
		int minI = ranges[0], maxI = ranges[1], minJ = ranges[2], maxJ = ranges[3];
		GeometryFactory factory = new GeometryFactory();
                //Compute intersection between the geom and grid cells
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
            RowIterator querySet = new RowIterator();
            for (int i = minI; i < maxI; i++) {
                for (int j = minJ; j < maxJ; j++) {
                    Integer flatIndex = getFlatIndex(i,j);
                    if(gridContent.containsKey(flatIndex)) {
                        querySet.addIntervals(gridContent.get(flatIndex).getRowRanges());
                    }
                }
            }
            return querySet;
	}
        //This iterator is specific to a multiple rows union classification result
        private class RowIterator implements Iterator<Integer> {
            private RowsUnionClassification rowsIndex=null;
            private Iterator<Integer> intervalsIterator = null;
            private Integer curIntervalCursor=null;
            private Integer curIntervalEnd=null;
            
            /**
             * Add an interval array
             * [0,50,60,100] mean all integer between 0 and 50 (begin and end included),
             * then all integers between 60 and 100(begin and end included).
             * @param newInterval 
             */
            public void addIntervals(Iterator<Integer> newInterval) {
                if(intervalsIterator!=null) {
                    throw new UnsupportedOperationException("Intervals can't be pushed when this iterator is used.");
                }
                if(newInterval.hasNext()) {
                    Integer begin = newInterval.next();
                    Integer end = newInterval.next();
                    if(rowsIndex==null) {
                        rowsIndex = new RowsUnionClassification(begin,end);
                    } else {
                        for(Integer currentIndex=begin;currentIndex<=end;currentIndex++) {
                            rowsIndex.addRow(currentIndex);
                        }
                    }
                }
            }
            
            @Override
            public boolean hasNext() {
                if(intervalsIterator==null) {
                    return !rowsIndex.isEmpty();
                } else {
                    return curIntervalCursor<curIntervalEnd || intervalsIterator.hasNext();
                }
            }

            @Override
            public Integer next() {
                //If the iterator is currently on an interval
                if(curIntervalEnd!=null && curIntervalCursor<curIntervalEnd) {
                    curIntervalCursor++;
                    return curIntervalCursor;
                    //Fetch the next interval
                }else{
                    //The interval is finished, fetch another interval
                    if(intervalsIterator==null) {
                        intervalsIterator = rowsIndex.getRowRanges();
                    }
                    if(intervalsIterator.hasNext()) {
                        //intervals always contains 2,4,6 .. items
                        curIntervalCursor=intervalsIterator.next();
                        curIntervalEnd=intervalsIterator.next();
                        return curIntervalCursor;                        
                    }else{
                        throw new NoSuchElementException("iteration has no more elements.");
                    }
                }
            }

            //User cannot remove a record
            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported operation.");
            }            
        }
}
