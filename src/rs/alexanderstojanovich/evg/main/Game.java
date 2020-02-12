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

import java.util.Arrays;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import rs.alexanderstojanovich.evg.audio.AudioFile;
import rs.alexanderstojanovich.evg.audio.AudioPlayer;
import rs.alexanderstojanovich.evg.audio.MasterAudio;
import rs.alexanderstojanovich.evg.core.Critter;
import rs.alexanderstojanovich.evg.core.Editor;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Coa
 */
public class Game {

    public static final String TITLE = "Demolition Synergy - v12 LATVIA";

    public static final int UPS_CAP = 80;

    public static final float AMOUNT = 0.05f;
    public static final float ANGLE = (float) (Math.PI / 180);

    public static final int FORWARD = 0;
    public static final int BACKWARD = 1;
    public static final int LEFT = 2;
    public static final int RIGHT = 3;

    public static final float EPSILON = 0.0001f;

    private static int upsCap; // updates per second cap 
    private static int ups; // current update per second    
    private static int fpsMax; // fps max or fps cap 

    private final Window myWindow;
    private final Renderer renderer;

    private boolean[] keys = new boolean[1024];

    private float lastX = 0.0f;
    private float lastY = 0.0f;
    private float xoffset = 0.0f;
    private float yoffset = 0.0f;
    private static float mouseSensitivity = 3.0f;
    private boolean moveMouse = false;

    private int crosshairColorNum = 0;
    private int blockColorNum = 0;

    private final boolean[] mouseButtons = new boolean[8];

    private static GLFWKeyCallback defaultKeyCallback;
    private static GLFWCursorPosCallback defaultCursorCallback;

    public static final String RESOURCES_DIR = "/rs/alexanderstojanovich/evg/resources/";

    public static final String DATA_ZIP = "dsynergy.zip";

    public static final String FONTS_ENTRY = "fonts/";
    public static final String INTRFACE_ENTRY = "intrface/";
    public static final String WORLD_ENTRY = "world/";
    public static final String EFFECTS_ENTRY = "effects/";
    public static final String SOUND_ENTRY = "sound/";

    private final Object objMutex = new Object(); // aka MUTEX and SYNC for "main" and "Renderer"

    private static boolean waterEffects = true;

    private static double upsTicks = 0.0;

    private final AudioPlayer audioPlayer = new AudioPlayer();

    public Game(Configuration config) {
        lastX = config.getWidth() / 2.0f;
        lastY = config.getHeight() / 2.0f;
        Game.upsCap = UPS_CAP;
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
        renderer = new Renderer(myWindow, objMutex);
        keys = new boolean[1024];
        initCallbacks();
        MasterAudio.init(); // important part        
    }

    private void observerDo() {
        if (keys[GLFW.GLFW_KEY_W] || keys[GLFW.GLFW_KEY_UP]) {
            Critter obs = renderer.getLevelRenderer().getObserver();
            obs.movePredictorForward(AMOUNT);
            if (renderer.getLevelRenderer().hasCollisionWithCritter(obs)) {
                obs.movePredictorBackward(AMOUNT);
                renderer.setAssertCollision(true);
            } else {
                obs.moveForward(AMOUNT);
                renderer.setAssertCollision(false);
            }
        }
        if (keys[GLFW.GLFW_KEY_S] || keys[GLFW.GLFW_KEY_DOWN]) {
            Critter obs = renderer.getLevelRenderer().getObserver();
            obs.movePredictorBackward(AMOUNT);
            if (renderer.getLevelRenderer().hasCollisionWithCritter(obs)) {
                obs.movePredictorForward(AMOUNT);
                renderer.setAssertCollision(true);
            } else {
                obs.moveBackward(AMOUNT);
                renderer.setAssertCollision(false);
            }

        }
        if (keys[GLFW.GLFW_KEY_A]) {
            Critter obs = renderer.getLevelRenderer().getObserver();
            obs.movePredictorLeft(AMOUNT);
            if (renderer.getLevelRenderer().hasCollisionWithCritter(obs)) {
                obs.movePredictorRight(AMOUNT);
                renderer.setAssertCollision(true);
            } else {
                obs.moveLeft(AMOUNT);
                renderer.setAssertCollision(false);
            }
        }
        if (keys[GLFW.GLFW_KEY_D]) {
            Critter obs = renderer.getLevelRenderer().getObserver();
            obs.movePredictorRight(AMOUNT);
            if (renderer.getLevelRenderer().hasCollisionWithCritter(obs)) {
                obs.movePredictorLeft(AMOUNT);
                renderer.setAssertCollision(true);
            } else {
                obs.moveRight(AMOUNT);
                renderer.setAssertCollision(false);
            }
        }
        if (keys[GLFW.GLFW_KEY_LEFT]) {
            renderer.getLevelRenderer().getObserver().turnLeft(ANGLE);
        }
        if (keys[GLFW.GLFW_KEY_RIGHT]) {
            renderer.getLevelRenderer().getObserver().turnRight(ANGLE);
        }
        if (moveMouse) {
            renderer.getLevelRenderer().getObserver().lookAt(mouseSensitivity, xoffset, yoffset);
            moveMouse = false;
        }
    }

    private void editorDo() {
        if (keys[GLFW.GLFW_KEY_N]) {
            Editor.selectNew(renderer.getLevelRenderer());
        }
        if (mouseButtons[GLFW.GLFW_MOUSE_BUTTON_LEFT]) {
            Editor.selectCurr(renderer.getLevelRenderer());
        }
        if (keys[GLFW.GLFW_KEY_1]) {
            Editor.selectAdjacent(renderer.getLevelRenderer(), Block.LEFT);
        }
        if (keys[GLFW.GLFW_KEY_2]) {
            Editor.selectAdjacent(renderer.getLevelRenderer(), Block.RIGHT);
        }
        if (keys[GLFW.GLFW_KEY_3]) {
            Editor.selectAdjacent(renderer.getLevelRenderer(), Block.BOTTOM);
        }
        if (keys[GLFW.GLFW_KEY_4]) {
            Editor.selectAdjacent(renderer.getLevelRenderer(), Block.TOP);
        }
        if (keys[GLFW.GLFW_KEY_5]) {
            Editor.selectAdjacent(renderer.getLevelRenderer(), Block.BACK);
        }
        if (keys[GLFW.GLFW_KEY_6]) {
            Editor.selectAdjacent(renderer.getLevelRenderer(), Block.FRONT);
        }
        if (keys[GLFW.GLFW_KEY_0] || keys[GLFW.GLFW_KEY_F]) {
            Editor.deselect();
        }
        if (mouseButtons[GLFW.GLFW_MOUSE_BUTTON_RIGHT]) {
            Editor.add(renderer.getLevelRenderer());
        }
        if (keys[GLFW.GLFW_KEY_R]) {
            Editor.remove(renderer.getLevelRenderer());
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
                    renderer.getIntrface().getCommandDialog().open();
                } else if (key == GLFW.GLFW_KEY_F1 && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
                    renderer.getIntrface().toggleShowHelp();
                } else if (key == GLFW.GLFW_KEY_F2 && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
                    renderer.getIntrface().getSaveDialog().open();
                } else if (key == GLFW.GLFW_KEY_F3 && action == GLFW.GLFW_PRESS) {
                    Arrays.fill(keys, false);
                    renderer.getIntrface().getLoadDialog().open();
                } else if (key == GLFW.GLFW_KEY_P && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    cycleCrosshairColor();
                } else if (key == GLFW.GLFW_KEY_M && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    cycleBlockColor();
                } else if (key == GLFW.GLFW_KEY_LEFT_BRACKET && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    Editor.selectPrevTexture(renderer.getLevelRenderer());
                } else if (key == GLFW.GLFW_KEY_RIGHT_BRACKET && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)) {
                    Editor.selectNextTexture(renderer.getLevelRenderer());
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

                if (xoffset != 0 || yoffset != 0) {
                    moveMouse = true;
                }

                lastX = (float) xpos;
                lastY = (float) ypos;
            }
        };
        GLFW.glfwSetCursorPosCallback(myWindow.getWindowID(), defaultCursorCallback);

        GLFW.glfwSetMouseButtonCallback(myWindow.getWindowID(), new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                if (action == GLFW.GLFW_PRESS) {
                    mouseButtons[button] = true;
                } else if (action == GLFW.GLFW_RELEASE) {
                    mouseButtons[button] = false;
                }
            }
        });
    }

    public void playAmbient() {

    }

    public void go() {
        // start the renderer
        renderer.start();

        // wait for renderer to initialize level renderer, water renderer and interface
        synchronized (objMutex) {
            try {
                objMutex.wait();
            } catch (InterruptedException ex) {
                DSLogger.reportFatalError(ex.getMessage());
            }
        }

        // start the music
        AudioFile audioFile = AudioFile.AMBIENT;
        audioPlayer.play(audioFile, true);

        double timer0 = GLFW.glfwGetTime();

        ups = 0;

        double lastTime = GLFW.glfwGetTime();
        double currTime;
        double diff;

        while (!myWindow.shouldClose()) {
            currTime = GLFW.glfwGetTime();
            diff = currTime - lastTime;
            upsTicks += diff * upsCap;
            lastTime = currTime;

            while (upsTicks >= 1.0) {
                GLFW.glfwPollEvents();
                renderer.update();
                observerDo();
                editorDo();
                ups++;
                upsTicks--;
            }

            // update label which shows fps every second
            if (GLFW.glfwGetTime() > timer0 + 1.0) {
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
            DSLogger.reportFatalError(ex.getMessage());
        }

        synchronized (objMutex) {
            myWindow.loadContext();
            myWindow.destroy();
        }

        audioPlayer.stop();
        MasterAudio.destroy();
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
        return cfg;
    }

    public static GLFWKeyCallback getDefaultKeyCallback() {
        return defaultKeyCallback;
    }

    public static GLFWCursorPosCallback getDefaultCursorCallback() {
        return defaultCursorCallback;
    }

    public static int getUpsCap() {
        return upsCap;
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

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }

}
