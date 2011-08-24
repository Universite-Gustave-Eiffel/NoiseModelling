package org.noisemap.core;

/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

import java.util.LinkedList;

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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Split triangle into area within the specified range values
 */

public class ST_TriangleContouring extends AbstractTableFunction {
	private static final double EPSILON = 1E-15;

	private static boolean isoEqual(double isoValue1, double isoValue2) {
		return Math.abs(isoValue1 - isoValue2) < EPSILON * isoValue2;
	}

	@Override
	public String getName() {
		return "ST_TriangleContouring";
	}

	@Override
	public String getSqlOrder() {
		return "select ST_TriangleContouring( the_geom, 'field_vert1' ,'field_vert2','field_vert3', '15,20,30,50,80') from triangle_table";
	}

	@Override
	public String getDescription() {
		return "select ST_TriangleContouring( geomToUpdate, verticeOneLevelValue,verticeTwoLevelValue,verticeThreeLevelValue, isoLevels ) from triangle_table";
	}

	private boolean computeSplitPositionOrdered(double marker1, double marker2,
			double isoValue, Coordinate p1, Coordinate p2,
			Coordinate splitPosition) {
		if (marker1 < isoValue && isoValue < marker2) {
			double interval = (isoValue - marker1) / (marker2 - marker1);
			splitPosition.setCoordinate(new Coordinate(p1.x + (p2.x - p1.x)
					* interval, p1.y + (p2.y - p1.y) * interval, p1.z
					+ (p2.z - p1.z) * interval));
			return true;
		} else {
			return false;
		}
	}

	private boolean computeSplitPosition(double marker1, double marker2,
			double isoValue, Coordinate p1, Coordinate p2,
			Coordinate splitPosition) {
		if (marker1 < marker2) {
			return computeSplitPositionOrdered(marker1, marker2, isoValue, p1,
					p2, splitPosition);
		} else {
			return computeSplitPositionOrdered(marker2, marker1, isoValue, p2,
					p1, splitPosition);
		}
	}

	private short findTriangleSide(TriMarkers currentTriangle, double isoValue,
			short sideException, Coordinate splitPosition) {
		if (sideException != 0
				&& computeSplitPosition(currentTriangle.m1, currentTriangle.m2,
						isoValue, currentTriangle.p0, currentTriangle.p1,
						splitPosition)) {
			return 0;
		} else if (sideException != 1
				&& computeSplitPosition(currentTriangle.m2, currentTriangle.m3,
						isoValue, currentTriangle.p1, currentTriangle.p2,
						splitPosition)) {
			return 1;
		} else if (sideException != 2
				&& computeSplitPosition(currentTriangle.m3, currentTriangle.m1,
						isoValue, currentTriangle.p2, currentTriangle.p0,
						splitPosition)) {
			return 2;
		} else {
			return -1;
		}
	}

	/*
	 * Return the splitting of a triangle in three parts. This function return
	 * always triangles in the counter-clockwise orientation of vertices (if the
	 * triangle provided is in the same orientation)
	 * 
	 * @param sideStart Start side of the splitting segment [0-2]
	 * 
	 * @param sideStop End side of the splitting segment [0-2] (must be >
	 * sideStart)
	 * 
	 * @param posIsoStart Start coordinate of the splitting segment
	 * 
	 * @param posIsoStop End coordinate of the splitting segment
	 * 
	 * @param isoLvl Iso value of the splitting segment
	 * 
	 * @param currentTriangle Input triangle
	 * 
	 * @param[out] aloneTri Splitted triangle, the side of the shared vertex of
	 * sideStart and sideStop
	 * 
	 * @param[out] firstTwinTri Splitted triangle
	 * 
	 * @param[out] secondTwinTri Splitted triangle
	 * 
	 * @return The shared vertex index [0-2]
	 */
	private short getSplittedTriangle(short sideStart, short sideStop,
			Coordinate posIsoStart, Coordinate posIsoStop, double isoLvl,
			TriMarkers currentTriangle, TriMarkers aloneTri,
			TriMarkers firstTwinTri, TriMarkers secondTwinTri)
			throws FunctionException {
		short sharedVertex = -1, secondVertex = -1, thirdVertex = -1;
		if (sideStart == 0 && sideStop == 2) {
			sharedVertex = 0;
			secondVertex = 1;
			thirdVertex = 2;
			aloneTri.setAll(posIsoStart, posIsoStop,
					currentTriangle.getVertice(sharedVertex), isoLvl, isoLvl,
					currentTriangle.getMarker(sharedVertex));
			firstTwinTri.setAll(posIsoStart,
					currentTriangle.getVertice(thirdVertex), posIsoStop,
					isoLvl, currentTriangle.getMarker(thirdVertex), isoLvl);
			secondTwinTri.setAll(posIsoStart,
					currentTriangle.getVertice(secondVertex),
					currentTriangle.getVertice(thirdVertex), isoLvl,
					currentTriangle.getMarker(secondVertex),
					currentTriangle.getMarker(thirdVertex));
		} else if (sideStart == 0 && sideStop == 1) {
			sharedVertex = 1;
			secondVertex = 2;
			thirdVertex = 0;
			aloneTri.setAll(posIsoStart,
					currentTriangle.getVertice(sharedVertex), posIsoStop,
					isoLvl, currentTriangle.getMarker(sharedVertex), isoLvl);
			firstTwinTri.setAll(posIsoStart, posIsoStop,
					currentTriangle.getVertice(thirdVertex), isoLvl, isoLvl,
					currentTriangle.getMarker(thirdVertex));
			secondTwinTri.setAll(posIsoStop,
					currentTriangle.getVertice(secondVertex),
					currentTriangle.getVertice(thirdVertex), isoLvl,
					currentTriangle.getMarker(secondVertex),
					currentTriangle.getMarker(thirdVertex));
		} else if (sideStart == 1 && sideStop == 2) {
			sharedVertex = 2;
			secondVertex = 0;
			thirdVertex = 1;
			aloneTri.setAll(posIsoStart,
					currentTriangle.getVertice(sharedVertex), posIsoStop,
					isoLvl, currentTriangle.getMarker(sharedVertex), isoLvl);
			firstTwinTri.setAll(posIsoStart, posIsoStop,
					currentTriangle.getVertice(secondVertex), isoLvl, isoLvl,
					currentTriangle.getMarker(secondVertex));
			secondTwinTri.setAll(posIsoStart,
					currentTriangle.getVertice(secondVertex),
					currentTriangle.getVertice(thirdVertex), isoLvl,
					currentTriangle.getMarker(secondVertex),
					currentTriangle.getMarker(thirdVertex));
		} else {
			throw new FunctionException("Can't find shared vertex");
		}
		return sharedVertex;
	}

	/**
	 * Use interval to split the triangle into severals ones
	 * 
	 * @param[in] beginIncluded Begin of iso level value
	 * @param[in] endExcluded End of iso level value
	 * @param[in] currentTriangle Triangle to process
	 * @param[out] outsideTriangles Split triangles outside of the region
	 * @param[out] intervalTriangles Split triangles covered by the region
	 * @return False if the entire geometry is outside of the region, true if
	 *         outsideTriangles or intervalTriangles has been updated.
	 * @throws ExecutionException
	 */
	private boolean splitInterval(Double beginIncluded, Double endExcluded,
			TriMarkers currentTriangle,
			LinkedList<TriMarkers> outsideTriangles,
			LinkedList<TriMarkers> intervalTriangles) throws FunctionException {
		if ((beginIncluded > currentTriangle.m1
				&& beginIncluded > currentTriangle.m2 && beginIncluded > currentTriangle.m3)
				|| // If triangles vertices ALL outside inferior
				(endExcluded < currentTriangle.m1
						&& endExcluded < currentTriangle.m2 && endExcluded < currentTriangle.m3)// If
																								// triangles
																								// vertices
																								// ALL
																								// outside
																								// inferior
		) {
			return false;
		}
		short vertIso1Start = -1, vertIso1Stop = -1; // for beginIncluded -
														// Vertice of the
														// triangle where the
														// split Origin and
														// destination will be
														// done
		short vertIso2Start = -1, vertIso2Stop = -1; // for endExcluded -
														// Vertice of the
														// triangle where the
														// split Origin and
														// destination will be
														// done
		short sideIso1Start = -1, sideIso1Stop = -1; // for beginIncluded - Side
														// of the triangle where
														// the split Origin and
														// destination will be
														// done
		short sideIso2Start = -1, sideIso2Stop = -1; // for endExcluded - Side
														// of the triangle where
														// the split Origin and
														// destination will be
														// done
		Coordinate posIso1Start = new Coordinate(), posIso1Stop = new Coordinate();
		Coordinate posIso2Start = new Coordinate(), posIso2Stop = new Coordinate();
		// Process ISO 1 beginIncluded
		// Find if we include some vertices
		if (isoEqual(currentTriangle.m1, beginIncluded)) {
			vertIso1Start = 0;
		}
		if (isoEqual(currentTriangle.m2, beginIncluded)) {
			if (vertIso1Start == -1) {
				vertIso1Start = 1;
			} else {
				vertIso1Stop = 1;
			}
		}
		if (isoEqual(currentTriangle.m3, beginIncluded)) {
			if (vertIso1Start == -1) {
				vertIso1Start = 2;
			} else {
				vertIso1Stop = 2;
			}
		}
		// Find if we need to split a side (interval between two points)
		if (vertIso1Start == -1 || vertIso1Stop == -1) {
			sideIso1Start = findTriangleSide(currentTriangle, beginIncluded,
					sideIso1Start, posIso1Start);
			if (sideIso1Start != -1) {
				sideIso1Stop = findTriangleSide(currentTriangle, beginIncluded,
						sideIso1Start, posIso1Stop);
			}
		}
		// Process ISO 2 endExcluded
		// Find if we include some vertices
		if (isoEqual(currentTriangle.m1, endExcluded)) {
			vertIso2Start = 0;
		}
		if (isoEqual(currentTriangle.m2, endExcluded)) {
			if (vertIso2Start == -1) {
				vertIso2Start = 1;
			} else {
				vertIso2Stop = 1;
			}
		}
		if (isoEqual(currentTriangle.m3, endExcluded)) {
			if (vertIso2Start == -1) {
				vertIso2Start = 2;
			} else {
				vertIso2Stop = 2;
			}
		}
		// Find if we need to split a side (interval between two points)
		if (vertIso2Start == -1 || vertIso2Stop == -1) {
			sideIso2Start = findTriangleSide(currentTriangle, endExcluded,
					sideIso2Start, posIso2Start);
			if (sideIso2Start != -1) {
				sideIso2Stop = findTriangleSide(currentTriangle, endExcluded,
						sideIso2Start, posIso2Stop);
			}
		}

		// /////////////////////////////
		// Split by specified parameters
		// Final possibilities
		if ((sideIso1Start == -1 && sideIso2Start == -1)
				&& ((vertIso1Start != -1 && vertIso1Stop == -1) || (vertIso2Start != -1 && vertIso2Stop == -1))) {
			// Only one vertex in the range domain
			if ((vertIso1Start != -1 && currentTriangle
					.getMaxMarker(vertIso1Start) < beginIncluded)
					|| (vertIso2Start != -1 && currentTriangle
							.getMinMarker(vertIso2Start) > endExcluded)) {
				return false;
			} else {
				// Covered totally by the range
				intervalTriangles.add(currentTriangle);
				return true;
			}
		} else if ((vertIso1Start == -1 && sideIso1Start == -1
				&& vertIso2Start == -1 && sideIso2Start == -1)
				|| // No iso limits inside the triangle
				(vertIso1Start != -1 && vertIso1Stop != -1 && vertIso2Start != -1)
				|| // Side == Iso 1 and the third vertice == Iso2
				(vertIso2Start != -1 && vertIso2Stop != -1 && vertIso1Start != -1) // Side
																					// ==
																					// Iso
																					// 2
																					// and
																					// the
																					// third
																					// vertice
																					// ==
																					// Iso1
		) { // Covered totally by the range
			intervalTriangles.add(currentTriangle);
			return true;
		} else if (((vertIso1Start != -1 || sideIso1Start != -1) && !((vertIso2Start != -1 || sideIso2Start != -1))) // Range
																														// begin
																														// found,
																														// but
																														// not
																														// range
																														// end
				|| ((vertIso2Start != -1 || sideIso2Start != -1) && !(vertIso1Start != -1 || sideIso1Start != -1))) // Range
																													// begin
																													// not
																													// found
																													// but
		{
			// Side to side
			if (sideIso1Start != -1 && sideIso1Stop != -1) {
				// Split triangle in three
				// ///////////////////////////////////
				// First triangle in the shared vertex side
				// Find the shared vertex between each

				TriMarkers aloneTri = new TriMarkers(), firstTwinTri = new TriMarkers(), secondTwinTri = new TriMarkers();
				short sharedVertex = getSplittedTriangle(sideIso1Start,
						sideIso1Stop, posIso1Start, posIso1Stop, beginIncluded,
						currentTriangle, aloneTri, firstTwinTri, secondTwinTri);

				if (currentTriangle.getMarker(sharedVertex) < beginIncluded) {
					outsideTriangles.add(aloneTri);

					// ///////////////////////////////////
					// Second and third triangle, at the other side as interval
					// surface

					intervalTriangles.add(firstTwinTri);
					intervalTriangles.add(secondTwinTri);
					return true;
				} else {
					intervalTriangles.add(aloneTri);

					// ///////////////////////////////////
					// Second and third triangle, at the other side as external
					// surface
					outsideTriangles.add(firstTwinTri);
					outsideTriangles.add(secondTwinTri);
					return true;
				}
			} else if (sideIso2Start != -1 && sideIso2Stop != -1) {
				// Split triangle in three
				// ///////////////////////////////////
				// First triangle in the shared vertex side
				// Find the shared vertex between each
				TriMarkers aloneTri = new TriMarkers(), firstTwinTri = new TriMarkers(), secondTwinTri = new TriMarkers();
				short sharedVertex = getSplittedTriangle(sideIso2Start,
						sideIso2Stop, posIso2Start, posIso2Stop, endExcluded,
						currentTriangle, aloneTri, firstTwinTri, secondTwinTri);
				if (currentTriangle.getMarker(sharedVertex) > endExcluded) {
					outsideTriangles.add(aloneTri);

					// ///////////////////////////////////
					// Second and third triangle, at the other side as interval
					// surface

					intervalTriangles.add(firstTwinTri);
					intervalTriangles.add(secondTwinTri);
					return true;
				} else {
					intervalTriangles.add(aloneTri);

					// ///////////////////////////////////
					// Second and third triangle, at the other side as external
					// surface
					outsideTriangles.add(firstTwinTri);
					outsideTriangles.add(secondTwinTri);
					return true;
				}
			}
			// Only One range found
			if ((vertIso1Start != -1 && vertIso1Stop != -1)
					|| (vertIso2Start != -1 && vertIso2Stop != -1)) {
				// Case side covered by iso
				short thirdVert = -1;
				if (vertIso1Start != 0 && vertIso1Stop != 0
						&& vertIso2Start != 0 && vertIso2Stop != 0) {
					thirdVert = 0;
				}
				if (vertIso1Start != 1 && vertIso1Stop != 1
						&& vertIso2Start != 1 && vertIso2Stop != 1) {
					thirdVert = 1;
				}
				if (vertIso1Start != 2 && vertIso1Stop != 2
						&& vertIso2Start != 2 && vertIso2Stop != 2) {
					thirdVert = 2;
				}
				if (currentTriangle.getMarker(thirdVert) >= beginIncluded
						&& currentTriangle.getMarker(thirdVert) < endExcluded) {
					intervalTriangles.add(currentTriangle);
					return true;
				} else {
					// Triangle is out of range
					return false;
				}
			}
			// Side to vertice
			if (vertIso1Start != -1 && sideIso1Start != -1) {
				// Split triangle in two
				short vertOutside = -1, vertInside = -1;
				if (currentTriangle.m1 < beginIncluded) {
					vertOutside = 0;
					if (vertIso1Start == 1) {
						vertInside = 2;
					} else {
						vertInside = 1;
					}
				} else if (currentTriangle.m2 < beginIncluded) {
					vertOutside = 1;
					if (vertIso1Start == 0) {
						vertInside = 2;
					} else {
						vertInside = 0;
					}
				} else if (currentTriangle.m3 < beginIncluded) {
					vertOutside = 2;
					if (vertIso1Start == 0) {
						vertInside = 1;
					} else {
						vertInside = 0;
					}
				}

				outsideTriangles.add(new TriMarkers(currentTriangle
						.getVertice(vertIso1Start), currentTriangle
						.getVertice(vertOutside), posIso1Start, beginIncluded,
						currentTriangle.getMarker(vertOutside), beginIncluded));
				intervalTriangles.add(new TriMarkers(currentTriangle
						.getVertice(vertIso1Start), currentTriangle
						.getVertice(vertInside), posIso1Start, beginIncluded,
						currentTriangle.getMarker(vertInside), beginIncluded));
				return true;
			} else if (vertIso2Start != -1 && sideIso2Start != -1) {
				// Split triangle in two
				short vertOutside = -1, vertInside = -1;
				if (currentTriangle.m1 < endExcluded) {
					vertOutside = 0;
					if (vertIso2Start == 1) {
						vertInside = 2;
					} else {
						vertInside = 1;
					}
				} else if (currentTriangle.m2 < endExcluded) {
					vertOutside = 1;
					if (vertIso2Start == 0) {
						vertInside = 2;
					} else {
						vertInside = 0;
					}
				} else if (currentTriangle.m3 < endExcluded) {
					vertOutside = 2;
					if (vertIso2Start == 0) {
						vertInside = 1;
					} else {
						vertInside = 0;
					}
				}

				outsideTriangles.add(new TriMarkers(currentTriangle
						.getVertice(vertIso2Start), currentTriangle
						.getVertice(vertOutside), posIso2Start, endExcluded,
						currentTriangle.getMarker(vertOutside), endExcluded));
				intervalTriangles.add(new TriMarkers(currentTriangle
						.getVertice(vertIso2Start), currentTriangle
						.getVertice(vertInside), posIso2Start, endExcluded,
						currentTriangle.getMarker(vertInside), endExcluded));
				return true;
			}

		} else {
			// Begin and end range inside the triangle

			// First step, make outside inferior triangle
			LinkedList<TriMarkers> insideTriangles = new LinkedList<TriMarkers>();
			splitInterval(beginIncluded, Double.POSITIVE_INFINITY,
					currentTriangle, outsideTriangles, insideTriangles);
			// distribute inside and outside superior triangle from the end iso
			for (TriMarkers insideTri : insideTriangles) {
				splitInterval(Double.NEGATIVE_INFINITY, endExcluded, insideTri,
						outsideTriangles, intervalTriangles);
			}
			return true;
		}
		// Unknown case throw
		throw new FunctionException(
				"Unhandled triangle splitting case :\n vertIso1Start("
						+ vertIso1Start + "), vertIso1Stop(" + vertIso1Stop
						+ "), vertIso2Start(" + vertIso2Start
						+ "), vertIso2Stop(" + vertIso2Stop
						+ "), sideIso1Start(" + sideIso1Start
						+ "), sideIso1Stop(" + sideIso1Stop
						+ "), sideIso2Start(" + sideIso2Start
						+ "), sideIso2Stop(" + sideIso2Stop + ")");
	}

	@Override
	public DataSet evaluate(SQLDataSourceFactory dsf, DataSet[] tables,
            Value[] values, ProgressMonitor pm) throws FunctionException {
		try {
			// Declare source and Destination tables
			final DataSet sds = tables[0];
			// Open source and Destination tables

			// Set defaultGeom as the geom set by the user
			// the_geom, 'field_vert1' ,'field_vert2','field_vert3',
			// '15,20,30,50,80'
			String spatialUpdateFieldName = values[0].toString();
			int spatialFieldIndex=sds.getMetadata().getFieldIndex(spatialUpdateFieldName);			
			int vertex1FieldIndex = sds.getMetadata().getFieldIndex(values[1]
					.getAsString());
			int vertex2FieldIndex = sds.getMetadata().getFieldIndex(values[2]
					.getAsString());
			int vertex3FieldIndex = sds.getMetadata().getFieldIndex(values[3]
					.getAsString());
			String isolevels_str = values[4].getAsString();
			LinkedList<Double> iso_lvls = new LinkedList<Double>();
			for (String isolvl : isolevels_str.split(",")) {
				iso_lvls.add(Double.valueOf(isolvl));
			}

			Metadata metaSource = sds.getMetadata();
			DefaultMetadata metadata = new DefaultMetadata();

			metadata.addField("the_geom", Type.GEOMETRY);
			String fieldDist = MetadataUtilities.getUniqueFieldName(metadata,
					"idiso");
			metadata.addField(fieldDist, Type.SHORT);
			LinkedList<Integer> fieldIdToExtract = new LinkedList<Integer>();
			for (int fieldid = 0; fieldid < metaSource.getFieldCount(); fieldid++) {
				String fieldName = metaSource.getFieldName(fieldid);
				if (fieldName != spatialUpdateFieldName
						&& fieldid != vertex1FieldIndex
						&& fieldid != vertex2FieldIndex
						&& fieldid != vertex3FieldIndex) {
					fieldIdToExtract.add(fieldid);
					String oldfield = MetadataUtilities.getUniqueFieldName(
							metadata, fieldName);
					metadata.addField(oldfield,
							metaSource.getFieldType(fieldid));
				}
			}
			final DiskBufferDriver driver = new DiskBufferDriver(dsf, metadata);
			GeometryFactory factory = new GeometryFactory();
			final long rowCount = sds.getRowCount();
			// For each triangle
			for (long rowIndex = 0; rowIndex < rowCount; rowIndex++) {
				final Geometry geometry = sds.getFieldValue(rowIndex, spatialFieldIndex).getAsGeometry();
				if (geometry instanceof Polygon && geometry.getNumPoints() == 4) {
					Coordinate[] pts = geometry.getCoordinates();
					if (Double.isNaN(pts[0].z)) {
						pts[0].z = 0;
					}
					if (Double.isNaN(pts[1].z)) {
						pts[1].z = 0;
					}
					if (Double.isNaN(pts[2].z)) {
						pts[2].z = 0;
					}
					TriMarkers currentTriangle = new TriMarkers(pts[0], pts[1],
							pts[2], sds.getFieldValue(rowIndex, vertex1FieldIndex).getAsDouble(),
							sds.getFieldValue(rowIndex, vertex2FieldIndex).getAsDouble(),
							sds.getFieldValue(rowIndex, vertex3FieldIndex).getAsDouble());

					// For each iso interval
					LinkedList<TriMarkers> triangleToProcess = new LinkedList<TriMarkers>();
					triangleToProcess.add(currentTriangle);

					do {
						currentTriangle = triangleToProcess.pop();
						Double beginInterval = Double.NEGATIVE_INFINITY;
						short isolvl = 0;
						for (Double endInterval : iso_lvls) {
							LinkedList<TriMarkers> triangleToDriver = new LinkedList<TriMarkers>();
							if (splitInterval(beginInterval, endInterval,
									currentTriangle, triangleToProcess,
									triangleToDriver)) {
								// triangleToProcess has been split. Then we
								// can't use it with the next iso levels.
								// We write to triangleToDriver the range
								// triangles, then process the list while its
								// not empty.
								for (TriMarkers triExport : triangleToDriver) {
									final Value[] newValues = new Value[2 + fieldIdToExtract
											.size()];
									Coordinate[] pverts = { triExport.p0,
											triExport.p1, triExport.p2,
											triExport.p0 };
									newValues[0] = ValueFactory
											.createValue(factory.createPolygon(
													factory.createLinearRing(pverts),
													null));
									newValues[1] = ValueFactory
											.createValue(isolvl);
									int destfield = 2;
									for (Integer srcFieldid : fieldIdToExtract) {
										newValues[destfield] = sds.getFieldValue(rowIndex, srcFieldid);
										destfield++;
									}
									driver.addValues(newValues);
								}
								// Exit the ISO for loop, we need to process
								// from the beginning of the iso values (other
								// triangle)

								break;
							}
							beginInterval = endInterval;
							isolvl++;
						}
					} while (!triangleToProcess.isEmpty());
				}
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
                            ScalarArgument.STRING,
                            ScalarArgument.STRING,
                            ScalarArgument.STRING,
                            ScalarArgument.STRING)
                    };
    }

}