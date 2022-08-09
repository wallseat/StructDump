package io.github.wallseat.structdump;

import com.google.inject.Inject;
import io.github.wallseat.structdump.selection.SelectionListener;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;


@Plugin("struct-dump")
public class StructDump {
    private final PluginContainer container;
    private final Logger logger;

    @Inject
    StructDump(final PluginContainer container, final Logger logger) {
        this.container = container;
        this.logger = logger;
    }

    @Listener
    public void onConstructPlugin(final ConstructPluginEvent event) {
        Sponge.eventManager()
                .registerListeners(
                        this.container,
                        new SelectionListener(this)
                );
    }

    public Logger getLogger() {
        return logger;
    }

    public PluginContainer getContainer() {
        return this.container;
    }
}

