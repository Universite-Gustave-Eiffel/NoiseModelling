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

package org.orbisgis.noisemap.h2osgi;

import org.h2gis.h2spatialapi.Function;
import org.orbisgis.noisemap.h2.BR_EvalSource;
import org.orbisgis.noisemap.h2.BR_PtGrid;
import org.orbisgis.noisemap.h2.BR_PtGrid3D;
import org.orbisgis.noisemap.h2.BR_SpectrumRepartition;
import org.orbisgis.noisemap.h2.BR_TriGrid;
import org.orbisgis.noisemap.h2.BR_TriGrid3D;
import org.orbisgis.noisemap.h2.BTW_EvalSource;
import org.orbisgis.noisemap.h2.BTW_SpectrumRepartition;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * As H2 function need to be in fragment bundle. This activator need to be placed in another package.
 * @author Nicolas Fortin
 */
public class Activator implements BundleActivator {
    @Override
    public void start(BundleContext ctx) throws Exception {
        reg(ctx, new BR_PtGrid());
        reg(ctx, new BR_PtGrid3D());
        reg(ctx, new BR_EvalSource());
        reg(ctx, new BR_SpectrumRepartition());
        reg(ctx, new BR_TriGrid());
        reg(ctx, new BR_TriGrid3D());
        reg(ctx, new BTW_EvalSource());
        reg(ctx, new BTW_SpectrumRepartition());
    }

    private void reg(BundleContext ctx, Function func) {
        ctx.registerService(Function.class, func, null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {

    }
}
