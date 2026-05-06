/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */


package org.noise_planet.noisemodelling.webserver.script;

import org.noise_planet.noisemodelling.webserver.OwsController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

/**
 * Monitors a specified directory and its subdirectories for changes in files.
 * Specifically, watches for creation, deletion, and modification events of files with the `.groovy` extension.
 * It triggers a script reload after a 5-second delay, which is reset if further changes are detected.
 */
public class ScriptFileWatchedProcess implements Callable<Boolean> {

    private final Path scriptsDir;
    private final OwsController owsController;
    private final Logger logger = LoggerFactory.getLogger(ScriptFileWatchedProcess.class);

    // Executor for handling the delayed reload task
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> pendingTask = null;
    private final long delaySeconds = 5;

    public ScriptFileWatchedProcess(Path scriptsDir, OwsController owsController) {
        this.scriptsDir = scriptsDir;
        this.owsController = owsController;
    }

    @Override
    public Boolean call() throws Exception {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            registerRecursive(watchService);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    WatchKey key = watchService.take();
                    boolean shouldReload = false;

                    for (WatchEvent<?> event : key.pollEvents()) {
                        Path fileName = (Path) event.context();
                        if (fileName.toString().endsWith(".groovy")) {
                            shouldReload = true;
                            break;
                        }
                    }

                    if (shouldReload) {
                        debounceReload();
                    }

                    boolean valid = key.reset();
                    if (!valid) {
                        break;
                    }
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error in file watch process: {}", e.getMessage(), e);
                    return false;
                }
            }
        } finally {
            scheduler.shutdownNow();
        }
        return true;
    }

    /**
     * Schedules the reload task. If a task is already pending, it cancels it and starts a new 5-second timer.
     */
    private synchronized void debounceReload() {
        if (pendingTask != null && !pendingTask.isDone()) {
            pendingTask.cancel(false);
        }

        pendingTask = scheduler.schedule(() -> {
            try {
                logger.info("Change detected in scripts. Reloading after {}s quiet period...", delaySeconds);
                owsController.reloadScripts();
            } catch (Exception e) {
                logger.error("Failed to reload scripts: {}", e.getMessage(), e);
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    protected void registerRecursive(WatchService watchService) throws IOException {
        try (Stream<Path> pathStream = Files.walk(scriptsDir)) {
            pathStream.filter(Files::isDirectory)
                    .forEach(dir -> {
                        try {
                            dir.register(watchService,
                                    StandardWatchEventKinds.ENTRY_CREATE,
                                    StandardWatchEventKinds.ENTRY_DELETE,
                                    StandardWatchEventKinds.ENTRY_MODIFY);
                        } catch (IOException e) {
                            logger.error("Could not register directory {}: {}", dir, e.getMessage());
                        }
                    });
        }
    }
}