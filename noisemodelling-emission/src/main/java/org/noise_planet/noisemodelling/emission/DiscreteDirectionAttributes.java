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

public class DiscreteDirectionAttributes {
    int directionIdentifier;
    double[] frequencies;
    // List of records, maintain the two lists sorted
    List<DirectivityRecord> recordsTheta = new ArrayList<>();
    List<DirectivityRecord> recordsPhi = new ArrayList<>();

    ThetaComparator thetaComparator = new ThetaComparator();
    PhiComparator phiComparator = new PhiComparator();

    public DiscreteDirectionAttributes(int directionIdentifier, double[] frequencies) {
        this.directionIdentifier = directionIdentifier;
        this.frequencies = frequencies;
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

    private static DirectivityRecord[] getIndexes(List<DirectivityRecord> records, Comparator<DirectivityRecord> comp, DirectivityRecord record) {
        int index = Collections.binarySearch(records, record, comp);
        if(index >= 0) {
            // This record already exists
            return new DirectivityRecord[] {records.get(index), records.get(index)};
        } else {
            index = - index - 1;
            int previousIndex = index - 1;
            if(previousIndex < 0) {
                previousIndex = records.size() - 1;
                // TODO, construct new angle with -2PI ?
            }
            return new DirectivityRecord[] {records.get(previousIndex), records.get(index)};
        }
    }

    private static double getDistance(float theta, float phi, DirectivityRecord b) {
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
        DirectivityRecord record = new DirectivityRecord(theta, phi, null);
        int index = Collections.binarySearch(recordsTheta, record, thetaComparator);
        if(index >= 0) {
            return recordsTheta.get(index);
        }
        index = - index - 1;
        float theta2 = recordsTheta.get(index).getTheta();
        // Take previous record
        index -= 1;
        if(index < 0) {
            index = recordsTheta.size() - 1;
        }
        float theta1 = recordsTheta.get(index).getTheta();
        index = Collections.binarySearch(recordsPhi, record, phiComparator);
        index = - index - 1;
        float phi2 = recordsPhi.get(index).getPhi();
        // Take previous record
        index -= 1;
        if(index < 0) {
            index = recordsPhi.size() - 1;
        }
        float phi1 = recordsPhi.get(index).getPhi();
        // Find closest records
        DirectivityRecord[] allRecords = new DirectivityRecord[] {
                recordsTheta.get(Collections.binarySearch(recordsTheta, new DirectivityRecord(theta1, phi1, null), thetaComparator)),
                recordsTheta.get(Collections.binarySearch(recordsTheta, new DirectivityRecord(theta2, phi1, null), thetaComparator)),
                recordsTheta.get(Collections.binarySearch(recordsTheta, new DirectivityRecord(theta2, phi2, null), thetaComparator)),
                recordsTheta.get(Collections.binarySearch(recordsTheta, new DirectivityRecord(theta1, phi2, null), thetaComparator))
        };
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
            // TODO bilinear interpolation
            return allRecords[0];
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
                    Float.compare(record.phi, phi) == 0 &&
                    Arrays.equals(attenuation, record.attenuation);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(theta, phi);
            result = 31 * result + Arrays.hashCode(attenuation);
            return result;
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
