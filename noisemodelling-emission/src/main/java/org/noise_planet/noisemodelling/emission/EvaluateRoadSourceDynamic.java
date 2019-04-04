/**
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact on urban mobility plans. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 * <p>
 * This version is developed at French IRSTV Institute and at IFSTTAR
 * (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 * <p>
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
 * <p>
 * Copyright (C) 2011 IFSTTAR
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
 * <p>
 * Noisemap is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * Noisemap. If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.noise_planet.noisemodelling.emission;
import org.noise_planet.noisemodelling.propagation.ComputeRays;

import java.util.Random;

/**
 * Return the dB value corresponding to the parameters
 * Reference document is Common Noise Assessment Methods in Europe(CNOSSOS-EU), 2012
 * Stylianos Kephalopoulos, Marco Paviotti, Fabienne Anfosso-Ledee
 * https://ec.europa.eu/jrc/sites/jrcsh/files/cnossos-eu%2520jrc%2520reference%2520report_final_on%2520line%2520version_10%2520august%25202012.pdf
 * @author Nicolas Fortin
 * @author Pierre Aumond - 27/04/2017 - 21/08/2018
 * @author Arnaud Can - 27/02/2018 - 21/08/2018
 */

public class EvaluateRoadSourceDynamic {

    /** acceleration coeff **/
    private static final double[][] Coeff_Acc = {
            {-4.5, 5.5, -4.4, 3.1}, //Table III.A.6 p.44 - Confirm data with CNOSSOS-EU phase B p.44
            {-4.0, 9, -2.3, 6.7},
            {-4.0, 9, -2.3, 6.7},
            {0, 0, 0, 0},
            {0, 0, 0, 0}};

    /** Road Categories -  All surface are from CNOSSOS-EU_Road_Catalogue_Final - 01April2014.xlsx **/

    /*private static final double[][] RoadCoeff_NL01 = { // 1-layer ZOAB
            {0.5, 0.9, 0.9, 0.0, 0.0, 0.0},
            {3.3, 1.4, 1.4, 0., 0., 0.},
            {2.4, 1.8, 1.8, 0., 0., 0.},
            {3.2, -0.4, -0.4, 0., 0., 0.},
            {-1.3, -5.2, -5.2, 0., 0., 0.},
            {-3.5, -4.6, -4.6, 0., 0., 0.},
            {-2.6, -3.0, -3.0, 0., 0., 0.},
            {0.5, -1.4, -1.4, 0., 0., 0.},
            {-6.5, 0.2, 0.2, 0., 0., 0.}, {50, 130}};*/

    private static final double[][] RoadCoeff_NL01 = { // 1-layer ZOAB
            {0.0, 0.9, 0.9, 0.0, 0.0, 0.0},
            {5.4, 1.4, 1.4, 0., 0., 0.},
            {4.3, 1.8, 1.8, 0., 0., 0.},
            {4.2, -0.4, -0.4, 0., 0., 0.},
            {-1.0, -5.2, -5.2, 0., 0., 0.},
            {-3.2, -4.6, -4.6, 0., 0., 0.},
            {-2.6, -3.0, -3.0, 0., 0., 0.},
            {0.8, -1.4, -1.4, 0., 0., 0.},
            {-6.5, 0.2, 0.2, 0., 0., 0.}, {50, 130}};

  /*  private static final double[][] RoadCoeff_NL02 = { // 2-layer ZOAB
            {0.4, 0.4, 0.4, 0., 0., 0.},
            {2.4, 0.2, 0.2, 0., 0., 0.},
            {0.2, -0.7, -0.7, 0., 0., 0.},
            {-3.1, -5.4, -5.4, 0., 0., 0.},
            {-4.2, -6.3, -6.3, 0., 0., 0.},
            {-6.3, -6.3, -6.3, 0., 0., 0.},
            {-4.8, -4.7, -4.7, 0., 0., 0.},
            {-2.0, -3.7, -3.7, 0., 0., 0.},
            {-3., 4.7, 4.7, 0., 0., 0.}, {50, 130}};*/
    private static final double[][] RoadCoeff_NL02 = { // 2-layer ZOAB
            {1.6, 0.4, 0.4, 0., 0., 0.},
            {4.0, 0.2, 0.2, 0., 0., 0.},
            {0.3, -0.7, -0.7, 0., 0., 0.},
            {-3.0, -5.4, -5.4, 0., 0., 0.},
            {-4.0, -6.3, -6.3, 0., 0., 0.},
            {-6.2, -6.3, -6.3, 0., 0., 0.},
            {-4.8, -4.7, -4.7, 0., 0., 0.},
            {-2.0, -3.7, -3.7, 0., 0., 0.},
            {-3., 4.7, 4.7, 0., 0., 0.}, {50, 130}};

    private static final double[][] RoadCoeff_NL03 = { // 2-layer ZOAB (fine)
            {-1., 1., 1., 0., 0., 0.},
            {1.7, 0.1, 0.1, 0., 0., 0.},
            {-1.5, -1.8, -1.8, 0., 0., 0.},
            {-5.3, -5.9, -5.9, 0., 0., 0.},
            {-6.3, -6.1, -6.1, 0., 0., 0.},
            {-8.5, -6.7, -6.7, 0., 0., 0.},
            {-5.3, -4.8, -4.8, 0., 0., 0.},
            {-2.4, -3.8, -3.8, 0., 0., 0.},
            {-0.1, -0.8, -0.8, 0., 0., 0.}, {80, 130}};

    private static final double[][] RoadCoeff_NL04 = { // SMA-NL5
            {1.1, 0., 0., 0., 0., 0.},
            {-1.0, 0., 0., 0., 0., 0.},
            {0.2, 0., 0., 0., 0., 0.},
            {1.3, 0., 0., 0., 0., 0.},
            {-1.9, 0., 0., 0., 0., 0.},
            {-2.8, 0., 0., 0., 0., 0.},
            {-2.1, 0., 0., 0., 0., 0.},
            {-1.4, 0., 0., 0., 0., 0.},
            {-1.0, 0., 0., 0., 0., 0.}, {40, 80}};

    /*private static final double[][] RoadCoeff_NL05 = { // SMA-NL8
            {0.3, 0., 0., 0., 0., 0.},
            {0., 0., 0., 0., 0., 0.},
            {0., 0., 0., 0., 0., 0.},
            {-0.1, 0., 0., 0., 0., 0.},
            {-0.7, 0., 0., 0., 0., 0.},
            {-1.3, 0., 0., 0., 0., 0.},
            {-0.8, 0., 0., 0., 0., 0.},
            {-0.8, 0., 0., 0., 0., 0.},
            {-1.0, 0., 0., 0., 0., 0.}, {40, 80}};*/

    private static final double[][] RoadCoeff_NL05 = { // SMA-NL8
            {6, 0., 0., 0., 0., 0.},
            {0.3, 0., 0., 0., 0., 0.},
            {0.3, 0., 0., 0., 0., 0.},
            {0.0, 0., 0., 0., 0., 0.},
            {-0.6, 0., 0., 0., 0., 0.},
            {-1.2, 0., 0., 0., 0., 0.},
            {-0.7, 0., 0., 0., 0., 0.},
            {-0.7, 0., 0., 0., 0., 0.},
            {-1.4, 0., 0., 0., 0., 0.}, {40, 80}};

    private static final double[][] RoadCoeff_NL06 = { // Brushed down concrete
            {1.1, 0., 0., 0., 0., 0.},
            {-0.4, 1.1, 1.1, 0., 0., 0.},
            {1.3, 0.4, 0.4, 0., 0., 0.},
            {2.2, -0.3, -0.3, 0., 0., 0.},
            {2.5, -0.2, -0.2, 0., 0., 0.},
            {0.8, -0.7, -0.7, 0., 0., 0.},
            {-0.2, -1.1, -1.1, 0., 0., 0.},
            {-0.1, -1.0, -1.0, 0., 0., 0.},
            {1.4, 4.4, 4.4, 0., 0., 0.}, {70, 120}};

    private static final double[][] RoadCoeff_NL07 = { // Optimized brushed down concrete
            {-0.2, -0.3, -0.3, 0., 0., 0.},
            {-0.7, 1.0, 1.0, 0., 0., 0.},
            {0.6, -1.7, -1.7, 0., 0., 0.},
            {1., -1.2, -1.2, 0., 0., 0.},
            {1.1, -1.6, -1.6, 0., 0., 0.},
            {-1.5, -2.4, -2.4, 0., 0., 0.},
            {-2., -1.7, -1.7, 0., 0., 0.},
            {-1.8, -1.7, -1.7, 0., 0., 0.},
            {1., -6.6, -6.6, 0., 0., 0.}, {70, 80}};

    private static final double[][] RoadCoeff_NL08 = { // Fine broomed concrete
            {1.1, 0., 0., 0., 0., 0.},
            {-0.5, 3.3, 3.3, 0., 0., 0.},
            {2.7, 2.4, 2.4, 0., 0., 0.},
            {2.1, 1.9, 1.9, 0., 0., 0.},
            {1.6, 2.0, 2.0, 0., 0., 0.},
            {2.7, 1.2, 1.2, 0., 0., 0.},
            {1.3, 0.1, 0.1, 0., 0., 0.},
            {-0.4, 0., 0., 0., 0., 0.},
            {7.7, 3.7, 3.7, 0., 0., 0.}, {70, 120}};

    private static final double[][] RoadCoeff_NL09 = { // Worked surface
            {1.1, 0., 0., 0., 0., 0.},
            {1., 2., 2., 0., 0., 0.},
            {2.6, 1.8, 1.8, 0., 0., 0.},
            {4., 1., 1., 0., 0., 0.},
            {4., -0.7, -0.7, 0., 0., 0.},
            {0.1, -2.1, -2.1, 0., 0., 0.},
            {-1., -1.9, -1.9, 0., 0., 0.},
            {-0.8, -1.7, -1.7, 0., 0., 0.},
            {-0.2, 1.7, 1.7, 0., 0., 0.}, {50, 130}};

    private static final double[][] RoadCoeff_NL10 = { // Hard elements in herring-bone
            {8.3, 8.3, 8.3, 0., 0., 0.},
            {8.7, 8.7, 8.7, 0., 0., 0.},
            {7.8, 7.8, 7.8, 0., 0., 0.},
            {5., 5., 5., 0., 0., 0.},
            {3., 3., 3., 0., 0., 0.},
            {-0.7, -0.7, -0.7, 0., 0., 0.},
            {0.8, 0.8, 0.8, 0., 0., 0.},
            {1.8, 1.8, 1.8, 0., 0., 0.},
            {2.5, 2.5, 2.5, 0., 0., 0.}, {30, 60}};

    private static final double[][] RoadCoeff_NL11 = { // Hard elements not in herring-bone
            {12.3, 12.3, 12.3, 0., 0., 0.},
            {11.9, 11.9, 11.9, 0., 0., 0.},
            {9.7, 9.7, 9.7, 0., 0., 0.},
            {7.1, 7.1, 7.1, 0., 0., 0.},
            {7.1, 7.1, 7.1, 0., 0., 0.},
            {2.8, 2.8, 2.8, 0., 0., 0.},
            {4.7, 4.7, 4.7, 0., 0., 0.},
            {4.5, 4.5, 4.5, 0., 0., 0.},
            {2.9, 2.9, 2.9, 0., 0., 0.}, {30, 60}};

    private static final double[][] RoadCoeff_NL12 = { // Quiet hard elements
            {7.8, 0.2, 0.2, 0., 0., 0.},
            {6.3, 0.7, 0.7, 0., 0., 0.},
            {5.2, 0.7, 0.7, 0., 0., 0.},
            {2.8, 1.1, 1.1, 0., 0., 0.},
            {-1.9, 1.8, 1.8, 0., 0., 0.},
            {-6.0, 1.2, 1.2, 0., 0., 0.},
            {-3., 1.1, 1.1, 0., 0., 0.},
            {-0.1, 0.2, 0.2, 0., 0., 0.},
            {-1.7, 0., 0., 0., 0., 0.}, {30, 60}};

    private static final double[][] RoadCoeff_NL13 = { // Thin layer A
            {1.1, 1.6, 1.6, 0., 0., 0.},
            {0.1, 1.3, 1.3, 0., 0., 0.},
            {-0.7, 0.9, 0.9, 0., 0., 0.},
            {-1.3, -0.4, -0.4, 0., 0., 0.},
            {-3.1, -1.8, -1.8, 0., 0., 0.},
            {-4.9, -2.1, -2.1, 0., 0., 0.},
            {-3.5, -0.7, -0.7, 0., 0., 0.},
            {-1.5, -0.2, -0.2, 0., 0., 0.},
            {-2.5, 0.5, 0.5, 0., 0., 0.}, {40, 130}};

    private static final double[][] RoadCoeff_NL14 = { // Thin layer B
            {0.4, 1.6, 1.6, 0., 0., 0.},
            {-1.3, 1.3, 1.3, 0., 0., 0.},
            {-1.3, 0.9, 0.9, 0., 0., 0.},
            {-0.4, -0.4, -0.4, 0., 0., 0.},
            {-5., -1.8, -1.8, 0., 0., 0.},
            {-7.1, -2.1, -2.1, 0., 0., 0.},
            {-4.9, -0.7, -0.7, 0., 0., 0.},
            {-3.3, -0.2, -0.2, 0., 0., 0.},
            {-1.5, 0.5, 0.5, 0., 0., 0.}, {40, 130}};
    /** French road pavement "Matching directive 2015/996/EC (CNOSSOS-EU)
     and the French emission model for road pavements", Dutilleux, Soldano, Euronoise 2018
     R1 (high acoustic performance): BBTM 0/6 types 1 et 2 - BBUM 0/6 - BBDr 0/10 - BBTM 0/10 type 2
     R2 (medium acoustic performance): BBTM 0/10 type 1 - BBSG 0/10 - ECF - BBUM 0/10
     R3 (low acoustic performance): BC - BBTM 0/14 - BBSG 0/14 - ES 6/10 - ES 10/14
     Drainant = Dr after the name
     **/

    private static final double[][] RoadCoeff_FR1 = { // R1 drainant (ajustement composante totale, Rapport Cerema 2016)
            {13.9, 23.3, 19, 0., 0., 0.},
            {14, 19.4, 15.5, 0., 0., 0.},
            {14.1, 15.5, 12, 0., 0., 0.},
            {8.7, 10.7, 5.8, 0., 0., 0.},
            {-2.5, 5.8, 1.5, 0., 0., 0.},
            {-3.9, 7.6, 3.3, 0., 0., 0.},
            {-0.5, 8.2, 4., 0., 0., 0.},
            {2.9, 8.9, 4.8, 0., 0., 0.},
            {-2.9, -0.1, 6.6, 0., 0., 0.}, {30, 130}};

    private static final double[][] RoadCoeff_FR2 = { // R1 non drainant(ajustement composante totale, Rapport Cerema 2016)
            {9, 18.6, 14.2, 0., 0., 0.},
            {9.6, 15.1, 11.1, 0., 0., 0.},
            {10.2, 11.7, 8.1, 0., 0., 0.},
            {5.8, 7.8, 2.8, 0., 0., 0.},
            {-0.3, 8.2, 3.8, 0., 0., 0.},
            {-2.7, 8.8, 4.4, 0., 0., 0.},
            {-0.7, 8.1, 3.8, 0., 0., 0.},
            {1.3, 7.5, 3.3, 0., 0., 0.},
            {-3.4, -1.3, 6.1, 0., 0., 0.}, {30, 130}};

    private static final double[][] RoadCoeff_FR3 = { // R2 drainant(ajustement composante totale, Rapport Cerema 2016)
            {15.7, 25.4, 21.4, 0., 0., 0.},
            {15.8, 21.5, 17.9, 0., 0., 0.},
            {15.9, 17.6, 14.4, 15.4, 0., 0.},
            {10.5, 12.8, 8.2, 0., 0., 0.},
            {-0.7, 7.9, 3.9, 0., 0., 0.},
            {-2.1, 9.7, 5.7, 0., 0., 0.},
            {1.3, 10.3, 6.4, 0., 0., 0.},
            {4.7, 11., 7.2, 0., 0., 0.},
            {-2.2, 0.3, 4.9, 0., 0., 0.}, {30, 130}};

    private static final double[][] RoadCoeff_FR4 = { // R2 non drainant(ajustement composante totale, Rapport Cerema 2016)
            {10.8, 20.6, 16.7, 0., 0., 0.},
            {11.4, 17.1, 13.6, 0., 0., 0.},
            {12, 13.7, 10.6, 0., 0., 0.},
            {7.5, 9.8, 5.3, 0., 0., 0.},
            {1.5, 10.2, 6.3, 0., 0., 0.},
            {-0.9, 10.8, 6.9, 0., 0., 0.},
            {1.1, 10.1, 6.3, 0., 0., 0.},
            {3.1, 9.5, 5.8, 0., 0., 0.},
            {-2.7, -1.2, 3.5, 0., 0., 0.}, {30, 130}};

    private static final double[][] RoadCoeff_FR5 = { // R3 drainant(ajustement composante totale, Rapport Cerema 2016)
            {17.5, 26.2, 22.4, 0., 0., 0.},
            {17.5, 22.3, 18.9, 0., 0., 0.},
            {17.6, 18.4, 15.4, 0., 0., 0.},
            {12.3, 13.6, 9.2, 0., 0., 0.},
            {1, 8.7, -1.8, 4.9, 0., 0.},
            {-0.3, 10.5, 6.7, 0., 0., 0.},
            {3.1, 11.1, 7.4, 0., 0., 0.},
            {6.4, 11.8, 8.2, 0., 0., 0.},
            {0.1, 0.4, 4.3, 0., 0., 0.}, {30, 130}};

    private static final double[][] RoadCoeff_FR6 = { // R3 non drainant(ajustement composante totale, Rapport Cerema 2016)
            {12.7, 21.5, 17.7, 0., 0., 0.},
            {13.2, 18, 14.6, 0., 0., 0.},
            {13.8, 14.6, 11.6, 0., 0., 0.},
            {9.4, 10.7, 6.3, 0., 0., 0.},
            {3.4, 11.1, 7.3, 0., 0., 0.},
            {0.9, 11.7, 7.9, 0., 0., 0.},
            {3, 11, 7.3, 0., 0., 0.},
            {5, 10.4, 6.8, 0., 0., 0.},
            {-0.8, -0.8, 2.8, 0., 0., 0.}, {30, 130}};

    /** CNOSSOS Vehicule Tables -  All coefficients are from CNOSSOS-EU_Road_Catalogue_Final - 01April2014.xlsx - Emission + Studded **/
    private static final double[][] VehCat1 = {   // light vehicle 2015
            {79.7, 30.0, 94.5, -1.3, 0., 0.},
            {85.7, 41.5, 89.2, 7.2, 0., 0.},
            {84.5, 38.9, 88.0, 7.7, 0., 0.},
            {90.2, 25.7, 85.9, 8.0, 2.6, -3.1},
            {97.3, 32.5, 84.2, 8.0, 2.9, -6.4},
            {93.9, 37.2, 86.9, 8.0, 1.5, -14},
            {84.1, 39.0, 83.3, 8.0, 2.3, -22.4},
            {74.3, 40.0, 76.1, 8.0, 9.2, -11.4}};
   /* private static final double[][] VehCat1 = {   // light vehicle 2018
            {83.4, 39.2, 98.0, 2.8, 0., 0.},
            {86.8, 37.5, 90.3, 6.1, 0., 0.},
            {86.1, 32.2, 89.7, 5.6, 0., 0.},
            {92.5, 18.4, 88.3, 5.4, 2.6, -3.1},
            {99.8, 24.9, 86.8, 5.1, 2.9, -6.4},
            {96.6, 25.8, 89.7, 3.5, 1.5, -14},
            {85.8, 32.1, 85.1, 5.3, 2.3, -22.4},
            {76.2, 35.1, 78.0, 6.3, 9.2, -11.4}};*/

    /*private static final double[][] VehCat1 = {   // light vehicle 2017
            {83.1, 30.0, 97.9, -1.3, 0., 0.},
            {89.2, 41.5, 92.5, 7.2, 0., 0.},
            {87.7, 38.9, 90.7, 7.7, 0., 0.},
            {93.1, 25.7, 87.2, 8.0, 2.6, -3.1},
            {100.1,32.5, 84.7, 8.0, 2.9, -6.4},
            {96.7, 37.2, 88.0, 8.0, 1.5, -14},
            {86.8, 39.0, 84.4, 8.0, 2.3, -22.4},
            {76.2, 40.0, 77.1, 8.0, 9.2, -11.4}};*/

    private static final double[][] VehCat2 = { // medium vehicle
            {84., 30.0, 101., -1.9, 0., 0.},
            {88.7, 35.8, 96.5, 4.7, 0., 0.},
            {91.5, 32.6, 98.8, 6.4, 0., 0.},
            {96.7, 23.8, 96.8, 6.5, 0., 0.},
            {97.4, 30.1, 98.6, 6.5, 0., 0.},
            {90.9, 36.2, 95.2, 6.5, 0., 0.},
            {83.8, 38.3, 88.8, 6.5, 0., 0.},
            {80.5, 40.1, 82.7, 6.5, 0., 0.}};

    private static final double[][] VehCat3 = {  // heavy vehicle
            {87.0, 30.0, 104.4, 0, 0., 0.},
            {91.7, 33.5, 100.6, 3.0, 0., 0.},
            {94.1, 31.3, 101.7, 4.6, 0., 0.},
            {100.7, 25.4, 101.0, 5.0, 0., 0.},
            {100.8, 31.8, 100.1, 5.0, 0., 0.},
            {94.3, 37.1, 95.9, 5.0, 0., 0.},
            {87.1, 38.6, 91.3, 5.0, 0., 0.},
            {82.5, 40.6, 85.3, 5.0, 0., 0.}};

    private static final double[][] VehCat41 = { // 2 wheels, type a
            {0., 0., 88., 4.2, 0., 0.},
            {0., 0., 87.5, 7.4, 0., 0.},
            {0., 0., 89.5, 9.8, 0., 0.},
            {0., 0., 93.7, 11.6, 0., 0.},
            {0., 0., 96.6, 15.7, 0., 0.},
            {0., 0., 98.8, 18.9, 0., 0.},
            {0., 0., 93.9, 20.3, 0., 0.},
            {0., 0., 88.7, 20.6, 0., 0.}};

    private static final double[][] VehCat42 = { // 2 wheels, type b
            {0., 0., 95.0, 3.2, 0., 0.},
            {0., 0., 97.2, 5.9, 0., 0.},
            {0., 0., 92.7, 11.9, 0., 0.},
            {0., 0., 92.9, 11.6, 0., 0.},
            {0., 0., 94.7, 11.5, 0., 0.},
            {0., 0., 93.2, 12.6, 0., 0.},
            {0., 0., 90.1, 11.1, 0., 0.},
            {0., 0., 86.5, 12., 0., 0.}};

    private static final double[][] VehCat5 = { // if needed one day
            {0., 0., 0., 0., 0., 0.},
            {0., 0., 0., 0., 0., 0.},
            {0., 0., 0., 0., 0., 0.},
            {0., 0., 0., 0., 0., 0.},
            {0., 0., 0., 0., 0., 0.},
            {0., 0., 0., 0., 0., 0.},
            {0., 0., 0., 0., 0., 0.},
            {0., 0., 0., 0., 0., 0.}};


    /** Get a Road Coeff by Freq **/
    public static Double getA_Roadcoeff(int Freq, int VehCat, int RoadSurface) { //CNOSSOS-EU_Road_Catalogue_Final - 01April2014.xlsx - https://circabc.europa.eu/webdav/CircaBC/env/noisedir/Library/Public/cnossos-eu/Final_methods%26software
        int Freq_ind;
        int VehCat_ind;
        double out_value;
        switch (Freq) {
            case 63:
                Freq_ind = 0;
                break;
            case 125:
                Freq_ind = 1;
                break;
            case 250:
                Freq_ind = 2;
                break;
            case 500:
                Freq_ind = 3;
                break;
            case 1000:
                Freq_ind = 4;
                break;
            case 2000:
                Freq_ind = 5;
                break;
            case 4000:
                Freq_ind = 6;
                break;
            case 8000:
                Freq_ind = 7;
                break;
            default:
                Freq_ind = 0;
        }
        switch (VehCat) {
            case 1:
                VehCat_ind = 0;
                break;
            case 2:
                VehCat_ind = 1;
                break;
            case 3:
                VehCat_ind = 2;
                break;
            case 41:
                VehCat_ind = 3;
                break;
            case 42:
                VehCat_ind = 4;
                break;
            case 5:
                VehCat_ind = 5;
                break;
            default:
                VehCat_ind = 0;
        }

        switch (RoadSurface) {
            case 1:
                out_value = RoadCoeff_NL01[Freq_ind][VehCat_ind];
                break;
            case 2:
                out_value = RoadCoeff_NL02[Freq_ind][VehCat_ind];
                break;
            case 3:
                out_value = RoadCoeff_NL03[Freq_ind][VehCat_ind];
                break;
            case 4:
                out_value = RoadCoeff_NL04[Freq_ind][VehCat_ind];
                break;
            case 5:
                out_value = RoadCoeff_NL05[Freq_ind][VehCat_ind];
                break;
            case 6:
                out_value = RoadCoeff_NL06[Freq_ind][VehCat_ind];
                break;
            case 7:
                out_value = RoadCoeff_NL07[Freq_ind][VehCat_ind];
                break;
            case 8:
                out_value = RoadCoeff_NL08[Freq_ind][VehCat_ind];
                break;
            case 9:
                out_value = RoadCoeff_NL09[Freq_ind][VehCat_ind];
                break;
            case 10:
                out_value = RoadCoeff_NL10[Freq_ind][VehCat_ind];
                break;
            case 11:
                out_value = RoadCoeff_NL11[Freq_ind][VehCat_ind];
                break;
            case 12:
                out_value = RoadCoeff_NL12[Freq_ind][VehCat_ind];
                break;
            case 13:
                out_value = RoadCoeff_NL13[Freq_ind][VehCat_ind];
                break;
            case 14:
                out_value = RoadCoeff_NL14[Freq_ind][VehCat_ind];
                break;
            case 15:
                out_value = RoadCoeff_FR1[Freq_ind][VehCat_ind];
                break;
            case 16:
                out_value = RoadCoeff_FR2[Freq_ind][VehCat_ind];
                break;
            case 17:
                out_value = RoadCoeff_FR3[Freq_ind][VehCat_ind];
                break;
            case 18:
                out_value = RoadCoeff_FR4[Freq_ind][VehCat_ind];
                break;
            case 19:
                out_value = RoadCoeff_FR5[Freq_ind][VehCat_ind];
                break;
            case 20:
                out_value = RoadCoeff_FR6[Freq_ind][VehCat_ind];
                break;
            default:
                out_value = 0;
                break;
        }
        return out_value;
    }

    /** Get b Road Coeff by Freq **/
    public static Double getB_Roadcoeff(int VehCat, int RoadSurface) { //CNOSSOS-EU_Road_Catalogue_Final - 01April2014.xlsx - https://circabc.europa.eu/webdav/CircaBC/env/noisedir/Library/Public/cnossos-eu/Final_methods%26software
        double out_value;
        int VehCat_ind;
        switch (VehCat) {
            case 1:
                VehCat_ind = 0;
                break;
            case 2:
                VehCat_ind = 1;
                break;
            case 3:
                VehCat_ind = 2;
                break;
            case 41:
                VehCat_ind = 3;
                break;
            case 42:
                VehCat_ind = 4;
                break;
            case 5:
                VehCat_ind = 5;
                break;
            default:
                VehCat_ind = 0;
        }

        switch (RoadSurface) {
            case 1:
                out_value = RoadCoeff_NL01[8][VehCat_ind];
                break;
            case 2:
                out_value = RoadCoeff_NL02[8][VehCat_ind];
                break;
            case 3:
                out_value = RoadCoeff_NL03[8][VehCat_ind];
                break;
            case 4:
                out_value = RoadCoeff_NL04[8][VehCat_ind];
                break;
            case 5:
                out_value = RoadCoeff_NL05[8][VehCat_ind];
                break;
            case 6:
                out_value = RoadCoeff_NL06[8][VehCat_ind];
                break;
            case 7:
                out_value = RoadCoeff_NL07[8][VehCat_ind];
                break;
            case 8:
                out_value = RoadCoeff_NL08[8][VehCat_ind];
                break;
            case 9:
                out_value = RoadCoeff_NL09[8][VehCat_ind];
                break;
            case 10:
                out_value = RoadCoeff_NL10[8][VehCat_ind];
                break;
            case 11:
                out_value = RoadCoeff_NL11[8][VehCat_ind];
                break;
            case 12:
                out_value = RoadCoeff_NL12[8][VehCat_ind];
                break;
            case 13:
                out_value = RoadCoeff_NL13[8][VehCat_ind];
                break;
            case 14:
                out_value = RoadCoeff_NL14[8][VehCat_ind];
                break;
            case 15:
                out_value = RoadCoeff_FR1[8][VehCat_ind];
                break;
            case 16:
                out_value = RoadCoeff_FR2[8][VehCat_ind];
                break;
            case 17:
                out_value = RoadCoeff_FR3[8][VehCat_ind];
                break;
            case 18:
                out_value = RoadCoeff_FR4[8][VehCat_ind];
                break;
            case 19:
                out_value = RoadCoeff_FR5[8][VehCat_ind];
                break;
            case 20:
                out_value = RoadCoeff_FR6[8][VehCat_ind];
                break;
            default:
                out_value = 0;
                break;
        }
        return out_value;
    }

    /** Get Road Speed min **/
    private static Double getRoadSpeedMin(int RoadSurface) { //CNOSSOS-EU_Road_Catalogue_Final - 01April2014.xlsx - https://circabc.europa.eu/webdav/CircaBC/env/noisedir/Library/Public/cnossos-eu/Final_methods%26software
        double out_value;
        switch (RoadSurface) {
            case 1:
                out_value = RoadCoeff_NL01[9][0];
                break;
            case 2:
                out_value = RoadCoeff_NL02[9][0];
                break;
            case 3:
                out_value = RoadCoeff_NL03[9][0];
                break;
            case 4:
                out_value = RoadCoeff_NL04[9][0];
                break;
            case 5:
                out_value = RoadCoeff_NL05[9][0];
                break;
            case 6:
                out_value = RoadCoeff_NL06[9][0];
                break;
            case 7:
                out_value = RoadCoeff_NL07[9][0];
                break;
            case 8:
                out_value = RoadCoeff_NL08[9][0];
                break;
            case 9:
                out_value = RoadCoeff_NL09[9][0];
                break;
            case 10:
                out_value = RoadCoeff_NL10[9][0];
                break;
            case 11:
                out_value = RoadCoeff_NL11[9][0];
                break;
            case 12:
                out_value = RoadCoeff_NL12[9][0];
                break;
            case 13:
                out_value = RoadCoeff_NL13[9][0];
                break;
            case 14:
                out_value = RoadCoeff_NL14[9][0];
                break;
            case 15:
                out_value = RoadCoeff_FR1[9][0];
                break;
            case 16:
                out_value = RoadCoeff_FR2[9][0];
                break;
            case 17:
                out_value = RoadCoeff_FR3[9][0];
                break;
            case 18:
                out_value = RoadCoeff_FR4[9][0];
                break;
            case 19:
                out_value = RoadCoeff_FR5[9][0];
                break;
            case 20:
                out_value = RoadCoeff_FR6[9][0];
                break;
            default:
                out_value = 20;
                break;
        }
        return out_value;
    }

    /** Get Road Speed max **/
    private static Double getRoadSpeedMax(int RoadSurface) { //CNOSSOS-EU_Road_Catalogue_Final - 01April2014.xlsx - https://circabc.europa.eu/webdav/CircaBC/env/noisedir/Library/Public/cnossos-eu/Final_methods%26software
        double out_value;
        switch (RoadSurface) {
            case 1:
                out_value = RoadCoeff_NL01[9][1];
                break;
            case 2:
                out_value = RoadCoeff_NL02[9][1];
                break;
            case 3:
                out_value = RoadCoeff_NL03[9][1];
                break;
            case 4:
                out_value = RoadCoeff_NL04[9][1];
                break;
            case 5:
                out_value = RoadCoeff_NL05[9][1];
                break;
            case 6:
                out_value = RoadCoeff_NL06[9][1];
                break;
            case 7:
                out_value = RoadCoeff_NL07[9][1];
                break;
            case 8:
                out_value = RoadCoeff_NL08[9][1];
                break;
            case 9:
                out_value = RoadCoeff_NL09[9][1];
                break;
            case 10:
                out_value = RoadCoeff_NL10[9][1];
                break;
            case 11:
                out_value = RoadCoeff_NL11[9][1];
                break;
            case 12:
                out_value = RoadCoeff_NL12[9][1];
                break;
            case 13:
                out_value = RoadCoeff_NL13[9][1];
                break;
            case 14:
                out_value = RoadCoeff_NL14[9][1];
                break;
            case 15:
                out_value = RoadCoeff_FR1[9][1];
                break;
            case 16:
                out_value = RoadCoeff_FR2[9][1];
                break;
            case 17:
                out_value = RoadCoeff_FR3[9][1];
                break;
            case 18:
                out_value = RoadCoeff_FR4[9][1];
                break;
            case 19:
                out_value = RoadCoeff_FR5[9][1];
                break;
            case 20:
                out_value = RoadCoeff_FR6[9][1];
                break;
            default:
                out_value = 130;
                break;
        }
        return out_value;
    }

    /** Get coeff vehicule by Freq **/
    public static Double getCoeff(int Coeff, int Freq, int VehCat) {
        // Coeff number, if 0=Ar, 1=Br, 2=Ap, 3=Bp, 4=a, 5=b, 6=k_road Table III.A.1
        // VehCat, 1=passenger cars, etc. Table III.A.1
        // VehCat, 3=heavy trucks, etc. Table III.A.1
        // Freq, 0 = 63 Hz, 1 = 125 Hz, etc.
        int Freq_ind;
        switch (Freq) {
            case 63:
                Freq_ind = 0;
                break;
            case 125:
                Freq_ind = 1;
                break;
            case 250:
                Freq_ind = 2;
                break;
            case 500:
                Freq_ind = 3;
                break;
            case 1000:
                Freq_ind = 4;
                break;
            case 2000:
                Freq_ind = 5;
                break;
            case 4000:
                Freq_ind = 6;
                break;
            case 8000:
                Freq_ind = 7;
                break;
            default:
                Freq_ind = 0;
        }

        if (VehCat == 1) {
            return VehCat1[Freq_ind][Coeff];
        } else if (VehCat == 3) {
            return VehCat3[Freq_ind][Coeff];
        } else if (VehCat == 41) {
            return VehCat41[Freq_ind][Coeff];
        } else if (VehCat == 42) {
            return VehCat42[Freq_ind][Coeff];
        } else if (VehCat == 2) {
            return VehCat2[Freq_ind][Coeff];
        } else {
            return VehCat1[Freq_ind][Coeff];
        }
    }

    /** get noise level from speed **/
    private static Double getNoiseLvl(Double base, Double adj, Double speed,
                                      Double speedBase) {
        return base + adj * Math.log10(speed / speedBase);
    }


    /** get sum dBa **/
    private static Double sumDba(Double dBA1, Double dBA2) {
        return ComputeRays.wToDba(ComputeRays.dbaToW(dBA1) + ComputeRays.dbaToW(dBA2));
    }

    private static Double sumDba_5(Double dBA1, Double dBA2, Double dBA3, Double dBA4, Double dBA5) {
        return ComputeRays.wToDba(ComputeRays.dbaToW(dBA1) + ComputeRays.dbaToW(dBA2) + ComputeRays.dbaToW(dBA3) + ComputeRays.dbaToW(dBA4) + ComputeRays.dbaToW(dBA5));
    }

    /**
     * Road noise evaluation.
     * @param parameters Noise emission parameters
     * @return Noise level in dB
     */
    public static double evaluate(RSParametersDynamic parameters) {
        double Compound;
        int FreqParam = parameters.getFreqParam();
        double Temperature = parameters.getTemperature();
        int RoadSurface = parameters.getRoadSurface();
        boolean Stud = parameters.getStud();
        double Junc_dist = parameters.getJunc_dist();
        int Junc_type = parameters.getJunc_type();
        int acc_type = parameters.getAcc_type();
        int veh_type = parameters.getVeh_type();
        int VehId = parameters.getVehId();
        double acceleration = parameters.getAcceleration();
        double speed = parameters.getSpeed();


        // ///////////////////////
        // Noise road/tire CNOSSOS
        double RoadLvl; // Lw/m (1 veh/h)

        // Noise level
        RoadLvl = getNoiseLvl(getCoeff(0, FreqParam, veh_type), getCoeff(1, FreqParam, veh_type), parameters.getSpeed(), 70.);

        // Correction by temperature p. 36
        switch (veh_type) {
            case 1:
                RoadLvl = RoadLvl + 0.08 * (20 - Temperature); // K = 0.08  p. 36
                break;
            case 2:
                RoadLvl = RoadLvl + 0.04 * (20 - Temperature); // K = 0.04 p. 36
                break;
            case 3:
                RoadLvl = RoadLvl + 0.04 * (20 - Temperature); // K = 0.04 p. 36
                break;
            default:
                break;
        }

        // Rolling noise acceleration correction
        int indJunc = (Junc_type == 2) ? 2 : 0; // because my table is not very smart
        switch (acc_type) {
            case 1:
                if (veh_type <= 3) {
                    RoadLvl = RoadLvl + Coeff_Acc[veh_type - 1][0 + indJunc] * Math.max(1 - Math.abs(Junc_dist) / 100, 0);
                }
                break;
            default:
                break;
        }

        //Studied tyres
        if (veh_type == 1) { // because studded tyres are only on Cat 1 vehicle
            if (Stud) {
                double deltastud = 0;
                double speedstud  = (speed >= 90) ? 90 : speed;
                speedstud = (speedstud <= 50) ? 50 : speedstud;
                deltastud = getNoiseLvl(getCoeff(4, FreqParam, 1), getCoeff(5, FreqParam, 1), speedstud, 70.);
                RoadLvl = RoadLvl + Math.pow(10, deltastud / 10);
            }
        }

        //Road surface correction on rolling noise
        RoadLvl = RoadLvl + getNoiseLvl(getA_Roadcoeff(FreqParam, veh_type, RoadSurface), getB_Roadcoeff(veh_type, RoadSurface), parameters.getSpeed(), 70.);

        // ///////////////////////
        // Noise motor
        // Calculate the emission powers of motors lights vehicles and heavies goods vehicles.
        double MotorLvl;
        RoadLvl = (speed <= 20) ? 0 : RoadLvl;
        speed = (speed <= 20) ? 20 : speed; // Because when vehicles are stopped they still emit motor sounds.
        // default or steady speed.
        MotorLvl = getCoeff(2, FreqParam, veh_type) + getCoeff(3, FreqParam, veh_type) * (speed - 70) / 70;

        // Propulsion noise acceleration correction
        double amax=0;
        switch (acc_type) {
            case 1:
                if (veh_type <= 3) {
                    MotorLvl = MotorLvl + Coeff_Acc[veh_type - 1][1 + indJunc] * Math.max(1 - Math.abs(Junc_dist) / 100, 0);
                }
                break;
            case 2:
                switch (veh_type) {
                    case 1:
                        amax = 2;
                        if (acceleration >= -1 & FreqParam < 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,amax) * 5.0;}
                        if (acceleration >= -1 & FreqParam >= 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,amax) * 2.0;}
                        if (acceleration < -1 & FreqParam < 250 ){ MotorLvl = MotorLvl +  -1 * 5.0;}
                        if (acceleration < -1 & FreqParam >= 250 ){ MotorLvl = MotorLvl +  -1 * 2.0;}
                        break;
                    case 2:
                        amax = 1;
                        if (acceleration >= -1 & FreqParam < 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,amax) * 7.0;}
                        if (acceleration >= -1 & FreqParam >= 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,amax) * 3.0;}
                        if (acceleration < -1 & FreqParam < 250 ){ MotorLvl = MotorLvl +  -1 * 7.0;}
                        if (acceleration < -1 & FreqParam >= 250 ){ MotorLvl = MotorLvl +  -1 * 3.0;}
                        break;
                    case 3:
                        amax = 1;
                        if (acceleration >= -1 & FreqParam < 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,amax) * 7.0;}
                        if (acceleration >= -1 & FreqParam >= 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,amax) * 3.0;}
                        if (acceleration < -1 & FreqParam < 250 ){ MotorLvl = MotorLvl +  -1 * 7.0;}
                        if (acceleration < -1 & FreqParam >= 250 ){ MotorLvl = MotorLvl +  -1 * 3.0;}
                        break;
                    case 41:
                        amax = 4;
                        if (acceleration >= -1 & FreqParam < 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,amax) * 5.0;}
                        if (acceleration >= -1 & FreqParam >= 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,amax) * 2.0;}
                        if (acceleration < -1 & FreqParam < 250 ){ MotorLvl = MotorLvl +  -1 * 5.0;}
                        if (acceleration < -1 & FreqParam >= 250 ){ MotorLvl = MotorLvl +  -1 * 2.0;}
                        break;
                    case 42:
                        amax = 4;
                        if (acceleration >= -1 & FreqParam < 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,amax) * 5.0;}
                        if (acceleration >= -1 & FreqParam >= 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,amax) * 2.0;}
                        if (acceleration < -1 & FreqParam < 250 ){ MotorLvl = MotorLvl +  -1 * 5.0;}
                        if (acceleration < -1 & FreqParam >= 250 ){ MotorLvl = MotorLvl +  -1 * 2.0;}
                        break;
                    default:
                        break;
                }
                break;
            case 3:
                switch (veh_type) {
                    case 1:
                        amax = 10;
                        if (acceleration >= -1 & FreqParam < 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,amax) * 5.0;}
                        if (acceleration >= -1 & FreqParam >= 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,amax) * 2.0;}
                        if (acceleration < -1 & FreqParam < 250 ){ MotorLvl = MotorLvl +  -1 * 5.0;}
                        if (acceleration < -1 & FreqParam >= 250 ){ MotorLvl = MotorLvl +  -1 * 2.0;}
                        break;
                    case 2:
                        amax = 10;
                        if (acceleration >= -1 & FreqParam < 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,amax) * 7.0;}
                        if (acceleration >= -1 & FreqParam >= 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,amax) * 3.0;}
                        if (acceleration < -1 & FreqParam < 250 ){ MotorLvl = MotorLvl +  -1 * 7.0;}
                        if (acceleration < -1 & FreqParam >= 250 ){ MotorLvl = MotorLvl +  -1 * 3.0;}
                        break;
                    case 3:
                        amax = 10;
                        if (acceleration >= -1 & FreqParam < 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,amax) * 7.0;}
                        if (acceleration >= -1 & FreqParam >= 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,amax) * 3.0;}
                        if (acceleration < -1 & FreqParam < 250 ){ MotorLvl = MotorLvl +  -1 * 7.0;}
                        if (acceleration < -1 & FreqParam >= 250 ){ MotorLvl = MotorLvl +  -1 * 3.0;}
                        break;
                    case 41:
                        amax = 10;
                        if (acceleration >= -1 & FreqParam < 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,amax) * 5.0;}
                        if (acceleration >= -1 & FreqParam >= 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,amax) * 2.0;}
                        if (acceleration < -1 & FreqParam < 250 ){ MotorLvl = MotorLvl +  -1 * 5.0;}
                        if (acceleration < -1 & FreqParam >= 250 ){ MotorLvl = MotorLvl +  -1 * 2.0;}
                        break;
                    case 42:
                        amax = 10;
                        if (acceleration >= -1 & FreqParam < 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,amax) * 5.0;}
                        if (acceleration >= -1 & FreqParam >= 250 ){ MotorLvl = MotorLvl +  Math.min(acceleration,amax) * 2.0;}
                        if (acceleration < -1 & FreqParam < 250 ){ MotorLvl = MotorLvl +  -1 * 5.0;}
                        if (acceleration < -1 & FreqParam >= 250 ){ MotorLvl = MotorLvl +  -1 * 2.0;}
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }


        // Correction gradient
        switch (veh_type) {
            case 1:
                if (parameters.getSlopePercentage() < -6) {
                    // downwards 2% <= p <= 6%
                    // Steady and deceleration, the same formulae
                    MotorLvl = MotorLvl + (Math.min(12, -parameters.getSlopePercentage()) - 6) / 1;
                } else if (parameters.getSlopePercentage() <= 2) {
                    // 0% <= p <= 2%
                    MotorLvl = MotorLvl + 0.;
                } else {
                    // upwards 2% <= p <= 6%
                    MotorLvl = MotorLvl + ((speed / 100) * ((Math.min(12, parameters.getSlopePercentage()) - 2) / 1.5));
                }
                break;
            case 2:
                if (parameters.getSlopePercentage() < -4) {
                    // Steady and deceleration, the same formulae
                    MotorLvl = MotorLvl + ((speed - 20) / 100) * (Math.min(12, -1 * parameters.getSlopePercentage()) - 4) / 0.7;
                } else if (parameters.getSlopePercentage() <= 0) {
                    MotorLvl = MotorLvl + 0.;

                } else {
                    MotorLvl = MotorLvl + (speed / 100) * (Math.min(12, parameters.getSlopePercentage())) / 1;
                }
                break;
            case 3:
                if (parameters.getSlopePercentage() < -4) {
                    // Steady and deceleration, the same formulae
                    MotorLvl = MotorLvl + ((speed - 10) / 100) * (Math.min(12, -1 * parameters.getSlopePercentage()) - 4) / 0.5;
                } else if (parameters.getSlopePercentage() <= 0) {
                    MotorLvl = MotorLvl + 0.;
                } else {
                    MotorLvl = MotorLvl + (speed / 100) * (Math.min(12, parameters.getSlopePercentage())) / 0.8;
                }
                break;
            default:
                break;
        }


        // Correction road on propulsion noise
        MotorLvl = MotorLvl + Math.min(getA_Roadcoeff(FreqParam, veh_type, RoadSurface), 0.);
        Random r = new Random(VehId);
        double deltaLwdistrib = 0.115*Math.pow(parameters.getLwStd(),2.0); // Ask Arnaud and Pierre !
        Compound = sumDba(RoadLvl, MotorLvl) - deltaLwdistrib +  r.nextGaussian()*parameters.getLwStd();

        return Compound;
    }
}
