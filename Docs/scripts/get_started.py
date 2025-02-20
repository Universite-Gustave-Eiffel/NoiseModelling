import urllib.request
from string import Template

import_file = Template('<p0:Execute xmlns:p0="http://www.opengis.net/wps/1.0.0" service="WPS" version="1.0.0"><p1'
                       ':Identifier xmlns:p1="http://www.opengis.net/ows/1.1">Import_and_Export:Import_File</p1'
                       ':Identifier><p0:DataInputs><p0:Input><p1:Identifier '
                       'xmlns:p1="http://www.opengis.net/ows/1.1">pathFile</p1:Identifier><p0:Data><p0:LiteralData'
                       '>$path</p0:LiteralData></p0:Data></p0:Input></p0:DataInputs'
                       '><p0:ResponseForm><p0:RawDataOutput><p1:Identifier '
                       'xmlns:p1="http://www.opengis.net/ows/1.1">result</p1:Identifier></p0:RawDataOutput></p0'
                       ':ResponseForm></p0:Execute>')

get_lday = Template('<p0:Execute xmlns:p0="http://www.opengis.net/wps/1.0.0" service="WPS" '
                    'version="1.0.0"><p1:Identifier xmlns:p1="http://www.opengis.net/ows/1.1">NoiseModelling'
                    ':Noise_level_from_traffic</p1:Identifier><p0:DataInputs><p0:Input><p1:Identifier '
                    'xmlns:p1="http://www.opengis.net/ows/1.1">tableReceivers</p1:Identifier><p0:Data><p0:LiteralData'
                    '>$table_receivers</p0:LiteralData></p0:Data></p0:Input><p0:Input><p1:Identifier '
                    'xmlns:p1="http://www.opengis.net/ows/1.1">tableBuilding</p1:Identifier><p0:Data><p0:LiteralData'
                    '>$table_buildings</p0:LiteralData></p0:Data></p0:Input><p0:Input><p1:Identifier '
                    'xmlns:p1="http://www.opengis.net/ows/1.1">tableDEM</p1:Identifier><p0:Data><p0:LiteralData'
                    '>$table_dem</p0:LiteralData></p0:Data></p0:Input><p0:Input><p1:Identifier '
                    'xmlns:p1="http://www.opengis.net/ows/1.1">tableRoads</p1:Identifier><p0:Data><p0:LiteralData'
                    '>$table_roads</p0:LiteralData></p0:Data></p0:Input></p0:DataInputs><p0:ResponseForm><p0'
                    ':RawDataOutput ><p1:Identifier xmlns:p1="http://www.opengis.net/ows/1.1">result</p1:Identifier'
                    '></p0:RawDataOutput></p0:ResponseForm></p0:Execute>')

export_table = Template('<p0:Execute xmlns:p0="http://www.opengis.net/wps/1.0.0" service="WPS" '
                        'version="1.0.0"><p1:Identifier '
                        'xmlns:p1="http://www.opengis.net/ows/1.1">Import_and_Export:Export_Table</p1:Identifier><p0'
                        ':DataInputs><p0:Input><p1:Identifier '
                        'xmlns:p1="http://www.opengis.net/ows/1.1">tableToExport</p1:Identifier><p0:Data><p0'
                        ':LiteralData>$table_to_export</p0:LiteralData></p0:Data></p0:Input><p0:Input><p1:Identifier '
                        'xmlns:p1="http://www.opengis.net/ows/1.1">exportPath</p1:Identifier><p0:Data><p0:LiteralData'
                        '>$export_path</p0:LiteralData></p0:Data></p0:Input></p0:DataInputs><p0:ResponseForm><p0'
                        ':RawDataOutput><p1:Identifier '
                        'xmlns:p1="http://www.opengis.net/ows/1.1">result</p1:Identifier></p0:RawDataOutput></p0'
                        ':ResponseForm></p0:Execute>')


def call_geoserver(data):
    req = urllib.request.Request(url='http://localhost:9580/geoserver/ows', data=bytes(data, encoding="utf8"),
                                 method='POST')
    req.add_header('Content-Type', 'application/xml; charset=utf-8')

    with urllib.request.urlopen(req) as f:
        print(f.status)
        print(f.reason)
        if f.status == 200:
            print(str(f.read(), encoding="utf8"))


call_geoserver(import_file.substitute({"path": "data_dir/data/wpsdata/buildings.shp"}))
call_geoserver(import_file.substitute({"path": "data_dir/data/wpsdata/ground_type.shp"}))
call_geoserver(import_file.substitute({"path": "data_dir/data/wpsdata/receivers.shp"}))
call_geoserver(import_file.substitute({"path": "data_dir/data/wpsdata/roads.shp"}))
call_geoserver(import_file.substitute({"path": "data_dir/data/wpsdata/dem.geojson"}))

call_geoserver(get_lday.substitute({"table_receivers": "RECEIVERS", "table_buildings": "BUILDINGS"
                                       , "table_roads": "ROADS", "table_dem": "DEM"}))

call_geoserver(export_table.substitute({"table_to_export": "RECEIVERS_LEVEL", "export_path" : "RECEIVERS_LEVEL.shp"}))
