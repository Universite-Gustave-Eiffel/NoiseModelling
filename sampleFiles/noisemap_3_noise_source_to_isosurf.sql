-- Sound propagation from sources through buildings
-- Parameters :
-- 
-- buildings(polygons)
-- sources(points & lines)
-- sound lvl field name(string)
-- maximum propagation distance (double meter)
-- subdivision level 4^n cells(int)
-- roads width (meter)
-- densification of receivers near roads (meter)
-- maximum area of triangles (square meter)
-- sound reflection order (int)
-- sound diffraction order (int)
-- absorption coefficient of walls ([0-1] double) 
drop table if exists tri_lvl;
create table tri_lvl as SELECT BR_TriGrid(b.the_geom,s.the_geom,'db_m',200,0,1.5,2.8,75,1,1,0.23) FROM buildings as b,roads_src as s;

-- Use the triangle area contouring interpolation (split triangle covering level parameter)
-- iso lvls in w '45,50,55,60,65,70,75,200'
create table tricontouring_noise_map AS SELECT ST_TriangleContouring(the_geom,'db_v1','db_v2','db_v3','31622, 100000, 316227, 1000000, 3162277, 1e+7, 31622776, 1e+20') FROM tri_lvl;

--Merge adjacent triangle into polygons (multiple polygon by row, for unique isoLevel and cellId key)
create table multipolygon_iso as select ST_Union(the_geom) as the_geom,cellid,idiso from tricontouring_noise_map GROUP BY idiso,cellid;
drop table tricontouring_noise_map purge;

-- Explode each row to keep only a polygon by row
drop table if exists contouring_noise_map;
create table contouring_noise_map as select ST_Explode(the_geom) as the_geom from multipolygon_iso ORDER BY cellid,idiso;
drop table multipolygon_iso purge;