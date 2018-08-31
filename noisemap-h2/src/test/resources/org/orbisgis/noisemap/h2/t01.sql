---------------------------------
--- CAS TESTS -------------------
---------------------------------

------------------
-- TEST n 1 ------
-- make buildings table
drop table if exists buildings;
create table buildings ( the_geom GEOMETRY, height double );
-- Insert 4 buildings
--INSERT INTO buildings (the_geom) VALUES (ST_GeomFromText('MULTIPOLYGON (((0 20 0,20 20 0,20 60 0,0 60 0,0 20 0)))'));
--INSERT INTO buildings (the_geom) VALUES (ST_GeomFromText('MULTIPOLYGON (((20 0 0,100 0 0, 100 20 0,20 20 0, 20 0 0)))'));
--INSERT INTO buildings (the_geom) VALUES (ST_GeomFromText('MULTIPOLYGON (((80 30 0,80 90 0,-10 90 0,-10 70 0,60 70 0,60 30 0,80 30 0)))'));
--INSERT INTO buildings (the_geom) VALUES (ST_GeomFromText('POLYGON ((137 89 0, 137 109 0, 153 109 0, 153 89 0, 137 89 0))'));
--INSERT INTO buildings (the_geom) VALUES (ST_GeomFromText('MULTIPOLYGON (((140 0 0,230 0 0, 230 60 0, 140 60 0,140 40 0,210 40 0,210 20 0, 140 20 0, 140 0 0)))'));
ALTER TABLE buildings ADD id SERIAL;

-- SOUND SOURCE
set @LW=85;
drop table if exists sound_source;
create table sound_source(the_geom geometry, db_m100 double,db_m125 double,db_m160 double,db_m200 double,db_m250 double,db_m315 double,db_m400 double,db_m500 double,db_m630 double,
db_m800 double,db_m1000 double,db_m1250 double,db_m1600 double,db_m2000 double,db_m2500 double,db_m3150 double,db_m4000 double,db_m5000 double);
insert into sound_source values ('POINT (10 10 1)'::geometry, @LW, @LW, @LW, @LW, @LW, @LW, @LW, @LW, @LW, @LW, @LW, @LW, @LW, @LW, @LW, @LW, @LW, @LW);
ALTER TABLE sound_source ADD id SERIAL;

drop table if exists sound_source_100;
create table sound_source_100(the_geom geometry, db_m100 double,db_m125 double,db_m160 double,db_m200 double,db_m250 double,db_m315 double,db_m400 double,db_m500 double,db_m630 double,
db_m800 double,db_m1000 double,db_m1250 double,db_m1600 double,db_m2000 double,db_m2500 double,db_m3150 double,db_m4000 double,db_m5000 double) as select s.the_geom, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100 from sound_source s;
INSERT INTO sound_source_100 (the_geom, db_m100,db_m125,db_m160,db_m200,db_m250,db_m315,db_m400,db_m500,db_m630,
db_m800,db_m1000,db_m1250,db_m1600,db_m2000,db_m2500,db_m3150,db_m4000,db_m5000) VALUES (ST_GeomFromText('POINT( -1000 -1000 0 )'),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0));
INSERT INTO sound_source_100 (the_geom, db_m100,db_m125,db_m160,db_m200,db_m250,db_m315,db_m400,db_m500,db_m630,
db_m800,db_m1000,db_m1250,db_m1600,db_m2000,db_m2500,db_m3150,db_m4000,db_m5000) VALUES (ST_GeomFromText('POINT( 1000 1000 0 )'),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0));
ALTER TABLE sound_source_100 ADD id SERIAL;

-- RECEIVERS
drop table if exists receivers;
create table receivers(the_geom geometry);
insert into receivers values ('POINT (200 50 4)'::geometry);
ALTER TABLE receivers ADD id SERIAL;

-- CALCULATE ATTENUATION TABLE
drop table ATT_TABLE if exists;
create table ATT_TABLE as select * from BR_PTGRID3D('BUILDINGS','HEIGHT','SOUND_SOURCE', 'RECEIVERS','DB_M', '','', 500, 50, 2, 2, 0);


