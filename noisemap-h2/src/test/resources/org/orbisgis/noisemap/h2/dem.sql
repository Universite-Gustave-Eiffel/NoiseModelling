-- make buildings table
drop table if exists buildings;
create table buildings ( the_geom GEOMETRY, height double );
INSERT INTO buildings VALUES
('POLYGON ((80 -30 0,80 90 0,-10 90 0,-10 70 0,60 70 0,60 -30 0,80 -30 0))',5);
drop table if exists sound_source;
create table sound_source(the_geom geometry, db_m100 double,db_m125 double,db_m160 double,db_m200 double,db_m250 double,db_m315 double,db_m400 double,db_m500 double,db_m630 double,
db_m800 double,db_m1000 double,db_m1250 double,db_m1600 double,db_m2000 double,db_m2500 double,db_m3150 double,db_m4000 double,db_m5000 double);
insert into sound_source VALUES ('POINT(55 60 1)', 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100);
INSERT INTO sound_source VALUES ('POINT( -300 -300 0 )',Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0));
INSERT INTO sound_source VALUES ('POINT( 500 500 0 )',Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0),Log10(0));
-- Create Digital elevation model using gaussian 2d function
drop table if exists all_dem;
SET @DOMAIN_XMIN = SELECT ST_XMIN(ST_EXTENT(THE_GEOM)) FROM SOUND_SOURCE;
SET @DOMAIN_XMAX = SELECT ST_XMAX(ST_EXTENT(THE_GEOM)) FROM SOUND_SOURCE;
SET @DOMAIN_YMIN = SELECT ST_YMIN(ST_EXTENT(THE_GEOM)) FROM SOUND_SOURCE;
SET @DOMAIN_YMAX = SELECT ST_YMAX(ST_EXTENT(THE_GEOM)) FROM SOUND_SOURCE;
SET @POINT_COUNT = 50;
SET @MOUNTAIN_X = -80;
SET @MOUNTAIN_Y = 50;
SET @MONTAIN_WIDTH = 8;
SET @MOUNTAIN_LENGTH = 50;
create table all_dem(the_geom POINT,Z double as ST_Z(the_geom)) as select ST_MAKEPOINT(X * ((@DOMAIN_XMAX - @DOMAIN_XMIN) / @POINT_COUNT) + @DOMAIN_XMIN, Y * ((@DOMAIN_YMAX - @DOMAIN_YMIN) / @POINT_COUNT) + @DOMAIN_YMIN,
-- Gaussian
10 * EXP(-(POWER(X - ((@MOUNTAIN_X - @DOMAIN_XMIN) / (@DOMAIN_XMAX - @DOMAIN_XMIN) * @POINT_COUNT)  ,2) / @MONTAIN_WIDTH  + POWER(Y - ((@MOUNTAIN_Y - @DOMAIN_YMIN) / (@DOMAIN_YMAX - @DOMAIN_YMIN) * @POINT_COUNT) ,2) / @MOUNTAIN_LENGTH ))) the_geom,

null  from (select X from system_range(0,@POINT_COUNT)),(select X Y from system_range(0,@POINT_COUNT)) ;
select (-24 - @DOMAIN_XMIN) / (@DOMAIN_XMAX - @DOMAIN_XMIN) * @POINT_COUNT;
-- Remove dem point too close from buildings
drop table if exists dem;
create spatial index on buildings(the_geom);
create table dem as select d.the_geom, d.Z from all_dem d where (select COUNT(*) near from buildings b where d.the_geom && b.the_geom AND ST_DISTANCE(d.the_geom, b.the_geom) < 1) = 0;
