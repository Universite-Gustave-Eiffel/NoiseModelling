package org.orbisgis.noisemap.h2;

import org.h2gis.api.ProgressVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeListener;

/**
 * @author Nicolas Fortin
 */
public class ProgressLogger implements ProgressVisitor {
    private static final Logger LOGGER = LoggerFactory.getLogger("gui."+ProgressLogger.class);
    private int receiverCount = 1;
    private int processed = 0;
    private int lastLogProgression = 0;

    @Override
    public ProgressVisitor subProcess(int i) {
        receiverCount = i;
        processed = 0;
        return this;
    }

    @Override
    public void endStep() {
        synchronized (this) {
            processed = Math.min(receiverCount, processed + 1);
            int prog = (int) ((processed / (double) receiverCount) * 100);
            if (prog != lastLogProgression) {
                lastLogProgression = prog;
                LOGGER.info(prog+" %");
            }
        }
    }

    @Override
    public void setStep(int i) {

    }

    @Override
    public int getStepCount() {
        return 0;
    }

    @Override
    public void endOfProgress() {

    }

    @Override
    public double getProgression() {
        return 0;
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public void cancel() {

    }

    @Override
    public void addPropertyChangeListener(String s, PropertyChangeListener listener) {

    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {

    }
}
