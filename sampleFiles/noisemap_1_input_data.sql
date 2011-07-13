-- You need to define a fence.
-- The fence is just to define a table with only one record

-- make buildings table
drop table if exists buildings;
create table buildings as select ST_GeomFromText('MULTIPOLYGON (EMPTY)"') as the_geom from fence WHERE False;

-- Insert 4 buildings
INSERT INTO buildings (the_geom) VALUES (ST_GeomFromText('MULTIPOLYGON (((0 20 0,20 20 0,20 60 0,0 60 0,0 20 0)))'));
INSERT INTO buildings (the_geom) VALUES (ST_GeomFromText('MULTIPOLYGON (((20 0 0,100 0 0, 100 20 0,20 20 0, 20 0 0)))'));
INSERT INTO buildings (the_geom) VALUES (ST_GeomFromText('MULTIPOLYGON (((80 30 0,80 90 0,-10 90 0,-10 70 0,60 70 0,60 30 0,80 30 0)))'));
INSERT INTO buildings (the_geom) VALUES (ST_GeomFromText('POLYGON ((137 89 0, 137 109 0, 153 109 0, 153 89 0, 137 89 0))'));
INSERT INTO buildings (the_geom) VALUES (ST_GeomFromText('POLYGON ((137 59 0, 160 59 0, 160 -19 0, 136 -19 0, 137 59 0))'));
--U build-> INSERT INTO buildings (the_geom) VALUES (ST_GeomFromText('MULTIPOLYGON (((140 0 0,230 0 0, 230 60 0, 140 60 0,140 40 0,210 40 0,210 20 0, 140 20 0, 140 0 0)))'));

-- Make roads table (just geometries and road type)
drop table if exists roads_geom;
create table roads_geom as select ST_GeomFromText('LINESTRING( EMPTY )') as the_geom,0 as NUM, 0 as node_from,1 as node_to,53 as  road_type FROM fence WHERE False;
INSERT INTO roads_geom (the_geom,NUM,node_from,node_to,road_type) VALUES (ST_GeomFromText('LINESTRING (88 -54 0, 125 -15 2)'),0,0,1,53);
INSERT INTO roads_geom (the_geom,NUM,node_from,node_to,road_type) VALUES (ST_GeomFromText('LINESTRING (125 -15 2,125 104 4)'),1,1,2,53);
INSERT INTO roads_geom (the_geom,NUM,node_from,node_to,road_type) VALUES (ST_GeomFromText('LINESTRING (125 104 4,-51 166 0)'),2,2,3,53);

-- Make traffic information table
drop table if exists roads_traffic;
create table roads_traffic as select 0 as node_from,0 as node_to, 35 as load_speed, 15 as junction_speed, 50 as max_speed,0 as lightVehicleCount,0 as heavyVehicleCount FROM fence WHERE False;

-- Insert traffic data
INSERT INTO roads_traffic (node_from,node_to,load_speed,junction_speed,max_speed,lightVehicleCount,heavyVehicleCount) VALUES (0,1,43,42,50,450,18);
INSERT INTO roads_traffic (node_from,node_to,load_speed,junction_speed,max_speed,lightVehicleCount,heavyVehicleCount) VALUES (1,0,43,42,50,650,10);
INSERT INTO roads_traffic (node_from,node_to,load_speed,junction_speed,max_speed,lightVehicleCount,heavyVehicleCount) VALUES (1,2,43,42,50,450,18);
INSERT INTO roads_traffic (node_from,node_to,load_speed,junction_speed,max_speed,lightVehicleCount,heavyVehicleCount) VALUES (2,1,43,42,50,650,10);
INSERT INTO roads_traffic (node_from,node_to,load_speed,junction_speed,max_speed,lightVehicleCount,heavyVehicleCount) VALUES (2,3,43,42,50,450,18);
INSERT INTO roads_traffic (node_from,node_to,load_speed,junction_speed,max_speed,lightVehicleCount,heavyVehicleCount) VALUES (3,2,46,45,50,650,10);
