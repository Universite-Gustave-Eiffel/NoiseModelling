// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 TAKT contributors
import groovy.json.JsonOutput
import groovy.sql.Sql
import java.sql.Connection

title = 'Prepare an official tutorial scene with one declared screen'
description = 'Fixture preparation only, without a noise calculation'
inputs = [:]
outputs = [result: [name: 'result', title: 'result', type: String.class]]

def exec(Connection connection, Map input) {
    File stock = new File(System.getenv('NM_STOCK'))
    double height = Double.parseDouble(System.getenv('BENCH_HEIGHT_M'))
    Script importer = new GroovyShell().parse(new File(stock,
            'noisemodelling/wps/Import_and_Export/Import_File.groovy'))
    ['ground_type.shp', 'buildings.shp', 'receivers.shp', 'ROADS2.shp', 'dem.geojson'].each { name ->
        importer.invokeMethod('exec', [connection, [pathFile: new File(stock,
                'resources/org/noise_planet/noisemodelling/wps/' + name).absolutePath]])
    }
    Sql sql = new Sql(connection)
    int srid = sql.firstRow('SELECT ST_SRID(THE_GEOM) AS S FROM RECEIVERS LIMIT 1').S
    String screen = null
    int receiver = -1
    for (row in sql.rows('SELECT PK, ST_X(THE_GEOM) AS X, ST_Y(THE_GEOM) AS Y FROM RECEIVERS ORDER BY PK')) {
        for (offset in [[10, 10], [20, 20], [-20, 20], [20, -20], [-20, -20], [40, 40]]) {
            double x = row.X + offset[0]
            double y = row.Y + offset[1]
            String wkt = "POLYGON((${x - 10} ${y - 0.1},${x + 10} ${y - 0.1}," +
                    "${x + 10} ${y + 0.1},${x - 10} ${y + 0.1},${x - 10} ${y - 0.1}))"
            boolean clear = ['BUILDINGS', 'ROADS2'].every { table ->
                sql.firstRow('SELECT COUNT(*) AS N FROM ' + table + ' WHERE ' +
                        'ST_DWITHIN(THE_GEOM, ST_GEOMFROMTEXT(?, ?),1)', [wkt, srid]).N == 0
            }
            if (clear) {
                screen = wkt
                receiver = row.PK
                break
            }
        }
        if (screen != null) break
    }
    if (screen == null) throw new IllegalStateException('No clear deterministic screen position')
    int screenId = sql.firstRow('SELECT MAX(PK) AS N FROM BUILDINGS').N + 1
    sql.execute('INSERT INTO BUILDINGS(PK, THE_GEOM, HEIGHT, ID_WAY) VALUES (?, ST_MULTI(ST_GEOMFROMTEXT(?, ?)), ?, ?)',
            [screenId, screen, srid, height, -1000001])
    Map counts = [:]
    ['BUILDINGS', 'ROADS2', 'RECEIVERS', 'DEM', 'GROUND_TYPE'].each { table ->
        counts[table] = sql.firstRow('SELECT COUNT(*) AS N FROM ' + table).N
    }
    new File('SCENE.json').text = JsonOutput.prettyPrint(JsonOutput.toJson([
            dataset: 'NoiseModelling 5.0.1 official get_started_tutorial resources',
            srid: srid, screen_id: screenId, screen_wkt: screen,
            height_m: height, placement_receiver_id: receiver, counts: counts,
            placement_rule: 'First PK-ordered receiver and listed offset with screen more than 1m from roads and buildings',
            geometry: '20m long, 0.2m wide; flat footprint, HEIGHT relative to ground'])) + '\n'
    return 'Prepared independent tutorial input with one screen'
}
