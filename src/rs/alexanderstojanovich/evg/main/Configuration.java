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
package rs.alexanderstojanovich.evg.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Configuration {

    private int fpsCap = 100;
    private int width = 640;
    private int height = 480;
    private boolean fullscreen = false;
    private boolean vsync = false;
    private int waterEffects = 0;
    private float mouseSensitivity = 1.5f;
    private DSLogger.DSLogLevel logLevel = DSLogger.DSLogLevel.ERR;
    private boolean logToFile = false;
    private float musicVolume = 0.2f;
    private float soundFXVolume = 0.2f;
    private int blockDynamicSize = 50;
    private int textDynamicSize = 10;
    private int textureSize = 512;
    private float gameTimeMultiplier = 1.0f;
    private int rendererPasses = 10;

    private static final String CONFIG_PATH = "dsynergy.ini";

    private static Configuration instance;

    public static Configuration getInstance() {
        if (instance == null) {
            instance = new Configuration();
        }
        return instance;
    }

    private Configuration() {

    }

    // reads configuration from the .ini file
    public void readConfigFile() {
        File cfg = new File(CONFIG_PATH);
        if (cfg.exists()) {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(cfg));
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("#")) {
                        continue; // ignore whole line comments
                    }

                    // ignore inline comments
                    if (line.contains("#")) {
                        line = line.substring(0, line.indexOf("#"));
                    }

                    // replace all white space chars with empty string
                    String[] words = line.replaceAll("\\s", "")
                            .trim()
                            .split("=");
                    int number;
                    float val;
                    if (words.length == 2) {
                        switch (words[0].toLowerCase()) {
                            case "fpscap":
                                fpsCap = Integer.parseInt(words[1]);
                                break;
                            case "width":
                                width = Integer.parseInt(words[1]);
                                break;
                            case "height":
                                height = Integer.parseInt(words[1]);
                                break;
                            case "fullscreen":
                                fullscreen = Boolean.parseBoolean(words[1].toLowerCase());
                                break;
                            case "vsync":
                                vsync = Boolean.parseBoolean(words[1].toLowerCase());
                                break;
                            case "watereffects":
                                waterEffects = Integer.parseInt(words[1].toLowerCase());
                                break;
                            case "mousesensitivity":
                                val = Float.parseFloat(words[1]);
                                if (val >= 0.05f && val <= 20.0f) {
                                    mouseSensitivity = val;
                                }
                                break;
                            case "musicvolume":
                                val = Float.parseFloat(words[1]);
                                if (val >= 0.05f && val <= 20.0f) {
                                    musicVolume = val;
                                }
                                break;
                            case "soundfxvolume":
                                val = Float.parseFloat(words[1]);
                                if (val >= 0.05f && val <= 20.0f) {
                                    soundFXVolume = val;
                                }
                                break;
                            case "loglevel":
                                int logLevelInt = Integer.parseInt(words[1].toLowerCase());
                                logLevel = DSLogger.DSLogLevel.values()[logLevelInt];
                                break;
                            case "logtofile":
                                logToFile = Boolean.parseBoolean(words[1].toLowerCase());
                                break;
                            case "blockdynamicsize":
                                number = Integer.parseInt(words[1]);
                                if (number >= 3 && number <= 5000) {
                                    blockDynamicSize = number;
                                }
                                break;
                            case "textdynamicsize":
                                number = Integer.parseInt(words[1]);
                                if (number >= 3 && number <= 1000) {
                                    textDynamicSize = number;
                                }
                                break;
                            case "rendererpasses":
                                number = Integer.parseInt(words[1]);
                                if (number > 0 && number <= Game.TPS * 2) {
                                    rendererPasses = number;
                                }
                            case "texturesize":
                                number = Integer.parseInt(words[1]);
                                // if tex size is a non-zero power of two
                                if (number != 0 && (number & (number - 1)) == 0 && number <= 4096) {
                                    textureSize = number;
                                }
                                break;
                            case "gametimemultiplier":
                                val = Float.parseFloat(words[1]);
                                if (val >= 0.0f && val <= 5.0f) {
                                    gameTimeMultiplier = val;
                                }
                        }
                    }
                }
            } catch (FileNotFoundException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            } catch (IOException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException ex) {
                        DSLogger.reportFatalError(ex.getMessage(), ex);
                    }
                }
            }
        }
    }

    // writes configuration to the .ini file (on game exit)
    public void writeConfigFile() {
        File cfg = new File(CONFIG_PATH);
        if (cfg.exists()) {
            cfg.delete();
        }
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(cfg);
            pw.println("# Maximum framerate. Depedends on VSync.");
            pw.println("FPSCap = " + fpsCap);
            pw.println("Width = " + width);
            pw.println("Height = " + height);
            pw.println("Fullscreen = " + fullscreen);
            pw.println("# Maximum Framerate set to refresh rate if enabled");
            pw.println("VSync = " + vsync);
            pw.println("# Water Effects (e.g. water reflections) {NONE=0, LOW=1, MEDIUM=2, HIGH=3, ULTRA=4}");
            pw.println("WaterEffects = " + waterEffects);
            pw.println("MouseSensitivity = " + mouseSensitivity);
            pw.println("MusicVolume = " + musicVolume);
            pw.println("SoundFXVolume = " + soundFXVolume);
            pw.println("# Log Level {ERR=0(default), DEBUG=1, ALL=2}");
            pw.println("LogLevel = " + logLevel.ordinal());
            pw.println("# If true generate log file, otherwise print only to console. Used in conjuction with log level.");
            pw.println("LogToFile = " + logToFile);
            pw.println("BlockDynamicSize = " + blockDynamicSize);
            pw.println("TextDynamicSize = " + textDynamicSize);
            pw.println("# Renderer strength. May Improve performance. Number passes per ticks. Allowed values 1-160");
            pw.println("RendererPasses = " + rendererPasses);
            pw.println("# Texture size. Must be power of two, non-zero and lesser or equal than 4096.");
            pw.println("TextureSize = " + textureSize);
            pw.println("# Game Time (decimal). Must be metween (0, 5]");
            pw.println("GameTimeMultiplier = " + gameTimeMultiplier);
        } catch (FileNotFoundException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } finally {
            if (pw != null) {
                pw.close();
            }
        }
    }

    public int getFpsCap() {
        return fpsCap;
    }

    public void setFpsCap(int fpsCap) {
        this.fpsCap = fpsCap;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
    }

    public boolean isVsync() {
        return vsync;
    }

    public void setVsync(boolean vsync) {
        this.vsync = vsync;
    }

    public int getWaterEffects() {
        return waterEffects;
    }

    public void setWaterEffects(int waterEffects) {
        this.waterEffects = waterEffects;
    }

    public float getMouseSensitivity() {
        return mouseSensitivity;
    }

    public void setMouseSensitivity(float mouseSensitivity) {
        this.mouseSensitivity = mouseSensitivity;
    }

    public DSLogger.DSLogLevel getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(DSLogger.DSLogLevel logLevel) {
        this.logLevel = logLevel;
    }

    public boolean isLogToFile() {
        return logToFile;
    }

    public void setLogToFile(boolean logToFile) {
        this.logToFile = logToFile;
    }

    public float getMusicVolume() {
        return musicVolume;
    }

    public void setMusicVolume(float musicVolume) {
        this.musicVolume = musicVolume;
    }

    public float getSoundFXVolume() {
        return soundFXVolume;
    }

    public void setSoundFXVolume(float soundFXVolume) {
        this.soundFXVolume = soundFXVolume;
    }

    public int getBlockDynamicSize() {
        return blockDynamicSize;
    }

    public int getTextDynamicSize() {
        return textDynamicSize;
    }

    public int getTextureSize() {
        return textureSize;
    }

    public float getGameTimeMultiplier() {
        return gameTimeMultiplier;
    }

    public int getRendererPasses() {
        return rendererPasses;
    }

}
