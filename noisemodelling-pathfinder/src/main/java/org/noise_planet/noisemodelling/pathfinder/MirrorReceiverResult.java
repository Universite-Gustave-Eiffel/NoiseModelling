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
package org.noise_planet.noisemodelling.pathfinder;

import org.locationtech.jts.geom.Coordinate;

import java.util.Objects;

/**
 *  Information for Receiver image.
 * @author Nicolas Fortin
 */
public class MirrorReceiverResult {

	private Coordinate receiverPos;
	private final MirrorReceiverResult parentMirror;
	private final ProfileBuilder.Wall wall;
    private final int buildingId; // building that belongs to this wall
    private final ProfileBuilder.IntersectionType type;

    /**
     * @return coordinate of mirrored receiver
     */
	public Coordinate getReceiverPos() {
		return receiverPos;
	}

    public void setReceiverPos(Coordinate receiverPos) {
        this.receiverPos = receiverPos;
    }

    /**
     * @return Other MirrorReceiverResult index, -1 for the first reflexion
     */
	public MirrorReceiverResult getParentMirror() {
		return parentMirror;
	}

    /**
     * @return Wall index of the last mirrored processed
     */
	public ProfileBuilder.Wall getWall() {
		return wall;
	}

    /**
     * @return building that belongs to this wall
     */
    public int getBuildingId() {
        return buildingId;
    }

    /**
     * @param receiverPos coordinate of mirrored receiver
     * @param mirrorResultId Other MirrorReceiverResult index, -1 for the first reflexion
     * @param wallId Wall index of the last mirrored processed
     * @param buildingId building that belongs to this wall
     */
    public MirrorReceiverResult(Coordinate receiverPos, MirrorReceiverResult parentMirror, ProfileBuilder.Wall wall, int buildingId, ProfileBuilder.IntersectionType type) {
        this.receiverPos = receiverPos;
        this.parentMirror = parentMirror;
        this.wall = wall;
        this.buildingId = buildingId;
        this.type = type;
    }

    /**
     * Copy constructor
     * @param cpy ref
     */
    public MirrorReceiverResult(MirrorReceiverResult cpy) {
        this.receiverPos = new Coordinate(cpy.receiverPos);
        this.parentMirror = cpy.parentMirror;
        this.wall = cpy.wall;
        this.buildingId = cpy.buildingId;
        this.type = cpy.type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MirrorReceiverResult that = (MirrorReceiverResult) o;
        return wall == that.wall && buildingId == that.buildingId && receiverPos.equals(that.receiverPos) && Objects.equals(parentMirror, that.parentMirror);
    }

    @Override
    public int hashCode() {
        return Objects.hash(receiverPos, parentMirror, wall, buildingId);
    }

    public ProfileBuilder.IntersectionType getType() {
        return type;
    }
}
