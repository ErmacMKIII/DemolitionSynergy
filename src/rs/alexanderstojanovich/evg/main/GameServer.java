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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.level.LevelActors;
import rs.alexanderstojanovich.evg.net.DSMachine;
import rs.alexanderstojanovich.evg.net.DSObject;
import rs.alexanderstojanovich.evg.net.RequestIfc;
import rs.alexanderstojanovich.evg.net.Response;
import rs.alexanderstojanovich.evg.net.ResponseIfc;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 * Demolition Synergy Game Server
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class GameServer implements DSMachine, Runnable {

    protected String worldName = "My World";
    protected String host = "localhost";
    public static int DEFAULT_PORT = 13667;
    protected int port = DEFAULT_PORT;

    protected static final int MAX_CLIENTS = 8;

    protected ServerSocket server;
    public final IList<Socket> clients = new GapList<>();
    protected final GameObject gameObject;

    protected boolean running = false;
    protected boolean shutDownSignal = false;
    protected final int version = 39;
    protected final int timeout = 30 * 1000; // 30 sec

    protected final Object SYNC_OBJ = new Object();

    /**
     * Magic bytes of End-of-Stream
     */
    public static final byte[] EOS = {(byte) 0xAB, (byte) 0xCD, (byte) 0x0F, (byte) 0x15}; // 4 Bytes

    /**
     * Server worker
     */
    public final ExecutorService serverExecutor = Executors.newSingleThreadExecutor();

    /**
     * Client service
     */
    public final ExecutorService clientServiceExecutor = Executors.newFixedThreadPool(MAX_CLIENTS);

    /**
     * Who is Client Socket <==> Player UniqueId
     */
    public final LinkedHashMap<Socket, String> whoIsMap = new LinkedHashMap<>();

    /**
     * Create new game server
     *
     * @param gameObject game object
     */
    public GameServer(GameObject gameObject) {
        this.gameObject = gameObject;
    }

    /**
     * Create new game server
     *
     * @param gameObject game object
     * @param name world name
     */
    public GameServer(GameObject gameObject, String name) {
        this.gameObject = gameObject;
        this.worldName = name;
    }

    /**
     * Start server.
     */
    public void startServer() {
        this.shutDownSignal = false;
        serverExecutor.execute(this);

        DSLogger.reportInfo(String.format("Commencing start of Game Server. Game Server will start on %s:%d", host, port), null);
    }

    /**
     * Stop running server server.
     */
    public void stopServer() {
        if (running) {
            // Attempt to disconnect clients
            for (Socket client : clients) {
                try {
                    client.close();
                } catch (IOException ex) {
                    DSLogger.reportError("Unable to close client!", ex);
                    DSLogger.reportError(ex.getMessage(), ex);
                }
            }

            // Attempt to close the server
            if (server != null && !server.isClosed()) {
                try {
                    server.close();
                } catch (IOException ex) {
                    DSLogger.reportError("Unable to close server!", ex);
                    DSLogger.reportError(ex.getMessage(), ex);
                } finally {
                    // revert back title
                    gameObject.WINDOW.setTitle(GameObject.WINDOW_TITLE);
                    this.shutDownSignal = true;
                    // wakeup client service
                    synchronized (SYNC_OBJ) {
                        SYNC_OBJ.notify();
                    }
                }
            }

            clients.clear();
        }
    }

    /**
     * Shut down execution service. Server is not available anymore.
     */
    public void shutDown() {
        this.clientServiceExecutor.shutdown();
        this.serverExecutor.shutdown();
    }

    /**
     * Open client input stream and receive first request. Acceptance test.
     *
     * @param client client socket (tried to connect)
     * @return
     * @throws java.io.IOException if something happens
     */
    protected boolean tst(Socket client) throws IOException, Exception {
        RequestIfc request = RequestIfc.receive(this, client);
        if (request == null) {
            return false;
        }

        if (request.getRequestType() == RequestIfc.RequestType.HELLO) {
            return true;
        }

        return false;
    }

    /**
     * Open client output stream and send first (welcome) response. Acceptance
     * test passed.
     *
     * @param client client socket (tried to connect)
     * @throws java.io.IOException if something happens
     */
    public void accept(Socket client) throws IOException, Exception {
        // Send a simple message with magic bytes prepended
        String welcome = String.format("Hello, you are connected to %s, v%s, for help write \"help\" without quotes. Welcome!", this.worldName, this.version);

        ResponseIfc response = new Response(ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, welcome);

        response.send(this, client);
        // wakeup client service
        synchronized (SYNC_OBJ) {
            SYNC_OBJ.notify();
        }
    }

    /**
     * Open client output stream and not accepted response. Acceptance test
     * failed.
     *
     * @param client client socket (tried to connect)
     * @throws java.io.IOException if something happens
     */
    public void reject(Socket client) throws IOException, Exception {
        // Send a simple message with magic bytes prepended
        String message = "Sorry, your connection refused!";

        ResponseIfc response = new Response(ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, message);

        response.send(this, client);
    }

    /**
     * Server loop
     */
    @Override
    public void run() {
        running = true;
        try {
            // Bind the server socket to a specific IP address and port
            server = new ServerSocket();
            server.bind(new InetSocketAddress(host, port));
            gameObject.WINDOW.setTitle(GameObject.WINDOW_TITLE + " - " + worldName + " - Player Count: " + (1 + clients.size()));
            DSLogger.reportInfo("Game Server started!", null);
        } catch (IOException ex) {
            DSLogger.reportError("Cannot create Game Server!", ex);
            DSLogger.reportError(ex.getMessage(), ex);
        }
        // Accept incoming connections and handle them
        while (!gameObject.WINDOW.shouldClose() && !shutDownSignal) {
            try {
                final Socket client = server.accept();
                client.setSoTimeout(timeout);
                clients.add(client);
                // Acceptance test (examination)
                if (tst(client) && clients.size() <= MAX_CLIENTS) {
                    // Send "Welcome" Response
                    accept(client); // Authenticated
                    client.setSoTimeout(0);
                    gameObject.WINDOW.setTitle(GameObject.WINDOW_TITLE + " - " + worldName + " - Player Count: " + (1 + clients.size()));
                    // Create handle client task ~ for handling requests and sending responses
                    CompletableFuture.supplyAsync(new HandleClientTask(client, this), this.clientServiceExecutor)
                            .exceptionally(ex -> {
                                // Handle exceptions
                                DSLogger.reportError("Error handling client: " + ex.getMessage(), ex);
                                clients.remove(client);
                                try {
                                    client.close();
                                } catch (IOException ex1) {
                                    DSLogger.reportError(ex1.getMessage(), ex1);
                                }

                                return HandleClientTask.Status.INTERNAL_ERROR;
                            }).whenComplete((result, ex) -> {
                        try {
                            DSLogger.reportInfo(String.format("Client task returned %s!", result), ex);
                            gameObject.intrface.getConsole().write("Client task returned " + result.toString(), result != HandleClientTask.Status.OK);
                            // Code to execute finally
                            // This block will execute regardless of whether the CompletableFuture completed exceptionally or successfully
                            // You can perform cleanup tasks or any other final actions here
                            performCleanUp(gameObject, whoIsMap.get(client));
                            clients.remove(client);
                            client.close();

                            whoIsMap.remove(client);

                            gameObject.WINDOW.setTitle(GameObject.WINDOW_TITLE + " - " + worldName + " - Player Count: " + (1 + clients.size()));
                        } catch (IOException ex1) {
                            DSLogger.reportError(ex1.getMessage(), ex1);
                        }
                    });
                } else {
                    DSLogger.reportError("Acceptance test failure!", null);
                    reject(client);
                    clients.remove(client);
                }
                // Handle the client connection
            } catch (IOException ex) {
                DSLogger.reportError("Network Error(s) Occurred!", ex);
                DSLogger.reportError(ex.getMessage(), ex);
            } catch (Exception ex) {
                DSLogger.reportError("Serialization or Deserialization failed!", ex);
            } finally {
                if (!server.isClosed()) {
                    try {
                        server.close();
                    } catch (IOException ex) {
                        // Handle exceptions
                        DSLogger.reportError("Server error: " + ex.getMessage(), ex);
                    }
                }
                shutDownSignal = true;
            }
        }

        clients.clear();
        running = false;
        DSLogger.reportInfo("Game Server finished!", null);
    }

    /**
     * Perform clean up after player has disconnected or lost connection.
     *
     * @param gameObject game object
     * @param uniqueId player unique id (which was registered)
     */
    public static void performCleanUp(GameObject gameObject, String uniqueId) {
        LevelActors levelActors = gameObject.game.gameObject.levelContainer.levelActors;

        levelActors.otherPlayers.removeIf(ply -> ply.uniqueId.equals(uniqueId));
        DSLogger.reportInfo(String.format("Player %s timed out.", uniqueId), null);
        gameObject.intrface.getConsole().write(String.format("Player %s timed out.", uniqueId));
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

    public ServerSocket getServer() {
        return server;
    }

    public IList<Socket> getClients() {
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

    public void setServer(ServerSocket server) {
        this.server = server;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

}
