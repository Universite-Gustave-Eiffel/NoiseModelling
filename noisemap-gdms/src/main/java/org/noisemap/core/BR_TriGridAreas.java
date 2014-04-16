/**
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact on urban mobility plans. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 *
 * This version is developed at French IRSTV Institute and at IFSTTAR
 * (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 *
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
 *
 * Copyright (C) 2011 IFSTTAR
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
 *
 * Noisemap is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Noisemap. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.noisemap.core;

import org.gdms.data.DataSourceFactory;
import org.gdms.data.values.Value;
import org.gdms.driver.DriverException;
import org.gdms.driver.DataSet;
import org.gdms.sql.function.FunctionException;
import org.gdms.sql.function.FunctionSignature;
import org.gdms.sql.function.ScalarArgument;
import org.gdms.sql.function.table.TableArgument;
import org.gdms.sql.function.table.TableDefinition;
import org.gdms.sql.function.table.TableFunctionSignature;
import org.gdms.driver.DiskBufferDriver;
import org.orbisgis.progress.ProgressMonitor;

/**
 *
 * @author SU Qi
 */
public class BR_TriGridAreas extends BR_TriGrid{
        
        @Override
        public FunctionSignature[] getFunctionSignatures() {
		// Builds geom , sources.the_geom, soil areas, sources.db_m ,max propa dist , subdiv
		// lev ,roads width , receiv road dis,max tri area ,sound refl o,sound
		// dif order,wall alpha
            return new FunctionSignature[]{
                            new TableFunctionSignature(TableDefinition.GEOMETRY,
                            new TableArgument(TableDefinition.GEOMETRY),//buildings
                            new TableArgument(TableDefinition.GEOMETRY),//src
                            new TableArgument(TableDefinition.GEOMETRY),//soil areas
                            ScalarArgument.STRING,
                            ScalarArgument.DOUBLE,
                            ScalarArgument.DOUBLE,
                            ScalarArgument.INT,
                            ScalarArgument.DOUBLE,
                            ScalarArgument.DOUBLE,
                            ScalarArgument.DOUBLE,
                            ScalarArgument.INT,
                            ScalarArgument.INT,
                            ScalarArgument.DOUBLE                            
                            )
                    };
        }
        
        @Override
        public DataSet evaluate(DataSourceFactory dsf, DataSet[] tables,
            Value[] values, ProgressMonitor pm) throws FunctionException {
            if (values.length < 10) {
                throw new FunctionException("Not enough parameters !");
            } else if (values.length > 10) {
                throw new FunctionException("Too many parameters !");
            }
            String dbField = values[0].toString();
            double maxSrcDist = values[1].getAsDouble();
            double maxRefDist = values[2].getAsDouble();
            int subdivLvl = values[3].getAsInt();
            // Minimum distance between source and receiver
            double minRecDist = values[4].getAsDouble();
            // Complexity distance of roads
            double srcPtDist = values[5].getAsDouble();
            double maximumArea = values[6].getAsDouble();
            int reflexionOrder = values[7].getAsInt();
            int diffractionOrder = values[8].getAsInt();
            double wallAlpha = values[9].getAsDouble();
            assert (maxSrcDist > maxRefDist); //Maximum Source-Receiver
            //distance must be superior than
            //maximum Receiver-Wall distance
            try {
                DiskBufferDriver driver = new DiskBufferDriver(dsf, getMetadata(null));
                TriGrid triGrid = new TriGrid();
                triGrid.evaluate(driver, dsf, dbField, maxSrcDist, maxRefDist, subdivLvl, minRecDist, srcPtDist,
                        maximumArea, reflexionOrder, diffractionOrder, wallAlpha, tables[0], tables[1],tables[2], pm);
                return driver.getTable("main");
            } catch (DriverException ex) {
                throw new FunctionException(ex);
            }
	}
        
        @Override
	public String getName() {
		return "BR_TriGridAreas";
	}
        
        
        @Override
	public String getSqlOrder() {
		return "create table result as select * from BR_TriGridAreas( buildings_table, sound_sources_table, soil_areas, 'source db field name',searchSourceLimit,searchReflectionWallLimit,subdivlevel,roadwith(1.8),densification_receiver(5),max triangle area(300),reflection order(2),diffraction order(1),wall absorption(0.1));";
	}

	@Override
	public String getDescription() {
		return "BR_TriGridAreas(buildings(polygons),sources(points),soilareas(polygons, G), sound lvl field name(string),maximum propagation distance (double meter),maximum wall seeking distance (double meter),subdivision level 4^n cells(int), roads width (meter), densification of receivers near roads (meter), maximum area of triangle, sound reflection order, sound diffraction order, alpha of walls ) Sound propagation from ponctual sound sources to ponctual receivers created by a delaunay triangulation of specified buildings geometry.";
	}
}
