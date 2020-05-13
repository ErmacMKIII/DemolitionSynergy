/*
 * Copyright (C) 2019 Coa
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

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.opengl.GL;
import rs.alexanderstojanovich.evg.audio.AudioFile;
import rs.alexanderstojanovich.evg.core.MasterRenderer;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.critter.Observer;
import rs.alexanderstojanovich.evg.level.Editor;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Coa
 */
public class Game {

    public static final int TPS = 80; // TICKS PER SECOND GENERATED

    public static final float AMOUNT = 0.05f;
    public static final float ANGLE = (float) (Math.PI / 180);

    public static final int FORWARD = 0;
    public static final int BACKWARD = 1;
    public static final int LEFT = 2;
    public static final int RIGHT = 3;

    public static final float EPSILON = 0.0001f;

    private static int ups; // current update per second    
    private static int fpsMax; // fps max or fps cap     
    private static int updPasses = 0;
    public static final int UPD_MAX_PASSES = 10;
    // if this is reach game will close without exception!
    public static final double CRITICAL_TIME = 5.0;

    private final GameObject gameObject;

    private final boolean[] keys = new boolean[512];

    private static float lastX = 0.0f;
    private static float lastY = 0.0f;
    private static float xoffset = 0.0f;
    private static float yoffset = 0.0f;
    private static float mouseSensitivity = 3.0f;
    private boolean moveMouse = false;

    private int crosshairColorNum = 0;
    private int blockColorNum = 0;

    private final boolean[] mouseButtons = new boolean[8];

    private static GLFWKeyCallback defaultKeyCallback;
    private static GLFWCursorPosCallback defaultCursorCallback;
    private static GLFWMouseButtonCallback defaultMouseButtonCallback;

    public static final String ROOT = "/";
    public static final String CURR = "./";
    public static final String RESOURCES_DIR = "/rs/alexanderstojanovich/evg/resources/";

    public static final String DATA_ZIP = "dsynergy.zip";

    public static final String SCREENSHOTS = "screenshots";

    public static final String INTRFACE_ENTRY = "intrface/";
    public static final String PLAYER_ENTRY = "player/";
    public static final String WORLD_ENTRY = "world/";
    public static final String EFFECTS_ENTRY = "effects/";
    public static final String SOUND_ENTRY = "sound/";

    private static boolean waterEffects = true;

    private static double upsTicks = 0.0;

    public static enum Mode {
        FREE, SINGLE_PLAYER, MULTIPLAYER, EDITOR
    };
    private static Mode currentMode = Mode.FREE;

    public Game(GameObject gameObject, Configuration config) {
        this.gameObject = gameObject;
        lastX = config.getWidth() / 2.0f;
        lastY = config.getHeight() / 2.0f;
        Game.fpsMax = config.getFpsCap();
        if (config.isFullscreen()) {
            gameObject.getMyWindow().fullscreen();
        } else {
            gameObject.getMyWindow().windowed();
        }
        if (config.isVsync()) {
            gameObject.getMyWindow().enableVSync();
        } else {
            gameObject.getMyWindow().disableVSync();
        }
        gameObject.getMyWindow().centerTheWindow();
        waterEffects = config.isWaterEffects();
        Arrays.fill(keys, false);
        initCallbacks();
    }

    private void observerDo() {
        if (keys[GLFW.GLFW_KEY_W] || keys[GLFW.GLFW_KEY_UP]) {
            Observer obs = gameObject.getLevelContainer().getLevelActors().getPlayer();
            obs.movePredictorForward(AMOUNT);
            if (gameObject.hasCollisionWithCritter(obs)) {
                obs.movePredictorBackward(AMOUNT);
                gameObject.setAssertCollision(true);
            } else {
                obs.moveForward(AMOUNT);
                gameObject.setAssertCollision(false);
            }
        }
        if (keys[GLFW.GLFW_KEY_S] || keys[GLFW.GLFW_KEY_DOWN]) {
            Observer obs = gameObject.getLevelContainer().getLevelActors().getPlayer();
            obs.movePredictorBackward(AMOUNT);
            if (gameObject.hasCollisionWithCritter(obs)) {
                obs.movePredictorForward(AMOUNT);
                gameObject.setAssertCollision(true);
            } else {
                obs.moveBackward(AMOUNT);
                gameObject.setAssertCollision(false);
            }

        }
        if (keys[GLFW.GLFW_KEY_A]) {
            Observer obs = gameObject.getLevelContainer().getLevelActors().getPlayer();
            obs.movePredictorLeft(AMOUNT);
            if (gameObject.hasCollisionWithCritter(obs)) {
                obs.movePredictorRight(AMOUNT);
                gameObject.setAssertCollision(true);
            } else {
                obs.moveLeft(AMOUNT);
                gameObject.setAssertCollision(false);
            }
        }
        if (keys[GLFW.GLFW_KEY_D]) {
            Observer obs = gameObject.getLevelContainer().getLevelActors().getPlayer();
            obs.movePredictorRight(AMOUNT);
            if (gameObject.hasCollisionWithCritter(obs)) {
                obs.movePredictorLeft(AMOUNT);
                gameObject.setAssertCollision(true);
            } else {
                obs.moveRight(AMOUNT);
                gameObject.setAssertCollision(false);
            }
        }
        if (keys[GLFW.GLFW_KEY_LEFT]) {
            gameObject.getLevelContainer().getLevelActors().getPlayer().turnLeft(ANGLE);
        }
        if (keys[GLFW.GLFW_KEY_RIGHT]) {
            gameObject.getLevelContainer().getLevelActors().getPlayer().turnRight(ANGLE);
        }
        if (moveMouse) {
            gameObject.getLevelContainer().getLevelActors().getPlayer().lookAt(mouseSensitivity, xoffset, yoffset);
            moveMouse = false;
        }
    }

    private void editorDo() {
        if (keys[GLFW.GLFW_KEY_N]) {
            Editor.selectNew(gameObject);
        }
        //----------------------------------------------------------------------
        if (mouseButtons[GLFW.GLFW_MOUSE_BUTTON_LEFT] && !keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectCurrSolid(gameObject);
        }

        if (mouseButtons[GLFW.GLFW_MOUSE_BUTTON_LEFT] && keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectCurrFluid(gameObject);
        }
        //----------------------------------------------------------------------
        if (keys[GLFW.GLFW_KEY_1] && !keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentSolid(gameObject, Block.LEFT);
        }
        if (keys[GLFW.GLFW_KEY_2] && !keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentSolid(gameObject, Block.RIGHT);
        }
        if (keys[GLFW.GLFW_KEY_3] && !keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentSolid(gameObject, Block.BOTTOM);
        }
        if (keys[GLFW.GLFW_KEY_4] && !keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentSolid(gameObject, Block.TOP);
        }
        if (keys[GLFW.GLFW_KEY_5] && !keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentSolid(gameObject, Block.BACK);
        }
        if (keys[GLFW.GLFW_KEY_6] && !keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentSolid(gameObject, Block.FRONT);
        }
        //----------------------------------------------------------------------
        if (keys[GLFW.GLFW_KEY_1] && keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentFluid(gameObject, Block.LEFT);
        }
        if (keys[GLFW.GLFW_KEY_2] && keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentFluid(gameObject, Block.RIGHT);
        }
        if (keys[GLFW.GLFW_KEY_3] && keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentFluid(gameObject, Block.BOTTOM);
        }
        if (keys[GLFW.GLFW_KEY_4] && keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentFluid(gameObject, Block.TOP);
        }
        if (keys[GLFW.GLFW_KEY_5] && keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentFluid(gameObject, Block.BACK);
        }
        if (keys[GLFW.GLFW_KEY_6] && keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentFluid(gameObject, Block.FRONT);
        }
        //----------------------------------------------------------------------
        if (keys[GLFW.GLFW_KEY_0] || keys[GLFW.GLFW_KEY_F]) {
            Editor.deselect();
        }
        if (mouseButtons[GLFW.GLFW_MOUSE_BUTTON_RIGHT]) {
            Editor.add(gameObject);
        }
        if (keys[GLFW.GLFW_KEY_R]) {
            Editor.remove(gameObject);
        }
    }

    public void playerDo() {
        if (mouseButtons[GLFW.GLFW_MOUSE_BUTTON_LEFT]) {

        }

        if (keys[GLFW.GLFW_KEY_1]) {
            gameObject.getLevelContainer().getLevelActors().getPlayer().switchWeapon(1);
        }
        if (keys[GLFW.GLFW_KEY_2]) {
            gameObject.getLevelContainer().getLevelActors().getPlayer().switchWeapon(2);
        }
        if (keys[GLFW.GLFW_KEY_3]) {
            gameObject.getLevelContainer().getLevelActors().getPlayer().switchWeapon(3);
        }
        if (keys[GLFW.GLFW_KEY_4]) {
            gameObject.getLevelContainer().getLevelActors().getPlayer().switchWeapon(4);
        }
        if (keys[GLFW.GLFW_KEY_5]) {
            gameObject.getLevelContainer().getLevelActors().getPlayer().switchWeapon(5);
        }
        if (keys[GLFW.GLFW_KEY_6]) {
            gameObject.getLevelContainer().getLevelActors().getPlayer().switchWeapon(6);
        }

        if (keys[GLFW.GLFW_KEY_R]) {

        }
    }

    private void setCrosshairColor(float red, float green, float blue) {
        gameObject.getIntrface().getCrosshair().getColor().x = red;
        gameObject.getIntrface().getCrosshair().getColor().y = green;
        gameObject.getIntrface().getCrosshair().getColor().z = blue;
    }

    private void setNewBlockColor(float red, float green, float blue) {
        if (Editor.getSelectedNew() != null) {
            Editor.getSelectedNew().getPrimaryColor().x = red;
            Editor.getSelectedNew().getPrimaryColor().y = green;
            Editor.getSelectedNew().getPrimaryColor().z = blue;
        }
    }

    private void cycleCrosshairColor() {
        switch (crosshairColorNum) {
            case 0:
                setCrosshairColor(1.0f, 0.0f, 0.0f); // RED                
                break;
            case 1:
                setCrosshairColor(0.0f, 1.0f, 0.0f); // GREEN
                break;
            case 2:
                setCrosshairColor(0.0f, 0.0f, 1.0f); // BLUE
                break;
            case 3:
                setCrosshairColor(0.0f, 1.0f, 1.0f); // CYAN
                break;
            case 4:
                setCrosshairColor(1.0f, 0.0f, 1.0f); // MAGENTA
                break;
            case 5:
                setCrosshairColor(1.0f, 1.0f, 0.0f); // YELLOW
                break;
            case 6:
                setCrosshairColor(1.0f, 1.0f, 1.0f); // WHITE
                break;
        }
        if (crosshairColorNum < 6) {
            crosshairColorNum++;
        } else {
            crosshairColorNum = 0;
        }
    }

    private void cycleBlockColor() {
        if (Editor.getSelectedNew() != null) {
            switch (blockColorNum) {
                case 0:
                    setNewBlockColor(1.0f, 0.0f, 0.0f); // RED                
                    break;
                case 1:
                    setNewBlockColor(0.0f, 1.0f, 0.0f); // GREEN
                    break;
                case 2:
                    setNewBlockColor(0.0f, 0.0f, 1.0f); // BLUE
                    break;
                case 3:
                    setNewBlockColor(0.0f, 1.0f, 1.0f); // CYAN
                    break;
                case 4:
                    setNewBlockColor(1.0f, 0.0f, 1.0f); // MAGENTA
                    break;
                case 5:
                    setNewBlockColor(1.0f, 1.0f, 0.0f); // YELLOW
                    break;
                case 6:
                    setNewBlockColor(1.0f, 1.0f, 1.0f); // WHITE
                    break;
            }
            if (blockColorNum < 6) {
                blockColorNum++;
            } else {
                blockColorNum = 0;
            }
        }
    }

    private void initCallbacks() {
        GLFWErrorCallback.createPrint(System.err).set();

        defaultKeyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
                    gameObject.getIntrface().setShowHelp(false);
                    gameObject.getIntrface().getHelpText().setEnabled(false);
                    gameObject.getIntrface().getCollText().setEnabled(true);
                    gameObject.getIntrface().getMainMenu().open();
                } else if (key == GLFW.GLFW_KEY_GRAVE_ACCENT && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
                    gameObject.getIntrface().getConsole().open();
                } else if (key == GLFW.GLFW_KEY_F1 && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
                    gameObject.getIntrface().toggleShowHelp();
                } else if (key == GLFW.GLFW_KEY_F2 && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
                    gameObject.getIntrface().getSaveDialog().open();
                } else if (key == GLFW.GLFW_KEY_F3 && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
                    gameObject.getIntrface().getLoadDialog().open();
                } else if (key == GLFW.GLFW_KEY_F4 && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
                    gameObject.printInfo();
                } else if (key == GLFW.GLFW_KEY_F5 && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
//                    LevelContainer.printPositionSets();
                } else if (key == GLFW.GLFW_KEY_F12 && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
                    File screenDir = new File(SCREENSHOTS);
                    if (!screenDir.isDirectory() && !screenDir.exists()) {
                        screenDir.mkdir();
                    }
                    LocalDateTime now = LocalDateTime.now();
                    File screenshot = new File(SCREENSHOTS + File.separator
                            + "dsynergy-" + now.getYear()
                            + "-" + now.getMonthValue()
                            + "-" + now.getDayOfMonth()
                            + "_" + now.getHour()
                            + "-" + now.getMinute()
                            + "-" + now.getSecond()
                            + "-" + now.getNano() / 1E6 // one million
                            + ".png");
                    if (screenshot.exists()) {
                        screenshot.delete();
                    }
                    synchronized (Main.OBJ_MUTEX) {
                        gameObject.getMyWindow().loadContext();
                        GL.setCapabilities(MasterRenderer.getGlCaps());
                        try {
                            ImageIO.write(gameObject.getMyWindow().getScreen(), "PNG", screenshot);
                        } catch (IOException ex) {
                            DSLogger.reportError(ex.getMessage(), ex);
                        }
                        GL.setCapabilities(null);
                        Window.unloadContext();
                    }
                    gameObject.getIntrface().getScreenText().setEnabled(true);
                    gameObject.getIntrface().getScreenText().setContent("Screen saved to " + screenshot.getAbsolutePath());
                } else if (key == GLFW.GLFW_KEY_P && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    cycleCrosshairColor();
                } else if (key == GLFW.GLFW_KEY_M && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    cycleBlockColor();
                } else if (key == GLFW.GLFW_KEY_LEFT_BRACKET && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    Editor.selectPrevTexture(gameObject);
                } else if (key == GLFW.GLFW_KEY_RIGHT_BRACKET && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    Editor.selectNextTexture(gameObject);
                } else {
                    if (action == GLFW.GLFW_PRESS) {
                        keys[key] = true;
                    } else if (action == GLFW.GLFW_RELEASE) {
                        keys[key] = false;
                    }
                }
            }
        };
        GLFW.glfwSetKeyCallback(gameObject.getMyWindow().getWindowID(), defaultKeyCallback);

        GLFW.glfwSetInputMode(gameObject.getMyWindow().getWindowID(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        GLFW.glfwSetCursorPos(gameObject.getMyWindow().getWindowID(), gameObject.getMyWindow().getWidth() / 2.0, gameObject.getMyWindow().getHeight() / 2.0);
        defaultCursorCallback = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                xoffset = ((float) xpos - lastX) / gameObject.getMyWindow().getWidth();
                yoffset = (lastY - (float) ypos) / gameObject.getMyWindow().getHeight();

                if (xoffset != 0.0f || yoffset != 0.0f) {
                    moveMouse = true;
                }

                lastX = (float) xpos;
                lastY = (float) ypos;
            }
        };
        GLFW.glfwSetCursorPosCallback(gameObject.getMyWindow().getWindowID(), defaultCursorCallback);

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
        GLFW.glfwSetMouseButtonCallback(gameObject.getMyWindow().getWindowID(), defaultMouseButtonCallback);
    }

    public void go() {
        // start the music
        AudioFile audioFile = AudioFile.AMBIENT;
        gameObject.getMusicPlayer().play(audioFile, true);

        double timer0 = GLFW.glfwGetTime();

        ups = 0;

        double lastTime = GLFW.glfwGetTime();
        double currTime;
        double diff;

        while (!gameObject.getMyWindow().shouldClose()) {
            currTime = GLFW.glfwGetTime();
            diff = currTime - lastTime;
            upsTicks += diff * Game.TPS;
            lastTime = currTime;

            // Detecting critical status
            if (ups == 0 && diff > CRITICAL_TIME) {
                DSLogger.reportFatalError("Game status critical!", null);
                gameObject.getMyWindow().close();
                break;
            }

            if (upsTicks > 1.0) {
                while (upsTicks >= 1.0 && updPasses < UPD_MAX_PASSES) {
                    GLFW.glfwPollEvents();
                    if (Renderer.getRenPasses() == 0) {
                        float deltaTime = (float) (upsTicks / TPS);
                        gameObject.update(deltaTime);
                    }
                    if (currentMode == Mode.SINGLE_PLAYER) {
                        playerDo();
                    } else if (currentMode == Mode.EDITOR) {
                        gameObject.getLevelContainer().getLevelActors().getPlayer().setCurrWeapon(null);
                        editorDo();
                    }
                    observerDo();
                    ups++;
                    upsTicks--;
                    updPasses++;
                }
                updPasses = 0;
            }

            // update label which shows fps every second
            if (GLFW.glfwGetTime() > timer0 + 1.0) {
                gameObject.getIntrface().getUpdText().setContent("ups: " + Game.getUps());
                ups = 0;
                timer0 += 1.0;
            }

        }
        // stops the music
        gameObject.getMusicPlayer().stop();
    }

    public Configuration makeConfig() {
        Configuration cfg = new Configuration();
        cfg.setFpsCap(fpsMax);
        cfg.setWidth(gameObject.getMyWindow().getWidth());
        cfg.setHeight(gameObject.getMyWindow().getHeight());
        cfg.setFullscreen(gameObject.getMyWindow().isFullscreen());
        cfg.setVsync(gameObject.getMyWindow().isVsync());
        cfg.setWaterEffects(waterEffects);
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

    public static int getUps() {
        return ups;
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

    public static boolean isWaterEffects() {
        return waterEffects;
    }

    public static void setWaterEffects(boolean waterEffects) {
        Game.waterEffects = waterEffects;
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

    public static int getUpdPasses() {
        return updPasses;
    }

}
