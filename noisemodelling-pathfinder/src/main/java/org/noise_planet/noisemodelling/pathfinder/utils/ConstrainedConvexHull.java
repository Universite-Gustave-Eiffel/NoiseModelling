/**
 * Copyright (c) 2016 Vivid Solutions.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

/**
 * Most of this class is extracted from the class ConvexHull from jts
 */

package org.noise_planet.noisemodelling.pathfinder.utils;

import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.algorithm.PointLocation;
import org.locationtech.jts.geom.*;
        import org.locationtech.jts.util.Assert;
import org.locationtech.jts.util.UniqueCoordinateArrayFilter;

import java.util.*;


public class ConstrainedConvexHull {
    private GeometryFactory geomFactory;
    private Coordinate[] inputPts;

    public ConstrainedConvexHull(Geometry geometry) {
        this(extractCoordinates(geometry), geometry.getFactory());
    }

    public ConstrainedConvexHull(Coordinate[] pts, GeometryFactory geomFactory) {
        this.inputPts = UniqueCoordinateArrayFilter.filterCoordinates(pts);
        this.geomFactory = geomFactory;
    }

    private static Coordinate[] extractCoordinates(Geometry geom) {
        UniqueCoordinateArrayFilter filter = new UniqueCoordinateArrayFilter();
        geom.apply(filter);
        return filter.getCoordinates();
    }

    public Geometry getConvexHull(Coordinate a, Coordinate b) {
        if (this.inputPts.length == 0) {
            return this.geomFactory.createGeometryCollection();
        } else if (this.inputPts.length == 1) {
            return this.geomFactory.createPoint(this.inputPts[0]);
        } else if (this.inputPts.length == 2) {
            return this.geomFactory.createLineString(this.inputPts);
        } else {
            Coordinate[] reducedPts = this.inputPts;
            if (this.inputPts.length > 50) {
                reducedPts = this.reduce(this.inputPts);
            }

            Coordinate[] sortedPts = this.preSort(reducedPts);
            Stack cHS = this.grahamScan(sortedPts, a, b);
            Coordinate[] cH = this.toCoordinateArray(cHS);
            return this.lineOrPolygon(cH);
        }
    }

    protected Coordinate[] toCoordinateArray(Stack stack) {
        Coordinate[] coordinates = new Coordinate[stack.size()];

        for(int i = 0; i < stack.size(); ++i) {
            Coordinate coordinate = (Coordinate)stack.get(i);
            coordinates[i] = coordinate;
        }

        return coordinates;
    }

    private Coordinate[] reduce(Coordinate[] inputPts) {
        Coordinate[] polyPts = this.computeOctRing(inputPts);
        if (polyPts == null) {
            return inputPts;
        } else {
            TreeSet reducedSet = new TreeSet();

            int i;
            for(i = 0; i < polyPts.length; ++i) {
                reducedSet.add(polyPts[i]);
            }

            for(i = 0; i < inputPts.length; ++i) {
                if (!PointLocation.isInRing(inputPts[i], polyPts)) {
                    reducedSet.add(inputPts[i]);
                }
            }

            Coordinate[] reducedPts = CoordinateArrays.toCoordinateArray(reducedSet);
            if (reducedPts.length < 3) {
                return this.padArray3(reducedPts);
            } else {
                return reducedPts;
            }
        }
    }

    private Coordinate[] padArray3(Coordinate[] pts) {
        Coordinate[] pad = new Coordinate[3];

        for(int i = 0; i < pad.length; ++i) {
            if (i < pts.length) {
                pad[i] = pts[i];
            } else {
                pad[i] = pts[0];
            }
        }

        return pad;
    }

    private Coordinate[] preSort(Coordinate[] pts) {
        for(int i = 1; i < pts.length; ++i) {
            if (pts[i].y < pts[0].y || pts[i].y == pts[0].y && pts[i].x < pts[0].x) {
                Coordinate t = pts[0];
                pts[0] = pts[i];
                pts[i] = t;
            }
        }

        Arrays.sort(pts, 1, pts.length, new RadialComparator(pts[0]));
        return pts;
    }

    private Stack<Coordinate> grahamScan(Coordinate[] c, Coordinate A, Coordinate B) {
        Stack<Coordinate> ps = new Stack<>();

        // Inicializamos el stack con el primer punto, asegurándonos de que sea A.
        ps.push(A);

        // Empezamos el escaneo desde el segundo punto.
        for (int i = 1; i < c.length; i++) {
            Coordinate current = c[i];

            // Si el punto actual es B, lo añadimos directamente y pasamos al siguiente punto.
            if (current.equals(B)) {
                ps.push(B);
                continue;
            }

            // Descartamos puntos que no mantienen la orientación correcta.
            Coordinate p;
            for (p = ps.pop();
                 !ps.empty() && Orientation.index(ps.peek(), p, current) > 0;
                 p = ps.pop()) {

                // Si p es B, lo volvemos a insertar inmediatamente para no descartarlo.
                if (p.equals(B)) {
                    ps.push(p);
                    break;
                }
            }

            ps.push(p);
            ps.push(current);
        }

        // Agregamos el punto inicial A para cerrar la envolvente convexa.
        ps.push(A);

        return ps;
    }


    private boolean isBetween(Coordinate c1, Coordinate c2, Coordinate c3) {
        if (Orientation.index(c1, c2, c3) != 0) {
            return false;
        } else {
            if (c1.x != c3.x) {
                if (c1.x <= c2.x && c2.x <= c3.x) {
                    return true;
                }

                if (c3.x <= c2.x && c2.x <= c1.x) {
                    return true;
                }
            }

            if (c1.y != c3.y) {
                if (c1.y <= c2.y && c2.y <= c3.y) {
                    return true;
                }

                if (c3.y <= c2.y && c2.y <= c1.y) {
                    return true;
                }
            }

            return false;
        }
    }

    private Coordinate[] computeOctRing(Coordinate[] inputPts) {
        Coordinate[] octPts = this.computeOctPts(inputPts);
        CoordinateList coordList = new CoordinateList();
        coordList.add(octPts, false);
        if (coordList.size() < 3) {
            return null;
        } else {
            coordList.closeRing();
            return coordList.toCoordinateArray();
        }
    }

    private Coordinate[] computeOctPts(Coordinate[] inputPts) {
        Coordinate[] pts = new Coordinate[8];

        int i;
        for(i = 0; i < pts.length; ++i) {
            pts[i] = inputPts[0];
        }

        for(i = 1; i < inputPts.length; ++i) {
            if (inputPts[i].x < pts[0].x) {
                pts[0] = inputPts[i];
            }

            if (inputPts[i].x - inputPts[i].y < pts[1].x - pts[1].y) {
                pts[1] = inputPts[i];
            }

            if (inputPts[i].y > pts[2].y) {
                pts[2] = inputPts[i];
            }

            if (inputPts[i].x + inputPts[i].y > pts[3].x + pts[3].y) {
                pts[3] = inputPts[i];
            }

            if (inputPts[i].x > pts[4].x) {
                pts[4] = inputPts[i];
            }

            if (inputPts[i].x - inputPts[i].y > pts[5].x - pts[5].y) {
                pts[5] = inputPts[i];
            }

            if (inputPts[i].y < pts[6].y) {
                pts[6] = inputPts[i];
            }

            if (inputPts[i].x + inputPts[i].y < pts[7].x + pts[7].y) {
                pts[7] = inputPts[i];
            }
        }

        return pts;
    }

    private Geometry lineOrPolygon(Coordinate[] coordinates) {
        coordinates = this.cleanRing(coordinates);
        if (coordinates.length == 3) {
            return this.geomFactory.createLineString(new Coordinate[]{coordinates[0], coordinates[1]});
        } else {
            LinearRing linearRing = this.geomFactory.createLinearRing(coordinates);
            return this.geomFactory.createPolygon(linearRing);
        }
    }

    private Coordinate[] cleanRing(Coordinate[] original) {
        Assert.equals(original[0], original[original.length - 1]);
        ArrayList cleanedRing = new ArrayList();
        Coordinate previousDistinctCoordinate = null;

        for(int i = 0; i <= original.length - 2; ++i) {
            Coordinate currentCoordinate = original[i];
            Coordinate nextCoordinate = original[i + 1];
            if (!currentCoordinate.equals(nextCoordinate) && (previousDistinctCoordinate == null || !this.isBetween(previousDistinctCoordinate, currentCoordinate, nextCoordinate))) {
                cleanedRing.add(currentCoordinate);
                previousDistinctCoordinate = currentCoordinate;
            }
        }

        cleanedRing.add(original[original.length - 1]);
        Coordinate[] cleanedRingCoordinates = new Coordinate[cleanedRing.size()];
        return (Coordinate[])((Coordinate[])cleanedRing.toArray(cleanedRingCoordinates));
    }

    private static class RadialComparator implements Comparator {
        private Coordinate origin;

        public RadialComparator(Coordinate origin) {
            this.origin = origin;
        }

        public int compare(Object o1, Object o2) {
            Coordinate p1 = (Coordinate)o1;
            Coordinate p2 = (Coordinate)o2;
            return polarCompare(this.origin, p1, p2);
        }

        private static int polarCompare(Coordinate o, Coordinate p, Coordinate q) {
            double dxp = p.x - o.x;
            double dyp = p.y - o.y;
            double dxq = q.x - o.x;
            double dyq = q.y - o.y;
            int orient = Orientation.index(o, p, q);
            if (orient == 1) {
                return 1;
            } else if (orient == -1) {
                return -1;
            } else {
                double op = dxp * dxp + dyp * dyp;
                double oq = dxq * dxq + dyq * dyq;
                if (op < oq) {
                    return -1;
                } else {
                    return op > oq ? 1 : 0;
                }
            }
        }
    }
}
