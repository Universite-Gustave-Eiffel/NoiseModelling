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
package org.orbisgis.noisemap.core;

/**
 * A triangle built from the combination of the 3 vertices index.
 * 
 * @author Nicolas Fortin
 */
public class Triangle {
	private int a = 0;
	private int b = 0;
	private int c = 0;
	private int attribute =-1;

	public int getA() {
		return a;
	}

	public int get(int id) {
		switch (id) {
		case 0:
			return a;
		case 1:
			return b;
		default:
			return c;
		}
	}
        public int getAttribute(){
                return this.attribute;
        }
        

	public void set(int id,int index) {
		switch (id) {
		case 0:
			a=index;
			break;
		case 1:
			b=index;
			break;
		default:
			c=index;
		}
	}

	public void setA(int a) {
		this.a = a;
	}

	public int getB() {
		return b;
	}

	public void setB(int b) {
		this.b = b;
	}

	public int getC() {
		return c;
	}

	public void setC(int c) {
		this.c = c;
	}

	/**
	 * Get triangle side (a side is the opposite of vertex index
     *    0
     *   /\
     * c/  \ b
	 * /____\ 
     *1  a   2
	 * 
	 * @param side
	 * @return
	 */
	public IntSegment getSegment(int side) {
		switch (side) {
		default:
		case 0: // a side
			return new IntSegment(this.b, this.c);
		case 1: // b side
			return new IntSegment(this.c, this.a);
		case 2: // c side
			return new IntSegment(this.a, this.b);
		}
	}

	public Triangle(int a, int b, int c, int attribute) {
		super();
		this.a = a;
		this.b = b;
		this.c = c;
                this.attribute = attribute;
	}
        
        public Triangle(int a, int b, int c) {
		super();
		this.a = a;
		this.b = b;
		this.c = c;
             
	}
}
