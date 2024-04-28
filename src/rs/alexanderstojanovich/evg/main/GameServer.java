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
import java.util.Arrays;
import java.util.List;
import org.magicwerk.brownies.collections.GapList;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class GameServer implements Runnable {

    protected static final String SERVER_NAME = String.format("DSYNERGY-SERVER%s", GameObject.VERSION);
    protected String worldName = "My World";
    protected String host = "localhost";
    protected int port = 13667;

    protected static final int BACKLOG = 16;

    protected ServerSocket server;
    public final List<Socket> clients = new GapList<>();
    protected final GameObject gameObject;

    protected boolean shutDownSignal = false;

    /**
     * Construct new game server
     *
     * @param gameObject game object
     * @param name
     */
    public GameServer(GameObject gameObject, String name) {
        this.gameObject = gameObject;
        this.worldName = name;

    }

    /**
     * Open client input stream and receive first request. Acceptance test.
     *
     * @param client client socket (tried to connect)
     * @return
     * @throws java.io.IOException if something happens
     */
    protected boolean tst(Socket client) throws IOException {
        byte[] request = new byte[32];
        int bytesRead = client.getInputStream().read(request);
        if (bytesRead == -1) {
            return false;
        }

        /* ==> EXAMPLE 
        // Send a simple message with magic bytes prepended
            final byte[] client = (Game.CLIENT_NAME).getBytes("US-ASCII"); // 8 Bytes            
            final byte[] version = {(byte) (GameObject.VERSION >> 24), (byte) (GameObject.VERSION >> 16), (byte) (GameObject.VERSION >> 8), (byte) (GameObject.VERSION)}; // 4 Bytes
            final byte[] hello = (Game.HELLO).getBytes("US-ASCII"); // 8 Bytes
            final byte[] magic = MAGIC_BYTES; // 4 Bytes                
            final byte[] reserved = RESERVED; // 8 Bytes
        <== */
        int pos = 0;
        String clientName = new String(request, 0, 8, "US-ASCII");
        if (!clientName.equals(Game.CLIENT_NAME)) {
            return false;
        }
        pos += 8;
        int version = (request[pos + 3] & 0xFF) << 24 | (request[pos + 2] & 0xFF) << 16 | (request[pos + 1] & 0xFF) << 8 | (request[pos + 0] & 0xFF);
        if (version < 39) {
            return false;
        }
        pos += 4;
        String hello = new String(request, pos, 8, "US-ASCII");
        if (!hello.equals(Game.HELLO)) {
            return false;
        }
        byte[] magic = new byte[Game.MAGIC_BYTES.length];
        System.arraycopy(request, pos, magic, 0, magic.length);
        if (!Arrays.equals(Game.MAGIC_BYTES, magic)) {
            return false;
        }
        pos += 4;
        byte[] reserved = new byte[Game.RESERVED.length];
        System.arraycopy(request, pos, reserved, 0, reserved.length);

        return Arrays.equals(Game.RESERVED, reserved);
    }

    /**
     * Open client output stream and send first (welcome) response. Acceptance
     * test passed.
     *
     * @param client client socket (tried to connect)
     * @throws java.io.IOException if something happens
     */
    public void welcome(Socket client) throws IOException {
        // Send a simple message with magic bytes prepended
        final StringBuilder sb = new StringBuilder();
        String welcome = String.format("Hello, you are connected to %s, v%s, for help write \"help\" without quotes. Welcome!", GameServer.SERVER_NAME, GameObject.VERSION);
        sb.append(welcome);

        client.getOutputStream().write(welcome.getBytes("US-ASCII"));
    }

    /**
     * Server loop
     */
    @Override
    public void run() {
        try {
            // Bind the server socket to a specific IP address and port
            server = new ServerSocket();
            server.bind(new InetSocketAddress(host, port));
        } catch (IOException ex) {
            DSLogger.reportError("Cannot create Game Server!", ex);
            DSLogger.reportError(ex.getMessage(), ex);
        }
        // Accept incoming connections and handle them
        while (!gameObject.WINDOW.shouldClose() || !shutDownSignal) {
            try {
                final Socket client = server.accept();
                clients.add(client);
                // Acceptance test failure
                if (tst(client)) {
                    // Send "Welcome" Response
                    welcome(client);
                } else {
                    DSLogger.reportError("Acceptance test failure!", null);
                    clients.remove(client);
                    shutDownSignal = true;
                }
                // Handle the client connection
            } catch (IOException ex) {
                DSLogger.reportError("Error(s) on listening!", ex);
                DSLogger.reportError(ex.getMessage(), ex);
            }
        }
    }

    /**
     * Send responnse to the client. Client was previously accepted.
     *
     * @param client game client
     */
    public void sendResponse(Socket client) {
        // TODO
    }

    /**
     * Receive request from the client. Client was previously accepted.
     *
     * @param client game client
     */
    public void recieveRequest(Socket client) {
        // TODO
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

    public List<Socket> getClients() {
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

}
