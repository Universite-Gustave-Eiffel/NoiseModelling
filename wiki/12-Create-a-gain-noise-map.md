# /!!\ This tutorial is currently being written

The function BR_TriGrid is useful to generate automatically the receivers coordinates and get a noise map to display.

However if the geometries or parameters are changed then the receivers coordinates too.

In order to obtain the same receiver coordinates, we will have to provide it.

The function BR_PTGrid is the same function as BR_TriGrid but it take an aditionnal indexed receiver coordinates.

Here a sample of how to generate a receiver list based on existing roads and buildings:

```sql
SET @ROADS_RECEIVER_MINIMAL_DISTANCE = 0.5;
SET @BUILDINGS_RECEIVER_MINIMAL_DISTANCE = 1;
SET @DIST_RECEIVER = 15.0;
SET @DENSIFY_DIST_RECEIVER = 2;
SET @RECEIVER_HEIGHT = 1.6;
SET @XCOUNT = SELECT ROUND((ST_XMAX(THE_GEOM) - ST_XMIN(THE_GEOM)) / @DIST_RECEIVER)::integer FROM FENCE;
SET @YCOUNT = SELECT ROUND((ST_YMAX(THE_GEOM) - ST_YMIN(THE_GEOM)) / @DIST_RECEIVER)::integer FROM FENCE;
SET @XMIN = SELECT ST_XMIN(THE_GEOM) FROM FENCE;
SET @YMIN = SELECT ST_YMIN(THE_GEOM) FROM FENCE;

drop table if exists buildings, roads;
-- Set source height at 0.6 m 
-- Journal of Sound and Vibration ( 1990) 143( 1 ), 39-50 ... equivalent point source height on highway vehicles
create table roads as select pk,st_updatez(the_geom,0.6) the_geom,DB_M100, DB_M125, DB_M160, DB_M200, DB_M250, DB_M315, DB_M400, DB_M500, DB_M630, DB_M800, DB_M1000, DB_M1250, DB_M1600, DB_M2000, DB_M2500, DB_M3150, DB_M4000, DB_M5000 from st_explode('(select pk, ST_ToMultiSegments(st_simplifypreservetopology(st_linemerge(the_geom), @DENSIFY_DIST_RECEIVER)) the_geom, DB_M100, DB_M125, DB_M160, DB_M200, DB_M250, DB_M315, DB_M400, DB_M500, DB_M630, DB_M800, DB_M1000, DB_M1250, DB_M1600, DB_M2000, DB_M2500, DB_M3150, DB_M4000, DB_M5000  from ROADS_SRC_ZONE)');
create spatial index on roads(the_geom);
create table buildings as select st_precisionreducer(st_simplifypreservetopology(the_geom, 0.1),1) the_geom, hauteur height from BUILDINGS_ZONE;
create spatial index on buildings(the_geom);

-- Areas around buildings without receivers

drop table if exists buildings_buffer_tmp;
create table buildings_buffer_tmp as select st_densify(st_union(st_accum(st_buffer(st_intersection(f.the_geom, st_precisionreducer(b.the_geom, 3)), @BUILDINGS_RECEIVER_MINIMAL_DISTANCE,'endcap=square join=bevel'))), @DENSIFY_DIST_RECEIVER) the_geom from buildings b, fence f where f.the_geom && b.the_geom and st_intersects(f.the_geom, b.the_geom);
drop table if exists buildings_buffer;
create table buildings_buffer as select * from st_explode('buildings_buffer_tmp');
create spatial index on buildings_buffer(the_geom);
drop table if exists buildings_buffer_tmp;

-- Remove grid points inside buildings or roads buffer

drop table if exists additional_points;

create table additional_points as select ST_MAKEPOINT(@XMIN + (X % @XCOUNT) * @DIST_RECEIVER, @YMIN + (X::integer / @XCOUNT::integer) * @DIST_RECEIVER, @RECEIVER_HEIGHT) the_geom from system_range(0,@XCOUNT * @YCOUNT);

insert into ADDITIONAL_POINTS select r.the_geom from st_explode('(select ST_REMOVEDUPLICATEDCOORDINATES(st_collect(st_tomultipoint(st_densify(st_buffer(r.the_geom, @ROADS_RECEIVER_MINIMAL_DISTANCE + 0.1,''endcap=square join=mitre ''), @DENSIFY_DIST_RECEIVER)))) the_geom from roads r, fence f where r.the_geom && f.the_geom)') r, fence f where st_intersects(r.the_geom, f.the_geom);

delete from additional_points a where exists (select 1 from buildings b where a.the_geom && b.the_geom and ST_distance(b.the_geom, a.the_geom) < @BUILDINGS_RECEIVER_MINIMAL_DISTANCE limit 1);

delete from additional_points a where exists (select 1 from roads r where st_expand(a.the_geom, 1) && r.the_geom and st_distance(a.the_geom, r.the_geom) < @ROADS_RECEIVER_MINIMAL_DISTANCE limit 1);

-- Create a triangle mesh of the union of buildings, roads and grid points

drop table if exists mesh_input;
create table mesh_input as select * from st_explode('(select st_union(st_collect(st_precisionreducer(m.the_geom,2))) the_geom from buildings_buffer  m)') d;
insert into mesh_input(the_geom) select the_geom from additional_points;
create spatial index on mesh_input(the_geom);

-- Create a mesh of the free field areas

drop table if exists mesh;
create table mesh(pk_mesh serial, the_geom geometry, pk_v1 integer, pk_v2 integer, pk_v3 integer) as select null, the_geom, null, null, null from st_explode('(select st_constraineddelaunay(st_collect(the_geom)) the_geom from mesh_input)') m where not exists (select 1 from buildings_buffer b where b.the_geom && m.the_geom and st_contains(b.the_geom, st_centroid(m.the_geom)) limit 1) and not exists (select 1 from ROADS R where R.the_geom && m.the_geom and ST_DISTANCE(r.the_geom, m.the_geom) < 0.01 limit 1);

-- Create a table with indexed vertices of triangles

drop table if exists indexed_points;
create table indexed_points(pk_point serial, the_geom point) as select null, ST_MAKEPOINT(ST_X(the_geom), ST_Y(THE_GEOM), @RECEIVER_HEIGHT) the_geom from ST_EXPLODE('(SELECT ST_RemoveDuplicatedCoordinates(ST_COLLECT(ST_TOMULTIPOINT(the_geom))) the_geom from mesh)');
create spatial index on indexed_points(the_geom);
update mesh set pk_v1 = (select pk_point from indexed_points ip where st_expand(ST_POINTN(ST_EXTERIORRING(mesh.the_geom), 1), 1, 1) && ip.the_geom order by st_distance(mesh.the_geom, ip.the_geom) ASC LIMIT 1), pk_v2 = (select pk_point from indexed_points ip where st_expand(ST_POINTN(ST_EXTERIORRING(mesh.the_geom), 2), 1, 1) && ip.the_geom order by st_distance(mesh.the_geom, ip.the_geom) ASC LIMIT 1), pk_v3 = (select pk_point from indexed_points ip where st_expand(ST_POINTN(ST_EXTERIORRING(mesh.the_geom), 3), 1, 1) && ip.the_geom order by st_distance(mesh.the_geom, ip.the_geom) ASC LIMIT 1);

-- Optimise land use table for faster computation
drop table if exists landuse;
create table landuse as select the_geom, g from st_explode('(select st_tessellate(st_precisionreducer(st_simplifypreservetopology(the_geom, 0.1),1)) the_geom, g from LAND_USE_ZONE_CENSE_2KM)') L;
create spatial index on landuse(the_geom);
drop table if exists RECEIVER_LVL;
create table RECEIVER_LVL as select * from BR_PTGRID3D('BUILDINGS','HEIGHT','ROADS','indexed_points','DB_M','landuse','',750,50,1,0,0.23);

alter table receiver_lvl alter column gid integer not null;
create index on receiver_lvl(gid);
delete from receiver_lvl R where CELL_ID > (SELECT MIN(CELL_ID) FROM RECEIVER_LVL RL WHERE R.GID = RL.GID);
alter table receiver_lvl add primary key(GID); 

-- Reconstruct tri_lvl table with mesh and computation result
drop table if exists tri_lvl;
create table tri_lvl as select the_geom, r1.w w_v1, r2.w w_v2, r3.w w_v3, r1.cell_id from MESH m, RECEIVER_LVL r1, RECEIVER_LVL r2,RECEIVER_LVL r3 where r1.GID = PK_V1 and r2.GID = PK_V2 and r3.GID = PK_V3; 

-- Use the triangle area contouring interpolation (split triangle covering level parameter)
-- iso lvls in w corresponding to dB->'45,50,55,60,65,70,75,200'
-- the output iso will be [-inf to 45] -> 0 ]45 to 50] -> 1 etc..
-- Theses levels corresponding to the ranges specified in the standard NF S 31 130 
drop table if exists tricontouring_noise_map;
create table tricontouring_noise_map AS SELECT * from ST_TriangleContouring('tri_lvl','w_v1','w_v2','w_v3',31622, 100000, 316227, 1000000, 3162277, 1e+7, 31622776, 1e+20);
-- Merge adjacent triangle into polygons (multiple polygon by row, for unique isoLevel and cellId key)
drop table if exists multipolygon_iso;
create table multipolygon_iso as select ST_UNION(st_buffer(ST_ACCUM(the_geom),0)) the_geom ,idiso from tricontouring_noise_map GROUP BY IDISO, CELL_ID;

-- Explode each row to keep only a polygon by row
drop table if exists contouring_noise_map;
create table contouring_noise_map as select the_geom,idiso from ST_Explode('multipolygon_iso');
drop table multipolygon_iso;
```