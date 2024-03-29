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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.imageio.ImageIO;
import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.cache.CacheModule;
import rs.alexanderstojanovich.evg.cache.CachedInfo;
import rs.alexanderstojanovich.evg.chunk.Chunk;
import rs.alexanderstojanovich.evg.core.MasterRenderer;
import rs.alexanderstojanovich.evg.core.PerspectiveRenderer;
import rs.alexanderstojanovich.evg.core.WaterRenderer;
import rs.alexanderstojanovich.evg.critter.Observer;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.main.GameRenderer;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.Trie;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Command implements Callable<Object> { // its not actually a thread but its used for remote execution (from Executor)

    /**
     * Command mnemonic (syntax)
     */
    public static enum Target {
        GAME_TICKS,
        FPS_MAX,
        RESOLUTION,
        FULLSCREEN,
        VSYNC,
        WATER_EFFECTS,
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
        ERROR
    };

    protected Target target = Target.NOP;

    public static enum Mode {
        GET, SET
    };
    protected Mode mode = Mode.GET;

    public static enum Status {
        INIT, PENDING, SUCCEEDED, FAILED
    }

    protected Object result = null;
    protected Status status = Status.INIT;

    // commands differ in arugment length and type, therefore list is used
    protected final List<Object> args = new ArrayList<>();

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
        String[] things = input.split(" ");
        if (things.length > 0) {
            switch (things[0].toLowerCase()) {
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
                    if (things.length == 2) {
                        command.args.add(WaterRenderer.WaterEffectsQuality.valueOf(things[1].toUpperCase()).name());
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
                default:
                    command.target = Target.ERROR;
                    break;
            }
        }

        if (command.args.isEmpty() && command.target != Target.CLEAR || command.target == Target.SIZEOF) {
            command.mode = Mode.GET;
        } else {
            command.mode = Mode.SET;
        }

        return command;
    }

    /**
     * Executes command which modifies game, renderer or game object Rule is
     * that commands which directly affect window or OpenGL are being called
     * from the GameRenderer, whilst other can be called from the main method
     *
     * @param command chosen command
     * @return execution status (true if successful, otherwise false)
     */
    public static Object execute(Command command) {
        Object result = null;
        command.status = Status.PENDING;
        switch (command.target) {
            case GAME_TICKS:
                switch (command.mode) {
                    case GET:
                        result = Game.getAccumulator();
                        command.status = Status.SUCCEEDED;
                        break;
                    case SET:
                        float ticks = (float) command.args.get(0);
                        if (ticks >= 0.0f) {
                            Game.setAccumulator(ticks);
                            GameRenderer.setAnimationTimer(ticks);
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
                            GameRenderer.setAnimationTimer(0.0);
                            Game.setFpsMax(fpsMax);
                            command.status = Status.SUCCEEDED;
                        }
                        break;

                }
                break;
            case RESOLUTION:
                switch (command.mode) {
                    case GET:
                        result = GameObject.MY_WINDOW.getWidth() + "x" + GameObject.MY_WINDOW.getHeight();
                        command.status = Status.SUCCEEDED;
                        break;
                    case SET:
                        // changing resolution if necessary
                        int width = (int) command.args.get(0);
                        int height = (int) command.args.get(1);
                        boolean status = GameObject.MY_WINDOW.setResolution(width, height);
                        if (status) {
                            MasterRenderer.setResolution(width, height);
                            PerspectiveRenderer.updatePerspective(GameObject.MY_WINDOW);
                            PerspectiveRenderer.setBuffered(false);
                            command.status = Status.SUCCEEDED;
                        } else {
                            command.status = Status.FAILED;
                        }
                        GameObject.MY_WINDOW.centerTheWindow();
                        break;
                }

                break;
            case FULLSCREEN:
                switch (command.mode) {
                    case GET:
                        result = GameObject.MY_WINDOW.isFullscreen();
                        command.status = Status.SUCCEEDED;
                        break;
                    case SET:
                        boolean bool = (boolean) command.args.get(0);
                        GameObject.MY_WINDOW.setFullscreen(bool);
                        GameObject.MY_WINDOW.centerTheWindow();
                        break;
                }
                break;
//            case WINDOWED:
//                GameObject.MY_WINDOW.windowed();
//                GameObject.MY_WINDOW.centerTheWindow();
//                command.status = Status.SUCCEEDED;
//                break;
            case VSYNC: // OpenGL
                switch (command.mode) {
                    case GET:
                        result = GameObject.MY_WINDOW.isVsync();
                        command.status = Status.SUCCEEDED;
                        break;
                    case SET:
                        boolean bool = (boolean) command.args.get(0);
                        GameObject.MY_WINDOW.setVSync(bool);
                        command.status = Status.SUCCEEDED;
                        break;
                }
                break;
            case WATER_EFFECTS:
                switch (command.mode) {
                    case GET:
                        result = GameObject.getWaterRenderer().getEffectsQuality();
                        command.status = Status.SUCCEEDED;
                        break;
                    case SET:
                        GameObject.getWaterRenderer().setEffectsQuality(WaterRenderer.WaterEffectsQuality.valueOf((String) command.args.get(0)));
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
                            command.status = Status.SUCCEEDED;
                        }
                        break;
                }
                break;
            case MUSIC_VOLUME:
                switch (command.mode) {
                    case GET:
                        result = GameObject.getMusicPlayer().getGain();
                        command.status = Status.SUCCEEDED;
                        break;
                    case SET:
                        float music = (float) command.args.get(0);
                        if (music >= 0.0f && music <= 1.0f) {
                            GameObject.getMusicPlayer().setGain(music);
                            command.status = Status.SUCCEEDED;
                        }
                        break;
                }
                break;
            case SOUND_VOLUME:
                switch (command.mode) {
                    case GET:
                        result = GameObject.getSoundFXPlayer().getGain();
                        command.status = Status.SUCCEEDED;
                        break;
                    case SET:
                        float sound = (float) command.args.get(0);
                        if (sound >= 0.0f && sound <= 1.0f) {
                            GameObject.getSoundFXPlayer().setGain(sound);
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
                    ImageIO.write(GameObject.MY_WINDOW.getScreenshot(), "PNG", screenshot);
                } catch (IOException ex) {
                    DSLogger.reportError(ex.getMessage(), ex);
                }
                GameObject.getIntrface().getScreenText().setEnabled(true);
                GameObject.getIntrface().getScreenText().setContent("Screen saved to " + screenshot.getAbsolutePath());
                command.status = Status.SUCCEEDED;
                break;
            case EXIT:
                GameObject.MY_WINDOW.close();
                command.status = Status.SUCCEEDED;
                break;
            case POSITION:
                Observer mainActor = GameObject.getLevelContainer().levelActors.mainObserver();
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
                        int totalSize = CacheModule.totalSize(GameObject.getLevelContainer().getChunks());
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
                            Chunk chunk = GameObject.getLevelContainer().getChunks().getChunk(chunkId);
                            if (chunk != null) {
                                size = chunk.getBlockList().size();
                            }
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
                    Console console = GameObject.getIntrface().getConsole();
                    console.clear();
                }
                command.status = Status.SUCCEEDED;
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
        return (this.result = Command.execute(this));
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
        return this.target == Target.GAME_TICKS || this.target == Target.FPS_MAX || this.target == Target.FULLSCREEN || this.target == Target.WATER_EFFECTS
                || this.target == Target.MOUSE_SENSITIVITY || this.target == Target.MUSIC_VOLUME || this.target == Target.SOUND_VOLUME || this.target == Target.EXIT || this.target == Target.POSITION || this.target == Target.SIZEOF || this.target == Target.CACHE || this.target == Target.CLEAR;
    }

    // game commands
    public static boolean isGameCommand(Command command) {
        return command.target == Target.GAME_TICKS || command.target == Target.FPS_MAX || command.target == Target.FULLSCREEN || command.target == Target.WATER_EFFECTS
                || command.target == Target.MOUSE_SENSITIVITY || command.target == Target.MUSIC_VOLUME || command.target == Target.SOUND_VOLUME || command.target == Target.EXIT || command.target == Target.POSITION || command.target == Target.SIZEOF || command.target == Target.SIZEOF || command.target == Target.CACHE || command.target == Target.CLEAR;
    }

    // renderer commands need OpenGL whilst other doesn't
    public static boolean isRendererCommand(Command command) {
        return command.target == Target.VSYNC || command.target == Target.SCREENSHOT || command.target == Target.RESOLUTION;
    }

    public Object getResult() {
        return result;
    }

}
