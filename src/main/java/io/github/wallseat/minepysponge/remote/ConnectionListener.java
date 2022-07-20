package io.github.wallseat.minepysponge.remote;

import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.*;

public class ConnectionListener extends Thread {
    private final WatcherTask watcher;
    private final Logger logger;
    private final ServerSocket serverSocket;
    private boolean isRun;

    public ConnectionListener(WatcherTask watcher, InetSocketAddress address, Logger logger) throws IOException {
        this.watcher = watcher;
        this.logger = logger;

        this.serverSocket = new ServerSocket();
        this.serverSocket.setReuseAddress(true);
        this.serverSocket.bind(address);
        this.isRun = true;
    }


    public void run() {
        logger.info("Connection listener started!");

        while (isRun) {
            try {
                Socket socket = serverSocket.accept();
                if (!isRun) break;
                logger.info(
                        "New connection from " +
                                socket.getInetAddress().getHostAddress() +
                                ":" +
                                socket.getPort()
                );
                RemoteSession newSession = new RemoteSession(socket, logger);
                watcher.addSession(newSession);
            } catch (IOException e) {
                logger.error(e);
                close();
            }
        }
    }

    public void close() {
        this.isRun = false;
    }

    public boolean isRun() {
        return isRun;
    }
}
