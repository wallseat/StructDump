package io.github.wallseat.minepysponge.remote;

import java.util.Arrays;

class Command {
    public final String command;
    public final String[] args;

    public Command(String command, String[] args) {
        this.command = command;
        this.args = args;
    }

    public static Command fromMsg(String msg) {
        String[] strings = msg.split(";");
        return new Command(
                strings[0],
                Arrays.copyOfRange(strings, 1, strings.length)
        );
    }

    @Override
    public String toString() {
        return "Command<command: " + this.command + ", args: [" + String.join(", ", this.args) + "]>";
    }
}
