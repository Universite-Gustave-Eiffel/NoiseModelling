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

/**
 * @Author Pierre Aumond, Université Gustave Eiffel
 */

package org.noise_planet.noisemodelling.scripts.Explain

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.sql.Sql
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.dbtypes.DBTypes
import org.h2gis.utilities.dbtypes.DBUtils
import org.geotools.api.referencing.operation.MathTransform
import org.geotools.geometry.jts.JTS
import org.geotools.referencing.CRS
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.MultiPoint
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.io.WKBReader
import org.locationtech.jts.io.WKTReader
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Clob
import java.sql.Connection
import java.util.Locale

title = 'Explain propagation rays'
description = '&#10145;&#65039; Display propagation rays of a <code>RAYS</code> table as an interactive HTML fragment. </br> </br>' +
        'The block selects the <b>top N rays by LEQ</b> for one receiver, meteo condition and period, then adds the <b>direct ray</b> when it is not already in that selection and when its level is within 20 dB of the total level (meaning it contributes to the receiver level). It draws a 2D scene, a vertical cut profile and a table of attenuation terms. </br> </br>' +
        '&#x1F4CB; <b>Prerequisite:</b> run <code>Noise_level_from_source</code> with <code>confRaysName=RAYS</code> so the <code>RAYS</code> table contains <code>IDRECEIVER</code>, <code>IDSOURCE</code>, <code>LEQ</code>, <code>METEO</code>, <code>PERIOD</code> and <code>PATH</code>. </br> </br>' +
        '&#x1F5FA; Optional tables (<code>DEM</code>, <code>BUILDINGS</code>, <code>GROUNDS</code>, <code>RECEIVERS</code>, <code>SOURCES</code>) enrich the scene but are never required. Terrain and ground coefficients are read from <code>PATH</code> first. </br> </br>' +
        '&#x1F4CB; <b>Example:</b> <code>tableRays=RAYS</code>, <code>meteo=homogeneous</code>, <code>period=D</code>, <code>maxRays=50</code>. When the direct ray is outside the top N it is appended at the end, so the total can reach N+1 rays. </br> </br>' +
        '&#x2139;&#65039; This block intentionally performs no comparison with observation data, no error computation and has no 3D view.'

inputs = [
        tableRays    : [
                name       : 'RAYS table',
                title      : 'RAYS table',
                description: 'Table produced with <code>Noise_level_from_source</code> and <code>confRaysName=RAYS</code>. Required columns: <code>IDRECEIVER</code>, <code>IDSOURCE</code>, <code>LEQ</code>, <code>METEO</code>, <code>PERIOD</code>, <code>PATH</code>. Example: <code>RAYS</code>.',
                type       : String.class
        ],
        inputSRID    : [
                name       : 'Source SRID (optional)',
                title      : 'Source SRID (optional)',
                description: 'EPSG code of the coordinates stored in <code>PATH</code> and in the optional tables (for example <code>2154</code> for Lambert-93). It is used to transform the 2D scene to WGS84 and to overlay the same OSM-WMS basemap as <code>Table_Visualization_Map</code>. If omitted, the SRID is read from the table geometry; when no SRID is available the basemap is disabled.',
                min        : 0, max: 1,
                type       : Integer.class
        ],
        tableDEM     : [
                name       : 'DEM table (optional)',
                title      : 'DEM table (optional)',
                description: 'Optional terrain table. The altitudes stored in <code>PATH</code> are used first; this table is accepted for scene enrichment but is not required.',
                min        : 0, max: 1,
                type       : String.class
        ],
        tableBuilding: [
                name       : 'Buildings table (optional)',
                title      : 'Buildings table (optional)',
                description: 'Optional building footprint table. Footprints intersecting the ray bounding box are drawn in the 2D view.',
                min        : 0, max: 1,
                type       : String.class
        ],
        tableGrounds : [
                name       : 'Grounds table (optional)',
                title      : 'Grounds table (optional)',
                description: 'Optional ground coefficient table. Coefficients stored in <code>PATH</code> are used first.',
                min        : 0, max: 1,
                type       : String.class
        ],
        tableReceivers: [
                name       : 'Receivers table (optional)',
                title      : 'Receivers table (optional)',
                description: 'Optional receiver point table used to place the receiver marker. Without it, the last point of the ray is used.',
                min        : 0, max: 1,
                type       : String.class
        ],
        tableSources : [
                name       : 'Sources table (optional)',
                title      : 'Sources table (optional)',
                description: 'Optional source point table used to place the source marker. Without it, the first point of the ray is used.',
                min        : 0, max: 1,
                type       : String.class
        ],
        receiver     : [
                name       : 'Receiver identifier (optional)',
                title      : 'Receiver identifier (optional)',
                description: 'Receiver to display. If omitted, the receiver with the highest sum of LEQ energy is selected.',
                min        : 0, max: 1,
                type       : Integer.class
        ],
        meteo        : [
                name       : 'Meteo (optional)',
                title      : 'Meteo (optional)',
                description: 'Meteorological condition filter (for example <code>homogeneous</code> or <code>favorable</code>). If omitted, the first distinct value found in the table is used and displayed.',
                min        : 0, max: 1,
                type       : String.class
        ],
        period       : [
                name       : 'Period (optional)',
                title      : 'Period (optional)',
                description: 'Period filter (for example <code>D</code>, <code>E</code> or <code>N</code>). If omitted, the first distinct value found in the table is used and displayed.',
                min        : 0, max: 1,
                type       : String.class
        ],
        maxRays      : [
                name       : 'Maximum rays (optional)',
                title      : 'Maximum rays (optional)',
                description: 'Number of rays kept by descending LEQ before adding the direct ray. Default <code>50</code>, bounded between 1 and 50.',
                default    : 50,
                min        : 0, max: 1,
                type       : Integer.class
        ]
]

outputs = [
        result: [
                name       : 'Result output string',
                title      : 'Result output string',
                description: 'HTML fragment of the propagation ray viewer. This output cannot be linked to another block.',
                type       : String.class
        ]
]

// ---------------------------------------------------------------------------
// Small helpers
// ---------------------------------------------------------------------------

static Double toFinite(Object value) {
    if (value == null) {
        return null
    }
    if (value instanceof Number) {
        double d = ((Number) value).doubleValue()
        return Double.isFinite(d) ? d : null
    }
    if (value instanceof CharSequence) {
        String text = value.toString().trim()
        if (text.isEmpty()) {
            return null
        }
        try {
            double d = Double.parseDouble(text)
            return Double.isFinite(d) ? d : null
        } catch (NumberFormatException ignored) {
            return null
        }
    }
    return null
}

static Double firstFinite(Object value) {
    if (value instanceof List) {
        for (Object item : (List) value) {
            Double number = toFinite(item)
            if (number != null) {
                return number
            }
        }
        return null
    }
    return toFinite(value)
}

static Integer toInteger(Object value, Integer defaultValue = null) {
    if (value == null) {
        return defaultValue
    }
    if (value instanceof Number) {
        return ((Number) value).intValue()
    }
    try {
        return Integer.parseInt(value.toString().trim())
    } catch (NumberFormatException ignored) {
        return defaultValue
    }
}

static String esc(Object value) {
    if (value == null) {
        return ''
    }
    return value.toString()
            .replace('&', '&amp;')
            .replace('<', '&lt;')
            .replace('>', '&gt;')
            .replace('"', '&quot;')
            .replace("'", '&#39;')
}

static String fmt(Double value, int digits = 1) {
    if (value == null || !Double.isFinite(value)) {
        return 'n/a'
    }
    return String.format(Locale.ROOT, "%.${digits}f", value)
}

static String svgNumber(Double value) {
    if (value == null || !Double.isFinite(value)) {
        return '0'
    }
    return String.format(Locale.ROOT, '%.2f', value)
}

static String jsonSafe(String json) {
    return json
            .replace('&', '\\u0026')
            .replace('<', '\\u003c')
            .replace('>', '\\u003e')
            .replace('\u2028', '\\u2028')
            .replace('\u2029', '\\u2029')
}

static String typeColor(String type) {
    switch (type) {
        case 'direct':
            return '#1f77b4'
        case 'reflected':
            return '#d62728'
        case 'diffracted':
            return '#2ca02c'
        case 'mixed':
            return '#9467bd'
        default:
            return '#52606d'
    }
}

static String rayType(int reflections, int diffractions) {
    if (reflections > 0 && diffractions > 0) {
        return 'mixed'
    }
    if (reflections > 0) {
        return 'reflected'
    }
    if (diffractions > 0) {
        return 'diffracted'
    }
    return 'direct'
}

static List<Object> asList(Object value) {
    return value instanceof List ? (List<Object>) value : Collections.emptyList()
}

static Map<Object, Object> asMap(Object value) {
    return value instanceof Map ? (Map<Object, Object>) value : Collections.emptyMap()
}

static Object mapValue(Object value, String key) {
    return value instanceof Map ? ((Map) value).get(key) : null
}

static String textOf(Object value) {
    return value == null ? null : value.toString()
}

static List<Double> coordinateOf(Object value) {
    Object coordinate = mapValue(value, 'coordinate')
    if (coordinate == null) {
        coordinate = value
    }
    if (!(coordinate instanceof Map)) {
        return null
    }
    Double x = toFinite(mapValue(coordinate, 'x'))
    Double y = toFinite(mapValue(coordinate, 'y'))
    Double z = toFinite(mapValue(coordinate, 'z'))
    if (x == null || y == null) {
        return null
    }
    return [x, y, z]
}

static String readPath(Object value) {
    if (value == null) {
        return null
    }
    if (value instanceof Clob) {
        Clob clob = (Clob) value
        return clob.getSubString(1L, (int) clob.length())
    }
    if (value instanceof byte[]) {
        return new String((byte[]) value, java.nio.charset.StandardCharsets.UTF_8)
    }
    return value.toString()
}

static boolean isReflectionType(String type) {
    return type != null && type.toUpperCase(Locale.ROOT).contains('REFL')
}

static boolean isDiffractionType(String type) {
    if (type == null) {
        return false
    }
    String upper = type.toUpperCase(Locale.ROOT)
    return upper.contains('DIF') || upper.contains('VEDGE') || upper.contains('DIFFRACTION')
}

static boolean isSourceOrReceiverType(String type) {
    if (type == null) {
        return false
    }
    String upper = type.toUpperCase(Locale.ROOT)
    return upper == 'SRCE' || upper == 'RECV' || upper == 'SOURCE' || upper == 'RECEIVER'
}

static String nodeLetter(String type) {
    if (isReflectionType(type)) {
        return 'R'
    }
    if (type != null && type.toUpperCase(Locale.ROOT).contains('RCRIT')) {
        return 'RC'
    }
    if (type != null && type.toUpperCase(Locale.ROOT).contains('DIFH')) {
        return 'DH'
    }
    if (type != null && (type.toUpperCase(Locale.ROOT).contains('DIFV') || type.toUpperCase(Locale.ROOT).contains('VEDGE'))) {
        return 'DV'
    }
    if (isDiffractionType(type)) {
        return 'D'
    }
    return 'O'
}

static String nodeColor(String type) {
    switch (nodeLetter(type)) {
        case 'R':
            return '#d62728'
        case 'DH':
            return '#2ca02c'
        case 'DV':
            return '#17becf'
        case 'D':
            return '#2ca02c'
        case 'RC':
            return '#ff7f0e'
        default:
            return '#9467bd'
    }
}

static String groundCoefficientColor(Double coefficient) {
    if (coefficient == null || !Double.isFinite(coefficient)) {
        return 'rgba(150,150,150,.45)'
    }
    double t = Math.max(0d, Math.min(1d, coefficient))
    int r = (int) Math.round(33 + (77 - 33) * t)
    int g = (int) Math.round(102 + (172 - 102) * t)
    int b = (int) Math.round(172 + (38 - 172) * t)
    return "rgb(${r},${g},${b})"
}

static int compareIds(Object left, Object right) {
    if (left == null && right == null) {
        return 0
    }
    if (left == null) {
        return 1
    }
    if (right == null) {
        return -1
    }
    String leftText = left.toString()
    String rightText = right.toString()
    try {
        return new BigDecimal(leftText).compareTo(new BigDecimal(rightText))
    } catch (Exception ignored) {
        return leftText <=> rightText
    }
}

static int compareRays(Map left, Map right) {
    Double leftLeq = toFinite(left.leq)
    Double rightLeq = toFinite(right.leq)
    if (leftLeq == null && rightLeq == null) {
        int idComparison = compareIds(left.source, right.source)
        return idComparison != 0 ? idComparison : ((Integer) left.index <=> (Integer) right.index)
    }
    if (leftLeq == null) {
        return 1
    }
    if (rightLeq == null) {
        return -1
    }
    int leqComparison = Double.compare(rightLeq, leftLeq)
    if (leqComparison != 0) {
        return leqComparison
    }
    int idComparison = compareIds(left.source, right.source)
    return idComparison != 0 ? idComparison : ((Integer) left.index <=> (Integer) right.index)
}

static double cumulativeDistance(List<Double> first, List<Double> second) {
    if (first == null || second == null) {
        return 0d
    }
    double dx = (first[0] ?: 0d) - (second[0] ?: 0d)
    double dy = (first[1] ?: 0d) - (second[1] ?: 0d)
    return Math.hypot(dx, dy)
}

static List<Double> cumulativeDistances(List<List<Double>> coordinates) {
    List<Double> distances = new ArrayList<>()
    double distance = 0d
    for (int i = 0; i < coordinates.size(); i++) {
        if (i > 0) {
            distance += cumulativeDistance(coordinates[i], coordinates[i - 1])
        }
        distances.add(distance)
    }
    return distances
}

static List<Integer> sampleIndices(int size, int maximum) {
    if (size <= maximum) {
        return (0..<size).toList()
    }
    List<Integer> indices = new ArrayList<>()
    double step = (size - 1) / (double) (maximum - 1)
    for (int i = 0; i < maximum; i++) {
        int index = (int) Math.round(i * step)
        if (indices.isEmpty() || indices[indices.size() - 1] != index) {
            indices.add(index)
        }
    }
    if (indices[indices.size() - 1] != size - 1) {
        indices.add(size - 1)
    }
    return indices
}

static List<List<Double>> sampleCoordinates(List<List<Double>> coordinates, int maximum) {
    if (coordinates.size() <= maximum) {
        return coordinates
    }
    List<List<Double>> sampled = new ArrayList<>()
    for (Integer index : sampleIndices(coordinates.size(), maximum)) {
        sampled.add(coordinates[index])
    }
    return sampled
}

static double distanceAlongPolyline(List<List<Double>> coordinates, Double x, Double y) {
    if (coordinates == null || coordinates.isEmpty() || x == null || y == null) {
        return 0d
    }
    if (coordinates.size() == 1) {
        return 0d
    }
    List<Double> distances = cumulativeDistances(coordinates)
    double bestDistance = Double.POSITIVE_INFINITY
    double bestAlong = 0d
    for (int i = 1; i < coordinates.size(); i++) {
        double x1 = coordinates[i - 1][0] ?: 0d
        double y1 = coordinates[i - 1][1] ?: 0d
        double x2 = coordinates[i][0] ?: 0d
        double y2 = coordinates[i][1] ?: 0d
        double dx = x2 - x1
        double dy = y2 - y1
        double lengthSquared = dx * dx + dy * dy
        double t = lengthSquared == 0d ? 0d : ((x - x1) * dx + (y - y1) * dy) / lengthSquared
        t = Math.max(0d, Math.min(1d, t))
        double px = x1 + t * dx
        double py = y1 + t * dy
        double distance = Math.hypot(x - px, y - py)
        if (distance < bestDistance) {
            bestDistance = distance
            bestAlong = distances[i - 1] + t * (distances[i] - distances[i - 1])
        }
    }
    return bestAlong
}

static Double valueAtDistance(List<Double> distances, List<Double> values, double distance) {
    if (distances == null || values == null || distances.isEmpty()) {
        return null
    }
    if (distance <= distances[0]) {
        return values[0]
    }
    for (int i = 1; i < distances.size(); i++) {
        if (distance <= distances[i]) {
            Double previous = values[i - 1]
            Double current = values[i]
            if (previous == null && current == null) {
                return null
            }
            if (previous == null) {
                return current
            }
            if (current == null) {
                return previous
            }
            double span = distances[i] - distances[i - 1]
            double t = span == 0d ? 0d : (distance - distances[i - 1]) / span
            return previous + t * (current - previous)
        }
    }
    return values[values.size() - 1]
}

static double niceStep(double span, int target) {
    if (!Double.isFinite(span) || span <= 0d) {
        return 1d
    }
    double raw = span / Math.max(1, target)
    double magnitude = Math.pow(10d, Math.floor(Math.log10(raw)))
    double normalized = raw / magnitude
    double step
    if (normalized <= 1d) {
        step = 1d
    } else if (normalized <= 2d) {
        step = 2d
    } else if (normalized <= 5d) {
        step = 5d
    } else {
        step = 10d
    }
    return step * magnitude
}

static List<Double> expandBbox(List<Double> bbox, double ratio, double minimum) {
    if (bbox == null) {
        return null
    }
    double deltaX = Math.max((bbox[2] - bbox[0]) * ratio, minimum)
    double deltaY = Math.max((bbox[3] - bbox[1]) * ratio, minimum)
    return [bbox[0] - deltaX, bbox[1] - deltaY, bbox[2] + deltaX, bbox[3] + deltaY]
}

static Integer detectSrid(Connection connection, DBTypes dbType, List<String> tableInputs, Logger logger) {
    for (String tableInput : tableInputs) {
        if (tableInput == null || tableInput.trim().isEmpty()) {
            continue
        }
        try {
            TableLocation tableLocation = TableLocation.parse(tableInput, dbType)
            if (!JDBCUtilities.tableExists(connection, tableLocation)) {
                continue
            }
            List<String> geometryColumns = GeometryTableUtilities.getGeometryColumnNames(connection, tableLocation)
            if (geometryColumns.isEmpty()) {
                continue
            }
            int srid = GeometryTableUtilities.getSRID(connection, tableLocation)
            if (srid > 0) {
                return srid
            }
        } catch (Exception exception) {
            logger.debug("Unable to read SRID from {}: {}", tableInput, exception.message)
        }
    }
    return null
}

static MathTransform createWgs84Transform(Integer srid, Logger logger) {
    if (srid == null || srid <= 0 || srid == 4326) {
        return null
    }
    try {
        def sourceCrs = CRS.decode("EPSG:${srid}", true)
        def targetCrs = CRS.decode('EPSG:4326', true)
        return CRS.findMathTransform(sourceCrs, targetCrs, true)
    } catch (Exception exception) {
        logger.warn("Unable to build the EPSG:{} to EPSG:4326 transform: {}", srid, exception.message)
        return null
    }
}

static List<Double> transformCoordinate(List<Double> coordinate, MathTransform transform) {
    if (transform == null || coordinate == null) {
        return coordinate
    }
    Coordinate source = new Coordinate(coordinate[0], coordinate[1])
    Coordinate target = JTS.transform(source, null, transform)
    if (target == null || !Double.isFinite(target.x) || !Double.isFinite(target.y)) {
        throw new IllegalArgumentException("Invalid projected coordinate")
    }
    return [target.x, target.y, coordinate[2]]
}

static List<List<Double>> transformCoordinates(List<List<Double>> coordinates, MathTransform transform) {
    if (transform == null || coordinates == null) {
        return coordinates
    }
    return coordinates.collect { List<Double> coordinate -> transformCoordinate(coordinate, transform) }
}

static String basemapUrl(List<Double> bbox) {
    return String.format(Locale.ROOT,
            'https://ows.terrestris.de/osm/service?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&LAYERS=OSM-WMS&STYLES=&SRS=EPSG:4326&BBOX=%.8f,%.8f,%.8f,%.8f&WIDTH=900&HEIGHT=500&FORMAT=image/png&TRANSPARENT=false',
            bbox[0], bbox[1], bbox[2], bbox[3])
}

static String groundFillColor(Double coefficient) {
    if (coefficient == null || !Double.isFinite(coefficient)) {
        return '#9e9e9e'
    }
    return groundCoefficientColor(coefficient)
}

// ---------------------------------------------------------------------------
// PATH extraction
// ---------------------------------------------------------------------------

static Map parseRay(Object receiver, Object source, Object leq, Object meteo, Object period, Object pathValue, int index) {
    Map ray = [
            index             : index,
            receiver          : receiver,
            source            : source,
            leq               : toFinite(leq),
            meteo             : meteo == null ? null : meteo.toString(),
            period            : period == null ? null : period.toString(),
            valid             : false,
            error             : null,
            aDiv              : null,
            aAtm              : null,
            aDif              : null,
            aRef              : null,
            aGlobal           : null,
            ground            : null,
            cutCoords         : [],
            cutZGround        : [],
            groundCoefficients: [],
            cutTypes          : [],
            rayCoords         : [],
            nodes             : [],
            nReflections      : 0,
            nDiffractions     : 0,
            direct            : false,
            type              : 'direct',
            length            : null
    ]

    String pathText = readPath(pathValue)
    if (pathText == null || pathText.trim().isEmpty()) {
        ray.error = 'empty PATH'
        return ray
    }

    Object parsed
    try {
        parsed = new JsonSlurper().parseText(pathText)
    } catch (Exception exception) {
        ray.error = 'invalid JSON in PATH: ' + exception.message
        return ray
    }
    if (!(parsed instanceof Map)) {
        ray.error = 'PATH is not a JSON object'
        return ray
    }
    Map path = (Map) parsed

    List<Object> cutPoints = asList(mapValue(mapValue(path, 'cutProfile'), 'cutPoints'))
    for (Object cutPoint : cutPoints) {
        List<Double> coordinate = coordinateOf(cutPoint)
        if (coordinate == null) {
            continue
        }
        ray.cutCoords.add(coordinate)
        ray.cutZGround.add(toFinite(mapValue(cutPoint, 'zGround')))
        ray.groundCoefficients.add(toFinite(mapValue(cutPoint, 'groundCoefficient')))
        String cutType = textOf(mapValue(cutPoint, 'type'))
        ray.cutTypes.add(cutType)
    }

    List<List<Double>> rayCoords = new ArrayList<>()
    Object lineCoordinates = mapValue(mapValue(path, 'lineString'), 'coordinates')
    for (Object coordinateValue : asList(lineCoordinates)) {
        if (!(coordinateValue instanceof List) || ((List) coordinateValue).size() < 2) {
            continue
        }
        List coordinate = (List) coordinateValue
        Double x = toFinite(coordinate[0])
        Double y = toFinite(coordinate[1])
        Double z = coordinate.size() > 2 ? toFinite(coordinate[2]) : null
        if (x != null && y != null) {
            rayCoords.add([x, y, z])
        }
    }

    Object propagationPath = mapValue(path, 'propagationPath')
    List<Object> pointList = asList(mapValue(propagationPath, 'pointList'))
    if (pointList.isEmpty()) {
        pointList = asList(mapValue(path, 'pointList'))
    }
    if (rayCoords.isEmpty()) {
        for (Object point : pointList) {
            List<Double> coordinate = coordinateOf(point)
            if (coordinate != null) {
                rayCoords.add([coordinate[0], coordinate[1], coordinate[2]])
            }
        }
    }
    if (rayCoords.isEmpty()) {
        for (List<Double> coordinate : ray.cutCoords) {
            rayCoords.add([coordinate[0], coordinate[1], coordinate[2]])
        }
    }

    if (rayCoords.isEmpty()) {
        ray.error = 'no usable geometry in PATH'
        return ray
    }
    if (ray.cutCoords.isEmpty()) {
        for (List<Double> coordinate : rayCoords) {
            ray.cutCoords.add([coordinate[0], coordinate[1], coordinate[2]])
            ray.cutZGround.add(null)
            ray.groundCoefficients.add(null)
            ray.cutTypes.add(null)
        }
    }

    ray.rayCoords = rayCoords
    ray.aDiv = firstFinite(mapValue(path, 'aDiv'))
    ray.aAtm = firstFinite(mapValue(path, 'aAtm'))
    ray.aDif = firstFinite(mapValue(path, 'aDif'))
    ray.aRef = firstFinite(mapValue(path, 'aRef'))
    ray.aGlobal = firstFinite(mapValue(path, 'aGlobal'))
    Object groundAttenuation = mapValue(path, 'groundAttenuation')
    if (groundAttenuation instanceof Map) {
        ray.ground = firstFinite(mapValue(groundAttenuation, 'aGround'))
    } else {
        ray.ground = firstFinite(groundAttenuation)
    }

    int pointReflections = 0
    int pointDiffractions = 0
    boolean onlySourceReceiver = !pointList.isEmpty()
    for (Object point : pointList) {
        String pointType = textOf(mapValue(point, 'type'))
        if (!isSourceOrReceiverType(pointType)) {
            onlySourceReceiver = false
        }
        if (isReflectionType(pointType)) {
            pointReflections++
        } else if (isDiffractionType(pointType)) {
            pointDiffractions++
        }
    }

    int cutReflections = ray.cutTypes.count { String type -> isReflectionType(type) }
    int cutDiffractions = ray.cutTypes.count { String type -> isDiffractionType(type) }
    ray.nReflections = Math.max(pointReflections, cutReflections)
    ray.nDiffractions = Math.max(pointDiffractions, cutDiffractions)
    ray.direct = onlySourceReceiver || (ray.nReflections == 0 && ray.nDiffractions == 0)
    ray.type = rayType(ray.nReflections, ray.nDiffractions)

    List<List<Object>> nodes = new ArrayList<>()
    for (Object point : pointList) {
        String pointType = textOf(mapValue(point, 'type'))
        if (!isReflectionType(pointType) && !isDiffractionType(pointType)) {
            continue
        }
        List<Double> coordinate = coordinateOf(point)
        if (coordinate == null) {
            continue
        }
        nodes.add([pointType, coordinate[0], coordinate[1], coordinate[2]])
    }
    if (nodes.isEmpty()) {
        for (int i = 0; i < ray.cutCoords.size(); i++) {
            String cutType = ray.cutTypes[i]
            if (!isReflectionType(cutType) && !isDiffractionType(cutType)) {
                continue
            }
            List<Double> coordinate = ray.cutCoords[i]
            nodes.add([cutType, coordinate[0], coordinate[1], coordinate[2]])
        }
    }
    nodes.sort { List<Object> left, List<Object> right ->
        double leftDistance = distanceAlongPolyline(ray.rayCoords, toFinite(left[1]), toFinite(left[2]))
        double rightDistance = distanceAlongPolyline(ray.rayCoords, toFinite(right[1]), toFinite(right[2]))
        return leftDistance <=> rightDistance
    }
    ray.nodes = nodes

    double length = 0d
    for (int i = 1; i < ray.rayCoords.size(); i++) {
        List<Double> previous = ray.rayCoords[i - 1]
        List<Double> current = ray.rayCoords[i]
        double dx = (current[0] ?: 0d) - (previous[0] ?: 0d)
        double dy = (current[1] ?: 0d) - (previous[1] ?: 0d)
        double dz = (current[2] ?: 0d) - (previous[2] ?: 0d)
        length += Math.sqrt(dx * dx + dy * dy + dz * dz)
    }
    ray.length = length
    ray.valid = true
    return ray
}

// ---------------------------------------------------------------------------
// Optional geometry tables
// ---------------------------------------------------------------------------

static Geometry asGeometry(Object value) {
    if (value instanceof Geometry) {
        return (Geometry) value
    }
    if (value instanceof byte[]) {
        try {
            return new WKBReader().read((byte[]) value)
        } catch (Exception ignored) {
            return null
        }
    }
    if (value instanceof CharSequence) {
        try {
            return new WKTReader().read(value.toString())
        } catch (Exception ignored) {
            return null
        }
    }
    return null
}

static List<Double> loadPoint(Sql sql, Connection connection, String tableInput, Object identifier,
                              List<String> identifierNames, DBTypes dbType, Logger logger) {
    if (tableInput == null || tableInput.trim().isEmpty() || identifier == null) {
        return null
    }
    try {
        TableLocation tableLocation = TableLocation.parse(tableInput, dbType)
        if (!JDBCUtilities.tableExists(connection, tableLocation)) {
            logger.warn("Optional table {} was not found", tableInput)
            return null
        }
        def geometryInfo = GeometryTableUtilities.getFirstGeometryColumnNameAndIndex(connection, tableLocation)
        if (geometryInfo == null) {
            logger.warn("Optional table {} has no geometry column", tableInput)
            return null
        }
        String geometryColumn = geometryInfo.first().toString()
        List<String> columns = JDBCUtilities.getColumnNames(connection, tableLocation)
                .collect { String column -> column.toUpperCase(Locale.ROOT) }
        String identifierColumn = identifierNames.find { String candidate ->
            columns.contains(candidate.toUpperCase(Locale.ROOT))
        }
        if (identifierColumn == null) {
            logger.warn("Optional table {} has no known identifier column", tableInput)
            return null
        }
        String sqlTable = TableLocation.capsIdentifier(tableInput, dbType)
        String pointQuery = "SELECT ${geometryColumn} AS \"G\" FROM ${sqlTable} WHERE ${identifierColumn} = ? LIMIT 1".toString()
        def rows = sql.rows(pointQuery, [identifier])
        if (rows.isEmpty()) {
            return null
        }
        Geometry geometry = asGeometry(rows[0].G)
        if (geometry == null) {
            return null
        }
        if (geometry instanceof Point) {
            return [geometry.coordinate.x, geometry.coordinate.y, toFinite(geometry.coordinate.z)]
        }
        if (geometry instanceof MultiPoint && geometry.getNumGeometries() > 0) {
            Geometry point = geometry.getGeometryN(0)
            return [point.coordinate.x, point.coordinate.y, toFinite(point.coordinate.z)]
        }
        if (!geometry.isEmpty()) {
            return [geometry.centroid.coordinate.x, geometry.centroid.coordinate.y, toFinite(geometry.centroid.coordinate.z)]
        }
    } catch (Exception exception) {
        logger.warn("Unable to read optional point table {}: {}", tableInput, exception.message)
    }
    return null
}

static List<List<List<Double>>> polygonRings(Geometry geometry, List<Double> bbox, int maximumPoints) {
    List<List<List<Double>>> rings = new ArrayList<>()
    if (geometry == null) {
        return rings
    }
    List<Geometry> polygons = new ArrayList<>()
    if (geometry instanceof Polygon) {
        polygons.add(geometry)
    } else if (geometry instanceof MultiPolygon) {
        for (int i = 0; i < geometry.getNumGeometries(); i++) {
            polygons.add(geometry.getGeometryN(i))
        }
    }
    for (Geometry polygon : polygons) {
        if (!(polygon instanceof Polygon)) {
            continue
        }
        LineString exterior = ((Polygon) polygon).exteriorRing
        if (exterior == null || exterior.numPoints < 4) {
            continue
        }
        List<List<Double>> ring = new ArrayList<>()
        double minX = Double.POSITIVE_INFINITY
        double minY = Double.POSITIVE_INFINITY
        double maxX = Double.NEGATIVE_INFINITY
        double maxY = Double.NEGATIVE_INFINITY
        for (int i = 0; i < exterior.numPoints; i++) {
            double x = exterior.getCoordinateN(i).x
            double y = exterior.getCoordinateN(i).y
            ring.add([x, y, null])
            minX = Math.min(minX, x)
            minY = Math.min(minY, y)
            maxX = Math.max(maxX, x)
            maxY = Math.max(maxY, y)
        }
        boolean intersects = bbox == null || !(maxX < bbox[0] || minX > bbox[2] || maxY < bbox[1] || minY > bbox[3])
        if (intersects) {
            rings.add(sampleCoordinates(ring, maximumPoints))
        }
    }
    return rings
}

static List<List<List<Double>>> loadBuildingRings(Sql sql, Connection connection, String tableInput,
                                                  DBTypes dbType, List<Double> bbox, Logger logger) {
    List<List<List<Double>>> rings = new ArrayList<>()
    if (tableInput == null || tableInput.trim().isEmpty()) {
        return rings
    }
    try {
        TableLocation tableLocation = TableLocation.parse(tableInput, dbType)
        if (!JDBCUtilities.tableExists(connection, tableLocation)) {
            logger.warn("Optional table {} was not found", tableInput)
            return rings
        }
        def geometryInfo = GeometryTableUtilities.getFirstGeometryColumnNameAndIndex(connection, tableLocation)
        if (geometryInfo == null) {
            logger.warn("Optional table {} has no geometry column", tableInput)
            return rings
        }
        String geometryColumn = geometryInfo.first().toString()
        String sqlTable = TableLocation.capsIdentifier(tableInput, dbType)
        String buildingQuery = "SELECT ${geometryColumn} AS \"G\" FROM ${sqlTable} LIMIT 5000".toString()
        sql.eachRow(buildingQuery) { row ->
            if (rings.size() >= 800) {
                return
            }
            Geometry geometry = asGeometry(row.G)
            for (List<List<Double>> ring : polygonRings(geometry, bbox, 120)) {
                if (rings.size() >= 800) {
                    return
                }
                rings.add(ring)
            }
        }
    } catch (Exception exception) {
        logger.warn("Unable to read optional buildings table {}: {}", tableInput, exception.message)
    }
    return rings
}

static List<Map> loadGroundAreas(Sql sql, Connection connection, String tableInput,
                                 DBTypes dbType, List<Double> bbox, Logger logger) {
    List<Map> areas = new ArrayList<>()
    if (tableInput == null || tableInput.trim().isEmpty()) {
        return areas
    }
    try {
        TableLocation tableLocation = TableLocation.parse(tableInput, dbType)
        if (!JDBCUtilities.tableExists(connection, tableLocation)) {
            logger.warn("Optional table {} was not found", tableInput)
            return areas
        }
        def geometryInfo = GeometryTableUtilities.getFirstGeometryColumnNameAndIndex(connection, tableLocation)
        if (geometryInfo == null) {
            logger.warn("Optional table {} has no geometry column", tableInput)
            return areas
        }
        String geometryColumn = geometryInfo.first().toString()
        List<String> columns = JDBCUtilities.getColumnNames(connection, tableLocation)
                .collect { String column -> column.toUpperCase(Locale.ROOT) }
        String coefficientColumn = ['G', 'GROUND_COEFFICIENT', 'GROUNDCOEFFICIENT', 'GROUND_ABSORPTION',
                                    'GROUNDABSORPTION', 'GROUND_COEF', 'GROUNDCOEFF', 'GROUND_COEFF',
                                    'COEFFICIENT', 'COEFF'].find { String candidate -> columns.contains(candidate) }
        String sqlTable = TableLocation.capsIdentifier(tableInput, dbType)
        String select = "SELECT ${geometryColumn} AS \"G\"" +
                (coefficientColumn == null ? '' : ", ${coefficientColumn} AS \"C\"") +
                " FROM ${sqlTable} LIMIT 5000"
        sql.eachRow(select.toString()) { row ->
            if (areas.size() >= 400) {
                return
            }
            Geometry geometry = asGeometry(row.G)
            Double coefficient = coefficientColumn == null ? null : toFinite(row.C)
            for (List<List<Double>> ring : polygonRings(geometry, bbox, 160)) {
                if (areas.size() >= 400) {
                    return
                }
                areas.add([ring: ring, coefficient: coefficient])
            }
        }
        if (coefficientColumn == null) {
            logger.info("Optional grounds table {} has no recognized coefficient column; ground areas use a neutral fill", tableInput)
        }
    } catch (Exception exception) {
        logger.warn("Unable to read optional grounds table {}: {}", tableInput, exception.message)
    }
    return areas
}

// ---------------------------------------------------------------------------
// HTML rendering
// ---------------------------------------------------------------------------

static String explainStyle() {
    return '''
<style>
.explain-rays { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif; color: #1f2933; line-height: 1.45; }
.explain-rays * { box-sizing: border-box; }
.explain-rays h3 { margin: 0 0 .35rem 0; font-size: 1.25rem; }
.explain-rays h4 { margin: 0 0 .45rem 0; font-size: .95rem; color: #334e68; }
.explain-rays-header { border: 1px solid #d9e2ec; border-radius: 6px; padding: .8rem .9rem; background: #ffffff; margin-bottom: .8rem; }
.explain-rays-kpis { display: flex; flex-wrap: wrap; gap: .45rem; margin: .35rem 0 .45rem 0; }
.explain-rays-kpis span { background: #eef2f7; border-radius: 4px; padding: .15rem .5rem; font-size: .82rem; }
.explain-rays-note { margin: .2rem 0; color: #52606d; font-size: .84rem; }
.explain-rays-warning { margin: .35rem 0 0 0; padding: .45rem .6rem; border-left: 4px solid #f0a202; background: #fff4e5; font-size: .84rem; }
.explain-rays-error { border: 1px solid #e0b4b4; border-left: 4px solid #c62828; background: #fdecec; padding: .7rem .85rem; border-radius: 4px; }
.explain-rays-grid { display: grid; grid-template-columns: minmax(0, 1.35fr) minmax(0, 1fr); gap: .8rem; align-items: start; }
@media (max-width: 960px) { .explain-rays-grid { grid-template-columns: minmax(0, 1fr); } }
.explain-rays-panel { border: 1px solid #d9e2ec; border-radius: 6px; padding: .6rem; background: #ffffff; margin-bottom: .8rem; }
.explain-rays-svg { width: 100%; height: auto; display: block; background: #f8fafc; border-radius: 4px; }
.explain-rays-map-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: .35rem; font-size: .8rem; color: #334e68; }
.explain-rays-map-toolbar label { display: inline-flex; align-items: center; gap: .3rem; cursor: pointer; }
.explain-rays-legend { display: flex; flex-wrap: wrap; gap: .6rem; margin: .5rem 0 0 0; font-size: .8rem; color: #334e68; }
.explain-rays-legend span { display: inline-flex; align-items: center; gap: .3rem; }
.explain-rays-swatch { display: inline-block; width: .8rem; height: .8rem; border-radius: 2px; vertical-align: -0.1rem; }
.explain-rays-line-key { display: inline-block; width: 1.1rem; border-top: 2px dashed #6b7c93; vertical-align: middle; }
.explain-rays-table-wrap { overflow-x: auto; }
.explain-rays-table { width: 100%; border-collapse: collapse; font-size: .8rem; }
.explain-rays-table th, .explain-rays-table td { border-bottom: 1px solid #e4ebf2; padding: .28rem .35rem; text-align: right; white-space: nowrap; }
.explain-rays-table th:first-child, .explain-rays-table td:first-child,
.explain-rays-table th:nth-child(2), .explain-rays-table td:nth-child(2) { text-align: left; }
.explain-rays-table thead th { color: #334e68; background: #f5f8fb; position: sticky; top: 0; }
.explain-rays-table tbody tr { cursor: pointer; }
.explain-rays-table tbody tr:hover { background: #eef6ff; }
.explain-rays-table tbody tr.is-selected { background: #fff3bf; }
.explain-rays-profiles { margin-top: .8rem; }
.explain-rays-profile { border: 1px solid #d9e2ec; border-radius: 5px; background: #ffffff; margin-bottom: .35rem; }
.explain-rays-profile[open] { border-color: #9fb3c8; }
.explain-rays-profile summary { cursor: pointer; padding: .4rem .55rem; font-size: .82rem; color: #243b53; }
.explain-rays-profile-body { padding: 0 .4rem .4rem .4rem; }
.explain-rays-selected { color: #486581; font-size: .82rem; margin-left: .35rem; }
.explain-rays-empty { color: #52606d; font-size: .85rem; padding: .5rem; }
</style>
'''
}

static String explainScript() {
    return '''
<script>
(function () {
  var root = document.getElementById('explain-rays-app');
  if (!root) { return; }
  var rows = Array.prototype.slice.call(root.querySelectorAll('[data-ray-row]'));
  var lines = Array.prototype.slice.call(root.querySelectorAll('polyline[data-ray-index]'));
  var profiles = Array.prototype.slice.call(root.querySelectorAll('details[data-ray-index]'));
  var meta = root.querySelector('[data-explain-rays-selected]');
  var basemapImage = root.querySelector('[data-explain-rays-basemap-image]');
  var basemapToggle = root.querySelector('[data-explain-rays-basemap]');
  if (basemapImage && basemapToggle) {
    basemapToggle.addEventListener('change', function () {
      basemapImage.style.display = basemapToggle.checked ? '' : 'none';
    });
  }
  var data = [];
  var dataNode = document.getElementById('explain-rays-data');
  if (dataNode) {
    try { data = JSON.parse(dataNode.textContent || '[]'); } catch (err) { data = []; }
  }
  function select(index) {
    var key = String(index);
    rows.forEach(function (row) { row.classList.toggle('is-selected', row.getAttribute('data-ray-row') === key); });
    lines.forEach(function (line) {
      var active = line.getAttribute('data-ray-index') === key;
      line.setAttribute('stroke-width', active ? '3.4' : '1.4');
      line.setAttribute('opacity', active ? '1' : '0.65');
    });
    profiles.forEach(function (profile) {
      var active = profile.getAttribute('data-ray-index') === key;
      profile.open = active;
      if (active && profile.scrollIntoView) { profile.scrollIntoView({ block: 'nearest' }); }
    });
    var item = data[index];
    if (meta) {
      meta.textContent = item
        ? ('Ray #' + (index + 1) + ' - ' + item.type + ' - LEQ ' + item.leqText + ' dB - ' + item.lengthText + ' m')
        : '';
    }
  }
  rows.forEach(function (row) {
    row.addEventListener('click', function () { select(Number(row.getAttribute('data-ray-row'))); });
  });
  lines.forEach(function (line) {
    line.addEventListener('click', function (event) {
      event.stopPropagation();
      select(Number(line.getAttribute('data-ray-index')));
    });
  });
  profiles.forEach(function (profile) {
    profile.addEventListener('toggle', function () {
      if (profile.open) { select(Number(profile.getAttribute('data-ray-index'))); }
    });
  });
})();
</script>
'''
}

static String renderError(String message) {
    return '<div class="explain-rays" id="explain-rays-app">' +
            explainStyle() +
            '<div class="explain-rays-header"><h3>Explain propagation rays</h3>' +
            '<p class="explain-rays-note">The viewer could not be built.</p></div>' +
            '<div class="explain-rays-error">' + esc(message) + '</div></div>'
}

static String render2D(List<Map> rays, List<List<List<Double>>> buildings, List<Map> groundAreas,
                       List<Double> receiverPoint, List<Double> sourcePoint, boolean basemapEnabled) {
    double minX = Double.POSITIVE_INFINITY
    double minY = Double.POSITIVE_INFINITY
    double maxX = Double.NEGATIVE_INFINITY
    double maxY = Double.NEGATIVE_INFINITY
    for (Map ray : rays) {
        for (List<Double> coordinate : (List<List<Double>>) ray.mapCoords) {
            if (coordinate[0] != null && coordinate[1] != null) {
                minX = Math.min(minX, coordinate[0])
                minY = Math.min(minY, coordinate[1])
                maxX = Math.max(maxX, coordinate[0])
                maxY = Math.max(maxY, coordinate[1])
            }
        }
    }
    for (List<List<Double>> ring : buildings) {
        for (List<Double> coordinate : ring) {
            minX = Math.min(minX, coordinate[0])
            minY = Math.min(minY, coordinate[1])
            maxX = Math.max(maxX, coordinate[0])
            maxY = Math.max(maxY, coordinate[1])
        }
    }
    if (receiverPoint != null) {
        minX = Math.min(minX, receiverPoint[0])
        minY = Math.min(minY, receiverPoint[1])
        maxX = Math.max(maxX, receiverPoint[0])
        maxY = Math.max(maxY, receiverPoint[1])
    }
    if (sourcePoint != null) {
        minX = Math.min(minX, sourcePoint[0])
        minY = Math.min(minY, sourcePoint[1])
        maxX = Math.max(maxX, sourcePoint[0])
        maxY = Math.max(maxY, sourcePoint[1])
    }
    if (!Double.isFinite(minX) || !Double.isFinite(minY) || !Double.isFinite(maxX) || !Double.isFinite(maxY)) {
        return '<div class="explain-rays-empty">No 2D geometry available.</div>'
    }
    double spanX = maxX - minX
    double spanY = maxY - minY
    if (spanX <= 0d) {
        minX -= 10d
        maxX += 10d
        spanX = maxX - minX
    }
    if (spanY <= 0d) {
        minY -= 10d
        maxY += 10d
        spanY = maxY - minY
    }
    double pad = Math.max(spanX, spanY) * 0.05
    minX -= pad
    maxX += pad
    minY -= pad
    maxY += pad
    spanX = maxX - minX
    spanY = maxY - minY
    double width = 900d
    double height = 500d
    double margin = 16d
    double drawWidth = width - 2d * margin
    double drawHeight = height - 2d * margin
    Closure<Double> mapX = { Double x -> margin + ((x - minX) / spanX) * drawWidth }
    Closure<Double> mapY = { Double y -> height - margin - ((y - minY) / spanY) * drawHeight }

    StringBuilder svg = new StringBuilder()
    svg.append('<svg class="explain-rays-svg" viewBox="0 0 900 500" role="img" aria-label="2D propagation rays" preserveAspectRatio="xMidYMid meet">')
    svg.append('<rect x="0" y="0" width="900" height="500" fill="#f8fafc"></rect>')
    if (basemapEnabled) {
        String wmsUrl = basemapUrl([minX, minY, maxX, maxY])
        svg.append('<image class="explain-rays-basemap" data-explain-rays-basemap-image="true" href="')
                .append(esc(wmsUrl))
                .append('" x="0" y="0" width="900" height="500" preserveAspectRatio="none"></image>')
    }
    for (Map area : groundAreas) {
        List<List<Double>> ring = (List<List<Double>>) area.ring
        if (ring == null || ring.isEmpty()) {
            continue
        }
        StringBuilder points = new StringBuilder()
        for (List<Double> coordinate : ring) {
            if (points.length() > 0) {
                points.append(' ')
            }
            points.append(svgNumber(mapX(coordinate[0]))).append(',').append(svgNumber(mapY(coordinate[1])))
        }
        svg.append('<polygon data-layer="ground" points="').append(points)
                .append('" fill="').append(groundFillColor((Double) area.coefficient))
                .append('" fill-opacity="0.5" stroke="none"></polygon>')
    }
    for (List<List<Double>> ring : buildings) {
        if (ring.isEmpty()) {
            continue
        }
        StringBuilder points = new StringBuilder()
        for (List<Double> coordinate : ring) {
            if (points.length() > 0) {
                points.append(' ')
            }
            points.append(svgNumber(mapX(coordinate[0]))).append(',').append(svgNumber(mapY(coordinate[1])))
        }
        svg.append('<polygon data-layer="building" points="').append(points)
                .append('" fill="#cfd8e3" fill-opacity="1" stroke="#8a9bb0" stroke-width="0.8"></polygon>')
    }
    for (Map ray : rays) {
        List<List<Double>> coordinates = (List<List<Double>>) ray.mapCoords
        if (coordinates.isEmpty()) {
            continue
        }
        StringBuilder points = new StringBuilder()
        for (List<Double> coordinate : coordinates) {
            if (points.length() > 0) {
                points.append(' ')
            }
            points.append(svgNumber(mapX(coordinate[0]))).append(',').append(svgNumber(mapY(coordinate[1])))
        }
        svg.append('<polyline class="explain-rays-ray" data-ray-index="').append(ray.htmlIndex)
                .append('" points="').append(points)
                .append('" fill="none" stroke="').append(typeColor((String) ray.type))
                .append('" stroke-width="1.4" stroke-linejoin="round" stroke-linecap="round" opacity="0.65" style="cursor:pointer">')
                .append('<title>').append(esc("LEQ ${fmt((Double) ray.leq, 1)} dB - ${ray.type} - source ${ray.source}")).append('</title></polyline>')
    }
    for (Map ray : rays) {
        List<List<Double>> coordinates = (List<List<Double>>) ray.mapCoords
        if (coordinates.isEmpty()) {
            continue
        }
        List<Double> sourceCoordinate = coordinates[0]
        if (sourcePoint != null && ((Integer) ray.htmlIndex) == 0) {
            sourceCoordinate = sourcePoint
        }
        svg.append('<circle data-marker="source" cx="').append(svgNumber(mapX(sourceCoordinate[0])))
                .append('" cy="').append(svgNumber(mapY(sourceCoordinate[1])))
                .append('" r="2.4" fill="#ffd400" stroke="#111" stroke-width="0.6">')
                .append('<title>').append(esc("Source ${ray.source}")).append('</title></circle>')
    }
    if (receiverPoint != null) {
        svg.append('<circle data-marker="receiver" cx="').append(svgNumber(mapX(receiverPoint[0])))
                .append('" cy="').append(svgNumber(mapY(receiverPoint[1])))
                .append('" r="5.5" fill="#00e5ff" stroke="#111" stroke-width="1.2"><title>Receiver</title></circle>')
    } else if (!rays.isEmpty() && !((List<List<Double>>) rays[0].mapCoords).isEmpty()) {
        List<List<Double>> coordinates = (List<List<Double>>) rays[0].mapCoords
        List<Double> receiverCoordinate = coordinates[coordinates.size() - 1]
        svg.append('<circle data-marker="receiver" cx="').append(svgNumber(mapX(receiverCoordinate[0])))
                .append('" cy="').append(svgNumber(mapY(receiverCoordinate[1])))
                .append('" r="5.5" fill="#00e5ff" stroke="#111" stroke-width="1.2"><title>Receiver</title></circle>')
    }
    svg.append('</svg>')
    return svg.toString()
}

static String renderProfileSvg(Map ray) {
    List<List<Double>> cutCoords = (List<List<Double>>) ray.displayCutCoords
    List<Double> cutZGround = (List<Double>) ray.displayCutZGround
    List<Double> coefficients = (List<Double>) ray.displayGroundCoefficients
    List<String> cutTypes = (List<String>) ray.displayCutTypes
    List<List<Double>> rayCoords = (List<List<Double>>) ray.displayRayCoords
    if (rayCoords.isEmpty()) {
        return '<div class="explain-rays-empty">No profile available for this ray.</div>'
    }
    List<Double> cutDistances = cumulativeDistances(cutCoords)
    List<Double> rayDistances = cumulativeDistances(rayCoords)
    double maxDistance = 1d
    if (!cutDistances.isEmpty()) {
        maxDistance = Math.max(maxDistance, cutDistances[cutDistances.size() - 1])
    }
    if (!rayDistances.isEmpty()) {
        maxDistance = Math.max(maxDistance, rayDistances[rayDistances.size() - 1])
    }

    List<Double> terrainZ = new ArrayList<>()
    List<Double> terrainDistances = new ArrayList<>()
    for (int i = 0; i < cutCoords.size(); i++) {
        Double z = cutZGround != null && i < cutZGround.size() ? cutZGround[i] : null
        if (z == null && cutCoords[i][2] != null) {
            z = cutCoords[i][2]
        }
        if (z != null) {
            terrainDistances.add(cutDistances[i])
            terrainZ.add(z)
        }
    }

    List<Double> allZ = new ArrayList<>()
    for (List<Double> coordinate : cutCoords) {
        if (coordinate[2] != null) {
            allZ.add(coordinate[2])
        }
    }
    allZ.addAll(terrainZ)
    for (List<Double> coordinate : rayCoords) {
        if (coordinate[2] != null) {
            allZ.add(coordinate[2])
        }
    }
    for (List<Object> node : (List<List<Object>>) ray.nodes) {
        Double z = toFinite(node[3])
        if (z != null) {
            allZ.add(z)
        }
    }
    if (allZ.isEmpty()) {
        return '<div class="explain-rays-empty">No altitude available for this profile.</div>'
    }
    double zMin = allZ.min()
    double zMax = allZ.max()
    if (zMax - zMin < 8d) {
        double middle = (zMax + zMin) / 2d
        zMin = middle - 4d
        zMax = middle + 4d
    }
    double zPadding = (zMax - zMin) * 0.12
    zMin -= zPadding
    zMax += zPadding

    double width = 900d
    double height = 300d
    double left = 58d
    double right = 18d
    double top = 16d
    double bottom = 42d
    double plotWidth = width - left - right
    double plotHeight = height - top - bottom
    Closure<Double> mapX = { Double distance -> left + (distance / maxDistance) * plotWidth }
    Closure<Double> mapY = { Double altitude -> top + (1d - (altitude - zMin) / (zMax - zMin)) * plotHeight }

    StringBuilder svg = new StringBuilder()
    svg.append('<svg class="explain-rays-svg" viewBox="0 0 900 300" role="img" aria-label="Vertical cut profile" preserveAspectRatio="xMidYMid meet">')
    svg.append('<rect x="0" y="0" width="900" height="300" fill="#f8fafc"></rect>')

    double zStep = niceStep(zMax - zMin, 5)
    double zStart = Math.ceil(zMin / zStep) * zStep
    for (double z = zStart; z <= zMax + zStep * 0.01; z += zStep) {
        double labelZ = Math.abs(z) < 0.5d ? 0d : z
        double py = mapY(z)
        svg.append('<line x1="').append(svgNumber(left)).append('" y1="').append(svgNumber(py))
                .append('" x2="').append(svgNumber(left + plotWidth)).append('" y2="').append(svgNumber(py))
                .append('" stroke="#e4ebf2" stroke-width="1"></line>')
        svg.append('<text x="').append(svgNumber(left - 6)).append('" y="').append(svgNumber(py + 3))
                .append('" font-size="10" font-family="monospace" fill="#52606d" text-anchor="end">')
                .append(esc(fmt(labelZ, 0) + ' m')).append('</text>')
    }
    double dStep = niceStep(maxDistance, 5)
    double dStart = 0d
    for (double d = dStart; d <= maxDistance + dStep * 0.01; d += dStep) {
        double px = mapX(d)
        svg.append('<line x1="').append(svgNumber(px)).append('" y1="').append(svgNumber(top))
                .append('" x2="').append(svgNumber(px)).append('" y2="').append(svgNumber(top + plotHeight))
                .append('" stroke="#e4ebf2" stroke-width="1"></line>')
        svg.append('<text x="').append(svgNumber(px)).append('" y="').append(svgNumber(top + plotHeight + 14))
                .append('" font-size="10" font-family="monospace" fill="#52606d" text-anchor="middle">')
                .append(esc(fmt(d, 0))).append('</text>')
    }
    svg.append('<text x="').append(svgNumber(left + plotWidth / 2d)).append('" y="').append(svgNumber(height - 6))
            .append('" font-size="10" fill="#52606d" text-anchor="middle">distance from source (m)</text>')
    svg.append('<text x="12" y="').append(svgNumber(top + plotHeight / 2d))
            .append('" font-size="10" fill="#52606d" text-anchor="middle" transform="rotate(-90 12 ')
            .append(svgNumber(top + plotHeight / 2d)).append(')">altitude (m)</text>')

    if (terrainZ.size() > 1) {
        StringBuilder terrainPoints = new StringBuilder()
        for (int i = 0; i < terrainZ.size(); i++) {
            if (terrainPoints.length() > 0) {
                terrainPoints.append(' ')
            }
            terrainPoints.append(svgNumber(mapX(terrainDistances[i]))).append(',').append(svgNumber(mapY(terrainZ[i])))
        }
        StringBuilder terrainArea = new StringBuilder(terrainPoints)
        terrainArea.append(' ').append(svgNumber(mapX(terrainDistances[terrainDistances.size() - 1]))).append(',').append(svgNumber(top + plotHeight))
        terrainArea.append(' ').append(svgNumber(mapX(terrainDistances[0]))).append(',').append(svgNumber(top + plotHeight))
        svg.append('<polygon points="').append(terrainArea).append('" fill="#e4ebf2" opacity="0.75"></polygon>')
        svg.append('<polyline points="').append(terrainPoints)
                .append('" fill="none" stroke="#8a9bb0" stroke-width="1.1"></polyline>')
    }

    if (cutCoords.size() > 1) {
        StringBuilder points = new StringBuilder()
        for (int i = 0; i < cutCoords.size(); i++) {
            Double z = cutCoords[i][2]
            if (z == null && cutZGround != null && i < cutZGround.size()) {
                z = cutZGround[i]
            }
            if (z == null) {
                continue
            }
            if (points.length() > 0) {
                points.append(' ')
            }
            points.append(svgNumber(mapX(cutDistances[i]))).append(',').append(svgNumber(mapY(z)))
        }
        svg.append('<polyline points="').append(points)
                .append('" fill="none" stroke="#8795a8" stroke-width="1.2" stroke-dasharray="4 3"></polyline>')
    }

    for (int i = 1; i < cutCoords.size() && terrainDistances.size() > 1; i++) {
        Double coefficient = coefficients != null && i < coefficients.size() ? coefficients[i] : null
        if (coefficient == null && coefficients != null && i > 0 && coefficients[i - 1] != null) {
            coefficient = coefficients[i - 1]
        }
        if (coefficient == null) {
            continue
        }
        Double zPrevious = valueAtDistance(terrainDistances, terrainZ, cutDistances[i - 1])
        Double zCurrent = valueAtDistance(terrainDistances, terrainZ, cutDistances[i])
        if (zPrevious == null || zCurrent == null) {
            continue
        }
        svg.append('<line x1="').append(svgNumber(mapX(cutDistances[i - 1]))).append('" y1="').append(svgNumber(mapY(zPrevious)))
                .append('" x2="').append(svgNumber(mapX(cutDistances[i]))).append('" y2="').append(svgNumber(mapY(zCurrent)))
                .append('" stroke="').append(groundCoefficientColor(coefficient))
                .append('" stroke-width="4" opacity="0.85"></line>')
    }

    for (int i = 0; i < cutCoords.size(); i++) {
        String cutType = cutTypes != null && i < cutTypes.size() ? cutTypes[i] : null
        if (cutType == null || !cutType.toUpperCase(Locale.ROOT).contains('WALL')) {
            continue
        }
        Double zTop = cutCoords[i][2]
        Double zGround = cutZGround != null && i < cutZGround.size() ? cutZGround[i] : null
        if (zTop == null) {
            continue
        }
        if (zGround == null || zGround > zTop) {
            zGround = valueAtDistance(terrainDistances, terrainZ, cutDistances[i])
        }
        if (zGround == null) {
            continue
        }
        double px = mapX(cutDistances[i])
        double yTop = mapY(zTop)
        double yBottom = mapY(zGround)
        svg.append('<rect x="').append(svgNumber(px - 2d)).append('" y="').append(svgNumber(Math.min(yTop, yBottom)))
                .append('" width="4" height="').append(svgNumber(Math.max(1d, Math.abs(yBottom - yTop))))
                .append('" fill="#919baa" opacity="0.75"></rect>')
    }

    List<Map> rayPoints = new ArrayList<>()
    for (int i = 0; i < rayCoords.size(); i++) {
        Double z = rayCoords[i][2]
        if (z == null) {
            continue
        }
        rayPoints.add([d: rayDistances[i], z: z])
    }
    if (rayPoints.size() > 1) {
        StringBuilder points = new StringBuilder()
        for (Map point : rayPoints) {
            if (points.length() > 0) {
                points.append(' ')
            }
            points.append(svgNumber(mapX((Double) point.d))).append(',').append(svgNumber(mapY((Double) point.z)))
        }
        svg.append('<polyline points="').append(points)
                .append('" fill="none" stroke="').append(typeColor((String) ray.type))
                .append('" stroke-width="2.2" stroke-linejoin="round" stroke-linecap="round"></polyline>')
    }

    for (List<Object> node : (List<List<Object>>) ray.nodes) {
        String nodeType = node[0] == null ? null : node[0].toString()
        Double nodeX = toFinite(node[1])
        Double nodeY = toFinite(node[2])
        Double nodeZ = toFinite(node[3])
        if (nodeX == null || nodeY == null || nodeZ == null) {
            continue
        }
        double distance = distanceAlongPolyline(cutCoords, nodeX, nodeY)
        double px = mapX(distance)
        double py = mapY(nodeZ)
        Double groundZ = valueAtDistance(terrainDistances, terrainZ, distance)
        if (groundZ != null && groundZ < nodeZ) {
            svg.append('<line x1="').append(svgNumber(px)).append('" y1="').append(svgNumber(mapY(groundZ)))
                    .append('" x2="').append(svgNumber(px)).append('" y2="').append(svgNumber(py))
                    .append('" stroke="').append(nodeColor(nodeType))
                    .append('" stroke-width="1" stroke-dasharray="3 3"></line>')
        }
        svg.append('<circle cx="').append(svgNumber(px)).append('" cy="').append(svgNumber(py))
                .append('" r="4" fill="').append(nodeColor(nodeType))
                .append('" stroke="#111" stroke-width="1"></circle>')
        svg.append('<text x="').append(svgNumber(px)).append('" y="').append(svgNumber(py - 6))
                .append('" font-size="10" font-weight="bold" font-family="monospace" fill="').append(nodeColor(nodeType))
                .append('" text-anchor="middle">').append(esc(nodeLetter(nodeType))).append('</text>')
    }

    List<Map> validRayPoints = rayPoints
    if (!validRayPoints.isEmpty()) {
        Map first = validRayPoints[0]
        Map last = validRayPoints[validRayPoints.size() - 1]
        svg.append('<circle cx="').append(svgNumber(mapX((Double) first.d))).append('" cy="').append(svgNumber(mapY((Double) first.z)))
                .append('" r="4.5" fill="#ffd400" stroke="#111" stroke-width="1"></circle>')
        svg.append('<text x="').append(svgNumber(mapX((Double) first.d) + 7)).append('" y="').append(svgNumber(mapY((Double) first.z) - 5))
                .append('" font-size="10" fill="#334e68" text-anchor="start">source</text>')
        svg.append('<circle cx="').append(svgNumber(mapX((Double) last.d))).append('" cy="').append(svgNumber(mapY((Double) last.z)))
                .append('" r="4.5" fill="#00e5ff" stroke="#111" stroke-width="1"></circle>')
        svg.append('<text x="').append(svgNumber(mapX((Double) last.d) - 7)).append('" y="').append(svgNumber(mapY((Double) last.z) - 5))
                .append('" font-size="10" fill="#334e68" text-anchor="end">receiver</text>')
    }

    svg.append('</svg>')
    return svg.toString()
}

static String renderLegend(boolean hasBuildings, boolean hasGround, boolean hasBasemap) {
    StringBuilder html = new StringBuilder('<div class="explain-rays-legend">')
    html.append('<span><i class="explain-rays-swatch" style="background:#1f77b4"></i>direct</span>')
            .append('<span><i class="explain-rays-swatch" style="background:#d62728"></i>reflected</span>')
            .append('<span><i class="explain-rays-swatch" style="background:#2ca02c"></i>diffracted</span>')
            .append('<span><i class="explain-rays-swatch" style="background:#9467bd"></i>mixed</span>')
            .append('<span><i class="explain-rays-line-key"></i>cut profile (dashed)</span>')
            .append('<span><i class="explain-rays-swatch" style="background:#d62728"></i>R reflection</span>')
            .append('<span><i class="explain-rays-swatch" style="background:#2ca02c"></i>DH horizontal diffraction</span>')
            .append('<span><i class="explain-rays-swatch" style="background:#17becf"></i>DV vertical diffraction</span>')
            .append('<span><i class="explain-rays-swatch" style="background:#ff7f0e"></i>RC Rayleigh criterion</span>')
    if (hasGround) {
        html.append('<span><i class="explain-rays-swatch" style="background:#9e9e9e;opacity:.5"></i>ground (50%)</span>')
    }
    if (hasBuildings) {
        html.append('<span><i class="explain-rays-swatch" style="background:#cfd8e3"></i>buildings (100%)</span>')
    }
    if (hasBasemap) {
        html.append('<span>basemap: OSM-WMS</span>')
    }
    html.append('</div>')
    return html.toString()
}

static String renderTable(List<Map> tableRows) {
    StringBuilder html = new StringBuilder()
    html.append('<div class="explain-rays-panel"><h4>Rays (sorted by LEQ)</h4><div class="explain-rays-table-wrap">')
    html.append('<table class="explain-rays-table"><thead><tr>')
            .append('<th>#</th><th>Type</th><th>LEQ (dB)</th><th>aGlobal</th>')
            .append('<th>aDiv</th><th>aDif</th><th>aRef</th><th>aAtm</th><th>Length (m)</th>')
            .append('</tr></thead><tbody>')
    int rank = 0
    for (Map ray : tableRows) {
        rank++
        html.append('<tr data-ray-row="').append(ray.htmlIndex).append('">')
                .append('<td>').append(rank).append('</td>')
                .append('<td><span class="explain-rays-swatch" style="background:').append(typeColor((String) ray.type)).append('"></span> ')
                .append(esc(ray.type)).append('</td>')
                .append('<td>').append(esc(fmt((Double) ray.leq, 1))).append('</td>')
                .append('<td>').append(esc(fmt((Double) ray.aGlobal, 1))).append('</td>')
                .append('<td>').append(esc(fmt((Double) ray.aDiv, 1))).append('</td>')
                .append('<td>').append(esc(fmt((Double) ray.aDif, 1))).append('</td>')
                .append('<td>').append(esc(fmt((Double) ray.aRef, 1))).append('</td>')
                .append('<td>').append(esc(fmt((Double) ray.aAtm, 1))).append('</td>')
                .append('<td>').append(esc(String.valueOf(Math.round(ray.length == null ? 0d : (Double) ray.length)))).append('</td>')
                .append('</tr>')
    }
    html.append('</tbody></table></div></div>')
    return html.toString()
}

static String renderProfiles(List<Map> rays) {
    StringBuilder html = new StringBuilder()
    html.append('<div class="explain-rays-profiles"><h4>Vertical profiles</h4>')
    for (Map ray : rays) {
        int index = (Integer) ray.htmlIndex
        html.append('<details class="explain-rays-profile" data-ray-index="').append(index).append('"')
                .append(index == 0 ? ' open' : '').append('>')
                .append('<summary><span class="explain-rays-swatch" style="background:').append(typeColor((String) ray.type))
                .append('"></span> Ray #').append(index + 1).append(' &middot; ').append(esc(ray.type))
                .append(' &middot; LEQ ').append(esc(fmt((Double) ray.leq, 1))).append(' dB')
                .append(' &middot; ').append(esc(String.valueOf(Math.round(ray.length == null ? 0d : (Double) ray.length)))).append(' m')
                .append(' &middot; source ').append(esc(ray.source))
                .append('</summary><div class="explain-rays-profile-body">')
                .append(renderProfileSvg(ray))
                .append('</div></details>')
    }
    html.append('</div>')
    return html.toString()
}

// ---------------------------------------------------------------------------
// Main block
// ---------------------------------------------------------------------------

def exec(Connection connection, Map input) {
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")
    logger.info('Start : Explain propagation rays')

    try {
        String tableRaysInput = input['tableRays'] == null ? null : input['tableRays'].toString().trim()
        if (tableRaysInput == null || tableRaysInput.isEmpty()) {
            return [result: renderError('The input tableRays is required.')]
        }

        DBTypes dbType = DBUtils.getDBType(connection)
        TableLocation tableLocation
        try {
            tableLocation = TableLocation.parse(tableRaysInput, dbType)
        } catch (Exception exception) {
            return [result: renderError("Invalid table name for tableRays: ${exception.message}")]
        }
        if (!JDBCUtilities.tableExists(connection, tableLocation)) {
            return [result: renderError("The table ${tableRaysInput} does not exist.")]
        }

        List<String> columns = JDBCUtilities.getColumnNames(connection, tableLocation)
                .collect { String column -> column.toUpperCase(Locale.ROOT) }
        List<String> requiredColumns = ['IDRECEIVER', 'IDSOURCE', 'LEQ', 'METEO', 'PERIOD', 'PATH']
        List<String> missingColumns = requiredColumns.findAll { String column -> !columns.contains(column) }
        if (!missingColumns.isEmpty()) {
            return [result: renderError("The table ${tableRaysInput} is missing required column(s): ${missingColumns.join(', ')}.")]
        }

        String sqlTable = TableLocation.capsIdentifier(tableRaysInput, dbType)
        Sql sql = new Sql(connection)

        String meteoFilter = input['meteo'] == null ? null : input['meteo'].toString().trim()
        if (meteoFilter != null && meteoFilter.isEmpty()) {
            meteoFilter = null
        }
        if (meteoFilter == null) {
            meteoFilter = firstDistinct(sql, sqlTable, 'METEO', columns.contains('PK') ? 'PK' : 'METEO')
        }
        String periodFilter = input['period'] == null ? null : input['period'].toString().trim()
        if (periodFilter != null && periodFilter.isEmpty()) {
            periodFilter = null
        }
        if (periodFilter == null) {
            periodFilter = firstDistinct(sql, sqlTable, 'PERIOD', columns.contains('PK') ? 'PK' : 'PERIOD')
        }

        List<String> whereParts = new ArrayList<>()
        List<Object> whereParameters = new ArrayList<>()
        if (meteoFilter == null) {
            whereParts.add('METEO IS NULL')
        } else {
            whereParts.add('METEO = ?')
            whereParameters.add(meteoFilter)
        }
        if (periodFilter == null) {
            whereParts.add('PERIOD IS NULL')
        } else {
            whereParts.add('PERIOD = ?')
            whereParameters.add(periodFilter)
        }
        String baseWhere = ' WHERE ' + whereParts.join(' AND ')

        Object receiverValue = null
        if (input['receiver'] != null) {
            receiverValue = toInteger(input['receiver'])
        }
        if (receiverValue == null) {
            String aggregateQuery = "SELECT IDRECEIVER AS \"R\", SUM(POWER(10.0, LEQ / 10.0)) AS \"E\" FROM ${sqlTable}${baseWhere} AND LEQ IS NOT NULL GROUP BY IDRECEIVER ORDER BY \"E\" DESC, IDRECEIVER"
            def aggregate = sql.firstRow(aggregateQuery, whereParameters)
            if (aggregate != null && aggregate.R != null) {
                receiverValue = aggregate.R
            } else {
                String firstReceiverQuery = "SELECT IDRECEIVER AS \"R\" FROM ${sqlTable}${baseWhere} ORDER BY IDRECEIVER LIMIT 1".toString()
                def firstReceiver = sql.firstRow(firstReceiverQuery, whereParameters)
                if (firstReceiver != null) {
                    receiverValue = firstReceiver.R
                }
            }
        }
        if (receiverValue == null) {
            return [result: renderError('No receiver found for the selected meteo and period filters.')]
        }

        List<Object> rayParameters = new ArrayList<>(whereParameters)
        rayParameters.add(receiverValue)
        String receiverWhere = baseWhere + ' AND IDRECEIVER = ?'
        String countQuery = "SELECT COUNT(*) AS \"C\" FROM ${sqlTable}${receiverWhere}"
        def countRow = sql.firstRow(countQuery, rayParameters)
        int totalRows = countRow == null ? 0 : ((Number) countRow.C).intValue()

        List<Map> parsedRays = new ArrayList<>()
        int pathIndex = 0
        int invalidPaths = 0
        String rayQuery = "SELECT IDRECEIVER, IDSOURCE, LEQ, METEO, PERIOD, PATH FROM ${sqlTable}${receiverWhere}".toString()
        sql.eachRow(rayQuery, rayParameters) { row ->
            Map ray = parseRay(row.getObject(1), row.getObject(2), row.getObject(3), row.getObject(4), row.getObject(5), row.getObject(6), pathIndex)
            if (ray.valid) {
                parsedRays.add(ray)
            } else {
                invalidPaths++
                logger.warn("Ray {} for receiver {} was ignored: {}", pathIndex, receiverValue, ray.error)
            }
            pathIndex++
        }

        if (parsedRays.isEmpty()) {
            return [result: renderError("No valid propagation ray was found for receiver ${receiverValue}, meteo ${meteoFilter == null ? 'NULL' : meteoFilter} and period ${periodFilter == null ? 'NULL' : periodFilter}.")]
        }

        parsedRays.sort { Map left, Map right -> compareRays(left, right) }

        int maxRays = toInteger(input['maxRays'], 50)
        maxRays = Math.max(1, Math.min(50, maxRays))

        List<Map> selectedRays = new ArrayList<>(parsedRays.subList(0, Math.min(maxRays, parsedRays.size())))

        double totalEnergy = 0d
        for (Map ray : parsedRays) {
            Double rayLeq = toFinite(ray.leq)
            if (rayLeq != null) {
                totalEnergy += Math.pow(10d, rayLeq / 10d)
            }
        }
        Double totalLeq = totalEnergy > 0d ? 10d * Math.log10(totalEnergy) : null

        Map directRay = parsedRays.find { Map ray ->
            (Boolean) ray.direct && ((List) ray.rayCoords).size() >= 2 &&
                    ray.length != null && ((Double) ray.length) > 0d
        }
        boolean directFound = directRay != null
        boolean directAdded = false
        boolean directSkipped = false
        if (directFound && !selectedRays.contains(directRay)) {
            Double directLeq = toFinite(directRay.leq)
            boolean contributes = directLeq != null && totalLeq != null && directLeq >= totalLeq - 20d
            if (contributes) {
                selectedRays.add(directRay)
                directAdded = true
            } else {
                directSkipped = true
            }
        }

        for (int i = 0; i < selectedRays.size(); i++) {
            Map ray = selectedRays[i]
            ray.htmlIndex = i
            ray.displayCoords = sampleCoordinates((List<List<Double>>) ray.rayCoords, 300)
            ray.displayCutCoords = sampleCoordinates((List<List<Double>>) ray.cutCoords, 300)
            List<Integer> cutIndices = sampleIndices(((List<List<Double>>) ray.cutCoords).size(), 300)
            ray.displayCutZGround = cutIndices.collect { Integer index -> ((List<Double>) ray.cutZGround)[index] }
            ray.displayGroundCoefficients = cutIndices.collect { Integer index -> ((List<Double>) ray.groundCoefficients)[index] }
            ray.displayCutTypes = cutIndices.collect { Integer index -> ((List<String>) ray.cutTypes)[index] }
            ray.displayRayCoords = sampleCoordinates((List<List<Double>>) ray.rayCoords, 300)
        }

        List<Map> tableRows = new ArrayList<>(selectedRays)
        tableRows.sort { Map left, Map right -> compareRays(left, right) }

        List<Double> bbox = [Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY]
        for (Map ray : selectedRays) {
            for (List<Double> coordinate : (List<List<Double>>) ray.rayCoords) {
                bbox[0] = Math.min(bbox[0], coordinate[0])
                bbox[1] = Math.min(bbox[1], coordinate[1])
                bbox[2] = Math.max(bbox[2], coordinate[0])
                bbox[3] = Math.max(bbox[3], coordinate[1])
            }
        }

        String tableDEM = input['tableDEM'] == null ? null : input['tableDEM'].toString().trim()
        String tableBuilding = input['tableBuilding'] == null ? null : input['tableBuilding'].toString().trim()
        String tableGrounds = input['tableGrounds'] == null ? null : input['tableGrounds'].toString().trim()
        String tableReceivers = input['tableReceivers'] == null ? null : input['tableReceivers'].toString().trim()
        String tableSources = input['tableSources'] == null ? null : input['tableSources'].toString().trim()
        if (tableDEM != null && !tableDEM.isEmpty()) {
            logger.info('Optional DEM table {} was provided; PATH zGround values remain the primary terrain source', tableDEM)
        }
        if (tableGrounds != null && !tableGrounds.isEmpty()) {
            logger.info('Optional grounds table {} was provided; PATH ground coefficients remain the primary source', tableGrounds)
        }

        List<Double> sceneBbox = expandBbox(bbox, 0.2d, 10d)
        Integer sourceSrid = toInteger(input['inputSRID'])
        if (sourceSrid == null) {
            sourceSrid = detectSrid(connection, dbType,
                    [tableRaysInput, tableBuilding, tableGrounds, tableReceivers, tableSources, tableDEM], logger)
        }
        boolean basemapEnabled = sourceSrid != null && sourceSrid > 0
        MathTransform wgs84Transform = null
        if (basemapEnabled && sourceSrid != 4326) {
            wgs84Transform = createWgs84Transform(sourceSrid, logger)
            if (wgs84Transform == null) {
                basemapEnabled = false
            }
        }
        if (basemapEnabled) {
            logger.info('Basemap enabled with EPSG:{} coordinates', sourceSrid)
        }

        List<List<List<Double>>> buildings = loadBuildingRings(sql, connection, tableBuilding, dbType, sceneBbox, logger)
        List<Map> groundAreas = loadGroundAreas(sql, connection, tableGrounds, dbType, sceneBbox, logger)
        List<Double> receiverPoint = loadPoint(sql, connection, tableReceivers, receiverValue,
                ['IDRECEIVER', 'ID', 'PK'], dbType, logger)
        List<Double> sourcePoint = loadPoint(sql, connection, tableSources,
                selectedRays.isEmpty() ? null : selectedRays[0].source, ['IDSOURCE', 'ID', 'PK'], dbType, logger)

        List<List<List<Double>>> mapBuildings = buildings
        List<Map> mapGroundAreas = groundAreas
        List<Double> mapReceiverPoint = receiverPoint
        List<Double> mapSourcePoint = sourcePoint
        for (Map ray : selectedRays) {
            ray.mapCoords = (List<List<Double>>) ray.displayCoords
        }
        if (basemapEnabled) {
            try {
                mapBuildings = buildings.collect { List<List<Double>> ring -> transformCoordinates(ring, wgs84Transform) }
                mapGroundAreas = groundAreas.collect { Map area ->
                    [ring       : transformCoordinates((List<List<Double>>) area.ring, wgs84Transform),
                     coefficient: area.coefficient]
                }
                mapReceiverPoint = receiverPoint == null ? null : transformCoordinate(receiverPoint, wgs84Transform)
                mapSourcePoint = sourcePoint == null ? null : transformCoordinate(sourcePoint, wgs84Transform)
                for (Map ray : selectedRays) {
                    ray.mapCoords = transformCoordinates((List<List<Double>>) ray.displayCoords, wgs84Transform)
                }
            } catch (Exception projectionException) {
                logger.warn('Unable to project the 2D scene to WGS84; basemap disabled: {}', projectionException.message)
                basemapEnabled = false
                mapBuildings = buildings
                mapGroundAreas = groundAreas
                mapReceiverPoint = receiverPoint
                mapSourcePoint = sourcePoint
                for (Map ray : selectedRays) {
                    ray.mapCoords = (List<List<Double>>) ray.displayCoords
                }
            }
        }

        List<Map<String, Object>> embeddedData = new ArrayList<>()
        for (Map ray : selectedRays) {
            embeddedData.add([
                    index     : ray.htmlIndex,
                    type      : ray.type,
                    leqText   : fmt((Double) ray.leq, 1),
                    lengthText: String.valueOf(Math.round(ray.length == null ? 0d : (Double) ray.length)),
                    source    : ray.source,
                    meteo     : ray.meteo,
                    period    : ray.period
            ])
        }

        StringBuilder html = new StringBuilder()
        html.append('<div class="explain-rays" id="explain-rays-app"')
                .append(' data-table="').append(esc(tableRaysInput)).append('"')
                .append(' data-receiver="').append(esc(receiverValue)).append('"')
                .append(' data-meteo="').append(esc(meteoFilter == null ? '' : meteoFilter)).append('"')
                .append(' data-period="').append(esc(periodFilter == null ? '' : periodFilter)).append('"')
                .append(' data-ray-count="').append(selectedRays.size()).append('"')
                .append(' data-total-rays="').append(totalRows).append('"')
                .append(' data-direct-found="').append(directFound).append('"')
                .append(' data-direct-added="').append(directAdded).append('"')
                .append(' data-direct-skipped="').append(directSkipped).append('"')
                .append(' data-direct-source="').append(esc(directFound ? directRay.source : '')).append('"')
                .append(' data-invalid-paths="').append(invalidPaths).append('"')
                .append(' data-srid="').append(esc(sourceSrid == null ? '' : sourceSrid)).append('"')
                .append(' data-basemap="').append(basemapEnabled).append('">')
        html.append(explainStyle())
        html.append('<div class="explain-rays-header"><h3>Explain propagation rays</h3>')
        html.append('<div class="explain-rays-kpis">')
                .append('<span>Table <b>').append(esc(tableRaysInput)).append('</b></span>')
                .append('<span>Receiver <b>').append(esc(receiverValue)).append('</b></span>')
                .append('<span>Meteo <b>').append(esc(meteoFilter == null ? 'n/a' : meteoFilter)).append('</b></span>')
                .append('<span>Period <b>').append(esc(periodFilter == null ? 'n/a' : periodFilter)).append('</b></span>')
                .append('<span>Rays <b>').append(selectedRays.size()).append('</b> / ').append(totalRows).append('</span>')
                .append('<span class="explain-rays-selected" data-explain-rays-selected></span>')
                .append('</div>')
        html.append('<p class="explain-rays-note">Top ').append(maxRays)
                .append(' by LEQ + direct ray when it contributes (within 20 dB of the total level). ')
                .append('Click a ray in the 2D view or a table row to open its vertical profile.</p>')
        if (directAdded) {
            html.append('<div class="explain-rays-warning">The direct ray (source ').append(esc(directRay.source))
                    .append(') was outside the top ').append(maxRays).append(' by LEQ and was added at the end because it contributes to the receiver level.</div>')
        }
        if (directSkipped) {
            html.append('<div class="explain-rays-warning">The direct ray (source ').append(esc(directRay.source))
                    .append(', LEQ ').append(esc(fmt((Double) directRay.leq, 1))).append(' dB) was not added because it is more than 20 dB below the total level (')
                    .append(esc(fmt(totalLeq, 1))).append(' dB); it does not contribute to the receiver level.</div>')
        }
        if (!directFound) {
            html.append('<div class="explain-rays-warning">No direct ray could be identified; the top ').append(maxRays)
                    .append(' rays by LEQ are shown without an added direct ray.</div>')
        }
        if (invalidPaths > 0) {
            html.append('<div class="explain-rays-warning">').append(invalidPaths)
                    .append(' row(s) with an invalid PATH were ignored.</div>')
        }
        html.append('</div>')

        html.append('<div class="explain-rays-grid">')
        html.append('<div class="explain-rays-panel"><h4>2D scene</h4>')
                .append('<div class="explain-rays-map-toolbar">')
        if (basemapEnabled) {
            html.append('<label><input type="checkbox" data-explain-rays-basemap checked> Basemap</label>')
        } else {
            html.append('<span>Basemap unavailable (no SRID)</span>')
        }
        html.append('<span>Layers: ground 50% / buildings 100%</span></div>')
                .append(render2D(selectedRays, mapBuildings, mapGroundAreas, mapReceiverPoint, mapSourcePoint, basemapEnabled))
                .append(renderLegend(!mapBuildings.isEmpty(), !mapGroundAreas.isEmpty(), basemapEnabled))
                .append('</div>')
        html.append(renderTable(tableRows))
        html.append('</div>')
        html.append(renderProfiles(selectedRays))

        String embeddedJson = jsonSafe(JsonOutput.toJson(embeddedData))
        html.append('<script type="application/json" id="explain-rays-data">').append(embeddedJson).append('</script>')
        html.append(explainScript())
        html.append('</div>')

        logger.info('Explain propagation rays: {} rays displayed out of {} for receiver {}',
                selectedRays.size(), totalRows, receiverValue)
        return [result: html.toString()]
    } catch (Exception exception) {
        logger.error('Explain propagation rays failed', exception)
        return [result: renderError("The propagation ray viewer failed: ${exception.message}")]
    }
}

static String firstDistinct(Sql sql, String sqlTable, String column, String orderColumn) {
    String query = "SELECT ${column} AS \"V\" FROM ${sqlTable} WHERE ${column} IS NOT NULL ORDER BY ${orderColumn} LIMIT 1".toString()
    def row = sql.firstRow(query)
    return row == null || row.V == null ? null : row.V.toString()
}
