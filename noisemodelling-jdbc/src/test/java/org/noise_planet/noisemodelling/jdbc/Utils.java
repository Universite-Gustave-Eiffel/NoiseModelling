package org.noise_planet.noisemodelling.jdbc;

import org.h2.util.StringUtils;
import org.h2gis.utilities.SpatialResultSet;
import org.locationtech.jts.geom.Geometry;
import org.noise_planet.noisemodelling.pathfinder.*;
import org.noise_planet.noisemodelling.propagation.ComputeRaysOut;
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Utils {
    public static double[] addArray(double[] first, double[] second) {
        int length = Math.min(first.length, second.length);
        double[] result = new double[length];

        for (int i = 0; i < length; i++) {
            result[i] = first[i] + second[i];
        }

        return result;
    }
    public static String getRunScriptRes(String fileName) throws URISyntaxException {
        File resourceFile = new File(PointNoiseMapTest.class.getResource(fileName).toURI());
        return "RUNSCRIPT FROM "+ StringUtils.quoteStringSQL(resourceFile.getPath());
    }

    public static class JDBCPropagationData implements PointNoiseMap.PropagationProcessDataFactory {
        @Override
        public PropagationProcessData create(FastObstructionTest freeFieldFinder) {
            return new DirectPropagationProcessData(freeFieldFinder);
        }

        @Override
        public void initialize(Connection connection, PointNoiseMap pointNoiseMap) {

        }
    }

    public static class JDBCComputeRaysOut implements PointNoiseMap.IComputeRaysOutFactory {
        boolean keepRays;

        public JDBCComputeRaysOut(boolean keepRays) {
            this.keepRays = keepRays;
        }

        @Override
        public IComputeRaysOut create(PropagationProcessData threadData, PropagationProcessPathData pathData) {
            return new RayOut(keepRays, pathData, (DirectPropagationProcessData)threadData);
        }
    }

    private static final class RayOut extends ComputeRaysOut {
        private DirectPropagationProcessData processData;

        public RayOut(boolean keepRays, PropagationProcessPathData pathData, DirectPropagationProcessData processData) {
            super(keepRays, pathData, processData);
            this.processData = processData;
        }

        @Override
        public double[] computeAttenuation(PropagationProcessPathData pathData, long sourceId, double sourceLi, long receiverId, List<PropagationPath> propagationPath) {
            double[] attenuation = super.computeAttenuation(pathData, sourceId, sourceLi, receiverId, propagationPath);
            double[] soundLevel = ComputeRays.wToDba(ComputeRays.multArray(processData.wjSources.get((int)sourceId), ComputeRays.dbaToW(attenuation)));
            return soundLevel;
        }
    }

    private static class DirectPropagationProcessData extends PropagationProcessData {
        List<double[]> wjSources = new ArrayList<>();
        private final static String[] powerColumns = new String[]{"db_m63", "db_m125", "db_m250", "db_m500", "db_m1000", "db_m2000", "db_m4000", "db_m8000"};

        public DirectPropagationProcessData(FastObstructionTest freeFieldFinder) {
            super(freeFieldFinder);
        }


        @Override
        public void addSource(Long pk, Geometry geom, SpatialResultSet rs) throws SQLException, IOException {
            super.addSource(pk, geom, rs);
            double sl[] = new double[powerColumns.length];
            int i = 0;
            for(String columnName : powerColumns) {
                sl[i++] = ComputeRays.dbaToW(rs.getDouble(columnName));
            }
            wjSources.add(sl);
        }

        @Override
        public double[] getMaximalSourcePower(int sourceId) {
            return wjSources.get(sourceId);
        }
    }


    public static double[] aWeighting(List<Double> lvls) {
        return new double[] {lvls.get(0) - 26.2, lvls.get(1) - 16.1, lvls.get(2) - 8.6, lvls.get(3) - 3.2, lvls.get(4) ,
                lvls.get(5) + 1.2, lvls.get(6) + 1, lvls.get(7)  - 1.1};
    }
}
