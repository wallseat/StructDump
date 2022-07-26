package io.github.wallseat.structdump.selection;

import io.github.wallseat.structdump.selection.commands.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.WorldTypes;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.plugin.PluginContainer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;


public class SelectionListener {

    private final Logger logger;
    private final PluginContainer pluginContainer;
    private final ArrayList<Area> areas = new ArrayList<>();
    private UUID scheduledParticleTaskID;
    private Area currentArea;

    public SelectionListener(Logger logger, PluginContainer pluginContainer) {
        this.logger = logger;
        this.pluginContainer = pluginContainer;
    }

    @Listener
    public void registerCommandClear(@NotNull RegisterCommandEvent<Command.Parameterized> event) {
        Command.Parameterized clearAllCommand = Command.builder()
                .extendedDescription(
                        Component.text("Clear all saved areas")
                ).permission("structdump.command.clear.all")
                .executor(
                        new ClearCommandExecutor(this, true)
                ).build();

        Command.Parameterized clearCommand = Command.builder()
                .extendedDescription(Component.text("Clear current area"))
                .permission("structdump.command.clear.base")
                .executor(
                        new ClearCommandExecutor(this, false)
                ).addChild(clearAllCommand, "all")
                .build();

        event.register(pluginContainer, clearCommand, "clear");
    }

    @Listener
    public void registerCommandSave(@NotNull RegisterCommandEvent<Command.Parameterized> event) {
        Command.Parameterized saveCommand = Command.builder()
                .extendedDescription(Component.text("Save current area to areas list"))
                .permission("structdump.command.save")
                .executor(
                        new SaveCommandExecutor(this)
                ).build();

        event.register(pluginContainer, saveCommand, "save");
    }

    @Listener
    public void registerCommandList(@NotNull RegisterCommandEvent<Command.Parameterized> event) {
        Command.Parameterized listCommand = Command.builder()
                .extendedDescription(Component.text("Print list of current saved areas"))
                .permission("structdump.command.list")
                .executor(
                        new ListCommandExecutor(this)
                ).build();

        event.register(pluginContainer, listCommand, "list");
    }

    @Listener
    public void registerCommandPos(@NotNull RegisterCommandEvent<Command.Parameterized> event) {
        Command.Parameterized listCommand = Command.builder()
                .extendedDescription(Component.text("Set pos of player"))
                .permission("structdump.command.pos")
                .addParameter(
                        Parameter.choices("first", "second", "1", "2")
                                .key("posKey")
                                .build()
                ).executor(
                        new PosCommandExecutor(this)
                ).build();

        event.register(pluginContainer, listCommand, "pos");
    }

    @Listener
    public void registerCommandDump(@NotNull RegisterCommandEvent<Command.Parameterized> event) {
        Command.Parameterized dumpCommand = Command.builder()
                .extendedDescription(
                        Component.text("Fit areas to normalization side and dump to file")
                ).permission("structdump.command.dump")
                .addParameter(
                        Parameter.integerNumber()
                                .key("side")
                                .build()
                ).addParameter(
                        Parameter.literal(Boolean.class, true, "with-y")
                                .optional()
                                .key("with-y")
                                .build()
                )
                .executor(
                        new DumpCommandExecutor(this)
                ).build();

        event.register(pluginContainer, dumpCommand, "dump");
    }

    @Listener
    public void onBlockInteract(@NotNull InteractBlockEvent event, @First Player player) throws Exception {
        EventContext context = event.cause().context();
        BlockSnapshot snapshot = context.require(EventContextKeys.BLOCK_HIT);

        if (!context.require(EventContextKeys.USED_ITEM).type().equals(ItemTypes.STICK.get())) return;

        ServerLocation loc;
        if (snapshot.location().isPresent()) {
            loc = snapshot.location().get();
        } else {
            throw new Exception("Unreachable");
        }

        if (context.containsKey(EventContextKeys.PLAYER_PLACE)) { // R_MOUSE
            updateArea(null, loc);
        } else { // L_MOUSE
            updateArea(loc, null);
        }


        posSelectedPrinter(player, loc);
    }

    @Listener
    public void onBlockBreak(ChangeBlockEvent.@NotNull All event, @First Player player) {
        EventContext context = event.cause().context();

        if (context.require(EventContextKeys.USED_ITEM).type().equals(ItemTypes.STICK.get())) {
            event.setCancelled(true);
        }
    }

    public void updateArea(ServerLocation first, ServerLocation second) {
        Vector3d pos;

        if (first != null) {
            pos = first.position().ceil();

            if (currentArea == null) {
                currentArea = Area.fromFirstPos(pos, first.world().key());
            } else {
                currentArea.setFirstPos(pos);
            }


        }

        if (second != null) {
            pos = second.position().round();

            if (currentArea == null) {
                currentArea = Area.fromSecondPos(pos, second.world().key());
            } else {
                currentArea.setSecondPos(pos);
            }

        }

        if (currentArea.isFullFilled()) {
            scheduleParticleTask();
        }

    }

    private void scheduleParticleTask() {
        stopParticleTask();

        scheduledParticleTaskID = Sponge.server()
                .scheduler()
                .submit(
                        Task.builder()
                                .execute(
                                        () -> {
                                            Optional<ServerWorld> world = Sponge.server()
                                                    .worldManager()
                                                    .world(
                                                            WorldTypes.OVERWORLD.location()
                                                    );

                                            if (!world.isPresent()) {
                                                throw new RuntimeException("Can't find world" + WorldTypes.OVERWORLD);
                                            }

                                            Area area;
                                            try {
                                                area = currentArea.normalized();
                                            } catch (Exception ignored) {
                                                return;
                                            }

                                            int x1 = area.getFirstPos().floorX();
                                            int x2 = area.getSecondPos().floorX();
                                            int z1 = area.getFirstPos().floorZ();
                                            int z2 = area.getSecondPos().floorZ();
                                            int y1 = area.getFirstPos().floorY();
                                            int y2 = area.getSecondPos().floorY();

                                            ParticleEffect particleEffect = ParticleEffect.builder()
                                                    .type(ParticleTypes.END_ROD)
                                                    .quantity(50).build();

                                            for (int dy = 0; dy <= y2 - y1 + 1; dy++) {
                                                world.get().spawnParticles(
                                                        particleEffect,
                                                        new Vector3d(x1 + 0.5, y1 + dy + 0.5, z1 + 0.5)
                                                );
                                                world.get().spawnParticles(
                                                        particleEffect,
                                                        new Vector3d(x1 + 0.5, y1 + dy + 0.5, z2 + 0.5)
                                                );
                                                world.get().spawnParticles(
                                                        particleEffect,
                                                        new Vector3d(x2 + 0.5, y1 + dy + 0.5, z1 + 0.5)
                                                );
                                                world.get().spawnParticles(
                                                        particleEffect,
                                                        new Vector3d(x2 + 0.5, y1 + dy + 0.5, z2 + 0.5)
                                                );
                                            }

                                        }
                                )
                                .interval(
                                        Duration.ofSeconds(1)
                                ).plugin(
                                        pluginContainer
                                ).build()
                ).uniqueId();
    }

    private void stopParticleTask() {
        if (scheduledParticleTaskID != null) {
            Sponge.asyncScheduler().findTask(scheduledParticleTaskID).ifPresent(ScheduledTask::cancel);
            scheduledParticleTaskID = null;
        }
    }

    public void posSelectedPrinter(Player player, @NotNull ServerLocation location) throws Exception {
        Component text = Component.text(
                "selected pos " + location.position().round()
        );

        if (currentArea.isFullFilled()) {
            Component selectedAreaExtra = Component.text(
                    ", selection " + currentArea.size()
            );
            text = text.append(selectedAreaExtra);
        }

        player.sendMessage(text.color(NamedTextColor.LIGHT_PURPLE));
    }

    public void clearCurrentArea() {
        if (currentArea == null) return;
        stopParticleTask();

        this.currentArea = null;
    }

    public void clearAreas() {
        this.areas.clear();
    }

    public void saveCurrentArea() throws Exception {
        if (!currentArea.isFullFilled()) throw new Exception("Area is not full filled!");
        areas.add(currentArea.normalized());
    }

    public ArrayList<Area> getAreas() {
        return areas;
    }

    public Logger getLogger() {
        return logger;
    }
}

