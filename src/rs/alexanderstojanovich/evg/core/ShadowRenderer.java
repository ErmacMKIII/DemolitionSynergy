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

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import rs.alexanderstojanovich.evg.intrface.Quad;
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

    private final GameObject gameObject;

    private final FrameBuffer frameBuffer = new FrameBuffer("Shadow", false);
    private final Camera camera = new Camera();
    private Quad debugQuad;
    protected ShadowBox shadowBox = new ShadowBox().zero();

    public static enum ShadowEffectsQuality {
        NONE, LOW, MEDIUM, HIGH, ULTRA
    };

    private ShadowRenderer.ShadowEffectsQuality effectsQuality = ShadowRenderer.ShadowEffectsQuality.values()[Configuration.getInstance().getShadowEffects()];
    private float shadowDistance = 500.0f;

    public ShadowRenderer(GameObject gameObject) {
        this.gameObject = gameObject;
        setDepthByQuality();
        this.debugQuad = new Quad(512, 512, frameBuffer.getTexture());
        this.debugQuad.setScale(0.5f);
        this.debugQuad.setPos(new Vector2f(1.0f, -1.0f));
    }

    private void setDepthByQuality() {
        switch (effectsQuality) {
            case NONE:
                shadowDistance = 0.0f;
            case LOW:
                shadowDistance = 250f;
                break;
            case MEDIUM:
                shadowDistance = 500f;
                break;
            case HIGH:
                shadowDistance = 1000f;
                break;
            case ULTRA:
                shadowDistance = 2000f;
                break;
        }
    }

    @Override
    public void prepare() {
        GL11.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
    }

    /**
     * Call externally in game object
     */
    public void updateShadowBox() {
        shadowBox = ShadowBox.createOrUpdate(shadowDistance, camera.viewMatrix, gameObject.levelContainer.levelActors.mainCamera());
    }

    private void updateCamera(Vector3f lightSrcPos) {
        camera.setPos(lightSrcPos);

        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f lightDir = lightSrcPos.negate(temp1).normalize(temp2);

        float lightYaw = org.joml.Math.atan2(lightDir.x, -lightDir.z); // negative cotangent
        float lightPitch = org.joml.Math.asin(lightDir.y);

        camera.lookAtAngle(lightYaw, lightPitch);
    }

    private void capture(Vector3f lightSrcPos) {
        updateCamera(lightSrcPos);
        gameObject.levelContainer.render(camera, ShaderProgram.getShadowBaseShader(), ShaderProgram.getShadowVoxelShader(), BlockEnvironment.LIGHT_MASK);
    }

    /**
     * Render the shadows reflections to texture (frame buffer)
     */
    @Override
    public void render() {
        frameBuffer.bind();
        prepare();
        for (LightSource ls : gameObject.levelContainer.lightSources.sourceList) {
            capture(ls.pos);
        }
        if (!debugQuad.isBuffered()) {
            debugQuad.bufferAll(gameObject.intrface);
        }
        debugQuad.render(gameObject.intrface, ShaderProgram.getIntrfaceShader());
        FrameBuffer.unbind(gameObject);
    }

    @Override
    public void release() {
        frameBuffer.getTexture().release();
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

    public Quad getDebugQuad() {
        return debugQuad;
    }

    public Texture texture() {
        return frameBuffer.getTexture();
    }

    public ShadowBox getShadowBox() {
        return shadowBox;
    }

    public void setEffectsQuality(ShadowRenderer.ShadowEffectsQuality effectsQuality) {
        this.effectsQuality = effectsQuality;
        this.setDepthByQuality();
    }

}
