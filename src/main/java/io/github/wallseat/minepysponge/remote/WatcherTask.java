package io.github.wallseat.minepysponge.remote;

import io.github.wallseat.minepysponge.remote.ConnectionListener;
import io.github.wallseat.minepysponge.remote.RemoteSession;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.service.ban.Ban;
import org.spongepowered.api.service.ban.BanService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class WatcherTask implements Consumer<ScheduledTask> {
    private final Logger logger;
    private ConnectionListener connectionListener;
    private final InetSocketAddress address;
    private final List<RemoteSession> sessions = new ArrayList<>();

    public WatcherTask(Logger logger) {
        this.logger = logger;

        this.address = new InetSocketAddress(25577);
    }

    @Override
    public void accept(ScheduledTask task) {
        checkConnectionListenerThread();
        processSessions();
    }

    private void checkConnectionListenerThread() {
        if (connectionListener == null) {
            createConnectionListenerThread(address);
        } else if (!connectionListener.isRun()) {
            logger.info("Restarting connection listener thread");
            createConnectionListenerThread(address);
        }
    }

    private void createConnectionListenerThread(InetSocketAddress address) {
        try {
            connectionListener = new ConnectionListener(this, address, logger);
            connectionListener.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void processSessions() {
        Iterator<RemoteSession> iSessions = sessions.iterator();
        while (iSessions.hasNext()) {
            RemoteSession session = iSessions.next();
            if (!session.isPresent() || session.isPendingClose()) {
                session.close();
                if (!session.haveUnprocessed()){
                    iSessions.remove();
                    continue;
                }
            }
            session.process();

        }
    }

    public void addSession(RemoteSession newSession) {
        if (checkBanned(newSession)) {
            logger.warn("Attempt to connect from banned IP address: " + newSession.getAddress());
            newSession.kick("You've been banned from this server!");
            return;
        }
        synchronized (sessions) {
            sessions.add(newSession);
        }
    }

    public boolean checkBanned(RemoteSession session) {
        Optional<BanService> service = Sponge.serviceProvider().provide(BanService.class);
        if (service.isPresent()) {
            try {
                Collection<Ban.IP> bannedIps = service.get().ipBans().get();
                if (
                        bannedIps.stream().anyMatch((ip) ->
                                ip.address() == session.getAddress())
                ) return true;

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }


}
