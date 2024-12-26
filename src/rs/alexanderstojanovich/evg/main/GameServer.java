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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.DatagramAcceptor;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.intrface.Command;
import rs.alexanderstojanovich.evg.level.LevelActors;
import rs.alexanderstojanovich.evg.net.ClientInfo;
import rs.alexanderstojanovich.evg.net.DSMachine;
import rs.alexanderstojanovich.evg.net.DSObject;
import rs.alexanderstojanovich.evg.net.Response;
import rs.alexanderstojanovich.evg.net.ResponseIfc;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 * Demolition Synergy Game Server
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class GameServer implements DSMachine, Runnable {

    public static final int TIME_TO_LIVE = 120;
    public static final int FAIL_ATTEMPT_MAX = 10;
    public static final int TOTAL_FAIL_ATTEMPT_MAX = 3000;

    public static int TotalFailedAttempts = 0;

    public static final Configuration config = Configuration.getInstance();
    protected String worldName = "My World";
    public static int DEFAULT_PORT = 13667;
    protected String localIP = config.getLocalIP();
    protected int port = config.getServerPort();

    protected static final int MAX_CLIENTS = config.getMaxClients();

    /**
     * Max total request per second
     */
    public static final int MAX_RPS = 1000; // Max Total Request Per Second

    protected SocketAddress endpoint;

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
     * Timeout to close session (await) after client said "GOODBYE" to
     * disconnect
     */
    public static final long GOODBYE_TIMEOUT = 15000L;

    /**
     * Server util helper (time to live etc.)
     */
    public final ExecutorService serverHelperExecutor = Executors.newSingleThreadExecutor();

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
     * UDP acceptor and session settings
     */
    protected DatagramAcceptor acceptor;

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
                // Decrease time-to-live for each client and remove expired clients
                clients.forEach((ClientInfo client) -> {
                    client.timeToLive--;
                    // if client is OK -- not timing out ; not on kicklist ; not abusing request per second
                    boolean timedOut = client.timeToLive <= 0;
                    boolean maxRPSReached = client.requestPerSecond > MAX_RPS;
                    // max reqest per second reached -> kick client
                    if (maxRPSReached || kicklist.contains(client.uniqueId)) {
                        // issuing kick to the client (guid as data) ~ best effort if has not successful first time
                        GameServer.kickPlayer(GameServer.this, client.uniqueId);
                    }

                    // timed out -> just clean up resources
                    if (timedOut) {
                        try {
                            // close session (with the client)
                            client.session.closeNow().await(GameServer.GOODBYE_TIMEOUT);

                            // clean up server from client data
                            GameServer.performCleanUp(GameServer.this.gameObject, client.uniqueId, true);

                            // remove from kicklist he/she timed out
                            kicklist.remove(client.uniqueId);
                        } catch (InterruptedException ex) {
                            DSLogger.reportError(ex.getMessage(), ex);
                        }
                    }

                    // reset request per second (RPS)
                    client.requestPerSecond = 0;
                });

                // Remove kicked and timed out players
                clients.removeIf(cli -> cli.timeToLive <= 0);

                // Update server window title with current player count
                GameServer.this.gameObject.WINDOW.setTitle(GameObject.WINDOW_TITLE + " - " + GameServer.this.worldName + " - Player Count: " + (1 + GameServer.this.clients.size()));
            }
        };
        timerClientChk.scheduleAtFixedRate(task1, 1000L, 1000L);

        serverHelperExecutor.execute(this);

        DSLogger.reportInfo(String.format("Commencing start of Game Server. Game Server will start on %s:%d", localIP, port), null);
    }

    /**
     * Stop running server endpoint. Server would have to be start again.
     */
    public void stopServer() {
        if (running) {
            // Kick all players
            clients.forEach(cli -> GameServer.kickPlayer(gameObject.gameServer, cli.uniqueId));

            // Reset server window title
            gameObject.WINDOW.setTitle(GameObject.WINDOW_TITLE);
            this.shutDownSignal = true;

            // close session(s) & acceptor
            acceptor.setCloseOnDeactivation(true);
            for (IoSession ss : acceptor.getManagedSessions().values()) {
                try {
                    ss.closeNow().await(GameServer.GOODBYE_TIMEOUT);
                } catch (InterruptedException ex) {
                    DSLogger.reportError("Unable to close session!", ex);
                    DSLogger.reportError(ex.getMessage(), ex);
                }
            }
            acceptor.unbind();
            acceptor.dispose();

            // Clear the client list
            clients.clear();
            running = false;

            DSLogger.reportInfo("Game Server finished!", null);
            gameObject.intrface.getConsole().write("Game Server finished!");
        }
    }

    /**
     * Shut down execution service(s). Server is not available anymore.
     */
    public void shutDown() {
        this.serverHelperExecutor.shutdown();
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
        if (filtered != null && ++filtered.failedAttempts >= FAIL_ATTEMPT_MAX && !blacklist.contains(failedHostName)) {
            blacklist.add(failedHostName);
            gameObject.intrface.getConsole().write(String.format("Client (%s) is now blacklisted!", failedHostName));
            DSLogger.reportWarning(String.format("Game Server (%s) is now blacklisted!", failedHostName), null);
        }

        // Too much failed attempts, endpoint is vulnerable .. try to shut down
        if (++TotalFailedAttempts >= TOTAL_FAIL_ATTEMPT_MAX) {
            gameObject.intrface.getConsole().write(String.format("Game Server (%s:%d) status critical! Trying to shut down!", this.localIP, this.port));
            DSLogger.reportWarning(String.format("Game Server (%s:%d) status critical! Trying to shut down!", this.localIP, this.port), null);

            stopServer();
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
            endpoint = new InetSocketAddress(InetAddress.getByName(localIP), port);

            // Configure the UDP acceptor and session settings
            acceptor = new NioDatagramAcceptor();
            DatagramSessionConfig sessionConfig = acceptor.getSessionConfig();
            sessionConfig.setReuseAddress(true);

            // Set the handler for incoming messages
            GameServerProcessor processor = new GameServerProcessor(GameServer.this);
            acceptor.setHandler(processor);
            acceptor.bind(endpoint);

            // Update server window title with current player count
            gameObject.WINDOW.setTitle(GameObject.WINDOW_TITLE + " - " + worldName + " - Player Count: " + (1 + clients.size()));
            DSLogger.reportInfo(String.format("Game Server (%s:%d) started!", this.localIP, this.port), null);
            gameObject.intrface.getConsole().write(String.format("Game Server (%s:%d) started!", this.localIP, this.port));
        } catch (IOException ex) {
            // Handle server creation failure
            DSLogger.reportError("Cannot create Game Server!", ex);
            gameObject.intrface.getConsole().write("Cannot create Game Server!", Command.Status.FAILED);
            DSLogger.reportError(ex.getMessage(), ex);
            shutDownSignal = true;
        }
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
        gameObject.intrface.getConsole().write(String.format(isError ? "Player %s timed out." : "Player %s disconnected.", uniqueId), isError ? Command.Status.WARNING : Command.Status.SUCCEEDED);
    }

    public ClientInfo[] getClientInfo() {
        ClientInfo[] result = new ClientInfo[clients.size()];
        clients.toArray(result);

        return result;
    }

    /**
     * Issue kick to the client. Client will be force to disconnect and it will
     * be cleaned up.
     *
     * @param gameServer game server managing
     * @param playerGuid player guid (16 chars) to be kicked
     */
    public static void kickPlayer(GameServer gameServer, String playerGuid) {
        final ClientInfo clientInfo;
        if ((clientInfo = gameServer.clients.getIf(cli -> cli.uniqueId.equals(playerGuid))) != null) {
            try {
                // issuing kick to the client (guid as data)
                ResponseIfc response = new Response(0L, ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, "KICK");
                response.send(clientInfo.uniqueId, gameServer, clientInfo.session);

                // close session (with the client)
                clientInfo.session.closeNow().await(GameServer.GOODBYE_TIMEOUT);
//
                // clean up server from client data
                GameServer.performCleanUp(gameServer.gameObject, clientInfo.uniqueId, false);
                // add to kick list for later removal from client list
                gameServer.kicklist.addIfAbsent(playerGuid);
            } catch (Exception ex) {
                DSLogger.reportError(String.format("Error during kick client %s !", clientInfo.uniqueId), ex);
                DSLogger.reportError(ex.getMessage(), ex);
            }
        }
    }

    public String getWorldName() {
        return worldName;
    }

    public int getPort() {
        return port;
    }

    public SocketAddress getEndpoint() {
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

    public void setEndpoint(SocketAddress endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public int getTimeout() {
        return timeout;
    }

    public ExecutorService getServerHelperExecutor() {
        return serverHelperExecutor;
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
