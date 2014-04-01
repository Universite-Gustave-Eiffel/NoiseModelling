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
create table tri_lvl as SELECT * from BR_TriGrid(buildings,roads_src,'db_m',750,50,0,1.5,2.8,75,2,1,0.23);

-- Use the triangle area contouring interpolation (split triangle covering level parameter)
-- iso lvls in w corresponding to dB->'45,50,55,60,65,70,75,200'
-- the output iso will be [-inf to 45] -> 0 ]45 to 50] -> 1 etc.. 
-- Theses levels corresponding to the ranges specified in the standart NF S 31 130 
create table tricontouring_noise_map AS SELECT * from ST_TriangleContouring(tri_lvl,'db_v1','db_v2','db_v3','31622, 100000, 316227, 1000000, 3162277, 1e+7, 31622776, 1e+20');

-- Merge adjacent triangle into polygons (multiple polygon by row, for unique isoLevel and cellId key)
-- Merge triangle together to reduce the number of rows
create table multipolygon_iso as select * from ST_TABLEGEOMETRYUNION(tricontouring_noise_map);

-- Explode each row to keep only a polygon by row
drop table if exists contouring_noise_map;
create table contouring_noise_map as select the_geom,idiso from ST_Explode(multipolygon_iso);
drop table multipolygon_iso purge;
