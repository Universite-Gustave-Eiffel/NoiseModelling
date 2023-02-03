package org.noise_planet.noisemodelling.pathfinder.utils;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import org.cts.CRSFactory;
import org.cts.IllegalCoordinateException;
import org.cts.crs.CRSException;
import org.cts.crs.CoordinateReferenceSystem;
import org.cts.crs.GeodeticCRS;
import org.cts.op.CoordinateOperation;
import org.cts.op.CoordinateOperationException;
import org.cts.op.CoordinateOperationFactory;
import org.cts.registry.EPSGRegistry;
import org.cts.registry.RegistryManager;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.noise_planet.noisemodelling.pathfinder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.Triangle;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Export rays for validation
 */
public class GeoJSONDocument {
    public JsonGenerator jsonGenerator;
    public String crs = "EPSG:4326";
    public int rounding = -1;
    public CoordinateOperation transform = null;

    public GeoJSONDocument(OutputStream outputStream) throws IOException {
        JsonFactory jsonFactory = new JsonFactory();
        jsonGenerator = jsonFactory.createGenerator(outputStream, JsonEncoding.UTF8);
        jsonGenerator.setPrettyPrinter(new DefaultPrettyPrinter("\n"));
    }

    public void doTransform(Geometry geometry) {
        if(transform != null && geometry != null) {
            geometry.apply(new KMLDocument.CRSTransformFilter(transform));
            // Recompute envelope
            geometry.setSRID(4326);
        }
    }

    public void setInputCRS(String crs) throws CRSException, CoordinateOperationException {
        // Create a new CRSFactory, a necessary element to create a CRS without defining one by one all its components
        CRSFactory cRSFactory = new CRSFactory();

        // Add the appropriate registry to the CRSFactory's registry manager. Here the EPSG registry is used.
        RegistryManager registryManager = cRSFactory.getRegistryManager();
        registryManager.addRegistry(new EPSGRegistry());

        // CTS will read the EPSG registry seeking the 4326 code, when it finds it,
        // it will create a CoordinateReferenceSystem using the parameters found in the registry.
        CoordinateReferenceSystem crsKML = cRSFactory.getCRS("EPSG:4326");
        CoordinateReferenceSystem crsSource = cRSFactory.getCRS(crs);
        if(crsKML instanceof GeodeticCRS && crsSource instanceof GeodeticCRS) {
            transform = CoordinateOperationFactory.createCoordinateOperations((GeodeticCRS) crsSource, (GeodeticCRS) crsKML).iterator().next();
        }
    }

    public void setRounding(int rounding) {
        this.rounding = rounding;
    }

    public void writeFooter() throws IOException {
        jsonGenerator.writeEndArray(); // features
        jsonGenerator.writeObjectFieldStart("crs");
        jsonGenerator.writeStringField("type", "name");
        jsonGenerator.writeObjectFieldStart("properties");
        jsonGenerator.writeStringField("name", crs);
        jsonGenerator.writeEndObject(); // properties
        jsonGenerator.writeEndObject(); // crs
        jsonGenerator.writeEndObject(); // {
        jsonGenerator.flush();
        jsonGenerator.close();
    }
    public void writeHeader() throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("type", "FeatureCollection");
        jsonGenerator.writeArrayFieldStart("features");
    }


    public void writeProfile(ProfileBuilder.CutProfile profile) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("type", "Feature");
        jsonGenerator.writeObjectFieldStart("geometry");
        jsonGenerator.writeStringField("type", "LineString");
        jsonGenerator.writeFieldName("coordinates");
        jsonGenerator.writeStartArray();

        for(ProfileBuilder.CutPoint cutPoint : profile.getCutPoints()) {
            writeCoordinate(new Coordinate(cutPoint.getCoordinate()));
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject(); // geometry
        // Write properties
        jsonGenerator.writeObjectFieldStart("properties");
        jsonGenerator.writeNumberField("receiver", profile.getReceiver().getId());
        jsonGenerator.writeNumberField("source", profile.getSource().getId());
        jsonGenerator.writeEndObject(); // properties
        jsonGenerator.writeEndObject();
    }

    public void writeCutPoint(ProfileBuilder.CutPoint cutPoint) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("type", "Feature");
        jsonGenerator.writeObjectFieldStart("geometry");
        jsonGenerator.writeStringField("type", "Point");
        jsonGenerator.writeFieldName("coordinates");
        writeCoordinate(new Coordinate(cutPoint.getCoordinate()));
        jsonGenerator.writeEndObject(); // geometry
        // Write properties
        jsonGenerator.writeObjectFieldStart("properties");
        Double zGround = cutPoint.getzGround();
        if(zGround != null && !Double.isNaN(zGround)) {
            jsonGenerator.writeNumberField("zGround", zGround);
        }
        if(cutPoint.getBuildingId() != - 1) {
            jsonGenerator.writeNumberField("building", cutPoint.getBuildingId());
            jsonGenerator.writeNumberField("height", cutPoint.getHeight());
            jsonGenerator.writeStringField("alpha", cutPoint.getWallAlpha().stream().
                    map(aDouble -> String.format("%.2f", aDouble)).collect(Collectors.joining(",")));
        }
        jsonGenerator.writeStringField("type", cutPoint.getType().toString());
        if(cutPoint.getGroundCoef() != 0) {
            jsonGenerator.writeNumberField("g", cutPoint.getGroundCoef());
        }
        jsonGenerator.writeEndObject(); // properties
        jsonGenerator.writeEndObject();
        jsonGenerator.flush();
    }

    /**
     * Write topography triangles
     * @param triVertices
     * @param vertices
     * @throws IOException
     */
    public void writeTopographic(List<Triangle> triVertices, List<Coordinate> vertices) throws IOException {
        for(Triangle triangle : triVertices) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("type", "Feature");
            jsonGenerator.writeObjectFieldStart("geometry");
            jsonGenerator.writeStringField("type", "Polygon");
            jsonGenerator.writeFieldName("coordinates");
            jsonGenerator.writeStartArray();
            jsonGenerator.writeStartArray(); // Outer line
            writeCoordinate(new Coordinate(vertices.get(triangle.getA())));
            writeCoordinate(new Coordinate(vertices.get(triangle.getB())));
            writeCoordinate(new Coordinate(vertices.get(triangle.getC())));
            writeCoordinate(new Coordinate(vertices.get(triangle.getA())));
            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject(); // geometry
            // Write properties
            jsonGenerator.writeObjectFieldStart("properties");
            jsonGenerator.writeNumberField("b", triangle.getAttribute());
            jsonGenerator.writeEndObject(); // properties
            jsonGenerator.writeEndObject();
        }
    }

    /**
     * Write coordinate positions.
     *
     * @param coordinate
     * @throws IOException
     */
    public void writeCoordinate(Coordinate coordinate) throws IOException {
        jsonGenerator.writeStartArray();
        if(transform != null) {
            try {
                double[] coords = transform.transform(new double[]{coordinate.x, coordinate.y});
                coordinate = new Coordinate(coords[0], coords[1], coordinate.z);
            } catch (IllegalCoordinateException | CoordinateOperationException ex) {
                throw new IOException("Error while doing transform", ex);
            }
        }
        writeNumber(coordinate.x);
        writeNumber(coordinate.y);
        if (!Double.isNaN(coordinate.z)) {
            writeNumber(coordinate.z);
        }
        jsonGenerator.writeEndArray();
    }

    public void writeNumber(double number) throws IOException {
        if(rounding >= 0) {
            jsonGenerator.writeNumber(String.format(Locale.ROOT,"%."+rounding+"f", number));
        } else {
            jsonGenerator.writeNumber(number);
        }
    }
}
