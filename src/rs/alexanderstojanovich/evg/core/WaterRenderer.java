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

import java.util.LinkedHashMap;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.main.Configuration;
import rs.alexanderstojanovich.evg.main.GameObject;
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

    public static enum WaterEffectsQuality {
        NONE, LOW, MEDIUM, HIGH, ULTRA
    };
    private WaterEffectsQuality effectsQuality = WaterEffectsQuality.values()[Configuration.getInstance().getWaterEffects()];
    private int maxWaterDepthSize = 3;

    public static final IList<Float> WATER_HEIGHTS = new GapList<>();
//    private final Quad debugQuad = new Quad(512, 512, frameBuffer.getTexture());

    public WaterRenderer(LevelContainer levelContainer) {
        this.camera = new Camera();
        this.levelContainer = levelContainer;
        this.setDepthByQuality();
    }

    private void setDepthByQuality() {
        switch (effectsQuality) {
            case LOW:
                maxWaterDepthSize = 3;
                break;
            case MEDIUM:
                maxWaterDepthSize = 5;
                break;
            case HIGH:
                maxWaterDepthSize = 8;
                break;
            case ULTRA:
                maxWaterDepthSize = 12;
                break;
        }
    }

    public void updateHeights() { // call this in update (renderer)
        if (effectsQuality == WaterEffectsQuality.NONE) {
            return;
        }

        Camera actCam = levelContainer.levelActors.mainCamera();
        final float chPosY = actCam.pos.y;

        synchronized (WATER_HEIGHTS) {
            WATER_HEIGHTS.clear();
        }

        final LinkedHashMap<Float, Float> deltaMap = new LinkedHashMap<>();
        OUTER:
        for (float y : LevelContainer.ALL_BLOCK_MAP.getPlanes().keySet()) {
            float delta = 2.0f * y - chPosY;
            if (delta > 0.0f && delta <= 64.0f && y < chPosY) {
                IList<Vector2f> xzVals = LevelContainer.ALL_BLOCK_MAP.getPlanes().get(y);
                for (Vector2f xz : xzVals) {
                    float x = xz.x;
                    float z = xz.y;
                    Vector3f value = new Vector3f(x, y, z);
                    float angleCos = actCam.pos.angleCos(value);
                    if (angleCos > 0.1f && angleCos <= 0.5f && deltaMap.size() <= maxWaterDepthSize) {
                        deltaMap.putIfAbsent(delta, y);
                    }

                    if (deltaMap.size() >= maxWaterDepthSize) {
                        break OUTER;
                    }
                }
            }

            if (deltaMap.size() >= maxWaterDepthSize) {
                break OUTER;
            }
        }

        synchronized (WATER_HEIGHTS) {
            WATER_HEIGHTS.addAll(deltaMap.values());
        }

//        System.err.println(WATER_HEIGHTS.size());
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
        if (effectsQuality == WaterEffectsQuality.NONE) {
            return;
        }

        frameBuffer.bind();
        prepare();
        synchronized (WATER_HEIGHTS) {
            for (float waterHeight : WATER_HEIGHTS) {
                capture(waterHeight);
            }
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

    public WaterEffectsQuality getEffectsQuality() {
        return effectsQuality;
    }

    public void setEffectsQuality(WaterEffectsQuality effectsQuality) {
        this.effectsQuality = effectsQuality;
        this.setDepthByQuality();
    }

}
