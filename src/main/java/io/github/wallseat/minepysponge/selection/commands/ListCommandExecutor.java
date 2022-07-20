package io.github.wallseat.minepysponge.selection.commands;

import io.github.wallseat.minepysponge.selection.Area;
import io.github.wallseat.minepysponge.selection.SelectionListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.entity.living.player.Player;

public class ListCommandExecutor implements CommandExecutor {
    private final SelectionListener listener;

    public ListCommandExecutor(SelectionListener listener) {
        this.listener = listener;
    }

    @Override
    public CommandResult execute(CommandContext context) {
        if (listener.getAreas().size() == 0) {
            return CommandResult.error(
                    Component.text("No saved areas!").color(NamedTextColor.RED)
            );
        }

        Area curArea;
        Component text = Component.text("Current areas:\n").color(NamedTextColor.LIGHT_PURPLE);

        for (int i = 0; i < listener.getAreas().size(); i++) {
            curArea = listener.getAreas().get(i);

            text = text.append(
                    Component.text((i + 1) + ". " +
                            "From " +
                            curArea.getFirstPos() +
                            ", to " +
                            curArea.getSecondPos() +
                            " in " +
                            curArea.getWorldKey() +
                            "\n"


                    ).color(NamedTextColor.LIGHT_PURPLE)
            );
        }

        Component finalText = text;
        context.contextCause()
                .first(Player.class)
                .ifPresent(
                        (player) -> player.sendMessage(finalText)
                );

        return CommandResult.success();

    }
}
