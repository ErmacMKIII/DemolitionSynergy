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

import com.google.gson.Gson;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.function.Supplier;
import org.joml.Vector3f;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.critter.Critter;
import rs.alexanderstojanovich.evg.level.LevelActors;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.net.DSObject;
import static rs.alexanderstojanovich.evg.net.DSObject.DataType.INT;
import static rs.alexanderstojanovich.evg.net.DSObject.DataType.STRING;
import rs.alexanderstojanovich.evg.net.PlayerInfo;
import rs.alexanderstojanovich.evg.net.PosInfo;
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

    public static final int BUFF_SIZE = 8192; // write bytes (chunk) buffer size

    public final Socket client;
    public final GameServer gameServer;

    protected boolean goodBye = false;

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

        // Handle null data type (Possible & always erroneous)
        if (request.getDataType() == null) {
            Response response = new Response(ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, "Bad Request - Bad data type!");
            response.send(gameServer, client);
            return;
        }

        ResponseIfc response;
        String msg;
        LevelActors levelActors;
        double gameTime;
        switch (request.getRequestType()) {
            case HELLO:
                msg = String.format("Bad Request - You are alerady connected to %s, v%s!", gameServer.worldName, gameServer.version);
                response = new Response(ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, msg);
                response.send(gameServer, client);
                break;
            case REGISTER:
                switch (request.getDataType()) {
                    case STRING: {
                        String newPlayerUniqueId = request.getData().toString();
                        levelActors = gameServer.gameObject.game.gameObject.levelContainer.levelActors;
                        if (!levelActors.player.uniqueId.equals(newPlayerUniqueId)
                                && (levelActors.otherPlayers.getIf(ot -> ot.uniqueId.equals(newPlayerUniqueId)) == null)) {
                            levelActors.otherPlayers.add(new Critter(newPlayerUniqueId, new Model(LevelActors.PLAYER_BODY)));
                            msg = String.format("Player ID is registered!", gameServer.worldName, gameServer.version);
                            response = new Response(ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, msg);

                            gameServer.gameObject.intrface.getConsole().write(String.format("Player %s has connected.", newPlayerUniqueId));
                            DSLogger.reportInfo(String.format("Player %s has connected.", newPlayerUniqueId), null);

                            gameServer.whoIsMap.put(client, newPlayerUniqueId);
                        } else {
                            msg = String.format("Player ID is invalid or already exists!", gameServer.worldName, gameServer.version);
                            response = new Response(ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, msg);
                        }
                        break;
                    }
                    case OBJECT: {
                        String jsonStr = request.getData().toString();
                        PlayerInfo info = PlayerInfo.fromJson(jsonStr);
                        levelActors = gameServer.gameObject.game.gameObject.levelContainer.levelActors;
                        if (!levelActors.player.uniqueId.equals(info.uniqueId)
                                && (levelActors.otherPlayers.getIf(ot -> ot.uniqueId.equals(info.uniqueId)) == null)) {
                            Critter critter = new Critter(info.uniqueId, new Model(LevelActors.PLAYER_BODY));
                            critter.setName(info.name);
                            critter.body.setPrimaryRGBAColor(info.color);
                            critter.body.texName = info.texModel;
                            levelActors.otherPlayers.add(critter);

                            gameServer.gameObject.intrface.getConsole().write(String.format("Player %s (%s) has connected.", info.name, info.uniqueId));
                            DSLogger.reportInfo(String.format("Player %s (%s) has connected.", info.name, info.uniqueId), null);

                            gameServer.whoIsMap.put(client, info.uniqueId);

                            msg = String.format("Player ID is registered!", gameServer.worldName, gameServer.version);
                            response = new Response(ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, msg);
                        } else {
                            msg = String.format("Player ID is invalid or already exists!", gameServer.worldName, gameServer.version);
                            response = new Response(ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, msg);
                        }
                        break;
                    }
                    default:
                        response = new Response(ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, "Bad Request - Bad data type!");
                        break;
                }
                response.send(gameServer, client);
                break;
            case GOODBYE:
                msg = "Goodbye, hope we will see you again!";
                response = new Response(ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, msg);
                response.send(gameServer, client);
                goodBye = true;
                break;
            case GET_TIME:
                gameTime = Game.gameTicks;
                response = new Response(ResponseIfc.ResponseStatus.OK, DSObject.DataType.DOUBLE, gameTime);
                response.send(gameServer, client);
                break;
            case PING:
                msg = String.format("You pinged %s", gameServer);
                response = new Response(ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, msg);
                response.send(gameServer, client);
                break;
            case GET_POS:
                switch (request.getDataType()) {
                    case INT: {
                        int playerIndex = (int) request.getData() - 1;
                        Vector3f vec3fPos;
                        Vector3f vec3fView;
                        PosInfo posInfo;
                        String obj;
                        levelActors = gameServer.gameObject.game.gameObject.levelContainer.levelActors;
                        if (playerIndex == -1) {
                            vec3fPos = levelActors.player.getPos();
                            vec3fView = levelActors.player.getFront();
                            posInfo = new PosInfo(levelActors.player.uniqueId, vec3fPos, vec3fView);
                            obj = posInfo.toString();
                            response = new Response(ResponseIfc.ResponseStatus.OK, DSObject.DataType.OBJECT, obj);
                        } else if (playerIndex >= 0 && playerIndex < levelActors.otherPlayers.size()) {
                            vec3fPos = levelActors.otherPlayers.get(playerIndex).getPos();
                            vec3fView = levelActors.otherPlayers.get(playerIndex).getFront();
                            posInfo = new PosInfo(levelActors.player.uniqueId, vec3fPos, vec3fView);
                            obj = posInfo.toString();
                            response = new Response(ResponseIfc.ResponseStatus.OK, DSObject.DataType.OBJECT, obj);
                        } else {
                            response = new Response(ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, "Bad Request - Invalid argument!");
                        }
                        break;
                    }
                    case STRING: {
                        String uuid = request.getData().toString();
                        Vector3f vec3fPos;
                        Vector3f vec3fView;
                        PosInfo posInfo;
                        String obj;
                        levelActors = gameServer.gameObject.game.gameObject.levelContainer.levelActors;
                        if (levelActors.player.uniqueId.equals(uuid)) {
                            vec3fPos = levelActors.player.getPos();
                            vec3fView = levelActors.player.getFront();
                            posInfo = new PosInfo(levelActors.player.uniqueId, vec3fPos, vec3fView);
                            obj = posInfo.toString();
                            response = new Response(ResponseIfc.ResponseStatus.OK, DSObject.DataType.OBJECT, obj);
                        } else {
                            Critter other = levelActors.otherPlayers.getIf(ply -> ply.uniqueId.equals(uuid));
                            if (other != null) {
                                vec3fPos = other.getPos();
                                vec3fView = other.getFront();
                                posInfo = new PosInfo(levelActors.player.uniqueId, vec3fPos, vec3fView);
                                obj = posInfo.toString();
                                response = new Response(ResponseIfc.ResponseStatus.OK, DSObject.DataType.OBJECT, obj);
                            } else {
                                response = new Response(ResponseIfc.ResponseStatus.ERR, DSObject.DataType.OBJECT, "Bad Request - Invalid Player ID or not registered!");
                            }
                        }
                        break;
                    }
                    default:
                        response = new Response(ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, "Bad Request - Bad data type!");
                        break;
                }
                response.send(gameServer, client);
                break;
            case SET_POS:
                String jsonStr = request.getData().toString();
                PosInfo posInfo = PosInfo.fromJson(jsonStr);
                levelActors = gameServer.gameObject.game.gameObject.levelContainer.levelActors;
                if (levelActors.player.uniqueId.equals(posInfo.uniqueId)) {
                    levelActors.player.setPos(posInfo.pos);
                    levelActors.player.getFront().set(posInfo.front);
                    levelActors.player.setRotationXYZ(posInfo.front);
                } else {
                    Critter other = levelActors.otherPlayers.getIf(ply -> ply.uniqueId.equals(posInfo.uniqueId));
                    other.setPos(posInfo.pos);
                    other.getFront().set(posInfo.front);
                    other.setRotationXYZ(posInfo.front);
                }
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
            case PLAYER_INFO:
                levelActors = gameServer.gameObject.game.gameObject.levelContainer.levelActors;
                Gson gson = new Gson();
                IList<PlayerInfo> playerInfos = new GapList<>();
                playerInfos.add(new PlayerInfo(levelActors.player.getName(), levelActors.player.body.texName, levelActors.player.uniqueId, levelActors.player.body.getPrimaryRGBAColor()));
                levelActors.otherPlayers.forEach(op -> {
                    playerInfos.add(new PlayerInfo(op.getName(), op.body.texName, op.uniqueId, op.body.getPrimaryRGBAColor()));
                });
                String obj = gson.toJson(playerInfos, IList.class);
                response = new Response(ResponseIfc.ResponseStatus.OK, DSObject.DataType.OBJECT, obj);
                response.send(gameServer, client);
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
            while ((client.isConnected() && !client.isClosed()) && !goodBye && !gameServer.isShutDownSignal()) {
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

    public static enum Status {
        INTERNAL_ERROR, CLIENT_ERROR, OK
    }

    public static int getBUFF_SIZE() {
        return BUFF_SIZE;
    }

    public boolean isGoodBye() {
        return goodBye;
    }

}
