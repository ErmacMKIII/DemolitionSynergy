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
import java.util.List;
import org.magicwerk.brownies.collections.GapList;
import rs.alexanderstojanovich.evg.net.DSMachine;
import rs.alexanderstojanovich.evg.net.DSObject;
import rs.alexanderstojanovich.evg.net.RequestIfc;
import rs.alexanderstojanovich.evg.net.Response;
import rs.alexanderstojanovich.evg.net.ResponseIfc;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class GameServer implements DSMachine, Runnable {
    
    protected String worldName = "My World";
    protected String host = "localhost";
    protected int port = 13667;
    
    protected static final int BACKLOG = 16;
    
    protected ServerSocket server;
    public final List<Socket> clients = new GapList<>();
    protected final GameObject gameObject;
    
    protected boolean shutDownSignal = false;
    protected final int version = 39;

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
    public void accept(Socket client) throws IOException {
        // Send a simple message with magic bytes prepended
        final StringBuilder sb = new StringBuilder();
        String welcome = String.format("Hello, you are connected to %s, v%s, for help write \"help\" without quotes. Welcome!", this.worldName, this.version);
        sb.append(welcome);
        
        Object welcomeObj = welcome.getBytes("US-ASCII");
        
        ResponseIfc response = new Response(ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, welcomeObj);
        
        response.send(this, client);
    }

    /**
     * Open client output stream and not accepted response. Acceptance test
     * failed.
     *
     * @param client client socket (tried to connect)
     * @throws java.io.IOException if something happens
     */
    public void reject(Socket client) throws IOException {
        // Send a simple message with magic bytes prepended
        final StringBuilder sb = new StringBuilder();
        String message = "Sorry, your connection refuesed!";
        sb.append(message);
        
        Object msgObj = message.getBytes("US-ASCII");
        
        ResponseIfc response = new Response(ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, msgObj);
        
        response.send(this, client);
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
                // Acceptance test (examination)
                if (tst(client)) {
                    // Send "Welcome" Response
                    accept(client);
                } else {
                    DSLogger.reportError("Acceptance test failure!", null);
                    reject(client);
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
    
    @Override
    public MachineType getMachineType() {
        return MachineType.DSSERVER;
    }
    
    @Override
    public int getVersion() {
        return this.version;
    }
    
}
