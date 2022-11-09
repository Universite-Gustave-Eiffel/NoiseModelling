/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.emission.directivity;

import org.noise_planet.noisemodelling.emission.utils.Utils;

import java.io.Serializable;
import java.util.*;

/**
 * Describe Attenuation directivity over a sphere
 * Values between specified angles are interpolated following an interpolation method (linear by default or closest neighbor)
 * @author Nicolas Fortin, Université Gustave Eiffel
 */

public class DiscreteDirectivitySphere implements DirectivitySphere {

    int interpolationMethod = 1; //interpolation method between specified angles : 0 for closest neighbor, 1 for Bilinear interpolation
    int directionIdentifier; // primary key of the directional sphere (a table with various directional sphere can be given by the user)
    double[] frequencies; // array of frequencies (Hz)
    Map<Long, Integer> frequencyMapping = new HashMap<>();
    // List of records, maintain the two lists sorted
    // A DirectivityRecord is the attenuation value for a specific angle (theta, phi) - a point of the directivity sphere
    List<DirectivityRecord> recordsTheta = new ArrayList<>();
    List<DirectivityRecord> recordsPhi = new ArrayList<>();

    ThetaComparator thetaComparator = new ThetaComparator();
    PhiComparator phiComparator = new PhiComparator();

    /**
     * DiscreteDirectivitySphere defines the discrete directional sphere
     * @param directionIdentifier primary key of the directional sphere (a table with various directional sphere can be given by the user)
     * @param frequencies array of frequencies (Hz)
     */
    public DiscreteDirectivitySphere(int directionIdentifier, double[] frequencies) {
        this.directionIdentifier = directionIdentifier;
        this.frequencies = frequencies;
        for (int idFrequency = 0; idFrequency < frequencies.length; idFrequency++) {
            frequencyMapping.put(Double.doubleToLongBits(frequencies[idFrequency]), idFrequency);
        }
    }

    /**
     * Get distance in spherical coordinates between a point on the sphere define by its specific angle (theta, phi) and a point of the directivity sphere (DirectivityRecord)
     * @param theta (-π/2 π/2) 0 is horizontal; π is top
     * @param phi (0 2π) 0 is front
     * @param b DirectivityRecord - point of the directivity sphere
     * @return distance
     */
    private static double getDistance(double theta, double phi, DirectivityRecord b) {
        return Math.acos(Math.sin(phi) * Math.sin(b.phi) + Math.cos(phi) * Math.cos(b.phi) * Math.cos(theta - b.theta));
    }

    /**
     * Set the interpolation method
     * @param interpolationMethod interpolation method between specified angles : 0 for closest neighbor, 1 for Bilinear interpolation
     */
    public void setInterpolationMethod(int interpolationMethod) {
        this.interpolationMethod = interpolationMethod;
    }

    public List<DirectivityRecord> getRecordsTheta() {
        return recordsTheta;
    }

    public List<DirectivityRecord> getRecordsPhi() {
        return recordsPhi;
    }

    public int getDirectionIdentifier() {
        return directionIdentifier;
    }

    /**
     * Returns the attenuation in dB for a particular frequency of the directivity pattern for a certain angle (phi, theta)
     * @param frequency Frequency in Hertz
     * @param phi (0 2π) with 0 is front
     * @param theta (-π/2 π/2) with 0 is horizontal; π is top
     * @return Attenuation level in dB
     */
    @Override
    public double getAttenuation(double frequency, double phi, double theta) {
        DirectivityRecord query = new DirectivityRecord(theta, phi, null);

        // look for frequency index
        Integer idFreq = frequencyMapping.get(Double.doubleToLongBits(frequency));
        if (idFreq == null) {
            // get closest index
            idFreq = Arrays.binarySearch(frequencies, frequency);
            if (idFreq < 0) {
                int last = Math.min(-idFreq - 1, frequencies.length - 1);
                int first = Math.max(last - 1, 0);
                idFreq = Math.abs(frequencies[first] - frequency) < Math.abs(frequencies[last] - frequency) ?
                        first : last;
            }
        }
        return getRecord(query.theta, query.phi, interpolationMethod).getAttenuation()[idFreq];
    }

    /**
     * Returns the attenuation in dB of the directivity pattern for a certain angle (phi, theta)
     * @param frequencies Frequency in Hertz (same order will be returned)
     * @param phi (0 2π) with 0 is front
     * @param theta (-π/2 π/2) with 0 is horizontal; π is top
     * @return Attenuation level in dB
     */
    @Override
    public double[] getAttenuationArray(double[] frequencies, double phi, double theta) {
        DirectivityRecord query = new DirectivityRecord(theta, phi, null);

        DirectivityRecord record = getRecord(query.theta, query.phi, interpolationMethod);

        double[] returnAttenuation = new double[frequencies.length];

        for (int frequencyIndex = 0; frequencyIndex < frequencies.length; frequencyIndex++) {
            double frequency = frequencies[frequencyIndex];
            // look for frequency index
            Integer idFreq = frequencyMapping.get(Double.doubleToLongBits(frequency));
            if (idFreq == null) {
                // get closest index
                idFreq = Arrays.binarySearch(frequencies, frequency);
                if (idFreq < 0) {
                    int last = Math.min(-idFreq - 1, frequencies.length - 1);
                    int first = Math.max(last - 1, 0);
                    idFreq = Math.abs(frequencies[first] - frequency) < Math.abs(frequencies[last] - frequency) ? first : last;
                }
            }
            returnAttenuation[frequencyIndex] = record.attenuation[idFreq];
        }

        return returnAttenuation;
    }

    /**
     * Add angle attenuation record for the directivity sphere
     * @param theta (-π/2 π/2) 0 is horizontal; π is top
     * @param phi (0 2π) 0 is front
     * @param attenuation Attenuation in dB
     */
    public void addDirectivityRecord(double theta, double phi, double[] attenuation) {
        DirectivityRecord record = new DirectivityRecord(theta, phi, attenuation);
        int index = Collections.binarySearch(recordsTheta, record, thetaComparator);
        if (index >= 0) {
            // This record already exists
            return;
        } else {
            index = -index - 1;
        }
        recordsTheta.add(index, record);
        index = Collections.binarySearch(recordsPhi, record, phiComparator);
        index = -index - 1;
        recordsPhi.add(index, record);
    }

    /**
     * @return Frequencies of columns
     */
    public double[] getFrequencies() {
        return frequencies;
    }

    /**
     * Retrieve DirectivityRecord for the specified angles
     * @param theta in radians
     * @param phi in radians
     * @param interpolate 0 for closest neighbor, 1 for Bilinear interpolation
     * @return DirectivityRecord instance
     */
    public DirectivityRecord getRecord(double theta, double phi, int interpolate) {
        // all records points must be ordered on anti-clockwise
        // point[0] and point[2] must be the most distant
        DirectivityRecord[] allRecords = null;
        DirectivityRecord record = new DirectivityRecord(theta, phi, null);
        int index = Collections.binarySearch(recordsTheta, record, thetaComparator);
        if (index >= 0) {
            return recordsTheta.get(index);
        }
        index = -index - 1;
        if (index >= recordsTheta.size()) {
            index = 0;
        }
        double theta2 = recordsTheta.get(index).getTheta();
        // Take previous record
        index -= 1;
        if (index < 0) {
            index = recordsTheta.size() - 1;
        }
        double theta1 = recordsTheta.get(index).getTheta();
        index = Collections.binarySearch(recordsPhi, record, phiComparator);
        index = -index - 1;
        if (index >= recordsPhi.size()) {
            index = 0;
        }
        double phi2 = recordsPhi.get(index).getPhi();
        // Take previous record
        index -= 1;
        if (index < 0) {
            index = recordsPhi.size() - 1;
        }
        double phi1 = recordsPhi.get(index).getPhi();
        // Find closest records
        int[] indexes = new int[]{
                Collections.binarySearch(recordsTheta, new DirectivityRecord(theta1, phi1, null), thetaComparator),
                Collections.binarySearch(recordsTheta, new DirectivityRecord(theta2, phi1, null), thetaComparator),
                Collections.binarySearch(recordsTheta, new DirectivityRecord(theta2, phi2, null), thetaComparator),
                Collections.binarySearch(recordsTheta, new DirectivityRecord(theta1, phi2, null), thetaComparator)};
        if (Arrays.stream(indexes).min().getAsInt() < 0) {
            // got issues looking for directivity
            return new DirectivityRecord(theta, phi, new double[frequencies.length]);
        }
        allRecords = new DirectivityRecord[]{
                recordsTheta.get(indexes[0]),
                recordsTheta.get(indexes[1]),
                recordsTheta.get(indexes[2]),
                recordsTheta.get(indexes[3])
        };
        if (interpolate == 0) {
            double minDist = Double.MAX_VALUE;
            DirectivityRecord closest = allRecords[0];
            for (DirectivityRecord r : allRecords) {
                double testDist = getDistance(theta, phi, r);
                if (testDist < minDist) {
                    minDist = testDist;
                    closest = r;
                }
            }
            return closest;
        } else {
            // https://en.wikipedia.org/wiki/Bilinear_interpolation#Unit_square
            double x1 = allRecords[0].theta;
            double y1 = allRecords[0].phi;

            double xLength = getDistance(allRecords[1].theta, allRecords[0].phi, allRecords[0]);
            double yLength = getDistance(allRecords[0].theta, allRecords[2].phi, allRecords[0]);
            // compute expected phi, theta as a normalized vector
            double x = Math.max(0, Math.min(1, getDistance(x1, record.phi, record) / xLength));
            double y = Math.max(0, Math.min(1, getDistance(record.theta, y1, record) / yLength));
            if (Double.isNaN(x)) {
                x = 0;
            }
            if (Double.isNaN(y)) {
                y = 0;
            }
            double[] att = new double[frequencies.length];
            for (int idFrequency = 0; idFrequency < frequencies.length; idFrequency++) {
                att[idFrequency] = Utils.wToDb(Utils.dbToW(allRecords[0].attenuation[idFrequency]) * (1 - x) * (1 - y)
                        + Utils.dbToW(allRecords[1].attenuation[idFrequency]) * x * (1 - y)
                        + Utils.dbToW(allRecords[3].attenuation[idFrequency]) * (1 - x) * y
                        + Utils.dbToW(allRecords[2].attenuation[idFrequency]) * x * y);
            }
            return new DirectivityRecord(theta, phi, att);
        }
    }

    /**
     * Add new records.
     * This function is much more efficient than {@link #addDirectivityRecord(double, double, double[])}
     * @param newRecords Records to push
     */
    public void addDirectivityRecords(Collection<DirectivityRecord> newRecords) {
        recordsTheta.addAll(newRecords);
        recordsTheta.sort(thetaComparator);
        recordsPhi.addAll(newRecords);
        recordsPhi.sort(phiComparator);
    }

    public static class ThetaComparator implements Comparator<DirectivityRecord>, Serializable {

        @Override
        public int compare(DirectivityRecord o1, DirectivityRecord o2) {
            final int thetaCompare = Double.compare(o1.theta, o2.theta);
            if (thetaCompare != 0) {
                return thetaCompare;
            }
            return Double.compare(o1.phi, o2.phi);
        }

    }

    public static class PhiComparator implements Comparator<DirectivityRecord>, Serializable {

        @Override
        public int compare(DirectivityRecord o1, DirectivityRecord o2) {
            final int phiCompare = Double.compare(o1.phi, o2.phi);
            if (phiCompare != 0) {
                return phiCompare;
            }
            return Double.compare(o1.theta, o2.theta);
        }

    }

    /**
     * directivity record is the attenuation value for a specific angle (theta, phi) - a point of the directivity sphere
     */
    public static class DirectivityRecord {
        private double theta;
        private double phi;
        private double[] attenuation;

        /**
         * directivity record is the attenuation value for a specific angle (theta, phi) - a point of the directivity sphere
         *
         * @param theta (-π/2 π/2) 0 is horizontal; π is top
         * @param phi (0 2π) 0 is front
         * @param attenuation in dB
         */
        public DirectivityRecord(double theta, double phi, double[] attenuation) {
            this.theta = theta;
            this.phi = phi;
            this.attenuation = attenuation;
        }

        public double getTheta() {
            return theta;
        }

        public double getPhi() {
            return phi;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DirectivityRecord record = (DirectivityRecord) o;
            return Double.compare(record.theta, theta) == 0 &&
                    Double.compare(record.phi, phi) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(theta, phi);
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "DirectivityRecord{theta=%.2f (%.2g°)" +
                            ", phi=%.2f (%.2g°) , attenuation=%s}", theta, Math.toDegrees(theta), phi, Math.toDegrees(phi),
                    Arrays.toString(attenuation));
        }

        public double[] getAttenuation() {
            return attenuation;
        }
    }
}
