package org.noise_planet.noisemodelling.wpsTools

import groovy.sql.Sql
import org.cts.crs.CRSException
import org.h2gis.utilities.SpatialResultSet
import org.locationtech.jts.geom.Geometry
import org.noise_planet.noisemodelling.emission.EvaluateRoadSourceCnossos
import org.noise_planet.noisemodelling.emission.EvaluateRoadSourceDynamic
import org.noise_planet.noisemodelling.emission.RSParametersCnossos
import org.noise_planet.noisemodelling.emission.RSParametersDynamic
import org.noise_planet.noisemodelling.propagation.ComputeRays
import org.noise_planet.noisemodelling.propagation.ComputeRaysOut
import org.noise_planet.noisemodelling.propagation.FastObstructionTest
import org.noise_planet.noisemodelling.propagation.KMLDocument
import org.noise_planet.noisemodelling.propagation.PropagationProcessData
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData
import org.noise_planet.noisemodelling.propagation.jdbc.PointNoiseMap

import javax.xml.stream.XMLStreamException
import java.sql.SQLException


class GeneralTools {

     /**
     * Spartan ProgressBar
     * @param newVal
     * @param currentVal
     * @return
     */
    static int ProgressBar(int newVal, int currentVal) {
        if (newVal != currentVal) {
            currentVal = newVal
            System.print(10 * currentVal + '% ... ')
        }
        return currentVal
    }


    /**
     *
     * @param array1
     * @param array2
     * @return
     */
    static double[] sumLinearArray(double[] array1, double[] array2) {
        if (array1.length != array2.length) {
            throw new IllegalArgumentException("Not same size array")
        } else {
            double[] sum = new double[array1.length]

            for (int i = 0; i < array1.length; ++i) {
                sum[i] = array1[i] + array2[i]
            }

            return sum
        }
    }


    /**
     * Export scene to kml format
     * @param name
     * @param manager
     * @param result
     * @return
     * @throws IOException
     */
    def static exportScene(String name, FastObstructionTest manager, ComputeRaysOut result) throws IOException {
        try {
            FileOutputStream outData = new FileOutputStream(name)
            KMLDocument kmlDocument = new KMLDocument(outData)
            kmlDocument.setInputCRS("EPSG:2154")
            kmlDocument.writeHeader()
            if (manager != null) {
                kmlDocument.writeTopographic(manager.getTriangles(), manager.getVertices())
            }
            if (result != null) {
                kmlDocument.writeRays(result.getPropagationPaths())
            }
            if (manager != null && manager.isHasBuildingWithHeight()) {
                kmlDocument.writeBuildings(manager)
            }
            kmlDocument.writeFooter()
        } catch (XMLStreamException | CRSException ex) {
            throw new IOException(ex)
        }
    }


    /**
     *
     * @param db
     * @return
     */
    static double[] DBToDBA(double[] db) {
        double[] dbA = [-26.2, -16.1, -8.6, -3.2, 0, 1.2, 1.0, -1.1]
        for (int i = 0; i < db.length; ++i) {
            db[i] = db[i] + dbA[i]
        }
        return db

    }

    /**
     * Sum two Array "octave band by octave band"
     * @param array1
     * @param array2
     * @return sum of to array
     */
    double[] sumArraySR(double[] array1, double[] array2) {
        if (array1.length != array2.length) {
            throw new IllegalArgumentException("Not same size array")
        } else {
            double[] sum = new double[array1.length]

            for (int i = 0; i < array1.length; ++i) {
                sum[i] = (array1[i]) + (array2[i])
            }

            return sum
        }
    }

}





