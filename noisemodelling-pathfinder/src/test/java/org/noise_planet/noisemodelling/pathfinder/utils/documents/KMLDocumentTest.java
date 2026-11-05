package org.noise_planet.noisemodelling.pathfinder.utils.documents;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.noise_planet.noisemodelling.pathfinder.path.Scene;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KMLDocumentTest {

    /**
     * Exporting the scene to KML must not move the sources used by the computation.
     */
    @Test
    public void testExportKeepsSourceCoordinates() throws IOException {
        ProfileBuilder profileBuilder = new ProfileBuilder();
        profileBuilder.finishFeeding();
        Scene scene = new Scene(profileBuilder);
        Coordinate source = new Coordinate(223222.88, 6757058.71, 0.05);
        scene.addSource(1L, new GeometryFactory().createPoint(source.copy()));

        KMLDocument.exportProfileBuilderData(new ByteArrayOutputStream(), scene, 2154);

        assertEquals(source, scene.sourceGeometries.get(0).getCoordinate());
    }
}
