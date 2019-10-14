#!/usr/bin/python
from __future__ import print_function
import os


try:
    import psycopg2
except ImportError:
    print("Module psycopg2 is missing, cannot connect to PostgreSQL")
    exit(1)
    
def executeScenario1(cursor):
    # Scenario sample
    # Sending/Receiving geometry data using odbc connection is very slow
    # It is advised to use shape file or other storage format, so use SHPREAD or FILETABLE sql functions

    print("make buildings table ..")
    
    cursor.execute("""
    drop table if exists buildings;
    create table buildings ( the_geom GEOMETRY );

    -- Insert 4 buildings
    INSERT INTO buildings (the_geom) VALUES (ST_GeomFromText('MULTIPOLYGON (((0 20 0,20 20 0,20 60 0,0 60 0,0 20 0)))'));
    INSERT INTO buildings (the_geom) VALUES (ST_GeomFromText('MULTIPOLYGON (((20 0 0,100 0 0, 100 20 0,20 20 0, 20 0 0)))'));
    INSERT INTO buildings (the_geom) VALUES (ST_GeomFromText('MULTIPOLYGON (((80 30 0,80 90 0,-10 90 0,-10 70 0,60 70 0,60 30 0,80 30 0)))'));
    INSERT INTO buildings (the_geom) VALUES (ST_GeomFromText('POLYGON ((137 89 0, 137 109 0, 153 109 0, 153 89 0, 137 89 0))'));
    INSERT INTO buildings (the_geom) VALUES (ST_GeomFromText('MULTIPOLYGON (((140 0 0,230 0 0, 230 60 0, 140 60 0,140 40 0,210 40 0,210 20 0, 140 20 0, 140 0 0)))'));""")


    print("Make roads table (just geometries and road type)..")

    cursor.execute("""
    drop table if exists roads_geom;
    create table roads_geom ( the_geom GEOMETRY, NUM INTEGER, node_from INTEGER, node_to INTEGER, road_type INTEGER );

    INSERT INTO roads_geom (the_geom,NUM,node_from,node_to,road_type) VALUES (ST_GeomFromText('LINESTRING (88 -54 0, 125 -15 2)'),0,0,1,53);
    INSERT INTO roads_geom (the_geom,NUM,node_from,node_to,road_type) VALUES (ST_GeomFromText('LINESTRING (125 -15 2,125 104 4)'),1,1,2,53);
    INSERT INTO roads_geom (the_geom,NUM,node_from,node_to,road_type) VALUES (ST_GeomFromText('LINESTRING (125 104 4,-51 166 0)'),2,2,3,53);""")

    print("Make traffic information table..")

    cursor.execute("""
    drop table if exists roads_traffic;
    create table roads_traffic ( node_from INTEGER, node_to INTEGER, load_speed DOUBLE, junction_speed DOUBLE, max_speed DOUBLE, lightVehicleCount DOUBLE, heavyVehicleCount DOUBLE);

    -- Insert traffic data
    INSERT INTO roads_traffic (node_from,node_to,load_speed,junction_speed,max_speed,lightVehicleCount,heavyVehicleCount) VALUES (0,1,43,42,50,450,18);
    INSERT INTO roads_traffic (node_from,node_to,load_speed,junction_speed,max_speed,lightVehicleCount,heavyVehicleCount) VALUES (1,0,43,42,50,650,10);
    INSERT INTO roads_traffic (node_from,node_to,load_speed,junction_speed,max_speed,lightVehicleCount,heavyVehicleCount) VALUES (1,2,43,42,50,450,18);
    INSERT INTO roads_traffic (node_from,node_to,load_speed,junction_speed,max_speed,lightVehicleCount,heavyVehicleCount) VALUES (2,1,43,42,50,650,10);
    INSERT INTO roads_traffic (node_from,node_to,load_speed,junction_speed,max_speed,lightVehicleCount,heavyVehicleCount) VALUES (2,3,43,42,50,450,18);
    INSERT INTO roads_traffic (node_from,node_to,load_speed,junction_speed,max_speed,lightVehicleCount,heavyVehicleCount) VALUES (3,2,46,45,50,650,10);""")

    print("Duplicate geometries to give sound level for each traffic direction..")

    cursor.execute("""
    drop table if exists roads_dir_one;
    drop table if exists roads_dir_two;
    CREATE TABLE roads_dir_one AS SELECT the_geom,road_type,load_speed,junction_speed,max_speed,lightVehicleCount,heavyVehicleCount FROM roads_geom as geo,roads_traffic traff WHERE geo.node_from=traff.node_from AND geo.node_to=traff.node_to;
    CREATE TABLE roads_dir_two AS SELECT the_geom,road_type,load_speed,junction_speed,max_speed,lightVehicleCount,heavyVehicleCount FROM roads_geom as geo,roads_traffic traff WHERE geo.node_to=traff.node_from AND geo.node_from=traff.node_to;
    -- Collapse two direction in one table
    drop table if exists roads_geo_and_traffic;
    CREATE TABLE roads_geo_and_traffic AS select * from roads_dir_one UNION select * from roads_dir_two;""")

    print("Compute the sound level for each segment of roads..")

    cursor.execute("""
    drop table if exists roads_src_global;
    CREATE TABLE roads_src_global AS SELECT the_geom,BR_EvalSource(load_speed,lightVehicleCount,heavyVehicleCount,junction_speed,max_speed,road_type,ST_Z(ST_GeometryN(ST_ToMultiPoint(the_geom),1)),ST_Z(ST_GeometryN(ST_ToMultiPoint(the_geom),2)),ST_Length(the_geom),False) as db_m from roads_geo_and_traffic;""")

    print("Apply frequency repartition of road noise level..")
    
    cursor.execute("""
    drop table if exists roads_src;
    CREATE TABLE roads_src AS SELECT the_geom,
    BR_SpectrumRepartition(100,1,db_m) as db_m100,
    BR_SpectrumRepartition(125,1,db_m) as db_m125,
    BR_SpectrumRepartition(160,1,db_m) as db_m160,
    BR_SpectrumRepartition(200,1,db_m) as db_m200,
    BR_SpectrumRepartition(250,1,db_m) as db_m250,
    BR_SpectrumRepartition(315,1,db_m) as db_m315,
    BR_SpectrumRepartition(400,1,db_m) as db_m400,
    BR_SpectrumRepartition(500,1,db_m) as db_m500,
    BR_SpectrumRepartition(630,1,db_m) as db_m630,
    BR_SpectrumRepartition(800,1,db_m) as db_m800,
    BR_SpectrumRepartition(1000,1,db_m) as db_m1000,
    BR_SpectrumRepartition(1250,1,db_m) as db_m1250,
    BR_SpectrumRepartition(1600,1,db_m) as db_m1600,
    BR_SpectrumRepartition(2000,1,db_m) as db_m2000,
    BR_SpectrumRepartition(2500,1,db_m) as db_m2500,
    BR_SpectrumRepartition(3150,1,db_m) as db_m3150,
    BR_SpectrumRepartition(4000,1,db_m) as db_m4000,
    BR_SpectrumRepartition(5000,1,db_m) as db_m5000 from roads_src_global;""")
    
        
    print("Please wait, sound propagation from sources through buildings..")

    cursor.execute("""drop table if exists tri_lvl;
    create table tri_lvl as SELECT * from
    BR_TriGrid((select st_expand(st_envelope(st_accum(the_geom)), 750, 750) the_geom from ROADS_SRC),'buildings','roads_src','DB_M','',750,50,1.5,2.8,75,0,0,0.23);
    """)

    print("Computation done !")

    print("Create isocountour and save it as a shapefile in the working folder..")
    
    cursor.execute("""
    drop table if exists tricontouring_noise_map;
    create table tricontouring_noise_map AS SELECT * from ST_TriangleContouring('tri_lvl','w_v1','w_v2','w_v3',31622, 100000, 316227, 1000000, 3162277, 1e+7, 31622776, 1e+20);
    -- Merge adjacent triangle into polygons (multiple polygon by row, for unique isoLevel and cellId key)
    drop table if exists multipolygon_iso;
    create table multipolygon_iso as select ST_UNION(ST_ACCUM(the_geom)) the_geom ,idiso from tricontouring_noise_map GROUP BY IDISO, CELL_ID;
    -- Explode each row to keep only a polygon by row
    drop table if exists contouring_noise_map;
    create table contouring_noise_map as select the_geom,idiso from ST_Explode('multipolygon_iso');
    drop table multipolygon_iso;""")

    # Now save in a shape file
    shapePath = os.path.abspath("./contouring_noise_map.shp")
    cursor.execute("CALL SHPWrite('"+shapePath+"', 'CONTOURING_NOISE_MAP');")

    print("Execution done! Open this file in a GIS:\n" + shapePath)
    
def main():    
    #Define our connection string
    # db name has to be an absolute path
    db_name = (os.path.abspath(".")+os.sep+"mydb").replace(os.sep, "/")
    conn_string = "host='localhost' port=5435 dbname='"+db_name+"' user='sa' password='sa'"

    # print the connection string we will use to connect
    print("Connecting to database\n	->%s" % (conn_string))

    # get a connection, if a connect cannot be made an exception will be raised here
    conn = psycopg2.connect(conn_string)

    # conn.cursor will return a cursor object, you can use this cursor to perform queries
    cursor = conn.cursor()
    print("Connected!\n")

    # Init spatial features
    cursor.execute("CREATE ALIAS IF NOT EXISTS H2GIS_SPATIAL FOR \"org.h2gis.functions.factory.H2GISFunctions.load\";")
    cursor.execute("CALL H2GIS_SPATIAL();")

    # Init NoiseModelling functions
    cursor.execute("CREATE ALIAS IF NOT EXISTS BR_PtGrid3D FOR \"org.orbisgis.noisemap.h2.BR_PtGrid3D.noisePropagation\";")
    cursor.execute("CREATE ALIAS IF NOT EXISTS BR_PtGrid FOR \"org.orbisgis.noisemap.h2.BR_PtGrid.noisePropagation\";")
    cursor.execute("CREATE ALIAS IF NOT EXISTS BR_SpectrumRepartition FOR \"org.orbisgis.noisemap.h2.BR_SpectrumRepartition.spectrumRepartition\";")
    cursor.execute("CREATE ALIAS IF NOT EXISTS BR_EvalSource FOR \"org.orbisgis.noisemap.h2.BR_EvalSource.evalSource\";")
    cursor.execute("CREATE ALIAS IF NOT EXISTS BR_SpectrumRepartition FOR \"org.orbisgis.noisemap.h2.BR_SpectrumRepartition.spectrumRepartition\";")
    cursor.execute("CREATE ALIAS IF NOT EXISTS BR_TriGrid FOR \"org.orbisgis.noisemap.h2.BR_TriGrid.noisePropagation\";")
    cursor.execute("CREATE ALIAS IF NOT EXISTS BR_TriGrid3D FOR \"org.orbisgis.noisemap.h2.BR_TriGrid3D.noisePropagation\";")

    executeScenario1(cursor)
main()


