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
import java.io.OutputStream;
import java.net.Socket;
import java.util.function.Supplier;
import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.critter.Player;
import rs.alexanderstojanovich.evg.level.LevelActors;
import rs.alexanderstojanovich.evg.net.DSObject;
import rs.alexanderstojanovich.evg.net.Request;
import rs.alexanderstojanovich.evg.net.RequestIfc;
import static rs.alexanderstojanovich.evg.net.RequestIfc.RequestType.DOWNLOAD;
import static rs.alexanderstojanovich.evg.net.RequestIfc.RequestType.GET_POS;
import static rs.alexanderstojanovich.evg.net.RequestIfc.RequestType.GET_TIME;
import static rs.alexanderstojanovich.evg.net.RequestIfc.RequestType.GOODBYE;
import static rs.alexanderstojanovich.evg.net.RequestIfc.RequestType.HELLO;
import static rs.alexanderstojanovich.evg.net.RequestIfc.RequestType.PING;
import rs.alexanderstojanovich.evg.net.Response;
import rs.alexanderstojanovich.evg.net.ResponseIfc;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 * Task to handle each client asynchronously.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class HandleClientTask implements Supplier<HandleClientTask.Status> {

    public static enum Status {
        INTERNAL_ERROR, CLIENT_ERROR, OK
    }

    public static final int BUFF_SIZE = 8192; // write bytes (chunk) buffer size

    public final Socket client;
    public final GameServer gameServer;

    /**
     * Constructor for HandleClientTask.
     *
     * @param client The client socket to handle.
     * @param gameServer game server handling the clients.
     */
    public HandleClientTask(Socket client, GameServer gameServer) {
        this.client = client;
        this.gameServer = gameServer;
    }

    /**
     * Process request from clients and send response.
     *
     * @throws java.lang.Exception if errors on serialization
     */
    public void process() throws Exception {
        // Handle client request and response
        RequestIfc request = RequestIfc.receive(gameServer, client);
        if (request == Request.INVALID) {
            // avoid processing invalid requests requests
            return;
        }
        ResponseIfc response;
        String msg;
        LevelActors levelActors;
        double time;
        switch (request.getRequestType()) {
            case HELLO:
                msg = String.format("Bad Request - You are alerady connected to %s, v%s!", gameServer.worldName, gameServer.version);
                response = new Response(ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, msg);
                response.send(gameServer, client);
                break;
            case REGISTER:
                String newPlayerUniqueId = request.getData().toString();
                levelActors = gameServer.gameObject.game.gameObject.levelContainer.levelActors;
                if (!levelActors.player.uniqueId.equals(newPlayerUniqueId)
                        && (levelActors.otherPlayers.getIf(ot -> ot.uniqueId.equals(newPlayerUniqueId)) == null)) {
                    levelActors.otherPlayers.add(new Player(LevelActors.PLAYER_BODY));
                    msg = String.format("Player ID is OK!", gameServer.worldName, gameServer.version);
                    response = new Response(ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, msg);
                } else {
                    msg = String.format("Player ID is invalid or already exists!", gameServer.worldName, gameServer.version);
                    response = new Response(ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, msg);
                }
                response.send(gameServer, client);
                break;
            case GOODBYE:
                msg = "Goodbye, hope we will see you again!";
                response = new Response(ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, msg);
                response.send(gameServer, client);
                client.close();
                break;
            case GET_TIME:
                time = GameTime.Now().getTime();
                response = new Response(ResponseIfc.ResponseStatus.OK, DSObject.DataType.DOUBLE, time);
                response.send(gameServer, client);
                break;
            case PING:
                msg = String.format("You pinged %s", gameServer);
                response = new Response(ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, msg);
                response.send(gameServer, client);
                break;
            case GET_POS:
                if (request.getDataType() == DSObject.DataType.INT) {
                    int playerIndex = (int) request.getData() - 1;
                    Vector3f vec3f;
                    levelActors = gameServer.gameObject.game.gameObject.levelContainer.levelActors;
                    if (playerIndex == -1) {
                        vec3f = levelActors.player.getPos();
                        response = new Response(ResponseIfc.ResponseStatus.OK, DSObject.DataType.VEC3F, vec3f);
                    } else if (playerIndex >= 0 && playerIndex < levelActors.otherPlayers.size()) {
                        vec3f = levelActors.otherPlayers.get(playerIndex).getPos();
                        response = new Response(ResponseIfc.ResponseStatus.OK, DSObject.DataType.VEC3F, vec3f);
                    } else {
                        response = new Response(ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, "Bad Request - Invalid argument!");
                    }
                } else if (request.getDataType() == DSObject.DataType.STRING) {
                    String uuid = request.getData().toString();
                    Vector3f vec3f;
                    levelActors = gameServer.gameObject.game.gameObject.levelContainer.levelActors;
                    if (levelActors.player.uniqueId.equals(uuid)) {
                        vec3f = levelActors.player.getPos();
                        response = new Response(ResponseIfc.ResponseStatus.OK, DSObject.DataType.VEC3F, vec3f);
                    } else {
                        Player other = levelActors.otherPlayers.getIf(ply -> ply.uniqueId.equals(uuid));
                        if (other != null) {
                            vec3f = other.getPos();
                            response = new Response(ResponseIfc.ResponseStatus.OK, DSObject.DataType.VEC3F, vec3f);
                        } else {
                            response = new Response(ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, "Bad Request - Invalid Player ID or not registered!");
                        }
                    }
                } else {
                    response = new Response(ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, "Bad Request - Bad data type!");
                }
                response.send(gameServer, client);
                break;
            case DOWNLOAD:
                response = new Response(ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, "Level download request is OK.");
                response.send(gameServer, client);
                if (gameServer.gameObject.levelContainer.saveLevelToFileAsync(gameServer.worldName + ".dat").get()) {
                    OutputStream out = client.getOutputStream();
                    int bytesWrite = 0;
                    final int totalBytesWrite = gameServer.gameObject.levelContainer.pos;

                    // Upload in chunks
                    while (bytesWrite < totalBytesWrite) {
                        // Calculate the remaining bytes to write in this iteration
                        int remainingBytes = totalBytesWrite - bytesWrite;
                        // Determine the size of the chunk to write in this iteration
                        int chunkSize = Math.min(remainingBytes, BUFF_SIZE);

                        // Write a chunk of data to the output stream
                        out.write(gameServer.gameObject.levelContainer.buffer, bytesWrite, chunkSize);
                        // Increment the total bytes written
                        bytesWrite += chunkSize;

                        // Logging the bytes written
                        DSLogger.reportInfo("Bytes written: " + bytesWrite + " / " + totalBytesWrite, null);
                    }

                    // signal end-of-stream
                    out.write(GameServer.EOS);
                }
                break;
        }
    }

    /**
     * Asynchronous task to handle the client.
     *
     * @return null.
     */
    @Override
    public HandleClientTask.Status get() {
        HandleClientTask.Status status;
        try {
            // Handle client request and response
            while (client.isConnected() && !client.isClosed()) {
                process();
            }
            status = Status.OK;
        } catch (IOException ex) {
            // Handle IOException
            status = Status.CLIENT_ERROR;
            DSLogger.reportError("IOException occurred!", ex);
        } catch (Exception ex) {
            // Handle other exceptions
            status = Status.INTERNAL_ERROR;
            DSLogger.reportError("Exception occurred!", ex);
        }

        return status;
    }

    public Socket getClient() {
        return client;
    }

    public GameServer getGameServer() {
        return gameServer;
    }

}
