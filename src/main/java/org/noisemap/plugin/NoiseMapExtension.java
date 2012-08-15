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
package org.noisemap.plugin;

import org.apache.log4j.Logger;
import org.gdms.sql.function.math.Power;
import org.noisemap.core.*;
import org.orbisgis.core.ui.pluginSystem.Extension;
import org.orbisgis.core.ui.pluginSystem.PlugInContext;
import org.orbisgis.core.ui.pluginSystem.workbench.FeatureInstaller;

/**
 * 
 * @author Nicolas Fortin
 */
public class NoiseMapExtension extends Extension {
        private Logger logger = Logger.getLogger(NoiseMapExtension.class);
	@Override
	public void configure(PlugInContext context) throws Exception {
		FeatureInstaller fi=context.getFeatureInstaller();
		fi.addRegisterFunction(BR_EvalSource.class);
		fi.addRegisterFunction(ST_SplitLineInPoints.class);
		fi.addRegisterFunction(Log10.class);
		fi.addRegisterFunction(Power.class);
		fi.addRegisterFunction(ST_SetNearestZ.class);
		fi.addRegisterFunction(ST_SetNearestGeometryId.class);
		fi.addRegisterFunction(ST_SplitSegment.class);
		fi.addRegisterFunction(BR_TriGrid.class);
		fi.addRegisterFunction(ST_TriangleContouring.class);
		fi.addRegisterFunction(BR_SpectrumRepartition.class);
		fi.addRegisterFunction(BTW_SpectrumRepartition.class);
		fi.addRegisterFunction(BTW_EvalSource.class);
                fi.addRegisterFunction(ST_TableGeometryUnion.class);
                fi.addRegisterFunction(ST_ExtractVerticesTriGrid.class);
                fi.addRegisterFunction(BR_PtGrid.class);
		logger.info("Noise mapping extension plugin loaded..");
	}

}
