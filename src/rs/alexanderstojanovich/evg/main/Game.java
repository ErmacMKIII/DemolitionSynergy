/* 
 * Copyright (C) 2020 Alexander Stojanovich <coas91@rocketmail.com>
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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.ReadFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioDatagramConnector;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.audio.AudioFile;
import rs.alexanderstojanovich.evg.critter.Critter;
import rs.alexanderstojanovich.evg.critter.Observer;
import rs.alexanderstojanovich.evg.critter.Player;
import rs.alexanderstojanovich.evg.intrface.Command;
import rs.alexanderstojanovich.evg.level.Editor;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.net.DSMachine;
import rs.alexanderstojanovich.evg.net.DSObject;
import rs.alexanderstojanovich.evg.net.LevelMapInfo;
import rs.alexanderstojanovich.evg.net.PlayerInfo;
import rs.alexanderstojanovich.evg.net.PosInfo;
import rs.alexanderstojanovich.evg.net.Request;
import rs.alexanderstojanovich.evg.net.RequestIfc;
import rs.alexanderstojanovich.evg.net.ResponseIfc;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.GlobalColors;
import rs.alexanderstojanovich.evg.util.Pair;

/**
 * DSynergy Game client. With multiplayer capabilities.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Game extends IoHandlerAdapter implements DSMachine {

    private static final Configuration config = Configuration.getInstance();

    public static final int TPS = 80; // TICKS PER SECOND GENERATED
    public static final int TPS_HALF = 40; // HALF OF TPS
    public static final int TPS_QUARTER = 20; // QUARTER OF TPS ~ 250 ms
    public static final int TPS_EIGHTH = 10; // EIGHTH OF TPS (Used for Chunk Operations) ~ 125 ms
    public static final int TPS_SIXTEENTH = 5; // EIGHTH OF TPS 

    public static final int TPS_ONE = 1; // One tick ~ 12.5 ms
    public static final int TPS_TWO = 2; // Two ticks ~ 25 ms (Used for Chunk Optimization) ~ default
    public static final int TICKS_PER_UPDATE = config.getTicksPerUpdate(); // (1 - FLUID, 2 - EFFICIENT)

    public static final double TICK_TIME = 1.0 / (double) TPS;

    public static final float AMOUNT = 4.5f;
    public static final float CROUCH_STR_AMOUNT = 45f;
    public static final float JUMP_STR_AMOUNT = 115f;
    public static final float ANGLE = (float) (Math.PI / 180);

    public static final int FORWARD = 0;
    public static final int BACKWARD = 1;
    public static final int LEFT = 2;
    public static final int RIGHT = 3;

    private static int ups; // current handleInput per second    
    private static int fpsMax = config.getFpsCap(); // fps max or fps cap  

    // if this is reach game will close without exception!
    public static final double CRITICAL_TIME = 30.0;
    public static final int WAIT_RECEIVE_TIME = 45; // 45 Seconds

    private final boolean[] keys = new boolean[512];

    private static float lastX = 0.0f;
    private static float lastY = 0.0f;
    private static float xoffset = 0.0f;
    private static float yoffset = 0.0f;
    private static float mouseSensitivity = config.getMouseSensitivity();
    private boolean moveMouse = false;

    private int crosshairColorNum = 0;

    private final boolean[] mouseButtons = new boolean[8];

    private static GLFWKeyCallback defaultKeyCallback;
    private static GLFWCursorPosCallback defaultCursorCallback;
    private static GLFWMouseButtonCallback defaultMouseButtonCallback;

    public static final String ROOT = "/";
    public static final String CURR = "./";
    public static final String RESOURCES_DIR = "/rs/alexanderstojanovich/evg/resources/";

    public static final String DATA_ZIP = "dsynergy.zip";

    public static final String SCREENSHOTS = "screenshots";
    public static final String CACHE = "cache";

    public static final String INTRFACE_ENTRY = "intrface/";
    public static final String WEAPON_ENTRY = "weapons/";
    public static final String WORLD_ENTRY = "world/";
    public static final String EFFECTS_ENTRY = "effects/";
    public static final String SOUND_ENTRY = "sound/";
    public static final String CHARACTER_ENTRY = "character/";

    protected static double accumulator = 0.0;
    protected static double gameTicks = 0.0;
    protected final int version = GameObject.VERSION;

    public static enum Mode {
        FREE, SINGLE_PLAYER, MULTIPLAYER_HOST, MULTIPLAYER_JOIN, EDITOR
    };
    private static Mode currentMode = Mode.FREE;

    protected static boolean actionPerformed = false; // movement for all actors (critters)
    protected static boolean jumpPerformed = false; // jump for player
    protected static boolean crouchPerformed = false; // crouch for player
    protected static boolean causingCollision = false; // collision with solid environment (all critters)    
    protected boolean running = false;

    protected static final int DEFAULT_TIMEOUT = 30000; // 30 sec
    protected static final int DEFAULT_EXTENDED_TIMEOUT = 120000; // 2 minutes
    protected static final int DEFAULT_SHORTENED_TIMEOUT = 10000; // 10 seconds 'TIMEOUT FOR FRAGMENTS'

    public static final int MAX_ATTEMPTS = 3; // 'ATTEMPTS FOR FRAGMENTS'

    protected final IList<Pair<RequestIfc, Double>> requests = new GapList<>();

    protected double waitReceiveTime = 0.0; // seconds

    /**
     * Magic bytes of End-of-Stream
     */
    public static final byte[] EOS = {(byte) 0xAB, (byte) 0xCD, (byte) 0x0F, (byte) 0x15}; // 4 Bytes

    /**
     * Connect to server stuff & endpoint
     */
    protected String serverHostName = config.getServerIP();
    protected InetAddress serverInetAddr = null;

    protected int port = config.getClientPort();
    protected int timeout = DEFAULT_TIMEOUT;
    public static final int BUFF_SIZE = 8192; // read bytes (chunk) buffer size

    /**
     * Connection Status (Client)
     */
    public static enum ConnectionStatus {
        CONNECTED, NOT_CONNECTED, RECONNECTED
    }

    /**
     * Is (game) client connected to (game) server. Status
     */
    protected ConnectionStatus connected = ConnectionStatus.NOT_CONNECTED;

    /**
     * Player alleged position on the game server
     */
    public final Vector3f playerServerPos = new Vector3f();

    /**
     * Interpolation factor
     */
    public double interpolationFactor = 0.5;

    /**
     * Maximum number of level attempts (retransmission)
     */
    public static final int RETRANSMISSION_MAX_ATTEMPTS = 3;

    /**
     * Number of successive packets to receive before confirmation (Client)
     */
    public static final int PACKETS_MAX = 8;

    /**
     * Access to Game Engine.
     */
    public final GameObject gameObject;

    public int weaponIndex = 0;

    public static enum DownloadStatus {
        ERR, WARNING, OK
    }

    /**
     * Connection session
     */
    protected IoSession session = null;

    /**
     * UDP connector for (Game) client
     */
    protected NioDatagramConnector connector;

    /**
     * Construct new game (client) view. Demolition Synergy client.
     *
     * @param gameObject game object
     */
    public Game(GameObject gameObject) {
        this.gameObject = gameObject;
        Arrays.fill(keys, false);
        initCallbacks();
    }

    /**
     * Handles input for observer Collision detection is being handle in.
     *
     * @param amount movement amount
     *
     * @return did observer do something.. (changes on input)
     */
    private boolean observerDo(LevelContainer lc, float amount) {
        boolean changed = false;
        causingCollision = false;

        Observer obs = lc.levelActors.spectator;

        if ((keys[GLFW.GLFW_KEY_W] || keys[GLFW.GLFW_KEY_UP])) {
            if (causingCollision = LevelContainer.hasCollisionWithEnvironment(obs)) {
                obs.moveBackward(AMOUNT);
            } else {
                obs.moveForward(amount);
                changed = true;
            }
        }

        if ((keys[GLFW.GLFW_KEY_S] || keys[GLFW.GLFW_KEY_DOWN])) {
            if (causingCollision = LevelContainer.hasCollisionWithEnvironment(obs)) {
                obs.moveForward(AMOUNT);
            } else {
                obs.moveBackward(amount);
                changed = true;
            }
        }

        if (keys[GLFW.GLFW_KEY_A]) {
            if (causingCollision = LevelContainer.hasCollisionWithEnvironment(obs)) {
                obs.moveRight(AMOUNT);
            } else {
                obs.moveLeft(amount);
                changed = true;
            }
        }

        if (keys[GLFW.GLFW_KEY_D]) {
            if (causingCollision = LevelContainer.hasCollisionWithEnvironment(obs)) {
                obs.moveLeft(AMOUNT);
            } else {
                obs.moveRight(amount);
                changed = true;
            }
        }

        if (keys[GLFW.GLFW_KEY_PAGE_UP]) {
            if (causingCollision = LevelContainer.hasCollisionWithEnvironment(obs)) {
                obs.descend(AMOUNT);
            } else {
                obs.ascend(amount);
                changed = true;
            }
        }

        if (keys[GLFW.GLFW_KEY_PAGE_DOWN]) {
            if (causingCollision = LevelContainer.hasCollisionWithEnvironment(obs)) {
                obs.ascend(AMOUNT);
            } else {
                obs.descend(amount);
                changed = true;
            }
        }

        if (keys[GLFW.GLFW_KEY_LEFT]) {
            lc.levelActors.spectator.turnLeft(ANGLE);
            changed = true;
        }
        if (keys[GLFW.GLFW_KEY_RIGHT]) {
            lc.levelActors.spectator.turnRight(ANGLE);
            changed = true;
        }
        if (moveMouse) {
            lc.levelActors.spectator.lookAtOffset(mouseSensitivity, xoffset, yoffset);
            moveMouse = false;
            changed = true;
        }

        return changed;
    }

    /**
     * Handles input for editor (when in editor mode) Collision detection is
     * being handle in.
     *
     * @return did editor do something..
     */
    private boolean editorDo(LevelContainer lc) {
        boolean changed = false;

        if (keys[GLFW.GLFW_KEY_N]) {
            Editor.selectNew(lc);
            changed = true;
        }
        //----------------------------------------------------------------------
        if (mouseButtons[GLFW.GLFW_MOUSE_BUTTON_LEFT] && !keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectCurrSolid(lc);
            changed = true;
        }

        if (mouseButtons[GLFW.GLFW_MOUSE_BUTTON_LEFT] && keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectCurrFluid(lc);
            changed = true;
        }
        //----------------------------------------------------------------------
        if (keys[GLFW.GLFW_KEY_1] && !keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentSolid(lc, Block.LEFT);
            changed = true;
        }
        if (keys[GLFW.GLFW_KEY_2] && !keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentSolid(lc, Block.RIGHT);
            changed = true;
        }
        if (keys[GLFW.GLFW_KEY_3] && !keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentSolid(lc, Block.BOTTOM);
            changed = true;
        }
        if (keys[GLFW.GLFW_KEY_4] && !keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentSolid(lc, Block.TOP);
            changed = true;
        }
        if (keys[GLFW.GLFW_KEY_5] && !keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentSolid(lc, Block.BACK);
            changed = true;
        }
        if (keys[GLFW.GLFW_KEY_6] && !keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentSolid(lc, Block.FRONT);
            changed = true;
        }
        //----------------------------------------------------------------------
        if (keys[GLFW.GLFW_KEY_1] && keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentFluid(lc, Block.LEFT);
            changed = true;
        }
        if (keys[GLFW.GLFW_KEY_2] && keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentFluid(lc, Block.RIGHT);
            changed = true;
        }
        if (keys[GLFW.GLFW_KEY_3] && keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentFluid(lc, Block.BOTTOM);
            changed = true;
        }
        if (keys[GLFW.GLFW_KEY_4] && keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentFluid(lc, Block.TOP);
            changed = true;
        }
        if (keys[GLFW.GLFW_KEY_5] && keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentFluid(lc, Block.BACK);
            changed = true;
        }
        if (keys[GLFW.GLFW_KEY_6] && keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentFluid(lc, Block.FRONT);
            changed = true;

        }
        //----------------------------------------------------------------------
        if (keys[GLFW.GLFW_KEY_0] || keys[GLFW.GLFW_KEY_F]) {
            Editor.deselect();
            changed = true;
        }
        if (mouseButtons[GLFW.GLFW_MOUSE_BUTTON_RIGHT]) {
            Editor.add(lc);
            changed = true;
        }
        if (keys[GLFW.GLFW_KEY_R]) {
            Editor.remove(lc);
            changed = true;
        }

        return changed;
    }

    /**
     * Handle input for player (Single player mode & Multiplayer mode)
     *
     * @param lc level (environment) container
     * @param amountXZ movement amount on XZ plane
     * @param amountY vertical movement amount on Y-axis when jump,
     * @param amountYNeg vertical movement amount on Y-axis when sink,
     * @param deltaTime tick time
     *
     * @return did player do something..
     */
    public boolean singlePlayerDo(LevelContainer lc, float amountXZ, float amountY, float amountYNeg, float deltaTime) {
        boolean changed = false;
        causingCollision = false;
        final Player player = lc.levelActors.player;

        if ((keys[GLFW.GLFW_KEY_W] || keys[GLFW.GLFW_KEY_UP])) {
            player.movePredictorXZForward(amountXZ);
            if (causingCollision = LevelContainer.hasCollisionWithEnvironment((Critter) player)) {
                player.movePredictorXZBackward(amountXZ);
            } else {
                player.moveXZForward(amountXZ);
                changed = true;
            }
        }

        if ((keys[GLFW.GLFW_KEY_S] || keys[GLFW.GLFW_KEY_DOWN])) {
            player.movePredictorXZBackward(amountXZ);
            if (causingCollision = LevelContainer.hasCollisionWithEnvironment((Critter) player)) {
                player.movePredictorXZForward(amountXZ);
            } else {
                player.moveXZBackward(amountXZ);
                changed = true;
            }
        }

        if (keys[GLFW.GLFW_KEY_A]) {
            player.movePredictorXZLeft(amountXZ);
            if (causingCollision = LevelContainer.hasCollisionWithEnvironment((Critter) player)) {
                player.movePredictorXZRight(amountXZ);
            } else {
                player.moveXZLeft(amountXZ);
                changed = true;
            }
        }

        if (keys[GLFW.GLFW_KEY_D]) {
            player.movePredictorXZRight(amountXZ);
            if (causingCollision = LevelContainer.hasCollisionWithEnvironment((Critter) player)) {
                player.movePredictorXZLeft(amountXZ);
            } else {
                player.moveXZRight(amountXZ);
                changed = true;
            }
        }

        if (keys[GLFW.GLFW_KEY_SPACE] && ((!jumpPerformed))) {
            player.movePredictorYUp(amountY);
            if (causingCollision = LevelContainer.hasCollisionWithEnvironment((Critter) player)) {
                player.movePredictorYDown(amountY);
            } else {
                jumpPerformed |= lc.jump(player, amountY, deltaTime);
                changed = true;
            }
        }

        if ((keys[GLFW.GLFW_KEY_LEFT_CONTROL] || keys[GLFW.GLFW_KEY_RIGHT_CONTROL]) && ((!crouchPerformed))) {
            player.movePredictorYDown(amountYNeg);
            if (causingCollision = LevelContainer.hasCollisionWithEnvironment((Critter) player)) {
                player.movePredictorYUp(amountYNeg);
            } else {
                crouchPerformed |= lc.crouch(player, amountYNeg, deltaTime);
                changed = true;
            }
        }

        if (keys[GLFW.GLFW_KEY_LEFT]) {
            lc.levelActors.player.turnLeft(ANGLE);
            changed = true;
        }
        if (keys[GLFW.GLFW_KEY_RIGHT]) {
            lc.levelActors.player.turnRight(ANGLE);
            changed = true;
        }

        if (moveMouse) {
            lc.levelActors.player.lookAtOffset(mouseSensitivity, xoffset, yoffset);
            moveMouse = false;
            changed = true;
        }

        if (mouseButtons[GLFW.GLFW_MOUSE_BUTTON_LEFT]) {
            changed = true;
        }

        if (keys[GLFW.GLFW_KEY_1]) {
            weaponIndex = (++weaponIndex) & 15;
            gameObject.getLevelContainer().levelActors.getPlayer().switchWeapon(gameObject.levelContainer.weapons, weaponIndex);
            changed = true;
        }

        if (keys[GLFW.GLFW_KEY_2]) {
            weaponIndex = (--weaponIndex) & 15;
            gameObject.getLevelContainer().levelActors.getPlayer().switchWeapon(gameObject.levelContainer.weapons, weaponIndex);
            changed = true;
        }

        return changed;
    }

    /**
     * Handle input for player (Single player mode & Multiplayer mode)
     *
     * @param lc level (environment) container
     * @param amountXZ movement amount on XZ plane
     * @param amountY vertical movement amount on Y-axis when jump,
     * @param amountYNeg vertical movement amount on Y-axis when sink,
     * @param deltaTime tick time
     *
     * @return did player do something..
     * @throws java.lang.Exception if deserialization fails
     */
    public boolean multiPlayerDo(LevelContainer lc, float amountXZ, float amountY, float amountYNeg, float deltaTime) throws Exception {
        boolean changed = false;
        causingCollision = false;
        final Player player = lc.levelActors.player;

        // Multiplayer-Join mode
        if (isConnected() && currentMode == Mode.MULTIPLAYER_JOIN && gameObject.levelContainer.levelActors.player.isRegistered()) {
            RequestIfc playerPosReq = new Request(RequestIfc.RequestType.GET_POS, DSObject.DataType.STRING, player.uniqueId);
            playerPosReq.send(this, session);
            double time = GLFW.glfwGetTime();
            requests.add(new Pair<>(playerPosReq, time));

            if ((keys[GLFW.GLFW_KEY_W] || keys[GLFW.GLFW_KEY_UP])) {
                player.movePredictorXZForward(amountXZ);
                if (causingCollision = LevelContainer.hasCollisionWithEnvironment((Critter) player, playerServerPos, (float) interpolationFactor)) {
                    player.movePredictorXZBackward(amountXZ);
                } else {
                    player.moveXZForward(amountXZ);
                    changed = true;
                }
            }

            if ((keys[GLFW.GLFW_KEY_S] || keys[GLFW.GLFW_KEY_DOWN])) {
                player.movePredictorXZBackward(amountXZ);
                if (causingCollision = LevelContainer.hasCollisionWithEnvironment((Critter) player, playerServerPos, (float) interpolationFactor)) {
                    player.movePredictorXZForward(amountXZ);
                } else {
                    player.moveXZBackward(amountXZ);
                    changed = true;
                }
            }

            if (keys[GLFW.GLFW_KEY_A]) {
                player.movePredictorXZLeft(amountXZ);
                if (causingCollision = LevelContainer.hasCollisionWithEnvironment((Critter) player, playerServerPos, (float) interpolationFactor)) {
                    player.movePredictorXZRight(amountXZ);
                } else {
                    player.moveXZLeft(amountXZ);
                    changed = true;
                }
            }

            if (keys[GLFW.GLFW_KEY_D]) {
                player.movePredictorXZRight(amountXZ);
                if (causingCollision = LevelContainer.hasCollisionWithEnvironment((Critter) player, playerServerPos, (float) interpolationFactor)) {
                    player.movePredictorXZLeft(amountXZ);
                } else {
                    player.moveXZRight(amountXZ);
                    changed = true;
                }
            }

            if (keys[GLFW.GLFW_KEY_SPACE] && ((!jumpPerformed))) {
                player.movePredictorYUp(amountY);
                if (causingCollision = LevelContainer.hasCollisionWithEnvironment((Critter) player, playerServerPos, (float) interpolationFactor)) {
                    player.movePredictorYDown(amountY);
                } else {
                    jumpPerformed |= lc.jump(player, amountY, deltaTime);
                    changed = true;
                }
            }

            if ((keys[GLFW.GLFW_KEY_LEFT_CONTROL] || keys[GLFW.GLFW_KEY_RIGHT_CONTROL]) && ((!crouchPerformed))) {
                player.movePredictorYDown(amountYNeg);
                if (causingCollision = LevelContainer.hasCollisionWithEnvironment((Critter) player, playerServerPos, (float) interpolationFactor)) {
                    player.movePredictorYUp(amountYNeg);
                } else {
                    crouchPerformed |= lc.crouch(player, amountYNeg, deltaTime);
                    changed = true;
                }
            }

        } else if (currentMode == Mode.MULTIPLAYER_HOST && gameObject.levelContainer.levelActors.player.isRegistered()) {
            if ((keys[GLFW.GLFW_KEY_W] || keys[GLFW.GLFW_KEY_UP])) {
                player.movePredictorXZForward(amountXZ);
                if (causingCollision = LevelContainer.hasCollisionWithEnvironment((Critter) player)) {
                    player.movePredictorXZBackward(amountXZ);
                } else {
                    player.moveXZForward(amountXZ);
                    changed = true;
                }
            }

            if ((keys[GLFW.GLFW_KEY_S] || keys[GLFW.GLFW_KEY_DOWN])) {
                player.movePredictorXZBackward(amountXZ);
                if (causingCollision = LevelContainer.hasCollisionWithEnvironment((Critter) player)) {
                    player.movePredictorXZForward(amountXZ);
                } else {
                    player.moveXZBackward(amountXZ);
                    changed = true;
                }
            }

            if (keys[GLFW.GLFW_KEY_A]) {
                player.movePredictorXZLeft(amountXZ);
                if (causingCollision = LevelContainer.hasCollisionWithEnvironment((Critter) player)) {
                    player.movePredictorXZRight(amountXZ);
                } else {
                    player.moveXZLeft(amountXZ);
                    changed = true;
                }
            }

            if (keys[GLFW.GLFW_KEY_D]) {
                player.movePredictorXZRight(amountXZ);
                if (causingCollision = LevelContainer.hasCollisionWithEnvironment((Critter) player)) {
                    player.movePredictorXZLeft(amountXZ);
                } else {
                    player.moveXZRight(amountXZ);
                    changed = true;
                }
            }

            if (keys[GLFW.GLFW_KEY_SPACE] && (!jumpPerformed)) {
                player.movePredictorYUp(amountY);
                if (causingCollision = LevelContainer.hasCollisionWithEnvironment((Critter) player)) {
                    player.movePredictorYDown(amountY);
                } else {
                    jumpPerformed |= lc.jump(player, amountY, deltaTime);
                    changed = true;
                }
            }

            if ((keys[GLFW.GLFW_KEY_LEFT_CONTROL] || keys[GLFW.GLFW_KEY_RIGHT_CONTROL]) && ((!crouchPerformed))) {
                player.movePredictorYDown(amountYNeg);
                if (causingCollision = LevelContainer.hasCollisionWithEnvironment((Critter) player)) {
                    player.movePredictorYUp(amountYNeg);
                } else {
                    crouchPerformed |= lc.crouch(player, amountYNeg, deltaTime);
                    changed = true;
                }
            }
        }

        if (keys[GLFW.GLFW_KEY_LEFT]) {
            lc.levelActors.player.turnLeft(ANGLE);
            changed = true;
        }
        if (keys[GLFW.GLFW_KEY_RIGHT]) {
            lc.levelActors.player.turnRight(ANGLE);
            changed = true;
        }

        if (moveMouse) {
            lc.levelActors.player.lookAtOffset(mouseSensitivity, xoffset, yoffset);
            moveMouse = false;
            changed = true;
        }

        if (mouseButtons[GLFW.GLFW_MOUSE_BUTTON_LEFT]) {
            changed = true;
        }

        if (keys[GLFW.GLFW_KEY_1]) {
            weaponIndex = (++weaponIndex) & 15;
            gameObject.getLevelContainer().levelActors.getPlayer().switchWeapon(gameObject.levelContainer.weapons, weaponIndex);
            changed = true;
        }

        if (keys[GLFW.GLFW_KEY_2]) {
            weaponIndex = (--weaponIndex) & 15;
            gameObject.getLevelContainer().levelActors.getPlayer().switchWeapon(gameObject.levelContainer.weapons, weaponIndex);
            changed = true;
        }

        if (keys[GLFW.GLFW_KEY_R]) {
            changed = true;
        }

        // in case of multiplayer join send to the server
        if (changed && isConnected() && Game.currentMode == Mode.MULTIPLAYER_JOIN) {
            this.requestSetPlayerPosition();
        }

        return changed;
    }

    private void setCrosshairColor(Vector4f color) {
        gameObject.intrface.getCrosshair().setColor(color);
    }

    private void cycleCrosshairColor() {
        GlobalColors.ColorName[] values = GlobalColors.ColorName.values();
        setCrosshairColor(GlobalColors.getRGBAColorOrDefault(values[++crosshairColorNum % values.length]));
    }

    /**
     * Init input (keyboard & mouse)
     */
    private void initCallbacks() {
        GLFWErrorCallback.createPrint(System.err).set();

        defaultKeyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
                    gameObject.intrface.setShowHelp(false);
                    gameObject.intrface.getHelpText().setEnabled(false);
                    gameObject.intrface.getCollText().setEnabled(true);
                    if (currentMode == Mode.SINGLE_PLAYER || (currentMode == Mode.MULTIPLAYER_HOST || currentMode == Mode.MULTIPLAYER_JOIN)) {
                        gameObject.intrface.getGameMenu().open();
                    } else {
                        gameObject.intrface.getMainMenu().open();
                    }
                    gameObject.intrface.getGuideText().setEnabled(false);
                } else if (key == GLFW.GLFW_KEY_GRAVE_ACCENT && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
                    gameObject.intrface.getGuideText().setEnabled(false);
                    gameObject.intrface.getConsole().open();
                } else if (key == GLFW.GLFW_KEY_F1 && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
                    gameObject.intrface.toggleShowHelp();
                } else if (key == GLFW.GLFW_KEY_F2 && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
                    gameObject.intrface.getSaveDialog().open(gameObject.intrface);
                } else if (key == GLFW.GLFW_KEY_F3 && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
                    gameObject.intrface.getLoadDialog().open(gameObject.intrface);
                } else if (key == GLFW.GLFW_KEY_F4 && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
//                    gameObject.levelContainer.chunks.printInfo();
                } else if (key == GLFW.GLFW_KEY_F5 && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
//                    LevelContainer.printPositionMaps();
                } else if (key == GLFW.GLFW_KEY_F6 && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
                    gameObject.levelContainer.printQueues();
                } else if (key == GLFW.GLFW_KEY_F12 && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
                    FutureTask<Object> task = new FutureTask<>(Command.getCommand(Command.Target.SCREENSHOT));
                    GameRenderer.TASK_QUEUE.add(task);
                } else if (key == GLFW.GLFW_KEY_P && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    Arrays.fill(keys, false);
                    cycleCrosshairColor();
                } else if (key == GLFW.GLFW_KEY_M && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    Arrays.fill(keys, false);
                    Editor.cycleBlockColor();
                } else if (key == GLFW.GLFW_KEY_V && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    Arrays.fill(keys, false);
                    gameObject.levelContainer.levelActors.player.switchViewToggle();
                } else if (key == GLFW.GLFW_KEY_Y && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    Arrays.fill(keys, false);
                } else if (key == GLFW.GLFW_KEY_LEFT_BRACKET && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    Arrays.fill(keys, false);
                    Editor.selectPrevTexture();
                } else if (key == GLFW.GLFW_KEY_RIGHT_BRACKET && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    Arrays.fill(keys, false);
                    Editor.selectNextTexture();
                } else if (key != -1) {
                    if (action == GLFW.GLFW_PRESS) {
                        keys[key] = true;
                    } else if (action == GLFW.GLFW_RELEASE) {
                        keys[key] = false;
                    }
                }
            }
        };
        GLFW.glfwSetKeyCallback(gameObject.WINDOW.getWindowID(), defaultKeyCallback);

        GLFW.glfwSetInputMode(gameObject.WINDOW.getWindowID(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        GLFW.glfwSetCursorPos(gameObject.WINDOW.getWindowID(), gameObject.WINDOW.getWidth() / 2.0, gameObject.WINDOW.getHeight() / 2.0);
        defaultCursorCallback = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                float xposGL = (float) (xpos / gameObject.WINDOW.getWidth() - 0.5f) * 2.0f;
                float yposGL = (float) (0.5f - ypos / gameObject.WINDOW.getHeight()) * 2.0f;

                xoffset = xposGL - lastX;
                yoffset = yposGL - lastY;

                if (xoffset != 0.0f || yoffset != 0.0f) {
                    moveMouse = true;

                    lastX = (float) xposGL;
                    lastY = (float) yposGL;
                }
            }
        };
        GLFW.glfwSetCursorPosCallback(gameObject.WINDOW.getWindowID(), defaultCursorCallback);

        defaultMouseButtonCallback = new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                if (action == GLFW.GLFW_PRESS) {
                    mouseButtons[button] = true;
                } else if (action == GLFW.GLFW_RELEASE) {
                    mouseButtons[button] = false;
                }
            }
        };
        GLFW.glfwSetMouseButtonCallback(gameObject.WINDOW.getWindowID(), defaultMouseButtonCallback);
    }

    /**
     * Receive Async (Multiplayer)
     *
     * @param deltaTime time interval between updates
     */
    public void receiveAsync(double deltaTime) {
//        DSLogger.reportInfo("wait="+waitReceiveTime, null);
        // if waiting time is less or equal than 45 sec
        if (waitReceiveTime <= WAIT_RECEIVE_TIME) {
            ResponseIfc.receiveAsync(this, session).thenAccept((ResponseIfc resp) -> {
                // this is issued kick from the server to the Guid of this machine
                if (resp.getData().equals(getGuid())) {
                    DSLogger.reportInfo("You got kicked from the server!", null);
                    this.gameObject.intrface.getConsole().write("You got kicked from the server!");
                    disconnectFromServer();
                    gameObject.clearEverything();
                } else {
                    RequestIfc reqTarg = null;
                    double beginTime = 0.0;
                    for (Pair<RequestIfc, Double> req : requests) {
                        if (req.getKey().getChecksum() == resp.getChecksum()) {
                            reqTarg = req.getKey();
                            beginTime = req.getValue();
                            break;
                        }
                    }
                    double endTime = GLFW.glfwGetTime();
                    if (reqTarg != null) {
                        final RequestIfc reqTargCopy = reqTarg;
                        requests.removeIf(req -> req.getKey() == reqTargCopy || req.getValue() >= WAIT_RECEIVE_TIME);
                        switch (reqTarg.getRequestType()) {
                            case GET_TIME:
                                Game.gameTicks = (double) resp.getData();
                                break;
                            case GET_POS:
                                String uniqueId = reqTarg.getData().toString();
                                PosInfo posInfo = PosInfo.fromJson(resp.getData().toString());
                                if (uniqueId.equals(gameObject.levelContainer.levelActors.player.uniqueId)) {
                                    playerServerPos.set(posInfo.pos);
                                } else {
                                    Critter other = gameObject.levelContainer.levelActors.otherPlayers.getIf(player -> player.uniqueId.equals(uniqueId));
                                    other.setPos(posInfo.pos);
                                    other.getFront().set(posInfo.front);
                                    other.setRotationXYZ(posInfo.front);
                                }
                                double ping = endTime - beginTime;
                                // calculate interpolation factor
                                interpolationFactor = 0.5 * (double) deltaTime / ((double) ping + deltaTime);
                                break;
                            case PLAYER_INFO:
                                Gson gson = new Gson();
                                PlayerInfo[] infos = gson.fromJson((String) resp.getData().toString(), PlayerInfo[].class);
                                gameObject.levelContainer.levelActors.configOtherPlayers(infos);
                                break;
                            case SAY:
                                // write chat messages
                                gameObject.intrface.getConsole().write(resp.getData().toString());
                                break;
                        }

                    }
                    long tripTime = Math.round((endTime - beginTime) * 1000.0);
                    if (tripTime <= WAIT_RECEIVE_TIME * 1000.0) {
                        gameObject.WINDOW.setTitle(GameObject.WINDOW_TITLE + " - " + gameObject.game.getServerHostName() + " ( " + tripTime + " ms )");
                    }

                    // Reset waiting time (as response arrived to the request)
                    waitReceiveTime = 0L;

                    // Reset reconnected to connected
                    connected = ConnectionStatus.CONNECTED;
                }
            });
        } else {
            // attempt to reconnected
            if (!reconnect()) {
                this.disconnectFromServer();
                this.gameObject.clearEverything();
            } else {
                // success reset wait receive time
                waitReceiveTime = 0L;
            }

            this.gameObject.intrface.getConsole().write("Connection with the sever lost!", Command.Status.FAILED);
            DSLogger.reportError("Connection with the sever lost!", null);
        }
        waitReceiveTime += deltaTime;
    }

    /**
     * Updates game (client).
     *
     * @param deltaTime time interval between updates
     */
    public void update(double deltaTime) {
        // Time Synchronization
        if (Game.currentMode == Mode.MULTIPLAYER_JOIN && (ups & (Game.TPS_QUARTER - 1)) == 0 && isConnected()) {
            try {
                RequestIfc timeSyncReq = new Request(RequestIfc.RequestType.GET_TIME, DSObject.DataType.VOID, null);
                timeSyncReq.send(this, session);
                double time = GLFW.glfwGetTime();
                requests.add(new Pair<>(timeSyncReq, time));
            } catch (Exception ex) {
                DSLogger.reportError(ex.getMessage(), ex);
            }
        }

        // update with delta time like gravity or sun
        if ((ups & (TICKS_PER_UPDATE - 1)) == 0) {
            gameObject.update((float) deltaTime * TICKS_PER_UPDATE);

            // Multiplayer update - get player info ~ 250 ms
            if ((Game.currentMode == Mode.MULTIPLAYER_JOIN) && isConnected() && (ups & (TPS_QUARTER - 1)) == 0) {
                try {
                    // Send a simple player info message with magic bytes prepended
                    final RequestIfc piReq = new Request(RequestIfc.RequestType.PLAYER_INFO, DSObject.DataType.VOID, null);
                    piReq.send(this, session);
                    double time = GLFW.glfwGetTime();
                    requests.add(new Pair<>(piReq, time));
                } catch (Exception ex) {
                    DSLogger.reportError(ex.getMessage(), ex);
                }
            }

            // Multiplayer update - get position ~ 125 ms
            if ((Game.currentMode == Mode.MULTIPLAYER_JOIN) && isConnected() && (ups & (TPS_EIGHTH - 1)) == 0) {
                gameObject.levelContainer.levelActors.otherPlayers.forEach(other -> {
                    try {
                        RequestIfc otherPlayerRequest = new Request(RequestIfc.RequestType.GET_POS, DSObject.DataType.STRING, other.uniqueId);
                        otherPlayerRequest.send(this, session);
                        double time = GLFW.glfwGetTime();
                        requests.add(new Pair<>(otherPlayerRequest, time));
                    } catch (Exception ex) {
                        DSLogger.reportError(ex.getMessage(), ex);
                    }
                });
            }

            gameObject.determineVisibleChunks();
        }

        // receive (connection) responses async
        if (Game.currentMode == Mode.MULTIPLAYER_JOIN && isConnected()) {
            receiveAsync(deltaTime);
        }

        // Heavy operations to run afterwards
        // determine visible chunks (can be altered with player position)
        if ((ups & (TPS_QUARTER - 1)) == 0) {
            // call utility functions (chunk loading etc.)
            gameObject.utilChunkOperations();
        }

        // call utility functions (optimizing etc. - heavy operation)            
        if ((ups & (TPS_EIGHTH - 1)) == 0) {
            gameObject.utilOptimization();
        }
    }

    /**
     * Handle game input (client).
     *
     * @param deltaTime time interval between updates
     */
    public void handleInput(double deltaTime) {
        try {
            if ((ups & (TICKS_PER_UPDATE - 1)) == 0) {
                final float time = (float) deltaTime * Game.TICKS_PER_UPDATE;
                final float amount = Game.AMOUNT * time;
                actionPerformed = false;
                switch (currentMode) {
                    case FREE:
                        // nobody has control
                        break;
                    case EDITOR:
                        // observer has control
                        actionPerformed |= observerDo(gameObject.levelContainer, amount);
                        actionPerformed |= editorDo(gameObject.levelContainer);

                        if (actionPerformed) {
                            LevelContainer.updateActorInFluid(gameObject.levelContainer);
                        }
                        break;
                    case SINGLE_PLAYER:
                        // player has control
                        actionPerformed |= singlePlayerDo(gameObject.levelContainer, amount, JUMP_STR_AMOUNT, CROUCH_STR_AMOUNT, (float) (deltaTime));

                        if (actionPerformed) {
                            LevelContainer.updateActorInFluid(gameObject.levelContainer);
                        }

                        // causes of stopping repeateble jump (ups quarter, underwater, under gravity) ~ Author understanding
                        if (keys[GLFW.GLFW_KEY_SPACE]) {
                            jumpPerformed &= LevelContainer.isActorInFluid() ^ gameObject.levelContainer.levelActors.player.isUnderGravity();
                        } else if (!gameObject.levelContainer.levelActors.player.isUnderGravity()) {
                            jumpPerformed &= !LevelContainer.isActorInFluid();
                        }
                        crouchPerformed &= (keys[GLFW.GLFW_KEY_LEFT_CONTROL] || keys[GLFW.GLFW_KEY_RIGHT_CONTROL]);
                        break;
                    case MULTIPLAYER_HOST:
                    case MULTIPLAYER_JOIN:
                        // player has control
                        actionPerformed |= multiPlayerDo(gameObject.levelContainer, amount, JUMP_STR_AMOUNT, CROUCH_STR_AMOUNT, (float) (deltaTime));

                        if (actionPerformed) {
                            LevelContainer.updateActorInFluid(gameObject.levelContainer);
                        }

                        // causes of stopping repeateble jump (ups quarter, underwater, under gravity) ~ Author understanding
                        if (keys[GLFW.GLFW_KEY_SPACE]) {
                            jumpPerformed &= LevelContainer.isActorInFluid() ^ gameObject.levelContainer.levelActors.player.isUnderGravity();
                        } else if (!gameObject.levelContainer.levelActors.player.isUnderGravity()) {
                            jumpPerformed &= !LevelContainer.isActorInFluid();
                        }
                        crouchPerformed &= (keys[GLFW.GLFW_KEY_LEFT_CONTROL] || keys[GLFW.GLFW_KEY_RIGHT_CONTROL]);
                        break;
                }
                // display collision text
                gameObject.assertCheckCollision(causingCollision);
            }
        } catch (Exception ex) {
            DSLogger.reportError(ex.getMessage(), ex);
        }
    }

    /**
     * Starts the main loop. Main loop is called from main method. (From
     * GameObject indirectly)
     */
    public void go() {
        this.running = true;
        Game.setCurrentMode(Mode.FREE);
        ups = 0;
        accumulator = 0.0;

        // gameTicks is progressive only ingame time
        gameTicks = config.getGameTicks();
        double lastTime = GLFW.glfwGetTime(); // time is measured in seconds
        double currTime;
        double deltaTime;
        int index = 0; // track index

        // first time we got nothing
        actionPerformed = false;
        causingCollision = false;

        GLFW.glfwWaitEvents(); // prevent not responding in title from Windows

        gameObject.intrface.getMainMenu().open();

        while (!gameObject.WINDOW.shouldClose()) {
            currTime = GLFW.glfwGetTime();
            deltaTime = currTime - lastTime;
            gameTicks += deltaTime * Game.TPS;
            if (gameTicks >= Double.MAX_VALUE) {
                gameTicks = 0.0;
            }

            accumulator += deltaTime;
            lastTime = currTime;

            // Detecting critical status
            if (ups == 0 && deltaTime > CRITICAL_TIME) {
                DSLogger.reportFatalError("Game status critical!", null);
                gameObject.WINDOW.close();
                break;
            }

            if (!gameObject.isWorking() && !gameObject.musicPlayer.isPlaying()) {
                gameObject.musicPlayer.play(AudioFile.TRACKS[index++], true, false);
                if (index == AudioFile.TRACKS.length) {
                    index = 0;
                }
            }

            // Poll GLFW Events
            GLFW.glfwPollEvents();
            while (accumulator >= TICK_TIME) {
                // Handle input (interrupt) events (keyboard & mouse)
                handleInput(TICK_TIME);

                // Update with fixed timestep (environment)
                update(TICK_TIME);

                ups++;
                accumulator -= TICK_TIME;
            }
        }
        // stops the music        
        gameObject.getMusicPlayer().stop();
        this.running = false;
        DSLogger.reportDebug("Main loop ended.", null);
    }

    /**
     * Connect to server (host). Multiplayer. Requires acceptance test to be
     * passed. According to protocol.
     *
     * @return connection status changed (from not connected to connected)
     */
    public boolean connectToServer() {
        if (isConnected()) {
            DSLogger.reportWarning("Error - you are already connect to Game Server!", null);
            gameObject.intrface.getConsole().write("Error - you are already connect to Game Server!", Command.Status.WARNING);

            return false;
        }
        try {
            // create (instance) new UDP connector
            connector = new NioDatagramConnector();
            connector.setHandler(new IoHandlerAdapter());
            DSLogger.reportInfo(String.format("Trying to connect to server %s:%d ...", serverHostName, port), null);
            // Try to connect to server
            ConnectFuture connFuture = connector.connect(new InetSocketAddress(serverHostName, port));
            connFuture.await(timeout);

            // if managed to connect
            if (connFuture.isConnected()) {
                this.session = connFuture.getSession();
                this.session.getConfig().setUseReadOperation(true);
                this.session.getConfig().setReadBufferSize(BUFF_SIZE);

                // Send a simple hello message with magic bytes prepended
                final RequestIfc helloRequest = new Request(RequestIfc.RequestType.HELLO, DSObject.DataType.VOID, null);
                helloRequest.send(this, connFuture.getSession());

                // Wait for response (assuming simple echo for demonstration)            
                ResponseIfc response = ResponseIfc.receiveAsync(this, session).get(timeout, TimeUnit.MILLISECONDS);
                if (response.getChecksum() == helloRequest.getChecksum() && response.getResponseStatus() == ResponseIfc.ResponseStatus.OK) {
                    // Authenticated                    
                    DSLogger.reportInfo("Connected to server!", null);
                    connected = ConnectionStatus.CONNECTED;
                }

                DSLogger.reportInfo(String.format("Server response: %s : %s", response.getResponseStatus().toString(), String.valueOf(response.getData())), null);
                gameObject.intrface.getConsole().write(String.valueOf(response.getData()));

                if (connected == ConnectionStatus.CONNECTED) {
                    return true;
                }
            }
        } catch (IOException | TimeoutException ex) {
            DSLogger.reportError("Unable to connect to server!", ex);
            gameObject.intrface.getConsole().write("Unable to connect to server!", Command.Status.FAILED);
            DSLogger.reportError(ex.getMessage(), ex);
        } catch (Exception ex) {
            DSLogger.reportError("Error occurred!", ex);
            gameObject.intrface.getConsole().write("Error occurred!", Command.Status.FAILED);
            DSLogger.reportError(ex.getMessage(), ex);
        }

        return false;
    }

    /**
     * Reestablish session if connection break
     *
     * @return status changed by opreation
     */
    public boolean reconnect() {
        // session is broken
        DSLogger.reportError("Session lost. Attempting to reconnect!", null);
        gameObject.intrface.getConsole().write("Session lost. Attempting to reconnect!", Command.Status.WARNING);
        try {
            // attempt to send 'GOODBYE' request to the server
            disconnectFromServer();
            // close it
            session.closeNow();
            connected = ConnectionStatus.NOT_CONNECTED;

            // open new one
            // Try to connect to server
            if (connectToServer()) {
                connected = ConnectionStatus.RECONNECTED;
                return true;
            }
        } catch (Exception ex) {
            DSLogger.reportError(String.format("Error ocurred during reconnect {ex:%s}!", ex.getMessage()), ex);
        }

        // everything is okey
        return false;
    }

    /**
     * Connect to server (host). Multiplayer. Requires acceptance test to be
     * passed. According to protocol.
     *
     * @return endpoint if connection succeeds and acceptance test is passed
     */
    public boolean registerPlayer() {
        if (!isConnected()) {
            return false;
        }

        boolean okey = false;

        try {
            // Send a simple 'goodbye' message with magic bytes prepended
            final Player player = gameObject.levelContainer.levelActors.player;
            final RequestIfc register = new Request(RequestIfc.RequestType.REGISTER,
                    DSObject.DataType.OBJECT, new PlayerInfo(player.getName(), player.body.texName, player.uniqueId, player.body.getPrimaryRGBAColor()).toString()
            );
            register.send(this, session);

            // Wait for response (assuming simple echo for demonstration)            
            ResponseIfc response = ResponseIfc.receiveAsync(this, session).get(timeout, TimeUnit.MILLISECONDS);
            if (response.getResponseStatus() == ResponseIfc.ResponseStatus.OK) { // Authorised
                gameObject.levelContainer.levelActors.player.setRegistered(true);
                DSLogger.reportInfo(String.format("Server response: %s : %s", response.getResponseStatus().toString(), response.getData().toString()), null);
                gameObject.intrface.getConsole().write(response.getData().toString());
                okey |= true;
            } else {
                DSLogger.reportInfo(String.format("Server response: %s : %s", response.getResponseStatus().toString(), response.getData().toString()), null);
                gameObject.intrface.getConsole().write(response.getData().toString(), Command.Status.FAILED);
            }
        } catch (IOException | TimeoutException ex) {
            DSLogger.reportError("Network error(s) occurred!", ex);
            DSLogger.reportError(ex.getMessage(), ex);
        } catch (Exception ex) {
            DSLogger.reportError("Error occurred!", ex);
            DSLogger.reportError(ex.getMessage(), ex);
        }

        return okey;
    }

    /**
     * Checks if the end-of-stream marker is present in the received data.
     *
     * @param buffer The buffer containing the received data.
     * @param bytesRead The number of bytes read into the buffer.
     * @return True if the end-of-stream marker is found, otherwise false.
     */
//    private static boolean isEndOfStream(byte[] buffer, int bytesRead) {
//        if (bytesRead >= Game.EOS.length) {
//            for (int i = 0; i <= bytesRead - Game.EOS.length; i++) {
//                if (Arrays.equals(Arrays.copyOfRange(buffer, i, i + Game.EOS.length), Game.EOS)) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
    /**
     * Download level (map) from the server
     *
     * @return true if the download was successful, false otherwise
     */
    public DownloadStatus downloadLevel() {
        if (!isConnected()) {
            return DownloadStatus.ERR;
        }

        Arrays.fill(gameObject.levelContainer.buffer, (byte) 0x00);
        try {
            // Send a request to begin downloading the level
            final RequestIfc downlBeginRequest = new Request(RequestIfc.RequestType.DOWNLOAD, DSObject.DataType.VOID, null);
            downlBeginRequest.send(this, session);

            ResponseIfc response0 = ResponseIfc.receiveAsync(this, session).get(timeout, TimeUnit.MILLISECONDS);
            if (response0.getResponseStatus() == ResponseIfc.ResponseStatus.OK && (int) response0.getData() >= 0) {
                DSLogger.reportInfo(String.format("Server response: %s : %s", response0.getResponseStatus().toString(), response0.getData().toString()), null);
                // Display server response in client console
                gameObject.intrface.getConsole().write(String.format("Download %s fragments", response0.getData()));

                // Define a buffer to hold the received data
                byte[] buffer = new byte[Game.BUFF_SIZE];
                int totalBytesRead = 0;
                final int totalIndices = (int) response0.getData();

                if (totalIndices == 0) {
                    // World is empty
                    DSLogger.reportWarning("World is empty. Disconnecting.", null);

                    // World is empty
                    gameObject.intrface.getConsole().write("World is empty. Disconnecting.", Command.Status.WARNING);

                    disconnectFromServer();
                    gameObject.clearEverything();

                    return DownloadStatus.WARNING;
                }

                // Loop through all fragment indices
                for (int fragmentIndex = 0; fragmentIndex < totalIndices; fragmentIndex++) {
                    boolean success = false;

                    // Try up to MAX_ATTEMPTS to get the fragment
                    for (int attempt = 0; attempt < MAX_ATTEMPTS && !success; attempt++) {
                        // Request the next fragment
                        RequestIfc fragmentRequest = new Request(RequestIfc.RequestType.GET_FRAGMENT, DSObject.DataType.INT, fragmentIndex);
                        fragmentRequest.send(this, session);

                        // Wait for the response with a timeout
                        ReadFuture future = session.read();
                        if (future.await(Game.DEFAULT_SHORTENED_TIMEOUT)) { // 10 sec x 3 times
                            // Response received within timeout
                            Object message = future.getMessage();
                            if (message instanceof IoBuffer) {
                                IoBuffer buffer1 = (IoBuffer) message;
                                buffer1.get(buffer, 0, buffer1.remaining());
                                int bytesRead = buffer1.position();

                                // Write the received data to the buffer
                                System.arraycopy(buffer, 0, gameObject.levelContainer.buffer, totalBytesRead, bytesRead);
                                totalBytesRead += bytesRead;

                                DSLogger.reportInfo(String.format("Received %d fragment, bytes read: %d", fragmentIndex, bytesRead), null);
                                gameObject.intrface.getConsole().write(String.format("Received %d fragment, bytes read: %d", fragmentIndex, bytesRead));

                                success = true; // Fragment successfully received

                                if (connected == ConnectionStatus.RECONNECTED) {
                                    // Reset reconnected to connected
                                    connected = ConnectionStatus.CONNECTED;
                                }
                            }
                        } else {
                            // Timeout occurred, log and retry
                            DSLogger.reportWarning(String.format("Timeout while waiting for fragment %d, attempt %d", fragmentIndex, attempt + 1), null);

                            // Timeout occurred, log and retry
                            gameObject.intrface.getConsole().write(String.format("Timeout while waiting for fragment %d, attempt %d", fragmentIndex, attempt + 1), Command.Status.WARNING);

                            // Try reconnect if session is lost - otherwise no action
                            reconnect();
                        }
                    }

                    if (!success) {
                        // If not successful after MAX_ATTEMPTS, log the failure
                        DSLogger.reportError(String.format("Failed to receive fragment %d after %d attempts", fragmentIndex, MAX_ATTEMPTS), null);

                        // If not successful after MAX_ATTEMPTS, log the failure (Console)
                        gameObject.intrface.getConsole().write(String.format("Failed to receive fragment %d after %d attempts", fragmentIndex, MAX_ATTEMPTS), Command.Status.FAILED);

                        return DownloadStatus.ERR;
                    }
                }

                // Logging download information
                DSLogger.reportInfo(String.format("Download: %d bytes", totalBytesRead), null);
                gameObject.intrface.getConsole().write(String.format("Download: %d bytes", totalBytesRead));
            } else {
                // Handle error response from server
                DSLogger.reportInfo(String.format("Server response: %s : %s", String.valueOf(response0.getResponseStatus()), String.valueOf(response0.getData())), null);
                gameObject.intrface.getConsole().write(response0.getData().toString(), Command.Status.FAILED);
            }
        } catch (IOException | TimeoutException ex) {
            DSLogger.reportError("Unable to connect to server!", ex);
        } catch (Exception ex) {
            DSLogger.reportError("Error occurred!", ex);
        }

        return DownloadStatus.OK;
    }

    /**
     * Get Player Info (Json)
     *
     * @return Player Info
     */
    public PlayerInfo[] getPlayerInfo() {
        PlayerInfo[] result = null;
        if (isConnected()) {
            try {
                // Send a simple player info message with magic bytes prepended
                final RequestIfc piReq = new Request(RequestIfc.RequestType.PLAYER_INFO, DSObject.DataType.VOID, null);
                piReq.send(this, session);

                // Wait for response (assuming simple echo for demonstration)            
                ResponseIfc objResp = ResponseIfc.receiveAsync(this, session).get(timeout, TimeUnit.MILLISECONDS);
                DSLogger.reportInfo(String.format("Server response: %s : %s", objResp.getResponseStatus().toString(), objResp.getData().toString()), null);
                gameObject.intrface.getConsole().write(objResp.getData().toString());

                Gson gson = new Gson();
                result = gson.fromJson((String) objResp.getData(), PlayerInfo[].class);
            } catch (IOException | TimeoutException ex) {
                DSLogger.reportError("Network error(s) occurred!", ex);
                DSLogger.reportError(ex.getMessage(), ex);
            } catch (Exception ex) {
                DSLogger.reportError("Error occurred!", ex);
                DSLogger.reportError(ex.getMessage(), ex);
            }
        }

        return result;
    }

    /**
     * Disconnect (host) server. Multiplayer. If was no connected has no effect.
     */
    public void disconnectFromServer() {
        if (isConnected()) {
            try {
                // Send a simple 'goodbye' message with magic bytes prepended
                final RequestIfc goodByeRequest = new Request(RequestIfc.RequestType.GOODBYE, DSObject.DataType.VOID, null);
                goodByeRequest.send(this, session);
//                timeout = DEFAULT_SHORTENED_TIMEOUT;
//                serverEndpoint.setSoTimeout(timeout);
                // Wait for response (assuming simple echo for demonstration)  
                ResponseIfc.receiveAsync(this, session).thenApply((ResponseIfc response) -> {
                    DSLogger.reportInfo(String.format("Server response: %s : %s", response.getResponseStatus().toString(), response.getData().toString()), null);
                    gameObject.intrface.getConsole().write(response.getData().toString());
                    session.closeNow();
                    DSLogger.reportInfo("Disconnected from server!", null);

                    return response; // need "return this again"
                });
            } catch (IOException | TimeoutException ex) {
                DSLogger.reportError("Network error(s) occurred!", ex);
                DSLogger.reportError(ex.getMessage(), ex);
            } catch (Exception ex) {
                DSLogger.reportError("Error occurred!", ex);
                DSLogger.reportError(ex.getMessage(), ex);
            } finally {
                gameObject.levelContainer.levelActors.player.setRegistered(false);
                connector.getManagedSessions().values().forEach((IoSession ss) -> {
                    try {
                        ss.closeNow().await();
                    } catch (InterruptedException ex) {
                        DSLogger.reportError("Unable to close session!", ex);
                        DSLogger.reportError(ex.getMessage(), ex);
                    }
                });
                connector.dispose();
                connected = ConnectionStatus.NOT_CONNECTED;
            }
        }
    }

    /**
     * Request server to update player position.
     */
    public void requestSetPlayerPosition() {
        try {
            final Player player = gameObject.levelContainer.levelActors.player;
            final PosInfo posInfo = new PosInfo(player.uniqueId, player.getPos(), player.getFront());
            String posStr = posInfo.toString();
            RequestIfc posReq = new Request(RequestIfc.RequestType.SET_POS, DSObject.DataType.OBJECT, posStr);
            posReq.send(this, session);
        } catch (Exception ex) {
            DSLogger.reportError("Error occurred!", ex);
            DSLogger.reportError(ex.getMessage(), ex);
        }
    }

    /*
    * Frees all the callbacks. Called after the main loop.
     */
    public void cleanUp() {
        defaultCursorCallback.free();
        defaultKeyCallback.free();
        defaultMouseButtonCallback.free();
        GLFWErrorCallback.createPrint(System.err).free();
        DSLogger.reportDebug("Game cleaned up.", null);
    }

    /**
     * Get World info from the server (Multiplayer). Request is send and
     * response is awaited.
     *
     * @return
     */
    public LevelMapInfo getWorldInfo() {
        try {
            // Send a simple player info message with magic bytes prepended
            final RequestIfc wrldReq = new Request(RequestIfc.RequestType.WORLD_INFO, DSObject.DataType.VOID, null);
            wrldReq.send(this, session);

            // Wait for response (assuming simple echo for demonstration)            
            ResponseIfc objResp = ResponseIfc.receiveAsync(this, session).get(timeout, TimeUnit.MILLISECONDS);
            DSLogger.reportInfo(String.format("Server response: %s : %s", objResp.getResponseStatus().toString(), String.valueOf(objResp.getData())), null);
            gameObject.intrface.getConsole().write(String.valueOf(objResp.getData()));
            LevelMapInfo jsonWorldInfo = LevelMapInfo.fromJson(String.valueOf(objResp.getData()));

            return jsonWorldInfo;

        } catch (Exception ex) {
            DSLogger.reportError("Error occurred!", ex);
            DSLogger.reportError(ex.getMessage(), ex);
        }

        return null;
    }

    /**
     * Creates configuration from settings
     *
     * @param gameObject gameObject (contains binds)
     * @return Configuration cfg
     */
    public static Configuration makeConfig(GameObject gameObject) {
        Configuration cfg = Configuration.getInstance();
        cfg.setFpsCap(fpsMax);
        cfg.setWidth(gameObject.WINDOW.getWidth());
        cfg.setHeight(gameObject.WINDOW.getHeight());
        cfg.setFullscreen(gameObject.WINDOW.isFullscreen());
        cfg.setVsync(gameObject.WINDOW.isVsync());
        cfg.setWaterEffects(gameObject.waterRenderer.getEffectsQuality().ordinal());
        cfg.setShadowEffects(gameObject.shadowRenderer.getEffectsQuality().ordinal());
        cfg.setMouseSensitivity(mouseSensitivity);
        cfg.setMusicVolume(gameObject.getMusicPlayer().getGain());
        cfg.setSoundFXVolume(gameObject.getSoundFXPlayer().getGain());
        cfg.setServerIP(gameObject.game.serverHostName);
        cfg.setClientPort(gameObject.game.port);
        cfg.setLocalIP(gameObject.gameServer.localIP);
        cfg.setServerPort(gameObject.gameServer.port);
        cfg.setName(gameObject.levelContainer.levelActors.player.getName());
        cfg.setModel(gameObject.levelContainer.levelActors.player.body.getTexName());
        cfg.setColor(gameObject.levelContainer.levelActors.player.body.getPrimaryRGBColor());

        return cfg;
    }

    public boolean isConnected() {
        return connected == ConnectionStatus.CONNECTED;
    }

    public static GLFWKeyCallback getDefaultKeyCallback() {
        return defaultKeyCallback;
    }

    public static GLFWCursorPosCallback getDefaultCursorCallback() {
        return defaultCursorCallback;
    }

    public static GLFWMouseButtonCallback getDefaultMouseButtonCallback() {
        return defaultMouseButtonCallback;
    }

    public static void setGameTicks(double gameTicks) {
        Game.gameTicks = gameTicks;
    }

    public static int getUps() {
        return ups;
    }

    public static void setUps(int ups) {
        Game.ups = ups;
    }

    public static int getFpsMax() {
        return fpsMax;
    }

    public static void setFpsMax(int fpsMax) {
        Game.fpsMax = fpsMax;
    }

    public static float getMouseSensitivity() {
        return mouseSensitivity;
    }

    public static void setMouseSensitivity(float mouseSensitivity) {
        Game.mouseSensitivity = mouseSensitivity;
    }

    public static double getAccumulator() {
        return accumulator;
    }

    public static Mode getCurrentMode() {
        return currentMode;
    }

    public static void setCurrentMode(Mode currentMode) {
        Game.currentMode = currentMode;
    }

    public static float getLastX() {
        return lastX;
    }

    public static float getLastY() {
        return lastY;
    }

    public static float getXoffset() {
        return xoffset;
    }

    public static float getYoffset() {
        return yoffset;
    }

    public static double getGameTicks() {
        return gameTicks;
    }

    public static boolean isChanged() {
        return actionPerformed;
    }

    public static boolean isActionPerformed() {
        return actionPerformed;
    }

    public static void setActionPerformed(boolean actionPerformed) {
        Game.actionPerformed = actionPerformed;
    }

    public static void setCausingCollision(boolean causingCollision) {
        Game.causingCollision = causingCollision;
    }

    public static boolean isJumpPerformed() {
        return jumpPerformed;
    }

    public static boolean isCausingCollision() {
        return causingCollision;
    }

    public GameObject getGameObject() {
        return gameObject;
    }

    @Override
    public MachineType getMachineType() {
        return MachineType.DSCLIENT;
    }

    @Override
    public int getVersion() {
        return this.version;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public boolean isMoveMouse() {
        return moveMouse;
    }

    public void setMoveMouse(boolean moveMouse) {
        this.moveMouse = moveMouse;
    }

    public int getCrosshairColorNum() {
        return crosshairColorNum;
    }

    public void setCrosshairColorNum(int crosshairColorNum) {
        this.crosshairColorNum = crosshairColorNum;
    }

    public String getServerHostName() {
        return serverHostName;
    }

    public void setServerHostName(String serverHostName) {
        this.serverHostName = serverHostName;
    }

    public InetAddress getServerInetAddr() {
        return serverInetAddr;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getTimeout() {
        return timeout;
    }

    public Vector3f getPlayerServerPos() {
        return playerServerPos;
    }

    public IList<Pair<RequestIfc, Double>> getRequests() {
        return requests;
    }

    @Override
    public String getGuid() {
        return gameObject.levelContainer.levelActors.player.uniqueId;
    }

    public double getInterpolationFactor() {
        return interpolationFactor;
    }

    public IoSession getSession() {
        return session;
    }

}
