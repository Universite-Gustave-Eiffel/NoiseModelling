drop table if exists receivers;
create table receivers(id serial PRIMARY KEY, the_geom GEOMETRY(POINTZ));
insert into receivers(the_geom) values ('POINTZ (183935.5265151515 2429052.6893939395 0.0)'),
 ('POINTZ (183955.03409090906 2429080.7803030303 0.0)'), ('POINTZ (183987.8068181818 2429136.9621212124 0.0)'),
  ('POINTZ (184025.26136363632 2429196.2651515156 0.0)'), ('POINTZ (184090.8068181818 2429297.7045454546 0.0)'),
   ('POINTZ (184125.14015151514 2429386.659090909 0.0)'), ('POINTZ (184215.6553030303 2429545.8409090913 0.0)');

-- Make roads table (just geometries and road type)
drop table if exists roads_geom;
create table roads_geom ( id serial PRIMARY KEY, the_geom GEOMETRY(LINESTRINGZ), db_md63 double,db_md125 double,db_md250 double,db_md500 double, db_md1000 double,db_md2000 double, db_md4000 double,db_md8000 double);
INSERT INTO roads_geom VALUES (DEFAULT,ST_GeomFromText('LINESTRINGZ (183817 2429175 0, 183810 2429142 0,183827 2429103 0,183822 2429055 0)'), 25.65, 38.15, 54.35, 60.35, 74.65, 66.75, 59.25, 53.95);
INSERT INTO roads_geom VALUES (DEFAULT, ST_GeomFromText('LINESTRINGZ (183822 2429055 0,183841 2429054 0,183872 2429072 0,183892 2429073 0, 184013 2428992 0,184062 2428974 0)'), 25.65, 38.15, 54.35, 60.35, 74.65, 66.75, 59.25, 53.95);
INSERT INTO roads_geom VALUES (DEFAULT, ST_GeomFromText('LINESTRINGZ (183822 2429055 0, 183805 2429031 0, 183792 2428986 0, 183783 2428945 0, 183789 2428889 0)'), 25.65, 38.15, 54.35, 60.35, 74.65, 66.75, 59.25, 53.95);



drop table if exists buildings;
create table buildings(id serial PRIMARY KEY, the_geom GEOMETRY(POLYGON), height double);
INSERT INTO buildings values (DEFAULT, 'POLYGON ((183945 2428977, 183963 2429007, 183943 2429019, 183936 2429008, 183947 2429003, 183936 2428983, 183945 2428977))', 10);
INSERT INTO buildings values (DEFAULT, 'POLYGON ((183926 2429066, 183952 2429047, 183960 2429061, 183933 2429080, 183926 2429066))', 6);

-- Using type name, convert into G coefficient.
drop table if exists land_g;
create table land_g as select the_geom, CASEWHEN(TYPE='built up areas', 0, CASEWHEN(TYPE='cereals', 1, 0.7)) as G from LANDCOVER2000;

