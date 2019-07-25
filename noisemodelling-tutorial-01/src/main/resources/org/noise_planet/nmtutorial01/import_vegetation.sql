--call osmread('/home/nicolas/github/NoiseModelling/noisemodelling-tutorial-01/src/main/resources/org/noise_planet/nmtutorial01/map.osm.gz', 'MAP');
DROP TABLE IF EXISTS MAP_SURFACE;
CREATE TABLE MAP_SURFACE(id serial, ID_WAY BIGINT, surf_cat varchar) AS SELECT null, ID_WAY, "VALUE" surf_cat
FROM MAP_WAY_TAG WT, MAP_TAG T
WHERE WT.ID_TAG = T.ID_TAG AND T.TAG_KEY IN ('surface', 'landcover', 'natural', 'landuse', 'leisure');
DROP TABLE IF EXISTS MAP_SURFACE_GEOM;
CREATE TABLE MAP_SURFACE_GEOM AS SELECT ID_WAY,
ST_MAKEPOLYGON(ST_MAKELINE(THE_GEOM)) THE_GEOM, surf_cat FROM (SELECT (SELECT
ST_ACCUM(THE_GEOM) THE_GEOM FROM (SELECT N.ID_NODE, N.THE_GEOM,WN.ID_WAY IDWAY FROM
MAP_NODE N,MAP_WAY_NODE WN WHERE N.ID_NODE = WN.ID_NODE ORDER BY
WN.NODE_ORDER) WHERE  IDWAY = W.ID_WAY) THE_GEOM ,W.ID_WAY, B.surf_cat
FROM MAP_WAY W,MAP_SURFACE B
WHERE W.ID_WAY = B.ID_WAY) GEOM_TABLE WHERE ST_GEOMETRYN(THE_GEOM,1) =
ST_GEOMETRYN(THE_GEOM, ST_NUMGEOMETRIES(THE_GEOM)) AND ST_NUMGEOMETRIES(THE_GEOM) >
2;
drop table if exists SURFACE_RAW;
create table SURFACE_RAW(id_way serial, the_geom geometry, surf_cat varchar, G double) as select id_way,  ST_TRANSFORM(ST_SETSRID(THE_GEOM, 4326), 2154) the_geom , surf_cat, 1 g from MAP_SURFACE_GEOM where surf_cat IN ('grass', 'village_green', 'park');
drop table if exists MAP_SURFACE_GEOM;

