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
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.level.LevelActors;
import rs.alexanderstojanovich.evg.net.DSMachine;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 * Demolition Synergy Game Server
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class GameServer implements DSMachine, Runnable {

    public static final int FAIL_ATTEMPT_MAX = 10;
    public static final int TOTAL_FAIL_ATTEMPT_MAX = 3000;

    public static int TotalFailedAttempts = 0;

    protected String worldName = "My World";
    protected String host = "localhost";
    public static int DEFAULT_PORT = 13667;
    protected int port = DEFAULT_PORT;

    protected static final int MAX_CLIENTS = 16;

    protected DatagramSocket endpoint;
    public final IList<String> clients = new GapList<>();
    protected final GameObject gameObject;

    protected volatile boolean running = false;
    protected boolean shutDownSignal = false;
    protected final int version = 39;
    protected final int timeout = 120 * 1000; // 30 sec

    public final Object SYNC_OBJ = new Object();

    /**
     * Magic bytes of End-of-Stream
     */
    public static final byte[] EOS = {(byte) 0xAB, (byte) 0xCD, (byte) 0x0F, (byte) 0x15}; // 4 Bytes

    /**
     * Server worker
     */
    public final ExecutorService serverExecutor = Executors.newSingleThreadExecutor();

    /**
     * Who is Client DatagramSocket <==> Player UniqueId
     */
    public final LinkedHashMap<String, String> whoIsMap = new LinkedHashMap<>();

    /**
     * Failed hosts with number of attempts
     */
    public final LinkedHashMap<String, Integer> failedAttempts = new LinkedHashMap<>();

    /**
     * Blacklisted hosts with number of attempts
     */
    public final IList<String> blacklist = new GapList<>();

    /**
     * Create new game server (UDP protocol based)
     *
     * @param gameObject game object
     */
    public GameServer(GameObject gameObject) {
        this.gameObject = gameObject;
    }

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
        serverExecutor.execute(this);

        DSLogger.reportInfo(String.format("Commencing start of Game Server. Game Server will start on %s:%d", host, port), null);
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
            // wakeup client service
            synchronized (SYNC_OBJ) {
                SYNC_OBJ.notify();
            }

            clients.clear();
        }
    }

    /**
     * Shut down execution service. Server is not available anymore.
     */
    public void shutDown() {
        this.serverExecutor.shutdown();
    }

    /**
     * Assert that failure has happen and client timed out or is about to be
     * rejected. In other words client will fail the test.
     *
     * @param failedHostName client who is submit to test
     */
    public void assertTstFailure(String failedHostName) {
        TotalFailedAttempts++;
        boolean contains = this.failedAttempts.containsKey(failedHostName);
        if (!contains) {
            this.failedAttempts.put(failedHostName, 1);
        } else {
            Integer failAttemptNum = this.failedAttempts.get(failedHostName);
            failAttemptNum++;

            // Blacklisting (equals ban)
            if (failAttemptNum >= FAIL_ATTEMPT_MAX && !blacklist.contains(failedHostName)) {
                blacklist.add(failedHostName);
                gameObject.intrface.getConsole().write(String.format("Client (%s) is now blacklisted!", endpoint.getInetAddress().getHostName()));
                DSLogger.reportWarning(String.format("Game Server (%s) is now blacklisted!", endpoint.getInetAddress().getHostName()), null);
            }

            // Too much failed attempts, endpoint is vulnerable .. try to shut down
            if (TotalFailedAttempts >= TOTAL_FAIL_ATTEMPT_MAX) {
                gameObject.intrface.getConsole().write(String.format("Game Server (%s) status critical! Trying to shut down!", endpoint.getInetAddress().getHostName()));
                DSLogger.reportWarning(String.format("Game Server (%s) status critical! Trying to shut down!", endpoint.getInetAddress().getHostName()), null);
                shutDownSignal = true;
            }

            this.failedAttempts.replace(failedHostName, failAttemptNum);
        }
    }

    /**
     * Server loop
     */
    @Override
    public void run() {
        running = true;
        try {
            // Bind the endpoint socket to a specific IP address and port
            endpoint = new DatagramSocket(port/*, InetAddress.getByName(host)*/);
            gameObject.WINDOW.setTitle(GameObject.WINDOW_TITLE + " - " + worldName + " - Player Count: " + (1 + clients.size()));
            DSLogger.reportInfo(String.format("Game Server (%s) started!", this.host), null);
            gameObject.intrface.getConsole().write(String.format("Game Server (%s) started!", this.host));
        } catch (IOException ex) {
            DSLogger.reportError("Cannot create Game Server!", ex);
            gameObject.intrface.getConsole().write("Cannot create Game Server!", true);
            DSLogger.reportError(ex.getMessage(), ex);
            shutDownSignal = true;
        } finally {
            // wakeup client service
            synchronized (SYNC_OBJ) {
                SYNC_OBJ.notify();
            } // so they realise what happened!
        }
        // Accept incoming connections and handle them
        while (!gameObject.WINDOW.shouldClose() && !shutDownSignal) {
            try {
                GameServerProcessor.Result procResult = GameServerProcessor.process(this, endpoint);
                final String msg;
                switch (procResult.status) {
                    case INTERNAL_ERROR:
                        msg = String.format("Server %s error!", procResult.client);
                        DSLogger.reportError(msg, null);
                        gameObject.intrface.getConsole().write(msg, true);
                        break;
                    case CLIENT_ERROR:
                        assertTstFailure(procResult.client);
                        msg = String.format("Client %s error!", procResult.client);
                        DSLogger.reportError(msg, null);
                        gameObject.intrface.getConsole().write(msg, true);
                        if (blacklist.contains(procResult.client)) {
                            DSLogger.reportWarning(msg, null);
                            gameObject.intrface.getConsole().write(msg, false);
                        }
                        break;
                    default:
                    case OK:
                        msg = String.format("OK (%s)", procResult.client);
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

    public String getWorldName() {
        return worldName;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public DatagramSocket getEndpoint() {
        return endpoint;
    }

    public IList<String> getClients() {
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

    public void setHost(String host) {
        this.host = host;
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

    public Object getSYNC_OBJ() {
        return SYNC_OBJ;
    }

}
