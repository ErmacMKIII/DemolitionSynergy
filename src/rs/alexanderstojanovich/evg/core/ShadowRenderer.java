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

    public final Matrix4f projectionMatrix = new Matrix4f();

    public static final float BASE = 25.0f;
    public static final Vector3f ORIGIN = new Vector3f();
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
                ShadowBox.FOV_Factor = 0.0f;
                shadowDistance = 0.0f;
                break;
            case LOW:
                ShadowBox.FOV_Factor = 0.5f;
                shadowDistance = BASE;
                break;
            case MEDIUM:
                ShadowBox.FOV_Factor = 1f;
                shadowDistance = 1.25f * BASE;
                break;
            case HIGH:
                ShadowBox.FOV_Factor = 2.5f;
                shadowDistance = 1.50f * BASE;
                break;
            case ULTRA:
                ShadowBox.FOV_Factor = 4f;
                shadowDistance = 1.75f * BASE;
                break;
        }
    }

    @Override
    public void prepare() {
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
    }

    /**
     * Update the shadow box
     */
    private void updateShadowBox() {
        /*In general, an orthographic projection matrix is defined based on the dimensions of the viewing frustum or the area you want to project onto. 
          It maps 3D coordinates to 2D coordinates without perspective, which is useful for rendering objects with uniform size regardless of their distance from the viewer.
          Whether the shadow box needs to be in world space depends on where the shadow mapping calculations are performed and how the shadow box is defined. 
          If you calculate the shadow box in world space, you'll likely transform it to light space before creating the orthographic projection matrix for shadow mapping. 
          This transformation ensures that the shadow mapping is consistent with the position and orientation of the light source. -Chat GPT*/
        final float aspectRatio = gameObject.WINDOW.getAspectRatio();
        ShadowBox.createOrUpdate(
                shadowDistance,
                aspectRatio,
                camera, // light point-of-view
                shadowBox); // shadow box to update
        projectionMatrix.set(shadowBox.toWorldSpace(camera.viewMatrix).projectionMatrix());
    }

    /**
     * Calculate camera position and view angles based on the light source
     * position & set view matrix for that light source
     *
     * @param lightSrc light source
     */
    private void updateCamera(LightSource lightSrc) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f lightDir = lightSrc.pos.negate(temp1).normalize(temp2);

        float lightYaw = org.joml.Math.atan2(-lightDir.z, lightDir.x);
        float lightPitch = -org.joml.Math.atan2(lightDir.y, org.joml.Math.sqrt(lightDir.x * lightDir.x + lightDir.z * lightDir.z));

        camera.lookAtAngle(lightYaw, lightPitch);
        camera.setPos(ORIGIN);
    }

    /**
     * Render the scene from light source point-of-view
     *
     * @param ls Light Source from which angle is captured or viewed
     */
    private void capture(LightSource ls) {
        updateCamera(ls);
        // shadow renderer uses light matrix from light source when rendering from the scene
        for (ShaderProgram sp : ShaderProgram.SHADOW_SHADERS) {
            sp.bind();
            sp.updateUniform(projectionMatrix, "projectionMatrix");
            sp.updateUniform(camera.viewMatrix, "viewMatrix");
            ShaderProgram.unbind();
        }
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
        updateShadowBox();
        capture(ls);
        FrameBuffer.unbind(gameObject);
        // PASS 2 .. render the scene
        for (ShaderProgram sp : ShaderProgram.ENVIRONMENTAL_SHADERS) {
            sp.bind();
            sp.updateUniform(projectionMatrix, "lightOrthoMatrix");
            sp.updateUniform(camera.viewMatrix, "lightViewMatrix");
            ShaderProgram.unbind();
        }
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

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    public ShadowBox getShadowBox() {
        return shadowBox;
    }

}
