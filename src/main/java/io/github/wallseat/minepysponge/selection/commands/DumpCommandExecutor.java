package io.github.wallseat.minepysponge.selection.commands;

import io.github.wallseat.minepysponge.selection.Area;
import io.github.wallseat.minepysponge.selection.SelectionListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.WorldTypes;
import org.spongepowered.api.world.server.ServerWorld;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Optional;

public class DumpCommandExecutor implements CommandExecutor {

    private final SelectionListener listener;

    public DumpCommandExecutor(SelectionListener listener) {
        this.listener = listener;
    }

    @Override
    public CommandResult execute(CommandContext context) throws  CommandException {
        String dirName = "area_dumps";
        File dir = new File(dirName);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
        LocalDateTime now = LocalDateTime.now();

        BufferedWriter writer;
        File file = new File(dirName + "/" + dtf.format(now) + ".dump");

        try {
            writer = new BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            listener.getLogger().error(e);
            return CommandResult.error(
                    Component.text("An internal error occurred!").color(NamedTextColor.RED)
            );
        }

        for (Area area : listener.getAreas()) {
            listener.getLogger().info("found area");
            Sponge.server().worldManager().world(WorldTypes.OVERWORLD.location()).ifPresent((world) -> {

                listener.getLogger().info("found world of area");
                for (int dy = 0; dy <= area.getSecondPos().floorY() - area.getFirstPos().floorY() + 1; dy++) {
                    int curY = area.getFirstPos().floorY() + dy;

                    for (int dz = 0; dz <= area.getSecondPos().floorZ() - area.getFirstPos().floorZ() + 1; dz++) {
                        int curZ = area.getFirstPos().floorZ() + dz;

                        for (int dx = 0; dx <= area.getSecondPos().floorX() - area.getFirstPos().floorX() + 1; dx++) {
                            int curX = area.getFirstPos().floorX() + dx;

                            BlockState block = world.locatableBlock(curX, curY, curZ).blockState();
                            if (!block.type().equals(BlockTypes.AIR.get())) {
                                try {
                                    writer.write(dx + ";" + dy + ";" + dz + ";" + BlockTypes.registry().findValueKey(block.type()).get().value() + "#");
                                } catch (IOException e) {
                                    throw new RuntimeException(e); // TODO
                                }
                            }
                        }
                    }
                }
                try {
                    writer.write("\n");
                } catch (IOException e) {
                    throw new RuntimeException(e); // TODO
                }
            });

        }

        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return CommandResult.success();
    }
}
