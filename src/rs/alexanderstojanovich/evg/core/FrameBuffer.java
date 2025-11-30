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
package rs.alexanderstojanovich.evg.core;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.texture.Texture;

/**
 * Class responsible for "Rendering to Texture"
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class FrameBuffer {

    private int fbo;
    private final Texture texture;

    private int depthRenderBuff = 0;

    public static enum Configuration {
        COLOR_ATTACHMENT, DEPTH_ATTACHMENT
    }

    private final Configuration cfg;

    /**
     * Create Frame Buffer. Used by shadow renderer (depth only) & water
     * renderer (color & depth)
     *
     * @param texName texture name
     * @param texFormat texture format flag {RGBA8, RGB5A1 or DEPTH24}
     * @param cfg must be either color attachment or depth attachment
     * @throws java.lang.Exception if invalid config flag is provided
     */
    public FrameBuffer(String texName, Texture.Format texFormat, Configuration cfg) throws Exception {
        this.texture = new Texture(texName, texFormat);
        this.cfg = cfg;
        if (this.cfg != Configuration.COLOR_ATTACHMENT && this.cfg != Configuration.DEPTH_ATTACHMENT) {
            throw new Exception("Invalid attachment was provided");
        }
    }

    /**
     * Call initialization of this from Game Renderer (requires OpenGL context
     * to be in that thread)
     *
     * @throws java.lang.Exception if buffer cannot be created
     */
    public void initBuffer() throws Exception { // requires OpenGL context        
        // loads empty texture to graphics card
        Texture.bufferAll(texture, null);
        createFrameBuffer();

        switch (cfg) {
            case COLOR_ATTACHMENT: // Water Frame Buffer
                configureColorTexture();
                createDepth();
                break;

            case DEPTH_ATTACHMENT: // Shadow Frame Buffer
                configureDepthTexture();
                break;
        }

        // where color buffer is written
        if (cfg == Configuration.COLOR_ATTACHMENT) {
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
        } else {
            GL11.glDrawBuffer(GL11.GL_NONE);
            GL11.glReadBuffer(GL11.GL_NONE);
        }

        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new Exception("Could not create FrameBuffer");
        }

        // unbind so the configuration is not ruined
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    private void createFrameBuffer() {
        // the framebuffer, which regroups 0, 1, or more textures, and 0 or 1 depth buffer.
        fbo = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
    }

    /**
     * Create color buffer (conditional, water renderer)
     */
    private void configureColorTexture() {
        // the depth buffer
        GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, texture.getTextureID(), 0);
    }

    /**
     * Create color buffer (conditional, shadow renderer)
     */
    private void configureDepthTexture() {
        // the depth buffer
        GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, texture.getTextureID(), 0);
    }

    /**
     * Create depth buffer (always)
     */
    private void createDepth() {
        // the depth buffer
        depthRenderBuff = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthRenderBuff);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH_COMPONENT24, texture.getImage().getWidth(), texture.getImage().getHeight());
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, depthRenderBuff);
    }

    public void bind() {
        // render to our framebuffer
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL11.glViewport(0, 0, texture.getImage().getWidth(), texture.getImage().getHeight()); // Render on the whole framebuffer, complete from the lower left corner to the upper right
    }

    public static void unbind(GameObject gameObject) {
        // render to the screen
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glViewport(0, 0, gameObject.gameWindow.getWidth(), gameObject.gameWindow.getHeight());
    }

    public void release() {
        GL30.glDeleteFramebuffers(fbo);
        GL30.glDeleteRenderbuffers(depthRenderBuff);
        texture.release();
    }

    public int getFbo() {
        return fbo;
    }

    public Texture getTexture() {
        return texture;
    }

}
