# Introduction

In this tutorial we will use some real geospatial data. Buildings and roads geospatial data and meta-data will be extracted for a crowdsourcing geospatial database called [OpenStreetMap][1].

# 1. Export All open street map informations

Go to https://www.openstreetmap.org and zoom into the area of interest. The area must should not be greater than 3 square km (api limitation). You can extract the osm file by clicking on the export tab.
You can however query larger zone by downloading region extracts (http://download.geofabrik.de)

The desired file format is `*.osm` or `*.osm.bz2`

# 2. Transfer osm file into OrbisGIS database

In the GeoCatalog, import the `map.osm` file. It will create several tables.

# 3. Extract 2D buildings from tables 

The following query will extract buildings using the projection UTM 30N. **Warning, switch to the correction projection.**

The datasource is in WGS84, corresponding to 4326 code.
You can change the code here : ST_SETSRID(ST_MAKEPOLYGON(ST_MAKELINE(THE_GEOM)), 4326), **32630**)
The 32630 code corresponding to UTM 30N is great for dataset from the west part of France.

```sql
-- Filter OSM entities to keep only buildings
DROP TABLE IF EXISTS MAP_BUILDINGS;
CREATE TABLE MAP_BUILDINGS(ID_WAY BIGINT PRIMARY KEY) AS SELECT DISTINCT ID_WAY FROM MAP_WAY_TAG WT, MAP_TAG T 
WHERE WT.ID_TAG = T.ID_TAG AND T.TAG_KEY IN ('building');
DROP TABLE IF EXISTS BUILDINGS_RAW;
-- Make building polygons and convert coordinates from angle to meter
-- EPSG 32630 is UTM 30-N http://spatialreference.org/ref/epsg/wgs-84-utm-zone-30n/ 
CREATE TABLE BUILDINGS_RAW AS SELECT ID_WAY, ST_TRANSFORM(ST_SETSRID(ST_MAKEPOLYGON(ST_MAKELINE(THE_GEOM)), 4326), 32630) THE_GEOM 
FROM (SELECT (SELECT ST_ACCUM(THE_GEOM) THE_GEOM FROM (SELECT N.ID_NODE, N.THE_GEOM,WN.ID_WAY IDWAY FROM MAP_NODE N,MAP_WAY_NODE WN
 WHERE N.ID_NODE = WN.ID_NODE ORDER BY WN.NODE_ORDER) WHERE  IDWAY = W.ID_WAY) THE_GEOM ,W.ID_WAY FROM MAP_WAY W,MAP_BUILDINGS B 
 WHERE W.ID_WAY = B.ID_WAY) GEOM_TABLE WHERE ST_GEOMETRYN(THE_GEOM,1) = ST_GEOMETRYN(THE_GEOM, ST_NUMGEOMETRIES(THE_GEOM)) 
 AND ST_NUMGEOMETRIES(THE_GEOM) > 2;
-- simplifiy and merge building (in order to reduce computation time and fix geometry issues)
drop table if exists BUILDINGS_SIMP_MERGE;
create table BUILDINGS_SIMP_MERGE as select ST_precisionreducer(ST_SIMPLIFYPRESERVETOPOLOGY(ST_buffer(ST_UNION(ST_ACCUM(the_geom)),0),0.1),1) the_geom from BUILDINGS_RAW;
drop table if exists buildings;
create table buildings(id serial, the_geom polygon) as select null, the_geom from st_explode('BUILDINGS_SIMP_MERGE');
```

# Note about the Coordinate System

In several input files, you need to specify coordinates, e.g road network. It is
strongly suggested not to use WGS84 coordinates (i.e. GPS coordinates). Acoustic propagation formulas make the assumption that coordinates are metric.
Many countries and regions have custom coordinate system defined, optimized for usages in
their appropriate areas. It might be best to ask some GIS specialists in your region of interest
what the most commonly used local coordinate system is and use that as well for your data.
If you don’t have any clue about what coordinate system is used in your region, it might be best
to use the Universal Transverse Mercator coordinate system. This coordinate system divides the
world into multiple bands, each six degrees width and separated into a northern and southern
part, which is called UTM zones (see http://en.wikipedia.org/wiki/UTM_zones#UTM_
zone for more details). For each zone, an optimized coordinate system is defined. Choose the
UTM zone which covers your region (Wikipedia has a nice map showing the zones) and use its
coordinate system.

Here is the map : [Utm Zone Map](https://upload.wikimedia.org/wikipedia/commons/e/ed/Utm-zones.jpg)
# 4. Extract roads

```sql
 -- Filter OSM entities to keep only roads
DROP TABLE IF EXISTS MAP_ROADS;
CREATE TABLE MAP_ROADS(ID_WAY BIGINT PRIMARY KEY,HIGHWAY_TYPE varchar(30) ) AS SELECT DISTINCT ID_WAY, VALUE HIGHWAY_TYPE FROM MAP_WAY_TAG WT, MAP_TAG T 
WHERE WT.ID_TAG = T.ID_TAG AND T.TAG_KEY IN ('highway');
DROP TABLE IF EXISTS MAP_ROADS_GEOM;
-- Make roads lines and convert coordinates from angle to meter
-- EPSG 32630 is UTM 30-N http://spatialreference.org/ref/epsg/wgs-84-utm-zone-30n/ 
CREATE TABLE MAP_ROADS_GEOM AS SELECT ID_WAY, ST_precisionreducer(ST_SIMPLIFYPRESERVETOPOLOGY(ST_TRANSFORM(ST_SETSRID(ST_MAKELINE(THE_GEOM), 4326), 32630),0.1),1) THE_GEOM, HIGHWAY_TYPE T FROM (SELECT (SELECT 
ST_ACCUM(THE_GEOM) THE_GEOM FROM (SELECT N.ID_NODE, N.THE_GEOM,WN.ID_WAY IDWAY FROM MAP_NODE 
N,MAP_WAY_NODE WN WHERE N.ID_NODE = WN.ID_NODE ORDER BY WN.NODE_ORDER) WHERE  IDWAY = W.ID_WAY) 
THE_GEOM ,W.ID_WAY, B.HIGHWAY_TYPE FROM MAP_WAY W,MAP_ROADS B WHERE W.ID_WAY = B.ID_WAY) GEOM_TABLE;
DROP TABLE MAP_ROADS;
```

# 5. Fake traffic

Traffic data are not available in this tutorial. So we will generate fake traffic using only the [category of road][2] to set the traffic speed and count.

```sql
-- Build fake traffic using only road category
DROP TABLE IF EXISTS ROADS;
CREATE TABLE ROADS(ID SERIAL,ID_WAY long , THE_GEOM LINESTRING, SPEED_LV float, SPEED_HV float, LV_HOUR float, HV_HOUR float) AS 
SELECT null, ID_WAY, THE_GEOM,
CASEWHEN(T = 'trunk', 110,
CASEWHEN(T = 'primary', 90, 
CASEWHEN(T = 'secondary', 90,
CASEWHEN(T = 'tertiary',90,
CASEWHEN(T = 'residential',50, 0
))))) SPEED_LV,
CASEWHEN(T = 'trunk', 100,
CASEWHEN(T = 'primary', 80, 
CASEWHEN(T = 'secondary', 80,
CASEWHEN(T = 'tertiary',80,
CASEWHEN(T = 'residential',40, 0
))))) SPEED_HV,
CASEWHEN(T = 'trunk', 1100,
CASEWHEN(T = 'primary', 900, 
CASEWHEN(T = 'secondary', 750,
CASEWHEN(T = 'tertiary',450,
CASEWHEN(T = 'residential',80, 0
))))) LV_HOUR,
CASEWHEN(T = 'trunk', 200,
CASEWHEN(T = 'primary', 120, 
CASEWHEN(T = 'secondary', 80,
CASEWHEN(T = 'tertiary',40,
CASEWHEN(T = 'residential',0, 0
))))) HV_HOUR FROM MAP_ROADS_GEOM where T in ('trunk', 'primary', 'secondary', 'tertiary', 'residential') ;
```

# Compute noisemap

Input data are ready. You can now evaluate the sound level of road source and propagate in the area.

```sql
drop table roads_src_global if exists;
create table roads_src_global as select the_geom, BR_EvalSource(speed_lv, speed_hv, lv_hour, hv_hour, 0, 0, 1) db_m from ROADS;
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
-- Sound propagation from sources through buildings
-- Compute only 750m inside the defined source input envelope 
drop table if exists tri_lvl;
create table tri_lvl as SELECT * from
BR_TriGrid((select st_envelope(st_accum(the_geom)) the_geom from ROADS_SRC),'buildings','roads_src','DB_M','',750,50,1.5,2.8,75,0,0,0.23);

-- Use the triangle area contouring interpolation (split triangle covering level parameter)
-- iso lvls in w corresponding to dB->'45,50,55,60,65,70,75,200'
-- the output iso will be [-inf to 45] -> 0 ]45 to 50] -> 1 etc..
-- Theses levels corresponding to the ranges specified in the standard NF S 31 130 
drop table if exists tricontouring_noise_map;
create table tricontouring_noise_map AS SELECT * from ST_TriangleContouring('tri_lvl','w_v1','w_v2','w_v3',31622, 100000, 316227, 1000000, 3162277, 1e+7, 31622776, 1e+20);

-- Merge adjacent triangle into polygons (multiple polygon by row, for unique isoLevel and cellId key)
drop table if exists multipolygon_iso;
create table multipolygon_iso as select ST_UNION(ST_ACCUM(the_geom)) the_geom ,idiso from tricontouring_noise_map GROUP BY IDISO, CELL_ID;
-- Explode each row to keep only a polygon by row
drop table if exists contouring_noise_map;
create table contouring_noise_map as select the_geom,idiso from ST_Explode('multipolygon_iso');
drop table multipolygon_iso;
```

The result is the table contouring_noise_map.

# Extended noise propagation

In order to produce 3D noise maps the **noise emission level** have to propagate between buildings and over the ground and roof.

The 3D noise propagation module requires the following information:
- Land cover
    - Law, meadow, field of cereals G=1
    - Undergrowth (resinous or decidious) G=1
    - non-compacted earth G=0.7
    - Compacted earth, track G=0.3
    - Road surface G=0
    - Smooth concrete G=0
- Digital elevation model. A cloud of `X,Y,Z` points
- Buildings polygons (`X,Y,Z` envelopes with Z being the bottom of wall) and height of building
- Road geometry with **noise emission level** at each frequency band

[1]: https://www.openstreetmap.org
[2]: http://wiki.openstreetmap.org/wiki/Key:highway
