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
import rs.alexanderstojanovich.evg.audio.AudioPlayer;
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

    public static final String TITLE = "Demolition Synergy - v18 STONEWALL";

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

    private final Window myWindow;
    private final Renderer renderer;

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

    private final Object objMutex = new Object(); // aka MUTEX and SYNC for "main" and "Renderer"

    private static boolean waterEffects = true;

    private static double upsTicks = 0.0;

    private final AudioPlayer musicPlayer = new AudioPlayer();
    private final AudioPlayer soundFXPlayer = new AudioPlayer();

    public static enum Mode {
        FREE, SINGLE_PLAYER, MULTIPLAYER, EDITOR
    };
    private static Mode currentMode = Mode.FREE;

    public Game(Configuration config) {
        lastX = config.getWidth() / 2.0f;
        lastY = config.getHeight() / 2.0f;
        Game.fpsMax = config.getFpsCap();
        myWindow = new Window(config.getWidth(), config.getHeight(), TITLE);
        if (config.isFullscreen()) {
            myWindow.fullscreen();
        } else {
            myWindow.windowed();
        }
        if (config.isVsync()) {
            myWindow.enableVSync();
        } else {
            myWindow.disableVSync();
        }
        myWindow.centerTheWindow();
        waterEffects = config.isWaterEffects();
        renderer = new Renderer(myWindow, objMutex, musicPlayer, soundFXPlayer);
        Arrays.fill(keys, false);
        initCallbacks();
        musicPlayer.setGain(config.getMusicVolume());
        soundFXPlayer.setGain(config.getSoundFXVolume());
    }

    private void observerDo() {
        if (keys[GLFW.GLFW_KEY_W] || keys[GLFW.GLFW_KEY_UP]) {
            Observer obs = renderer.getLevelContainer().getLevelActors().getPlayer();
            obs.movePredictorForward(AMOUNT);
            if (renderer.getLevelContainer().hasCollisionWithCritter(obs)) {
                obs.movePredictorBackward(AMOUNT);
                renderer.setAssertCollision(true);
            } else {
                obs.moveForward(AMOUNT);
                renderer.setAssertCollision(false);
            }
        }
        if (keys[GLFW.GLFW_KEY_S] || keys[GLFW.GLFW_KEY_DOWN]) {
            Observer obs = renderer.getLevelContainer().getLevelActors().getPlayer();
            obs.movePredictorBackward(AMOUNT);
            if (renderer.getLevelContainer().hasCollisionWithCritter(obs)) {
                obs.movePredictorForward(AMOUNT);
                renderer.setAssertCollision(true);
            } else {
                obs.moveBackward(AMOUNT);
                renderer.setAssertCollision(false);
            }

        }
        if (keys[GLFW.GLFW_KEY_A]) {
            Observer obs = renderer.getLevelContainer().getLevelActors().getPlayer();
            obs.movePredictorLeft(AMOUNT);
            if (renderer.getLevelContainer().hasCollisionWithCritter(obs)) {
                obs.movePredictorRight(AMOUNT);
                renderer.setAssertCollision(true);
            } else {
                obs.moveLeft(AMOUNT);
                renderer.setAssertCollision(false);
            }
        }
        if (keys[GLFW.GLFW_KEY_D]) {
            Observer obs = renderer.getLevelContainer().getLevelActors().getPlayer();
            obs.movePredictorRight(AMOUNT);
            if (renderer.getLevelContainer().hasCollisionWithCritter(obs)) {
                obs.movePredictorLeft(AMOUNT);
                renderer.setAssertCollision(true);
            } else {
                obs.moveRight(AMOUNT);
                renderer.setAssertCollision(false);
            }
        }
        if (keys[GLFW.GLFW_KEY_LEFT]) {
            renderer.getLevelContainer().getLevelActors().getPlayer().turnLeft(ANGLE);
        }
        if (keys[GLFW.GLFW_KEY_RIGHT]) {
            renderer.getLevelContainer().getLevelActors().getPlayer().turnRight(ANGLE);
        }
        if (moveMouse) {
            renderer.getLevelContainer().getLevelActors().getPlayer().lookAt(mouseSensitivity, xoffset, yoffset);
            moveMouse = false;
        }
    }

    private void editorDo() {
        if (keys[GLFW.GLFW_KEY_N]) {
            Editor.selectNew(renderer.getLevelContainer());
        }
        //----------------------------------------------------------------------
        if (mouseButtons[GLFW.GLFW_MOUSE_BUTTON_LEFT] && !keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectCurrSolid(renderer.getLevelContainer());
        }

        if (mouseButtons[GLFW.GLFW_MOUSE_BUTTON_LEFT] && keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectCurrFluid(renderer.getLevelContainer());
        }
        //----------------------------------------------------------------------
        if (keys[GLFW.GLFW_KEY_1] && !keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentSolid(renderer.getLevelContainer(), Block.LEFT);
        }
        if (keys[GLFW.GLFW_KEY_2] && !keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentSolid(renderer.getLevelContainer(), Block.RIGHT);
        }
        if (keys[GLFW.GLFW_KEY_3] && !keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentSolid(renderer.getLevelContainer(), Block.BOTTOM);
        }
        if (keys[GLFW.GLFW_KEY_4] && !keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentSolid(renderer.getLevelContainer(), Block.TOP);
        }
        if (keys[GLFW.GLFW_KEY_5] && !keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentSolid(renderer.getLevelContainer(), Block.BACK);
        }
        if (keys[GLFW.GLFW_KEY_6] && !keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentSolid(renderer.getLevelContainer(), Block.FRONT);
        }
        //----------------------------------------------------------------------
        if (keys[GLFW.GLFW_KEY_1] && keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentFluid(renderer.getLevelContainer(), Block.LEFT);
        }
        if (keys[GLFW.GLFW_KEY_2] && keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentFluid(renderer.getLevelContainer(), Block.RIGHT);
        }
        if (keys[GLFW.GLFW_KEY_3] && keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentFluid(renderer.getLevelContainer(), Block.BOTTOM);
        }
        if (keys[GLFW.GLFW_KEY_4] && keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentFluid(renderer.getLevelContainer(), Block.TOP);
        }
        if (keys[GLFW.GLFW_KEY_5] && keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentFluid(renderer.getLevelContainer(), Block.BACK);
        }
        if (keys[GLFW.GLFW_KEY_6] && keys[GLFW.GLFW_KEY_LEFT_SHIFT]) {
            Editor.selectAdjacentFluid(renderer.getLevelContainer(), Block.FRONT);
        }
        //----------------------------------------------------------------------
        if (keys[GLFW.GLFW_KEY_0] || keys[GLFW.GLFW_KEY_F]) {
            Editor.deselect();
        }
        if (mouseButtons[GLFW.GLFW_MOUSE_BUTTON_RIGHT]) {
            Editor.add(renderer.getLevelContainer());
        }
        if (keys[GLFW.GLFW_KEY_R]) {
            Editor.remove(renderer.getLevelContainer());
        }
    }

    public void playerDo() {
        if (mouseButtons[GLFW.GLFW_MOUSE_BUTTON_LEFT]) {

        }

        if (keys[GLFW.GLFW_KEY_1]) {
            renderer.getLevelContainer().getLevelActors().getPlayer().switchWeapon(1);
        }
        if (keys[GLFW.GLFW_KEY_2]) {
            renderer.getLevelContainer().getLevelActors().getPlayer().switchWeapon(2);
        }
        if (keys[GLFW.GLFW_KEY_3]) {
            renderer.getLevelContainer().getLevelActors().getPlayer().switchWeapon(3);
        }
        if (keys[GLFW.GLFW_KEY_4]) {
            renderer.getLevelContainer().getLevelActors().getPlayer().switchWeapon(4);
        }
        if (keys[GLFW.GLFW_KEY_5]) {
            renderer.getLevelContainer().getLevelActors().getPlayer().switchWeapon(5);
        }
        if (keys[GLFW.GLFW_KEY_6]) {
            renderer.getLevelContainer().getLevelActors().getPlayer().switchWeapon(6);
        }

        if (keys[GLFW.GLFW_KEY_R]) {

        }
    }

    private void setCrosshairColor(float red, float green, float blue) {
        renderer.getIntrface().getCrosshair().getColor().x = red;
        renderer.getIntrface().getCrosshair().getColor().y = green;
        renderer.getIntrface().getCrosshair().getColor().z = blue;
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
                    renderer.getIntrface().setShowHelp(false);
                    renderer.getIntrface().getHelpText().setEnabled(false);
                    renderer.getIntrface().getCollText().setEnabled(true);
                    renderer.getIntrface().getMainMenu().open();
                } else if (key == GLFW.GLFW_KEY_GRAVE_ACCENT && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
                    renderer.getIntrface().getConsole().open();
                } else if (key == GLFW.GLFW_KEY_F1 && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
                    renderer.getIntrface().toggleShowHelp();
                } else if (key == GLFW.GLFW_KEY_F2 && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
                    renderer.getIntrface().getSaveDialog().open();
                } else if (key == GLFW.GLFW_KEY_F3 && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
                    renderer.getIntrface().getLoadDialog().open();
                } else if (key == GLFW.GLFW_KEY_F4 && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
                    renderer.getLevelContainer().getSolidChunks().printInfo();
                    renderer.getLevelContainer().getFluidChunks().printInfo();
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
                    synchronized (objMutex) {
                        myWindow.loadContext();
                        GL.setCapabilities(MasterRenderer.getGlCaps());
                        try {
                            ImageIO.write(myWindow.getScreen(), "PNG", screenshot);
                        } catch (IOException ex) {
                            DSLogger.reportError(ex.getMessage(), ex);
                        }
                        GL.setCapabilities(null);
                        Window.unloadContext();
                    }
                    renderer.getIntrface().getScreenText().setEnabled(true);
                    renderer.getIntrface().getScreenText().setContent("Screen saved to " + screenshot.getAbsolutePath());
                } else if (key == GLFW.GLFW_KEY_P && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    cycleCrosshairColor();
                } else if (key == GLFW.GLFW_KEY_M && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    cycleBlockColor();
                } else if (key == GLFW.GLFW_KEY_LEFT_BRACKET && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    Editor.selectPrevTexture(renderer.getLevelContainer());
                } else if (key == GLFW.GLFW_KEY_RIGHT_BRACKET && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    Editor.selectNextTexture(renderer.getLevelContainer());
                } else {
                    if (action == GLFW.GLFW_PRESS) {
                        keys[key] = true;
                    } else if (action == GLFW.GLFW_RELEASE) {
                        keys[key] = false;
                    }
                }
            }
        };
        GLFW.glfwSetKeyCallback(myWindow.getWindowID(), defaultKeyCallback);

        GLFW.glfwSetInputMode(myWindow.getWindowID(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        GLFW.glfwSetCursorPos(myWindow.getWindowID(), myWindow.getWidth() / 2.0, myWindow.getHeight() / 2.0);
        defaultCursorCallback = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                xoffset = ((float) xpos - lastX) / myWindow.getWidth();
                yoffset = (lastY - (float) ypos) / myWindow.getHeight();

                if (xoffset != 0.0f || yoffset != 0.0f) {
                    moveMouse = true;
                }

                lastX = (float) xpos;
                lastY = (float) ypos;
            }
        };
        GLFW.glfwSetCursorPosCallback(myWindow.getWindowID(), defaultCursorCallback);

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
        GLFW.glfwSetMouseButtonCallback(myWindow.getWindowID(), defaultMouseButtonCallback);
    }

    public void go() {
        // start the renderer
        renderer.start();

        // wait for renderer to initialize level renderer, water renderer and interface
        synchronized (objMutex) {
            try {
                objMutex.wait();
            } catch (InterruptedException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
        }

        // start the music
        AudioFile audioFile = AudioFile.AMBIENT;
        musicPlayer.play(audioFile, true);

        double timer0 = GLFW.glfwGetTime();

        ups = 0;

        double lastTime = GLFW.glfwGetTime();
        double currTime;
        double diff;

        while (!myWindow.shouldClose()) {
            currTime = GLFW.glfwGetTime();
            diff = currTime - lastTime;
            upsTicks += -Math.expm1(-diff * Game.TPS);
            lastTime = currTime;

            // Detecting critical status
            if (ups == 0 && diff > CRITICAL_TIME) {
                DSLogger.reportFatalError("Game status critical!", null);
                myWindow.close();
                break;
            }

            if (Renderer.getRenPasses() == 0) {
                while (upsTicks >= 1.0 && updPasses < UPD_MAX_PASSES) {
                    GLFW.glfwPollEvents();
                    float deltaTime = (float) (upsTicks / TPS);
                    renderer.update(deltaTime);
                    if (currentMode == Mode.SINGLE_PLAYER) {
                        playerDo();
                    } else if (currentMode == Mode.EDITOR) {
                        renderer.getLevelContainer().getLevelActors().getPlayer().setCurrWeapon(null);
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
                renderer.getIntrface().getUpdText().setContent("ups: " + Game.getUps());
                ups = 0;
                timer0 += 1.0;
            }

        }

        Thread randDialogThread = renderer.getIntrface().getRandLvlDialog().getDialogThread();

        try {
            if (randDialogThread != null && randDialogThread.isAlive()) {
                randDialogThread.join();
            }
            renderer.join(); // waits for the renderer to finish life         
        } catch (InterruptedException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        }

        synchronized (objMutex) {
            myWindow.loadContext();
            myWindow.destroy();
        }

        musicPlayer.stop();
    }

    public Configuration makeConfig() {
        Configuration cfg = new Configuration();
        cfg.setFpsCap(fpsMax);
        cfg.setWidth(myWindow.getWidth());
        cfg.setHeight(myWindow.getHeight());
        cfg.setFullscreen(myWindow.isFullscreen());
        cfg.setVsync(myWindow.isVsync());
        cfg.setWaterEffects(waterEffects);
        cfg.setMouseSensitivity(mouseSensitivity);
        cfg.setMusicVolume(musicPlayer.getGain());
        cfg.setSoundFXVolume(soundFXPlayer.getGain());
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

    public Object getObjMutex() {
        return objMutex;
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

    public AudioPlayer getMusicPlayer() {
        return musicPlayer;
    }

    public AudioPlayer getSoundFXPlayer() {
        return soundFXPlayer;
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
