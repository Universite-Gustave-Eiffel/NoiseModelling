/*
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact in urban areas. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 *
 * This plugin is currently developed by the Environmental Acoustics Laboratory (LAE) of Ifsttar
 * (http://wwww.lae.ifsttar.fr/) in collaboration with the Lab-STICC CNRS laboratory.
 * It was initially developed as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 *
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * <nicolas.fortin@ifsttar.fr>
 *
 * Copyright (C) 2011-2016 IFSTTAR-CNRS
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
 * For more information concerning NoiseM@p, please consult: <http://noisemap.orbisgis.org/>
 *
 * For more information concerning OrbisGis, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 *
 * info_at_ orbisgis.org
 */
package org.orbisgis.noisemap.core;

import java.util.List;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.quadtree.Quadtree;

/**
 * @brief Sources merging This class can merge source point by a
 *        maximum distance, from another closed source.
 *  TODO use hashmap with position/epsilon_merge
 * @author Nicolas Fortin
 */
public class PointsMerge {
	private Quadtree ptQuad = new Quadtree();
	private double distMerge = 1.;
        private int index_counter=-1;
	
	public PointsMerge(double distMerge) {
		super();
		this.distMerge = distMerge;
	}

    /**
     * @return THe number of points in the query structure.
     */
    public int getSize() {
        return index_counter + 1;
    }


	@SuppressWarnings("unchecked")
	/**
	 * Compute unique index for the coordinate
	 * Index count from 0 to n
	 * If the new vertex is closer than distMerge with an another vertex then it will return its index.
	 * @return The index of the vertex
	 */
	public int getOrAppendVertex(Coordinate newCoord) {
		Envelope queryEnv = new Envelope(newCoord);
		queryEnv.expandBy(this.distMerge*2);
		List<EnvelopeWithIndex<Integer>> result = this.ptQuad.query(queryEnv);
		for (EnvelopeWithIndex<Integer> envel : result) {
			if (envel.getPosition().distance(newCoord) < this.distMerge) {
				return envel.getId();
			}
		}
		// Not found then
		// Append to the list and QuadTree
                index_counter++;
		this.ptQuad.insert(queryEnv, new EnvelopeWithIndex<Integer>(queryEnv,
				index_counter));
                
		return index_counter;
	}
}
