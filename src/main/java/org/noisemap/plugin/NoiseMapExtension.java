package org.noisemap.plugin;

/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

import org.noisemap.core.BR_EvalSource;
import org.noisemap.core.BR_SpectrumRepartition;
import org.noisemap.core.BR_TriGrid;
import org.noisemap.core.BTW_EvalSource;
import org.noisemap.core.BTW_SpectrumRepartition;
import org.noisemap.core.Log10;
import org.noisemap.core.ST_SetNearestGeometryId;
import org.noisemap.core.ST_SetNearestZ;
import org.noisemap.core.ST_SplitLineInPoints;
import org.noisemap.core.ST_SplitSegment;
import org.noisemap.core.ST_TriangleContouring;
import org.gdms.sql.function.math.Power;
import org.orbisgis.core.ui.pluginSystem.Extension;
import org.orbisgis.core.ui.pluginSystem.PlugInContext;
import org.orbisgis.core.ui.pluginSystem.workbench.FeatureInstaller;

public class NoiseMapExtension extends Extension {

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
		System.out.println("Noise mapping extension plugin loaded..");
	}

}
