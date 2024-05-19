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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import org.joml.Vector3f;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.critter.Critter;
import rs.alexanderstojanovich.evg.level.LevelActors;
import static rs.alexanderstojanovich.evg.main.GameServer.MAX_CLIENTS;
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
 * Task to handle each endpoint asynchronously.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class GameServerProcessor {

    public static final int BUFF_SIZE = 8192; // write bytes (chunk) buffer size

    public static final int FAIL_ATTEMPT_MAX = 10;
    public static final int TOTAL_FAIL_ATTEMPT_MAX = 3000;

    public static int TotalFailedAttempts = 0;

    /**
     * Assert that failure has happen and client timed out or is about to be
     * rejected. In other words client will fail the test.
     *
     * @param gameServer game server
     * @param failedHostName client who is submit to test
     */
    public static void assertTstFailure(GameServer gameServer, String failedHostName) {
        TotalFailedAttempts++;
        boolean contains = gameServer.failedAttempts.containsKey(failedHostName);
        if (!contains) {
            gameServer.failedAttempts.put(failedHostName, 1);
        } else {
            Integer failAttemptNum = gameServer.failedAttempts.get(failedHostName);
            failAttemptNum++;

            // Blacklisting (equals ban)
            if (failAttemptNum >= FAIL_ATTEMPT_MAX && !gameServer.blacklist.contains(failedHostName)) {
                gameServer.blacklist.add(failedHostName);
                gameServer.gameObject.intrface.getConsole().write(String.format("Client (%s) is now blacklisted!", failedHostName));
                DSLogger.reportWarning(String.format("Game Server (%s) is now blacklisted!", failedHostName), null);
            }

            // Too much failed attempts, endpoint is vulnerable .. try to shut down
            if (TotalFailedAttempts >= TOTAL_FAIL_ATTEMPT_MAX) {
                gameServer.gameObject.intrface.getConsole().write(String.format("Game Server (%s) status critical! Trying to shut down!", failedHostName));
                DSLogger.reportWarning(String.format("Game Server (%s) status critical! Trying to shut down!", failedHostName), null);
                gameServer.stopServer();
            }

            gameServer.failedAttempts.replace(failedHostName, failAttemptNum);
        }
    }

    /**
     * Process request from clients and send response.
     *
     * @param endpoint The endpoint socket to handle.
     * @param gameServer game endpoint handling the clients.
     * @return result status of processing to the end point
     * @throws java.lang.Exception if errors on serialization
     */
    public static GameServerProcessor.Result process(GameServer gameServer, DatagramSocket endpoint) throws Exception {
        // Handle endpoint request and response
        RequestIfc request = RequestIfc.receive(gameServer);
        if (request == null) {
            // avoid processing invalid requests requests
            return new Result(Status.INTERNAL_ERROR, null);
        }

        final InetAddress clientAddress = request.getClientAddress();
        final int clientPort = request.getClientPort();
        String clientHostName = clientAddress.getHostName();
        if (request == Request.INVALID) {
            // avoid processing invalid requests requests
            return new Result(Status.INTERNAL_ERROR, request.getClientAddress().getHostName());
        }

        // Handle null data type (Possible & always erroneous)
        if (request.getDataType() == null) {
            Response response = new Response(ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, "Bad Request - Bad data type!");
            response.send(gameServer, clientAddress, clientPort);

            return new Result(Status.INTERNAL_ERROR, clientHostName);
        }

        if (!gameServer.clients.contains(clientHostName) && request.getRequestType() != RequestIfc.RequestType.HELLO) {
            GameServerProcessor.assertTstFailure(gameServer, clientHostName);
            return new Result(Status.CLIENT_ERROR, clientHostName);
        }

        if (gameServer.blacklist.contains(clientHostName) || gameServer.clients.size() >= MAX_CLIENTS) {
            GameServerProcessor.assertTstFailure(gameServer, clientHostName);
            return new Result(Status.CLIENT_ERROR, clientHostName);
        }

        ResponseIfc response;
        String msg;
        LevelActors levelActors;

        double gameTime;
        switch (request.getRequestType()) {
            case HELLO:
                if (gameServer.clients.contains(clientHostName)) {
                    msg = String.format("Bad Request - You are alerady connected to %s, v%s!", gameServer.worldName, gameServer.version);
                    response = new Response(ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, msg);
                } else {
                    // Send a simple message with magic bytes prepended
                    msg = String.format("Hello, you are connected to %s, v%s, for help write \"help\" without quotes. Welcome!", gameServer.worldName, gameServer.version);
                    response = new Response(ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, msg);
                    gameServer.clients.add(clientHostName);
                }
                response.send(gameServer, clientAddress, clientPort);
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

                            gameServer.whoIsMap.put(endpoint, newPlayerUniqueId);
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

                            gameServer.whoIsMap.put(endpoint, info.uniqueId);

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
                response.send(gameServer, clientAddress, clientPort);
                break;
            case GOODBYE:
                msg = "Goodbye, hope we will see you again!";
                response = new Response(ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, msg);
                response.send(gameServer, clientAddress, clientPort);
                gameServer.clients.remove(clientHostName);
                break;
            case GET_TIME:
                gameTime = Game.gameTicks;
                response = new Response(ResponseIfc.ResponseStatus.OK, DSObject.DataType.DOUBLE, gameTime);
                response.send(gameServer, clientAddress, clientPort);
                break;
            case PING:
                msg = String.format("You pinged %s", gameServer);
                response = new Response(ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, msg);
                response.send(gameServer, clientAddress, clientPort);
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
                response.send(gameServer, clientAddress, clientPort);
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
                response.send(gameServer, clientAddress, clientPort);
                if (gameServer.gameObject.levelContainer.saveLevelToFileAsync(gameServer.worldName + ".dat").get()) {
                    int bytesWrite = 0;
                    final int totalBytesWrite = gameServer.gameObject.levelContainer.pos;

                    final byte[] buff = new byte[BUFF_SIZE];
                    // Upload in chunks
                    while (bytesWrite < totalBytesWrite) {
                        // Calculate the remaining bytes to write in this iteration
                        int remainingBytes = totalBytesWrite - bytesWrite;
                        // Determine the size of the chunk to write in this iteration
                        int chunkSize = Math.min(remainingBytes, BUFF_SIZE);

                        // Write a chunk of data to the outbound datagram packet
                        System.arraycopy(gameServer.gameObject.levelContainer.buffer, bytesWrite, buff, 0, chunkSize);
                        DatagramPacket dp = new DatagramPacket(buff, chunkSize);
                        endpoint.send(dp);

                        // Increment the total bytes written
                        bytesWrite += chunkSize;

                        // Logging the bytes written
                        DSLogger.reportInfo("Bytes written: " + bytesWrite + " / " + totalBytesWrite, null);
                    }

                    // signal end-of-stream
                    DatagramPacket dp = new DatagramPacket(GameServer.EOS, GameServer.EOS.length);
                    endpoint.send(dp);
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
                response.send(gameServer, clientAddress, clientPort);
                break;
        }

        return new Result(Status.OK, clientHostName);
    }

    /**
     * Result of the processing
     */
    public static class Result {

        /**
         * Status of the processing result
         */
        public final Status status;
        /**
         * Client who was processed
         */
        public final String client;

        /**
         * Result of the processing
         *
         * @param status Status of the processing result
         * @param client Client who was processed
         */
        public Result(Status status, String client) {
            this.status = status;
            this.client = client;
        }

        /**
         * Processing result status. One of the following {INTERNAL_ERROR,
         * CLIENT_ERROR, OK }
         *
         * @return
         */
        public Status getStatus() {
            return status;
        }

        /**
         * Get Client who was processed
         *
         * @return client hostname
         */
        public String getClient() {
            return client;
        }

    }

    /**
     * Processing result status
     */
    public static enum Status {
        /**
         * Error on server side
         */
        INTERNAL_ERROR,
        /**
         * Error on client side (such as wrong protocol)
         */
        CLIENT_ERROR,
        /**
         * Result Is okey
         */
        OK;
    }

    public static int getBUFF_SIZE() {
        return BUFF_SIZE;
    }

}
