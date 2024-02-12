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

import java.util.Arrays;
import java.util.concurrent.FutureTask;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import rs.alexanderstojanovich.evg.audio.AudioFile;
import rs.alexanderstojanovich.evg.critter.Critter;
import rs.alexanderstojanovich.evg.critter.Observer;
import rs.alexanderstojanovich.evg.critter.Player;
import rs.alexanderstojanovich.evg.intrface.Command;
import rs.alexanderstojanovich.evg.level.Editor;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.GlobalColors;
import rs.alexanderstojanovich.evg.util.MathUtils;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Game {

    private static final Configuration cfg = Configuration.getInstance();

    public static final int TPS = 80; // TICKS PER SECOND GENERATED
    public static final double TICK_TIME = 1.0 / (double) TPS;

    public static final float AMOUNT = 4.0f;
    public static final float ANGLE = (float) (Math.PI / 180);

    public static final int FORWARD = 0;
    public static final int BACKWARD = 1;
    public static final int LEFT = 2;
    public static final int RIGHT = 3;

    private static int ups; // current update per second    
    private static int fpsMax = cfg.getFpsCap(); // fps max or fps cap  

    // if this is reach game will close without exception!
    public static final double CRITICAL_TIME = 10.0;

    private final boolean[] keys = new boolean[512];

    private static float lastX = 0.0f;
    private static float lastY = 0.0f;
    private static float xoffset = 0.0f;
    private static float yoffset = 0.0f;
    private static float mouseSensitivity = 1.5f;
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
    public static final String PLAYER_ENTRY = "player/";
    public static final String WORLD_ENTRY = "world/";
    public static final String EFFECTS_ENTRY = "effects/";
    public static final String SOUND_ENTRY = "sound/";
    public static final String CHARACTER_ENTRY = "character/";

    protected static double upsTicks = 0.0;
    protected static double accumulator = 0.0;

    public static enum Mode {
        FREE, SINGLE_PLAYER, MULTIPLAYER, EDITOR
    };
    private static Mode currentMode = Mode.SINGLE_PLAYER;

    protected static boolean actionPerformed = false; // movement for all actors (critters)
    protected static boolean jumpPerformed = false; // jump for player
    protected static boolean causingCollision = false; // collision with solid environment (all critters)    

    protected final GameObject gameObject;

    /**
     * Construct new game view
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

        if ((keys[GLFW.GLFW_KEY_W] || keys[GLFW.GLFW_KEY_UP]) && !causingCollision) {
            if (causingCollision = LevelContainer.hasCollisionWithEnvironment(obs)) {
                obs.moveBackward(AMOUNT);
            } else {
                obs.moveForward(amount);
                changed = true;
            }
        }

        if ((keys[GLFW.GLFW_KEY_S] || keys[GLFW.GLFW_KEY_DOWN]) && !causingCollision) {
            if (causingCollision = LevelContainer.hasCollisionWithEnvironment(obs)) {
                obs.moveForward(AMOUNT);
            } else {
                obs.moveBackward(amount);
                changed = true;
            }
        }

        if (keys[GLFW.GLFW_KEY_A] && !causingCollision) {
            if (causingCollision = LevelContainer.hasCollisionWithEnvironment(obs)) {
                obs.moveRight(AMOUNT);
            } else {
                obs.moveLeft(amount);
                changed = true;
            }
        }

        if (keys[GLFW.GLFW_KEY_D] && !causingCollision) {
            if (causingCollision = LevelContainer.hasCollisionWithEnvironment(obs)) {
                obs.moveLeft(AMOUNT);
            } else {
                obs.moveRight(amount);
                changed = true;
            }
        }

        if (keys[GLFW.GLFW_KEY_PAGE_UP] && !causingCollision) {
            if (causingCollision = LevelContainer.hasCollisionWithEnvironment(obs)) {
                obs.descend(AMOUNT);
            } else {
                obs.ascend(amount);
                changed = true;
            }
        }

        if (keys[GLFW.GLFW_KEY_PAGE_DOWN] && !causingCollision) {
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
    public boolean playerDo(LevelContainer lc, float amountXZ, float amountY, float amountYNeg, float deltaTime) {
        boolean changed = false;
        causingCollision = false;
        final Player player = lc.levelActors.player;

        if (lc.isCameraInFluid()) {
            jumpPerformed = false;
        }

        if (keys[GLFW.GLFW_KEY_SPACE] && !causingCollision && !jumpPerformed) {
            player.movePredictorYUp(amountY);
            if (causingCollision = LevelContainer.hasCollisionWithEnvironment((Critter) player)) {
                player.movePredictorYDown(amountY);
            } else {
                jumpPerformed |= lc.jump(player, amountY, deltaTime);
                changed = true;
            }
        }

        if ((keys[GLFW.GLFW_KEY_LEFT_CONTROL] || keys[GLFW.GLFW_KEY_RIGHT_CONTROL]) && !causingCollision) {
            player.movePredictorYDown(amountYNeg);
            if (causingCollision = LevelContainer.hasCollisionWithEnvironment((Critter) player)) {
                player.movePredictorYUp(amountYNeg);
            } else {
                player.sinkY(amountYNeg);
                changed = true;
            }
        }

        if ((keys[GLFW.GLFW_KEY_W] || keys[GLFW.GLFW_KEY_UP]) && !causingCollision) {
            player.movePredictorXZForward(amountXZ);
            if (causingCollision = LevelContainer.hasCollisionWithEnvironment((Critter) player)) {
                player.movePredictorXZBackward(amountXZ);
            } else {
                player.moveXZForward(amountXZ);
                changed = true;
            }
        }

        if ((keys[GLFW.GLFW_KEY_S] || keys[GLFW.GLFW_KEY_DOWN]) && !causingCollision) {
            player.movePredictorXZBackward(amountXZ);
            if (causingCollision = LevelContainer.hasCollisionWithEnvironment((Critter) player)) {
                player.movePredictorXZForward(amountXZ);
            } else {
                player.moveXZBackward(amountXZ);
                changed = true;
            }
        }

        if (keys[GLFW.GLFW_KEY_A] && !causingCollision) {
            player.movePredictorXZLeft(amountXZ);
            if (causingCollision = LevelContainer.hasCollisionWithEnvironment((Critter) player)) {
                player.movePredictorXZRight(amountXZ);
            } else {
                player.moveXZLeft(amountXZ);
                changed = true;
            }
        }

        if (keys[GLFW.GLFW_KEY_D] && !causingCollision) {
            player.movePredictorXZRight(amountXZ);
            if (causingCollision = LevelContainer.hasCollisionWithEnvironment((Critter) player)) {
                player.movePredictorXZLeft(amountXZ);
            } else {
                player.moveXZRight(amountXZ);
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

//        if (keys[GLFW.GLFW_KEY_1]) {
//            gameObject.getLevelContainer().levelActors.getPlayer().switchWeapon(1);
//            changed = true;
//        }
//        if (keys[GLFW.GLFW_KEY_2]) {
//            gameObject.getLevelContainer().levelActors.getPlayer().switchWeapon(2);
//            changed = true;
//        }
//        if (keys[GLFW.GLFW_KEY_3]) {
//            gameObject.getLevelContainer().levelActors.getPlayer().switchWeapon(3);
//            changed = true;
//        }
//        if (keys[GLFW.GLFW_KEY_4]) {
//            gameObject.getLevelContainer().levelActors.getPlayer().switchWeapon(4);
//            changed = true;
//        }
//        if (keys[GLFW.GLFW_KEY_5]) {
//            gameObject.getLevelContainer().levelActors.getPlayer().switchWeapon(5);
//            changed = true;
//        }
//        if (keys[GLFW.GLFW_KEY_6]) {
//            gameObject.getLevelContainer().levelActors.getPlayer().switchWeapon(6);
//            changed = true;
//        }
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
        GLFWErrorCallback.createPrint(System.err).set();

        defaultKeyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
                    gameObject.intrface.setShowHelp(false);
                    gameObject.intrface.getHelpText().setEnabled(false);
                    gameObject.intrface.getCollText().setEnabled(true);
                    gameObject.intrface.getMainMenu().open();
                } else if (key == GLFW.GLFW_KEY_GRAVE_ACCENT && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
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
                    gameObject.levelContainer.chunks.printInfo();
                } else if (key == GLFW.GLFW_KEY_F5 && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
                    LevelContainer.printPositionMaps();
                } else if (key == GLFW.GLFW_KEY_F6 && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
                    gameObject.levelContainer.printQueues();
                } else if (key == GLFW.GLFW_KEY_F12 && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
                    FutureTask<Object> task = new FutureTask<>(Command.getCommand(Command.Target.SCREENSHOT));
                    GameRenderer.TASK_QUEUE.add(task);
                } else if (key == GLFW.GLFW_KEY_P && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    cycleCrosshairColor();
                } else if (key == GLFW.GLFW_KEY_M && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    Editor.cycleBlockColor();
                } else if (key == GLFW.GLFW_KEY_LEFT_BRACKET && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    Editor.selectPrevTexture(gameObject);
                } else if (key == GLFW.GLFW_KEY_RIGHT_BRACKET && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    Editor.selectNextTexture(gameObject);
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

    /**
     * Starts the main (update) loop
     */
    public void go() {
        Game.setCurrentMode(Mode.FREE);
        ups = 0;

        accumulator = cfg.getGameTicks();
        double lastTime = GLFW.glfwGetTime();
        double currTime;
        double deltaTime;

        int index = 0; // track index

        // first time we got nothing
        actionPerformed = false;
        causingCollision = false;

        GLFW.glfwWaitEvents(); // prevent not responding in title from Windows

        while (!gameObject.WINDOW.shouldClose()) {
            currTime = GLFW.glfwGetTime();
            deltaTime = currTime - lastTime;
            // hunger time
            accumulator += deltaTime * Game.TPS;
            upsTicks += MathUtils.lerp(deltaTime * Game.TPS, deltaTime * ups, 0.02);
            lastTime = currTime;

            // Detecting critical status
            if (ups == 0 && deltaTime > CRITICAL_TIME) {
                DSLogger.reportFatalError("Game status critical!", null);
                gameObject.WINDOW.close();
                break;
            }

            if (!gameObject.musicPlayer.isPlaying()) {
                gameObject.musicPlayer.play(AudioFile.TRACKS[index++], false);
                if (index == AudioFile.TRACKS.length) {
                    index = 0;
                }
            }

            if (upsTicks >= 1.0) {
                // update with delta time like gravity
                gameObject.update((float) (Game.upsTicks * TICK_TIME));
                // call utility functions (chunking, optimizing etc.)
                gameObject.util();
            }

            while (upsTicks >= 1.0) {
                GLFW.glfwPollEvents();
                actionPerformed = false;
                switch (currentMode) {
                    case FREE:
                        // nobody has control
                        break;
                    case EDITOR:
                        // observer has control
                        synchronized (GameObject.UPDATE_RENDER_MUTEX) {
                            actionPerformed |= observerDo(gameObject.levelContainer, AMOUNT * (float) TICK_TIME);
                            actionPerformed |= editorDo(gameObject.levelContainer);
                        }

                        if (actionPerformed) {
                            LevelContainer.updateCameraInFluid(gameObject.levelContainer);
                        }
                        break;
                    case SINGLE_PLAYER:
                    case MULTIPLAYER:
                        // player has control
                        synchronized (GameObject.UPDATE_RENDER_MUTEX) {
                            actionPerformed |= playerDo(gameObject.levelContainer, 1.1f * AMOUNT * (float) TICK_TIME, 2500.0f * Game.AMOUNT * (float) TICK_TIME, 1.1f * AMOUNT * (float) TICK_TIME, (float) TICK_TIME);
                        }

                        if (actionPerformed) {
                            LevelContainer.updateCameraInFluid(gameObject.levelContainer);
                        }

                        if (keys[GLFW.GLFW_KEY_SPACE] // prevent repeat jumping (while not in water)
                                && !gameObject.levelContainer.levelActors.player.isUnderGravity() && actionPerformed) {
                            jumpPerformed = false;
                        }
                        break;
                }

                // display collision text
                gameObject.assertCheckCollision(causingCollision);

                // determine visible chunks (can be altered with player position)
                gameObject.determineVisibleChunks();

                ups++;
                upsTicks--;
            }

        }
        // stops the music        
        gameObject.getMusicPlayer().stop();

        DSLogger.reportDebug("Main loop ended.", null);
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
        return cfg;
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

    public static void setAccumulator(double accumulator) {
        Game.accumulator = accumulator;
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

    public static double getUpsTicks() {
        return upsTicks;
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

    public static double getAccumulator() {
        return accumulator;
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

}
