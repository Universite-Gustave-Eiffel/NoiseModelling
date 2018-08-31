drop table if exists batiments;
create table batiments ( the_geom GEOMETRY, hauteur double );
INSERT INTO batiments (the_geom) VALUES (ST_GeomFromText('MULTIPOLYGON (((0 1 0,1 1 0,1 2 0,0 2 0,0 1 0)))'));
INSERT INTO batiments (the_geom) VALUES (ST_GeomFromText('MULTIPOLYGON (((500 200 0,499 200 0, 499 199 0,500 199 0, 500 200 0)))'));

drop table route if exists;
create table route as select ST_LineFromText('LINESTRING(245 100 0.05, 255 100 0.05)') the_geom, 50 VIT_VL, 50 VIT_PL, 300 MT, 30 PT;
INSERT INTO route(the_geom,VIT_VL,VIT_PL,  MT,  PT) VALUES (ST_LineFromText('LINESTRING(0 150 0, 500 150 0)') , 50 , 50 , 300 , 30 );
alter table route add column id serial;

drop table recepteur if exists;
create table recepteur as select ST_PointFromText('POINT(250 0 1.5)') the_geom;
INSERT INTO recepteur (the_geom) VALUES (ST_PointFromText('POINT(250 50 1.5)'));
INSERT INTO recepteur (the_geom) VALUES (ST_PointFromText('POINT(250 90 1.5)'));
INSERT INTO recepteur (the_geom) VALUES (ST_PointFromText('POINT(250 -100 1.5)'));

alter table recepteur add column id serial;

drop table route_src if exists;
create table route_src as select id, the_geom, BR_EvalSource(VIT_VL, VIT_PL, MT, PT, 0, 0, 1) db_m from route;
--
drop table if exists route_srcf;
CREATE TABLE route_srcf AS SELECT id, the_geom,
BR_SpectrumRepartition(100,1,db_m) as db_m100,
BR_SpectrumRepartition(125,1,db_m) as db_m125,
BR_SpectrumRepartition(250,1,db_m) as db_m250,
BR_SpectrumRepartition(500,1,db_m) as db_m500,
BR_SpectrumRepartition(1000,1,db_m) as db_m1000,
BR_SpectrumRepartition(2000,1,db_m) as db_m2000,
BR_SpectrumRepartition(4000,1,db_m) as db_m4000,
BR_SpectrumRepartition(5000,1,db_m) as db_m5000 from route_src;
drop table if exists route_srcf100;
CREATE TABLE route_srcf100 AS SELECT id, the_geom,
100 as db_m100,
100 as db_m125,
100  as db_m250,
100 as db_m500,
100 as db_m1000,
100 as db_m2000,
100 as db_m4000,
100  as db_m5000 from route_src;

-- FROM ROAD TO POINTS
drop table if exists route_explode;
create table route_explode as SELECT * FROM ST_Explode('route_srcf');
alter table route_explode add length double as select ST_LENGTH(the_geom);
alter table route_explode add  column id_te serial ;

drop table if exists route_explode_seg;
create table route_explode_seg as SELECT ST_ToMultiSegments(the_geom) the_geom, id_te, length, 0 dB,
    db_m100, db_m125,db_m250, db_m500, 
     db_m1000,  db_m2000, db_m4000, db_m5000 
FROM route_explode;


alter table route_explode_seg add  column id_teg serial ;

drop table route_explode_seg2 if exists;
create table route_explode_seg2 as SELECT * from ST_Explode('route_explode_seg');
alter table route_explode_seg2 add  column id_teg2 serial ;

drop table route_traf if exists;
create table route_traf as SELECT ST_Tomultipoint(ST_Densify(the_geom, 21.5)) the_geom, dB,
    db_m100, db_m125, db_m250,  db_m500, 
    db_m1000, db_m2000, db_m4000, db_m5000
    , length, id_te, id_teg from route_explode_seg2;
alter table route_traf add  column id_gt serial ;



drop table route_traf2 if exists;
create table route_traf2 as SELECT * from ST_Explode('route_traf');

drop table route_traf2_wd if exists;
create table route_traf2_wd as SELECT DISTINCT the_geom, dB,
    db_m100, db_m125, db_m250,  db_m500, 
     db_m1000,  db_m2000,  db_m4000, db_m5000 , length, id_te, id_teg from route_traf2;

drop table route_count_points if exists;
create table route_count_points as SELECT id_TE, AVG(length)/(COUNT(*)) len_point from route_traf2_wd group by id_TE;

drop table route_src_pt if exists;
--create table route_src_pt as select a.the_geom, b.len_point, dB+10*LOG10(b.len_point) DB, 
--   db_m100+10*LOG10(b.len_point) db_m100, 
--   db_m125+10*LOG10(b.len_point) db_m125, 
--   db_m250+10*LOG10(b.len_point) db_m250, 
--   db_m500+10*LOG10(b.len_point) db_m500, 
--    db_m1000+10*LOG10(b.len_point) db_m1000, 
--    db_m2000+10*LOG10(b.len_point) db_m2000, 
--    db_m4000+10*LOG10(b.len_point) db_m4000, 
--    db_m5000+10*LOG10(b.len_point) db_m5000  from route_traf2_wd a, route_count_points b where a.id_te=b.id_te;
    create table route_src_pt as select a.the_geom, b.len_point, 0 DB, 
   0 db_m100, 
   0 db_m125, 
   0 db_m250, 
   0 db_m500, 
    0 db_m1000, 
    0 db_m2000, 
    100 db_m4000, 
   0 db_m5000  from route_traf2_wd a, route_count_points b where a.id_te=b.id_te;
alter table route_src_pt add  column pk serial ;


drop table if exists route_src_pt_100;
create table route_src_pt_100(PK int PRIMARY KEY,the_geom geometry, db_m100 double,db_m125 double,db_m250 double,db_m500 double,db_m1000 double,db_m2000 double,db_m4000 double,db_m5000 double) 
as select pk ,ST_UpdateZ(the_geom, 0.05) the_geom,  0, 0, 0, 0,0, 0, 100, 0 from route_src_pt;


-----------------------------
-- BIG COMPUTATION
-----------------------
drop table if exists test_LEQ ;
create table test_LEQ  as SELECT * from BR_PTGRID3D('batiments', 'HAUTEUR',  'route_srcf', 'recepteur', 'DB_M','','', 750, 10, 0, 0, 0);

drop table if exists test_LEQ_ATT ;
create table test_LEQ_ATT as  SELECT * from BR_PTGRID3D_ATT_F('batiments', 'HAUTEUR', 'route_srcf100', 'recepteur', 'DB_M','', '', 750, 10, 0, 0, 0);