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
package org.noise_planet.noisemodelling.pathfinder;

import junit.framework.TestCase;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author SU Qi
 */
public class Test3DPropagation extends TestCase{

    @Test
    public void testChangePlan() {
        GeometryFactory factory = new GeometryFactory();
        List<Coordinate> coords = JTSUtility.getNewCoordinateSystem(Arrays.asList(new Coordinate(5, 5, 5), new Coordinate(10, 5, 6)));
        List<Coordinate> coordsInv = JTSUtility.getNewCoordinateSystem(Arrays.asList(new Coordinate(10, 5, 6), new Coordinate(5, 5, 5)));
        assertEquals(coords.get(0).y, coordsInv.get(1).y);
        assertEquals(factory.createLineString(coords.toArray(new Coordinate[coords.size()])).getLength(),
                factory.createLineString(coordsInv.toArray(new Coordinate[coordsInv.size()])).getLength(), 1e-12);
        coords = JTSUtility.getNewCoordinateSystem(Arrays.asList(new Coordinate(5, 5, 5), new Coordinate(6, 5, 5.5), new Coordinate(10, 5, 6)));
        coordsInv = JTSUtility.getNewCoordinateSystem(Arrays.asList(new Coordinate(10, 5, 6), new Coordinate(6, 5, 5.5), new Coordinate(5, 5, 5)));
        assertEquals(factory.createLineString(coords.toArray(new Coordinate[coords.size()])).getLength(),
                factory.createLineString(coordsInv.toArray(new Coordinate[coordsInv.size()])).getLength(), 1e-12);
    }

    @Test
    public void testChangePlan2() {
        Coordinate[] pts = new Coordinate[] { new Coordinate(38.0, 14.0, 1.0),
                new Coordinate(56.78816787229409, 17.25389284165093, 2.857552975310886),
                new Coordinate(60.30669482663231, 17.8632609156269, 5.0),
                new Coordinate(61.01835202171596, 17.986511690717474, 5.0),
                new Coordinate(61.62431382421983, 18.09145724926706, 5.0),
                new Coordinate(66.03640021193571, 18.85557945699466, 2.340442869150613),
                new Coordinate(107.0, 25.95, 4.0)};
        List<Coordinate> coords = JTSUtility.getNewCoordinateSystem(Arrays.asList(pts));
        assertEquals(pts[0].distance3D(pts[2]), coords.get(0).distance(coords.get(2)), 1e-6);
    }
    
}
