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
import rs.alexanderstojanovich.evg.core.PerspectiveRenderer;
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
    NOP;
    
    private final List<Object> args = new ArrayList<>();
    
    public static Command getCommand(String input) {
        Command command = Command.NOP;
        String[] things = input.split(" ");
        if (things.length > 0) {
            switch (things[0].toLowerCase()) {
                case "fps_max":
                case "fpsmax":
                    if (things.length == 2) {
                        int num = Integer.parseInt(things[1]);
                        if (num > 0) {
                           command = FPS_MAX;
                           command.args.add(num);
                        }
                    }
                    break;
                case "resolution":
                case "res":
                    if (things.length == 3) {
                        int width = Integer.parseInt(things[1]);
                        int height = Integer.parseInt(things[2]);
                        command = RESOLUTION;
                        command.args.add(width);
                        command.args.add(height);
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
                        float volume = Float.parseFloat(things[1]);
                        if (volume >= 0.0f && volume <= 1.0f) {
                            command = MUSIC_VOLUME;
                            command.args.add(volume);
                        }
                    }
                    break;
                case "sound":
                case "soundVolume":
                    if (things.length == 2) {
                        float volume = Float.parseFloat(things[1]);
                        if (volume >= 0.0f && volume <= 1.0f) {
                            command = SOUND_VOLUME;
                            command.args.add(volume);
                        }
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
                    command = NOP;
                    break;
            }
        }
        
        return command;
    }
    
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
                PerspectiveRenderer.updatePerspective(GameObject.MY_WINDOW);
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
            case VSYNC:
                boolean bool = (boolean) command.args.get(0);
                if (bool)
                    GameObject.MY_WINDOW.enableVSync();
                else
                    GameObject.MY_WINDOW.disableVSync();
                success = true;
                break;
            case WATER_EFFECTS:
                Game.setWaterEffects((boolean) command.args.get(0));
                success = true;
                break;
            case MOUSE_SENSITIVITY:
                Game.setMouseSensitivity((float) command.args.get(0));
                success = true;
                break;
            case MUSIC_VOLUME:
                GameObject.getInstance().getMusicPlayer().setGain((float) command.args.get(0));
                success = true;
                break;
            case SOUND_VOLUME:
                GameObject.getInstance().getSoundFXPlayer().setGain((float) command.args.get(0));
                success = true;
                break;  
            case SCREENSHOT:
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
        
        return success;
    }

    @Override
    public Boolean call() throws Exception {
        return Command.execute(this);
    }
   
    public List<Object> getArgs() {
        return args;
    }
    
    public boolean isRendererCommand() {
        if (this == RESOLUTION 
                || this == VSYNC
                || this == SCREENSHOT) {
            return true;
        } else {
            return false;
        }
    }
    
    public static boolean isRendererCommand(Command command) {
        if (command == RESOLUTION 
                || command == VSYNC
                || command == SCREENSHOT) {
            return true;
        } else {
            return false;
        }
    }    

}
