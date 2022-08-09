package io.github.wallseat.structdump.selection.commands;

import io.github.wallseat.structdump.selection.SelectionListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.entity.living.player.Player;

public class SaveCommandExecutor implements CommandExecutor {

    private final SelectionListener listener;

    public SaveCommandExecutor(SelectionListener listener) {
        this.listener = listener;
    }

    @Override
    public CommandResult execute(CommandContext context) throws CommandException {
        try {
            listener.saveCurrentArea();
            listener.clearCurrentArea();
            context.contextCause()
                    .first(Player.class)
                    .ifPresent(
                            (player) -> player.sendMessage(
                                    Component.text("Saved!").color(NamedTextColor.LIGHT_PURPLE)
                            )
                    );
            return CommandResult.success();

        } catch (Exception e) {
            e.printStackTrace();
            throw new CommandException(
                    Component.text("An internal error occurred!\n" + e.getMessage())
                            .color(NamedTextColor.RED));
        }


    }
}
