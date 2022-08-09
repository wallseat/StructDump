package io.github.wallseat.structdump.selection.commands;

import io.github.wallseat.structdump.selection.SelectionListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.server.ServerLocation;

import java.util.Optional;

public class PosCommandExecutor implements CommandExecutor {
    private final SelectionListener listener;

    public PosCommandExecutor(SelectionListener listener) {
        this.listener = listener;
    }

    @Override
    public CommandResult execute(CommandContext context) throws CommandException {
        Optional<Player> player = context.cause().first(Player.class);

        if (!player.isPresent())
            throw new CommandException(
                    Component.text(
                            "An internal error occurred!" +
                                    "\nInvalid command context cause, player not found!"
                    ).color(NamedTextColor.RED)
            );

        ServerLocation playerLoc = player.get().serverLocation();
        String posKey = context.requireOne(Parameter.key("posKey", String.class));

        try {
            if (posKey.equals("first") || posKey.equals("1")) {
                listener.updateArea(playerLoc, null);
            } else {
                listener.updateArea(null, playerLoc);
            }

            listener.posSelectedPrinter(player.get(), playerLoc);

        } catch (Exception e) {
            e.printStackTrace();
            throw new CommandException(
                    Component.text("An internal error occurred!\n" + e.getMessage())
                            .color(NamedTextColor.RED)
            );
        }

        return CommandResult.success();
    }
}
