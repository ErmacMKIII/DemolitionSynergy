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

import rs.alexanderstojanovich.evg.audio.MasterAudio;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Coa
 */
public class Main {

    public static void main(String[] args) {
        Configuration inCfg = new Configuration(); // makes default configuration
        inCfg.readConfigFile(); // this line reads if input file exists otherwise uses defaults
        boolean debug = inCfg.isDebug(); // determine debug flag (write in a log file or not)
        DSLogger.init(debug); // this is important initializing Apache logger
        MasterAudio.init(); // audio init before game loading
        Game game = new Game(inCfg); // init game with given config (or default one)       
        DSLogger.reportInfo("Game initialized.", null);
        game.go(); // starts the game (launchs the threads)         
        DSLogger.reportInfo("Game finished.", null);
        Configuration outCfg = game.makeConfig(); // makes configuration from ingame settings
        outCfg.setDebug(debug); // what's on the input carries through the output
        outCfg.writeConfigFile();  // writes configuration to the output file
        MasterAudio.destroy(); // destroy context after writting to the ini file
    }
}
