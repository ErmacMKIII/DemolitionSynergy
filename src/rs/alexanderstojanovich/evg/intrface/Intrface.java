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
import java.util.concurrent.FutureTask;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.audio.AudioPlayer;
import rs.alexanderstojanovich.evg.core.ShadowRenderer;
import rs.alexanderstojanovich.evg.core.WaterRenderer;
import rs.alexanderstojanovich.evg.critter.Player;
import rs.alexanderstojanovich.evg.level.Editor;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.main.Game.Mode;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.main.GameRenderer;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.GlobalColors;
import rs.alexanderstojanovich.evg.util.PlainTextReader;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Intrface {

    public final GameObject gameObject;
    private Quad crosshair;
    private DynamicText updText; // displays updates
    private DynamicText fpsText; // displays framerates
    private DynamicText posText; // display position
    private DynamicText viewText; // display view

    private DynamicText chunkText; // display current chunk (player)
    private DynamicText gameTimeText;

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

    public static final String FONT_IMG = "font.png"; // modified JetBrains font

    private OptionsMenu singlPlayerMenu;

    private final Console console;

    /**
     * Init interface with all GL components: text, dialogs & menus.
     *
     * @param gameObject game object
     */
    public Intrface(GameObject gameObject) {
        this.gameObject = gameObject;
        this.console = new Console(this);
        initSelf();
    }

    /**
     * Long Initialization. All components get their purpose now.
     */
    private void initSelf() {
        try {
            final float menuScale = 2.0f;

            AudioPlayer musicPlayer = gameObject.getMusicPlayer();
            AudioPlayer soundFXPlayer = gameObject.getSoundFXPlayer();

            updText = new DynamicText(Texture.FONT, "", GlobalColors.GREEN_RGBA, new Vector2f(-1.0f, 1.0f));
            updText.alignToNextChar(this);
            fpsText = new DynamicText(Texture.FONT, "", GlobalColors.GREEN_RGBA, new Vector2f(-1.0f, 0.85f));
            fpsText.alignToNextChar(this);

            posText = new DynamicText(Texture.FONT, "", GlobalColors.GREEN_RGBA, new Vector2f(1.0f, -1.0f));
            posText.setAlignment(Text.ALIGNMENT_RIGHT);
            posText.alignToNextChar(this);

            viewText = new DynamicText(Texture.FONT, "", GlobalColors.GREEN_RGBA, new Vector2f(1.0f, -0.85f));
            viewText.setAlignment(Text.ALIGNMENT_RIGHT);
            viewText.alignToNextChar(this);

            chunkText = new DynamicText(Texture.FONT, "", GlobalColors.CYAN_RGBA, new Vector2f(1.0f, -0.75f));
            chunkText.setAlignment(Text.ALIGNMENT_RIGHT);
            chunkText.alignToNextChar(this);

            collText = new DynamicText(Texture.FONT, "No Collision", GlobalColors.GREEN_RGBA, new Vector2f(-1.0f, -1.0f));
            collText.alignToNextChar(this);
            helpText = new DynamicText(Texture.FONT, PlainTextReader.readFromFile(Game.INTRFACE_ENTRY, "help.txt"), new Vector2f(-1.0f, 0.75f), 14, 14);
            helpText.alignToNextChar(this);
            helpText.setEnabled(false);
            progText = new DynamicText(Texture.FONT, "", GlobalColors.YELLOW_RGBA, new Vector2f(-1.0f, -0.9f));
            progText.alignToNextChar(this);
            screenText = new DynamicText(Texture.FONT, "", new Vector2f(-1.0f, -0.7f), 18, 18);
            screenText.alignToNextChar(this);
            gameModeText = new DynamicText(Texture.FONT, Game.getCurrentMode().name(), GlobalColors.GREEN_RGBA, new Vector2f(1.0f, 1.0f));
            gameModeText.setAlignment(Text.ALIGNMENT_RIGHT);
            gameModeText.alignToNextChar(this);

            gameTimeText = new DynamicText(Texture.FONT, "", GlobalColors.YELLOW_RGBA, new Vector2f(0.0f, 1.0f));
            gameTimeText.setAlignment(Text.ALIGNMENT_CENTER);
            gameTimeText.alignToNextChar(this);

            crosshair = new Quad(27, 27, Texture.CROSSHAIR, true); // it ignores resolution changes and doesn't scale
            crosshair.setColor(new Vector4f(GlobalColors.WHITE, 1.0f));
            IList<MenuItem> mainMenuItems = new GapList<>();
            mainMenuItems.add(new MenuItem("SINGLE PLAYER", Menu.EditType.EditNoValue, null));
            mainMenuItems.add(new MenuItem("MULTIPLAYER", Menu.EditType.EditNoValue, null));
            mainMenuItems.add(new MenuItem("EDITOR", Menu.EditType.EditNoValue, null));
            mainMenuItems.add(new MenuItem("OPTIONS", Menu.EditType.EditNoValue, null));
            mainMenuItems.add(new MenuItem("CREDITS", Menu.EditType.EditNoValue, null));
            mainMenuItems.add(new MenuItem("EXIT", Menu.EditType.EditNoValue, null));
            mainMenu = new Menu(this, "", mainMenuItems, FONT_IMG, new Vector2f(0.0f, 0.35f), menuScale) {
                @Override
                protected void leave() {

                }

                @Override
                protected void execute() {
                    String s = mainMenu.items.get(mainMenu.getSelected()).keyText.content;
                    switch (s) {
                        case "SINGLE PLAYER":
                            singlPlayerMenu.open();
                            break;
                        case "EDITOR":
                            editorMenu.open();
                            break;
                        case "OPTIONS":
                            optionsMenu.open();
                            viewText.setEnabled(false);
                            posText.setEnabled(false);
                            chunkText.setEnabled(false);
                            break;
                        case "CREDITS":
                            creditsMenu.open();
                            break;
                        case "EXIT":
                            gameObject.WINDOW.close();
                            break;
                    }
                }
            };
            Quad logo = new Quad(120, 90, Texture.LOGO);
            logo.setColor(new Vector4f(GlobalColors.YELLOW, 1.0f));
            logo.setScale(1.5f);
            mainMenu.setLogo(logo);
            mainMenu.setAlignmentAmount(Text.ALIGNMENT_CENTER);

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
            loadDialog.dialog.alignToNextChar(this);

            File currFile = new File("./");
            String[] datFileList = currFile.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".dat");
                }
            });

            IList<MenuItem> loadLvlMenuPairs = new GapList<>();
            for (String datFile : datFileList) {
                loadLvlMenuPairs.add(new MenuItem(datFile, Menu.EditType.EditNoValue, null));
            }

            loadLvlMenu = new Menu(this, "LOAD LEVEL", loadLvlMenuPairs, FONT_IMG, new Vector2f(0.0f, 0.5f), menuScale) {
                @Override
                protected void leave() {
                    editorMenu.open();
                }

                @Override
                protected void execute() {
                    String chosen = loadLvlMenu.items.get(loadLvlMenu.getSelected()).keyText.getContent();
                    gameObject.loadLevelFromFile(chosen);
                }
            };
            loadLvlMenu.setAlignmentAmount(Text.ALIGNMENT_LEFT);

            randLvlDialog = new ConcurrentDialog(Texture.FONT, new Vector2f(-0.95f, 0.65f),
                    "GENERATE RANDOM LEVEL\n(TIME-CONSUMING OPERATION) (Y/N)? ", "LEVEL GENERATED SUCESSFULLY!", "LEVEL GENERATION FAILED!") {
                @Override
                protected boolean execute(String command) {
                    boolean ok = false;
                    if (!gameObject.isWorking() && (command.equalsIgnoreCase("yes") || command.equalsIgnoreCase("y"))) {
                        Editor.deselect();
                        ok |= gameObject.generateRandomLevel(numBlocks);
                        if (ok) {
                            Game.setCurrentMode(Mode.EDITOR);
                        } else {
                            Game.setCurrentMode(Mode.FREE);
                        }
                    }
                    return ok;
                }
            };
            randLvlDialog.dialog.alignToNextChar(this);

            IList<MenuItem> randLvlMenuItems = new GapList<>();
            randLvlMenuItems.add(new MenuItem("SMALL  (25000  blocks)", Menu.EditType.EditNoValue, null));
            randLvlMenuItems.add(new MenuItem("MEDIUM (50000  blocks)", Menu.EditType.EditNoValue, null));
            randLvlMenuItems.add(new MenuItem("LARGE  (100000 blocks)", Menu.EditType.EditNoValue, null));
            randLvlMenuItems.add(new MenuItem("HUGE   (131070 blocks)", Menu.EditType.EditNoValue, null));
            randLvlMenuItems.add(new MenuItem("SEED  ", Menu.EditType.EditSingleValue, new SingleValue(gameObject.getRandomLevelGenerator().getSeed(), MenuValue.Type.LONG)));

            randLvlMenu = new OptionsMenu(this, "GENERATE RANDOM LEVEL", randLvlMenuItems, FONT_IMG, new Vector2f(0.0f, 0.5f), menuScale) {
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
                            gameObject.getRandomLevelGenerator().setSeed((long) selectedMenuItem.menuValue.getCurrentValue());
                            break;
                    }

                    if (numBlocks != 0 && selected != 4) {
                        randLvlDialog.open(Intrface.this);
                    }
                }

            };
            randLvlMenu.getItems().get(4).menuValue.getValueText().setScale(menuScale);

            singlePlayerDialog = new ConcurrentDialog(Texture.FONT, new Vector2f(-0.95f, 0.65f), "START NEW GAME (Y/N)? ", "OK!", "ERROR!") {
                @Override
                protected boolean execute(String command) {
                    boolean ok = false;
                    if (!gameObject.isWorking() && (command.equalsIgnoreCase("yes") || command.equalsIgnoreCase("y"))) {
                        Editor.deselect();
                        ok |= gameObject.generateSinglePlayerLevel(numBlocks);
                        if (ok) {
                            Game.setCurrentMode(Mode.SINGLE_PLAYER);
                        } else {
                            Game.setCurrentMode(Mode.FREE);
                        }
                    }

                    return ok;
                }
            };
            singlePlayerDialog.dialog.alignToNextChar(this);

            Object[] fpsCaps = {35, 60, 75, 100, 200, 300};
            Object[] resolutions = gameObject.WINDOW.giveAllResolutions();
            Object[] swtch = {"OFF", "ON"};
            Object[] swtchFX = {"NONE", "LOW", "MEDIUM", "HIGH", "ULTRA"};
            Object[] mouseSens = {1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f, 5.0f, 5.5f, 6.0f, 6.5f, 7.0f, 7.5f, 8.0f, 8.5f, 9.0f, 9.5f, 10.0f};
            Object[] volume = new Float[21];
            int k = 0;
            for (float i = 0.0f; i < 1.05f; i += 0.05f) {
                volume[k++] = Math.round(i * 100.0f) / 100.f; // rounding to two decimal places
            }

            IList<MenuItem> optionsMenuPairs = new GapList<>();
            optionsMenuPairs.add(new MenuItem("FPS CAP", Menu.EditType.EditMultiValue, new MultiValue(fpsCaps, MenuValue.Type.INT, String.valueOf(Game.getFpsMax()))));
            optionsMenuPairs.add(new MenuItem("RESOLUTION", Menu.EditType.EditMultiValue, new MultiValue(
                    resolutions,
                    MenuValue.Type.STRING,
                    String.valueOf(gameObject.WINDOW.getWidth()) + "x" + String.valueOf(gameObject.WINDOW.getHeight()))));
            optionsMenuPairs.add(new MenuItem("FULLSCREEN", Menu.EditType.EditMultiValue, new MultiValue(swtch, MenuValue.Type.STRING, gameObject.WINDOW.isFullscreen() ? "ON" : "OFF")));
            optionsMenuPairs.add(new MenuItem("VSYNC", Menu.EditType.EditMultiValue, new MultiValue(swtch, MenuValue.Type.STRING, gameObject.WINDOW.isVsync() ? "ON" : "OFF")));
            optionsMenuPairs.add(new MenuItem("WATER EFFECTS", Menu.EditType.EditMultiValue, new MultiValue(swtchFX, MenuValue.Type.STRING, gameObject.waterRenderer.getEffectsQuality().toString())));
            optionsMenuPairs.add(new MenuItem("SHADOW EFFECTS", Menu.EditType.EditMultiValue, new MultiValue(swtchFX, MenuValue.Type.STRING, gameObject.shadowRenderer.getEffectsQuality().toString())));
            optionsMenuPairs.add(new MenuItem("MOUSE SENSITIVITY", Menu.EditType.EditMultiValue, new MultiValue(mouseSens, MenuValue.Type.FLOAT, Game.getMouseSensitivity())));
            optionsMenuPairs.add(new MenuItem("MUSIC VOLUME", Menu.EditType.EditMultiValue, new MultiValue(volume, MenuValue.Type.FLOAT, musicPlayer.getGain())));
            optionsMenuPairs.add(new MenuItem("SOUND VOLUME", Menu.EditType.EditMultiValue, new MultiValue(volume, MenuValue.Type.FLOAT, soundFXPlayer.getGain())));

            optionsMenu = new OptionsMenu(this, "OPTIONS", optionsMenuPairs, FONT_IMG, new Vector2f(0.0f, 0.5f), menuScale) {
                @Override
                protected void leave() {
                    mainMenu.open();
                    viewText.setEnabled(true);
                    posText.setEnabled(true);
                    chunkText.setEnabled(true);
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
                            Command.execute(gameObject, command);
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
                            Command.execute(gameObject, command);
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
                            String waterFX = WaterRenderer.WaterEffectsQuality.valueOf(waterEffects).toString();
                            command.getArgs().add(waterFX);
                            command.setMode(Command.Mode.SET);
                            Command.execute(gameObject, command);
                            break;
                        case 5:
                            String shadowEffects = (String) items.get(selected).menuValue.getCurrentValue();
                            command = Command.getCommand(Command.Target.SHADOW_EFFECTS);
                            String shadoFX = ShadowRenderer.ShadowEffectsQuality.valueOf(shadowEffects).toString();
                            command.getArgs().add(shadoFX);
                            command.setMode(Command.Mode.SET);
                            Command.execute(gameObject, command);
                            break;
                        case 6:
                            float msens = (float) items.get(selected).menuValue.getCurrentValue();
                            command = Command.getCommand(Command.Target.MOUSE_SENSITIVITY);
                            command.getArgs().add(msens);
                            command.setMode(Command.Mode.SET);
                            Command.execute(gameObject, command);
                            break;
                        case 7:
                            command = Command.getCommand(Command.Target.MUSIC_VOLUME);
                            command.getArgs().add(items.get(selected).menuValue.getCurrentValue());
                            command.setMode(Command.Mode.SET);
                            Command.execute(gameObject, command);
                            break;
                        case 8:
                            command = Command.getCommand(Command.Target.SOUND_VOLUME);
                            command.getArgs().add(items.get(selected).menuValue.getCurrentValue());
                            command.setMode(Command.Mode.SET);
                            Command.execute(gameObject, command);
                            break;
                    }
                }
            };

            optionsMenu.setAlignmentAmount(Text.ALIGNMENT_RIGHT);

            IList<MenuItem> editorMenuPairs = new GapList<>();
            editorMenuPairs.add(new MenuItem("START NEW LEVEL", Menu.EditType.EditNoValue, null));
            editorMenuPairs.add(new MenuItem("GENERATE RANDOM LEVEL", Menu.EditType.EditNoValue, null));
            editorMenuPairs.add(new MenuItem("SAVE LEVEL TO FILE", Menu.EditType.EditNoValue, null));
            editorMenuPairs.add(new MenuItem("LOAD LEVEL FROM FILE", Menu.EditType.EditNoValue, null));

            editorMenu = new Menu(this, "EDITOR", editorMenuPairs, FONT_IMG, new Vector2f(0.0f, 0.5f), menuScale) {
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
                            gameObject.startNewLevel();
                            Game.setCurrentMode(Mode.EDITOR);
                            break;
                        case "GENERATE RANDOM LEVEL":
                            progText.setEnabled(true);
                            //randLvlDialog.open();
                            randLvlMenu.open();
                            break;
                        case "SAVE LEVEL TO FILE":
                            progText.setEnabled(true);
                            saveDialog.open(Intrface.this);
                            break;
                        case "LOAD LEVEL FROM FILE":
                            progText.setEnabled(true);
                            loadLvlMenu.open();
                            break;
                    }
                }
            };
            editorMenu.setAlignmentAmount(Text.ALIGNMENT_LEFT);

            IList<MenuItem> creditsMenuPairs = new GapList<>();
            creditsMenuPairs.add(new MenuItem("Programmer", Menu.EditType.EditNoValue, null));
            creditsMenuPairs.add(new MenuItem("Alexander \"Ermac\" Stojanovich", Menu.EditType.EditNoValue, null));
            creditsMenuPairs.add(new MenuItem("Testers", Menu.EditType.EditNoValue, null));
            creditsMenuPairs.add(new MenuItem("Jesse \"13\" Collins", Menu.EditType.EditNoValue, null));
            creditsMenuPairs.add(new MenuItem("\n", Menu.EditType.EditNoValue, null));
            creditsMenuPairs.add(new MenuItem("Art", Menu.EditType.EditNoValue, null));
            creditsMenuPairs.add(new MenuItem("Alexander \"Ermac\" Stojanovich", Menu.EditType.EditNoValue, null));
            creditsMenuPairs.add(new MenuItem("Music/FX", Menu.EditType.EditNoValue, null));
            creditsMenuPairs.add(new MenuItem("Jordan \"Erokia\" Powell", Menu.EditType.EditNoValue, null));

            creditsMenu = new Menu(this, "CREDITS", creditsMenuPairs, FONT_IMG, new Vector2f(0.0f, 0.5f), menuScale) {
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
                    item.keyText.setColor(new Vector4f(GlobalColors.WHITE, 1.0f));
                    item.keyText.setBuffered(false);
                }
                index++;
            }
            creditsMenu.iterator.setEnabled(false);
            creditsMenu.setAlignmentAmount(Text.ALIGNMENT_CENTER);

            IList<MenuItem> singlPlayerMenuItems = new GapList<>();
            singlPlayerMenuItems.add(new MenuItem("CHARACTER MODEL", Menu.EditType.EditMultiValue, new MultiValue(new String[]{"ALEX", "STEVE"}, MenuValue.Type.STRING, "ALEX")));
            singlPlayerMenuItems.add(new MenuItem("COLOR", Menu.EditType.EditMultiValue, new MultiValue(GlobalColors.ColorName.names(), MenuValue.Type.STRING, GlobalColors.ColorName.WHITE.name())));
            singlPlayerMenuItems.add(new MenuItem("LEVEL SIZE", Menu.EditType.EditMultiValue, new MultiValue(new String[]{"SMALL", "MEDIUM", "LARGE", "HUGE"}, MenuValue.Type.STRING, "SMALL")));
            singlPlayerMenuItems.add(new MenuItem("SEED", Menu.EditType.EditSingleValue, new SingleValue(gameObject.getRandomLevelGenerator().getSeed(), MenuValue.Type.LONG)));
            singlPlayerMenuItems.add(new MenuItem("PLAY", Menu.EditType.EditNoValue, null));

            singlPlayerMenu = new OptionsMenu(this, "SINGLE PLAYER", singlPlayerMenuItems, FONT_IMG, new Vector2f(0.0f, 0.5f), menuScale) {
                @Override
                protected void leave() {
                    mainMenu.open();
                }

                @Override
                protected void execute() {
                    // set player character model & color
                    if (singlPlayerMenu.selected == 1) {
                        singlPlayerMenu.items.get(1).menuValue.getValueText().color = new Vector4f(GlobalColors.getRGBColorOrDefault(singlPlayerMenu.items.get(1).menuValue.getCurrentValue().toString().toUpperCase()), 1.0f);
                    }
                    if (singlPlayerMenu.selected == 4) {
                        Player player = gameObject.getLevelContainer().levelActors.player;
                        player.body.texName = singlPlayerMenu.items.getFirst().menuValue.getCurrentValue().toString().toLowerCase();
                        player.body.setPrimaryRGBAColor(new Vector4f(GlobalColors.getRGBColorOrDefault(singlPlayerMenu.items.get(1).menuValue.getCurrentValue().toString().toUpperCase()), 1.0f));
                        // set level size & seed
                        String levelSize = singlPlayerMenu.items.get(2).menuValue.getCurrentValue().toString().toUpperCase();
                        switch (levelSize) {
                            default:
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
                        }
                        long seedValue = Long.parseLong(singlPlayerMenu.items.get(3).menuValue.getCurrentValue().toString());
                        gameObject.getRandomLevelGenerator().setSeed(seedValue);
                        singlePlayerDialog.open(Intrface.this);
                    }

                }

            };
            singlPlayerMenu.items.getLast().keyText.color = new Vector4f(GlobalColors.CYAN, 1.0f);
            singlPlayerMenu.alignmentAmount = Text.ALIGNMENT_RIGHT; // the best for options menu
            DSLogger.reportDebug("Interface initialized.", null);
        } catch (Exception ex) {
            DSLogger.reportError("Unable to initialize interface! => " + ex.getMessage(), ex);
        }
    }

    /**
     * Set collision text w/ color according to collision mode.
     *
     * @param mode true/false for collision
     */
    public void setCollText(boolean mode) {
        if (mode) {
            collText.setContent("Collision!");
            collText.setColor(GlobalColors.RED_RGBA);
        } else {
            collText.setContent("No Collision");
            collText.setColor(GlobalColors.GREEN_RGBA);
        }
    }

    /**
     * Toggle ingame help, help is displayed as a long text.
     */
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

    /**
     * Render all the components of the interface
     *
     * @param ifcShaderProgram Interface shader program (default - use that
     * one).
     * @param ifcContShaderProgram Interface contour shader program
     * (animated-through - use that one).
     */
    public void render(ShaderProgram ifcShaderProgram, ShaderProgram ifcContShaderProgram) {
        saveDialog.render(this, ifcShaderProgram);
        loadDialog.render(this, ifcShaderProgram);
        randLvlDialog.render(this, ifcShaderProgram);
        singlePlayerDialog.render(this, ifcShaderProgram);
        if (!updText.isBuffered()) {
            updText.bufferSmart(this);
        }
        updText.render(this, ifcShaderProgram);
        if (!fpsText.isBuffered()) {
            fpsText.bufferSmart(this);
        }
        fpsText.render(this, ifcShaderProgram);
        if (!posText.isBuffered()) {
            posText.bufferSmart(this);
        }
        posText.render(this, ifcShaderProgram);
        if (!chunkText.isBuffered()) {
            chunkText.bufferSmart(this);
        }
        chunkText.render(this, ifcShaderProgram);
        if (!viewText.isBuffered()) {
            viewText.bufferSmart(this);
        }
        viewText.render(this, ifcShaderProgram);
        if (!collText.isBuffered()) {
            collText.bufferSmart(this);
        }
        collText.render(this, ifcShaderProgram);
        if (!helpText.isBuffered()) {
            helpText.bufferSmart(this);
        }
        helpText.render(this, ifcShaderProgram);
        if (!gameModeText.isBuffered()) {
            gameModeText.bufferSmart(this);
        }
        gameModeText.render(this, ifcShaderProgram);
        if (!progText.isBuffered()) {
            progText.bufferSmart(this);
        }
        progText.render(this, ifcShaderProgram);
        if (!screenText.isBuffered()) {
            screenText.bufferSmart(this);
        }

        if (!gameTimeText.isBuffered()) {
            gameTimeText.bufferSmart(this);
        }
        gameTimeText.render(this, ifcShaderProgram);

        screenText.render(this, ifcShaderProgram);
        mainMenu.render(this, ifcShaderProgram);
        optionsMenu.render(this, ifcShaderProgram);
        editorMenu.render(this, ifcShaderProgram);
        creditsMenu.render(this, ifcShaderProgram);
        randLvlMenu.render(this, ifcShaderProgram);
        loadLvlMenu.render(this, ifcShaderProgram);
        singlPlayerMenu.render(this, ifcShaderProgram);

        if (!mainMenu.isEnabled() && !loadLvlMenu.isEnabled() && !optionsMenu.isEnabled() && !editorMenu.isEnabled()
                && !creditsMenu.isEnabled() && !randLvlMenu.isEnabled() && !showHelp) {
            if (!crosshair.isBuffered()) {
                crosshair.bufferAll(this);
            }
            crosshair.render(this, ifcShaderProgram);
        }
        console.render(this, ifcShaderProgram, ifcContShaderProgram);
    }

    /**
     * Capturing mouse input for menus
     */
    public void update() { // update menu components
        mainMenu.update();
        optionsMenu.update();
        editorMenu.update();
        randLvlMenu.update();
        loadLvlMenu.update();
        singlPlayerMenu.update();
    }

    /**
     * Cleanup all callbacks
     */
    public void cleanUp() {
        mainMenu.cleanUp();
        optionsMenu.cleanUp();
        editorMenu.cleanUp();

        randLvlMenu.cleanUp();
        loadLvlMenu.cleanUp();
        singlPlayerMenu.cleanUp();

        creditsMenu.cleanUp();

        saveDialog.cleanUp();
        loadDialog.cleanUp();
        randLvlDialog.cleanUp();
        singlePlayerDialog.cleanUp();

        console.cleanUp();

        DSLogger.reportDebug("Interface cleaned up.", null);
    }

    /*
    * Deletes all GL Buffers. Call from Renderer thread
     */
    public void release() {
        mainMenu.release();
        optionsMenu.release();
        editorMenu.release();

        randLvlMenu.release();
        loadLvlMenu.release();

        creditsMenu.release();

        updText.release();
        fpsText.release();
        viewText.release();
        posText.release();
        chunkText.release();

        collText.release();
        helpText.release();
        progText.release();
        screenText.release();
        gameModeText.release();

        saveDialog.release();
        loadDialog.release();
        randLvlDialog.release();
        singlePlayerDialog.release();

        console.release();

        DSLogger.reportDebug("Interface buffers deleted.", null);
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

    public DynamicText getGameTimeText() {
        return gameTimeText;
    }

    public DynamicText getViewText() {
        return viewText;
    }

}
