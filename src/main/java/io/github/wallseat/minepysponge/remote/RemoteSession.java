package io.github.wallseat.minepysponge.remote;

import org.apache.logging.log4j.Logger;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Optional;

public class RemoteSession {
    private final Logger logger;
    private final Socket socket;
    private BufferedWriter outStream;
    private BufferedReader inStream;
    private boolean isPresent;
    private boolean pendingClose;
    private final ArrayDeque<String> inQueue = new ArrayDeque<>();
    private final ArrayDeque<String> outQueue = new ArrayDeque<>();
    private Thread inThread;
    private Thread outThread;
    private final int maxCommandsPerTick = 1000;


    public RemoteSession(Socket socket, Logger logger) throws IOException {
        this.logger = logger;
        this.socket = socket;

        open();
    }

    private void open() throws IOException {
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        socket.setTrafficClass(0x10);
        outStream = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        inStream = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        isPresent = true;

        startThreads();
    }

    public void close() {
        if (!isPresent) return;
        isPresent = false;

        try {
            inThread.join(2000);
            outThread.join(2000);
        } catch (InterruptedException e) {
            logger.warn("Failed to stop in/out thread");
            e.printStackTrace();
        }

        try {
            inStream.close();
            outStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void startThreads() {
        inThread = new Thread(new InputThread());
        inThread.start();
        outThread = new Thread(new OutputThread());
        outThread.start();
    }

    private class InputThread implements Runnable {
        public void run() {
            while (isPresent) {
                try {
                    String newLine = inStream.readLine();
                    if (newLine == null) {
                        isPresent = false;
                    } else {
                        inQueue.add(newLine);
                    }
                } catch (Exception e) {
                    if (isPresent) {
                        if (e.getMessage().equals("Connection reset")) {
                            logger.info("Connection reset");
                        } else {
                            e.printStackTrace();
                        }
                        isPresent = false;
                    }
                }
            }
            close();
        }
    }

    private class OutputThread implements Runnable {
        public void run() {
            while (isPresent) {
                try {
                    String line;
                    while ((line = outQueue.poll()) != null) {
                        outStream.write(line);
                        outStream.write('\n');
                    }
                    outStream.flush();
                    Thread.yield();
                    Thread.sleep(2L);
                } catch (Exception e) {
                    if (isPresent) {
                        e.printStackTrace();
                        isPresent = false;
                    }
                }
            }
            close();
        }
    }

    public InetAddress getAddress() {
        return socket.getInetAddress();
    }

    public void kick(String reason) {
        try {
            outStream.write(reason);
            outStream.flush();
            close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public boolean isPresent() {
        return isPresent;
    }

    public boolean isPendingClose() {
        return pendingClose;
    }

    public void process() {
        int processedCount = 0;
        String message;
        while ((message = inQueue.poll()) != null) {
            handleMsg(message);
            processedCount++;
            if (processedCount >= maxCommandsPerTick) {
                logger.warn(
                        "Over " + maxCommandsPerTick +
                                " commands were queued - deferring " + inQueue.size() + " to next tick");
                break;
            }
        }

        if (!isPresent && inQueue.size() <= 0) {
            pendingClose = true;
        }
    }

    public boolean haveUnprocessed() {
        return !inQueue.isEmpty();
    }

    private void handleMsg(String msg) {
        Command command = Command.fromMsg(msg);

        this.logger.info("Receive command: " + command);

        if (command.command.equals("world.setBlock")) {

            String worldName = command.args[0];
            int x = Integer.parseInt(command.args[1]);
            int y = Integer.parseInt(command.args[2]);
            int z = Integer.parseInt(command.args[3]);

            Optional<BlockType> blockType = Optional.empty();
            if (command.args[4].contains(":")) {
                String[] id = command.args[4].split(":");
                blockType = BlockTypes.registry().findValue(ResourceKey.of(id[0], id[1]));
            }

            if (!blockType.isPresent()) {
                outQueue.add("Invalid block type - " + command.args[4]);
                return;
            }

            Optional<ServerWorld> world = Sponge
                    .server()
                    .worldManager()
                    .world(
                            ResourceKey
                                    .builder()
                                    .namespace("minecraft")
                                    .value(worldName)
                                    .build()
                    );

            if (!world.isPresent()) {
                outQueue.add("Can't place block, world not found");
                return;
            }

            ServerLocation blockLoc = world.get().location(x, y, z);
            blockLoc.setBlockType(blockType.get());

        }

        outQueue.add("Ok");
    }
}

