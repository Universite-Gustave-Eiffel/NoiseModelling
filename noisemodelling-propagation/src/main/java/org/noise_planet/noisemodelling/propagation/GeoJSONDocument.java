package org.noise_planet.noisemodelling.propagation;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import org.locationtech.jts.geom.Coordinate;

import java.io.IOException;
import java.io.OutputStream;
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
        jsonGenerator.writeEndObject(); // {
        jsonGenerator.flush();
        jsonGenerator.close();
    }
    public void writeHeader() throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("type", "FeatureCollection");
        jsonGenerator.writeObjectFieldStart("crs");
        jsonGenerator.writeStringField("type", "name");
        jsonGenerator.writeObjectFieldStart("properties");
        jsonGenerator.writeStringField("name", crs);
        jsonGenerator.writeEndObject(); // properties
        jsonGenerator.writeEndObject(); // crs
        jsonGenerator.writeArrayFieldStart("features");
    }

    public void writeRay(PropagationPath path) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("type", "Feature");
        jsonGenerator.writeObjectFieldStart("geometry");
        jsonGenerator.writeStringField("type", "LineString");
        jsonGenerator.writeFieldName("coordinates");
        jsonGenerator.writeStartArray();
        for(PropagationPath.PointPath pointPath : path.getPointList()) {
            writeCoordinate(new Coordinate(pointPath.coordinate));
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject(); // geometry
        // Write properties
        jsonGenerator.writeObjectFieldStart("properties");
        jsonGenerator.writeNumberField("receiver", path.idReceiver);
        jsonGenerator.writeNumberField("source", path.idSource);
        if(path.getSRList() == null || path.getSRList().isEmpty()) {
            path.computeAugmentedSRPath();
        }
        jsonGenerator.writeArrayFieldStart("gPath");
        for(PropagationPath.SegmentPath sr : path.getSegmentList()) {
            jsonGenerator.writeNumber(String.format(Locale.ROOT, "%.2f", sr.gPath));
        }
        jsonGenerator.writeEndArray(); //gPath
        jsonGenerator.writeEndObject(); // properties
        jsonGenerator.writeEndObject();
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
