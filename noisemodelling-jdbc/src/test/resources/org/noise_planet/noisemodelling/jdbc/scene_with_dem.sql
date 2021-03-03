-- make buildings table
drop table if exists buildings;
create table buildings ( id serial, the_geom GEOMETRY, height double );
-- Insert 4 buildings
INSERT INTO buildings(the_geom, height) VALUES
('MULTIPOLYGON (((0 20,20 20,20 60,0 60,0 20)))',5),
('MULTIPOLYGON (((20 0,100 0, 100 20,20 20, 20 0)))',5),
('MULTIPOLYGON (((80 30,80 90,-10 90,-10 70,60 70,60 30,80 30)))',5),
('POLYGON ((137 89, 137 109, 153 109, 153 89, 137 89))',5),
('MULTIPOLYGON (((140 0,230 0, 230 60, 140 60,140 40,210 40,210 20, 140 20, 140 0)))',10);
drop table if exists sound_source;
create table sound_source(the_geom geometry,gid serial, db_m63 double,db_m125 double,db_m250 double,db_m500 double, db_m1000 double,db_m2000 double, db_m4000 double,db_m8000 double);
insert into sound_source VALUES ('LINESTRING (26.3 175.5 0.05, 111.9 90.9 0.05, 123 -70.9 0.05, 345.2 -137.8 0.05)',1, 25.65, 38.15, 54.35, 60.35, 74.65, 66.75, 59.25, 53.95);
-- Create Digital elevation model using gaussian 2d function
drop table if exists all_dem;
SET @DOMAIN_XMIN = SELECT ST_XMIN(ST_EXPAND(ST_EXTENT(THE_GEOM), 100, 100)) FROM SOUND_SOURCE;
SET @DOMAIN_XMAX = SELECT ST_XMAX(ST_EXPAND(ST_EXTENT(THE_GEOM), 100, 100)) FROM SOUND_SOURCE;
SET @DOMAIN_YMIN = SELECT ST_YMIN(ST_EXPAND(ST_EXTENT(THE_GEOM), 100, 100)) FROM SOUND_SOURCE;
SET @DOMAIN_YMAX = SELECT ST_YMAX(ST_EXPAND(ST_EXTENT(THE_GEOM), 100, 100)) FROM SOUND_SOURCE;
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