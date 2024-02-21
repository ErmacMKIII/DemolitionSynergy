/*
 * Copyright (C) 2024 Alexander Stojanovich <coas91@rocketmail.com>
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

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import rs.alexanderstojanovich.evg.level.BlockEnvironment;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.light.LightSource;
import rs.alexanderstojanovich.evg.main.Configuration;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class ShadowRenderer implements CoreRenderer {

    public final Matrix4f lightSpaceMatrix = new Matrix4f().zero();

    public static final float SCALE = 50.0f;
    public static final Vector3f ORIGIN = new Vector3f(0, 0, 2.1f);
    private final GameObject gameObject;

    private final FrameBuffer frameBuffer;
    private final Camera camera = new Camera();
//    private Quad debugQuad;
    public final ShadowBox shadowBox = new ShadowBox();

    public static enum ShadowEffectsQuality {
        NONE, LOW, MEDIUM, HIGH, ULTRA
    };

    private ShadowRenderer.ShadowEffectsQuality effectsQuality = ShadowRenderer.ShadowEffectsQuality.values()[Configuration.getInstance().getShadowEffects()];
    private float shadowDistance = 0.0f;

    public ShadowRenderer(GameObject gameObject) throws Exception {
        this.frameBuffer = new FrameBuffer("Shadow", Texture.Format.DEPTH24, FrameBuffer.Configuration.DEPTH_ATTACHMENT);
        this.gameObject = gameObject;
        setDepthByQuality();
//        this.debugQuad = new Quad(512, 512, frameBuffer.getTexture());
//        this.debugQuad.setScale(0.25f);
//        this.debugQuad.setPos(new Vector2f(-0.5f, 0.5f));
//        this.debugQuad.setColor(GlobalColors.GREEN_RGBA);
    }

    private void setDepthByQuality() {
        switch (effectsQuality) {
            case NONE:
                shadowDistance = 0.0f;
                break;
            case LOW:
                shadowDistance = SCALE;
                break;
            case MEDIUM:
                shadowDistance = 2f * SCALE;
                break;
            case HIGH:
                shadowDistance = 5f * SCALE;
                break;
            case ULTRA:
                shadowDistance = 10f * SCALE;
                break;
        }
    }

    @Override
    public void prepare() {
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
    }

    /**
     * Get (derive) light space matrix
     *
     */
    public void transformToLightSpace() {
        /*In general, an orthographic projection matrix is defined based on the dimensions of the viewing frustum or the area you want to project onto. 
          It maps 3D coordinates to 2D coordinates without perspective, which is useful for rendering objects with uniform size regardless of their distance from the viewer.
          Whether the shadow box needs to be in world space depends on where the shadow mapping calculations are performed and how the shadow box is defined. 
          If you calculate the shadow box in world space, you'll likely transform it to light space before creating the orthographic projection matrix for shadow mapping. 
          This transformation ensures that the shadow mapping is consistent with the position and orientation of the light source. -Chat GPT*/

        final Matrix4f lightProjMatrix = shadowBox.toLightSpace(camera.viewMatrix).projectionMatrix();
        Matrix4f temp = new Matrix4f();
        lightSpaceMatrix.set(lightProjMatrix.mul(camera.viewMatrix, temp));
    }

    /**
     * Update the shadow box
     */
    private void updateShadowBox() {
        final float aspectRatio = gameObject.WINDOW.getAspectRatio();
        ShadowBox.createOrUpdate(
                shadowDistance,
                aspectRatio,
                camera, // light point-of-view
                shadowBox); // shadow box to update
    }

    /**
     * Calculate camera position and view angles based on the light source
     * position
     *
     * @param lightSrcPos light source position
     */
    private void updateCamera(Vector3f lightSrcPos) {
        final float pi = (float) org.joml.Math.PI;

        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f lightDir = lightSrcPos.negate(temp1).normalize(temp2);

        float lightYaw = org.joml.Math.atan2(-lightDir.z, lightDir.x);

        // Ensure lightYaw is within the range 0 to PI
        lightYaw = (lightYaw + 2.0f * (float) pi) % (2.0f * (float) pi);

        float lightPitch = -org.joml.Math.atan2(lightDir.y, org.joml.Math.sqrt(lightDir.x * lightDir.x + lightDir.z * lightDir.z));

        // Ensure lightPitch is within the range -PI/2 to PI/2
        lightPitch = Math.max(-pi / 2.0f, Math.min(lightPitch, pi / 2.0f));

        camera.lookAtAngle(lightYaw, lightPitch);
        camera.setPos(ORIGIN);
    }

    private void capture() {
        gameObject.levelContainer.lightSources.lightSpaceMatrix.set(this.lightSpaceMatrix); // shadow renderer uses light matrix from light source when rendering from the scene
        gameObject.levelContainer.render(camera, ShaderProgram.getShadowBaseShader(), ShaderProgram.getShadowVoxelShader(), BlockEnvironment.LIGHT_MASK);
    }

    /**
     * Render the shadows reflections to texture (frame buffer)
     */
    @Override
    public void render() {
        if (effectsQuality == ShadowRenderer.ShadowEffectsQuality.NONE) {
            return;
        }

        frameBuffer.bind();
        // PASS 1 .. render depth to texture
        prepare();
        final LightSource ls = LevelContainer.SUNLIGHT;
        if (ls.getIntensity() > 0.0f) {
            updateCamera(ls.pos);
            updateShadowBox();
            transformToLightSpace();
            capture();
        }
        FrameBuffer.unbind(gameObject);
        // PASS 2 .. render the scene
//        if (!debugQuad.isBuffered()) {
//            debugQuad.bufferAll(gameObject.intrface);
//        }
//        debugQuad.render(gameObject.intrface, ShaderProgram.getIntrfaceShader());
    }

    @Override
    public void release() {
        frameBuffer.release();
        DSLogger.reportDebug("Shadow Renderer released.", null);
    }

    public ShadowRenderer.ShadowEffectsQuality getEffectsQuality() {
        return effectsQuality;
    }

    public float getShadowDistance() {
        return shadowDistance;
    }

    public GameObject getGameObject() {
        return gameObject;
    }

    public LevelContainer getLevelContainer() {
        return gameObject.levelContainer;
    }

    public FrameBuffer getFrameBuffer() {
        return frameBuffer;
    }

    public Camera getCamera() {
        return camera;
    }

//    public Quad getDebugQuad() {
//        return debugQuad;
//    }
    public Texture texture() {
        return frameBuffer.getTexture();
    }

    public void setEffectsQuality(ShadowRenderer.ShadowEffectsQuality effectsQuality) {
        this.effectsQuality = effectsQuality;
        this.setDepthByQuality();
    }

    public Matrix4f getLightSpaceMatrix() {
        return lightSpaceMatrix;
    }

    public ShadowBox getShadowBox() {
        return shadowBox;
    }

}
