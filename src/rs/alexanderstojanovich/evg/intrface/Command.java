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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.cache.CacheModule;
import rs.alexanderstojanovich.evg.cache.CachedInfo;
import rs.alexanderstojanovich.evg.chunk.Chunk;
import rs.alexanderstojanovich.evg.core.ShadowRenderer;
import rs.alexanderstojanovich.evg.core.WaterRenderer;
import rs.alexanderstojanovich.evg.critter.Critter;
import rs.alexanderstojanovich.evg.critter.Observer;
import rs.alexanderstojanovich.evg.critter.Player;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.main.GameRenderer;
import rs.alexanderstojanovich.evg.main.GameServer;
import rs.alexanderstojanovich.evg.net.ClientInfo;
import rs.alexanderstojanovich.evg.net.DSObject;
import rs.alexanderstojanovich.evg.net.Request;
import rs.alexanderstojanovich.evg.net.RequestIfc;
import rs.alexanderstojanovich.evg.net.Response;
import rs.alexanderstojanovich.evg.net.ResponseIfc;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.GlobalColors;
import rs.alexanderstojanovich.evg.util.Trie;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Command implements Callable<Object> {
// its not actually a thread but its used for remote execution (from Executor)

    /**
     * Command mnemonic (syntax)
     */
    public static enum Target {
        MONITOR_GET,
        MONITOR_ID,
        GAME_TICKS,
        FPS_MAX,
        RESOLUTION,
        FULLSCREEN,
        VSYNC,
        WATER_EFFECTS,
        SHADOW_EFFECTS,
        MOUSE_SENSITIVITY,
        MUSIC_VOLUME,
        SOUND_VOLUME,
        EXIT,
        POSITION,
        SCREENSHOT,
        SIZEOF,
        CACHE,
        CLEAR,
        NOP,
        PING,
        PRINT,
        SAY,
        CONNECT,
        DISCONNECT,
        START_SERVER,
        STOP_SERVER,
        KICK_PLAYER,
        NAME,
        MODEL,
        COLOR,
        ERROR
    };

    /**
     * No operation command.
     */
    protected Target target = Target.NOP;

    /**
     * Command mode 'GET' gets the value, command mode 'SET' set the value
     */
    public static enum Mode {
        GET, SET
    };
    protected Mode mode = Mode.GET;

    /**
     * Command status (console light bulb color)
     */
    public static enum Status {
        /**
         * Command initial status
         */
        INIT,
        /**
         * Command pending execution finished
         */
        PENDING,
        /**
         * Command finished execution successfully
         */
        SUCCEEDED,
        /**
         * Command execution resulted in warning
         */
        WARNING,
        /**
         * Command execution resulted in failure
         */
        FAILED
    }

    protected Object result = null;
    protected Status status = Status.INIT;

    // commands differ in arugment length and type, therefore list is used
    protected final IList<Object> args = new GapList<>();

    protected static final Trie trie = new Trie();

    protected String input;

    protected Command(String input) {
        this.input = input;
    }

    protected Command(Target target) {
        this.target = target;
    }

    static {
        for (Target target : Target.values()) {
            if (target != Target.ERROR && target != Target.NOP) {
                trie.insert(target.name().toLowerCase());
            }
        }
    }

    /**
     * Perform auto complete with list of string for given input
     *
     * @param input given input
     * @return possible commands
     */
    public static List<String> autoComplete(String input) {
        List<String> words = trie.autoComplete(input);
        Collections.sort(words);

        return words;
    }

    /**
     * Constructs command from given target
     *
     * @param target give target to construct command
     * @return Command with empty args.
     */
    public static Command getCommand(Target target) {
        return new Command(target);
    }

    /**
     * Constructs command from given string input
     *
     * @param input given input
     * @return Command with empty args.
     */
    public static Command getCommand(String input) {
        Command command = new Command(input);
        command.args.clear();
        String[] things = input.split("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (int i = 0; i < things.length; i++) {
            things[i] = things[i].replaceAll("^\"|\"$", "");
        }
        if (things.length > 0) {
            switch (things[0].toLowerCase()) {
                case "monitor_get":
                case "monitorget":
                    command.target = Target.MONITOR_GET;
                    break;
                case "monitorid":
                case "monitor_id":
                    command.target = Target.MONITOR_ID;
                    break;
                case "game_ticks":
                case "gameticks":
                    command.target = Target.GAME_TICKS;
                    if (things.length == 2) {
                        command.args.add(Float.valueOf(things[1]));
                    }
                    break;
                case "fps_max":
                case "fpsmax":
                    command.target = Target.FPS_MAX;
                    if (things.length == 2) {
                        command.args.add(Integer.valueOf(things[1]));
                    }
                    break;
                case "resolution":
                case "res":
                    command.target = Target.RESOLUTION;
                    if (things.length == 3) {
                        command.args.add(Integer.valueOf(things[1]));
                        command.args.add(Integer.valueOf(things[2]));
                    }
                    break;
                case "fullscreen":
                    command.target = Target.FULLSCREEN;
                    break;
//                case "windowed":
//                    command.target = Target.WINDOWED;
//                    break;
                case "v_sync":
                case "vsync":
                    command.target = Target.VSYNC;
                    if (things.length == 2) {
                        command.args.add(Boolean.valueOf(things[1]));
                    }
                    break;
                case "waterEffects":
                case "water_effects":
                    command.target = Target.WATER_EFFECTS;
                    if (things[1].matches("[0-9]+")) {
                        command.args.add(WaterRenderer.WaterEffectsQuality.values()[Integer.parseInt(things[1])]);
                    } else {
                        command.args.add(WaterRenderer.WaterEffectsQuality.valueOf(things[1].toUpperCase()).name());
                    }
                    break;
                case "shadowEffects":
                case "shadow_effects":
                    command.target = Target.SHADOW_EFFECTS;
                    if (things.length == 2) {
                        if (things[1].matches("[0-9]+")) {
                            command.args.add(ShadowRenderer.ShadowEffectsQuality.values()[Integer.parseInt(things[1])]);
                        } else {
                            command.args.add(ShadowRenderer.ShadowEffectsQuality.valueOf(things[1].toUpperCase()).name());
                        }
                    }
                    break;
                case "msens":
                case "mouse_sensitivity":
                    command.target = Target.MOUSE_SENSITIVITY;
                    if (things.length == 2) {
                        command.args.add(Float.valueOf(things[1]));
                    }
                    break;
                case "music_volume":
                case "musicVolume":
                    command.target = Target.MUSIC_VOLUME;
                    if (things.length == 2) {
                        command.args.add(Float.valueOf(things[1]));
                    }
                    break;
                case "sound_volume":
                case "soundVolume":
                    command.target = Target.SOUND_VOLUME;
                    if (things.length == 2) {
                        command.args.add(Float.valueOf(things[1]));
                    }
                    break;
                case "quit":
                case "exit":
                    command.target = Target.EXIT;
                    break;
                case "screenshot":
                    command.target = Target.SCREENSHOT;
                    break;
                case "pos":
                case "position":
                    command.target = Target.POSITION;
                    if (things.length == 2) {
                        command.args.add(Integer.valueOf(things[1]));
                    }
                    if (things.length == 4) {
                        command.args.add(Float.valueOf(things[1]));
                        command.args.add(Float.valueOf(things[2]));
                        command.args.add(Float.valueOf(things[3]));
                    }
                    break;
                case "sizeof":
                case "size_of":
                    command.target = Target.SIZEOF;
                    if (things.length == 2) {
                        command.args.add(Integer.valueOf(things[1]));
                    }
                    break;
                case "cache":
                case "cacheinfo":
                    command.target = Target.CACHE;
                    break;
                case "clr":
                case "clear":
                    command.target = Target.CLEAR;
                    break;
                case "print": // Visual Basic
                case "echo": // PHP
                case "write": // C#
                case "log": // JS
                    command.target = Target.PRINT;
                    if (things.length == 2) {
                        command.args.add((String) things[1]);
                    }
                    break;
                case "say":
                    command.target = Target.SAY;
                    if (things.length == 2) {
                        command.args.add((String) things[1]);
                    }
                    break;
                case "ping":
                    command.target = Target.PING;
                    break;
                case "connect":
                    command.target = Target.CONNECT;
                    if (things.length == 2) {
                        command.args.add((String) things[1]);
                    }
                    break;
                case "disconnect":
                    command.target = Target.DISCONNECT;
                    break;
                case "startserver":
                    command.target = Target.START_SERVER;
                    if (things.length == 2) {
                        command.args.add((String) things[1]);
                    } else if (things.length == 3) {
                        command.args.add((String) things[1]);
                        command.args.add((String) things[2]);
                    }
                    break;
                case "stopserver":
                    command.target = Target.STOP_SERVER;
                    break;
                case "kick":
                case "kickplayer":
                case "kick_player":
                    command.target = Target.KICK_PLAYER;
                    command.args.addAll(Arrays.asList(things).subList(1, things.length));
                    break;
                case "name":
                    command.target = Target.NAME;
                    if (things.length == 2) {
                        command.args.add((String) things[1]);
                    }
                    break;
                case "model":
                    command.target = Target.MODEL;
                    if (things.length == 2) {
                        command.args.add((String) things[1]);
                    }
                    break;
                case "color":
                case "colour":
                    command.target = Target.COLOR;
                    if (things.length == 2) {
                        command.args.add((String) things[1]);
                    } else if (things.length == 4) {
                        command.args.add((String) things[1]);
                        command.args.add((String) things[2]);
                        command.args.add((String) things[3]);
                    }
                    break;
                default:
                    command.target = Target.ERROR;
                    break;
            }
        }

        boolean argsEmpty = command.args.isEmpty();
        boolean isGetOnly = command.target == Target.SIZEOF;
        boolean isSetOnly = command.target == Target.CLEAR || command.target == Target.PRINT || command.target == Target.SAY
                || command.target == Target.CONNECT || command.target == Target.DISCONNECT || command.target == Target.START_SERVER || command.target == Target.STOP_SERVER || command.target == Target.KICK_PLAYER;

        command.mode = Mode.GET;

        if (isGetOnly) {
            command.mode = Mode.GET;
        }

        if (isSetOnly) {
            command.mode = Mode.SET;
        }

        if (!isGetOnly && !isSetOnly && !argsEmpty) {
            command.mode = Mode.SET;
        }

        return command;
    }

    /**
     * Executes command which modifies game, renderer or game object Rule is
     * that commands which directly affect window or OpenGL are being called
     * from the GameRenderer, whilst other can be called from the main method
     *
     * @param gameObject gameObject instance
     * @param command chosen command
     * @return execution status (true if successful, otherwise false)
     */
    public static Object execute(GameObject gameObject, Command command) {
        Object result = null;
        // reflect updates (SET) command
        final OptionsMenu optionsMenu = gameObject.intrface.getOptionsMenu();

        // reflect singleplayer updates (SET) command
        final OptionsMenu singleplayerMenu = gameObject.intrface.getSinglPlayerMenu();

        // reflect multiplayer updates (SET) command
        final OptionsMenu multiplayerMenu = gameObject.intrface.getMultiPlayerMenu();

        command.status = Status.PENDING;
        switch (command.target) {
            case MONITOR_ID:
                switch (command.mode) {
                    case GET:
                        result = Long.toHexString(gameObject.WINDOW.getMonitorID());
                        command.status = Status.SUCCEEDED;
                        break;
                }
                break;
            case MONITOR_GET:
                switch (command.mode) {
                    case GET:
                        StringBuilder sb = new StringBuilder();
                        gameObject.WINDOW.getMonitors().forEach(monitor -> sb.append(Long.toHexString(monitor)).append(","));
                        sb.setLength(sb.length() - 1);
                        result = sb.toString();
                        command.status = Status.SUCCEEDED;
                        break;
                }
                break;
            case GAME_TICKS:
                switch (command.mode) {
                    case GET:
                        result = Game.getGameTicks();
                        command.status = Status.SUCCEEDED;
                        break;
                    case SET:
                        float ticks = (float) command.args.get(0);
                        if (ticks >= 0.0f) {
                            Game.setGameTicks(ticks);
//                            GameRenderer.setFps(0);
                            GameRenderer.setFpsTicks(0.0f);
                            GameRenderer.setAnimationTimer((int) ticks);
                            GameRenderer.setGlCommandTimer((int) ticks);
                            command.status = Status.SUCCEEDED;
                        }
                        break;
                }
                break;
            case FPS_MAX:
                switch (command.mode) {
                    case GET:
                        result = Game.getFpsMax();
                        command.status = Status.SUCCEEDED;
                        break;
                    case SET:
                        int fpsMax = (int) command.args.get(0);
                        if (fpsMax > 0 && fpsMax <= 1E6) {
                            GameRenderer.setFps(0);
                            GameRenderer.setFpsTicks(0.0);
                            GameRenderer.setAnimationTimer(0);
                            Game.setFpsMax(fpsMax);
                            optionsMenu.items.getIf(x -> x.keyText.getContent().equals("FPS CAP")).getMenuValue().setCurrentValue(fpsMax);
                            command.status = Status.SUCCEEDED;
                        }

                        break;

                }
                break;
            case RESOLUTION:
                switch (command.mode) {
                    case GET:
                        result = gameObject.WINDOW.getWidth() + "x" + gameObject.WINDOW.getHeight();
                        command.status = Status.SUCCEEDED;
                        break;
                    case SET:
                        // changing resolution if necessary
                        int width = (int) command.args.get(0);
                        int height = (int) command.args.get(1);
                        boolean status = gameObject.WINDOW.setResolution(width, height);
                        if (status) {
                            gameObject.masterRenderer.setResolution(width, height);
                            gameObject.perspectiveRenderer.updatePerspective();
                            optionsMenu.items.getIf(x -> x.keyText.getContent().equals("RESOLUTION")).getMenuValue().setCurrentValue(width + "x" + height);
                            command.status = Status.SUCCEEDED;
                        } else {
                            command.status = Status.FAILED;
                        }
                        gameObject.WINDOW.centerTheWindow();
                        break;
                }

                break;
            case FULLSCREEN:
                switch (command.mode) {
                    case GET:
                        result = gameObject.WINDOW.isFullscreen();
                        command.status = Status.SUCCEEDED;
                        break;
                    case SET:
                        boolean bool = (boolean) command.args.get(0);
                        gameObject.WINDOW.setFullscreen(bool);
                        gameObject.WINDOW.centerTheWindow();
                        optionsMenu.items.getIf(x -> x.keyText.getContent().equals("FULLSCREEN")).getMenuValue().setCurrentValue(bool ? "ON" : "OFF");
                        break;
                }
                break;
//            case WINDOWED:
//                GameObject.WINDOW.windowed();
//                GameObject.WINDOW.centerTheWindow();
//                command.status = Status.SUCCEEDED;
//                break;
            case VSYNC: // OpenGL
                switch (command.mode) {
                    case GET:
                        result = gameObject.WINDOW.isVsync();
                        command.status = Status.SUCCEEDED;
                        break;
                    case SET:
                        boolean bool = (boolean) command.args.get(0);
                        gameObject.WINDOW.setVSync(bool);
                        optionsMenu.items.getIf(x -> x.keyText.getContent().equals("VSYNC")).getMenuValue().setCurrentValue(bool ? "ON" : "OFF");
                        command.status = Status.SUCCEEDED;
                        break;
                }
                break;
            case WATER_EFFECTS:
                switch (command.mode) {
                    case GET:
                        result = gameObject.waterRenderer.getEffectsQuality();
                        command.status = Status.SUCCEEDED;
                        break;
                    case SET:
                        if (command.args.get(0).toString().matches("[0-9]+")) {
                            gameObject.waterRenderer.setEffectsQuality(WaterRenderer.WaterEffectsQuality.values()[Integer.parseInt(command.args.get(0).toString())]);
                            optionsMenu.items.getIf(x -> x.keyText.getContent().equals("WATER EFFECTS")).getMenuValue().setCurrentValue(WaterRenderer.WaterEffectsQuality.values()[Integer.parseInt(command.args.get(0).toString())].toString());
                        } else {
                            gameObject.waterRenderer.setEffectsQuality(WaterRenderer.WaterEffectsQuality.valueOf(command.args.get(0).toString()));
                            optionsMenu.items.getIf(x -> x.keyText.getContent().equals("WATER EFFECTS")).getMenuValue().setCurrentValue(WaterRenderer.WaterEffectsQuality.valueOf(command.args.get(0).toString()).toString());
                        }
                        command.status = Status.SUCCEEDED;
                        break;
                }
                break;
            case SHADOW_EFFECTS:
                switch (command.mode) {
                    case GET:
                        result = gameObject.shadowRenderer.getEffectsQuality();
                        command.status = Status.SUCCEEDED;
                        break;
                    case SET:
                        if (command.args.get(0).toString().matches("[0-9]+")) {
                            gameObject.shadowRenderer.setEffectsQuality(ShadowRenderer.ShadowEffectsQuality.values()[Integer.parseInt(command.args.get(0).toString())]);
                            optionsMenu.items.getIf(x -> x.keyText.getContent().equals("SHADOW EFFECTS")).getMenuValue().setCurrentValue(ShadowRenderer.ShadowEffectsQuality.values()[Integer.parseInt(command.args.get(0).toString())].toString());
                        } else {
                            gameObject.shadowRenderer.setEffectsQuality(ShadowRenderer.ShadowEffectsQuality.valueOf(command.args.get(0).toString()));
                            optionsMenu.items.getIf(x -> x.keyText.getContent().equals("SHADOW EFFECTS")).getMenuValue().setCurrentValue(ShadowRenderer.ShadowEffectsQuality.valueOf(command.args.get(0).toString()).toString());
                        }
                        command.status = Status.SUCCEEDED;
                        break;
                }
                break;
            case MOUSE_SENSITIVITY:
                switch (command.mode) {
                    case GET:
                        result = Game.getMouseSensitivity();
                        command.status = Status.SUCCEEDED;
                        break;
                    case SET:
                        float msens = (float) command.args.get(0);
                        if (msens >= 0.0f && msens <= 100.0f) {
                            Game.setMouseSensitivity(msens);
                            optionsMenu.items.getIf(x -> x.keyText.getContent().equals("MOUSE SENSITIVITY")).getMenuValue().setCurrentValue(msens);
                            command.status = Status.SUCCEEDED;
                        }
                        break;
                }
                break;
            case MUSIC_VOLUME:
                switch (command.mode) {
                    case GET:
                        result = gameObject.getMusicPlayer().getGain();
                        command.status = Status.SUCCEEDED;
                        break;
                    case SET:
                        float music = (float) command.args.get(0);
                        if (music >= 0.0f && music <= 1.0f) {
                            gameObject.getMusicPlayer().setGain(music);
                            optionsMenu.items.getIf(x -> x.keyText.getContent().equals("MUSIC VOLUME")).getMenuValue().setCurrentValue(music);
                            command.status = Status.SUCCEEDED;
                        }
                        break;
                }
                break;
            case SOUND_VOLUME:
                switch (command.mode) {
                    case GET:
                        result = gameObject.getSoundFXPlayer().getGain();
                        command.status = Status.SUCCEEDED;
                        break;
                    case SET:
                        float sound = (float) command.args.get(0);
                        if (sound >= 0.0f && sound <= 1.0f) {
                            gameObject.getSoundFXPlayer().setGain(sound);
                            optionsMenu.items.getIf(x -> x.keyText.getContent().equals("SOUND VOLUME")).getMenuValue().setCurrentValue(sound);
                            command.status = Status.SUCCEEDED;
                        }
                        break;
                }
                break;
            case SCREENSHOT: // OpenGL
                File screenDir = new File(Game.SCREENSHOTS);
                if (!screenDir.isDirectory() && !screenDir.exists()) {
                    screenDir.mkdir();
                }
                LocalDateTime now = LocalDateTime.now();
                File screenshot = new File(Game.SCREENSHOTS + File.separator
                        + "dsynergy-" + now.getYear()
                        + "-" + now.getMonthValue()
                        + "-" + now.getDayOfMonth()
                        + "_" + now.getHour()
                        + "-" + now.getMinute()
                        + "-" + now.getSecond()
                        + "-" + Math.round(now.getNano() / (float) 1E6) // one million
                        + ".png");
                if (screenshot.exists()) {
                    screenshot.delete();
                }
                try {
                    ImageIO.write(gameObject.WINDOW.getScreenshot(), "PNG", screenshot);
                } catch (IOException ex) {
                    DSLogger.reportError(ex.getMessage(), ex);
                }
                gameObject.getIntrface().getScreenText().setEnabled(true);
                gameObject.getIntrface().getScreenText().setContent("Screen saved to " + screenshot.getAbsolutePath());
                command.status = Status.SUCCEEDED;
                break;
            case EXIT:
                gameObject.game.disconnectFromServer();
                gameObject.WINDOW.close();
                command.status = Status.SUCCEEDED;
                break;
            case POSITION:
                Observer mainActor = gameObject.levelContainer.levelActors.mainActor();
                int chunkId;
                switch (command.mode) {
                    case GET:
                        final StringBuilder sb = new StringBuilder();
                        sb.append(String.format("pos: (%.1f,%.1f,%.1f)", mainActor.getPos().x, mainActor.getPos().y, mainActor.getPos().z));
                        chunkId = Chunk.chunkFunc(mainActor.getPos());
                        sb.append(" | ");
                        sb.append(String.format("chunkId: %d", chunkId));
                        result = sb.toString();
                        command.status = Status.SUCCEEDED;
                        break;
                    case SET:
                        if (command.args.size() == 1) {
                            chunkId = (int) command.args.get(0);
                            if (chunkId >= 0 && chunkId < Chunk.CHUNK_NUM) {
                                Vector3f newPos = Chunk.invChunkFunc(chunkId);
                                mainActor.setPos(newPos);
                                command.status = Status.SUCCEEDED;
                            } else {
                                command.status = Status.FAILED;
                                result = "Invalid ChunkId!";
                            }
                        } else if (command.args.size() == 3) {
                            float newPosx = (float) command.args.get(0);
                            float newPosy = (float) command.args.get(1);
                            float newPosz = (float) command.args.get(2);

                            Vector3f newPos = new Vector3f(newPosx, newPosy, newPosz);
                            chunkId = Chunk.chunkFunc(newPos);
                            if (chunkId >= 0 && chunkId < Chunk.CHUNK_NUM) {
                                mainActor.setPos(newPos);
                                command.status = Status.SUCCEEDED;
                            } else {
                                command.status = Status.FAILED;
                                result = "Invalid position (must be in chunk bounds)!";
                            }
                        }
                        break;
                }
                break;
            case SIZEOF:
                if (command.mode == Mode.GET) {
                    if (command.args.isEmpty()) {
                        int totalSize = LevelContainer.AllBlockMap.getPopulation();
                        int cachedSize = 0;
                        for (CachedInfo ci : CacheModule.CACHED_CHUNKS) {
                            cachedSize += ci.cachedSize;
                        }
                        result = String.format("TotalSize= %d | TotalChunks= %d\nCachedChunks= %d | CachedSize= %d",
                                totalSize, Chunk.CHUNK_NUM, CacheModule.CACHED_CHUNKS.size(), cachedSize);
                        command.status = Status.SUCCEEDED;
                    } else {
                        chunkId = (int) command.args.get(0);
                        boolean cached = CacheModule.isCached(chunkId);
                        int size = -1;
                        if (cached) {
                            size = CacheModule.cachedSize(chunkId);
                        } else {
                            size = LevelContainer.AllBlockMap.getPopulatedLocations(chunkId).size();
                        }

                        if (size != -1) {
                            result = String.format("Size= %d | Cached= %s", size, cached);
                            command.status = Status.SUCCEEDED;
                        } else {
                            result = "Chunk not exists!";
                            command.status = Status.FAILED;
                        }
                    }
                }
                break;
            case CACHE:
                StringBuilder sb = new StringBuilder();
                if (command.mode == Mode.GET) {
                    if (CacheModule.CACHED_CHUNKS.isEmpty()) {
                        sb.append("<empty>");
                    } else {
                        for (CachedInfo ci : CacheModule.CACHED_CHUNKS) {
                            sb.append(String.format("ChunkId= %d | BlockSize= %d| CachedSize= %d | FileName= %s",
                                    ci.chunkId, ci.blockSize, ci.cachedSize, ci.fileName));
                        }
                    }
                }
                result = sb.toString();
                command.status = Status.SUCCEEDED;
                break;
            case CLEAR:
                if (command.mode == Mode.SET) {
                    Console console = gameObject.intrface.getConsole();
                    console.clear();
                }
                command.status = Status.SUCCEEDED;
                break;
            case PING:
                if (command.mode == Mode.GET && Game.getCurrentMode() == Game.Mode.MULTIPLAYER_JOIN && gameObject.game.isConnected()) {
                    // ingame ping calculation
                    result = Math.round(gameObject.game.getPing() * 1000.0); // ping express in milliseconds
                    command.status = Status.SUCCEEDED;
                }
                break;
            case PRINT:
                if (command.mode == Command.Mode.SET) {
                    if (!command.args.isEmpty()) {
                        command.status = Status.SUCCEEDED;
                    }
                }
                break;
            case SAY:
                if (command.mode == Command.Mode.SET) {
                    if (!command.args.isEmpty()) {
                        if (Game.getCurrentMode() == Game.Mode.MULTIPLAYER_HOST) {
                            String msg = gameObject.levelContainer.levelActors.player.getName() + ":" + command.args.getFirst();
                            Response response = new Response(0L, ResponseIfc.ResponseStatus.OK, DSObject.DataType.STRING, msg);
                            gameObject.gameServer.clients.forEach(ci -> {
                                try {
                                    response.send("*", gameObject.gameServer, ci.session);
                                } catch (IOException ex) {
                                    DSLogger.reportError("Unable to deliver chat message, ex:", ex);
                                }
                            });
                            command.status = Status.SUCCEEDED;
                        } else if (Game.getCurrentMode() == Game.Mode.MULTIPLAYER_JOIN && gameObject.game.isConnected()) {
                            try {
                                RequestIfc sendChatMsgReq = new Request(RequestIfc.RequestType.SAY, DSObject.DataType.STRING, command.args.getFirst());
                                sendChatMsgReq.send(gameObject.game, gameObject.game.getSession());
                                command.status = Status.SUCCEEDED;
                            } catch (Exception ex) {
                                DSLogger.reportError("Unable to send chat message!", ex);
                                DSLogger.reportError(ex.getMessage(), ex);
                            }
                        }
                    }
                }
                break;
            case CONNECT:
                if (command.mode == Command.Mode.SET) {
                    if (!command.args.isEmpty()) {
                        String parts[] = command.args.get(0).toString().split(":");

                        if (parts.length >= 1) {
                            String serverHostName = parts[0];
                            gameObject.game.setServerHostName(serverHostName);
                        }
                        if (parts.length <= 2) {
                            int port = Integer.parseInt(parts[1]);
                            gameObject.game.setPort(port);
                        }

//                        gameObject.intrface.getConsole().write(String.format("Trying to connect to server %s:%d ...", gameObject.game.gameObject.game.getServerHostName(), gameObject.game.gameObject.game.getPort()), false);                        
                        try {
                            command.status = Status.PENDING;

                            gameObject.intrface.getInfoMsgText().setContent("Trying to connect to server...");
                            gameObject.intrface.getInfoMsgText().setEnabled(true);

                            double beginTime = GLFW.glfwGetTime();
                            if (gameObject.game.connectToServer()) {
                                gameObject.intrface.getInfoMsgText().setContent("Connected to server!");
                                gameObject.intrface.getConsole().write("Connected to server!");
                                double endTime = GLFW.glfwGetTime();
                                long tripTime = Math.round((endTime - beginTime) * 1000.0);
                                gameObject.WINDOW.setTitle(GameObject.WINDOW_TITLE + " - " + gameObject.game.getServerHostName() + " ( " + tripTime + " ms )");
                                if (gameObject.generateMultiPlayerLevelAsJoin()) {
                                    Game.setCurrentMode(Game.Mode.MULTIPLAYER_JOIN);
                                    gameObject.intrface.getGameMenu().getTitle().setContent("MULTIPLAYER");
                                }
                                command.status = Status.SUCCEEDED;
                            } else {
                                command.status = Status.FAILED;
                            }
                            gameObject.intrface.getInfoMsgText().setEnabled(false);
                        } catch (InterruptedException | ExecutionException | UnsupportedEncodingException ex) {
                            DSLogger.reportError(ex.getMessage(), ex);
                        }
                    }
                }
                break;
            case DISCONNECT:
                if (command.mode == Command.Mode.SET) {
                    gameObject.game.disconnectFromServer();
                    gameObject.clearEverything();

                    command.status = Status.SUCCEEDED;
                }
                break;
            case START_SERVER:
                if (command.mode == Command.Mode.SET) {
                    if (!command.args.isEmpty()) {
                        final int numBlocks;
                        // set number of blocks
                        switch (command.args.get(0).toString().toUpperCase()) {
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
                                numBlocks = 0;
                                break;
                        }

                        gameObject.intrface.setNumBlocks(numBlocks);
                        gameObject.randomLevelGenerator.setNumberOfBlocks(numBlocks);

                        if (command.args.size() == 2) {
                            gameObject.randomLevelGenerator.setSeed(Long.parseLong(command.args.get(1).toString()));
                        }

                        gameObject.clearEverything();
                        if (!gameObject.gameServer.isRunning() && numBlocks != 0) {
                            gameObject.intrface.getProgText().setEnabled(true);
                            gameObject.gameServer.startServer();

                            // if game endpoint is running and not shut down
                            gameObject.TaskExecutor.execute(() -> {
                                try {
                                    boolean ok = false;
                                    if (gameObject.gameServer.isRunning() && !gameObject.gameServer.isShutDownSignal()) {
                                        ok |= gameObject.generateMultiPlayerLevelAsHost(numBlocks);
                                        gameObject.WINDOW.setTitle(GameObject.WINDOW_TITLE + " - " + gameObject.gameServer.getWorldName() + " - Player Count: " + (1 + gameObject.gameServer.clients.size()));
                                        Game.setCurrentMode(Game.Mode.MULTIPLAYER_HOST);
                                        gameObject.intrface.getGameMenu().getTitle().setContent("MULTIPLAYER");
                                    }

                                    if (!ok) {
                                        command.status = Status.FAILED;
                                        gameObject.clearEverything();
                                    } else {
                                        command.status = Status.SUCCEEDED;
                                    }
                                } catch (InterruptedException | ExecutionException ex) {
                                    DSLogger.reportError(ex.getMessage(), ex);
                                }

                            });

                        }
                        command.status = Status.PENDING;
                    }
                }
                break;
            case STOP_SERVER:
                if (command.mode == Command.Mode.SET) {
                    if (Game.getCurrentMode() == Game.Mode.MULTIPLAYER_HOST) {
                        if (gameObject.gameServer.isRunning()) {
                            gameObject.gameServer.stopServer();
                        }
                    } else if (Game.getCurrentMode() == Game.Mode.MULTIPLAYER_JOIN) {
                        gameObject.game.disconnectFromServer();
                    }
                    gameObject.clearEverything();
                    command.status = Status.SUCCEEDED;
                }
                break;
            case KICK_PLAYER:
                if (command.mode == Command.Mode.SET) {
                    final List<ClientInfo> clientInfo = Arrays.asList(gameObject.gameServer.getClientInfo());
                    final List<String> guids = gameObject.levelContainer.levelActors.otherPlayers.stream().map(
                            (Critter t) -> t.uniqueId).filter(x -> command.args.contains(x)).collect((Collectors.toList()));
                    clientInfo.forEach(ci -> {
                        if (guids.contains(ci.uniqueId)) {
                            GameServer.kickPlayer(gameObject.gameServer, ci.uniqueId);
                        }
                    });
                    command.status = Status.SUCCEEDED;
                }
            case NAME:
                if (command.mode == Mode.GET) {
                    result = gameObject.levelContainer.levelActors.player.getName();
                    command.status = Status.SUCCEEDED;
                } else if (command.mode == Command.Mode.SET) {
                    gameObject.levelContainer.levelActors.player.setName(command.args.getFirst().toString());
                    multiplayerMenu.items.getIf(y -> y.keyText.content.equals("PLAYER NAME")).menuValue.setCurrentValue(command.args.getFirst().toString());
                    command.status = Status.SUCCEEDED;
                }
                break;
            case MODEL:
                if (command.mode == Mode.GET) {
                    result = gameObject.levelContainer.levelActors.player.body.getTexName();
                    command.status = Status.SUCCEEDED;
                } else if (command.mode == Command.Mode.SET) {
                    gameObject.levelContainer.levelActors.player.setModelClazz(command.args.getFirst().toString());
                    gameObject.levelContainer.levelActors.player.switchBodyModel();
                    singleplayerMenu.items.getIf(y -> y.keyText.content.equals("CHARACTER MODEL")).menuValue.setCurrentValue(command.args.getFirst().toString().toUpperCase());
                    multiplayerMenu.items.getIf(y -> y.keyText.content.equals("CHARACTER MODEL")).menuValue.setCurrentValue(command.args.getFirst().toString().toUpperCase());
                    command.status = Status.SUCCEEDED;
                }
                break;
            case COLOR:
                if (command.mode == Mode.GET) {
                    result = gameObject.levelContainer.levelActors.player.body.getPrimaryRGBColor().toString();
                    command.status = Status.SUCCEEDED;
                } else if (command.mode == Command.Mode.SET) {
                    final Player player = gameObject.levelContainer.levelActors.player;
                    Vector4f colorRGBA = GlobalColors.WHITE_RGBA;
                    if (command.args.size() == 1) {
                        colorRGBA = new Vector4f(GlobalColors.getRGBColorOrDefault(command.args.getFirst().toString().toUpperCase()), 1.0f);
                        singleplayerMenu.items.getIf(y -> y.keyText.content.equals("COLOR")).menuValue.getValueText().setColor(colorRGBA);
                        singleplayerMenu.items.getIf(y -> y.keyText.content.equals("COLOR")).menuValue.setCurrentValue(command.args.getFirst().toString().toUpperCase());
                        multiplayerMenu.items.getIf(y -> y.keyText.content.equals("COLOR")).menuValue.getValueText().setColor(colorRGBA);
                        multiplayerMenu.items.getIf(y -> y.keyText.content.equals("COLOR")).menuValue.setCurrentValue(command.args.getFirst().toString().toUpperCase());
                        command.status = Status.SUCCEEDED;
                    } else if (command.args.size() == 3) {
                        float r = Float.parseFloat(command.args.get(0).toString());
                        float g = Float.parseFloat(command.args.get(1).toString());
                        float b = Float.parseFloat(command.args.get(2).toString());
                        colorRGBA = new Vector4f(r, g, b, 1.0f);
                        singleplayerMenu.items.getIf(y -> y.keyText.content.equals("COLOR")).menuValue.getValueText().setColor(colorRGBA);
                        singleplayerMenu.items.getIf(y -> y.keyText.content.equals("COLOR")).menuValue.setCurrentValue("Custom");
                        multiplayerMenu.items.getIf(y -> y.keyText.content.equals("COLOR")).menuValue.getValueText().setColor(colorRGBA);
                        multiplayerMenu.items.getIf(y -> y.keyText.content.equals("COLOR")).menuValue.setCurrentValue("Custom");
                        command.status = Status.SUCCEEDED;
                    }
                    player.body.setPrimaryRGBAColor(colorRGBA);
                }
                break;
            case NOP:
            default:
                break;
        }
        // clearing the arguments allow execution repeatedly
        if (command.status != Status.SUCCEEDED) {
            command.status = Status.FAILED;
        }

        command.result = result;

        return result;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Status getStatus() {
        return status;
    }

    @Override
    public Object call() throws Exception {
        return (this.result = Command.execute(GameObject.getInstance(), this));
    }

    public List<Object> getArgs() {
        return args;
    }

    // renderer commands need OpenGL whilst other doesn't
    public boolean isRendererCommand() {
        return this.target == Target.VSYNC || this.target == Target.SCREENSHOT || this.target == Target.RESOLUTION;
    }

    // game commands
    public boolean isGameCommand() {
        return this.target == Target.MONITOR_GET || this.target == Target.MONITOR_ID || this.target == Target.GAME_TICKS || this.target == Target.FPS_MAX || this.target == Target.FULLSCREEN || this.target == Target.WATER_EFFECTS || this.target == Target.SHADOW_EFFECTS
                || this.target == Target.MOUSE_SENSITIVITY || this.target == Target.MUSIC_VOLUME || this.target == Target.SOUND_VOLUME || this.target == Target.EXIT || this.target == Target.POSITION || this.target == Target.SIZEOF || this.target == Target.CACHE || this.target == Target.CLEAR
                || this.target == Target.PRINT || this.target == Target.SAY || this.target == Target.PING
                || this.target == Target.CONNECT || this.target == Target.DISCONNECT
                || this.target == Target.START_SERVER || this.target == Target.STOP_SERVER || this.target == Target.KICK_PLAYER
                || this.target == Target.NAME || this.target == Target.MODEL || this.target == Target.COLOR;
    }

    // game commands
    public static boolean isGameCommand(Command command) {
        return command.target == Target.MONITOR_GET || command.target == Target.MONITOR_ID || command.target == Target.GAME_TICKS || command.target == Target.FPS_MAX || command.target == Target.FULLSCREEN || command.target == Target.WATER_EFFECTS || command.target == Target.SHADOW_EFFECTS
                || command.target == Target.MOUSE_SENSITIVITY || command.target == Target.MUSIC_VOLUME || command.target == Target.SOUND_VOLUME || command.target == Target.EXIT || command.target == Target.POSITION || command.target == Target.SIZEOF || command.target == Target.SIZEOF || command.target == Target.CACHE || command.target == Target.CLEAR
                || command.target == Target.PRINT || command.target == Target.SAY || command.target == Target.PING
                || command.target == Target.CONNECT || command.target == Target.DISCONNECT
                || command.target == Target.START_SERVER || command.target == Target.STOP_SERVER || command.target == Target.KICK_PLAYER
                || command.target == Target.NAME || command.target == Target.MODEL || command.target == Target.COLOR;
    }

    // renderer commands need OpenGL whilst other doesn't
    public static boolean isRendererCommand(Command command) {
        return command.target == Target.VSYNC || command.target == Target.SCREENSHOT || command.target == Target.RESOLUTION;
    }

    public Object getResult() {
        return result;
    }

}
