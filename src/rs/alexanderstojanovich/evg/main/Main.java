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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import rs.alexanderstojanovich.evg.audio.MasterAudio;
import rs.alexanderstojanovich.evg.models.Chunk;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Main {

    public static final ExecutorService SERVICE = Executors.newSingleThreadExecutor();

    public static void main(String[] args) {
        Configuration inCfg = new Configuration();
        inCfg.readConfigFile(); // this line reads if input file exists otherwise uses defaults
        boolean debug = inCfg.isDebug(); // determine debug flag (write in a log file or not)
        DSLogger.init(debug); // this is important initializing Apache logger
        MasterAudio.init(); // audio init before game loading            
        //----------------------------------------------------------------------                
        boolean ok = GameObject.MY_WINDOW.setResolution(inCfg.getWidth(), inCfg.getHeight());
        if (!ok) {
            DSLogger.reportError("Game unable to set resolution!", null);
        }
        GameObject.MY_WINDOW.centerTheWindow();
        GameObject gameObject = GameObject.getInstance(); // inits it once if null and returns it
        Game game = new Game(inCfg, gameObject); // init game with given configuration and game object
        Renderer renderer = new Renderer(gameObject); // init renderer with given game object
        DSLogger.reportInfo("Game initialized.", null);
        //---------------------------------------------------------------------- 
        SERVICE.execute(new Runnable() {
            @Override
            public void run() {
                renderer.start();
                DSLogger.reportInfo("Renderer started.", null);
            }
        });
        SERVICE.shutdown();
        DSLogger.reportInfo("Game will start soon.", null);
        game.go();
        try {
            renderer.join(); // and it's blocked here until it finishes
        } catch (InterruptedException ex) {
            DSLogger.reportError(ex.getMessage(), ex);
        }
        //----------------------------------------------------------------------        
        Configuration outCfg = game.makeConfig(); // makes configuration from ingame settings
        outCfg.setDebug(debug); // what's on the input carries through the output
        outCfg.writeConfigFile();  // writes configuration to the output file
        MasterAudio.destroy(); // destroy context after writting to the ini file                                
        //---------------------------------------------------------------------- 
        Chunk.deleteCache();
        DSLogger.reportInfo("Game finished.", null);
    }

}
