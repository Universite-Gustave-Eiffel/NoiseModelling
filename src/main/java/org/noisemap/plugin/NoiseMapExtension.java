package org.noisemap.plugin;

/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

import org.apache.log4j.Logger;
import org.gdms.sql.function.math.Power;
import org.noisemap.core.*;
import org.orbisgis.core.ui.pluginSystem.Extension;
import org.orbisgis.core.ui.pluginSystem.PlugInContext;
import org.orbisgis.core.ui.pluginSystem.workbench.FeatureInstaller;

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
