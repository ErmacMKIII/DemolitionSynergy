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
package rs.alexanderstojanovich.evg.core;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;

/**
 *
 * @author Coa
 */
public class MasterRenderer {

    private static GLCapabilities glCaps; // GL context   

    // load GL context into this thread  -> important!
    public static void initGL(Window myWindow) {
        // load context
        myWindow.loadContext();
        // enable/disable vsync
        if (myWindow.isVsync()) {
            myWindow.enableVSync();
        } else {
            myWindow.disableVSync();
        }

        // create openGL context        
        glCaps = GL.createCapabilities();

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);

        GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f); // set the background to black        
    }

    public static void render() {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    public static GLCapabilities getGlCaps() {
        return glCaps;
    }

}
