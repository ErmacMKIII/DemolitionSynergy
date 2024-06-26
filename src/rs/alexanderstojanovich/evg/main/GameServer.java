/*
 * Copyright (C) 2024 Alexander Stojanovich <coas91@rocketmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package rs.alexanderstojanovich.evg.main;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.level.LevelActors;
import rs.alexanderstojanovich.evg.net.ClientInfo;
import rs.alexanderstojanovich.evg.net.DSMachine;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 * Demolition Synergy Game Server
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class GameServer implements DSMachine, Runnable {

    public static final int TIME_TO_LIVE = 90;
    public static final int FAIL_ATTEMPT_MAX = 10;
    public static final int TOTAL_FAIL_ATTEMPT_MAX = 3000;

    public static int TotalFailedAttempts = 0;

    public final Configuration config = Configuration.getInstance();
    protected String worldName = "My World";
    public static int DEFAULT_PORT = 13667;
    protected String localIP = config.getLocalIP();
    protected int port = config.getServerPort();

    protected static final int MAX_CLIENTS = 16;

    protected DatagramSocket endpoint;

    /**
     * Client list with IPs (or hostnames)
     */
    public final IList<ClientInfo> clients = new GapList<>();

    protected final GameObject gameObject;

    /**
     * Is (game) server running..
     */
    protected volatile boolean running = false;

    /**
     * Shutdown signal. To stop the (game) server.
     */
    protected boolean shutDownSignal = false;
    protected final int version = GameObject.VERSION;
    protected final int timeout = 120 * 1000; // 2 minutes

    /**
     * Magic bytes of End-of-Stream
     */
    public static final byte[] EOS = {(byte) 0xAB, (byte) 0xCD, (byte) 0x0F, (byte) 0x15}; // 4 Bytes

    /**
     * Server worker
     */
    public final ExecutorService serverExecutor = Executors.newSingleThreadExecutor();

    /**
     * Blacklisted hosts with number of attempts
     */
    public final IList<String> blacklist = new GapList<>();

    /**
     * Kick list hosts with number of attempts
     */
    public final IList<String> kicklist = new GapList<>();

    /**
     * Create new game server (UDP protocol based)
     *
     * @param gameObject game object
     */
    public GameServer(GameObject gameObject) {
        this.gameObject = gameObject;
    }

    /**
     * Schedule timer task to check clients (Time-To-Live)
     */
    public final Timer timerClientChk = new Timer("Server Utils");

    /**
     * Create new game server (UDP protocol based)
     *
     * @param gameObject game object
     * @param name world name
     */
    public GameServer(GameObject gameObject, String name) {
        this.gameObject = gameObject;
        this.worldName = name;
    }

    /**
     * Start endpoint.
     */
    public void startServer() {
        this.shutDownSignal = false;

        // Schedule timer task to decrease Time-to-live for clients
        TimerTask task1 = new TimerTask() {
            @Override
            public void run() {
                // hosts to remove (init empty)
                final GapList<String> host2Rem = new GapList<>();

                // iterate through clients and check banlis & kicklist and time-to-live
                clients.forEach(((ClientInfo client) -> {
                    if (blacklist.contains(client.hostName) || kicklist.contains(client.uniqueId) || --client.timeToLive <= 0) {
                        host2Rem.add(client.hostName);
                        GameServer.performCleanUp(GameServer.this.gameObject, client.uniqueId, client.timeToLive <= 0);
                    }
                }));

                clients.removeIf(cli -> host2Rem.contains(cli.hostName));

                GameServer.this.gameObject.WINDOW.setTitle(GameObject.WINDOW_TITLE + " - " + GameServer.this.worldName + " - Player Count: " + (GameServer.this.clients.size()));
            }
        };
        timerClientChk.scheduleAtFixedRate(task1, 1000L, 1000L);

        serverExecutor.execute(this);

        DSLogger.reportInfo(String.format("Commencing start of Game Server. Game Server will start on %s:%d", localIP, port), null);
    }

    /**
     * Stop running server endpoint. Server would have to be start again.
     */
    public void stopServer() {
        if (running) {
            // Attempt to disconnect clients            
            this.endpoint.close();

            gameObject.WINDOW.setTitle(GameObject.WINDOW_TITLE);
            this.shutDownSignal = true;

            clients.clear();
        }
    }

    /**
     * Shut down execution service(s). Server is not available anymore.
     */
    public void shutDown() {
        this.serverExecutor.shutdown();
//        this.serverTaskExecutor.shutdown();
        this.timerClientChk.cancel();
    }

    /**
     * Assert that failure has happen and client timed out or is about to be
     * rejected. In other words client will fail the test.
     *
     * @param failedHostName client who is submit to test
     * @param failedGuid player guid who submit (with hostname)
     */
    public void assertTstFailure(String failedHostName, String failedGuid) {
        // Filter those who failed
        ClientInfo filtered = clients.getIf(client -> client.hostName.equals(failedHostName) && client.uniqueId.equals(failedGuid));

        // Blacklisting (equals ban)
        if (++filtered.failedAttempts >= FAIL_ATTEMPT_MAX && !blacklist.contains(failedHostName)) {
            blacklist.add(failedHostName);
            gameObject.intrface.getConsole().write(String.format("Client (%s) is now blacklisted!", failedHostName));
            DSLogger.reportWarning(String.format("Game Server (%s) is now blacklisted!", failedHostName), null);
        }

        // Too much failed attempts, endpoint is vulnerable .. try to shut down
        if (++TotalFailedAttempts >= TOTAL_FAIL_ATTEMPT_MAX) {
            gameObject.intrface.getConsole().write(String.format("Game Server (%s:%d) status critical! Trying to shut down!", this.localIP, this.port));
            DSLogger.reportWarning(String.format("Game Server (%s:%d) status critical! Trying to shut down!", this.localIP, this.port), null);
            shutDownSignal = true;
        }
    }

    /**
     * Server loop
     */
    @Override
    public void run() {
        running = true;
        try {
            // Bind the endpoint socket to a 'wildcard' IP address amd given port
            endpoint = new DatagramSocket(port, InetAddress.getByName(localIP));
            gameObject.WINDOW.setTitle(GameObject.WINDOW_TITLE + " - " + worldName + " - Player Count: " + (clients.size()));
            DSLogger.reportInfo(String.format("Game Server (%s:%d) started!", this.localIP, this.port), null);
            gameObject.intrface.getConsole().write(String.format("Game Server (%s:%d) started!", this.localIP, this.port));
        } catch (IOException ex) {
            DSLogger.reportError("Cannot create Game Server!", ex);
            gameObject.intrface.getConsole().write("Cannot create Game Server!", true);
            DSLogger.reportError(ex.getMessage(), ex);
            shutDownSignal = true;
        }
        // Accept incoming connections and handle them
        while (!shutDownSignal) {
            try {
                GameServerProcessor.Result procResult = GameServerProcessor.process(this, endpoint);
                final String msg;
                switch (procResult.status) {
                    case INTERNAL_ERROR:
                        msg = String.format("Server %s %s %s error!", procResult.hostname, procResult.guid, procResult.message);
                        DSLogger.reportError(msg, null);
                        gameObject.intrface.getConsole().write(msg, true);
                        break;
                    case CLIENT_ERROR:
                        assertTstFailure(procResult.hostname, procResult.guid);
                        msg = String.format("Client %s %s %s error!", procResult.hostname, procResult.guid, procResult.message);
                        DSLogger.reportError(msg, null);
                        gameObject.intrface.getConsole().write(msg, true);
                        if (blacklist.contains(procResult.hostname)) {
                            DSLogger.reportWarning(msg, null);
                            gameObject.intrface.getConsole().write(msg, false);
                        }
                        break;
                    default:
                    case OK:
                        clients.filter(client -> client.hostName.equals(procResult.hostname) && client.getUniqueId().equals(procResult.guid))
                                .forEach(client2 -> client2.timeToLive = GameServer.TIME_TO_LIVE);
                        msg = String.format("Client %s %s %s OK", procResult.hostname, procResult.guid, procResult.message);
                        DSLogger.reportInfo(msg, null);
                        gameObject.intrface.getConsole().write(msg, false);
                        break;
                }
            } catch (Exception ex) {
                DSLogger.reportError("Server error: " + ex.getMessage(), ex);
            }
        }

        if (endpoint != null && !endpoint.isClosed()) {
            endpoint.close(); // Handle exceptions
        }
        shutDownSignal = true;

        clients.clear();
        running = false;
        DSLogger.reportInfo("Game Server finished!", null);
        gameObject.intrface.getConsole().write("Game Server finished!");
    }

    /**
     * Perform clean up after player has disconnected or lost connection.
     *
     * @param gameObject game object
     * @param uniqueId player unique id (which was registered)
     * @param isError was client disconnected with error (timed out)
     */
    public static void performCleanUp(GameObject gameObject, String uniqueId, boolean isError) {
        LevelActors levelActors = gameObject.game.gameObject.levelContainer.levelActors;

        levelActors.otherPlayers.removeIf(ply -> ply.uniqueId.equals(uniqueId));
        DSLogger.reportInfo(String.format(isError ? "Player %s timed out." : "Player %s disconnected.", uniqueId), null);
        gameObject.intrface.getConsole().write(String.format(isError ? "Player %s timed out." : "Player %s disconnected.", uniqueId), isError);
    }

    public ClientInfo[] getClientInfo() {
        ClientInfo[] result = new ClientInfo[clients.size()];
        clients.toArray(result);

        return result;
    }

    public static void kickPlayer(GameServer gameServer, String playerGuid) {
        if (gameServer.clients.containsIf(client -> client.uniqueId.equals(playerGuid)) && !gameServer.kicklist.contains(playerGuid)) {
            gameServer.kicklist.add(playerGuid);
        }
    }

    public String getWorldName() {
        return worldName;
    }

    public int getPort() {
        return port;
    }

    public DatagramSocket getEndpoint() {
        return endpoint;
    }

    public IList<ClientInfo> getClients() {
        return clients;
    }

    public GameObject getGameObject() {
        return gameObject;
    }

    public boolean isShutDownSignal() {
        return shutDownSignal;
    }

    public void setShutDownSignal(boolean shutDownSignal) {
        this.shutDownSignal = shutDownSignal;
    }

    @Override
    public MachineType getMachineType() {
        return MachineType.DSSERVER;
    }

    @Override
    public int getVersion() {
        return this.version;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setEndpoint(DatagramSocket endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public int getTimeout() {
        return timeout;
    }

    public ExecutorService getServerExecutor() {
        return serverExecutor;
    }

    public IList<String> getBlacklist() {
        return blacklist;
    }

    public Timer getTimerClientChk() {
        return timerClientChk;
    }

    public void setLocalIP(String localIP) {
        this.localIP = localIP;
    }

    public String getLocalIP() {
        return localIP;
    }

    public IList<String> getKicklist() {
        return kicklist;
    }

    @Override
    public String getGuid() {
        return "*";
    }

}
