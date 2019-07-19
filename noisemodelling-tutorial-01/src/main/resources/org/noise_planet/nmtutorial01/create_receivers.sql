drop table if exists receivers;
create spatial index on BUILDINGS_RAW(the_geom);
create table receivers(id serial, the_geom geometry) as select null, the_geom from
 ST_MAKEGRIDPOINTS('BUILDINGS_RAW', 50, 50) grid where (select id_way from buildings_raw b
 where b.the_geom && st_expand(grid.the_geom, 10, 10) and st_distance(b.the_geom, grid.the_geom) < 1 limit 1) is null;