/*
 * Copyright (C) 2019 Coa
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
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Coa
 */
public class Main {

    public static final String TITLE = "Demolition Synergy - v19 TITANIUM";

    public static final Object OBJ_MUTEX = new Object(); // mutex for window, used for game and renderer

    public static final ExecutorService SERVICE = Executors.newSingleThreadExecutor();

    public static void main(String[] args) {
        Configuration inCfg = new Configuration(); // makes default configuration
        inCfg.readConfigFile(); // this line reads if input file exists otherwise uses defaults
        boolean debug = inCfg.isDebug(); // determine debug flag (write in a log file or not)
        DSLogger.init(debug); // this is important initializing Apache logger
        MasterAudio.init(); // audio init before game loading        
        //----------------------------------------------------------------------
        Window myWindow = new Window(inCfg.getWidth(), inCfg.getHeight(), TITLE); // creating the window
        GameObject gameObject = new GameObject(myWindow);
        Game game = new Game(gameObject, inCfg); // init game with given config (or default one)               
        Renderer renderer = new Renderer(gameObject);
        DSLogger.reportInfo("Game initialized.", null);
        //---------------------------------------------------------------------- 
        SERVICE.execute(new Runnable() {
            @Override
            public void run() {
                renderer.start();
                DSLogger.reportInfo("Renderer started.", null);
            }
        });
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
        synchronized (Main.OBJ_MUTEX) {
            gameObject.getMyWindow().loadContext();
            gameObject.getMyWindow().destroy();
        }
        SERVICE.shutdown();
        DSLogger.reportInfo("Game finished.", null);
    }
}
