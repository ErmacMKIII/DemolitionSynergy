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

import java.util.LinkedHashMap;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.chunk.Chunk;
import rs.alexanderstojanovich.evg.level.BlockEnvironment;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.main.Configuration;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.MathUtils;

/**
 * Responsible for rendering the water reflections. Requires more CPU/GPU usage.
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class WaterRenderer implements CoreRenderer {

    private final FrameBuffer frameBuffer;
    private final Camera camera = new Camera();

    public static final float BASE = Chunk.LENGTH / 4f;

    public static enum WaterEffectsQuality {
        NONE, LOW, MEDIUM, HIGH, ULTRA
    };
    private WaterEffectsQuality effectsQuality = WaterEffectsQuality.values()[Configuration.getInstance().getWaterEffects()];
    private int maxWaterDepthSize = 3;
    public final IList<Float> waterHeights = new GapList<>();
    protected final GameObject gameObject;
//    private final Quad debugQuad;

    public float distance = 0.0f;

    public WaterRenderer(GameObject gameObject) throws Exception {
        this.frameBuffer = new FrameBuffer("Water", Texture.Format.RGB5_A1, FrameBuffer.Configuration.COLOR_ATTACHMENT);
        this.gameObject = gameObject;
        this.setDepthByQuality();
//        this.debugQuad = new Quad(512, 512, frameBuffer.getTexture());
//        this.debugQuad.setScale(0.25f);
//        this.debugQuad.setPos(new Vector2f(0.5f, 0.5f));
    }

    private void setDepthByQuality() {
        switch (effectsQuality) {
            case LOW:
                maxWaterDepthSize = 3;
                distance = BASE;
                break;
            case MEDIUM:
                maxWaterDepthSize = 5;
                distance = 2.0f * BASE;
                break;
            case HIGH:
                maxWaterDepthSize = 8;
                distance = 3.5f * BASE;
                break;
            case ULTRA:
                maxWaterDepthSize = 12;
                distance = 4.0f * BASE;
                break;
        }
    }

    public void updateWaterHeights() { // call this in update (renderer)
        if (effectsQuality == WaterEffectsQuality.NONE) {
            return;
        }

        // player must notice that water heights are updating!
        waterHeights.clear();

        Camera actCam = gameObject.levelContainer.levelActors.mainCamera();
        Vector3f temp = new Vector3f();
        final Vector3f frontNeg = actCam.front.negate(temp);
        final float chPosY = actCam.pos.y;

        float dotYAxis = frontNeg.dot(Camera.Y_AXIS);
        if (dotYAxis > 0.0f) {
            final LinkedHashMap<Float, Float> deltaMap = new LinkedHashMap<>();
            OUTER:
            for (Vector3f xyzLoc : LevelContainer.AllBlockMap.getPopulatedLocations(tb -> !tb.solid && ((~tb.byteValue & Block.Y_MASK) != 0), actCam.pos, distance)) {
                if (chPosY >= xyzLoc.y) {
                    float delta = 2.0f * xyzLoc.y - chPosY;
                    float angleCos = actCam.pos.angleCos(xyzLoc);
                    float angleDeg = MathUtils.toDegrees(MathUtils.acos(angleCos));
                    if (angleDeg >= 5.0f && angleDeg < 90.0f && deltaMap.size() <= 2 * maxWaterDepthSize) {
                        deltaMap.putIfAbsent(delta, xyzLoc.y);
                    }
                }
                if (deltaMap.size() >= 2 * maxWaterDepthSize) {
                    break OUTER;
                }
            }

            float avgWaterHeight;
            float sum = 0.0f;

            IList<Float> values = new GapList<>(deltaMap.values());

            // binary sampler
            while (values.size() > maxWaterDepthSize) {
                for (float value : values) {
                    sum += value;
                }
                avgWaterHeight = sum / (float) values.size();
                IList<Float> tmpList = new GapList<>();
                for (int i = 0; i <= values.size() - 2; i += 2) {
                    float a = values.get(i);
                    float b = values.get(i + 1);
                    float halftwo = (a + b) / 2.0f;
                    float delta = halftwo - avgWaterHeight;
                    if (delta >= -2.0f && delta <= 2.0f) {
                        tmpList.addIfAbsent(halftwo);
                    } else {
                        tmpList.addIfAbsent(Math.max(a, b));
                    }
                }
                values = new GapList<>(tmpList);
            }

            waterHeights.addAll(values);
        }
    }

    @Override
    public void prepare() {
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    private void updateClipPlane(float waterHeight) {
        for (ShaderProgram sp : ShaderProgram.WATER_SHADERS) {
            sp.bind();
            sp.updateUniform(waterHeight, "waterHeight");
            ShaderProgram.unbind();
        }
    }

    /**
     * Update camera position and view angles based on water height
     *
     * @param waterHeight water height parameter
     */
    private void updateCamera(float waterHeight) {
        Camera mainCamera = gameObject.levelContainer.levelActors.mainCamera();

        camera.pos.x = mainCamera.pos.x;
        camera.pos.y = 2.0f * waterHeight - mainCamera.pos.y;
        camera.pos.z = mainCamera.pos.z;
        camera.lookAtAngle(mainCamera.yaw, -mainCamera.pitch);
    }

    /**
     * Render the scene from water camera point-of-view
     *
     * @param waterHeight water height to update camera position & angle
     */
    private void capture(float waterHeight) {
        updateClipPlane(waterHeight);
        updateCamera(waterHeight);
        gameObject.levelContainer.render(camera, ShaderProgram.getWaterBaseShader(), ShaderProgram.getWaterVoxelShader(), BlockEnvironment.LIGHT_MASK);
    }

    /**
     * Render the water reflections to water textures
     */
    @Override
    public void render() {
        if (effectsQuality == WaterEffectsQuality.NONE) {
            return;
        }

        updateWaterHeights();
//        DSLogger.reportInfo("Size=" + waterHeights.size(), null);

        frameBuffer.bind();
        // PASS 1 .. render color & depth to texture
        prepare();
        GL11.glEnable(GL30.GL_CLIP_DISTANCE0);
        for (float waterHeight : waterHeights) {
            capture(waterHeight);
        }
        GL11.glDisable(GL30.GL_CLIP_DISTANCE0);
        FrameBuffer.unbind(gameObject);
//        if (!debugQuad.isBuffered()) {
//            debugQuad.bufferAll(gameObject.intrface);
//        }
//        debugQuad.render(gameObject.intrface, ShaderProgram.getIntrfaceShader());
    }

    @Override
    public void release() {
        frameBuffer.release();
        DSLogger.reportDebug("Water Renderer released.", null);
    }

    public FrameBuffer getFrameBuffer() {
        return frameBuffer;
    }

    public Camera getCamera() {
        return camera;
    }

    public LevelContainer getLevelContainer() {
        return gameObject.levelContainer;
    }

    public WaterEffectsQuality getEffectsQuality() {
        return effectsQuality;
    }

    public void setEffectsQuality(WaterEffectsQuality effectsQuality) {
        this.effectsQuality = effectsQuality;
        this.setDepthByQuality();
    }

    public Texture texture() {
        return frameBuffer.getTexture();
    }

    public int getMaxWaterDepthSize() {
        return maxWaterDepthSize;
    }

    public IList<Float> getWaterHeights() {
        return waterHeights;
    }

    public GameObject getGameObject() {
        return gameObject;
    }

    public float getDistance() {
        return distance;
    }

}
