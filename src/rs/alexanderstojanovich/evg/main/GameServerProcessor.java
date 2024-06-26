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
import rs.alexanderstojanovich.evg.net.ClientInfo;
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

    public static final int BUFF_SIZE = 8192; // append bytes (chunk) buffer size

    public static final int FAIL_ATTEMPT_MAX = 10;
    public static final int TOTAL_FAIL_ATTEMPT_MAX = 3000;

    public static int TotalFailedAttempts = 0;

    /**
     * Number of successive packets to receive before confirmation (Server)
     */
    public static final int PACKETS_MAX = 8;

    /**
     * Maximum number of level attempts (retransmission)
     */
    public static final int RETRANSMISSION_MAX_ATTEMPTS = 3;

    /**
     * Last checksum to avoid (duping)
     */
    protected static long lastChecksum = 0L;

    /**
     * Last time request was sent
     */
    protected static long lastTime = System.nanoTime();

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
        final RequestIfc request;

        request = RequestIfc.receive(gameServer);

        if (request == null || request.getClientAddress() == null) {
            // avoid processing invalid requests requests
            return new Result(Status.INTERNAL_ERROR, null, null, "Invalid request - Is null or client address is null");
        }

        String clientGuid = request.getGuid();
        final InetAddress clientAddress = request.getClientAddress();
        final int clientPort = request.getClientPort();
        String clientHostName = clientAddress.getHostName();

        // defence against duping packets (possibility bridged connections)
        long currTime = System.nanoTime();
        double deltaTime = (currTime - lastTime) / 1E9D;
        if (request.getRequestType() != GET_POS && request.getChecksum() == lastChecksum && deltaTime < Game.TICK_TIME / 16.0) {
            // avoid processing duplicate packages
            return new Result(Status.CLIENT_ERROR, clientHostName, clientGuid, "Sent duplicate packet - rejecting");
        }
        lastChecksum = request.getChecksum();
        lastTime = currTime;

        // pending kick
        if (gameServer.kicklist.contains(clientGuid)) {
            ResponseIfc response = new Response(0L, ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, clientGuid);
            response.send(gameServer, clientAddress, clientPort);

            gameServer.kicklist.remove(clientGuid);
            gameServer.clients.removeIf(c -> c.uniqueId.equals(clientGuid));

            return new Result(Status.OK, clientHostName, clientGuid, "OK => kick issued to the client!");
        }

        if (request == Request.INVALID) {
            // avoid processing invalid requests requests
            return new Result(Status.INTERNAL_ERROR, clientHostName, clientGuid, "Invalid request - Reason Unknown!");
        }

        // Handle null data type (Possible & always erroneous)
        if (request.getDataType() == null) {
            Response response = new Response(request.getChecksum(), ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, "Bad Request - Bad data type!");
            response.send(gameServer, clientAddress, clientPort);

            return new Result(Status.INTERNAL_ERROR, clientHostName, clientGuid, "Bad Request - Bad data type!");
        }

        if (!gameServer.clients.containsIf(c -> c.getUniqueId().equals(clientGuid)) && request.getRequestType() != RequestIfc.RequestType.HELLO && request.getRequestType() != RequestIfc.RequestType.GOODBYE) {
            gameServer.assertTstFailure(clientHostName, clientGuid);

            return new Result(Status.CLIENT_ERROR, clientHostName, clientGuid, "Client issued invalid request type (HELLO, GOODBYE)");
        }

        if (gameServer.blacklist.contains(clientHostName) || gameServer.clients.size() >= MAX_CLIENTS) {
            gameServer.assertTstFailure(clientHostName, clientGuid);

            return new Result(Status.CLIENT_ERROR, clientHostName, clientGuid, "Client is banned");
        }

        final ResponseIfc response;
        String msg;
        LevelActors levelActors;
        final int totalBytes;

        double gameTime;
        switch (request.getRequestType()) {
            case HELLO:
                if (gameServer.clients.containsIf(c -> c.getUniqueId().equals(clientGuid))) {
                    msg = String.format("Bad Request - You are alerady connected to %s, v%s!", gameServer.worldName, gameServer.version);
                    response = new Response(request.getChecksum(), ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, msg);
                } else {
                    // Send a simple message with magic bytes prepended
                    msg = String.format("Hello, you are connected to %s, v%s, for help append \"help\" without quotes. Welcome!", gameServer.worldName, gameServer.version);
                    response = new Response(request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, msg);
                    gameServer.clients.add(new ClientInfo(clientHostName, clientGuid, GameServer.TIME_TO_LIVE));
                    gameServer.gameObject.WINDOW.setTitle(GameObject.WINDOW_TITLE + " - " + gameServer.worldName + " - Player Count: " + (gameServer.clients.size()));
                }
                response.send(gameServer, clientAddress, clientPort);
                break;
            case REGISTER:
                switch (request.getDataType()) {
                    case STRING: {
                        String newPlayerUniqueId = request.getData().toString();
                        levelActors = gameServer.gameObject.game.gameObject.levelContainer.levelActors;
                        if ((levelActors.otherPlayers.getIf(ot -> ot.uniqueId.equals(newPlayerUniqueId)) == null)) {
                            levelActors.otherPlayers.add(new Critter(newPlayerUniqueId, new Model(gameServer.gameObject.GameAssets.PLAYER_BODY_DEFAULT)));
                            msg = String.format("Player ID is registered!", gameServer.worldName, gameServer.version);
                            response = new Response(request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, msg);

                            gameServer.gameObject.intrface.getConsole().write(String.format("Player %s has connected.", newPlayerUniqueId));
                            DSLogger.reportInfo(String.format("Player %s has connected.", newPlayerUniqueId), null);

                        } else {
                            msg = String.format("Player ID is invalid or already exists!", gameServer.worldName, gameServer.version);
                            response = new Response(request.getChecksum(), ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, msg);
                        }
                        break;
                    }
                    case OBJECT: {
                        String jsonStr = request.getData().toString();
                        PlayerInfo info = PlayerInfo.fromJson(jsonStr);
                        levelActors = gameServer.gameObject.game.gameObject.levelContainer.levelActors;
                        if ((levelActors.otherPlayers.getIf(ot -> ot.uniqueId.equals(info.uniqueId)) == null)) {
                            Critter critter = new Critter(info.uniqueId, new Model(gameServer.gameObject.GameAssets.PLAYER_BODY_DEFAULT));
                            critter.setName(info.name);
                            critter.body.setPrimaryRGBAColor(info.color);
                            critter.body.texName = info.texModel;
                            levelActors.otherPlayers.add(critter);

                            gameServer.gameObject.intrface.getConsole().write(String.format("Player %s (%s) has connected.", info.name, info.uniqueId));
                            DSLogger.reportInfo(String.format("Player %s (%s) has connected.", info.name, info.uniqueId), null);

                            msg = String.format("Player ID is registered!", gameServer.worldName, gameServer.version);
                            response = new Response(request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, msg);
                        } else {
                            msg = String.format("Player ID is invalid or already exists!", gameServer.worldName, gameServer.version);
                            response = new Response(request.getChecksum(), ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, msg);
                        }
                        break;
                    }
                    default:
                        response = new Response(request.getChecksum(), ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, "Bad Request - Bad data type!");
                        break;
                }
                response.send(gameServer, clientAddress, clientPort);
                break;
            case GOODBYE:
                msg = "Goodbye, hope we will see you again!";
                response = new Response(request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, msg);
                response.send(gameServer, clientAddress, clientPort);
                gameServer.clients.removeIf(c -> c.uniqueId.equals(clientGuid));
                gameServer.gameObject.WINDOW.setTitle(GameObject.WINDOW_TITLE + " - " + gameServer.worldName + " - Player Count: " + (gameServer.clients.size()));
                if (clientGuid != null) {
                    gameServer.kicklist.remove(clientGuid);
                    GameServer.performCleanUp(gameServer.gameObject, clientGuid, false);
                }
                break;
            case GET_TIME:
                gameTime = Game.gameTicks;
                response = new Response(request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.DOUBLE, gameTime);
                response.send(gameServer, clientAddress, clientPort);
                break;
            case PING:
                msg = String.format("You pinged %s", gameServer);
                response = new Response(request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, msg);
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
                            response = new Response(request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.OBJECT, obj);
                        } else if (playerIndex >= 0 && playerIndex < levelActors.otherPlayers.size()) {
                            vec3fPos = levelActors.otherPlayers.get(playerIndex).getPos();
                            vec3fView = levelActors.otherPlayers.get(playerIndex).getFront();
                            posInfo = new PosInfo(levelActors.player.uniqueId, vec3fPos, vec3fView);
                            obj = posInfo.toString();
                            response = new Response(request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.OBJECT, obj);
                        } else {
                            response = new Response(request.getChecksum(), ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, "Bad Request - Invalid argument!");
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
                            response = new Response(request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.OBJECT, obj);
                        } else {
                            Critter other = levelActors.otherPlayers.getIf(ply -> ply.uniqueId.equals(uuid));
                            if (other != null) {
                                vec3fPos = other.getPos();
                                vec3fView = other.getFront();
                                posInfo = new PosInfo(levelActors.player.uniqueId, vec3fPos, vec3fView);
                                obj = posInfo.toString();
                                response = new Response(request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.OBJECT, obj);
                            } else {
                                response = new Response(request.getChecksum(), ResponseIfc.ResponseStatus.ERR, DSObject.DataType.OBJECT, "Bad Request - Invalid Player ID or not registered!");
                            }
                        }
                        break;
                    }
                    default:
                        response = new Response(request.getChecksum(), ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, "Bad Request - Bad data type!");
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
                // Server alraedy saved the level
                gameServer.gameObject.levelContainer.storeLevelToBufferNewFormat();
                System.arraycopy(gameServer.gameObject.levelContainer.buffer, 0, gameServer.gameObject.levelContainer.bak_buffer, 0, gameServer.gameObject.levelContainer.pos);
                gameServer.gameObject.levelContainer.bak_pos = gameServer.gameObject.levelContainer.pos;
                totalBytes = gameServer.gameObject.levelContainer.bak_pos;
                final int bytesPerFragment = BUFF_SIZE;
                int fullFragments = totalBytes / bytesPerFragment;
                int remainingBytes = totalBytes % bytesPerFragment;

                int totalFragments = fullFragments + (remainingBytes > 0 ? 1 : 0);
                final ResponseIfc downloadResponse = new Response(request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.INT, totalFragments);
                try {
                    downloadResponse.send(gameServer, clientAddress, clientPort);
                } catch (Exception ex) {
                    DSLogger.reportError(ex.getMessage(), ex);
                    throw new RuntimeException("Failed to send download response!", ex);
                }
                break;
            case GET_FRAGMENT:
                int n = (int) request.getData(); // Assuming the N-th fragment number is sent in the request data
                totalBytes = gameServer.gameObject.levelContainer.bak_pos;
                final byte[] buffer = gameServer.gameObject.levelContainer.bak_buffer;

                if (n < 0 || n * BUFF_SIZE >= totalBytes) {
                    response = new Response(request.getChecksum(), ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, "Invalid fragment number");
                    response.send(gameServer, clientAddress, clientPort);
                    return new Result(Status.CLIENT_ERROR, clientHostName, clientGuid, "Invalid fragment number!");
                }

                int fragmentStart = n * BUFF_SIZE;
                int fragmentEnd = Math.min(fragmentStart + BUFF_SIZE, totalBytes);
                int fragmentSize = fragmentEnd - fragmentStart;
                byte[] fragment = new byte[fragmentSize];
                System.arraycopy(buffer, fragmentStart, fragment, 0, fragmentSize);

                DatagramPacket packet = new DatagramPacket(fragment, fragmentSize, clientAddress, clientPort);
                endpoint.send(packet);

                DSLogger.reportInfo(String.format("Sent %d fragment, %d total bytes written", n, fragmentSize), null);
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
                response = new Response(request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.OBJECT, obj);
                response.send(gameServer, clientAddress, clientPort);
                break;
            case SAY:
                levelActors = gameServer.gameObject.levelContainer.levelActors;
                String senderName = "?";
                if (clientGuid.equals(levelActors.player.uniqueId)) {
                    senderName = levelActors.player.getName();
                } else {
                    Critter otherPlayerOrNull = levelActors.otherPlayers.getIf(player -> player.uniqueId.equals(clientGuid));
                    if (otherPlayerOrNull != null) {
                        senderName = otherPlayerOrNull.getName();
                    }
                }
                response = new Response(request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, senderName + ":" + request.getData());
                for (ClientInfo recipients : gameServer.clients) {
                    response.send(gameServer, InetAddress.getByName(recipients.hostName), clientPort);
                }
                break;
        }

        return new Result(Status.OK, clientHostName, clientGuid, String.format("%s data= %s", request.getRequestType().name(), String.valueOf(request.getData())));
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
         * Client (hostname) who was processed
         */
        public final String hostname;

        /**
         * Guid who was processed
         */
        public final String guid;

        /**
         * Explanation of what happened
         */
        public final String message;

        /**
         * Result of the processing
         *
         * @param status Status of the processing result
         * @param hostname Client who was processed
         * @param guid Client guid
         * @param message message explanation
         */
        public Result(Status status, String hostname, String guid, String message) {
            this.status = status;
            this.hostname = hostname;
            this.guid = guid;
            this.message = message;
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
         * Client hostname
         *
         * @return hostname of client
         */
        public String getHostname() {
            return hostname;
        }

        /**
         * Client guid
         *
         * @return guid of client
         */
        public String getGuid() {
            return guid;
        }

        /**
         * Get Message of this result of processing
         *
         * @return string message
         */
        public String getMessage() {
            return message;
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

    public static int getTotalFailedAttempts() {
        return TotalFailedAttempts;
    }

//    public static Object getInternMutex() {
//        return InternMutex;
//    }
//
//    public static boolean isWaitOnDownload() {
//        return waitOnDownload;
//    }
}
