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

/**
 * Class responsible for "Rendering to Texture"
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class FrameBuffer {

    private int fbo;
    private final Texture texture;

    public static final int COLOR_ATTACHMENT = GL30.GL_COLOR_ATTACHMENT0;
    public static final int DEPTH_ATTACHMENT = GL30.GL_DEPTH_ATTACHMENT;

    private final int cfgFlag;

    /**
     * Create Frame Buffer. Used by shadow renderer (depth only) & water
     * renderer (color & depth)
     *
     * @param texName texture name
     * @param texFmtFlag texture format flag {RGBA8, RGB5A1 or DEPTH24}
     * @param cfgFlag must be either color attachment or depth attachment
     * @throws java.lang.Exception if invalid config flag is provided
     */
    public FrameBuffer(String texName, int texFmtFlag, int cfgFlag) throws Exception {
        this.texture = new Texture(texName, texFmtFlag);
        this.cfgFlag = cfgFlag;
        if (cfgFlag != COLOR_ATTACHMENT && cfgFlag != DEPTH_ATTACHMENT) {
            throw new Exception("Invalid attachment was provided");
        }
    }

    /**
     * Call initialization of this from Game Renderer (requires OpenGL context
     * to be in that thread)
     */
    public void initBuffer() { // requires OpenGL context        
        texture.bufferAll(); // loads empty texture to graphics card
        createFrameBuffer();
        createRenderBuffer();
        if (cfgFlag == COLOR_ATTACHMENT || cfgFlag == DEPTH_ATTACHMENT) {
            createColorOrDepthBuffer(cfgFlag);
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
     * Create depth buffer (always)
     */
    private void createRenderBuffer() {
        // the depth buffer
        int depthRenderBuffer = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthRenderBuffer);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH_COMPONENT24, texture.getImage().getWidth(), texture.getImage().getHeight());
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, depthRenderBuffer);
    }

    /**
     * Create color buffer (color textures) or depth only (shadow)
     *
     * @param attachmentType
     */
    private void createColorOrDepthBuffer(int attachmentType) {
        // set "renderedTexture" as our colour attachement #0
        GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, attachmentType, texture.getTextureID(), 0);
    }

    public void bind() {
        // render to our framebuffer
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL11.glViewport(0, 0, texture.getImage().getWidth(), texture.getImage().getHeight()); // Render on the whole framebuffer, complete from the lower left corner to the upper right
    }

    public static void unbind(GameObject gameObject) {
        // render to the screen
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glViewport(0, 0, gameObject.WINDOW.getWidth(), gameObject.WINDOW.getHeight());
    }

    public int getFbo() {
        return fbo;
    }

    public Texture getTexture() {
        return texture;
    }

    public int getCfgFlag() {
        return cfgFlag;
    }

}
