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
package org.orbisgis.noisemap.core;
import java.util.*;
/**
 * The Jarvis March, sometimes known as the Gift Wrap Algorithm.
 * The next point is the point with the next largest angle.
 * <p/>
 * Imagine wrapping a string around a set of nails in a board.  Tie the string to the leftmost nail
 * and hold the string vertical.  Now move the string clockwise until you hit the next, then the next, then
 * the next.  When the string is vertical again, you will have found the hull.
 *  
 * @link http://butunclebob.com/ArticleS.UncleBob.ConvexHullTiming
 * @author UncleBob
 */

public class JarvisMarch {
  Points pts;
  private Points hullPoints = null;
  private List<Double> hy;
  private List<Double> hx;
  private List<Integer> hi;
  private int startingPoint;
  private double currentAngle;
  private static final double MAX_ANGLE = 4;

  public JarvisMarch(Points pts) {
    this.pts = pts;
  }
 

  public Points calculateHull() {
    initializeHull();

    startingPoint = getStartingPoint();
    currentAngle = 0;

    addToHull(startingPoint);
    for (int p = getNextPoint(startingPoint); p != startingPoint; p = getNextPoint(p))
      addToHull(p);

    buildHullPoints();
    return this.hullPoints;
  }

  public int getStartingPoint() {
    return pts.startingPoint();
  }

  private int getNextPoint(int p) {
    double minAngle = MAX_ANGLE;
    int minP = startingPoint;
    for (int i = 0; i < pts.x.length; i++) {
      if (i != p) {
        double thisAngle = relativeAngle(i, p);
        if (thisAngle >= currentAngle && thisAngle <= minAngle) {
          minP = i;
          minAngle = thisAngle;
        }
      }
    }
    currentAngle = minAngle;
    return minP;
  }

  private double relativeAngle(int i, int p) {return pseudoAngle(pts.x[i] - pts.x[p], pts.y[i] - pts.y[p]);}

  private void initializeHull() {
    hx = new LinkedList<Double>();
    hy = new LinkedList<Double>();
    hi = new LinkedList<>();
  }

  private void buildHullPoints() {
    double[] ax = new double[hx.size()];
    double[] ay = new double[hy.size()];
    int n = 0;
    for (Iterator<Double> ix = hx.iterator(); ix.hasNext();)
      ax[n++] = ix.next();

    n = 0;
    for (Iterator<Double> iy = hy.iterator(); iy.hasNext();)
      ay[n++] = iy.next();

    hullPoints = new Points(ax, ay);
  }

  private void addToHull(int p) {
    hx.add(pts.x[p]);
    hy.add(pts.y[p]);
    hi.add(p);
  }

  /**
   * The PseudoAngle is a number that increases as the angle from vertical increases.
   * The current implementation has the maximum pseudo angle < 4.  The pseudo angle for each quadrant is 1.
   * The algorithm is very simple.  It just finds where the angle intersects a square and measures the
   * perimeter of the square at that point.  The math is in my Sept '06 notebook.  UncleBob.
   */
  public static double pseudoAngle(double dx, double dy) {
    if (dx >= 0 && dy >= 0)
      return quadrantOnePseudoAngle(dx, dy);
    if (dx >= 0 && dy < 0)
      return 1 + quadrantOnePseudoAngle(Math.abs(dy), dx);
    if (dx < 0 && dy < 0)
      return 2 + quadrantOnePseudoAngle(Math.abs(dx), Math.abs(dy));
    if (dx < 0 && dy >= 0)
      return 3 + quadrantOnePseudoAngle(dy, Math.abs(dx));
    throw new Error("Impossible");
  }

  public static double quadrantOnePseudoAngle(double dx, double dy) {
    return dx / (dy + dx);
  }

  public Points getHullPoints() {
    return hullPoints;
  }

  public List<Integer> getHullPointId() {
      return hi;
  }

  public static class Points {
    public double x[];
    public double y[];

    public Points(double[] x, double[] y) {
      this.x = x;
      this.y = y;
    }

    // The starting point is the point with the lowest X
    // With ties going to the lowest Y.  This guarantees
    // that the next point over is clockwise.
    int startingPoint() {
      double minY = y[0];  
      double minX = x[0];
      int iMin = 0;
      for (int i = 1; i < x.length; i++) {
        if (x[i] < minX) {
          minX = x[i];
          iMin = i;
        } else if (minX == x[i] && y[i] < minY) {
          minY = y[i];
          iMin = i;
        }
      }
      return iMin;
    }

  }
}