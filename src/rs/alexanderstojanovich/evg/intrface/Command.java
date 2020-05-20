/*
 * Copyright (C) 2020 Coa
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
import java.util.List;
import java.util.concurrent.Callable;
import javax.imageio.ImageIO;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.main.Renderer;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Coa
 */
public enum Command implements Callable<Boolean> { // its not actually a thread but its used for remote execution (from Executor)
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
    SCREENSHOT,
    NOP,
    ERROR;

    // commands differ in arugment length and type, therefore list is used
    private final List<Object> args = new ArrayList<>();

    // constructs command from given string input
    public static Command getCommand(String input) {
        Command command = ERROR;
        String[] things = input.split(" ");
        if (things.length > 0) {
            switch (things[0].toLowerCase()) {
                case "fps_max":
                case "fpsmax":
                    if (things.length == 2) {
                        command = FPS_MAX;
                        command.args.add(Integer.parseInt(things[1]));
                    }
                    break;
                case "resolution":
                case "res":
                    if (things.length == 3) {
                        command = RESOLUTION;
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
                    if (things.length == 2) {
                        command = VSYNC;
                        command.args.add(Boolean.parseBoolean(things[1]));
                    }
                    break;
                case "waterEffects":
                case "water_effects":
                    if (things.length == 2) {
                        command = WATER_EFFECTS;
                        command.args.add(Boolean.parseBoolean(things[1]));
                    }
                    break;
                case "msens":
                case "mouse_sensitivity":
                    if (things.length == 2) {
                        command = MOUSE_SENSITIVITY;
                        command.args.add(Float.parseFloat(things[1]));
                    }
                    break;
                case "music":
                case "musicVolume":
                    if (things.length == 2) {
                        command = MUSIC_VOLUME;
                        command.args.add(Float.parseFloat(things[1]));
                    }
                    break;
                case "sound":
                case "soundVolume":
                    if (things.length == 2) {
                        command = SOUND_VOLUME;
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
                default:
                    command = ERROR;
                    break;
            }
        }

        return command;
    }

    // executes command which modifies game, renderer or game object
    // Rule is that commands which directly affect window or OpenGL 
    // are being called from the Renderer, whilst other can be called from the main method
    public static boolean execute(Command command) {
        boolean success = false;
        switch (command) {
            case FPS_MAX:
                Game.setFpsMax((int) command.args.get(0));
                Renderer.setFpsTicks(0.0);
                success = true;
                break;
            case RESOLUTION:
                success = GameObject.MY_WINDOW.setResolution((int) command.args.get(0), (int) command.args.get(1));
                GameObject.MY_WINDOW.centerTheWindow();
                break;
            case FULLSCREEN:
                GameObject.MY_WINDOW.fullscreen();
                GameObject.MY_WINDOW.centerTheWindow();
                success = true;
                break;
            case WINDOWED:
                GameObject.MY_WINDOW.windowed();
                GameObject.MY_WINDOW.centerTheWindow();
                success = true;
                break;
            case VSYNC: // OpenGL
                boolean bool = (boolean) command.args.get(0);
                if (bool) {
                    GameObject.MY_WINDOW.enableVSync();
                } else {
                    GameObject.MY_WINDOW.disableVSync();
                }
                success = true;
                break;
            case WATER_EFFECTS:
                Game.setWaterEffects((boolean) command.args.get(0));
                success = true;
                break;
            case MOUSE_SENSITIVITY:
                float msens = (float) command.args.get(0);
                if (msens >= 0.0f && msens <= 100.0f) {
                    Game.setMouseSensitivity(msens);
                    success = true;
                }
                break;
            case MUSIC_VOLUME:
                float music = (float) command.args.get(0);
                if (music >= 0.0f && music <= 1.0f) {
                    GameObject.getInstance().getMusicPlayer().setGain(music);
                    success = true;
                }
                break;
            case SOUND_VOLUME:
                float sound = (float) command.args.get(0);
                if (sound >= 0.0f && sound <= 1.0f) {
                    GameObject.getInstance().getSoundFXPlayer().setGain(sound);
                    success = true;
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
                GameObject gameObject = GameObject.getInstance();
                gameObject.getIntrface().getScreenText().setEnabled(true);
                gameObject.getIntrface().getScreenText().setContent("Screen saved to " + screenshot.getAbsolutePath());
                success = true;
                break;
            case EXIT:
                GameObject.MY_WINDOW.close();
                success = true;
                break;
            case NOP:
            default:
                success = true;
                break;
        }
        command.args.clear();
        return success;
    }

    @Override
    public Boolean call() throws Exception {
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
