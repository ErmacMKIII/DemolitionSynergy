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

import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.audio.MasterAudio;
import rs.alexanderstojanovich.evg.cache.CacheModule;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Main {

    public static void main(String[] args) {
        // JOML configuration
        System.setProperty("joml.nounsafe", "true");
        System.setProperty("joml.fastmath", "true");

        CacheModule.deleteCache();
        Configuration inCfg = Configuration.getInstance();
        inCfg.readConfigFile(); // this line reads if input file exists otherwise uses defaults
        IList<String> argsList = new GapList();
        for (String arg : args) {
            argsList.add(arg);
        }
        final boolean logToFile = (argsList.contains("-logtofile") || inCfg.isLogToFile()); // determine debug flag (write in a log file or not)
        String arg = argsList.getIf(a -> a.equals("-" + DSLogger.DSLogLevel.ERR.name()) || a.equals("-" + DSLogger.DSLogLevel.DEBUG.name()) || a.equals("-" + DSLogger.DSLogLevel.ALL.name()));
        final DSLogger.DSLogLevel logLevel;
        if (arg == null) {
            logLevel = inCfg.getLogLevel();
        } else {
            logLevel = DSLogger.DSLogLevel.valueOf(arg.replaceFirst("-", ""));
        }
        DSLogger.init(logLevel, logToFile); // this is important initializing Apache logger        
        DSLogger.INTERNAL_LOGGER.log(DSLogger.INTERNAL_LOGGER.getLevel(), "Logging level: " + logLevel);
        if (logToFile) {
            DSLogger.reportDebug("Logging to file set.", null);
        }
        MasterAudio.init(); // audio init before game loading            
        //----------------------------------------------------------------------                        
        GameObject.init();
        boolean ok = GameObject.MY_WINDOW.setResolution(inCfg.getWidth(), inCfg.getHeight());
        if (!ok) {
            DSLogger.reportError("Game unable to set resolution!", null);
        }
        GameObject.MY_WINDOW.centerTheWindow();
        GameObject.start();
        //----------------------------------------------------------------------        
        Configuration outCfg = Game.makeConfig(); // makes configuration from ingame settings
        outCfg.writeConfigFile();  // writes configuration to the output file
        DSLogger.reportDebug("Writing configuration done.", null);
        GameObject.destroy(); // destroy window alongside with the OpenGL context        
        MasterAudio.destroy(); // destroy context after writting to the ini file 
        DSLogger.reportDebug("Finalize game & release resources done.", null);
        //---------------------------------------------------------------------- 
        CacheModule.deleteCache();
        DSLogger.reportDebug("Game finished.", null);
    }

}
