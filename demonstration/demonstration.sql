--
-- NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
-- evaluate the noise impact on urban mobility plans. This model is
-- based on the French standard method NMPB2008. It includes traffic-to-noise
-- sources evaluation and sound propagation processing.
--
-- This version is developed at French IRSTV Institute and at IFSTTAR
-- (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
-- French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
--
-- Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
-- Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
-- as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
--
-- Copyright (C) 2011 IFSTTAR
-- Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
--
-- Noisemap is free software: you can redistribute it and/or modify it under the
-- terms of the GNU General Public License as published by the Free Software
-- Foundation, either version 3 of the License, or (at your option) any later
-- version.
--
-- Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
-- WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
-- A PARTICULAR PURPOSE. See the GNU General Public License for more details.
--
-- You should have received a copy of the GNU General Public License along with
-- Noisemap. If not, see <http://www.gnu.org/licenses/>.
--
-- For more information, please consult: <http://www.orbisgis.org/>
-- or contact directly:
-- info_at_ orbisgis.org
--

-- Coordinates are in RGF93 	Lambert 93 	EPSG:2154
-- Create a 1km² noise map around Ecole Centrale de Nantes

-- Input tables :
-- bati_indifferencie
-- bati_industriel
-- LDEN_private (the_geom (line or point) , db_m double)
-- LDEN_tram    (the_geom (line or point), db_m double)


-- Create the fence that will contain the final map, 
create table fence ( the_geom GEOMETRY );
insert into fence (the_geom) VALUES (ST_Buffer(ST_GeomFromText('POINT (305992.94462239597 2257201.2078125007)'),1000/2,'square'));

-- Create a fence with the sound propagation distance limitation added (750m propagation)
create table extrafence ( the_geom GEOMETRY );
insert into extrafence (the_geom) VALUES (ST_Buffer(ST_GeomFromText('POINT (305992.94462239597 2257201.2078125007)'),(1000/2)+750,'square'));

-- Keep only buildings in the extrafence
create table bati_indifferencie_in_fence as select b.the_geom from bati_indifferencie b,extrafence f where ST_Intersects(f.the_geom,b.the_geom);
create table bati_industriel_in_fence as select b.the_geom from bati_industriel b,extrafence f where ST_Intersects(f.the_geom,b.the_geom);
-- merge buildings
create table bati_in_fence as select * from bati_indifferencie_in_fence UNION select * from bati_industriel_in_fence;

-- Filter sources
create table road_sources_in_fence as select ST_Intersection(f.the_geom,s.the_geom) as the_geom,s.db_m from LDEN_private s,extrafence f where ST_Intersects(f.the_geom,s.the_geom);
create table tw_sources_in_fence as select ST_Intersection(f.the_geom,s.the_geom) as the_geom,s.db_m from LDEN_tram  s,extrafence f where ST_Intersects(f.the_geom,s.the_geom);
-- Apply frequency repartition of road SPL
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
BR_SpectrumRepartition(5000,1,db_m) as db_m5000 from road_sources_in_fence;

-- Apply frequency repartition of tramway SPL
CREATE TABLE tram_src AS SELECT the_geom,
BTW_SpectrumRepartition(100,db_m) as db_m100,
BTW_SpectrumRepartition(125,db_m) as db_m125,
BTW_SpectrumRepartition(160,db_m) as db_m160,
BTW_SpectrumRepartition(200,db_m) as db_m200,
BTW_SpectrumRepartition(250,db_m) as db_m250,
BTW_SpectrumRepartition(315,db_m) as db_m315,
BTW_SpectrumRepartition(400,db_m) as db_m400,
BTW_SpectrumRepartition(500,db_m) as db_m500,
BTW_SpectrumRepartition(630,db_m) as db_m630,
BTW_SpectrumRepartition(800,db_m) as db_m800,
BTW_SpectrumRepartition(1000,db_m) as db_m1000,
BTW_SpectrumRepartition(1250,db_m) as db_m1250,
BTW_SpectrumRepartition(1600,db_m) as db_m1600,
BTW_SpectrumRepartition(2000,db_m) as db_m2000,
BTW_SpectrumRepartition(2500,db_m) as db_m2500,
BTW_SpectrumRepartition(3150,db_m) as db_m3150,
BTW_SpectrumRepartition(4000,db_m) as db_m4000,
BTW_SpectrumRepartition(5000,db_m) as db_m5000 from tw_sources_in_fence;

-- Merge sources
create table sources as select * from roads_src UNION select * from tram_src;

-- Ready to compute








----------------------------------------------------------------------
-- With a user specified receiver list

create table receivers as select * from ST_CREATEPOINTSGRID(fence,5,5);

-- Evaluation of sound level I, direct field only
create table receivers_i as select * from BR_PTGRID(bati_in_fence,sources,receivers,'db_m',750,50,2,0,0,0.23);
-- Convert to dB(A)
create table receivers_db as select the_geom, 10 * LOG10(db_m) as db_m from receivers_i;
-- Create a raster
create table raster_dbm as select * from ST_INTERPOLATE(receivers_db,2,'db_m');



-------------------------------------------
-------------------------------------------
-- With delaunay triangulation

-- Sound propagation
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
-- 2 min map comput
create table tri_lvl as SELECT * from BR_TRIGRID(bati_in_fence,sources ,'db_m',750,50,2,0,5,280,1,0,0.23);

-- Use the triangle area contouring interpolation (split triangle covering level parameter)
-- iso lvls in w corresponding to dB->'45,50,55,60,65,70,75,200'
-- the output iso will be [-inf to 45] -> 0 ]45 to 50] -> 1 etc.. 
create table tricontouring_noise_map AS SELECT * from ST_TriangleContouring(tri_lvl,'the_geom','db_v1','db_v2','db_v3','31622, 100000, 316227, 1000000, 3162277, 1e+7, 31622776, 1e+20');

-- Merge triangle together to reduce the number of rows
create table contouring_noise_map as select * from ST_TABLEGEOMETRYUNION(tricontouring_noise_map);


