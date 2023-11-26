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

import java.util.Collection;
import java.util.LinkedHashMap;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 * Responsible for rendering the water reflections. Requires more CPU/GPU usage.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class WaterRenderer {

    private final LevelContainer levelContainer;
    private final FrameBuffer frameBuffer = new FrameBuffer(GameObject.MY_WINDOW);
    private final Camera camera;

    public static final int TOP_MASK = 1 << Block.TOP;
    public static final int BOTTOM_MASK = 1 << Block.BOTTOM;

//    private final Quad debugQuad = new Quad(512, 512, frameBuffer.getTexture());
    public WaterRenderer(LevelContainer levelContainer) {
        this.camera = new Camera();
        this.levelContainer = levelContainer;
//        this.debugQuad.setScale(0.25f);
    }

    private Collection<Float> renderedHeights() { // call this in update (renderer)
        Camera actCam = levelContainer.levelActors.mainCamera();
        final float chPosY = actCam.pos.y;

        final LinkedHashMap<Float, Float> deltaMap = new LinkedHashMap<>();
        for (float y : LevelContainer.ALL_BLOCK_MAP.getPlanes().keySet()) {
            float delta = 2.0f * y - chPosY;
            if (delta > 0.0f && delta <= 64.0f && y < chPosY) {
                deltaMap.putIfAbsent(delta, y);
            }
        }

        return deltaMap.values();
    }

    private void prepare() {
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    private void updateClipPlane(float waterHeight) {
        ShaderProgram.getWaterBaseShader().updateUniform(waterHeight, "waterHeight");
    }

    private void updateCamera(float waterHeight) {
        Camera mainCamera = levelContainer.levelActors.mainCamera();

        camera.getPos().x = mainCamera.pos.x;
        camera.getPos().y = 2.0f * waterHeight - mainCamera.pos.y;
        camera.getPos().z = mainCamera.pos.z;
        camera.lookAtAngle(mainCamera.yaw, -mainCamera.pitch);
    }

    private void capture(float waterHeight) {
        GL11.glEnable(GL30.GL_CLIP_DISTANCE0);
        updateClipPlane(waterHeight);
        updateCamera(waterHeight);
        levelContainer.render(camera);
        GL11.glDisable(GL30.GL_CLIP_DISTANCE0);
    }

    /**
     * Render the water reflections to water textures
     */
    public void render() {
        frameBuffer.bind();
        prepare();
        Collection<Float> renderedHeights = renderedHeights();
        for (float waterHeight : renderedHeights) {
            capture(waterHeight);
        }
        frameBuffer.unbind();
//        if (!debugQuad.isBuffered()) {
//            debugQuad.bufferAll();
//        }
//        debugQuad.render(ShaderProgram.getIntrfaceShader());
    }

    public void release() {
        DSLogger.reportDebug("Water Renderer released.", null);
        frameBuffer.getTexture().release();
    }

    public FrameBuffer getFrameBuffer() {
        return frameBuffer;
    }

    public Camera getCamera() {
        return camera;
    }

    public LevelContainer getLevelContainer() {
        return levelContainer;
    }

}
