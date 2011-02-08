package lcpc_son;

import org.orbisgis.core.ui.pluginSystem.Extension;
import org.orbisgis.core.ui.pluginSystem.PlugInContext;
import org.gdms.sql.function.math.Power;

public class LcpcSonExtension extends Extension{

	@Override
	public void configure(PlugInContext context) throws Exception {
		context.getFeatureInstaller().addRegisterFunction(BR_EvalSourceV1.class);
		context.getFeatureInstaller().addRegisterFunction(BR_EvalSourceV2.class);
		context.getFeatureInstaller().addRegisterFunction(BR_EvalSourceV3.class);
		context.getFeatureInstaller().addRegisterFunction(BR_EvalSourceV4.class);
		context.getFeatureInstaller().addRegisterFunction(ST_SplitLineInPoints.class);
		context.getFeatureInstaller().addRegisterFunction(Log10.class);
		context.getFeatureInstaller().addRegisterFunction(Power.class);
		context.getFeatureInstaller().addRegisterCustomQuery(ST_SetNearestZ.class);
		context.getFeatureInstaller().addRegisterCustomQuery(ST_SetNearestGeometryId.class);
		context.getFeatureInstaller().addRegisterFunction(ST_SplitSegment.class);
		context.getFeatureInstaller().addRegisterCustomQuery(BR_TriGrid.class);
		context.getFeatureInstaller().addRegisterCustomQuery(ST_TriangleContouring.class);
		context.getFeatureInstaller().addRegisterFunction(BR_SpectrumRepartition.class);		
	}
}
