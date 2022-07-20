package io.github.wallseat.minepysponge;

import com.google.inject.Inject;
import io.github.wallseat.minepysponge.remote.WatcherTask;
import io.github.wallseat.minepysponge.selection.SelectionListener;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.StartingEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.util.UUID;

@Plugin("minepy-sponge")
public class MinePySponge {
    private final PluginContainer container;
    private final Logger logger;
    private UUID watcherTaskUUID;

    @Inject
    MinePySponge(final PluginContainer container, final Logger logger) {
        this.container = container;
        this.logger = logger;
    }

    @Listener
    public void onConstructPlugin(final ConstructPluginEvent event) {
        this.logger.info("Constructing RaspberryJuiceSponge");

        Sponge.eventManager().registerListeners(this.container, new SelectionListener(this));
    }

    @Listener
    public void onServerStarting(final StartingEngineEvent<Server> event) {
        this.watcherTaskUUID = scheduleWatcherTask().uniqueId();

    }

    @Listener
    public void onServerStopping(final StoppingEngineEvent<Server> event) {
        Sponge.asyncScheduler().findTask(watcherTaskUUID).ifPresent(ScheduledTask::cancel);
    }


    private ScheduledTask scheduleWatcherTask() {
        logger.info("Running watcher thread");
        return Sponge.server().scheduler().submit(
                Task.builder()
                        .execute(new WatcherTask(this.logger))
                        .interval(Ticks.single())
                        .plugin(this.container)
                        .build()
        );
    }

    public Logger getLogger() {
        return logger;
    }

    public PluginContainer getContainer() {
        return this.container;
    }
}

