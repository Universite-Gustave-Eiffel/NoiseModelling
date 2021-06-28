/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */
package org.noise_planet.noisemodelling.emission;

import java.io.Serializable;
import java.util.*;

public class DiscreteDirectionAttributes implements DirectionAttributes {
    int interpolationMethod = 1;
    int directionIdentifier;
    double[] frequencies;
    Map<Long, Integer> frequencyMapping = new HashMap<>();
    DirectivityRecord lastQuery = null;
    DirectivityRecord lastQueryRecord = null;
    // List of records, maintain the two lists sorted
    List<DirectivityRecord> recordsTheta = new ArrayList<>();
    List<DirectivityRecord> recordsPhi = new ArrayList<>();

    ThetaComparator thetaComparator = new ThetaComparator();
    PhiComparator phiComparator = new PhiComparator();

    public DiscreteDirectionAttributes(int directionIdentifier, double[] frequencies) {
        this.directionIdentifier = directionIdentifier;
        this.frequencies = frequencies;
        for(int idFrequency = 0; idFrequency < frequencies.length; idFrequency++) {
            frequencyMapping.put(Double.doubleToLongBits(frequencies[idFrequency]), idFrequency);
        }
    }

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

    @Override
    public double getAttenuation(double frequency, double phi, double theta) {
        DirectivityRecord query = new DirectivityRecord((float)theta, (float)phi, null);

        // look for frequency index
        Integer idFreq = frequencyMapping.get(Double.doubleToLongBits(frequency));
        if(idFreq == null) {
            // get closest index
            idFreq = Arrays.binarySearch(frequencies, frequency);
            if(idFreq < 0) {
                int last = Math.min(-idFreq - 1, frequencies.length - 1);
                int first = Math.max(last - 1, 0);
                idFreq = Math.abs(frequencies[first] - frequency) < Math.abs(frequencies[last] - frequency) ?
                        first : last;
            }
        }

        // for speed up, check if it is the same theta and phi queried last time
        if(!query.equals(lastQuery)) {
            // if not go looking for it
            lastQueryRecord = getRecord(query.theta, query.phi, interpolationMethod);
            lastQuery = query;
        }
        return lastQueryRecord.getAttenuation()[idFreq];
    }

    public void addDirectivityRecord(float theta, float phi, double[] attenuation) {
        DirectivityRecord record = new DirectivityRecord(theta, phi, attenuation);
        int index = Collections.binarySearch(recordsTheta, record, thetaComparator);
        if(index >= 0) {
            // This record already exists
            return;
        } else {
            index = - index - 1;
        }
        recordsTheta.add(index, record);
        index = Collections.binarySearch(recordsPhi, record, phiComparator);
        index = - index - 1;
        recordsPhi.add(index, record);
    }

    /**
     * @return Frequencies of columns
     */
    public double[] getFrequencies() {
        return frequencies;
    }

    private static double getDistance(double theta, double phi, DirectivityRecord b) {
        return Math.acos(Math.sin(phi) * Math.sin(b.phi) + Math.cos(phi) * Math.cos(b.phi) * Math.cos(theta - b.theta));
    }
    /**
     * Retrieve DirectivityRecord for the specified angles
     * @param theta in radians
     * @param phi in radians
     * @param interpolate 0 for closest neighbor, 1 for Bilinear interpolation
     * @return DirectivityRecord instance
     */
    public DirectivityRecord getRecord(float theta, float phi, int interpolate) {
        // all records points must be ordered on anti-clockwise
        // point[0] and point[2] must be the most distant
        DirectivityRecord[] allRecords = null;
        DirectivityRecord record = new DirectivityRecord(theta, phi, null);
        int index = Collections.binarySearch(recordsTheta, record, thetaComparator);
        if(index >= 0) {
            return recordsTheta.get(index);
        }
        index = - index - 1;
        if(index >= recordsTheta.size()) {
            index = 0;
        }
        float theta2 = recordsTheta.get(index).getTheta();
        // Take previous record
        index -= 1;
        if(index < 0) {
            index = recordsTheta.size() - 1;
        }
        float theta1 = recordsTheta.get(index).getTheta();
        index = Collections.binarySearch(recordsPhi, record, phiComparator);
        index = - index - 1;
        // If phi is out of bounds, we have to check all records
        boolean checkAllRecords = false;
        if(index >= recordsPhi.size()) {
            checkAllRecords = true;
        } else {
            float phi2 = recordsPhi.get(index).getPhi();
            // Take previous record
            index -= 1;
            if (index < 0) {
                checkAllRecords = true;
            } else {
                float phi1 = recordsPhi.get(index).getPhi();
                // Find closest records
                int[] indexes = new int[]{
                        Collections.binarySearch(recordsTheta, new DirectivityRecord(theta1, phi1, null), thetaComparator),
                        Collections.binarySearch(recordsTheta, new DirectivityRecord(theta2, phi1, null), thetaComparator),
                        Collections.binarySearch(recordsTheta, new DirectivityRecord(theta2, phi2, null), thetaComparator),
                        Collections.binarySearch(recordsTheta, new DirectivityRecord(theta1, phi2, null), thetaComparator)};
                if (Arrays.stream(indexes).min().getAsInt() < 0) {
                    checkAllRecords = true;
                }
                if (!checkAllRecords) {
                    allRecords = new DirectivityRecord[]{
                            recordsTheta.get(indexes[0]),
                            recordsTheta.get(indexes[1]),
                            recordsTheta.get(indexes[2]),
                            recordsTheta.get(indexes[3])
                    };
                }
            }
        }
        if(checkAllRecords) {
            // its quite time consuming process
            // but it only when requested around the poles
            // so its not quite often called
            // Find the 4 closest points
            allRecords = new DirectivityRecord[4];
            double[] minDist = new double[] { Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE};
            for(DirectivityRecord r : recordsTheta) {
                double testDist = getDistance(theta, phi, r);
                for(int idClosest = 0; idClosest < allRecords.length; idClosest++) {
                    if (testDist < minDist[idClosest]) {
                        minDist[idClosest] = testDist;
                        allRecords[idClosest] = r;
                        break;
                    }
                }
            }
            // place furthest from 0 on the 2 position
            double maxDist = 0;
            int furthestIndex = -1;
            int idrecord = 1;
            for(DirectivityRecord r : Arrays.copyOfRange(allRecords, 1, allRecords.length)) {
                double testDist = getDistance(r.theta, r.phi, allRecords[0]);
                if(testDist > maxDist) {
                    maxDist = testDist;
                    furthestIndex = idrecord;
                }
                idrecord++;
            }
            if(furthestIndex != 2) {
                // switch records
                DirectivityRecord r = allRecords[2];
                allRecords[2] = allRecords[furthestIndex];
                allRecords[furthestIndex] = r;
            }
        }
        if(interpolate == 0) {
            double minDist = Double.MAX_VALUE;
            DirectivityRecord closest = allRecords[0];
            for(DirectivityRecord r : allRecords) {
                double testDist = getDistance(theta, phi, r);
                if(testDist < minDist) {
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
            if(Double.isNaN(x)) {
                x = 0;
            }
            if(Double.isNaN(y)) {
                y = 0;
            }
            double[] att = new double[frequencies.length];
            for(int idFrequency = 0; idFrequency < frequencies.length; idFrequency++) {
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
     * This function is much more efficient than {@link #addDirectivityRecord(float, float, double[])}
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
            final int thetaCompare = Float.compare(o1.theta, o2.theta);
            if(thetaCompare != 0) {
                return thetaCompare;
            }
            return Float.compare(o1.phi, o2.phi);
        }

    }

    public static class PhiComparator implements Comparator<DirectivityRecord>, Serializable {

        @Override
        public int compare(DirectivityRecord o1, DirectivityRecord o2) {
            final int phiCompare = Float.compare(o1.phi, o2.phi);
            if(phiCompare != 0) {
                return phiCompare;
            }
            return Float.compare(o1.theta, o2.theta);
        }

    }
    public static class DirectivityRecord {
        private float theta;
        private float phi;
        private double[] attenuation;

        public DirectivityRecord(float theta, float phi, double[] attenuation) {
            this.theta = theta;
            this.phi = phi;
            this.attenuation = attenuation;
        }

        public float getTheta() {
            return theta;
        }

        public float getPhi() {
            return phi;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DirectivityRecord record = (DirectivityRecord) o;
            return Float.compare(record.theta, theta) == 0 &&
                    Float.compare(record.phi, phi) == 0;
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
