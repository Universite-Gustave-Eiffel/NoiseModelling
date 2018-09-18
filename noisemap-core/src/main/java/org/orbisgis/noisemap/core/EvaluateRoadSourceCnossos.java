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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;

import java.io.IOException;
import java.io.InputStream;

/**
 * Return the dB value corresponding to the parameters
 * Reference document is Common Noise Assessment Methods in Europe(CNOSSOS-EU), 2012
 * Stylianos Kephalopoulos, Marco Paviotti, Fabienne Anfosso-Ledee
 * https://ec.europa.eu/jrc/sites/jrcsh/files/cnossos-eu%2520jrc%2520reference%2520report_final_on%2520line%2520version_10%2520august%25202012.pdf
 * @author Nicolas Fortin
 * @author Pierre Aumond - 27/04/2017.
 */

public class EvaluateRoadSourceCnossos {

    private final static JsonNode cnossosData = parse(EvaluateRoadSourceCnossos.class.getResourceAsStream("coefficients_cnossos.json"));

    private static JsonNode parse(InputStream inputStream) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(inputStream);
        } catch (IOException ex) {
            return NullNode.getInstance();
        }
    }
    /** acceleration coeff **/
    private static final double[][] Coeff_Acc={
            {-4.5,5.5,-4.4,3.1}, //Table III.A.6 p.44 - Confirm data with CNOSSOS-EU phase B p.44
            {-4.0,9,-2.3,6.7},
            {-4.0,9,-2.3,6.7},
            {0,0,0,0},
            {0,0,0,0}};

    /** Road Categories -  All surface are from CNOSSOS-EU_Road_Catalogue_Final - 01April2014.xlsx **/

    private static final double[][] RoadCoeff_NL01 = { // 1-layer ZOAB
            {0.5, 0.9, 0.9, 0.0, 0.0, 0.0},
            {3.3, 1.4, 1.4, 0., 0., 0.   },
            {2.4, 1.8, 1.8, 0., 0., 0.   },
            {3.2, -0.4, -0.4, 0., 0., 0. },
            {-1.3, -5.2, -5.2, 0., 0., 0.},
            {-3.5, -4.6, -4.6, 0., 0., 0.},
            {-2.6, -3.0, -3.0, 0., 0., 0.},
            {0.5, -1.4, -1.4, 0., 0., 0. },
            {-6.5, 0.2, 0.2, 0., 0., 0.  }, {50, 130}};

    private static final double[][] RoadCoeff_NL02={ // 2-layer ZOAB
            {0.4,0.4,0.4,0.,0.,0.},
            {2.4,0.2,0.2,0.,0.,0.},
            {0.2,-0.7,-0.7,0.,0.,0.},
            {-3.1,-5.4,-5.4,0.,0.,0.},
            {-4.2,-6.3,-6.3,0.,0.,0.},
            {-6.3,-6.3,-6.3,0.,0.,0.},
            {-4.8,-4.7,-4.7,0.,0.,0.},
            {-2.0,-3.7,-3.7,0.,0.,0.},
            {-3.,4.7,4.7,0.,0.,0.},{50,130}};

    private static final double[][] RoadCoeff_NL03={ // 2-layer ZOAB (fine)
            {-1.,1.,1.,0.,0.,0.},
            {1.7,0.1,0.1,0.,0.,0.},
            {-1.5,-1.8,-1.8,0.,0.,0.},
            {-5.3,-5.9,-5.9,0.,0.,0.},
            {-6.3,-6.1,-6.1,0.,0.,0.},
            {-8.5,-6.7,-6.7,0.,0.,0.},
            {-5.3,-4.8,-4.8,0.,0.,0.},
            {-2.4,-3.8,-3.8,0.,0.,0.},
            {-0.1,-0.8,-0.8,0.,0.,0.},{80,130}};

    private static final double[][] RoadCoeff_NL04={ // SMA-NL5
            {1.1,0.,0.,0.,0.,0.},
            {-1.0,0.,0.,0.,0.,0.},
            {0.2,0.,0.,0.,0.,0.},
            {1.3,0.,0.,0.,0.,0.},
            {-1.9,0.,0.,0.,0.,0.},
            {-2.8,0.,0.,0.,0.,0.},
            {-2.1,0.,0.,0.,0.,0.},
            {-1.4,0.,0.,0.,0.,0.},
            {-1.0,0.,0.,0.,0.,0.},{40,80}};

    private static final double[][] RoadCoeff_NL05={ // SMA-NL8
            {0.3,0.,0.,0.,0.,0.},
            {0.,0.,0.,0.,0.,0.},
            {0.,0.,0.,0.,0.,0.},
            {-0.1,0.,0.,0.,0.,0.},
            {-0.7,0.,0.,0.,0.,0.},
            {-1.3,0.,0.,0.,0.,0.},
            {-0.8,0.,0.,0.,0.,0.},
            {-0.8,0.,0.,0.,0.,0.},
            {-1.0,0.,0.,0.,0.,0.},{40,80}};

    private static final double[][] RoadCoeff_NL06={ // Brushed down concrete
            {1.1,0.,0.,0.,0.,0.},
            {-0.4,1.1,1.1,0.,0.,0.},
            {1.3,0.4,0.4,0.,0.,0.},
            {2.2,-0.3,-0.3,0.,0.,0.},
            {2.5,-0.2,-0.2,0.,0.,0.},
            {0.8,-0.7,-0.7,0.,0.,0.},
            {-0.2,-1.1,-1.1,0.,0.,0.},
            {-0.1,-1.0,-1.0,0.,0.,0.},
            {1.4,4.4,4.4,0.,0.,0.},{70,120}};

    private static final double[][] RoadCoeff_NL07={ // Optimized brushed down concrete
            {-0.2,-0.3,-0.3,0.,0.,0.},
            {-0.7,1.0,1.0,0.,0.,0.},
            {0.6,-1.7,-1.7,0.,0.,0.},
            {1.,-1.2,-1.2,0.,0.,0.},
            {1.1,-1.6,-1.6,0.,0.,0.},
            {-1.5,-2.4,-2.4,0.,0.,0.},
            {-2.,-1.7,-1.7,0.,0.,0.},
            {-1.8,-1.7,-1.7,0.,0.,0.},
            {1.,-6.6,-6.6,0.,0.,0.},{70,80}};

    private static final double[][] RoadCoeff_NL08={ // Fine broomed concrete
            {1.1,0.,0.,0.,0.,0.},
            {-0.5,3.3,3.3,0.,0.,0.},
            {2.7,2.4,2.4,0.,0.,0.},
            {2.1,1.9,1.9,0.,0.,0.},
            {1.6,2.0,2.0,0.,0.,0.},
            {2.7,1.2,1.2,0.,0.,0.},
            {1.3,0.1,0.1,0.,0.,0.},
            {-0.4,0.,0.,0.,0.,0.},
            {7.7,3.7,3.7,0.,0.,0.},{70,120}};

    private static final double[][] RoadCoeff_NL09={ // Worked surface
            {1.1,0.,0.,0.,0.,0.},
            {1.,2.,2.,0.,0.,0.},
            {2.6,1.8,1.8,0.,0.,0.},
            {4.,1.,1.,0.,0.,0.},
            {4.,-0.7,-0.7,0.,0.,0.},
            {0.1,-2.1,-2.1,0.,0.,0.},
            {-1.,-1.9,-1.9,0.,0.,0.},
            {-0.8,-1.7,-1.7,0.,0.,0.},
            {-0.2,1.7,1.7,0.,0.,0.},{50,130}};

    private static final double[][] RoadCoeff_NL10={ // Hard elements in herring-bone
            {8.3,8.3,8.3,0.,0.,0.},
            {8.7,8.7,8.7,0.,0.,0.},
            {7.8,7.8,7.8,0.,0.,0.},
            {5.,5.,5.,0.,0.,0.},
            {3.,3.,3.,0.,0.,0.},
            {-0.7,-0.7,-0.7,0.,0.,0.},
            {0.8,0.8,0.8,0.,0.,0.},
            {1.8,1.8,1.8,0.,0.,0.},
            {2.5,2.5,2.5,0.,0.,0.},{30,60}};

    private static final double[][] RoadCoeff_NL11={ // Hard elements not in herring-bone
            {12.3,12.3,12.3,0.,0.,0.},
            {11.9,11.9,11.9,0.,0.,0.},
            {9.7,9.7,9.7,0.,0.,0.},
            {7.1,7.1,7.1,0.,0.,0.},
            {7.1,7.1,7.1,0.,0.,0.},
            {2.8,2.8,2.8,0.,0.,0.},
            {4.7,4.7,4.7,0.,0.,0.},
            {4.5,4.5,4.5,0.,0.,0.},
            {2.9,2.9,2.9,0.,0.,0.},{30,60}};

    private static final double[][] RoadCoeff_NL12={ // Quiet hard elements
            {7.8,0.2,0.2,0.,0.,0.},
            {6.3,0.7,0.7,0.,0.,0.},
            {5.2,0.7,0.7,0.,0.,0.},
            {2.8,1.1,1.1,0.,0.,0.},
            {-1.9,1.8,1.8,0.,0.,0.},
            {-6.0,1.2,1.2,0.,0.,0.},
            {-3.,1.1,1.1,0.,0.,0.},
            {-0.1,0.2,0.2,0.,0.,0.},
            {-1.7,0.,0.,0.,0.,0.},{30,60}};

    private static final double[][] RoadCoeff_NL13={ // Thin layer A
            {1.1,1.6,1.6,0.,0.,0.},
            {0.1,1.3,1.3,0.,0.,0.},
            {-0.7,0.9,0.9,0.,0.,0.},
            {-1.3,-0.4,-0.4,0.,0.,0.},
            {-3.1,-1.8,-1.8,0.,0.,0.},
            {-4.9,-2.1,-2.1,0.,0.,0.},
            {-3.5,-0.7,-0.7,0.,0.,0.},
            {-1.5,-0.2,-0.2,0.,0.,0.},
            {-2.5,0.5,0.5,0.,0.,0.},{40,130}};

    private static final double[][] RoadCoeff_NL14={ // Thin layer B
            {0.4,1.6,1.6,0.,0.,0.},
            {-1.3,1.3,1.3,0.,0.,0.},
            {-1.3,0.9,0.9,0.,0.,0.},
            {-0.4,-0.4,-0.4,0.,0.,0.},
            {-5.,-1.8,-1.8,0.,0.,0.},
            {-7.1,-2.1,-2.1,0.,0.,0.},
            {-4.9,-0.7,-0.7,0.,0.,0.},
            {-3.3,-0.2,-0.2,0.,0.,0.},
            {-1.5,0.5,0.5,0.,0.,0.},{40,130}};
    /** French road pavement "Matching directive 2015/996/EC (CNOSSOS-EU)
     and the French emission model for road pavements", Dutilleux, Soldano, Euronoise 2018
     R1 (high acoustic performance): BBTM 0/6 types 1 et 2 - BBUM 0/6 - BBDr 0/10 - BBTM 0/10 type 2
     R2 (medium acoustic performance): BBTM 0/10 type 1 - BBSG 0/10 - ECF - BBUM 0/10
     R3 (low acoustic performance): BC - BBTM 0/14 - BBSG 0/14 - ES 6/10 - ES 10/14
     Drainant = Dr after the name
     **/

    private static final double[][] RoadCoeff_FR1={ // R1 drainant (ajustement composante totale, Rapport Cerema 2016)
            {13.9,23.3,19,0.,0.,0.},
            {14,19.4,15.5,0.,0.,0.},
            {14.1,15.5,12,0.,0.,0.},
            {8.7,10.7,5.8,0.,0.,0.},
            {-2.5,5.8,1.5,0.,0.,0.},
            {-3.9,7.6,3.3,0.,0.,0.},
            {-0.5,8.2,4.,0.,0.,0.},
            {2.9,8.9,4.8,0.,0.,0.},
            {-2.9,-0.1,6.6,0.,0.,0.},{30,130}};

    private static final double[][] RoadCoeff_FR2={ // R1 non drainant(ajustement composante totale, Rapport Cerema 2016)
            {9,18.6,14.2,0.,0.,0.},
            {9.6,15.1,11.1,0.,0.,0.},
            {10.2,11.7,8.1,0.,0.,0.},
            {5.8,7.8,2.8,0.,0.,0.},
            {-0.3,8.2,3.8,0.,0.,0.},
            {-2.7,8.8,4.4,0.,0.,0.},
            {-0.7,8.1,3.8,0.,0.,0.},
            {1.3,7.5,3.3,0.,0.,0.},
            {-3.4,-1.3,6.1,0.,0.,0.},{30,130}};

    private static final double[][] RoadCoeff_FR3={ // R2 drainant(ajustement composante totale, Rapport Cerema 2016)
            {15.7,25.4,21.4,0.,0.,0.},
            {15.8,21.5,17.9,0.,0.,0.},
            {15.9,17.6,14.4,15.4,0.,0.},
            {10.5,12.8,8.2,0.,0.,0.},
            {-0.7,7.9,3.9,0.,0.,0.},
            {-2.1,9.7,5.7,0.,0.,0.},
            {1.3,10.3,6.4,0.,0.,0.},
            {4.7,11.,7.2,0.,0.,0.},
            {-2.2,0.3,4.9,0.,0.,0.},{30,130}};

    private static final double[][] RoadCoeff_FR4={ // R2 non drainant(ajustement composante totale, Rapport Cerema 2016)
            {10.8,20.6,16.7,0.,0.,0.},
            {11.4,17.1,13.6,0.,0.,0.},
            {12,13.7,10.6,0.,0.,0.},
            {7.5,9.8,5.3,0.,0.,0.},
            {1.5,10.2,6.3,0.,0.,0.},
            {-0.9,10.8,6.9,0.,0.,0.},
            {1.1,10.1,6.3,0.,0.,0.},
            {3.1,9.5,5.8,0.,0.,0.},
            {-2.7,-1.2,3.5,0.,0.,0.},{30,130}};

    private static final double[][] RoadCoeff_FR5={ // R3 drainant(ajustement composante totale, Rapport Cerema 2016)
            {17.5,26.2,22.4,0.,0.,0.},
            {17.5,22.3,18.9,0.,0.,0.},
            {17.6,18.4,15.4,0.,0.,0.},
            {12.3,13.6,9.2,0.,0.,0.},
            {1,8.7,-1.8,4.9,0.,0.},
            {-0.3,10.5,6.7,0.,0.,0.},
            {3.1,11.1,7.4,0.,0.,0.},
            {6.4,11.8,8.2,0.,0.,0.},
            {0.1,0.4,4.3,0.,0.,0.},{30,130}};

    private static final double[][] RoadCoeff_FR6={ // R3 non drainant(ajustement composante totale, Rapport Cerema 2016)
            {12.7,21.5,17.7,0.,0.,0.},
            {13.2,18,14.6,0.,0.,0.},
            {13.8,14.6,11.6,0.,0.,0.},
            {9.4,10.7,6.3,0.,0.,0.},
            {3.4,11.1,7.3,0.,0.,0.},
            {0.9,11.7,7.9,0.,0.,0.},
            {3,11,7.3,0.,0.,0.},
            {5,10.4,6.8,0.,0.,0.},
            {-0.8,-0.8,2.8,0.,0.,0.},{30,130}};

    /** CNOSSOS Vehicule Tables -  All coefficients are from CNOSSOS-EU_Road_Catalogue_Final - 01April2014.xlsx - Emission + Studded **/
    private static final double[][] VehCat1={   // light vehicle
            {79.7,30.0,94.5,-1.3,0.,0.},
            {85.7,41.5,89.2,7.2,0.,0.},
            {84.5,38.9,88.0,7.7,0.,0.},
            {90.2,25.7,85.9,8.0,2.6,-3.1},
            {97.3,32.5,84.2,8.0,2.9,-6.4},
            {93.9,37.2,86.9,8.0,1.5,-14},
            {84.1,39.0,83.3,8.0,2.3,-22.4},
            {74.3,40.0,76.1,8.0,9.2,-11.4}};

    private static final double[][] VehCat2={ // medium vehicle
            {84.,30.0,101.,-1.9,0.,0.},
            {88.7,35.8,96.5,4.7,0.,0.},
            {91.5,32.6,98.8,6.4,0.,0.},
            {96.7,23.8,96.8,6.5,0.,0.},
            {97.4,30.1,98.6,6.5,0.,0.},
            {90.9,36.2,95.2,6.5,0.,0.},
            {83.8,38.3,88.8,6.5,0.,0.},
            {80.5,40.1,82.7,6.5,0.,0.}};

    private static final double[][] VehCat3={  // heavy vehicle
            {87.0,30.0,104.4,0,0.,0.},
            {91.7,33.5,100.6,3.0,0.,0.},
            {94.1,31.3,101.7,4.6,0.,0.},
            {100.7,25.4,101.0,5.0,0.,0.},
            {100.8,31.8,100.1,5.0,0.,0.},
            {94.3,37.1,95.9,5.0,0.,0.},
            {87.1,38.6,91.3,5.0,0.,0.},
            {82.5,40.6,85.3,5.0,0.,0.}};

    private static final double[][] VehCat41={ // 2 wheels, type a
            {0.,0.,88.,4.2,0.,0.},
            {0.,0.,87.5,7.4,0.,0.},
            {0.,0.,89.5,9.8,0.,0.},
            {0.,0.,93.7,11.6,0.,0.},
            {0.,0.,96.6,15.7,0.,0.},
            {0.,0.,98.8,18.9,0.,0.},
            {0.,0.,93.9,20.3,0.,0.},
            {0.,0.,88.7,20.6,0.,0.}};

    private static final double[][] VehCat42={ // 2 wheels, type b
            {0.,0.,95.0,3.2,0.,0.},
            {0.,0.,97.2,5.9,0.,0.},
            {0.,0.,92.7,11.9,0.,0.},
            {0.,0.,92.9,11.6,0.,0.},
            {0.,0.,94.7,11.5,0.,0.},
            {0.,0.,93.2,12.6,0.,0.},
            {0.,0.,90.1,11.1,0.,0.},
            {0.,0.,86.5,12.,0.,0.}};

    private static final double[][] VehCat5={ // if needed one day
            {0.,0.,0.,0.,0.,0.},
            {0.,0.,0.,0.,0.,0.},
            {0.,0.,0.,0.,0.,0.},
            {0.,0.,0.,0.,0.,0.},
            {0.,0.,0.,0.,0.,0.},
            {0.,0.,0.,0.,0.,0.},
            {0.,0.,0.,0.,0.,0.},
            {0.,0.,0.,0.,0.,0.}};


    /** Get a Road Coeff by Freq **/
    public static Double getA_Roadcoeff(int Freq, String vehCat, String RoadSurface) { //CNOSSOS-EU_Road_Catalogue_Final - 01April2014.xlsx - https://circabc.europa.eu/webdav/CircaBC/env/noisedir/Library/Public/cnossos-eu/Final_methods%26software
        int Freq_ind;
        int VehCat_ind;
        double out_value;
        switch (Freq) {
            case 63:
                Freq_ind=0;
                break;
            case 125:
                Freq_ind=1;
                break;
            case 250:
                Freq_ind=2;
                break;
            case 500:
                Freq_ind=3;
                break;
            case 1000:
                Freq_ind=4;
                break;
            case 2000:
                Freq_ind=5;
                break;
            case 4000:
                Freq_ind=6;
                break;
            case 8000:
                Freq_ind=7;
                break;
            default:
                Freq_ind=0;
        }
        return cnossosData.get("roads").get(RoadSurface).get("ref").get(vehCat).get("spectrum").get(Freq_ind).doubleValue();
    }

    /** Get b Road Coeff by Freq **/
    public static Double getB_Roadcoeff(String vehCat, String roadSurface) { //CNOSSOS-EU_Road_Catalogue_Final - 01April2014.xlsx - https://circabc.europa.eu/webdav/CircaBC/env/noisedir/Library/Public/cnossos-eu/Final_methods%26software
        return cnossosData.get("roads").get(roadSurface).get("ref").get(vehCat).get("ßm").doubleValue();
    }

    public static double getCr(String vehCat, int k) {
        return cnossosData.get("vehicles").get(vehCat).get(k == 1 ? "crossing" : "roundabout").get("cr").doubleValue();
    }

    public static double getCp(String vehCat, int k) {
        return cnossosData.get("vehicles").get(vehCat).get(k == 1 ? "crossing" : "roundabout").get("cp").doubleValue();
    }

    /**
     * Vehicle emission values coefficients
     * @param coeff ar,br,ap,bp,a,b
     * @param freq 0 = 63 Hz, 1 = 125 Hz, etc.
     * @param vehicleCategory 1,2,3,4a,4b..
     * @return
     */
    public static Double getCoeff(String coeff, int freq, String vehicleCategory) {
        int Freq_ind;
        switch (freq) {
            case 63:
                Freq_ind=0;
                break;
            case 125:
                Freq_ind=1;
                break;
            case 250:
                Freq_ind=2;
                break;
            case 500:
                Freq_ind=3;
                break;
            case 1000:
                Freq_ind=4;
                break;
            case 2000:
                Freq_ind=5;
                break;
            case 4000:
                Freq_ind=6;
                break;
            case 8000:
                Freq_ind=7;
                break;
            default:
                Freq_ind=0;
        }
        return cnossosData.get("vehicles").get(vehicleCategory).get(coeff).get(Freq_ind).doubleValue();
    }

    /** get noise level from speed **/
    private static Double getNoiseLvl(double base, double adj, double speed,
                                      double speedBase) {
        return base + adj * Math.log10(speed / speedBase);
    }

    /** compute Noise Level from flow_rate and speed **/
    private static Double Vperhour2NoiseLevel(double NoiseLevel, double vperhour, double speed) {
        if (speed > 0) {
            return NoiseLevel + 10 * Math.log10(vperhour / (1000 * speed));
        }else{
            return 0.;
        }
    }



    /** get sum dBa **/
    private static Double sumDba(Double dBA1, Double dBA2) {
        return PropagationProcess.wToDba(PropagationProcess.dbaToW(dBA1) + PropagationProcess.dbaToW(dBA2));
    }

    private static Double sumDba_5(Double dBA1, Double dBA2, Double dBA3, Double dBA4, Double dBA5) {
        return PropagationProcess.wToDba(PropagationProcess.dbaToW(dBA1) + PropagationProcess.dbaToW(dBA2) + PropagationProcess.dbaToW(dBA3) + PropagationProcess.dbaToW(dBA4) + PropagationProcess.dbaToW(dBA5));
    }

    /**
     * Road noise evaluation.
     * @param parameters Noise emission parameters
     * @return Noise level in dB
     */
    public static double evaluate(RSParametersCnossos parameters) {
        final int freqParam = parameters.getFreqParam();
        final double Temperature = parameters.getTemperature();
        final double Ts_stud = parameters.getTs_stud();
        final double Pm_stud = parameters.getPm_stud();
        final double Junc_dist = parameters.getJunc_dist();
        final int Junc_type = parameters.getJunc_type();
        final String roadSurface = parameters.getRoadSurface();


        // ///////////////////////
        // Noise road/tire CNOSSOS
        double lvRoadLvl; // Lw/m (1 veh/h)
        double medRoadLvl;// Lw/m (1 veh/h)
        double hgvRoadLvl;// Lw/m (1 veh/h)
        double wheelaRoadLvl;// Lw/m (1 veh/h)
        double wheelbRoadLvl;// Lw/m (1 veh/h)

        // Noise level
        lvRoadLvl = getNoiseLvl(getCoeff("ar", freqParam , "1"  ), getCoeff("br", freqParam , "1"  ), parameters.getSpeedLv(), 70.);
        medRoadLvl = getNoiseLvl(getCoeff("ar", freqParam , "2"  ), getCoeff("br", freqParam , "2"  ), parameters.getSpeedMv(), 70.);
        hgvRoadLvl = getNoiseLvl(getCoeff("ar", freqParam , "3"  ), getCoeff("br", freqParam , "3"  ), parameters.getSpeedHgv(), 70.);
        wheelaRoadLvl = getNoiseLvl(getCoeff("ar", freqParam , "4a"  ), getCoeff("br", freqParam , "4a"  ), parameters.getSpeedWav(), 70.);
        wheelbRoadLvl = getNoiseLvl(getCoeff("ar", freqParam , "4b"  ), getCoeff("br", freqParam , "4b"  ), parameters.getSpeedWbv(), 70.);

        // Correction by temperature p. 36
        lvRoadLvl = lvRoadLvl+ 0.08*(20-Temperature); // K = 0.08  p. 36
        medRoadLvl = medRoadLvl + 0.04*(20-Temperature); // K = 0.04 p. 36
        hgvRoadLvl = hgvRoadLvl + 0.04*(20-Temperature); // K = 0.04 p. 36


        // Rolling noise acceleration correction
        lvRoadLvl = lvRoadLvl + getCr("1", Junc_type) * Math.max(1-Math.abs(Junc_dist)/100,0) ;
        medRoadLvl = medRoadLvl + getCr("2", Junc_type)  * Math.max(1-Math.abs(Junc_dist)/100,0);
        hgvRoadLvl = hgvRoadLvl + getCr("3", Junc_type)  * Math.max(1-Math.abs(Junc_dist)/100,0);

        //Studied tyres
        if (Pm_stud >0 && Ts_stud > 0) {
            double deltastud = 0;
            double speed = parameters.getSpeedLv();
            double ps = Pm_stud * Ts_stud / 12; //yearly average proportion of vehicles equipped with studded tyres
            speed = (speed >= 90) ? 90 : speed;
            speed = (speed <= 50) ? 50 : speed;
            deltastud = getNoiseLvl(getCoeff("a", freqParam, "1"), getCoeff("b", freqParam, "1"), speed, 70.);
            lvRoadLvl = lvRoadLvl + 10 * Math.log10((1 - ps) + ps * Math.pow(10, deltastud / 10));
        }

        //Road surface correction on rolling noise
        lvRoadLvl = lvRoadLvl+ getNoiseLvl(getA_Roadcoeff(freqParam,"1", parameters.getRoadSurface()), getB_Roadcoeff("1",roadSurface), parameters.getSpeedLv(), 70.);
        medRoadLvl = medRoadLvl + getNoiseLvl(getA_Roadcoeff(freqParam,"2", parameters.getRoadSurface()), getB_Roadcoeff("2",roadSurface), parameters.getSpeedMv(), 70.);
        hgvRoadLvl = hgvRoadLvl + getNoiseLvl(getA_Roadcoeff(freqParam,"3", parameters.getRoadSurface()), getB_Roadcoeff("3",roadSurface), parameters.getSpeedHgv(), 70.);
        wheelaRoadLvl = wheelaRoadLvl + getNoiseLvl(getA_Roadcoeff(freqParam,"4a", parameters.getRoadSurface()), getB_Roadcoeff("4a",roadSurface), parameters.getSpeedWav(), 70.);
        wheelbRoadLvl = wheelbRoadLvl + getNoiseLvl(getA_Roadcoeff(freqParam,"4b", parameters.getRoadSurface()), getB_Roadcoeff("4b",roadSurface), parameters.getSpeedWbv(), 70.);

        // ///////////////////////
        // Noise motor
        // Calculate the emission powers of motors lights vehicles and heavies goods vehicles.

        // default or steady speed.
        double lvMotorLvl = getCoeff("ap", freqParam , "1"  ) + getCoeff("bp", freqParam , "1"  ) * (parameters.getSpeedLv()-70)/70 ;
        double medMotorLvl =  getCoeff("ap", freqParam , "2"  ) + getCoeff("bp", freqParam , "2"  ) * (parameters.getSpeedMv()-70)/70 ;
        double hgvMotorLvl =  getCoeff("ap", freqParam , "3"  ) + getCoeff("bp", freqParam , "3"  ) * (parameters.getSpeedHgv()-70)/70 ;
        double wheelaMotorLvl =  getCoeff("ap", freqParam , "4a"  ) + getCoeff("bp", freqParam , "4a"  ) * (parameters.getSpeedWav()-70)/70 ;
        double wheelbMotorLvl =  getCoeff("ap", freqParam , "4b"  ) + getCoeff("bp", freqParam , "4b"  ) * (parameters.getSpeedWbv()-70)/70 ;


        // Propulsion noise acceleration correction

        lvMotorLvl = lvMotorLvl + getCp("1", Junc_type) * Math.max(1-Math.abs(Junc_dist)/100,0) ;
        medMotorLvl = medMotorLvl + getCp("2", Junc_type)  * Math.max(1-Math.abs(Junc_dist)/100,0);
        hgvMotorLvl = hgvMotorLvl + getCp("3", Junc_type)  * Math.max(1-Math.abs(Junc_dist)/100,0);


        // Correction gradient for light vehicle
        if (parameters.getSlopePercentage() < -6) {
            // downwards 2% <= p <= 6%
            // Steady and deceleration, the same formulae
            lvMotorLvl = lvMotorLvl + (Math.min(12,-parameters.getSlopePercentage())-6)/1;
        }
        else if (parameters.getSlopePercentage() <= 2) {
            // 0% <= p <= 2%
            lvMotorLvl = lvMotorLvl + 0.;
        } else {
            // upwards 2% <= p <= 6%
            lvMotorLvl = lvMotorLvl + ((parameters.getSpeedLv()/100) * ((Math.min(12,parameters.getSlopePercentage())-2)/1.5));
        }
        // Correction gradient for trucks
        if (parameters.getSlopePercentage() < -4) {
            // Steady and deceleration, the same formulae
            medMotorLvl = medMotorLvl + ((parameters.getSpeedMv()-20)/100) * (Math.min(12,-1*parameters.getSlopePercentage())-4)/0.7;
            hgvMotorLvl = hgvMotorLvl + ((parameters.getSpeedHgv()-10)/100) * (Math.min(12,-1*parameters.getSlopePercentage())-4)/0.5;
        }
        else if (parameters.getSlopePercentage() <= 0) {
            medMotorLvl = medMotorLvl + 0.;
            hgvMotorLvl = hgvMotorLvl + 0.;
        } else {
            medMotorLvl = medMotorLvl + (parameters.getSpeedMv()/100) * (Math.min(12,parameters.getSlopePercentage()))/1;
            hgvMotorLvl = hgvMotorLvl + (parameters.getSpeedHgv()/100) * (Math.min(12,parameters.getSlopePercentage()))/0.8;
        }

        // Correction road on propulsion noise
        lvMotorLvl = lvMotorLvl+ Math.min(getA_Roadcoeff(freqParam ,"1",roadSurface), 0.);
        medMotorLvl = medMotorLvl + Math.min(getA_Roadcoeff(freqParam ,"2",roadSurface), 0.);
        hgvMotorLvl = hgvMotorLvl + Math.min(getA_Roadcoeff(freqParam ,"3",roadSurface), 0.);
        wheelaMotorLvl = wheelaMotorLvl + Math.min(getA_Roadcoeff(freqParam ,"4a",roadSurface), 0.);
        wheelbMotorLvl = wheelbMotorLvl + Math.min(getA_Roadcoeff(freqParam ,"4b",roadSurface), 0.);


        final double lvCompound = sumDba(lvRoadLvl, lvMotorLvl);
        final double medCompound = sumDba(medRoadLvl, medMotorLvl);
        final double hgvCompound = sumDba(hgvRoadLvl, hgvMotorLvl);
        final double wheelaCompound = sumDba(wheelaRoadLvl, wheelaMotorLvl);
        final double wheelbCompound = sumDba(wheelbRoadLvl, wheelbMotorLvl);


        // ////////////////////////
        // Lw/m (1 veh/h) to ?

        double lvLvl = Vperhour2NoiseLevel(lvCompound , parameters.getLvPerHour(), parameters.getSpeedLv());
        double medLvl =Vperhour2NoiseLevel(medCompound , parameters.getMvPerHour(), parameters.getSpeedMv());
        double hgvLvl =Vperhour2NoiseLevel(hgvCompound , parameters.getHgvPerHour(), parameters.getSpeedHgv());
        double wheelaLvl =Vperhour2NoiseLevel(wheelaCompound , parameters.getWavPerHour(), parameters.getSpeedWav());
        double wheelbLvl =Vperhour2NoiseLevel(wheelbCompound , parameters.getWbvPerHour(), parameters.getSpeedWbv());
        return sumDba_5(lvLvl, medLvl, hgvLvl, wheelaLvl, wheelbLvl);
    }
}
