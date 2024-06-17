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
     * Number of successive packets to receive before confirmation (Server)
     */
    public static final int PACKETS_MAX = 8;

    /**
     * Maximum number of level attempts (retransmission)
     */
    public static final int RETRANSMISSION_MAX_ATTEMPTS = 3;

//    /**
//     * Internal Mutex object
//     */
//    public static final Object InternMutex = new Object();
//    /**
//     * Is waiting confirm
//     */
//    public static volatile boolean waitOnDownload = false;
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
        final RequestIfc request;
        //        if (waitOnDownload) { // no requests will be taken into consideration 
//            // when processing download request
//            synchronized (GameServerProcessor.InternMutex) {
//                GameServerProcessor.InternMutex.wait();
//                request = null;
//            }
//        } else {
        request = RequestIfc.receive(gameServer);
//        }

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
            Response response = new Response(request.getChecksum(), ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, "Bad Request - Bad data type!");
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

        final ResponseIfc response;
        String msg;
        LevelActors levelActors;
        final int totalBytes;

        double gameTime;
        switch (request.getRequestType()) {
            case HELLO:
                if (gameServer.clients.contains(clientHostName)) {
                    msg = String.format("Bad Request - You are alerady connected to %s, v%s!", gameServer.worldName, gameServer.version);
                    response = new Response(request.getChecksum(), ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, msg);
                } else {
                    // Send a simple message with magic bytes prepended
                    msg = String.format("Hello, you are connected to %s, v%s, for help write \"help\" without quotes. Welcome!", gameServer.worldName, gameServer.version);
                    response = new Response(request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, msg);
                    gameServer.clients.add(clientHostName);
                    gameServer.timeToLiveMap.putIfAbsent(clientHostName, GameServer.TIME_TO_LIVE);
                    gameServer.gameObject.WINDOW.setTitle(GameObject.WINDOW_TITLE + " - " + gameServer.worldName + " - Player Count: " + (1 + gameServer.clients.size()));
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
                            levelActors.otherPlayers.add(new Critter(newPlayerUniqueId, new Model(gameServer.gameObject.GameAssets.PLAYER_BODY_DEFAULT)));
                            msg = String.format("Player ID is registered!", gameServer.worldName, gameServer.version);
                            response = new Response(request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, msg);

                            gameServer.gameObject.intrface.getConsole().write(String.format("Player %s has connected.", newPlayerUniqueId));
                            DSLogger.reportInfo(String.format("Player %s has connected.", newPlayerUniqueId), null);

                            gameServer.whoIsMap.put(clientHostName, newPlayerUniqueId);
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
                        if (!levelActors.player.uniqueId.equals(info.uniqueId)
                                && (levelActors.otherPlayers.getIf(ot -> ot.uniqueId.equals(info.uniqueId)) == null)) {
                            Critter critter = new Critter(info.uniqueId, new Model(gameServer.gameObject.GameAssets.PLAYER_BODY_DEFAULT));
                            critter.setName(info.name);
                            critter.body.setPrimaryRGBAColor(info.color);
                            critter.body.texName = info.texModel;
                            levelActors.otherPlayers.add(critter);

                            gameServer.gameObject.intrface.getConsole().write(String.format("Player %s (%s) has connected.", info.name, info.uniqueId));
                            DSLogger.reportInfo(String.format("Player %s (%s) has connected.", info.name, info.uniqueId), null);

                            gameServer.whoIsMap.put(clientHostName, info.uniqueId);

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
                gameServer.timeToLiveMap.remove(clientHostName);
                gameServer.clients.remove(clientHostName);
                gameServer.gameObject.WINDOW.setTitle(GameObject.WINDOW_TITLE + " - " + gameServer.worldName + " - Player Count: " + (1 + gameServer.clients.size()));
                String uniqueId = gameServer.whoIsMap.get(clientHostName);
                if (uniqueId != null) {
                    GameServer.performCleanUp(gameServer.gameObject, uniqueId, false);
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
                totalBytes = gameServer.gameObject.levelContainer.pos;
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
                totalBytes = gameServer.gameObject.levelContainer.pos;
                final byte[] buffer = gameServer.gameObject.levelContainer.buffer;

                if (n < 0 || n * BUFF_SIZE >= totalBytes) {
                    response = new Response(request.getChecksum(), ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, "Invalid fragment number");
                    response.send(gameServer, clientAddress, clientPort);
                    return new Result(Status.CLIENT_ERROR, clientHostName);
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
