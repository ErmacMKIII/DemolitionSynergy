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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.zip.CRC32C;
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
import rs.alexanderstojanovich.evg.net.Response;
import rs.alexanderstojanovich.evg.net.ResponseIfc;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.GlobalColors;
import rs.alexanderstojanovich.evg.weapons.Weapons;

/**
 * DSynergy Game client. With multiplayer capabilities.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Game extends IoHandlerAdapter implements DSMachine {

    private static final Configuration config = Configuration.getInstance();

    /**
     * Ticks per second. Game Constant.
     */
    public static final int TPS = 80; // TICKS PER SECOND GENERATED
    public static final int TPS_HALF = 40; // HALF OF TPS
    public static final int TPS_QUARTER = 20; // QUARTER OF TPS ~ 250 ms
    public static final int TPS_EIGHTH = 10; // EIGHTH OF TPS (Used for Chunk Operations) ~ 125 ms
    public static final int TPS_SIXTEENTH = 5; // EIGHTH OF TPS 

    public static final int TPS_ONE = 1; // One tick ~ 12.5 ms
    public static final int TPS_TWO = 2; // Two ticks ~ 25 ms (Used for Chunk Optimization) ~ default
    private static int ticksPerUpdate = config.getTicksPerUpdate(); // (1 - FLUID, 2 - EFFICIENT, 3 - SAVING)

    /**
     * Time of the single ticks (constant). In seconds.
     */
    public static final double TICK_TIME = 1.0 / (double) TPS;

    public static final float AMOUNT = 12E-3f;
    public static final float CROUCH_STR_AMOUNT = 4f;
    public static final float JUMP_STR_AMOUNT = 7.5f;
    public static final float ANGLE = (float) (5E-4d * Math.PI / 180);

    public static final int FORWARD = 0;
    public static final int BACKWARD = 1;
    public static final int LEFT = 2;
    public static final int RIGHT = 3;

    private static int ups; // current handleInput per second    
    private static int fpsMax = config.getFpsCap(); // fps max or fps cap  

    // if this is reach game will close without exception!
    public static final double CRITICAL_TIME = 30.0;
    public static final int MAX_WAIT_RECEIVE_TIME = 45; // 45 Seconds

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
    protected static final int DEFAULT_SHORTENED_TIMEOUT = 10000; // 10 seconds 'TIMEOUT FOR FRAGMENTS' or 'GOODBYE'

    public static final int MAX_ATTEMPTS = 3; // 'ATTEMPTS FOR FRAGMENTS'
    public static final int REQUEST_FULFILLMENT_LENGTH = 8; // Used in calculaton for AVG ping
    public static final int MAX_SIZE = 16000; // Max Total Request List SIZE

    protected int fulfillNum = 0;
    public final Object internRequestMutex = new Object();
    protected final IList<RequestIfc> requests = new GapList<>();
    protected double pingSum = 0.0;
    protected double ping = 0.0;
    protected double waitReceiveTime = 0.0; // seconds

    /**
     * Connect to server stuff & endpoint
     */
    protected String serverHostName = config.getServerIP();
    protected InetAddress serverInetAddr = null;

    protected int port = config.getClientPort();
    protected int timeout = DEFAULT_TIMEOUT;
    public static final int BUFF_SIZE = 8192; // read bytes (chunk) buffer size

    /**
     * Async Client Executor (async receive responses)
     */
    public final ExecutorService clientExecutor = Executors.newSingleThreadExecutor();

    /**
     * Connection Status (Client)
     */
    public static enum ConnectionStatus {
        /**
         * Client is connected to Game Server
         */
        CONNECTED,
        /**
         * Client is not connected to Game Server
         */
        NOT_CONNECTED,
        /**
         * Client is reconnected to Game Server. Will transition to 'CONNECTED'.
         */
        RECONNECTED,
        /**
         * Client lost connection with the game server (presumably,
         * unwillingly). Will attempt to reconnect.
         */
        LOST
    }

    /**
     * When about to disconnect disabled
     */
    protected AtomicBoolean asyncReceivedEnabled = new AtomicBoolean(false);

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
     * Access to Game Engine.
     */
    public final GameObject gameObject;

    public int weaponIndex = 0;

    /**
     * Download status from the (world) level map
     */
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
    protected NioDatagramConnector connector = new NioDatagramConnector();

    /**
     * Construct new game (client) view. Demolition Synergy client.
     *
     * @param gameObject game object
     */
    public Game(GameObject gameObject) {
        this.gameObject = gameObject;
        // keys press and hold (init all released - false)
        Arrays.fill(keys, false);
        // init default callbacks (keyboard & mouse inputs)
        initCallbacks();
        // create (instance) new UDP connector            
        connector.setHandler(Game.this);
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

        if (keys[GLFW.GLFW_KEY_0]) {
            weaponIndex = (++weaponIndex) & 15;
            gameObject.getLevelContainer().levelActors.getPlayer().switchWeapon(Weapons.NONE);
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
        if (isConnected() && currentMode == Mode.MULTIPLAYER_JOIN && player.isRegistered() && isAsyncReceivedEnabled()) {
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

        } else if (currentMode == Mode.MULTIPLAYER_HOST && player.isRegistered()) {
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

        if (keys[GLFW.GLFW_KEY_0]) {
            weaponIndex = (++weaponIndex) & 15;
            gameObject.getLevelContainer().levelActors.getPlayer().switchWeapon(Weapons.NONE);
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
                } else if (key == GLFW.GLFW_KEY_Y && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    gameObject.intrface.getChat().open();
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
                }

                lastX = (float) xposGL;
                lastY = (float) yposGL;
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

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {

    }

    /**
     * Event on message received. Replacement for async receive.
     *
     * @param session session with (server) endpoint
     * @param message object message received
     *
     * @throws Exception
     */
    @Override
    public void messageReceived(IoSession session, Object message) throws Exception { // pool-3-thread-1
        if (isAsyncReceivedEnabled() && !gameObject.WINDOW.shouldClose()) {
            // receive & process 
            ResponseIfc.receiveAsync(this, session, clientExecutor).thenAccept((ResponseIfc response) -> {
                // '*' - indicates everyone
                // 'basically if this response is for me' - my (player) guid 
                if (response.getReceiverGuid().equals("*") || response.getReceiverGuid().equals(getGuid())) {
                    process(response);
                    if (config.getLogLevel() == DSLogger.DSLogLevel.DEBUG || config.getLogLevel() == DSLogger.DSLogLevel.ALL) {
                        gameObject.intrface.getConsole().write(String.valueOf(response.getData()), response.getResponseStatus() == ResponseIfc.ResponseStatus.OK ? Command.Status.SUCCEEDED : Command.Status.FAILED);
                        DSLogger.reportInfo(String.valueOf(response.getData()), null);
                    }
                }
            });

        }
    }

    /**
     * Process received response (client-side)
     *
     * @param response client received response
     */
    public void process(ResponseIfc response) {
        if (response.getChecksum() == 0L && String.valueOf(response.getData()).contains(":")) {
            // write chat messages
            gameObject.intrface.getConsole().write(String.valueOf(response.getData()));
            gameObject.intrface.getChat().write(String.valueOf(response.getData()));
            DSLogger.reportInfo(String.valueOf(response.getData()), null);
        }

        // this is issued kick from the server to the Guid of this machine
        if (response.getData().equals("KICK")) {
            // disable async 'read point' in 'message received'
            asyncReceivedEnabled.set(false);

            DSLogger.reportInfo("You got kicked from the server!", null);
            this.gameObject.intrface.getConsole().write("You got kicked from the server!");
            disconnectFromServer();
            gameObject.clearEverything();

            waitReceiveTime = 0L;
            return;
        }

        // choose the one which fits by unique checksum
        RequestIfc request;
        synchronized (internRequestMutex) {
            request = requests.filter(req -> req.getId().equals(response.getId())).getFirstOrNull();
            requests.remove(request);
        }
        if (request != null) {
            // detailed processing
            switch (request.getRequestType()) {
                case HELLO:
                    if (response.getReceiverGuid().equals(getGuid())
                            && response.getChecksum() == request.getChecksum()
                            && response.getResponseStatus() == ResponseIfc.ResponseStatus.OK) {
                        // Authenticated                    
                        DSLogger.reportInfo("Connected to server!", null);
                        connected = ConnectionStatus.CONNECTED;
                    } else {
                        // Got bad response or invalid checksum (either is bad)
                        // disconnect from server to avoid "unexisting connection"
                        disconnectFromServer();
                    }
                    DSLogger.reportInfo(String.format("Server response: %s : %s", response.getResponseStatus().toString(), String.valueOf(response.getData())), null);
                    gameObject.intrface.getConsole().write(String.valueOf(response.getData()));
                    // notify waiters on connect
                    synchronized (internRequestMutex) {
                        internRequestMutex.notify();
                    }
                    break;
                case REGISTER:
                    if (response.getChecksum() == request.getChecksum() && response.getResponseStatus() == ResponseIfc.ResponseStatus.OK) { // Authorised
                        gameObject.levelContainer.levelActors.player.setRegistered(true);

                        DSLogger.reportInfo(String.format("Server response: %s : %s", response.getResponseStatus().toString(), String.valueOf(response.getData())), null);
                        gameObject.intrface.getConsole().write(String.valueOf(response.getData()));
                    } else {
                        DSLogger.reportInfo(String.format("Server response: %s : %s", response.getResponseStatus().toString(), String.valueOf(response.getData())), null);
                        gameObject.intrface.getConsole().write(String.valueOf(response.getData()), Command.Status.FAILED);
                    }
                    break;
                case WORLD_INFO:
                    DSLogger.reportInfo(String.format("Server response: %s : %s", response.getResponseStatus().toString(), String.valueOf(response.getData())), null);
                    gameObject.intrface.getConsole().write(String.valueOf(response.getData()));
                    LevelMapInfo jsonWorldInfo = LevelMapInfo.fromJson(String.valueOf(response.getData()));
                    loadWorld(jsonWorldInfo);
                    break;
                case PING:
                    DSLogger.reportInfo(String.format("Server response: %s : %s", response.getResponseStatus().toString(), String.valueOf(response.getData())), null);
                    gameObject.intrface.getConsole().write(String.valueOf(response.getData()));
                    break;
                case GET_TIME:
                    Game.gameTicks = (double) response.getData();
                    break;
                case GET_POS:
                    String uniqueId = request.getData().toString();
                    PosInfo posInfo = PosInfo.fromJson(response.getData().toString());
                    if (uniqueId.equals(gameObject.levelContainer.levelActors.player.uniqueId)) {
                        playerServerPos.set(posInfo.pos);
                    } else {
                        Critter other = gameObject.levelContainer.levelActors.otherPlayers.getIf(player -> player.uniqueId.equals(uniqueId));
                        other.setPos(posInfo.pos);
                        other.getFront().set(posInfo.front);
                        other.setRotationXYZ(posInfo.front);
                    }
                    break;
                // get player info
                case GET_PLAYER_INFO:
                    Gson gson = new Gson();
                    PlayerInfo[] infos = gson.fromJson((String) response.getData().toString(), PlayerInfo[].class);
                    gameObject.levelContainer.levelActors.configOtherPlayers(infos);
                    break;
                case GOODBYE:
                    // disable async 'read point' in 'message received'
                    asyncReceivedEnabled.set(false); // no more 'messages' to read/process
                    // print GOODBYE message (disconnecting)
                    DSLogger.reportInfo(String.format("Server response: %s : %s", response.getResponseStatus().toString(), String.valueOf(response.getData())), null);
                    gameObject.intrface.getConsole().write(String.valueOf(response.getData()));
                    DSLogger.reportInfo("Disconnected from server!", null);
                    // notify wait on disconnect
                    synchronized (internRequestMutex) {
                        internRequestMutex.notifyAll();
                    }
                    break;
            }

            if (fulfillNum < Game.REQUEST_FULFILLMENT_LENGTH) {
                // end time is current timestamp (millis)
                double endTime = System.currentTimeMillis();
                // Affilate response time recevied with request time sent
                // initial value -- gonna be replaced with request begin time
                double beginTime = request.getTimestamp();

                // trip time millis, multiplied by cuz it is called in a loop!
                double tripTime = endTime - beginTime;
                // add to ping sum (as avg ping is displayed in window title)
                pingSum += tripTime;
                fulfillNum++;
            } else {
                // calculate average ping for 8 requests
                ping = pingSum / (double) fulfillNum;

                // reset measurements
                pingSum = 0.0;
                fulfillNum = 0;
            }

            // Reset waiting time (as response arrived to the request)
            waitReceiveTime = 0L;
        }
    }

    /**
     * Wait Async (Multiplayer) until last response received.
     *
     *
     * @param deltaTime time interval between updates
     * @throws java.lang.Exception
     */
    public void waitAsync(double deltaTime) throws Exception {
        if (!isAsyncReceivedEnabled() || requests.isEmpty()) {
            return;
        }

        // clean long time unhandled requests
        // remove all X-requests which exceed max wait time of 45 sec
        synchronized (internRequestMutex) {
            requests.removeIf(x -> (System.currentTimeMillis() - x.getTimestamp()) / 1000.0 > MAX_WAIT_RECEIVE_TIME);
        }

        // if too much requests sent close connection with server
        synchronized (internRequestMutex) {
            if (requests.size() >= MAX_SIZE) {
                throw new Exception("Too much request sent. Connection will abort!");
            }
        }

        // if waiting time is less or equal than 45 sec
        synchronized (internRequestMutex) {
            if (waitReceiveTime > MAX_WAIT_RECEIVE_TIME) {
                connected = ConnectionStatus.LOST;
                // don't block main thread if game is unresponsive
                CompletableFuture.runAsync(() -> {
                    // if not async received enabled ~ probably kick occurred so 'don't fall' down the code
                    if (!isAsyncReceivedEnabled() || connected != ConnectionStatus.LOST) {
                        return;
                    }

                    // attempt to reconnected
                    if (!reconnect()) {
                        this.gameObject.intrface.getConsole().write("Connection with the server lost!", Command.Status.FAILED);
                        DSLogger.reportError("Connection with the server lost!", null);
                        this.disconnectFromServer();
                        this.gameObject.clearEverything();
                    } else {
                        if (connected == ConnectionStatus.RECONNECTED) {
                            // Reset reconnected to connected
                            connected = ConnectionStatus.CONNECTED;

                            // enable async received (player connected)
                            asyncReceivedEnabled.set(true);
                        }

                        // success reset wait receive time
                        waitReceiveTime = 0L;
                    }
                }, gameObject.TaskExecutor);
            }
        }

        // display ping in game window title
        gameObject.WINDOW.setTitle(GameObject.WINDOW_TITLE + " - " + gameObject.game.getServerHostName() + String.format(" (%.1f ms)", ping));

        // calculate interpolation factor
        interpolationFactor = 0.5 * (double) deltaTime / ((double) ping + deltaTime);

        waitReceiveTime += deltaTime;
    }

    /**
     * Updates game (client).
     *
     * @param deltaTime time interval between updates
     */
    public void update(double deltaTime) {
        // Time Synchronization
        if (Game.currentMode == Mode.MULTIPLAYER_JOIN && (ups & (Game.TPS_QUARTER - 1)) == 0 && isConnected() && isAsyncReceivedEnabled()) {
            try {
                RequestIfc timeSyncReq = new Request(RequestIfc.RequestType.GET_TIME, DSObject.DataType.VOID, null);
                timeSyncReq.send(this, session);
                synchronized (internRequestMutex) {
                    if (requests.size() < MAX_SIZE) {
                        requests.add(timeSyncReq);
                    }
                }
            } catch (Exception ex) {
                DSLogger.reportError(ex.getMessage(), ex);
            }
        }

        // update with delta time like gravity or sun
        if ((ups & (ticksPerUpdate - 1)) == 0) {
            // Update World (Sun, Earth motion, etc..)
            gameObject.update((float) deltaTime * ticksPerUpdate);

            // determine visible chunk ids and put into the list (or queue)
            gameObject.determineVisibleChunks();

            // Multiplayer update - get player info ~ 250 ms
            if ((Game.currentMode == Mode.MULTIPLAYER_JOIN) && isConnected() && (ups & (TPS_QUARTER - 1)) == 0 && isAsyncReceivedEnabled()) {
                try {
                    // Send a simple player info message with magic bytes prepended
                    final RequestIfc piReq = new Request(RequestIfc.RequestType.GET_PLAYER_INFO, DSObject.DataType.VOID, null);
                    piReq.send(this, session);
                    synchronized (internRequestMutex) {
                        if (requests.size() < MAX_SIZE) {
                            requests.add(piReq);
                        }
                    }
                } catch (Exception ex) {
                    DSLogger.reportError(ex.getMessage(), ex);
                }
            }

            // Multiplayer update - get position ~ 125 ms
            if ((Game.currentMode == Mode.MULTIPLAYER_JOIN) && isConnected() && (ups & (TPS_EIGHTH - 1)) == 0 && isAsyncReceivedEnabled()) {
                // request get player position (interpolation)
                this.requestGetPlayerPos();

                // update player model guns etc.
                this.requestUpdatePlayer();

                // request update player view & pos
                this.requestGetOtherPlayersPos();
            }

            // Multiplayer update - set player position
            if ((Game.currentMode == Mode.MULTIPLAYER_JOIN) && isConnected() && (actionPerformed || moveMouse) && isAsyncReceivedEnabled()) {
                // request to set player view/position. Send update to the server.
                this.requestSetPlayerPos();
            }

        }

        // receive (connection) responses async
        if (Game.currentMode == Mode.MULTIPLAYER_JOIN && isConnected()) {
            try {
                waitAsync(deltaTime);
            } catch (Exception ex) {
                disconnectFromServer();
                gameObject.clearEverything();
                DSLogger.reportError(ex.getMessage(), ex);
            }
        }

    }

    /**
     * Heavy util operation. Takes lot of CPU time.
     */
    public void util() {
        if ((ups & (TPS_QUARTER - 1)) == 0) {
            // call utility functions (chunk loading etc.)
            gameObject.utilChunkOperations();
        }

        if ((ups & (TPS_EIGHTH - 1)) == 0) {
            // block optimization (separate visible from not visible)
            gameObject.utilOptimization();
            // clear visible and invisible chunk lists
            gameObject.clearChunkLists();
        }
    }

    /**
     * Handle game input (client).
     *
     * @param deltaTime time interval between updates
     */
    public void handleInput(double deltaTime) {
        try {
            if ((ups & (ticksPerUpdate - 1)) == 0) {
                final float time = (float) deltaTime * ticksPerUpdate;
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

                        // causes of stopping repeateble jump (underwater, under gravity) ~ Author understanding
                        jumpPerformed &= !((LevelContainer.isActorInFluid()) || !gameObject.levelContainer.levelActors.player.isUnderGravity());
                        crouchPerformed = false;
                        break;
                    case MULTIPLAYER_HOST:
                    case MULTIPLAYER_JOIN:
                        // player has control
                        actionPerformed |= multiPlayerDo(gameObject.levelContainer, amount, JUMP_STR_AMOUNT, CROUCH_STR_AMOUNT, (float) (deltaTime));

                        if (actionPerformed) {
                            LevelContainer.updateActorInFluid(gameObject.levelContainer);
                        }

                        // causes of stopping repeateble jump (ups quarter, underwater, under gravity) ~ Author understanding
                        jumpPerformed &= !((LevelContainer.isActorInFluid()) || !gameObject.levelContainer.levelActors.player.isUnderGravity());
                        crouchPerformed = false;
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
        gameObject.intrface.getGuideText().setEnabled(false);

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

            // util operations (heavy CPU time), requires a single tick+
            if (accumulator >= TICK_TIME && !GameRenderer.couldRender()) {
                // interrupt rendering, is okey
                gameObject.renderer.interrupt();
                // util chunks (~chunk loader to Author understanding)
                util();
            }

            // Poll GLFW Events
            GLFW.glfwPollEvents();
            // Handle input (interrupt) events (keyboard & mouse)
            handleInput(TICK_TIME);

            // Fixed time-step
            while (accumulator >= TICK_TIME) {
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
            DSLogger.reportWarning("Error - you are already connected to Game Server!", null);
            gameObject.intrface.getConsole().write("Error - you are already connected to Game Server!", Command.Status.WARNING);

            return false;
        }
        try {

            // set default timeout (30 sec)
            timeout = Game.DEFAULT_TIMEOUT;
            DSLogger.reportInfo(String.format("Trying to connect to server %s:%d ...", serverHostName, port), null);
            // Try to connect to server
            ConnectFuture connFuture = connector.connect(new InetSocketAddress(serverHostName, port));
            connFuture.await(timeout);

            // if managed to connect
            if (connFuture.isConnected()) {
                this.session = connFuture.getSession();
                this.session.getConfig().setUseReadOperation(true);
                this.session.getConfig().setReadBufferSize(BUFF_SIZE);

                // enable async receive
                asyncReceivedEnabled.set(true);

                // Send a simple hello message with magic bytes prepended
                final RequestIfc helloRequest = new Request(RequestIfc.RequestType.HELLO, DSObject.DataType.VOID, null);
                helloRequest.send(this, connFuture.getSession());
                // add to requests to they can be 'processed' in 'messageReceived'
                synchronized (internRequestMutex) {
                    if (requests.size() < MAX_SIZE) {
                        requests.add(helloRequest);
                    }
                    // Wait for response (assuming simple echo for demonstration)
                    internRequestMutex.wait(timeout);
                }

                // if connected changed status to 'CONNECTED'
                if (connected == ConnectionStatus.CONNECTED) {
                    return true;
                }
            }
        } catch (Exception ex) {
            DSLogger.reportError("Unable to connect to server!", ex);
            gameObject.intrface.getInfoMsgText().setContent("Unable to connect to server!");
            gameObject.intrface.getConsole().write("Unable to connect to server!", Command.Status.FAILED);
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
                    DSObject.DataType.OBJECT, new PlayerInfo(player.getName(), player.body.texName, player.uniqueId, player.body.getPrimaryRGBAColor(), player.getWeapon().getTexName()).toString()
            );
            register.send(this, session);

//            synchronized (internRequestMutex) {
//                if (requests.size() < MAX_SIZE) {
//                    requests.add(register);
//                }
//                // Wait for response (assuming simple echo for demonstration)
//                internRequestMutex.wait(DEFAULT_SHORTENED_TIMEOUT);
//            }
            okey = true;

        } catch (Exception ex) {
            DSLogger.reportError("Network error(s) occurred!", ex);
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

            ResponseIfc response0 = ResponseIfc.receive(this, session);
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
                            if (connected == ConnectionStatus.LOST) {
                                reconnect();
                            }
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
                gameObject.intrface.getConsole().write(String.valueOf(response0.getData()), Command.Status.FAILED);
            }
        } catch (Exception ex) {
            DSLogger.reportError("Unable to connect to server!", ex);
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
                final RequestIfc piReq = new Request(RequestIfc.RequestType.GET_PLAYER_INFO, DSObject.DataType.VOID, null);
                piReq.send(this, session);
                synchronized (internRequestMutex) {
                    if (requests.size() < MAX_SIZE) {
                        requests.add(piReq);
                    }
                }
            } catch (Exception ex) {
                DSLogger.reportError("Network error(s) occurred!", ex);
                DSLogger.reportError(ex.getMessage(), ex);
            }
        }

        return result;
    }

    /**
     * Disconnect (host) server. Multiplayer. If was no connected has no effect.
     * Usually player has this variant of graceful disconnect. For response
     * "Goodbye, hope we will see you again!" is awaited.
     */
    public void disconnectFromServer() {
        if (isConnected()) {
            try {
                // short timeout
                timeout = Game.DEFAULT_SHORTENED_TIMEOUT;

                // Send a simple 'goodbye' message with magic bytes prepended
                final RequestIfc goodByeRequest = new Request(RequestIfc.RequestType.GOODBYE, DSObject.DataType.VOID, null);
                goodByeRequest.send(this, session);

                if (!gameObject.WINDOW.shouldClose()) { // if 'EXIT' fomr Main Menu or 'QUIT/EXIT' command don't wait for response from the (game) server
                    synchronized (internRequestMutex) {
                        if (requests.size() < MAX_SIZE) {
                            requests.add(goodByeRequest);
                        }

                        // we have async receive enabled, wait to receive the last message
                        internRequestMutex.wait(timeout); // goodbye response will notify us & avoid deadlock if server does not respond with goodbye msg (rare)
                    }
                }

                // player is not registered anymore
                gameObject.levelContainer.levelActors.player.setRegistered(false);
                session.closeOnFlush().await(timeout);
                connector.getManagedSessions().values().forEach((IoSession ss) -> {
                    try {
                        ss.closeNow().await(DEFAULT_SHORTENED_TIMEOUT);
                    } catch (InterruptedException ex) {
                        DSLogger.reportError("Unable to close session!", ex);
                        DSLogger.reportError(ex.getMessage(), ex);
                    }
                });

                fulfillNum = 0;
                pingSum = 0.0;
                ping = 0.0;

                requests.clear();

                connected = ConnectionStatus.NOT_CONNECTED;
                // !IMPORTANT -- DISABLE ASYNC READPOINT/WRITEPOINT (so it doesn't block in process)
                asyncReceivedEnabled.set(false);
            } catch (Exception ex) {
                DSLogger.reportError("Error occurred!", ex);
                DSLogger.reportError(ex.getMessage(), ex);
            }
        }
    }

    /**
     * Request server to update player position.
     */
    public void requestSetPlayerPos() {
        try {
            // Send update about player position to the server
            final Player player = gameObject.levelContainer.levelActors.player;
            final PosInfo posInfo = new PosInfo(player.uniqueId, player.getPos(), player.getFront());
            String posStr = posInfo.toString();
            RequestIfc posReq = new Request(RequestIfc.RequestType.SET_POS, DSObject.DataType.OBJECT, posStr);
            posReq.send(this, session);
            synchronized (internRequestMutex) {
                if (requests.size() < MAX_SIZE) {
                    requests.add(posReq);
                }
            }
        } catch (Exception ex) {
            DSLogger.reportError("Error occurred!", ex);
            DSLogger.reportError(ex.getMessage(), ex);
        }
    }

    /*
    * Frees all the callbacks. Called after the main loop.
     */
    public void cleanUp() {
        clientExecutor.shutdown();
        connector.dispose();

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
     * @return success (true) or failure (false) boolean result
     */
    public boolean getWorldInfo() {
        try {
            // Send a request to obtain world info
            final RequestIfc wrldReq = new Request(RequestIfc.RequestType.WORLD_INFO, DSObject.DataType.VOID, null);
            wrldReq.send(this, session);
            synchronized (internRequestMutex) {
                if (requests.size() < MAX_SIZE) {
                    requests.add(wrldReq);
                }
                internRequestMutex.wait(DEFAULT_SHORTENED_TIMEOUT);
            }

            return true;
        } catch (Exception ex) {
            DSLogger.reportError("Network Error occurred!", ex);
            DSLogger.reportError(ex.getMessage(), ex);
        }

        return false;
    }

    /**
     * Request server to get the player pos on the server. It is used to
     * interpolate the client once client is aware where the position of the
     * player on server is.
     */
    public void requestGetPlayerPos() {
        RequestIfc playerPosReq = new Request(RequestIfc.RequestType.GET_POS, DSObject.DataType.STRING, gameObject.levelContainer.levelActors.player.uniqueId);
        try {
            playerPosReq.send(this, session);
            synchronized (internRequestMutex) {
                if (requests.size() < MAX_SIZE) {
                    requests.add(playerPosReq);
                }
            }
        } catch (Exception ex) {
            DSLogger.reportError(ex.getMessage(), ex);
        }
    }

    /**
     * Request (client) update of player info from the server for other players
     */
    public void requestGetOtherPlayersPos() {
        gameObject.levelContainer.levelActors.otherPlayers.forEach(other -> {
            try {
                RequestIfc otherPlayerRequest = new Request(RequestIfc.RequestType.GET_POS, DSObject.DataType.STRING, other.uniqueId);
                otherPlayerRequest.send(this, session);
                synchronized (internRequestMutex) {
                    if (requests.size() < MAX_SIZE) {
                        requests.add(otherPlayerRequest);
                    }
                }
            } catch (Exception ex) {
                DSLogger.reportError(ex.getMessage(), ex);
            }
        });
    }

    /**
     * Request update of player info to the server. Reason examples: Model
     * change {alex, steve}, color change, weapon change.
     */
    public void requestUpdatePlayer() {
        try {
            // Send player (info) update to the server
            final Player player = gameObject.levelContainer.levelActors.player;
            final RequestIfc request = new Request(RequestIfc.RequestType.SET_PLAYER_INFO,
                    DSObject.DataType.OBJECT, new PlayerInfo(player.getName(), player.body.texName, player.uniqueId, player.body.getPrimaryRGBAColor(), player.getWeapon().getTexName()).toString()
            );
            request.send(this, session);
            synchronized (internRequestMutex) {
                if (requests.size() < MAX_SIZE) {
                    requests.add(request);
                }
            }
        } catch (Exception ex) {
            DSLogger.reportError("Network error(s) occurred!", ex);
            DSLogger.reportError(ex.getMessage(), ex);
        }
    }

    /**
     * Send request to ping the (remote) server
     */
    public void sendPingRequest() {
        try {
            // Send a simple 'goodbye' message with magic bytes prepended
            final RequestIfc request = new Request(RequestIfc.RequestType.PING, DSObject.DataType.VOID, null);
            request.send(this, session);
            synchronized (internRequestMutex) {
                if (requests.size() < MAX_SIZE) {
                    requests.add(request);
                }

                // we have async receive enabled, wait to receive the last message
                if (!isAsyncReceivedEnabled()) {
                    // we don't have async receive enabled write the 'goodbye' response message
                    ResponseIfc response = ResponseIfc.receive(this, session);

                    if (response == Response.INVALID) {
                        connected = ConnectionStatus.NOT_CONNECTED;
                        throw new Exception("Server not responding (ping resulted in timeout)!");
                    }

                    if (response.getReceiverGuid().equals(getGuid())) {
                        DSLogger.reportInfo(String.format("Server response: %s : %s", response.getResponseStatus().toString(), String.valueOf(response.getData())), null);
                        gameObject.intrface.getConsole().write(String.valueOf(response.getData()), response.getResponseStatus() == ResponseIfc.ResponseStatus.OK ? Command.Status.SUCCEEDED : Command.Status.FAILED);
                    }
                }

            }
        } catch (Exception ex) {
            DSLogger.reportError("Network error(s) occurred!", ex);
            DSLogger.reportError(ex.getMessage(), ex);
        }
    }

    /**
     * Once World Info response is received load the world.
     *
     * @param worldInfo world info as Json.
     * @return success indicator (true - success, false - failure)
     */
    public boolean loadWorld(LevelMapInfo worldInfo) {
        boolean success = false;
        int numberOfAttempts = 0;
        Game.DownloadStatus status = Game.DownloadStatus.ERR;

        // Locate all level map files with dat or ndat extension
        File clientDir = new File("./");
        String worldNameEscaped = Pattern.quote(worldInfo.worldname);
        final Pattern pattern = Pattern.compile(worldNameEscaped + "\\.(n)?dat$", Pattern.CASE_INSENSITIVE);
        List<String> datFileList = Arrays.asList(clientDir.list((dir, name) -> pattern.matcher(name.toLowerCase()).find()));
        GapList<String> datFileListCopy = GapList.create(datFileList);
        String mapFileOrNull = datFileListCopy.getFirstOrNull();
        boolean lvlMapIsCorrect = false;
        CRC32C checksum = new CRC32C();

        if (mapFileOrNull != null) {
            File mapFileLevel = new File(mapFileOrNull);

            if (mapFileLevel.exists()) {
                try (FileChannel fileChannel = new FileInputStream(mapFileLevel).getChannel()) {
                    int sizeBytes = (int) Files.size(Path.of(mapFileOrNull));
                    ByteBuffer buffer = ByteBuffer.allocate((int) fileChannel.size());
                    while (fileChannel.read(buffer) > 0) {
                        // Do nothing, just read the file into the buffer
                    }
                    buffer.flip();
                    checksum.update(buffer);

                    lvlMapIsCorrect = worldInfo.sizebytes == sizeBytes && worldInfo.chksum == checksum.getValue();
                } catch (IOException ex) {
                    DSLogger.reportError(ex.getMessage(), ex);
                } catch (Exception ex) {
                    DSLogger.reportError(String.format("Error - level map info is null! %s", ex.getMessage()), ex);
                }
            }
        }

        // If level does not exist or is not correct (checksum value is different)
        if (lvlMapIsCorrect) {
            DSLogger.reportInfo("Level Map OK!", null);
            gameObject.intrface.getConsole().write("Level Map OK!");

            gameObject.levelContainer.loadLevelFromFile(mapFileOrNull);
        } else {
            do {
                status = this.downloadLevel();
            } while (status == Game.DownloadStatus.ERR && ++numberOfAttempts <= MAX_ATTEMPTS);

            if (status == Game.DownloadStatus.WARNING) {
                DSLogger.reportWarning("Connected to empty world - disconnect!", null);
                gameObject.intrface.getConsole().write("Connected to empty world - disconnect!");
                this.disconnectFromServer();
                return false;
            }

            try {
                // Load downloaded level (from fragments)
                gameObject.levelContainer.loadLevelFromBufferNewFormat();
            } catch (UnsupportedEncodingException ex) {
                DSLogger.reportError(ex.getMessage(), ex);
            } catch (Exception ex) {
                DSLogger.reportWarning("Connected to empty world - disconnect!", null);
                gameObject.intrface.getConsole().write("Connected to empty world - disconnect!");
                this.disconnectFromServer();
                gameObject.clearEverything();
                return false;
            }
            // Save world to world name + ndat. For example MyWorld.ndat
            gameObject.levelContainer.saveLevelToFile(worldInfo.worldname + ".ndat");
        }

        if (lvlMapIsCorrect || status == Game.DownloadStatus.OK) {
            try {

                DSLogger.reportInfo(String.format("World '%s.ndat' saved!", worldInfo.worldname), null);
                gameObject.intrface.getConsole().write(String.format("World '%s.ndat' saved!", worldInfo.worldname));
                // Avoid reconnection errors
                // On reconnecting, the player needs to be registered again
                if (!gameObject.levelContainer.levelActors.player.isRegistered()) {
                    registerPlayer();
                }

                success = true;
            } catch (Exception ex) {
                DSLogger.reportWarning("Not able to spawn player. Disconnecting.", null);
                gameObject.intrface.getConsole().write("Not able to spawn player. Disconnecting.", Command.Status.WARNING);
                this.disconnectFromServer();
            }
        } else {
            DSLogger.reportWarning("Not able to load level. Disconnecting.", null);
            gameObject.intrface.getConsole().write("Not able to load level. Disconnecting.", Command.Status.FAILED);
            this.disconnectFromServer();
        }

        return success;
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

    public IList<RequestIfc> getRequests() {
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

    public double getWaitReceiveTime() {
        return waitReceiveTime;
    }

    public boolean isAsyncReceivedEnabled() {
        return asyncReceivedEnabled.get();
    }

    public void setAsyncReceivedForceEnabled(boolean asyncReceivedEnabled) {
        this.asyncReceivedEnabled.compareAndSet(false, asyncReceivedEnabled);
    }

    public double getPing() {
        return ping;
    }

    public static int getTicksPerUpdate() {
        return ticksPerUpdate;
    }

    public static void setTicksPerUpdate(int ticksPerUpdate) {
        Game.ticksPerUpdate = ticksPerUpdate;
    }

}
