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
package rs.alexanderstojanovich.evg.intrface;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import rs.alexanderstojanovich.evg.core.Combo;
import rs.alexanderstojanovich.evg.core.Editor;
import rs.alexanderstojanovich.evg.core.LevelRenderer;
import rs.alexanderstojanovich.evg.core.MasterRenderer;
import rs.alexanderstojanovich.evg.core.PerspectiveRenderer;
import rs.alexanderstojanovich.evg.core.Texture;
import rs.alexanderstojanovich.evg.core.WaterRenderer;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.main.Game;

/**
 *
 * @author Coa
 */
public class Intrface {

    private final Window myWindow;

    private Quad crosshair;
    private DynamicText infoText; // displays update and framerate
    private DynamicText collText; // collision info
    private DynamicText helpText; // displays the help (toggle)
    private DynamicText progText; // progress text;

    private boolean showHelp = false;

    private Dialog commandDialog;
    private Dialog saveDialog;
    private Dialog loadDialog;
    private Dialog randLvlDialog;

    private Menu mainMenu;
    private OptionsMenu optionsMenu;
    private Menu editorMenu;

    private final LevelRenderer levelRenderer;
    private final WaterRenderer waterRenderer;

    public static final String FONT_IMG = "hack.png";

    private final Object objMutex;

    public Intrface(Window myWindow, LevelRenderer levelRenderer, WaterRenderer waterRenderer, Object objMutex) {
        this.myWindow = myWindow;
        this.levelRenderer = levelRenderer;
        this.waterRenderer = waterRenderer;
        this.objMutex = objMutex;
        initIntrface();
    }

    private void initIntrface() {
        infoText = new DynamicText(myWindow, Texture.FONT, "Hello World!", new Vector3f(0.0f, 1.0f, 0.0f), new Vector2f(-0.98f, 0.95f));
        collText = new DynamicText(myWindow, Texture.FONT, "No Collision", new Vector3f(0.0f, 1.0f, 0.0f), new Vector2f(-0.98f, -0.95f));
        helpText = new DynamicText(myWindow, Texture.FONT, Text.readFromFile("help.txt"), new Vector3f(1.0f, 1.0f, 1.0f), new Vector2f(-0.98f, 0.85f));
        progText = new DynamicText(myWindow, Texture.FONT, "", new Vector3f(1.0f, 1.0f, 0.0f), new Vector2f(-0.98f, -0.85f));
        helpText.setEnabled(false);

        crosshair = new Quad(myWindow, 27, 27, Texture.CROSSHAIR, true); // it ignores resolution changes and doesn't scale

        mainMenu = new Menu(myWindow, "", "mainMenu.txt", FONT_IMG, new Vector2f(0.0f, 0.5f), 2.0f) {
            @Override
            protected void leave() {

            }

            @Override
            protected void execute() {
                String s = mainMenu.getItems().get(mainMenu.getSelected()).getContent();
                switch (s) {
                    case "EDITOR":
                        editorMenu.open();
                        break;
                    case "OPTIONS":
                        optionsMenu.open();
                        break;
                    case "EXIT":
                        GLFW.glfwSetWindowShouldClose(myWindow.getWindowID(), true);
                        break;
                }
            }
        };
        Quad logo = new Quad(myWindow, 180, 100, Texture.LOGO);
        logo.getColor().x = 1.0f;
        logo.getColor().y = 0.7f;
        logo.getColor().z = 0.1f;
        mainMenu.setLogo(logo);
        mainMenu.setAlignmentAmount(Menu.ALIGNMENT_CENTER);

        commandDialog = new Dialog(myWindow, Texture.FONT, new Vector2f(-0.95f, 0.85f)) {
            @Override
            protected boolean execute(String command) {
                boolean success = false;
                String[] things = command.split(" ");
                if (things.length > 0) {
                    switch (things[0].toLowerCase()) {
                        case "fps_max":
                        case "fpsmax":
                            if (things.length == 2) {
                                int num = Integer.parseInt(things[1]);
                                if (num > 0) {
                                    Game.setFpsMax(num);
                                    success = true;
                                }
                            }
                            break;
                        case "resolution":
                        case "res":
                            if (things.length == 3) {
                                int width = Integer.parseInt(things[1]);
                                int height = Integer.parseInt(things[2]);
                                synchronized (objMutex) {
                                    myWindow.loadContext();
                                    GL.setCapabilities(MasterRenderer.getGlCaps());
                                    success = myWindow.setResolution(width, height);
                                    myWindow.centerTheWindow();
                                    PerspectiveRenderer.updatePerspective(myWindow);
                                    GL.setCapabilities(null);
                                    Window.unloadContext();
                                }
                            }
                            break;
                        case "fullscreen":
                            synchronized (objMutex) {
                                myWindow.loadContext();
                                myWindow.fullscreen();
                                myWindow.centerTheWindow();
                                Window.unloadContext();
                            }
                            success = true;
                            break;
                        case "windowed":
                            synchronized (objMutex) {
                                myWindow.loadContext();
                                myWindow.windowed();
                                myWindow.centerTheWindow();
                                Window.unloadContext();
                            }
                            success = true;
                            break;
                        case "v_sync":
                        case "vsync":
                            if (things.length == 2) {
                                synchronized (objMutex) {
                                    myWindow.loadContext();
                                    if (Boolean.parseBoolean(things[1])) {
                                        myWindow.enableVSync();
                                    } else {
                                        myWindow.disableVSync();
                                    }
                                    Window.unloadContext();
                                }
                                success = true;
                            }
                            break;
                        case "water_effects":
                            if (things.length == 2) {
                                if (Boolean.parseBoolean(things[1])) {
                                    Game.setWaterEffects(true);
                                } else {
                                    Game.setWaterEffects(false);
                                    waterRenderer.removeEffects();
                                }
                                success = true;
                            }
                        case "msens":
                        case "mouse_sensitivity":
                            if (things.length == 2) {
                                Game.setMouseSensitivity(Float.parseFloat(things[1]));
                                success = true;
                            }
                            break;
                        default:
                            success = false;
                            break;
                    }
                }
                return success;
            }
        };

        saveDialog = new Dialog(myWindow, Texture.FONT, new Vector2f(-0.95f, 0.85f)) {
            @Override
            protected boolean execute(String command) {
                Editor.deselect();
                progText.enabled = true;
                return levelRenderer.saveLevelToFile(command);
            }
        };

        loadDialog = new Dialog(myWindow, Texture.FONT, new Vector2f(-0.95f, 0.85f)) {
            @Override
            protected boolean execute(String command) {
                Editor.deselect();
                progText.enabled = true;
                return (levelRenderer.loadLevelFromFile(command));
            }
        };

        randLvlDialog = new Dialog(myWindow, Texture.FONT, new Vector2f(-0.95f, 0.85f)) {
            @Override
            protected boolean execute(String command) {
                Editor.deselect();
                progText.enabled = true;
                return levelRenderer.generateRandomLevel(Integer.valueOf(command));
            }
        };

        optionsMenu = new OptionsMenu(myWindow, "OPTIONS", "optionsMenu.txt", FONT_IMG, new Vector2f(0.0f, 0.5f), 2.0f) {
            @Override
            protected void leave() {
                mainMenu.open();
            }

            @Override
            protected void refreshValues() {
                getValues()[0].setContent(String.valueOf(Game.getFpsMax()));
                getValues()[1].setContent(String.valueOf(myWindow.getWidth()) + "x" + String.valueOf(myWindow.getHeight()));
                getValues()[2].setContent(myWindow.isFullscreen() ? "ON" : "OFF");
                getValues()[3].setContent(myWindow.isVsync() ? "ON" : "OFF");
                getValues()[4].setContent(Game.isWaterEffects() ? "ON" : "OFF");
                getValues()[5].setContent(String.valueOf(Game.getMouseSensitivity()));
            }

            @Override
            protected void execute() {
                if (getOptions()[0].giveCurrent() != null) {
                    Game.setFpsMax((int) getOptions()[0].giveCurrent());
                }
                //--------------------------------------------------------------
                if (getOptions()[1].giveCurrent() != null) {
                    String[] things = getOptions()[1].giveCurrent().toString().split("x");
                    synchronized (objMutex) {
                        myWindow.loadContext();
                        GL.setCapabilities(MasterRenderer.getGlCaps());
                        myWindow.setResolution(Integer.parseInt(things[0]), Integer.parseInt(things[1]));
                        PerspectiveRenderer.updatePerspective(myWindow);
                        GL.setCapabilities(null);
                        Window.unloadContext();
                    }
                }
                //--------------------------------------------------------------
                if (getOptions()[2].giveCurrent() != null) {
                    switch (getOptions()[2].giveCurrent().toString()) {
                        case "OFF":
                            synchronized (objMutex) {
                                myWindow.loadContext();
                                myWindow.windowed();
                                myWindow.centerTheWindow();
                                Window.unloadContext();
                            }
                            break;
                        case "ON":
                            synchronized (objMutex) {
                                myWindow.loadContext();
                                myWindow.fullscreen();
                                myWindow.centerTheWindow();
                                Window.unloadContext();
                            }
                            break;
                    }
                }
                //--------------------------------------------------------------
                if (getOptions()[3].giveCurrent() != null) {
                    switch (getOptions()[3].giveCurrent().toString()) {
                        case "OFF":
                            synchronized (objMutex) {
                                myWindow.loadContext();
                                myWindow.disableVSync();
                                Window.unloadContext();
                            }
                            break;
                        case "ON":
                            synchronized (objMutex) {
                                myWindow.loadContext();
                                myWindow.enableVSync();
                                Window.unloadContext();
                            }
                            break;
                    }
                }
                //--------------------------------------------------------------
                if (getOptions()[4].giveCurrent() != null) {
                    switch (getOptions()[4].giveCurrent().toString()) {
                        case "OFF":
                            Game.setWaterEffects(false);
                            waterRenderer.removeEffects();
                            break;
                        case "ON":
                            Game.setWaterEffects(true);
                            break;
                    }
                }
                //--------------------------------------------------------------
                if (getOptions()[5].giveCurrent() != null) {
                    Game.setMouseSensitivity(Float.parseFloat(getOptions()[5].giveCurrent().toString()));
                }
            }
        };
        Object[] fpsCaps = {35, 60, 75, 100, 200, 300};
        Object[] resolutions = myWindow.giveAllResolutions();
        Object[] swtch = {"OFF", "ON"};
        Object[] mouseSens = {1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f, 5.0f, 5.5f, 6.0f, 6.5f, 7.0f, 7.5f, 8.0f, 8.5f, 9.0f, 9.5f, 10.0f};
        optionsMenu.getOptions()[0] = new Combo(fpsCaps, 3);
        optionsMenu.getOptions()[1] = new Combo(resolutions, 0);
        optionsMenu.getOptions()[2] = new Combo(swtch, 0);
        optionsMenu.getOptions()[3] = new Combo(swtch, 0);
        optionsMenu.getOptions()[4] = new Combo(swtch, 1);
        optionsMenu.getOptions()[5] = new Combo(mouseSens, 4);
        optionsMenu.setAlignmentAmount(Menu.ALIGNMENT_LEFT);

        editorMenu = new Menu(myWindow, "EDITOR", "editorMenu.txt", FONT_IMG, new Vector2f(0.0f, 0.5f), 2.0f) {
            @Override
            protected void leave() {
                mainMenu.open();
            }

            @Override
            protected void execute() {
                String s = editorMenu.getItems().get(editorMenu.getSelected()).getContent();
                switch (s) {
                    case "START NEW LEVEL":
                        progText.setEnabled(true);
                        levelRenderer.startNewLevel();
                        break;
                    case "GENERATE RANDOM LEVEL":
                        progText.setEnabled(true);
                        randLvlDialog.open("ENTER MAX NUMBER OF BLOCKS (LIMIT 131070): ", "LEVEL GENERATED SUCESSFULLY", "LEVEL GENERATION FAILED!");
                        break;
                    case "SAVE LEVEL TO FILE":
                        progText.setEnabled(true);
                        saveDialog.open("SAVE LEVEL TO FILE: ", "LEVEL SAVED SUCESSFULLY!", "SAVING LEVEL FAILED!");
                        break;
                    case "LOAD LEVEL FROM FILE":
                        progText.setEnabled(true);
                        loadDialog.open("LOAD LEVEL FROM FILE: ", "LEVEL LOADED SUCESSFULLY!", "LOADING LEVEL FAILED!");
                        break;
                }
            }
        };
        editorMenu.setAlignmentAmount(Menu.ALIGNMENT_LEFT);
    }

    public void setCollText(boolean mode) {
        if (mode) {
            collText.setContent("Collision!");
            collText.getQuad().getColor().x = 1.0f;
            collText.getQuad().getColor().y = 0.0f;
            collText.getQuad().getColor().z = 0.0f;
        } else {
            collText.setContent("No Collision");
            collText.getQuad().getColor().x = 0.0f;
            collText.getQuad().getColor().y = 1.0f;
            collText.getQuad().getColor().z = 0.0f;
        }
    }

    public void toggleShowHelp() {
        showHelp = !showHelp;
        if (showHelp) {
            helpText.setEnabled(true);
            collText.setEnabled(false);
        } else {
            helpText.setEnabled(false);
            collText.setEnabled(true);
        }
    }

    public void render() {
        commandDialog.render();
        saveDialog.render();
        loadDialog.render();
        randLvlDialog.render();
        if (!infoText.isBuffered()) {
            infoText.buffer();
        }
        infoText.render();
        if (!collText.isBuffered()) {
            collText.buffer();
        }
        collText.render();
        if (!helpText.isBuffered()) {
            helpText.buffer();
        }
        helpText.render();
        mainMenu.render();
        optionsMenu.render();
        editorMenu.render();
        if (!mainMenu.isEnabled() && !optionsMenu.isEnabled() && !editorMenu.isEnabled() && !showHelp) {
            if (!crosshair.isBuffered()) {
                crosshair.buffer();
            }
            crosshair.render();
        }
    }

    public Window getMyWindow() {
        return myWindow;
    }

    public Quad getCrosshair() {
        return crosshair;
    }

    public DynamicText getInfoText() {
        return infoText;
    }

    public DynamicText getCollText() {
        return collText;
    }

    public DynamicText getHelpText() {
        return helpText;
    }

    public boolean isShowHelp() {
        return showHelp;
    }

    public Dialog getCommandDialog() {
        return commandDialog;
    }

    public Dialog getSaveDialog() {
        return saveDialog;
    }

    public Dialog getLoadDialog() {
        return loadDialog;
    }

    public Menu getMainMenu() {
        return mainMenu;
    }

    public OptionsMenu getOptionsMenu() {
        return optionsMenu;
    }

    public Menu getEditorMenu() {
        return editorMenu;
    }

    public LevelRenderer getLevelRenderer() {
        return levelRenderer;
    }

    public void setShowHelp(boolean showHelp) {
        this.showHelp = showHelp;
    }

    public WaterRenderer getWaterRenderer() {
        return waterRenderer;
    }

    public Dialog getRandLvlDialog() {
        return randLvlDialog;
    }

    public DynamicText getProgText() {
        return progText;
    }

    public Object getObjMutex() {
        return objMutex;
    }

}
