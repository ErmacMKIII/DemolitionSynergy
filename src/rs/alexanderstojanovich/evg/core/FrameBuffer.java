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

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 * Class responsible for "Rendering to Texture"
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class FrameBuffer {

    private static int fbo;
    private final Texture texture = new Texture("FrameBuffer");

    /**
     * Call initilzation of this from Game Renderer (requires OpenGL context to
     * be in that thread)
     */
    public void init() { // requires OpenGL context
        texture.bufferAll(); // loads empty texture to graphics card
        createFrameBuffer();
        createDepthBuffer();
        configureFrameBuffer();
        DSLogger.reportDebug("Water renderer initialized!", null);
    }

    private void createFrameBuffer() {
        // the framebuffer, which regroups 0, 1, or more textures, and 0 or 1 depth buffer.
        fbo = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
    }

    private void createDepthBuffer() {
        // the depth buffer
        int depthRenderBuffer = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthRenderBuffer);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH_COMPONENT24, texture.getImage().getWidth(), texture.getImage().getHeight());
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, depthRenderBuffer);
    }

    private void configureFrameBuffer() {
        // set "renderedTexture" as our colour attachement #0
        GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, texture.getTextureID(), 0);
        // unbinding
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    public void bind() {
        // render to our framebuffer
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL11.glViewport(0, 0, texture.getImage().getWidth(), texture.getImage().getHeight()); // Render on the whole framebuffer, complete from the lower left corner to the upper right
    }

    public static void unbind() {
        // render to the screen
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glViewport(0, 0, GameObject.MY_WINDOW.getWidth(), GameObject.MY_WINDOW.getHeight());
    }

    public int getFbo() {
        return fbo;
    }

    public Texture getTexture() {
        return texture;
    }

}
