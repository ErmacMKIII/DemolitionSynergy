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
import javax.imageio.ImageIO;
import org.lwjgl.opengl.GL;
import rs.alexanderstojanovich.evg.audio.AudioPlayer;
import rs.alexanderstojanovich.evg.core.MasterRenderer;
import rs.alexanderstojanovich.evg.core.PerspectiveRenderer;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.main.Renderer;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Coa
 */
public class Commands {

    private final Window myWindow;
    private final Object objMutex; // it's window related
    private final AudioPlayer musicPlayer;
    private final AudioPlayer soundFXPlayer;

    public Commands(Window myWindow, Object objMutex, AudioPlayer musicPlayer, AudioPlayer soundFXPlayer) {
        this.myWindow = myWindow;
        this.objMutex = objMutex;
        this.musicPlayer = musicPlayer;
        this.soundFXPlayer = soundFXPlayer;
    }

    public boolean execute(String command) {
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
                            Renderer.setFpsTicks(0.0);
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
                case "waterEffects":
                case "water_effects":
                    if (things.length == 2) {
                        if (Boolean.parseBoolean(things[1])) {
                            Game.setWaterEffects(true);
                        } else {
                            Game.setWaterEffects(false);
                        }
                        success = true;
                    }
                    break;
                case "msens":
                case "mouse_sensitivity":
                    if (things.length == 2) {
                        Game.setMouseSensitivity(Float.parseFloat(things[1]));
                        success = true;
                    }
                    break;
                case "music":
                case "musicVolume":
                    if (things.length == 2) {
                        float volume = Float.parseFloat(things[1]);
                        if (volume >= 0.0f && volume <= 1.0f) {
                            musicPlayer.setGain(volume);
                            success = true;
                        }
                    }
                    break;
                case "sound":
                case "soundVolume":
                    if (things.length == 2) {
                        float volume = Float.parseFloat(things[1]);
                        if (volume >= 0.0f && volume <= 1.0f) {
                            soundFXPlayer.setGain(volume);
                            success = true;
                        }
                    }
                    break;
                case "quit":
                case "exit":
                    myWindow.close();
                    success = true;
                    break;
                case "screenshot":
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
                    synchronized (objMutex) {
                        myWindow.loadContext();
                        GL.setCapabilities(MasterRenderer.getGlCaps());
                        try {
                            ImageIO.write(myWindow.getScreen(), "PNG", screenshot);
                        } catch (IOException ex) {
                            DSLogger.reportError(ex.getMessage(), ex);
                        }
                        GL.setCapabilities(null);
                        Window.unloadContext();
                    }
                    success = true;
                    break;
                default:
                    success = false;
                    break;
            }
        }
        return success;
    }

    public Window getMyWindow() {
        return myWindow;
    }

    public Object getObjMutex() {
        return objMutex;
    }

    public AudioPlayer getMusicPlayer() {
        return musicPlayer;
    }

    public AudioPlayer getSoundFXPlayer() {
        return soundFXPlayer;
    }

}
