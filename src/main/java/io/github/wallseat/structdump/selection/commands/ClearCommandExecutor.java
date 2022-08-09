package io.github.wallseat.structdump.selection.commands;

import io.github.wallseat.structdump.selection.SelectionListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.entity.living.player.Player;

public class ClearCommandExecutor implements CommandExecutor {

    private final SelectionListener listener;
    private final boolean all;

    public ClearCommandExecutor(SelectionListener listener, boolean all) {
        this.listener = listener;
        this.all = all;
    }

    @Override
    public CommandResult execute(CommandContext context) {
        Component clearedText;

        if (this.all) {
            listener.clearAreas();
            listener.clearCurrentArea();
            clearedText = Component.text("Cleared all saved areas");
        } else {
            listener.clearCurrentArea();
            clearedText = Component.text("Cleared current selection");
        }

        context.contextCause()
                .first(Player.class)
                .ifPresent(
                        (player) -> player.sendMessage(
                                clearedText.color(NamedTextColor.LIGHT_PURPLE)
                        )
                );

        return CommandResult.success();
    }
}
