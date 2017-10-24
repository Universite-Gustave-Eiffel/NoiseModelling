-- Duplicate geometries to give sound level for each traffic direction
drop table if exists roads_dir_one;
drop table if exists roads_dir_two;
CREATE TABLE roads_dir_one AS SELECT the_geom,road_type,load_speed,junction_speed,max_speed,lightVehicleCount,heavyVehicleCount FROM roads_geom as geo,roads_traffic traff WHERE geo.node_from=traff.node_from AND geo.node_to=traff.node_to;
CREATE TABLE roads_dir_two AS SELECT the_geom,road_type,load_speed,junction_speed,max_speed,lightVehicleCount,heavyVehicleCount FROM roads_geom as geo,roads_traffic traff WHERE geo.node_to=traff.node_from AND geo.node_from=traff.node_to;
-- Collapse two direction in one table
drop table if exists roads_geo_and_traffic;
CREATE TABLE roads_geo_and_traffic AS select * from roads_dir_one UNION select * from roads_dir_two;

-- Compute the sound level for each segment of roads
drop table if exists roads_src_global;
CREATE TABLE roads_src_global AS SELECT the_geom,BR_EvalSource(load_speed,lightVehicleCount,heavyVehicleCount,junction_speed,max_speed,road_type,ST_Z(ST_GeometryN(ST_ToMultiPoint(the_geom),1)),ST_Z(ST_GeometryN(ST_ToMultiPoint(the_geom),2)),ST_Length(the_geom),False) as db_m from roads_geo_and_traffic;

-- Apply frequency repartition of road noise level
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
BR_SpectrumRepartition(5000,1,db_m) as db_m5000 from roads_src_global;

-- Just for extending noise map zone, add 2 -inf db source point to specify a "bounding box" of triangulation zone

INSERT INTO roads_src (the_geom, db_m100,db_m125,db_m160,db_m200,db_m250,db_m315,db_m400,db_m500,db_m630,
db_m800,db_m1000,db_m1250,db_m1600,db_m2000,db_m2500,db_m3150,db_m4000,db_m5000) VALUES (ST_GeomFromText('POINT( -300 -300 0 )'),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0));
INSERT INTO roads_src (the_geom, db_m100,db_m125,db_m160,db_m200,db_m250,db_m315,db_m400,db_m500,db_m630,
db_m800,db_m1000,db_m1250,db_m1600,db_m2000,db_m2500,db_m3150,db_m4000,db_m5000) VALUES (ST_GeomFromText('POINT( 500 500 0 )'),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0));

-- Clear intermediate tables
