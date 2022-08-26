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
import rs.alexanderstojanovich.evg.level.CacheModule;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.main.Renderer;
import rs.alexanderstojanovich.evg.models.Chunk;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.Trie;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public enum Command implements Callable<Object> { // its not actually a thread but its used for remote execution (from Executor)
    FPS_MAX,
    RESOLUTION,
    FULLSCREEN,
    WINDOWED,
    VSYNC,
    WATER_EFFECTS,
    MOUSE_SENSITIVITY,
    MUSIC_VOLUME,
    SOUND_VOLUME,
    EXIT,
    POSITION,
    SCREENSHOT,
    SIZEOF,
    NOP,
    ERROR;

    public static enum Mode {
        GET, SET
    };
    protected Mode mode = Mode.GET;
    protected boolean status = false;

    // commands differ in arugment length and type, therefore list is used
    protected final List<Object> args = new ArrayList<>();

    protected static final Trie trie = new Trie();

    static {
        for (Command command : values()) {
            if (command != ERROR && command != NOP) {
                trie.insert(command.name().toLowerCase());
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
     * Constructs command from given string input
     *
     * @param input given input
     * @return Command with empty args.
     */
    public static Command getCommand(String input) {
        Command command = ERROR;
        String[] things = input.split(" ");
        if (things.length > 0) {
            switch (things[0].toLowerCase()) {
                case "fps_max":
                case "fpsmax":
                    command = FPS_MAX;
                    if (things.length == 2) {
                        command.args.add(Integer.parseInt(things[1]));
                    }
                    break;
                case "resolution":
                case "res":
                    command = RESOLUTION;
                    if (things.length == 3) {
                        command.args.add(Integer.parseInt(things[1]));
                        command.args.add(Integer.parseInt(things[2]));
                    }
                    break;
                case "fullscreen":
                    command = FULLSCREEN;
                    break;
                case "windowed":
                    command = WINDOWED;
                    break;
                case "v_sync":
                case "vsync":
                    command = VSYNC;
                    if (things.length == 2) {
                        command.args.add(Boolean.parseBoolean(things[1]));
                    }
                    break;
                case "waterEffects":
                case "water_effects":
                    command = WATER_EFFECTS;
                    if (things.length == 2) {
                        command.args.add(Boolean.parseBoolean(things[1]));
                    }
                    break;
                case "msens":
                case "mouse_sensitivity":
                    command = MOUSE_SENSITIVITY;
                    if (things.length == 2) {
                        command.args.add(Float.parseFloat(things[1]));
                    }
                    break;
                case "music_volume":
                case "musicVolume":
                    command = MUSIC_VOLUME;
                    if (things.length == 2) {
                        command.args.add(Float.parseFloat(things[1]));
                    }
                    break;
                case "sound_volume":
                case "soundVolume":
                    command = SOUND_VOLUME;
                    if (things.length == 2) {
                        command.args.add(Float.parseFloat(things[1]));
                    }
                    break;
                case "quit":
                case "exit":
                    command = EXIT;
                    break;
                case "screenshot":
                    command = SCREENSHOT;
                    break;
                case "pos":
                case "position":
                    command = POSITION;
                    if (things.length == 2) {
                        command.args.add(Integer.parseInt(things[1]));
                    }
                    if (things.length == 4) {
                        command.args.add(Float.parseFloat(things[1]));
                        command.args.add(Float.parseFloat(things[2]));
                        command.args.add(Float.parseFloat(things[3]));
                    }
                    break;
                case "sizeof":
                case "size_of":
                    command = SIZEOF;
                    if (things.length == 2) {
                        command.args.add(Integer.parseInt(things[1]));
                    }
                    break;
                default:
                    command = ERROR;
                    break;
            }
        }

        if (command.args.isEmpty() || command == SIZEOF) {
            command.mode = Mode.GET;
        } else {
            command.mode = Mode.SET;
        }

        return command;
    }

    /**
     * Executes command which modifies game, renderer or game object Rule is
     * that commands which directly affect window or OpenGL are being called
     * from the Renderer, whilst other can be called from the main method
     *
     * @param command chosen command
     * @return execution status (true if successful, otherwise false)
     */
    public static Object execute(Command command) {
        Object result = null;
        command.status = false;
        GameObject gameObject = GameObject.getInstance();
        switch (command) {
            case FPS_MAX:
                switch (command.mode) {
                    case GET:
                        result = Game.getFpsMax();
                        command.status = true;
                        break;
                    case SET:
                        int fpsMax = (int) command.args.get(0);
                        if (fpsMax > 0 && fpsMax <= 1E6) {
                            Renderer.setFps(0);
                            Renderer.setFpsTicks(0.0);
                            Game.setFpsMax(fpsMax);
                            command.status = true;
                        }
                        break;

                }
                break;
            case RESOLUTION:
                switch (command.mode) {
                    case GET:
                        result = GameObject.MY_WINDOW.getWidth() + "x" + GameObject.MY_WINDOW.getHeight();
                        command.status = true;
                        break;
                    case SET:
                        command.status = GameObject.MY_WINDOW.setResolution((int) command.args.get(0), (int) command.args.get(1));
                        GameObject.MY_WINDOW.centerTheWindow();
                        break;
                }

                break;
            case FULLSCREEN:
                GameObject.MY_WINDOW.fullscreen();
                GameObject.MY_WINDOW.centerTheWindow();
                command.status = true;
                break;
            case WINDOWED:
                GameObject.MY_WINDOW.windowed();
                GameObject.MY_WINDOW.centerTheWindow();
                command.status = true;
                break;
            case VSYNC: // OpenGL
                switch (command.mode) {
                    case GET:
                        result = GameObject.MY_WINDOW.isVsync();
                        command.status = true;
                        break;
                    case SET:
                        boolean bool = (boolean) command.args.get(0);
                        if (bool) {
                            GameObject.MY_WINDOW.enableVSync();
                        } else {
                            GameObject.MY_WINDOW.disableVSync();
                        }
                        command.status = true;
                        break;
                }
                break;
            case WATER_EFFECTS:
                switch (command.mode) {
                    case GET:
                        result = Game.isWaterEffects();
                        command.status = true;
                        break;
                    case SET:
                        Game.setWaterEffects((boolean) command.args.get(0));
                        command.status = true;
                        break;
                }
                break;
            case MOUSE_SENSITIVITY:
                switch (command.mode) {
                    case GET:
                        result = Game.getMouseSensitivity();
                        command.status = true;
                        break;
                    case SET:
                        float msens = (float) command.args.get(0);
                        if (msens >= 0.0f && msens <= 100.0f) {
                            Game.setMouseSensitivity(msens);
                            command.status = true;
                        }
                        break;
                }
                break;
            case MUSIC_VOLUME:
                switch (command.mode) {
                    case GET:
                        result = GameObject.getInstance().getMusicPlayer().getGain();
                        command.status = true;
                        break;
                    case SET:
                        float music = (float) command.args.get(0);
                        if (music >= 0.0f && music <= 1.0f) {
                            GameObject.getInstance().getMusicPlayer().setGain(music);
                            command.status = true;
                        }
                        break;
                }
                break;
            case SOUND_VOLUME:
                switch (command.mode) {
                    case GET:
                        result = GameObject.getInstance().getSoundFXPlayer().getGain();
                        command.status = true;
                        break;
                    case SET:
                        float sound = (float) command.args.get(0);
                        if (sound >= 0.0f && sound <= 1.0f) {
                            GameObject.getInstance().getSoundFXPlayer().setGain(sound);
                            command.status = true;
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
                        + "-" + now.getNano() / 1E6 // one million
                        + ".png");
                if (screenshot.exists()) {
                    screenshot.delete();
                }
                try {
                    ImageIO.write(GameObject.MY_WINDOW.getScreen(), "PNG", screenshot);
                } catch (IOException ex) {
                    DSLogger.reportError(ex.getMessage(), ex);
                }
                gameObject.getIntrface().getScreenText().setEnabled(true);
                gameObject.getIntrface().getScreenText().setContent("Screen saved to " + screenshot.getAbsolutePath());
                command.status = true;
                break;
            case EXIT:
                GameObject.MY_WINDOW.close();
                command.status = true;
                break;
            case POSITION:
                Vector3f mainActorPos = gameObject.getLevelContainer().levelActors.getMainActor().getPosition();
                int chunkId;
                switch (command.mode) {
                    case GET:
                        final StringBuilder sb = new StringBuilder();
                        sb.append(String.format("pos: (%.1f,%.1f,%.1f)", mainActorPos.x, mainActorPos.y, mainActorPos.z));
                        chunkId = Chunk.chunkFunc(mainActorPos);
                        sb.append(" | ");
                        sb.append(String.format("chunkId: %d", chunkId));
                        result = sb.toString();
                        command.status = true;
                        break;
                    case SET:
                        if (command.args.size() == 1) {
                            chunkId = (int) command.args.get(0);
                            Vector3f newPos = Chunk.invChunkFunc(chunkId);
                            mainActorPos.x = newPos.x;
                            mainActorPos.y = newPos.y;
                            mainActorPos.z = newPos.z;
                            command.status = true;
                        } else if (command.args.size() == 3) {
                            float newPosx = (float) command.args.get(0);
                            float newPosy = (float) command.args.get(1);
                            float newPosz = (float) command.args.get(2);
                            mainActorPos.x = newPosx;
                            mainActorPos.y = newPosy;
                            mainActorPos.z = newPosz;
                            command.status = true;
                        }
                        break;
                }
                break;
            case SIZEOF:
                if (command.mode == Mode.GET) {
                    if (command.args.isEmpty()) {
                        int solidSize = CacheModule.totalSize(gameObject.getLevelContainer().getSolidChunks(), true);
                        int fluidSize = CacheModule.totalSize(gameObject.getLevelContainer().getFluidChunks(), false);
                        result = String.format("SolidSize = %d | FluidSize = %d | TotalChunks = %d", solidSize, fluidSize, Chunk.CHUNK_NUM);
                    } else {
                        chunkId = (int) command.args.get(0);
                        boolean cached = CacheModule.isCached(chunkId, true);
                        int solidSize = 0, fluidSize = 0;
                        if (cached) {
                            solidSize = CacheModule.cachedSize(chunkId, true);
                            fluidSize = CacheModule.cachedSize(chunkId, false);
                        } else {
                            Chunk solidChunk = gameObject.getLevelContainer().getSolidChunks().getChunk(chunkId);
                            if (solidChunk != null) {
                                solidSize = solidChunk.getBlockList().size();
                            }
                            Chunk fluidChunk = gameObject.getLevelContainer().getFluidChunks().getChunk(chunkId);
                            if (fluidChunk != null) {
                                fluidSize = fluidChunk.getBlockList().size();
                            }
                        }
                        result = String.format("SolidSize = %d | FluidSize = %d | Cached = %s", solidSize, fluidSize, cached);
                    }
                }
                command.status = true;
                break;
            case NOP:
            default:
                break;
        }
        // clearing the arguments allow execution repeatedly
        command.args.clear();
        return result;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public boolean isStatus() {
        return status;
    }

    @Override
    public Object call() throws Exception {
        return Command.execute(this);
    }

    public List<Object> getArgs() {
        return args;
    }

    // renderer commands need OpenGL whilst other doesn't
    public boolean isRendererCommand() {
        if (this == VSYNC
                || this == SCREENSHOT) {
            return true;
        } else {
            return false;
        }
    }

    // renderer commands need OpenGL whilst other doesn't
    public static boolean isRendererCommand(Command command) {
        if (command == VSYNC
                || command == SCREENSHOT) {
            return true;
        } else {
            return false;
        }
    }

}
