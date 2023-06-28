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
package rs.alexanderstojanovich.evg.core;

import org.joml.Vector3f;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.main.Configuration;

/**
 * Responsible for primitive rendering. OpenGL initialization happens here.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class MasterRenderer {

    private static GLCapabilities glCaps; // GL context   

    // load GL context into this thread  -> important!
    /**
     * Initializes OpenGL into this thread and configures it.Notice that OpenGL
     * is being rendered in the Window. Call only from Renderer.
     *
     * @param myWindow window associated with rendering.
     * @param cfg ingame configuration
     */
    public static void initGL(Window myWindow, Configuration cfg) {
        // load context
        myWindow.loadContext();

        // create openGL context        
        glCaps = GL.createCapabilities();

        // enable/disable vsync
        myWindow.setVSync(cfg.isVsync());

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);

        GL11.glClearColor(LevelContainer.SKYBOX_COLOR.x, LevelContainer.SKYBOX_COLOR.y, LevelContainer.SKYBOX_COLOR.z, 1.0f);
    }

    public static void setResolution(int width, int height) {
        GL11.glViewport(0, 0, width, height);
    }

    /**
     * Render by clearing the color (COLOR_BUFFER_BIT | DEPTH_BUFFER_BIT)
     */
    public static void render() {
        Vector3f skyColor = LevelContainer.SKYBOX.getPrimaryRGBColor();
        GL11.glClearColor(skyColor.x, skyColor.y, skyColor.z, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    public static GLCapabilities getGlCaps() {
        return glCaps;
    }

}
