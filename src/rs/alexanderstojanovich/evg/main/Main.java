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

import rs.alexanderstojanovich.evg.audio.MasterAudio;
import rs.alexanderstojanovich.evg.level.CacheModule;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Main {

    public static void main(String[] args) {
        CacheModule.deleteCache();
        Configuration inCfg = Configuration.getInstance();
        inCfg.readConfigFile(); // this line reads if input file exists otherwise uses defaults
        boolean debug = inCfg.isDebug(); // determine debug flag (write in a log file or not)
        DSLogger.init(debug); // this is important initializing Apache logger
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
        Configuration outCfg = GameObject.game.makeConfig(); // makes configuration from ingame settings
        outCfg.setDebug(debug); // what's on the input carries through the output
        outCfg.writeConfigFile();  // writes configuration to the output file
        GameObject.destroy(); // destroy window alongside with the OpenGL context
        MasterAudio.destroy(); // destroy context after writting to the ini file                                
        //---------------------------------------------------------------------- 
        CacheModule.deleteCache();
        DSLogger.reportInfo("Game finished.", null);
    }

}
