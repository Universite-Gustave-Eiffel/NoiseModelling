/**
 * NoiseModelling is a free and open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by Université Gustave Eiffel and CNRS
 * <http://noise-planet.org/noisemodelling.html>
 * as part of:
 * the Eval-PDU project (ANR-08-VILL-0005) 2008-2011, funded by the Agence Nationale de la Recherche (French)
 * the CENSE project (ANR-16-CE22-0012) 2017-2021, funded by the Agence Nationale de la Recherche (French)
 * the Nature4cities (N4C) project, funded by European Union’s Horizon 2020 research and innovation programme under grant agreement No 730468
 *
 * Noisemap is distributed under GPL 3 license.
 *
 * Contact: contact@noise-planet.org
 *
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488) and Ifsttar
 * Copyright (C) 2013-2019 Ifsttar and CNRS
 * Copyright (C) 2020 Université Gustave Eiffel and CNRS
 *
 * @Author Pierre Aumond, Université Gustave Eiffel
 * @Author Nicolas Fortin, Université Gustave Eiffel
 */
package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.functions.io.shp.SHPRead;
import org.h2gis.utilities.SFSUtilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.noise_planet.noisemodelling.pathfinder.*;
import org.noise_planet.noisemodelling.pathfinder.utils.KMLDocument;
import org.noise_planet.noisemodelling.propagation.ComputeRaysOutAttenuation;
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TestCodeRegression {
    Logger logger = LoggerFactory.getLogger(TestCodeRegression.class);

    private Connection connection;

    @Before
    public void tearUp() throws Exception {
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(TestCodeRegression.class.getSimpleName(), true, ""));
    }

    @After
    public void tearDown() throws Exception {
        if(connection != null) {
            connection.close();
        }
    }

    Coordinate get3dCoordinate(FastObstructionTest ft, double x, double y, double deltaZ) {
        Coordinate c = new Coordinate(x,y);
        c.setOrdinate(2, ft.getHeightAtPosition(c) + deltaZ);
        return c;
    }

    @Test
    public void testDiffractionOverDem() throws SQLException, IOException {
        // Import Data
        SHPRead.readShape(connection, "/home/nicolas/data/plamade/debug/2/study_BUILDINGS_SCREENS.shp", "BUILDINGS");
        SHPRead.readShape(connection, "/home/nicolas/data/plamade/debug/2/study_DEM.shp", "DEM");
        SHPRead.readShape(connection, "/home/nicolas/data/plamade/debug/2/study_LANDCOVER.shp", "LANDCOVER");
        SHPRead.readShape(connection, "/home/nicolas/data/plamade/debug/2/study_LW_ROADS.shp", "LW_ROADS");
        try(Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE RECEIVERS(pk serial primary key, the_geom geometry)");
            st.execute("INSERT INTO RECEIVERS(THE_GEOM) VALUES ('POINT(812994.84 6389199.87)'::geometry)");
            st.execute("INSERT INTO RECEIVERS(THE_GEOM) VALUES ('POINT(813038.10 6389308.50)'::geometry)");
        }
        Set<Long> receiversDone = new HashSet<>();
        PointNoiseMap pointNoiseMap = new PointNoiseMap("BUILDINGS", "LW_ROADS", "RECEIVERS");
        pointNoiseMap.setDemTable("DEM");
        pointNoiseMap.setSoilTableName("LANDCOVER");
        pointNoiseMap.setMaximumPropagationDistance(2000);
        pointNoiseMap.initialize(connection, new EmptyProgressVisitor());
        pointNoiseMap.setGridDim(1);

        PropagationProcessData threadData = pointNoiseMap.prepareCell(connection, 0, 0,
                new EmptyProgressVisitor(), receiversDone);

        ComputeRaysOutAttenuation computeRaysOut = new ComputeRaysOutAttenuation(true,
                pointNoiseMap.propagationProcessPathData ,threadData);

        ComputeRays computeRays = new ComputeRays(threadData);

        computeRays.makeReceiverRelativeZToAbsolute();

        computeRays.makeSourceRelativeZToAbsolute();

        computeRays.initStructures();

        Coordinate sourceCoordinate = get3dCoordinate(threadData.freeFieldFinder, 813343.64,6388665.24, 0.05);
        Coordinate receiverCoordinate = get3dCoordinate(threadData.freeFieldFinder, 813345.06, 6388577.92, 4);
        AtomicInteger raysCount = new AtomicInteger();
        ComputeRays.SourcePointInfo srcPoint = new ComputeRays.SourcePointInfo(
                threadData.getMaximalSourcePower(0),0,
                sourceCoordinate,1.0,new Orientation(0,0, 0),0);
        computeRays.receiverSourcePropa( srcPoint, receiverCoordinate, 0
                , raysCount, computeRaysOut, new ArrayList<FastObstructionTest.Wall>(), new ArrayList<MirrorReceiverResult>());


        for(ComputeRaysOutAttenuation.VerticeSL v : computeRaysOut.receiversAttenuationLevels) {
            logger.info(Arrays.toString(v.value));
        }

    }
}
