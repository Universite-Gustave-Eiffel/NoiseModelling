package org.noise_planet.noisemodelling.pathfinder.utils;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import org.locationtech.jts.geom.Coordinate;
import org.noise_planet.noisemodelling.pathfinder.PointPath;
import org.noise_planet.noisemodelling.pathfinder.PropagationPath;
import org.noise_planet.noisemodelling.pathfinder.SegmentPath;
import org.noise_planet.noisemodelling.pathfinder.Triangle;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

/**
 * Export rays for validation
 */
public class GeoJSONDocument {
    JsonGenerator jsonGenerator;
    String crs = "EPSG:4326";
    int rounding = -1;

    public GeoJSONDocument(OutputStream outputStream) throws IOException {
        JsonFactory jsonFactory = new JsonFactory();
        jsonGenerator = jsonFactory.createGenerator(outputStream, JsonEncoding.UTF8);
        jsonGenerator.setPrettyPrinter(new DefaultPrettyPrinter("\n"));
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

    public void writeRay(PropagationPath path) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("type", "Feature");
        jsonGenerator.writeObjectFieldStart("geometry");
        jsonGenerator.writeStringField("type", "LineString");
        jsonGenerator.writeFieldName("coordinates");
        jsonGenerator.writeStartArray();
        for(PointPath pointPath : path.getPointList()) {
            writeCoordinate(new Coordinate(pointPath.coordinate));
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject(); // geometry
        // Write properties
        jsonGenerator.writeObjectFieldStart("properties");
        jsonGenerator.writeNumberField("receiver", path.getIdReceiver());
        jsonGenerator.writeNumberField("source", path.getIdSource());
        if(path.getSRSegment() == null) {
            path.computeAugmentedSRPath();
        }
        jsonGenerator.writeArrayFieldStart("gPath");
        for(SegmentPath sr : path.getSegmentList()) {
            jsonGenerator.writeNumber(String.format(Locale.ROOT, "%.2f", sr.gPath));
        }
        jsonGenerator.writeEndArray(); //gPath
        jsonGenerator.writeEndObject(); // properties
        jsonGenerator.writeEndObject();
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
    private void writeCoordinate(Coordinate coordinate) throws IOException {
        jsonGenerator.writeStartArray();
        writeNumber(coordinate.x);
        writeNumber(coordinate.y);
        if (!Double.isNaN(coordinate.z)) {
            writeNumber(coordinate.z);
        }
        jsonGenerator.writeEndArray();
    }

    private void writeNumber(double number) throws IOException {
        if(rounding >= 0) {
            jsonGenerator.writeNumber(String.format(Locale.ROOT,"%."+rounding+"f", number));
        } else {
            jsonGenerator.writeNumber(number);
        }
    }
}
