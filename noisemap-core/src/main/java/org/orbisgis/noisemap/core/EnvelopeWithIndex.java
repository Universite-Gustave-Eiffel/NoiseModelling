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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

/**
 * This class append an index to the envelope class
 * 
 * @param <index_t>
 * @author Nicolas Fortin
 */
public class EnvelopeWithIndex<index_t> extends Envelope {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8552159007637756012L;
	private index_t index;

	public EnvelopeWithIndex(Coordinate p, index_t id) {
		super(p);
		index = id;
	}

	public EnvelopeWithIndex(Envelope env, index_t id) {
		super(env);
		index = id;
	}

	public EnvelopeWithIndex(Coordinate p1, Coordinate p2, index_t id) {
		super(p1, p2);
		index = id;
	}

	public EnvelopeWithIndex(double x1, double x2, double y1, double y2,
			index_t id) {
		super(x1, x2, y1, y2);
		index = id;
	}

	public index_t getId() {
		return index;
	}

	public Coordinate getPosition() {
		return super.centre();
	}

	public void setId(index_t id) {
		index = id;
	}

}
