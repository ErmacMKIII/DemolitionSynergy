/*
 * Copyright (C) 2024 Aleksandar Stojanovic <coas91@rocketmail.com>
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.CRC32C;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.joml.Vector3f;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.critter.Critter;
import rs.alexanderstojanovich.evg.intrface.Command;
import rs.alexanderstojanovich.evg.level.LevelActors;
import static rs.alexanderstojanovich.evg.main.GameServer.MAX_CLIENTS;
import rs.alexanderstojanovich.evg.models.Model;
import rs.alexanderstojanovich.evg.net.ClientInfo;
import rs.alexanderstojanovich.evg.net.DSObject;
import rs.alexanderstojanovich.evg.net.LevelMapInfo;
import rs.alexanderstojanovich.evg.net.PlayerInfo;
import rs.alexanderstojanovich.evg.net.PosInfo;
import rs.alexanderstojanovich.evg.net.Request;
import rs.alexanderstojanovich.evg.net.RequestIfc;
import rs.alexanderstojanovich.evg.net.Response;
import rs.alexanderstojanovich.evg.net.ResponseIfc;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.weapons.WeaponIfc;
import rs.alexanderstojanovich.evg.weapons.Weapons;

/**
 * Task to handle each endpoint asynchronously.
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class GameServerProcessor extends IoHandlerAdapter {

    /**
     * One and only configuration instance
     */
    public static final Configuration config = Configuration.getInstance();

    /**
     * Game (DSynergy) server. Server is UDP (connectionless).
     */
    public final GameServer gameServer;

    public static final int BUFF_SIZE = 8192; // append bytes (chunk) mainBuffer size

    public static final int FAIL_ATTEMPT_MAX = 10;
    public static final int TOTAL_FAIL_ATTEMPT_MAX = 3000;

    public static int TotalFailedAttempts = 0;

    /**
     * Create new Game Server processor which process receives request(s) and
     * sends (optional) response(s).
     *
     * @param gameServer game server (on top of this processor)
     */
    public GameServerProcessor(GameServer gameServer) {
        this.gameServer = gameServer;
    }

    /**
     * Event on message received.
     *
     * @param session session with (client) endpoint
     * @param message object message received
     *
     * @throws Exception
     */
    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        // Process recived (as request)
        GameServerProcessor.Result procResult = process(session, message);
        // Post process that request was processed (and move on . . )
        postProcess(procResult);
    }

    /**
     * Event on message sent
     *
     * @param session session with (client) endpoint
     * @param message object message sent
     * @throws Exception
     */
    @Override
    public void messageSent(IoSession session, Object message) throws Exception {

    }

    /**
     * Process request from clients and send response.
     *
     * @param session session with (client) endpoint
     * @param message message received. Usually IoBuffer.
     * @return result status of processing to the end point
     * @throws java.lang.Exception if errors on serialization
     */
    public GameServerProcessor.Result process(IoSession session, Object message) throws Exception {
        // Handle endpoint request and response
        final RequestIfc request;

        request = RequestIfc.receive(gameServer, session, message);

        if (request == null || request.getClientAddress() == null) {
            // avoid processing invalid requestList requestList
            return new Result(Status.INTERNAL_ERROR, null, null, "Invalid request - Is null or client address is null");
        }

        String clientGuid = request.getSenderGuid();
        final InetAddress clientAddress = request.getClientAddress();
        String clientHostName = clientAddress.getHostName();

        if (request == Request.INVALID) {
            // avoid processing invalid requestList requestList
            return new Result(Status.INTERNAL_ERROR, clientHostName, clientGuid, "Invalid request - Reason Unknown!");
        }

        // Handle null data type (Possible & always erroneous)
        if (request.getDataType() == null) {
            Response response = new Response(request.getId(), request.getChecksum(), ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, "Bad Request - Bad data type!");
            response.send(clientGuid, gameServer, session);

            return new Result(Status.INTERNAL_ERROR, clientHostName, clientGuid, "Bad Request - Bad data type!");
        }

        // If server has registered client and received request other than hello or goodbye (is probably invalid/malicious request)
        if (!gameServer.clients.containsIf(c -> c.getUniqueId().equals(clientGuid)) && request.getRequestType() != RequestIfc.RequestType.HELLO && request.getRequestType() != RequestIfc.RequestType.GOODBYE) {
            gameServer.assertTstFailure(clientHostName, clientGuid);
        }

        if (gameServer.blacklist.contains(clientHostName)
                || gameServer.clients.size() >= MAX_CLIENTS) {
            gameServer.assertTstFailure(clientHostName, clientGuid);

            return new Result(Status.CLIENT_ERROR, clientHostName, clientGuid, "Client is banned");
        }

        final ResponseIfc response;
        String msg;
        LevelActors levelActors;
        final int totalBytes;
        boolean okey;

        double gameTime;
        switch (request.getRequestType()) {
            case HELLO:
                ClientInfo clientInfo = gameServer.clients.getIf(c -> c.getUniqueId().equals(clientGuid));
                if (clientInfo != null && clientInfo.timeToLive == GameServer.TIME_TO_LIVE) {
                    msg = String.format("Bad Request - You are already connected to %s, v%s!", gameServer.worldName, gameServer.version);
                    response = new Response(request.getId(), request.getChecksum(), ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, msg);
                    response.send(clientGuid, gameServer, session);
                } else if (clientInfo == null) {
                    // Send a simple message with magic bytes prepended
                    msg = String.format("Hello, you are connected to %s, v%s, for help append \"help\" without quotes. Welcome!", gameServer.worldName, gameServer.version);
                    response = new Response(request.getId(), request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, msg);
                    gameServer.clients.add(new ClientInfo(session, clientHostName, clientGuid, GameServer.TIME_TO_LIVE));
                    gameServer.gameObject.gameWindow.setTitle(GameObject.WINDOW_TITLE + " - " + gameServer.worldName + " - Player Count: " + (gameServer.clients.size()));
                    response.send(clientGuid, gameServer, session);
                }
                break;
            case REGISTER:
                switch (request.getDataType()) {
                    case STRING: {
                        String newPlayerUniqueId = request.getData().toString();
                        levelActors = gameServer.gameObject.game.gameObject.levelContainer.levelActors;
                        if ((levelActors.otherPlayers.getIf(ot -> ot.uniqueId.equals(newPlayerUniqueId)) == null)) {
                            levelActors.otherPlayers.add(new Critter(this.gameServer.gameObject.GameAssets, newPlayerUniqueId, new Model(gameServer.gameObject.GameAssets.ALEX_BODY_DEFAULT)));
                            msg = String.format("Player ID is registered!", gameServer.worldName, gameServer.version);
                            response = new Response(request.getId(), request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, msg);

                            gameServer.gameObject.intrface.getConsole().write(String.format("Player %s has connected.", newPlayerUniqueId));
                            DSLogger.reportInfo(String.format("Player %s has connected.", newPlayerUniqueId), null);

                        } else {
                            msg = String.format("Player ID is invalid or already exists!", gameServer.worldName, gameServer.version);
                            response = new Response(request.getId(), request.getChecksum(), ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, msg);
                        }
                        break;
                    }
                    case OBJECT: {
                        String jsonStr = request.getData().toString();
                        PlayerInfo info = PlayerInfo.fromJson(jsonStr);
                        levelActors = gameServer.gameObject.game.gameObject.levelContainer.levelActors;
                        if ((levelActors.otherPlayers.getIf(ot -> ot.uniqueId.equals(info.uniqueId)) == null)) {
                            Critter critter = new Critter(this.gameServer.gameObject.GameAssets, info.uniqueId, new Model(gameServer.gameObject.GameAssets.ALEX_BODY_DEFAULT));
                            critter.setName(info.name);
                            critter.body.setPrimaryRGBAColor(info.color);
                            critter.body.texName = info.texModel;
                            levelActors.otherPlayers.add(critter);

                            gameServer.gameObject.intrface.getConsole().write(String.format("Player %s (%s) has connected.", info.name, info.uniqueId));
                            DSLogger.reportInfo(String.format("Player %s (%s) has connected.", info.name, info.uniqueId), null);

                            msg = String.format("Player ID is registered!", gameServer.worldName, gameServer.version);
                            response = new Response(request.getId(), request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, msg);
                        } else {
                            msg = String.format("Player ID is invalid or already exists!", gameServer.worldName, gameServer.version);
                            response = new Response(request.getId(), request.getChecksum(), ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, msg);
                        }
                        break;
                    }
                    default:
                        response = new Response(request.getId(), request.getChecksum(), ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, "Bad Request - Bad data type!");
                        break;
                }
                response.send(clientGuid, gameServer, session);
                break;
            case GOODBYE:
                msg = "Goodbye, hope we will see you again!";
                response = new Response(request.getId(), request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, msg);
                response.send(clientGuid, gameServer, session);
                gameServer.clients.removeIf(c -> c.uniqueId.equals(clientGuid));
                gameServer.gameObject.gameWindow.setTitle(GameObject.WINDOW_TITLE + " - " + gameServer.worldName + " - Player Count: " + (gameServer.clients.size()));
                if (clientGuid != null) {
                    GameServer.performCleanUp(gameServer.gameObject, clientGuid, false);
                }
                session.closeNow().await(GameServer.GOODBYE_TIMEOUT);
                break;
            case GET_TIME:
                gameTime = Game.gameTicks;
                response = new Response(request.getId(), request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.DOUBLE, gameTime);
                response.send(clientGuid, gameServer, session);
                break;
            case PING:
                msg = String.format("You pinged %s", gameServer.worldName);
                response = new Response(request.getId(), request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, msg);
                response.send(clientGuid, gameServer, session);
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
                            response = new Response(request.getId(), request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.OBJECT, obj);
                        } else if (playerIndex >= 0 && playerIndex < levelActors.otherPlayers.size()) {
                            vec3fPos = levelActors.otherPlayers.get(playerIndex).getPos();
                            vec3fView = levelActors.otherPlayers.get(playerIndex).getFront();
                            posInfo = new PosInfo(levelActors.player.uniqueId, vec3fPos, vec3fView);
                            obj = posInfo.toString();
                            response = new Response(request.getId(), request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.OBJECT, obj);
                        } else {
                            response = new Response(request.getId(), request.getChecksum(), ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, "Bad Request - Invalid argument!");
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
                            response = new Response(request.getId(), request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.OBJECT, obj);
                        } else {
                            Critter other = levelActors.otherPlayers.getIf(ply -> ply.uniqueId.equals(uuid));
                            if (other != null) {
                                vec3fPos = other.getPos();
                                vec3fView = other.getFront();
                                posInfo = new PosInfo(levelActors.player.uniqueId, vec3fPos, vec3fView);
                                obj = posInfo.toString();
                                response = new Response(request.getId(), request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.OBJECT, obj);
                            } else {
                                response = new Response(request.getId(), request.getChecksum(), ResponseIfc.ResponseStatus.ERR, DSObject.DataType.OBJECT, "Bad Request - Invalid Player ID or not registered!");
                            }
                        }
                        break;
                    }
                    default:
                        response = new Response(request.getId(), request.getChecksum(), ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, "Bad Request - Bad data type!");
                        break;
                }
                response.send(clientGuid, gameServer, session);
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
                totalBytes = gameServer.gameObject.levelContainer.levelBuffer.uploadBuffer.limit();
                final int bytesPerFragment = BUFF_SIZE;
                int fullFragments = totalBytes / bytesPerFragment;
                int remainingBytes = totalBytes % bytesPerFragment;

                int totalFragments = fullFragments + (remainingBytes > 0 ? 1 : 0);
                final ResponseIfc downloadResponse = new Response(request.getId(), request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.INT, totalFragments);
                try {
                    downloadResponse.send(clientGuid, gameServer, session);
                } catch (Exception ex) {
                    DSLogger.reportError(ex.getMessage(), ex);
                    throw new RuntimeException("Failed to send download response!", ex);
                }
                break;
            case GET_FRAGMENT:
                int n = (int) request.getData(); // Assuming the N-th fragment number is sent in the request data
                totalBytes = gameServer.gameObject.levelContainer.levelBuffer.uploadBuffer.limit();
                final ByteBuffer buffer = gameServer.gameObject.levelContainer.levelBuffer.uploadBuffer;

                if (n < 0 || n * BUFF_SIZE >= totalBytes) {
                    response = new Response(request.getId(), request.getChecksum(), ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, "Invalid fragment number");
                    response.send(clientGuid, gameServer, session);
                    return new Result(Status.CLIENT_ERROR, clientHostName, clientGuid, "Invalid fragment number!");
                }

                int fragmentStart = n * BUFF_SIZE;
                int fragmentEnd = Math.min(fragmentStart + BUFF_SIZE, totalBytes);
                int fragmentSize = fragmentEnd - fragmentStart;
                byte[] fragment = new byte[fragmentSize];

                buffer.position(fragmentStart); // Position at fragment
                buffer.get(fragment, 0, fragmentSize); // Read from BAK Buffer into fragment buffer
                buffer.rewind(); // Clear BAK Buffer, so it could be read from beginning

                response = new Response(request.getId(), request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.ARRAY, fragment);
                response.send(clientGuid, gameServer, session);

                DSLogger.reportInfo(String.format("Sent %d fragment, %d total bytes written", n, fragmentSize), null);
                break;
            case GET_PLAYER_INFO:
                levelActors = gameServer.gameObject.game.gameObject.levelContainer.levelActors;
                Gson gson = new Gson();
                IList<PlayerInfo> playerInfos = new GapList<>();
                playerInfos.add(new PlayerInfo(levelActors.player.getName(), levelActors.player.body.texName, levelActors.player.uniqueId, levelActors.player.body.getPrimaryRGBAColor(), levelActors.player.activeWeapon().getTexName()));
                levelActors.otherPlayers.forEach(op -> {
                    playerInfos.add(new PlayerInfo(op.getName(), op.body.texName, op.uniqueId, op.body.getPrimaryRGBAColor(), op.activeWeapon().getTexName()));
                });
                String obj = gson.toJson(playerInfos, IList.class);
                response = new Response(request.getId(), request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.OBJECT, obj);
                response.send(clientGuid, gameServer, session);
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

                gameServer.gameObject.intrface.getConsole().write(String.format("%s:%s", senderName, request.getData()));
                DSLogger.reportInfo(String.format("%s:%s", senderName, request.getData()), null);

                response = new Response(DSObject.NIL_ID, 0L, ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, senderName + ":" + request.getData());
                gameServer.clients.forEach(ci -> {
                    try {
                        if (!ci.uniqueId.equals(clientGuid)) {
                            response.send(ci.uniqueId, gameServer, ci.session);
                        }
                    } catch (Exception ex) {
                        DSLogger.reportError("Unable to deliver chat message, ex:", ex);
                    }
                });
                break;
            case WORLD_INFO:
                // Locate all level map files with dat or ndat extension
                final File clientDir = new File("./");
                final String worldNameEscaped = Pattern.quote(gameServer.worldName);
                Pattern pattern = Pattern.compile(worldNameEscaped + "\\.(n)?dat$", Pattern.CASE_INSENSITIVE);
                List<String> datFileList = Arrays.asList(clientDir.list((dir, name) -> pattern.matcher(name).find()));
                GapList<String> datFileListCopy = GapList.create(datFileList);
                String mapFileOrNull = datFileListCopy.getFirstOrNull();
                CRC32C checksum = new CRC32C();

                if (mapFileOrNull == null) {
                    mapFileOrNull = gameServer.worldName + ".ndat";
                    okey = gameServer.gameObject.levelContainer.levelBuffer.saveLevelToFile(mapFileOrNull);
                    if (!okey) {
                        return new Result(Status.INTERNAL_ERROR, clientHostName, clientGuid, "Internal error - Level still does not exist!");
                    }
                }

                if (mapFileOrNull == null) {
                    return new Result(Status.INTERNAL_ERROR, clientHostName, clientGuid, "Internal error - Level still does not exist!");
                }

                File mapFileLevel = new File(mapFileOrNull);
                if (!mapFileLevel.exists()) {
                    return new Result(Status.INTERNAL_ERROR, clientHostName, clientGuid, "Internal error - Level still does not exist!");
                }

                // calculating file size & checksum
                // with attend to send to the client
                try (FileChannel fileChannel = new FileInputStream(mapFileLevel).getChannel()) {
                    int sizeBytes = (int) Files.size(Path.of(mapFileOrNull));
                    ByteBuffer buffc = ByteBuffer.allocate((int) fileChannel.size());
                    while ((fileChannel.read(buffc)) > 0) {
                        // Do nothing, just read the file into the mainBuffer
                    }
                    buffc.flip();
                    checksum.update(buffc);

                    LevelMapInfo mapInfo = new LevelMapInfo(gameServer.worldName, checksum.getValue(), sizeBytes);
                    response = new Response(request.getId(), request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.OBJECT, mapInfo.toString());
                    response.send(clientGuid, gameServer, session);
                } catch (IOException ex) {
                    DSLogger.reportError(ex.getMessage(), ex);
                    return new Result(Status.INTERNAL_ERROR, clientHostName, clientGuid, "Internal error - Unable to read the level file!");
                }
                break;
            case SET_PLAYER_INFO:
                switch (request.getDataType()) {
                    case OBJECT: {
                        String jsonStro = request.getData().toString();
                        PlayerInfo info = PlayerInfo.fromJson(jsonStro);
                        levelActors = gameServer.gameObject.game.gameObject.levelContainer.levelActors;
                        Critter targCrit = levelActors.otherPlayers.getIf(ot -> ot.uniqueId.equals(info.uniqueId));
                        if (targCrit == null) {
                            msg = String.format("Players ID is not registered. Registration required!", gameServer.worldName, gameServer.version);
                            response = new Response(request.getId(), request.getChecksum(), ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, msg);
                        } else {
                            targCrit.setName(info.name);
                            targCrit.body.setPrimaryRGBAColor(info.color);
                            targCrit.body.texName = info.texModel;

                            IList<WeaponIfc> weaponsAsList = GapList.create(Arrays.asList(gameServer.gameObject.levelContainer.weapons.AllWeapons));
                            WeaponIfc weapon = weaponsAsList.getIf(w -> w.getTexName().equals(info.weapon));
                            if (weapon == null) { // if there is no weapon, switch to 'NONE' - unarmed, avoid nulls!
                                weapon = Weapons.NONE;
                            } else {
                                weapon = targCrit.activeWeapon(); // *LATER*
                            }
                            targCrit.switchWeapon(targCrit.getActiveHand());
                            response = new Response(request.getId(), request.getChecksum(), ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, "OK - Player info updated.");
                        }
                        break;
                    }
                    default:
                        response = new Response(request.getId(), request.getChecksum(), ResponseIfc.ResponseStatus.ERR, DSObject.DataType.STRING, "Bad Request - Bad data type!");
                        break;
                }
                response.send(clientGuid, gameServer, session);
                break;
        }

        return new Result(Status.OK, clientHostName, clientGuid, String.format("%s data= %s", request.getRequestType().name(), String.valueOf(request.getData())));
    }

    /**
     * Post processing result. After received request.
     *
     * Results could be written to console or to log.
     *
     * @param procResult process result
     */
    public void postProcess(GameServerProcessor.Result procResult) {
        final String msg;
        switch (procResult.status) {
            case INTERNAL_ERROR:
                msg = String.format("Server %s %s %s error!", procResult.hostname, procResult.guid, procResult.message);
                DSLogger.reportError(msg, null);
                gameServer.gameObject.intrface.getConsole().write(msg, Command.Status.FAILED);
                break;
            case CLIENT_ERROR:
                // kick violators
                gameServer.kickPlayer(procResult.guid);
                gameServer.assertTstFailure(procResult.hostname, procResult.guid);
                msg = String.format("Client %s %s %s error!", procResult.hostname, procResult.guid, procResult.message);
                DSLogger.reportError(msg, null);
                gameServer.gameObject.intrface.getConsole().write(msg, Command.Status.WARNING);
                if (gameServer.blacklist.contains(procResult.hostname)) {
                    DSLogger.reportWarning(msg, null);
                    gameServer.gameObject.intrface.getConsole().write(msg, Command.Status.FAILED);
                }
                break;
            default:
            case OK: // if client send valid request reset TTL to 120 and increase request per second (RPS)
                gameServer.clients.filter(client -> client.hostName.equals(procResult.hostname) && client.getUniqueId().equals(procResult.guid))
                        .forEach(client2 -> {
                            client2.timeToLive = GameServer.TIME_TO_LIVE;
                            client2.requestPerSecond++;
                        });
                if (config.getLogLevel() == DSLogger.DSLogLevel.DEBUG || config.getLogLevel() == DSLogger.DSLogLevel.ALL) {
                    msg = String.format("Client %s %s %s OK", procResult.hostname, procResult.guid, procResult.message);
                    DSLogger.reportInfo(msg, null);
                    // avoid spam client console
                    gameServer.gameObject.intrface.getConsole().write(msg);
                }
                break;

        }
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
