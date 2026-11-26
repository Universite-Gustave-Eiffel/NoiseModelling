// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 TAKT contributors
import groovy.json.JsonOutput
import java.sql.Connection

title = 'Complete stock noise map and complete CSV export'
description = 'Official stock WPS scripts with elapsed command boundaries'
inputs = [:]
outputs = [result: [name: 'result', title: 'result', type: String.class]]

def exec(Connection connection, Map input) {
    File stock = new File(System.getenv('NM_STOCK'))
    long started = System.nanoTime()
    GroovyShell shell = new GroovyShell()
    Script noise = shell.parse(new File(stock,
            'noisemodelling/wps/NoiseModelling/Noise_level_from_traffic.groovy'))
    Script exporter = shell.parse(new File(stock,
            'noisemodelling/wps/Import_and_Export/Export_Table.groovy'))
    long parsed = System.nanoTime()
    noise.invokeMethod('exec', [connection, [
            tableBuilding: 'BUILDINGS', tableRoads: 'ROADS2', tableReceivers: 'RECEIVERS',
            tableDEM: 'DEM', tableGroundAbs: 'GROUND_TYPE',
            confReflOrder: '1', confMaxSrcDist: '150', confMaxReflDist: '50',
            confThreadNumber: '1', confDiffVertical: true, confDiffHorizontal: true,
            confMaxError: '0', confExportSourceId: false,
            confTemperature: 20.0, confHumidity: 70.0, paramWallAlpha: '0.1']])
    long calculated = System.nanoTime()
    exporter.invokeMethod('exec', [connection, [tableToExport: 'RECEIVERS_LEVEL',
            exportPath: new File('RECEIVERS_LEVEL.csv').absolutePath]])
    long exported = System.nanoTime()
    new File('STAGES.json').text = JsonOutput.toJson([
            stock_script_parse_s: (parsed - started) / 1e9,
            stock_noise_call_s: (calculated - parsed) / 1e9,
            complete_csv_export_s: (exported - calculated) / 1e9,
            note: 'noise call includes input reading, preparation, propagation and database writer']) + '\n'
    return 'Complete stock CSV exported'
}
