/* 
 * Copyright (C) 2020 Aleksandar Stojanovic <coas91@rocketmail.com>
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
import org.joml.Vector3f;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.GlobalColors;

/**
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class Configuration {

    private int monitor = 0;
    private int fpsCap = 100;
    private int width = 640;
    private int height = 480;
    private boolean fullscreen = false;
    private boolean vsync = false;
    private int waterEffects = 0;
    private int shadowEffects = 0;
    private float mouseSensitivity = 10f;
    private DSLogger.DSLogLevel logLevel = DSLogger.DSLogLevel.ERR;
    private boolean logToFile = false;
    private float musicVolume = 0.2f;
    private float soundFXVolume = 0.2f;
    private int blockDynamicSize = 50;
    private int textDynamicSize = 10;
    private int textureSize = 512;
    private float gameTimeMultiplier = 1.0f;
    private int rendererPasses = 16;
    private int optimizationPasses = 4;
    private double gameTicks = 0.0;
    private int blocksPerRun = 1000;
    private int ticksPerUpdate = Game.TPS_TWO;
    private String name = "Player";
    private String model = "alex";
    private Vector3f color = new Vector3f(GlobalColors.WHITE);

    private String localIP = "127.0.0.1"; // local IP address used to host the server on (local) machine
    private String serverIP = ""; // server ip used to connect client to server
    private int serverPort = 13667; // used in conjunction with local IP
    private int clientPort = 13667; // used in conjunction with server IP
    private int maxClients = 4; // max number of clients connected (server logic)

    private boolean useBakGuid = false;

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
                            case "monitor":
                                monitor = Integer.parseInt(words[1]);
                                break;
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
                            case "shadoweffects":
                                shadowEffects = Integer.parseInt(words[1].toLowerCase());
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
                                break;
                            case "optimizationpasses":
                                number = Integer.parseInt(words[1]);
                                if (number != 0 && (number & (number - 1)) == 0 && number <= 64) {
                                    optimizationPasses = number;
                                }
                                break;
                            case "texturesize":
                                number = Integer.parseInt(words[1]);
                                // if tex size is a non-zero power of two
                                if (number != 0 && (number & (number - 1)) == 0 && number <= 4096) {
                                    textureSize = number;
                                }
                                break;
                            case "gameticks":
                                val = Float.parseFloat(words[1]);
                                if (val >= 0.0f) {
                                    gameTicks = val;
                                }
                                break;
                            case "gametimemultiplier":
                                val = Float.parseFloat(words[1]);
                                if (val >= 0.0f && val <= 5.0f) {
                                    gameTimeMultiplier = val;
                                }
                                break;
                            case "blocksperrun":
                                number = Integer.parseInt(words[1]);
                                // block per cache loading run
                                if (number > 0 && number <= 25000) {
                                    blocksPerRun = number;
                                }
                                break;
                            case "ticksperupdate":
                                number = Integer.parseInt(words[1]);
                                // block per cache loading run
                                if (number > 0 && number <= 3) {
                                    ticksPerUpdate = number;
                                }
                                break;
                            case "localip":
                                localIP = words[1];
                                break;
                            case "serverip":
                                serverIP = words[1];
                                break;
                            case "serverport":
                                number = Integer.parseInt(words[1]);
                                // server port is constrained on local machine
                                if (number >= 13660 && number <= 13669) {
                                    serverPort = number;
                                }
                                break;
                            case "clientport":
                                number = Integer.parseInt(words[1]);
                                // clent port is free
                                if (number > 0) {
                                    clientPort = number;
                                }
                                break;
                            case "maxclients":
                                number = Integer.parseInt(words[1]);
                                // max authorised clients on the server
                                if (number > 0 && number <= 4) {
                                    maxClients = number;
                                }
                                break;
                            case "usebakguid":
                                useBakGuid = Boolean.parseBoolean(words[1].toLowerCase());
                                break;
                            case "name":
                                name = words[1].toLowerCase();
                                break;
                            case "model":
                                model = words[1].toLowerCase();
                                break;
                            case "color":
                                String[] split = words[1].trim().split("^\\(|,|\\)$");
                                float red = Float.parseFloat(split[1]);
                                float green = Float.parseFloat(split[2]);
                                float blue = Float.parseFloat(split[3]);
                                color = new Vector3f(red, green, blue);
                                break;
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
        try (PrintWriter pw = new PrintWriter(cfg)) {
            pw.println("# Monitor (0 - Window; !=0 available monitors)");
            pw.println("Monitor = " + monitor);
            pw.println("# Maximum framerate. Dependents on VSync.");
            pw.println("FPSCap = " + fpsCap);
            pw.println("Width = " + width);
            pw.println("Height = " + height);
            pw.println("Fullscreen = " + fullscreen);
            pw.println("# Maximum framerate set to refresh rate if is enabled");
            pw.println("VSync = " + vsync);
            pw.println("# Water Effects (e.g. water reflections) {NONE=0, LOW=1, MEDIUM=2, HIGH=3, ULTRA=4}");
            pw.println("WaterEffects = " + waterEffects);
            pw.println("# Shadow Effects (e.g. shadow effects) {NONE=0, LOW=1, MEDIUM=2, HIGH=3, ULTRA=4}");
            pw.println("ShadowEffects = " + shadowEffects);
            pw.println("MouseSensitivity = " + mouseSensitivity);
            pw.println("MusicVolume = " + musicVolume);
            pw.println("SoundFXVolume = " + soundFXVolume);
            pw.println("# Log Level {ERR=0(default), DEBUG=1, ALL=2}");
            pw.println("LogLevel = " + logLevel.ordinal());
            pw.println("# If true generate log file, otherwise print only to console. Used in conjunction with log level.");
            pw.println("LogToFile = " + logToFile);
            pw.println("BlockDynamicSize = " + blockDynamicSize);
            pw.println("TextDynamicSize = " + textDynamicSize);
            pw.println("# Renderer strength. May Improve performance. Number passes per ticks. Allowed values 1-160");
            pw.println("RendererPasses = " + rendererPasses);
            pw.println("# Optimization strength. May Improve performance. Number passes per optimization run. Allowed values 1-64");
            pw.println("OptimizationPasses = " + optimizationPasses);
            pw.println("# Texture size. Must be power of two, non-zero and lesser or equal than 4096.");
            pw.println("TextureSize = " + textureSize);
            pw.println("# Game Ticks (decimal). Must be greater or equal zero");
            pw.println("GameTicks = " + gameTicks);
            pw.println("# Game Time (decimal). Must be between (0, 5]");
            pw.println("GameTimeMultiplier = " + gameTimeMultiplier);
            pw.println("# Block number load per cache run. Must be between (0, 25000]");
            pw.println("BlocksPerRun = " + blocksPerRun);
            pw.println("# Ticks per update (1 - FLUID, 2 - EFFICIENT, 3 - SAVING)");
            pw.println("TicksPerUpdate = " + ticksPerUpdate);
            pw.println("# Local IP address used to host the server on (local) machine. Used with server port.");
            pw.println("LocalIP = " + localIP);
            pw.println("# Preferred game server (local or public IP address) for client to connect. Used in conjunction with client port.");
            pw.println("ServerIP = " + serverIP);
            pw.println("# Preferred game server port (to run the server). Must be in range 13660-13669");
            pw.println("ServerPort = " + serverPort);
            pw.println("# Client port set to connect to game server. Varying.");
            pw.println("ClientPort = " + clientPort);
            pw.println("# Max server clients.");
            pw.println("MaxClients = " + maxClients);
            pw.println("# Use backup guid. Testing purposes.");
            pw.println("UseBakGuid = " + useBakGuid);
            pw.println("# Player name - Multiplayer");
            pw.println("Name = " + name);
            pw.println("# Player color");
            pw.println("Color = " + String.format("(%.2f, %.2f, %.2f)", color.x, color.y, color.z));
            pw.println("# Player model {alex, steve}");
            pw.println("Model = " + model);
        } catch (FileNotFoundException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        }
    }

    public int getMonitor() {
        return monitor;
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

    public double getGameTicks() {
        return gameTicks;
    }

    public int getBlocksPerRun() {
        return blocksPerRun;
    }

    public int getOptimizationPasses() {
        return optimizationPasses;
    }

    public int getShadowEffects() {
        return shadowEffects;
    }

    public void setShadowEffects(int shadowEffects) {
        this.shadowEffects = shadowEffects;
    }

    public int getTicksPerUpdate() {
        return ticksPerUpdate;
    }

    public String getServerIP() {
        return serverIP;
    }

    public void setServerIP(String serverIP) {
        this.serverIP = serverIP;
    }

    public int getServerPort() {
        return serverPort;
    }

    public int getClientPort() {
        return clientPort;
    }

    public String getLocalIP() {
        return localIP;
    }

    public void setLocalIP(String localIP) {
        this.localIP = localIP;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public void setClientPort(int clientPort) {
        this.clientPort = clientPort;
    }

    public boolean isUseBakGuid() {
        return useBakGuid;
    }

    public String getName() {
        return name;
    }

    public String getModel() {
        return model;
    }

    public Vector3f getColor() {
        return color;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setColor(Vector3f color) {
        this.color = color;
    }

    public int getMaxClients() {
        return maxClients;
    }

}
