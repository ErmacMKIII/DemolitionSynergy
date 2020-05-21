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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import org.joml.Vector2f;
import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.audio.AudioPlayer;
import rs.alexanderstojanovich.evg.level.Editor;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.main.Game.Mode;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.main.Renderer;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.Pair;
import rs.alexanderstojanovich.evg.util.PlainTextReader;

/**
 *
 * @author Coa
 */
public class Intrface {

    private final GameObject gameObject;

    private Quad crosshair;
    private DynamicText updText; // displays updates
    private DynamicText fpsText; // displays framerates
    private DynamicText collText; // collision info
    private DynamicText helpText; // displays the help (toggle)
    private DynamicText progText; // progress text;
    private DynamicText screenText; // screenshot information
    private DynamicText gameModeText; // displays game mode {EDITOR, SINGLE_PLAYER or MUTLIPLAYER}
    private boolean showHelp = false;

    private ConcurrentDialog saveDialog;
    private ConcurrentDialog loadDialog;
    private ConcurrentDialog randLvlDialog;
    private ConcurrentDialog singlePlayerDialog;

    private Menu mainMenu;
    private OptionsMenu optionsMenu;
    private Menu editorMenu;

    public static final String FONT_IMG = "font.png"; // modified Hack font

    private final Console console = new Console();

    public Intrface(GameObject gameObject) {
        this.gameObject = gameObject;
        initIntrface();
    }

    private void initIntrface() {
        AudioPlayer musicPlayer = gameObject.getMusicPlayer();
        AudioPlayer soundFXPlayer = gameObject.getSoundFXPlayer();

        updText = new DynamicText(Texture.FONT, "", new Vector3f(0.0f, 1.0f, 0.0f), new Vector2f(-1.0f, 1.0f));
        updText.setColor(new Vector3f(0.0f, 1.0f, 0.0f));
        updText.setOffset(new Vector2f(1.0f, 1.0f));
        fpsText = new DynamicText(Texture.FONT, "", new Vector3f(0.0f, 1.0f, 0.0f), new Vector2f(-1.0f, 0.85f));
        fpsText.setColor(new Vector3f(0.0f, 1.0f, 0.0f));
        fpsText.setOffset(new Vector2f(1.0f, 1.0f));

        collText = new DynamicText(Texture.FONT, "No Collision", new Vector3f(0.0f, 1.0f, 0.0f), new Vector2f(-1.0f, -1.0f));
        collText.setOffset(new Vector2f(1.0f, -1.0f));
        helpText = new DynamicText(Texture.FONT, PlainTextReader.readFromFile(Game.INTRFACE_ENTRY, "help.txt"), new Vector3f(1.0f, 1.0f, 1.0f), new Vector2f(-1.0f, 0.9f));
        helpText.setOffset(new Vector2f(1.0f, 1.0f));
        helpText.setScale(0.625f);
        helpText.setEnabled(false);
        progText = new DynamicText(Texture.FONT, "", new Vector3f(1.0f, 1.0f, 0.0f), new Vector2f(-1.0f, -0.9f));
        progText.setOffset(new Vector2f(1.0f, -1.0f));
        screenText = new DynamicText(Texture.FONT, "", new Vector3f(1.0f, 1.0f, 1.0f), new Vector2f(-1.0f, -0.7f));
        screenText.setOffset(new Vector2f(1.0f, 1.0f));
        screenText.setScale(0.625f);
        gameModeText = new DynamicText(Texture.FONT, Game.getCurrentMode().name(), new Vector3f(0.0f, 1.0f, 0.0f), new Vector2f(1.0f, 1.0f));

        crosshair = new Quad(27, 27, Texture.CROSSHAIR, true); // it ignores resolution changes and doesn't scale
        List<Pair<String, Boolean>> mainMenuPairs = new ArrayList<>();
        mainMenuPairs.add(new Pair<>("SINGLE PLAYER", true));
        mainMenuPairs.add(new Pair<>("MULTIPLAYER", false));
        mainMenuPairs.add(new Pair<>("EDITOR", true));
        mainMenuPairs.add(new Pair<>("OPTIONS", true));
        mainMenuPairs.add(new Pair<>("EXIT", true));
        mainMenu = new Menu("", mainMenuPairs, FONT_IMG, new Vector2f(0.0f, 0.5f), 2.0f) {
            @Override
            protected void leave() {

            }

            @Override
            protected void execute() {
                String s = mainMenu.getItems().get(mainMenu.getSelected()).getContent();
                switch (s) {
                    case "SINGLE PLAYER":
                        singlePlayerDialog.open();
                        break;
                    case "EDITOR":
                        editorMenu.open();
                        break;
                    case "OPTIONS":
                        optionsMenu.open();
                        break;
                    case "EXIT":
                        GameObject.MY_WINDOW.close();
                        break;
                }
            }
        };
        Quad logo = new Quad(232, 100, Texture.LOGO);
        logo.getColor().x = 1.0f;
        logo.getColor().y = 0.7f;
        logo.getColor().z = 0.1f;
        mainMenu.setLogo(logo);
        mainMenu.setAlignmentAmount(Menu.ALIGNMENT_CENTER);

        saveDialog = new ConcurrentDialog(Texture.FONT, new Vector2f(-0.95f, 0.65f),
                "SAVE LEVEL TO FILE: ", "LEVEL SAVED SUCESSFULLY!", "SAVING LEVEL FAILED!") {
            @Override
            protected boolean execute(String command) {
                Editor.deselect();
                progText.enabled = true;
                boolean ok = gameObject.saveLevelToFile(command);
                if (ok) {
                    Game.setCurrentMode(Mode.EDITOR);
                }
                return ok;
            }
        };

        loadDialog = new ConcurrentDialog(Texture.FONT, new Vector2f(-0.95f, 0.65f),
                "LOAD LEVEL FROM FILE: ", "LEVEL LOADED SUCESSFULLY!", "LOADING LEVEL FAILED!") {
            @Override
            protected boolean execute(String command) {
                Editor.deselect();
                progText.enabled = true;
                boolean ok = gameObject.loadLevelFromFile(command);
                if (ok) {
                    Game.setCurrentMode(Mode.EDITOR);
                }
                return ok;
            }
        };

        randLvlDialog = new ConcurrentDialog(Texture.FONT, new Vector2f(-0.95f, 0.65f),
                "ENTER NUMBER OF BLOCKS (LIMIT 131070): ", "LEVEL GENERATED SUCESSFULLY", "LEVEL GENERATION FAILED!") {
            @Override
            protected boolean execute(String command) {
                Editor.deselect();
                progText.enabled = true;
                boolean ok = gameObject.generateRandomLevel(Integer.valueOf(command));
                if (ok) {
                    Game.setCurrentMode(Mode.EDITOR);
                }
                return ok;
            }
        };

        singlePlayerDialog = new ConcurrentDialog(Texture.FONT, new Vector2f(-0.95f, 0.65f), "START NEW GAME (Y/N)? ", "OK!", "ERROR!") {
            @Override
            protected boolean execute(String command) {
                boolean ok = false;
                if (!gameObject.isWorking() && (command.equalsIgnoreCase("yes") || command.equalsIgnoreCase("y"))) {
                    Editor.deselect();
                    Game.setCurrentMode(Mode.SINGLE_PLAYER);
                    ok = true;
                }
                return ok;
            }
        };

        List<Pair<String, Boolean>> optionsMenuPairs = new ArrayList<>();
        optionsMenuPairs.add(new Pair<>("FPS CAP", true));
        optionsMenuPairs.add(new Pair<>("RESOLUTION", true));
        optionsMenuPairs.add(new Pair<>("FULLSCREEN", true));
        optionsMenuPairs.add(new Pair<>("VSYNC", true));
        optionsMenuPairs.add(new Pair<>("WATER EFFECTS", true));
        optionsMenuPairs.add(new Pair<>("MOUSE SENSITIVITY", true));
        optionsMenuPairs.add(new Pair<>("MUSIC VOLUME", true));
        optionsMenuPairs.add(new Pair<>("SOUND VOLUME", true));
        optionsMenu = new OptionsMenu("OPTIONS", optionsMenuPairs, FONT_IMG, new Vector2f(0.0f, 0.5f), 2.0f) {
            @Override
            protected void leave() {
                mainMenu.open();
            }

            @Override
            protected void getValues() {
                options.get(0).getKey().setContent(String.valueOf(Game.getFpsMax()));
                options.get(1).getKey().setContent(String.valueOf(GameObject.MY_WINDOW.getWidth()) + "x" + String.valueOf(GameObject.MY_WINDOW.getHeight()));
                options.get(2).getKey().setContent(GameObject.MY_WINDOW.isFullscreen() ? "ON" : "OFF");
                options.get(3).getKey().setContent(GameObject.MY_WINDOW.isVsync() ? "ON" : "OFF");
                options.get(4).getKey().setContent(Game.isWaterEffects() ? "ON" : "OFF");
                options.get(5).getKey().setContent(String.valueOf(Game.getMouseSensitivity()));
                options.get(6).getKey().setContent(String.valueOf(musicPlayer.getGain()));
                options.get(7).getKey().setContent(String.valueOf(soundFXPlayer.getGain()));
            }

            @Override
            protected void execute() {
                Command command = Command.NOP;
                switch (selected) {
                    case 0:
                        command = Command.FPS_MAX;
                        command.getArgs().add(options.get(selected).getValue().giveCurrent());
                        Command.execute(command);
                        break;
                    case 1:
                        command = Command.RESOLUTION;
                        String giveCurrent = (String) options.get(selected).getValue().giveCurrent();
                        String things[] = giveCurrent.split("x");
                        command.getArgs().add(Integer.parseInt(things[0]));
                        command.getArgs().add(Integer.parseInt(things[1]));
                        Command.execute(command);
                        break;
                    case 2:
                        String fullscreen = (String) options.get(selected).getValue().giveCurrent();
                        switch (fullscreen) {
                            case "ON":
                                command = Command.FULLSCREEN;
                                break;
                            case "OFF":
                                command = Command.WINDOWED;
                                break;
                        }
                        Command.execute(command);
                        break;
                    case 3:
                        String vsync = (String) options.get(selected).getValue().giveCurrent();
                        command = Command.VSYNC;
                        switch (vsync) {
                            case "ON":
                                command.getArgs().add(true);
                                break;
                            case "OFF":
                                command.getArgs().add(false);
                                break;
                        }
                        FutureTask<Boolean> task = new FutureTask<Boolean>(command);
                        Renderer.TASK_QUEUE.add(task);
                        break;
                    case 4:
                        String waterEffects = (String) options.get(selected).getValue().giveCurrent();
                        command = Command.WATER_EFFECTS;
                        switch (waterEffects) {
                            case "ON":
                                command.getArgs().add(true);
                                break;
                            case "OFF":
                                command.getArgs().add(false);
                                break;
                        }
                        Command.execute(command);
                        break;
                    case 5:
                        float msens = (float) options.get(selected).getValue().giveCurrent();
                        command = Command.MOUSE_SENSITIVITY;
                        command.getArgs().add(msens);
                        Command.execute(command);
                        break;
                    case 6:
                        command = Command.MUSIC_VOLUME;
                        command.getArgs().add(options.get(selected).getValue().giveCurrent());
                        Command.execute(command);
                        break;
                    case 7:
                        command = Command.SOUND_VOLUME;
                        command.getArgs().add(options.get(selected).getValue().giveCurrent());
                        Command.execute(command);
                        break;
                }
            }
        };
        Object[] fpsCaps = {35, 60, 75, 100, 200, 300};
        Object[] resolutions = GameObject.MY_WINDOW.giveAllResolutions();
        Object[] swtch = {"OFF", "ON"};
        Object[] mouseSens = {1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f, 5.0f, 5.5f, 6.0f, 6.5f, 7.0f, 7.5f, 8.0f, 8.5f, 9.0f, 9.5f, 10.0f};
        Object[] volume = new Float[21];
        int k = 0;
        for (float i = 0.0f; i < 1.05f; i += 0.05f) {
            volume[k++] = Math.round(i * 100.0f) / 100.f; // rounding to two decimal places
        }

        optionsMenu.options.get(0).getValue().fetchFromArray(fpsCaps, 3);
        optionsMenu.options.get(1).getValue().fetchFromArray(resolutions, 0);
        optionsMenu.options.get(2).getValue().fetchFromArray(swtch, 0);
        optionsMenu.options.get(3).getValue().fetchFromArray(swtch, 0);
        optionsMenu.options.get(4).getValue().fetchFromArray(swtch, 1);
        optionsMenu.options.get(5).getValue().fetchFromArray(mouseSens, 1);
        optionsMenu.options.get(6).getValue().fetchFromArray(volume, 10);
        optionsMenu.options.get(7).getValue().fetchFromArray(volume, 10);
        optionsMenu.setAlignmentAmount(Menu.ALIGNMENT_LEFT);

        List<Pair<String, Boolean>> editorMenuPairs = new ArrayList<>();
        editorMenuPairs.add(new Pair<>("START NEW LEVEL", true));
        editorMenuPairs.add(new Pair<>("GENERATE RANDOM LEVEL", true));
        editorMenuPairs.add(new Pair<>("SAVE LEVEL TO FILE", true));
        editorMenuPairs.add(new Pair<>("LOAD LEVEL FROM FILE", true));

        editorMenu = new Menu("EDITOR", editorMenuPairs, FONT_IMG, new Vector2f(0.0f, 0.5f), 2.0f) {
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
                        gameObject.startNewLevel();
                        Game.setCurrentMode(Mode.EDITOR);
                        break;
                    case "GENERATE RANDOM LEVEL":
                        progText.setEnabled(true);
                        randLvlDialog.open();
                        break;
                    case "SAVE LEVEL TO FILE":
                        progText.setEnabled(true);
                        saveDialog.open();
                        break;
                    case "LOAD LEVEL FROM FILE":
                        progText.setEnabled(true);
                        loadDialog.open();
                        break;
                }
            }
        };
        editorMenu.setAlignmentAmount(Menu.ALIGNMENT_LEFT);
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
        } else {
            helpText.setEnabled(false);
            collText.setEnabled(true);
        }
    }

    public void render() {
        saveDialog.render();
        loadDialog.render();
        randLvlDialog.render();
        singlePlayerDialog.render();
        if (!updText.isBuffered()) {
            updText.buffer();
        }
        updText.render();
        if (!fpsText.isBuffered()) {
            fpsText.buffer();
        }
        fpsText.render();
        if (!collText.isBuffered()) {
            collText.buffer();
        }
        collText.render();
        if (!helpText.isBuffered()) {
            helpText.buffer();
        }
        helpText.render();
        if (!gameModeText.isBuffered()) {
            gameModeText.buffer();
        }
        gameModeText.render();
        if (!progText.isBuffered()) {
            progText.buffer();
        }
        progText.render();
        if (!screenText.isBuffered()) {
            screenText.buffer();
        }
        screenText.render();
        mainMenu.render();
        optionsMenu.render();
        editorMenu.render();
        if (!mainMenu.isEnabled() && !optionsMenu.isEnabled() && !editorMenu.isEnabled() && !showHelp) {
            if (!crosshair.isBuffered()) {
                crosshair.buffer();
            }
            crosshair.render();
        }
        console.render();
    }

    // update menu components
    public void update() {
        mainMenu.update();
        optionsMenu.update();
        editorMenu.update();
    }

    public GameObject getGameObject() {
        return gameObject;
    }

    public Quad getCrosshair() {
        return crosshair;
    }

    public DynamicText getUpdText() {
        return updText;
    }

    public DynamicText getFpsText() {
        return fpsText;
    }

    public DynamicText getCollText() {
        return collText;
    }

    public DynamicText getHelpText() {
        return helpText;
    }

    public DynamicText getScreenText() {
        return screenText;
    }

    public boolean isShowHelp() {
        return showHelp;
    }

    public ConcurrentDialog getSaveDialog() {
        return saveDialog;
    }

    public ConcurrentDialog getLoadDialog() {
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

    public static String getFONT_IMG() {
        return FONT_IMG;
    }

    public void setShowHelp(boolean showHelp) {
        this.showHelp = showHelp;
    }

    public ConcurrentDialog getRandLvlDialog() {
        return randLvlDialog;
    }

    public DynamicText getProgText() {
        return progText;
    }

    public DynamicText getGameModeText() {
        return gameModeText;
    }

    public ConcurrentDialog getSinglePlayerDialog() {
        return singlePlayerDialog;
    }

    public Console getConsole() {
        return console;
    }

}
