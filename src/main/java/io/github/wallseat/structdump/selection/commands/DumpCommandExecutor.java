package io.github.wallseat.structdump.selection.commands;

import io.github.wallseat.structdump.selection.Area;
import io.github.wallseat.structdump.selection.SelectionListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.world.WorldTypes;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class DumpCommandExecutor implements CommandExecutor {

    private final SelectionListener listener;

    private final CommandException InternalError = new CommandException(
            Component.text("An internal error occurred!").color(NamedTextColor.RED)
    );

    public DumpCommandExecutor(SelectionListener listener) {
        this.listener = listener;
    }

    @Override
    public CommandResult execute(CommandContext context) throws CommandException {
        String dirName = "area_dumps";
        File dir = new File(dirName);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw InternalError;
            }
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
        LocalDateTime now = LocalDateTime.now();

        BufferedWriter writer;
        File file = new File(dirName + "/" + dtf.format(now) + ".dump");

        try {
            writer = new BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            listener.getLogger().error(e);
            throw InternalError;
        }

        int normalizationSide = context.requireOne(Parameter.key("side", Integer.class));

        for (Area area : listener.getAreas()) {

            int sideX = area.getSecondPos().floorX() - area.getFirstPos().floorX() + 1;
            if (sideX > normalizationSide) {
                throw new CommandException(
                        Component.text("Can't fit X side to normalization side for area " + area)
                );
            }
            int deltaX = (normalizationSide - sideX) / 2;

            int sideY = area.getSecondPos().floorY() - area.getFirstPos().floorY() + 1;
            if (sideY > normalizationSide) {
                throw new CommandException(
                        Component.text("Can't fit Y side to normalization side for area " + area)
                );
            }
            int deltaY = (normalizationSide - sideY) / 2;

            int sideZ = area.getSecondPos().floorZ() - area.getFirstPos().floorZ() + 1;
            if (sideZ > normalizationSide) {
                throw new CommandException(
                        Component.text("Can't fit Z side to normalization side for area " + area)
                );
            }
            int deltaZ = (normalizationSide - sideZ) / 2;

            Sponge.server().worldManager().world(WorldTypes.OVERWORLD.location()).ifPresent((world) -> {

                for (int dy = 0; dy <= sideY; dy++) {
                    int curY = area.getFirstPos().floorY() + dy;

                    for (int dz = 0; dz <= sideZ; dz++) {
                        int curZ = area.getFirstPos().floorZ() + dz;

                        for (int dx = 0; dx <= sideX; dx++) {
                            int curX = area.getFirstPos().floorX() + dx;

                            BlockState block = world.locatableBlock(curX, curY, curZ).blockState();
                            if (!block.type().equals(BlockTypes.AIR.get())) {
                                Optional<ResourceKey> blockType = BlockTypes.registry().findValueKey(block.type());

                                if (!blockType.isPresent()) {
                                    try {
                                        throw InternalError;
                                    } catch (CommandException e) {
                                        throw new RuntimeException(e);
                                    }
                                }

                                try {
                                    writer.write((dx + deltaX) + ";" + (dy + deltaY) + ";" + (dz + deltaZ) + ";" + blockType.get().value() + "#");
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }

                            }
                        }
                    }
                }
                try {
                    writer.write("\n");
                } catch (IOException e) {
                    try {
                        throw InternalError;
                    } catch (CommandException ex) {
                        throw new RuntimeException(ex);
                    }

                }
            });

        }

        try {
            writer.close();
        } catch (IOException e) {
            throw InternalError;
        }

        return CommandResult.success();
    }
}
