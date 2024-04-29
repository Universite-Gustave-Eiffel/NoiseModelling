package org.noise_planet.noisemodelling.jdbc;

import org.noise_planet.noisemodelling.pathfinder.IComputeRaysOut;
import org.noise_planet.noisemodelling.pathfinder.PropagationPath;
import org.noise_planet.noisemodelling.pathfinder.utils.PowerUtils;
import org.noise_planet.noisemodelling.propagation.AttenuationCnossosParameters;
import org.noise_planet.noisemodelling.propagation.ComputeRaysOutAttenuation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

import static org.noise_planet.noisemodelling.pathfinder.utils.PowerUtils.*;

public class LDENComputeRaysOut extends ComputeRaysOutAttenuation {
    LdenData ldenData;
    LDENPropagationProcessData ldenPropagationProcessData;
    public AttenuationCnossosParameters dayPathData;
    public AttenuationCnossosParameters eveningPathData;
    public AttenuationCnossosParameters nightPathData;
    public LDENConfig ldenConfig;

    public LDENComputeRaysOut(AttenuationCnossosParameters dayPathData, AttenuationCnossosParameters eveningPathData,
                              AttenuationCnossosParameters nightPathData, LDENPropagationProcessData inputData,
                              LdenData ldenData, LDENConfig ldenConfig) {
        super(inputData.ldenConfig.exportRaysMethod != LDENConfig.ExportRaysMethods.NONE, null, inputData);
        this.keepAbsorption = inputData.ldenConfig.keepAbsorption;
        this.ldenData = ldenData;
        this.ldenPropagationProcessData = inputData;
        this.dayPathData = dayPathData;
        this.eveningPathData = eveningPathData;
        this.nightPathData = nightPathData;
        this.ldenConfig = ldenConfig;
    }

    public LdenData getLdenData() {
        return ldenData;
    }

    @Override
    public IComputeRaysOut subProcess() {
        return new ThreadComputeRaysOut(this);
    }




    public static class LdenData {
        public final AtomicLong queueSize = new AtomicLong(0);
        public final AtomicLong totalRaysInserted = new AtomicLong(0);
        public final ConcurrentLinkedDeque<VerticeSL> lDayLevels = new ConcurrentLinkedDeque<>();
        public final ConcurrentLinkedDeque<VerticeSL> lEveningLevels = new ConcurrentLinkedDeque<>();
        public final ConcurrentLinkedDeque<VerticeSL> lNightLevels = new ConcurrentLinkedDeque<>();
        public final ConcurrentLinkedDeque<VerticeSL> lDenLevels = new ConcurrentLinkedDeque<>();
        public final ConcurrentLinkedDeque<PropagationPath> rays = new ConcurrentLinkedDeque<>();
    }
}
