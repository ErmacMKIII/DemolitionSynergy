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
import rs.alexanderstojanovich.evg.core.Combo;
import rs.alexanderstojanovich.evg.core.Editor;
import rs.alexanderstojanovich.evg.core.LevelRenderer;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.main.Game;

/**
 *
 * @author Coa
 */
public class Intrface {

    private final Window myWindow;

    private Quad crosshair;
    private Text infoText;
    private Text collText;
    private Text helpText;

    private boolean showHelp = false;

    private Dialog commandDialog;
    private Dialog saveDialog;
    private Dialog loadDialog;

    private Menu mainMenu;
    private AdvMenu optionsMenu;
    private Menu editorMenu;

    private final LevelRenderer levelRenderer;

    public Intrface(Window myWindow, LevelRenderer levelRenderer) {
        this.myWindow = myWindow;
        this.levelRenderer = levelRenderer;
        initIntrface();
    }

    private void initIntrface() {
        infoText = new Text(myWindow, "consolas.png", "Hello World!", new Vector3f(0.0f, 1.0f, 0.0f), new Vector2f(-0.95f, 0.95f));
        collText = new Text(myWindow, "consolas.png", "No Collision", new Vector3f(0.0f, 1.0f, 0.0f), new Vector2f(-0.95f, -0.95f));
        helpText = new Text(myWindow, "consolas.png", "", new Vector3f(1.0f, 1.0f, 1.0f), new Vector2f(-0.95f, 0.75f));
        helpText.setContent(Text.readFromFile("help.txt"));
        helpText.setEnabled(false);

        crosshair = new Quad(myWindow, 27, 27, "crosshairUltimate.png");

        mainMenu = new Menu(myWindow, "", "mainMenu.txt", "consolas.png", new Vector2f(-0.5f, 0.5f), 2.0f) {
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
        mainMenu.setAlignmentAmount(Menu.ALIGNMENT_CENTER);
        Quad logo = new Quad(myWindow, 180, 100, "ds_acro.png");
        mainMenu.setLogo(logo);

        commandDialog = new Dialog(myWindow, "consolas.png", new Vector2f(-0.95f, 0.85f)) {
            @Override
            protected boolean execute(String command) {
                boolean success = true;
                String[] things = command.split(" ");
                switch (things[0].toLowerCase()) {
                    case "fps_max":
                    case "fpsmax":
                        int num = Integer.parseInt(things[1]);
                        if (num > 0) {
                            Game.setFpsMax(num);
                        } else {
                            success = false;
                        }
                        break;
                    case "resolution":
                    case "res":
                        int width = Integer.parseInt(things[1]);
                        int height = Integer.parseInt(things[2]);
                        success = myWindow.setResolution(width, height);
                        myWindow.centerTheWindow();
                        break;
                    case "fullscreen":
                        myWindow.fullscreen();
                        myWindow.centerTheWindow();
                        break;
                    case "windowed":
                        myWindow.windowed();
                        myWindow.centerTheWindow();
                        break;
                    case "v_sync":
                    case "vsync":
                        if (Boolean.parseBoolean(things[1])) {
                            myWindow.enableVSync();
                        } else {
                            myWindow.disableVSync();
                        }
                        break;
                    default:
                        success = false;
                        break;
                }
                return success;
            }
        };

        saveDialog = new Dialog(myWindow, "consolas.png", new Vector2f(-0.95f, 0.85f)) {
            @Override
            protected boolean execute(String command) {
                Editor.deselect();
                return (levelRenderer.saveLevelToFile(command));
            }
        };

        loadDialog = new Dialog(myWindow, "consolas.png", new Vector2f(-0.95f, 0.85f)) {
            @Override
            protected boolean execute(String command) {
                Editor.deselect();
                return (levelRenderer.loadLevelFromFile(command));
            }
        };

        optionsMenu = new AdvMenu(myWindow, "OPTIONS", "optionsMenu.txt", "consolas.png", new Vector2f(-0.5f, 0.5f), 2.0f) {
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
            }

            @Override
            protected void execute() {
                if (getOptions()[0].giveCurrent() != null) {
                    Game.setFpsMax((int) getOptions()[0].giveCurrent());
                }
                //--------------------------------------------------------------
                if (getOptions()[1].giveCurrent() != null) {
                    String[] things = getOptions()[1].giveCurrent().toString().split("x");
                    myWindow.setResolution(Integer.parseInt(things[0]), Integer.parseInt(things[1]));
                }
                //--------------------------------------------------------------
                if (getOptions()[2].giveCurrent() != null) {
                    switch (getOptions()[2].giveCurrent().toString()) {
                        case "OFF":
                            myWindow.windowed();
                            myWindow.centerTheWindow();
                            break;
                        case "ON":
                            myWindow.fullscreen();
                            myWindow.centerTheWindow();
                            break;
                    }
                }
                //--------------------------------------------------------------
                if (getOptions()[3].giveCurrent() != null) {
                    switch (getOptions()[3].giveCurrent().toString()) {
                        case "OFF":
                            myWindow.disableVSync();
                            break;
                        case "ON":
                            myWindow.enableVSync();
                            break;
                    }
                }
            }
        };
        Object[] fpsCaps = {35, 60, 75, 100, 200};
        Object[] resolutions = myWindow.giveAllResolutions();
        Object[] swtch = {"OFF", "ON"};
        optionsMenu.getOptions()[0] = new Combo(fpsCaps, 3);
        optionsMenu.getOptions()[1] = new Combo(resolutions, 0);
        optionsMenu.getOptions()[2] = new Combo(swtch, 0);
        optionsMenu.getOptions()[3] = new Combo(swtch, 0);
        optionsMenu.setAlignmentAmount(Menu.ALIGNMENT_RIGHT);

        editorMenu = new Menu(myWindow, "EDITOR", "editorMenu.txt", "consolas.png", new Vector2f(-0.5f, 0.5f), 2.0f) {
            @Override
            protected void leave() {
                mainMenu.open();
            }

            @Override
            protected void execute() {
                String s = editorMenu.getItems().get(editorMenu.getSelected()).getContent();
                switch (s) {
                    case "START NEW LEVEL":
                        levelRenderer.startNewLevel();
                        break;
                    case "SAVE LEVEL TO FILE":
                        saveDialog.open("SAVE LEVEL TO FILE: ", "LEVEL SAVED SUCESSFULLY!", "SAVING LEVEL FAILED!");
                        break;
                    case "LOAD LEVEL FROM FILE":
                        loadDialog.open("LOAD LEVEL FROM FILE: ", "LEVEL LOADED SUCESSFULLY!", "LOADING LEVEL FAILED!");
                        break;
                }
            }
        };
    }

    public void setCollText(boolean mode) {
        if (mode) {
            collText.setContent("Collision!");
            collText.getColor().x = 1.0f;
            collText.getColor().y = 0.0f;
            collText.getColor().z = 0.0f;
        } else {
            collText.setContent("No Collision");
            collText.getColor().x = 0.0f;
            collText.getColor().y = 1.0f;
            collText.getColor().z = 0.0f;
        }
    }

    public void toggleShowHelp() {
        showHelp = !showHelp;
        if (showHelp) {
            helpText.setEnabled(true);
            collText.setEnabled(false);
            crosshair.setEnabled(false);
        } else {
            helpText.setEnabled(false);
            collText.setEnabled(true);
            crosshair.setEnabled(true);
        }
    }

    public void render() {
        commandDialog.render();
        saveDialog.render();
        loadDialog.render();
        infoText.render();
        collText.render();
        helpText.render();
        mainMenu.render();
        optionsMenu.render();
        editorMenu.render();
        if (!mainMenu.isEnabled() && !optionsMenu.isEnabled() && !editorMenu.isEnabled()) {
            crosshair.render();
        }
    }

    public Window getMyWindow() {
        return myWindow;
    }

    public Quad getCrosshair() {
        return crosshair;
    }

    public Text getInfoText() {
        return infoText;
    }

    public Text getCollText() {
        return collText;
    }

    public Text getHelpText() {
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

    public AdvMenu getOptionsMenu() {
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

}
