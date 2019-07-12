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
    ConcurrentLinkedDeque<PointToPointPaths> pathQueue = new ConcurrentLinkedDeque<>();
    GZIPOutputStream gzipOutputStream;
    AtomicBoolean waitForMorePaths = new AtomicBoolean(true);
    public static final int GZIP_CACHE_SIZE = (int)Math.pow(2, 19);
    String workingDir;

    void openPathOutputFile(String path) throws IOException {
        gzipOutputStream = new GZIPOutputStream(new FileOutputStream(path), GZIP_CACHE_SIZE);
        if(gzipOutputStream != null) {
            new Thread(new WriteThread(pathQueue, waitForMorePaths, gzipOutputStream)).start();
        }
    }

    void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }

    @Override
    public IComputeRaysOut create(PropagationProcessData propagationProcessData, PropagationProcessPathData propagationProcessPathData) {
        return new PropagationPathStorage(propagationProcessData, propagationProcessPathData, pathQueue);
    }

    void closeWriteThread() {
        waitForMorePaths.set(false);
    }

    /**
     * Write paths on disk using a single thread
     */
    static class WriteThread implements Runnable {
        Logger logger = LoggerFactory.getLogger(WriteThread.class);
        ConcurrentLinkedDeque<PointToPointPaths> pathQueue;
        AtomicBoolean waitForMorePaths;
        GZIPOutputStream gzipOutputStream;

        WriteThread(ConcurrentLinkedDeque<PointToPointPaths> pathQueue, AtomicBoolean waitForMorePaths, GZIPOutputStream gzipOutputStream) {
            this.pathQueue = pathQueue;
            this.waitForMorePaths = waitForMorePaths;
            this.gzipOutputStream = gzipOutputStream;
        }

        @Override
        public void run() {
            DataOutputStream dataOutputStream = new DataOutputStream(gzipOutputStream);
            try {
                while (waitForMorePaths.get()) {
                    while (!pathQueue.isEmpty()) {
                        PointToPointPaths paths = pathQueue.pop();
                        paths.writePropagationPathListStream(dataOutputStream);
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        break;
                    }
                }
            } catch (IOException ex) {
                logger.error(ex.getLocalizedMessage());
            } finally {
                try {
                    dataOutputStream.flush();
                    gzipOutputStream.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
            pathQueue = null; // Avoid filling up memory
        }
    }
}

