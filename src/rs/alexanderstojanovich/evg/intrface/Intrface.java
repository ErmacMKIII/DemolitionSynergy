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
package rs.alexanderstojanovich.evg.intrface;

import java.io.File;
import java.io.FilenameFilter;
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
import rs.alexanderstojanovich.evg.main.GameRenderer;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.PlainTextReader;
import rs.alexanderstojanovich.evg.util.Vector3fColors;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Intrface {

    private Quad crosshair;
    private DynamicText updText; // displays updates
    private DynamicText fpsText; // displays framerates
    private DynamicText posText; // display position
    private DynamicText chunkText; // display current chunk (player)

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
    private Menu creditsMenu;
    private OptionsMenu randLvlMenu;
    private Menu loadLvlMenu;

    private int numBlocks = 0;

    public static final String FONT_IMG = "font.png"; // modified Hack font

    private final Console console = new Console();

    public Intrface() {
        initIntrface();
    }

    private void initIntrface() {
        AudioPlayer musicPlayer = GameObject.getMusicPlayer();
        AudioPlayer soundFXPlayer = GameObject.getSoundFXPlayer();

        updText = new DynamicText(Texture.FONT, "", Vector3fColors.GREEN, new Vector2f(-1.0f, 1.0f));
        updText.alignToNextChar();
        fpsText = new DynamicText(Texture.FONT, "", Vector3fColors.GREEN, new Vector2f(-1.0f, 0.85f));
        fpsText.alignToNextChar();

        posText = new DynamicText(Texture.FONT, "", Vector3fColors.GREEN, new Vector2f(1.0f, -1.0f));
        posText.setAlignment(Text.ALIGNMENT_RIGHT);
        posText.alignToNextChar();

        chunkText = new DynamicText(Texture.FONT, "", Vector3fColors.GREEN, new Vector2f(1.0f, -0.85f));
        chunkText.setAlignment(Text.ALIGNMENT_RIGHT);
        chunkText.alignToNextChar();

        collText = new DynamicText(Texture.FONT, "No Collision", Vector3fColors.GREEN, new Vector2f(-1.0f, -1.0f));
        collText.alignToNextChar();
        helpText = new DynamicText(Texture.FONT, PlainTextReader.readFromFile(Game.INTRFACE_ENTRY, "help.txt"), Vector3fColors.WHITE, new Vector2f(-1.0f, 0.9f));
        helpText.setScale(0.625f);
        helpText.alignToNextChar();
        helpText.setEnabled(false);
        progText = new DynamicText(Texture.FONT, "", Vector3fColors.YELLOW, new Vector2f(-1.0f, -0.9f));
        progText.alignToNextChar();
        screenText = new DynamicText(Texture.FONT, "", Vector3fColors.WHITE, new Vector2f(-1.0f, -0.7f));
        screenText.setScale(0.625f);
        screenText.alignToNextChar();
        gameModeText = new DynamicText(Texture.FONT, Game.getCurrentMode().name(), Vector3fColors.GREEN, new Vector2f(1.0f, 1.0f));
        gameModeText.setAlignment(Text.ALIGNMENT_RIGHT);
        gameModeText.alignToNextChar();

        crosshair = new Quad(27, 27, Texture.CROSSHAIR, true); // it ignores resolution changes and doesn't scale
        crosshair.setColor(Vector3fColors.WHITE);
        List<MenuItem> mainMenuItems = new ArrayList<>();
        mainMenuItems.add(new MenuItem("SINGLE PLAYER", Menu.EditType.EditNoValue, null));
        mainMenuItems.add(new MenuItem("MULTIPLAYER", Menu.EditType.EditNoValue, null));
        mainMenuItems.add(new MenuItem("EDITOR", Menu.EditType.EditNoValue, null));
        mainMenuItems.add(new MenuItem("OPTIONS", Menu.EditType.EditNoValue, null));
        mainMenuItems.add(new MenuItem("CREDITS", Menu.EditType.EditNoValue, null));
        mainMenuItems.add(new MenuItem("EXIT", Menu.EditType.EditNoValue, null));
        mainMenu = new Menu("", mainMenuItems, FONT_IMG, new Vector2f(0.0f, 0.5f), 2.0f) {
            @Override
            protected void leave() {

            }

            @Override
            protected void execute() {
                String s = mainMenu.items.get(mainMenu.getSelected()).keyText.content;
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
                    case "CREDITS":
                        creditsMenu.open();
                        break;
                    case "EXIT":
                        GameObject.MY_WINDOW.close();
                        break;
                }
            }
        };
        Quad logo = new Quad(232, 100, Texture.LOGO);
        logo.setColor(new Vector3f(1.0f, 0.7f, 0.1f));
        mainMenu.setLogo(logo);
        mainMenu.setAlignmentAmount(Text.ALIGNMENT_CENTER);

        saveDialog = new ConcurrentDialog(Texture.FONT, new Vector2f(-0.95f, 0.65f),
                "SAVE LEVEL TO FILE: ", "LEVEL SAVED SUCESSFULLY!", "SAVING LEVEL FAILED!") {
            @Override
            protected boolean execute(String command) {
                Editor.deselect();
                progText.enabled = true;
                boolean ok = GameObject.saveLevelToFile(command);
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
                boolean ok = GameObject.loadLevelFromFile(command);
                if (ok) {
                    Game.setCurrentMode(Mode.EDITOR);
                }
                return ok;
            }
        };
        loadDialog.dialog.alignToNextChar();

        File currFile = new File("./");
        String[] datFileList = currFile.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".dat");
            }
        });

        List<MenuItem> loadLvlMenuPairs = new ArrayList<>();
        for (String datFile : datFileList) {
            loadLvlMenuPairs.add(new MenuItem(datFile, Menu.EditType.EditNoValue, null));
        }

        loadLvlMenu = new Menu("LOAD LEVEL", loadLvlMenuPairs, FONT_IMG, new Vector2f(0.0f, 0.5f), 2.0f) {
            @Override
            protected void leave() {
                editorMenu.open();
            }

            @Override
            protected void execute() {
                String chosen = loadLvlMenu.items.get(loadLvlMenu.getSelected()).keyText.getContent();
                GameObject.loadLevelFromFile(chosen);
            }
        };
        loadLvlMenu.setAlignmentAmount(Text.ALIGNMENT_LEFT);

        randLvlDialog = new ConcurrentDialog(Texture.FONT, new Vector2f(-0.95f, 0.65f),
                "GENERATE RANDOM LEVEL\n(TIME-CONSUMING OPERATION) (Y/N)? ", "LEVEL GENERATED SUCESSFULLY!", "LEVEL GENERATION FAILED!") {
            @Override
            protected boolean execute(String command) {
                boolean ok = false;
                if (!GameObject.isWorking() && (command.equalsIgnoreCase("yes") || command.equalsIgnoreCase("y"))) {
                    Editor.deselect();
                    ok = GameObject.generateRandomLevel(numBlocks);
                    if (ok) {
                        Game.setCurrentMode(Mode.EDITOR);
                    }
                }
                return ok;
            }
        };
        randLvlDialog.dialog.alignToNextChar();

        List<MenuItem> randLvlMenuItems = new ArrayList<>();
        randLvlMenuItems.add(new MenuItem("SMALL  (25000  blocks)", Menu.EditType.EditNoValue, null));
        randLvlMenuItems.add(new MenuItem("MEDIUM (50000  blocks)", Menu.EditType.EditNoValue, null));
        randLvlMenuItems.add(new MenuItem("LARGE  (100000 blocks)", Menu.EditType.EditNoValue, null));
        randLvlMenuItems.add(new MenuItem("HUGE   (131070 blocks)", Menu.EditType.EditNoValue, null));
        randLvlMenuItems.add(new MenuItem("SEED  ", Menu.EditType.EditSingleValue, new SingleValue(GameObject.getRandomLevelGenerator().getSeed(), MenuValue.Type.LONG)));

        randLvlMenu = new OptionsMenu("GENERATE RANDOM LEVEL", randLvlMenuItems, FONT_IMG, new Vector2f(0.0f, 0.5f), 2.0f) {
            @Override
            protected void leave() {
                mainMenu.open();
            }

            @Override
            protected void execute() {
                String str = randLvlMenu.items.get(selected).keyText.content;
                String[] split = str.split("\\s+");
                switch (split[0]) {
                    case "SMALL":
                        numBlocks = 25000;
                        break;
                    case "MEDIUM":
                        numBlocks = 50000;
                        break;
                    case "LARGE":
                        numBlocks = 100000;
                        break;
                    case "HUGE":
                        numBlocks = 131070;
                        break;
                    default:
                    case "SEED":
                        MenuItem selectedMenuItem = randLvlMenu.items.get(selected);
                        GameObject.getRandomLevelGenerator().setSeed((long) selectedMenuItem.menuValue.getCurrentValue());
                        break;
                }

                if (numBlocks != 0 && selected != 4) {
                    randLvlDialog.open();
                }
            }

        };
        randLvlMenu.getItems().get(4).menuValue.getValueText().setScale(2.0f);

        singlePlayerDialog = new ConcurrentDialog(Texture.FONT, new Vector2f(-0.95f, 0.65f), "START NEW GAME (Y/N)? ", "OK!", "ERROR!") {
            @Override
            protected boolean execute(String command) {
                boolean ok = false;
                if (!GameObject.isWorking() && (command.equalsIgnoreCase("yes") || command.equalsIgnoreCase("y"))) {
                    Editor.deselect();
                    Game.setCurrentMode(Mode.SINGLE_PLAYER);
                    ok = true;
                }
                return ok;
            }
        };
        singlePlayerDialog.dialog.alignToNextChar();

        Object[] fpsCaps = {35, 60, 75, 100, 200, 300};
        Object[] resolutions = GameObject.MY_WINDOW.giveAllResolutions();
        Object[] swtch = {"OFF", "ON"};
        Object[] mouseSens = {1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f, 5.0f, 5.5f, 6.0f, 6.5f, 7.0f, 7.5f, 8.0f, 8.5f, 9.0f, 9.5f, 10.0f};
        Object[] volume = new Float[21];
        int k = 0;
        for (float i = 0.0f; i < 1.05f; i += 0.05f) {
            volume[k++] = Math.round(i * 100.0f) / 100.f; // rounding to two decimal places
        }

        List<MenuItem> optionsMenuPairs = new ArrayList<>();
        optionsMenuPairs.add(new MenuItem("FPS CAP", Menu.EditType.EditMultiValue, new MultiValue(fpsCaps, MenuValue.Type.INT, String.valueOf(Game.getFpsMax()))));
        optionsMenuPairs.add(new MenuItem("RESOLUTION", Menu.EditType.EditMultiValue, new MultiValue(
                resolutions,
                MenuValue.Type.STRING,
                String.valueOf(GameObject.MY_WINDOW.getWidth()) + "x" + String.valueOf(GameObject.MY_WINDOW.getHeight()))));
        optionsMenuPairs.add(new MenuItem("FULLSCREEN", Menu.EditType.EditMultiValue, new MultiValue(swtch, MenuValue.Type.STRING, GameObject.MY_WINDOW.isFullscreen() ? "ON" : "OFF")));
        optionsMenuPairs.add(new MenuItem("VSYNC", Menu.EditType.EditMultiValue, new MultiValue(swtch, MenuValue.Type.STRING, GameObject.MY_WINDOW.isVsync() ? "ON" : "OFF")));
        optionsMenuPairs.add(new MenuItem("WATER EFFECTS", Menu.EditType.EditMultiValue, new MultiValue(swtch, MenuValue.Type.STRING, Game.isWaterEffects() ? "ON" : "OFF")));
        optionsMenuPairs.add(new MenuItem("MOUSE SENSITIVITY", Menu.EditType.EditMultiValue, new MultiValue(mouseSens, MenuValue.Type.FLOAT, Game.getMouseSensitivity())));
        optionsMenuPairs.add(new MenuItem("MUSIC VOLUME", Menu.EditType.EditMultiValue, new MultiValue(volume, MenuValue.Type.FLOAT, musicPlayer.getGain())));
        optionsMenuPairs.add(new MenuItem("SOUND VOLUME", Menu.EditType.EditMultiValue, new MultiValue(volume, MenuValue.Type.FLOAT, soundFXPlayer.getGain())));

        optionsMenu = new OptionsMenu("OPTIONS", optionsMenuPairs, FONT_IMG, new Vector2f(0.0f, 0.5f), 2.0f) {
            @Override
            protected void leave() {
                mainMenu.open();
            }

            @Override
            protected void execute() {
                Command command;
                FutureTask<Object> task;
                switch (selected) {
                    case 0:
                        command = Command.getCommand(Command.Target.FPS_MAX);
                        command.getArgs().add(items.get(selected).menuValue.getCurrentValue());
                        command.setMode(Command.Mode.SET);
                        Command.execute(command);
                        break;
                    case 1:
                        command = Command.getCommand(Command.Target.RESOLUTION);
                        String giveCurrent = (String) items.get(selected).menuValue.getCurrentValue();
                        String things[] = giveCurrent.split("x");
                        command.getArgs().add(Integer.valueOf(things[0]));
                        command.getArgs().add(Integer.valueOf(things[1]));
                        command.setMode(Command.Mode.SET);
                        task = new FutureTask<>(command);
                        GameRenderer.TASK_QUEUE.add(task);
                        break;
                    case 2:
                        String fullscreen = (String) items.get(selected).menuValue.getCurrentValue();
                        command = Command.getCommand(Command.Target.FULLSCREEN);
                        switch (fullscreen) {
                            case "ON":
                                command.args.add(true);
                                break;
                            case "OFF":
                                command.args.add(false);
                                break;
                        }
                        command.setMode(Command.Mode.SET);
                        Command.execute(command);
                        break;
                    case 3:
                        String vsync = (String) items.get(selected).menuValue.getCurrentValue();
                        command = Command.getCommand(Command.Target.VSYNC);
                        switch (vsync) {
                            case "ON":
                                command.getArgs().add(true);
                                break;
                            case "OFF":
                                command.getArgs().add(false);
                                break;
                        }
                        command.setMode(Command.Mode.SET);
                        task = new FutureTask<>(command);
                        GameRenderer.TASK_QUEUE.add(task);
                        break;
                    case 4:
                        String waterEffects = (String) items.get(selected).menuValue.getCurrentValue();
                        command = Command.getCommand(Command.Target.WATER_EFFECTS);
                        switch (waterEffects) {
                            case "ON":
                                command.getArgs().add(true);
                                break;
                            case "OFF":
                                command.getArgs().add(false);
                                break;
                        }
                        command.setMode(Command.Mode.SET);
                        Command.execute(command);
                        break;
                    case 5:
                        float msens = (float) items.get(selected).menuValue.getCurrentValue();
                        command = Command.getCommand(Command.Target.MOUSE_SENSITIVITY);
                        command.getArgs().add(msens);
                        command.setMode(Command.Mode.SET);
                        Command.execute(command);
                        break;
                    case 6:
                        command = Command.getCommand(Command.Target.MUSIC_VOLUME);
                        command.getArgs().add(items.get(selected).menuValue.getCurrentValue());
                        command.setMode(Command.Mode.SET);
                        Command.execute(command);
                        break;
                    case 7:
                        command = Command.getCommand(Command.Target.SOUND_VOLUME);
                        command.getArgs().add(items.get(selected).menuValue.getCurrentValue());
                        command.setMode(Command.Mode.SET);
                        Command.execute(command);
                        break;
                }
            }
        };

        optionsMenu.setAlignmentAmount(Text.ALIGNMENT_RIGHT);

        List<MenuItem> editorMenuPairs = new ArrayList<>();
        editorMenuPairs.add(new MenuItem("START NEW LEVEL", Menu.EditType.EditNoValue, null));
        editorMenuPairs.add(new MenuItem("GENERATE RANDOM LEVEL", Menu.EditType.EditNoValue, null));
        editorMenuPairs.add(new MenuItem("SAVE LEVEL TO FILE", Menu.EditType.EditNoValue, null));
        editorMenuPairs.add(new MenuItem("LOAD LEVEL FROM FILE", Menu.EditType.EditNoValue, null));

        editorMenu = new Menu("EDITOR", editorMenuPairs, FONT_IMG, new Vector2f(0.0f, 0.5f), 2.0f) {
            @Override
            protected void leave() {
                mainMenu.open();
            }

            @Override
            protected void execute() {
                String s = editorMenu.items.get(editorMenu.getSelected()).keyText.content;
                switch (s) {
                    case "START NEW LEVEL":
                        progText.setEnabled(true);
                        GameObject.startNewLevel();
                        Game.setCurrentMode(Mode.EDITOR);
                        break;
                    case "GENERATE RANDOM LEVEL":
                        progText.setEnabled(true);
                        //randLvlDialog.open();
                        randLvlMenu.open();
                        break;
                    case "SAVE LEVEL TO FILE":
                        progText.setEnabled(true);
                        saveDialog.open();
                        break;
                    case "LOAD LEVEL FROM FILE":
                        progText.setEnabled(true);
                        loadLvlMenu.open();
                        break;
                }
            }
        };
        editorMenu.setAlignmentAmount(Text.ALIGNMENT_LEFT);

        List<MenuItem> creditsMenuPairs = new ArrayList<>();
        creditsMenuPairs.add(new MenuItem("Programmer", Menu.EditType.EditNoValue, null));
        creditsMenuPairs.add(new MenuItem("Alexander \"Ermac\" Stojanovich", Menu.EditType.EditNoValue, null));
        creditsMenuPairs.add(new MenuItem("Testers", Menu.EditType.EditNoValue, null));
        creditsMenuPairs.add(new MenuItem("Jesse \"13\" Collins", Menu.EditType.EditNoValue, null));
        creditsMenuPairs.add(new MenuItem("Edmund \"HellBlade64\" Alby", Menu.EditType.EditNoValue, null));
        creditsMenuPairs.add(new MenuItem("Art", Menu.EditType.EditNoValue, null));
        creditsMenuPairs.add(new MenuItem("Alexander \"Ermac\" Stojanovich", Menu.EditType.EditNoValue, null));
        creditsMenuPairs.add(new MenuItem("Music/FX", Menu.EditType.EditNoValue, null));
        creditsMenuPairs.add(new MenuItem("Jordan \"Erokia\" Powell", Menu.EditType.EditNoValue, null));

        creditsMenu = new Menu("CREDITS", creditsMenuPairs, FONT_IMG, new Vector2f(0.0f, 0.5f), 2.0f) {
            @Override
            protected void leave() {
                mainMenu.open();
            }

            @Override
            protected void execute() {

            }

        };

        int index = 0;
        for (MenuItem item : creditsMenu.items) {
            if (index == 3 || index < 3 && (index & 1) != 0 || index > 3 && (index & 1) == 0) {
                item.keyText.scale = 1.0f;
                item.keyText.setColor(Vector3fColors.WHITE);
                item.keyText.setBuffered(false);
            }
            index++;
        }
        creditsMenu.iterator.setEnabled(false);
        creditsMenu.setAlignmentAmount(Text.ALIGNMENT_CENTER);
    }

    public void setCollText(boolean mode) {
        if (mode) {
            collText.setContent("Collision!");
            collText.setColor(Vector3fColors.RED);
        } else {
            collText.setContent("No Collision");
            collText.setColor(Vector3fColors.GREEN);
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

    public void render(ShaderProgram ifcShaderProgram) {
        saveDialog.render(ifcShaderProgram);
        loadDialog.render(ifcShaderProgram);
        randLvlDialog.render(ifcShaderProgram);
        singlePlayerDialog.render(ifcShaderProgram);
        if (!updText.isBuffered()) {
            updText.bufferAll();
        }
        updText.render(ifcShaderProgram);
        if (!fpsText.isBuffered()) {
            fpsText.bufferAll();
        }
        fpsText.render(ifcShaderProgram);
        if (!posText.isBuffered()) {
            posText.bufferAll();
        }
        posText.render(ifcShaderProgram);
        if (!chunkText.isBuffered()) {
            chunkText.bufferAll();
        }
        chunkText.render(ifcShaderProgram);
        if (!collText.isBuffered()) {
            collText.bufferAll();
        }
        collText.render(ifcShaderProgram);
        if (!helpText.isBuffered()) {
            helpText.bufferAll();
        }
        helpText.render(ifcShaderProgram);
        if (!gameModeText.isBuffered()) {
            gameModeText.bufferAll();
        }
        gameModeText.render(ifcShaderProgram);
        if (!progText.isBuffered()) {
            progText.bufferAll();
        }
        progText.render(ifcShaderProgram);
        if (!screenText.isBuffered()) {
            screenText.bufferAll();
        }
        screenText.render(ifcShaderProgram);
        mainMenu.render(ifcShaderProgram);
        optionsMenu.render(ifcShaderProgram);
        editorMenu.render(ifcShaderProgram);
        creditsMenu.render(ifcShaderProgram);
        randLvlMenu.render(ifcShaderProgram);
        loadLvlMenu.render(ifcShaderProgram);

        if (!mainMenu.isEnabled() && !loadLvlMenu.isEnabled() && !optionsMenu.isEnabled() && !editorMenu.isEnabled()
                && !creditsMenu.isEnabled() && !randLvlMenu.isEnabled() && !showHelp) {
            if (!crosshair.isBuffered()) {
                crosshair.bufferAll();
            }
            crosshair.render(ifcShaderProgram);
        }
        console.render(ifcShaderProgram);
    }

    // update menu components
    public void update() {
        mainMenu.update();
        optionsMenu.update();
        editorMenu.update();
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

    public DynamicText getPosText() {
        return posText;
    }

    public DynamicText getChunkText() {
        return chunkText;
    }

    public Menu getCreditsMenu() {
        return creditsMenu;
    }

    public Menu getRandLvlMenu() {
        return randLvlMenu;
    }

    public int getNumBlocks() {
        return numBlocks;
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
