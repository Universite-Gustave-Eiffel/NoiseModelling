package org.noise_planet.nmtutorial01;

import org.noise_planet.noisemodelling.propagation.GeoJSONDocument;
import org.noise_planet.noisemodelling.propagation.IComputeRaysOut;
import org.noise_planet.noisemodelling.propagation.PropagationPath;
import org.noise_planet.noisemodelling.propagation.PropagationProcessData;
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData;
import org.noise_planet.noisemodelling.propagation.jdbc.PointNoiseMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;

class PropagationPathStorageFactory implements PointNoiseMap.IComputeRaysOutFactory {
    String workingDir;


    void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }

    @Override
    public IComputeRaysOut create(PropagationProcessData propagationProcessData, PropagationProcessPathData propagationProcessPathData) {
        return new PropagationPathStorage(propagationProcessData, propagationProcessPathData);
    }
}

