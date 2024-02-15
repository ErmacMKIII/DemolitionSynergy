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
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class ShadowRenderer implements CoreRenderer {

    private final GameObject gameObject;

    private final FrameBuffer frameBuffer;
    private final Camera camera = new Camera();
    private Quad debugQuad;
    protected ShadowBox shadowBox = new ShadowBox().zero();

    public static enum ShadowEffectsQuality {
        NONE, LOW, MEDIUM, HIGH, ULTRA
    };

    private ShadowRenderer.ShadowEffectsQuality effectsQuality = ShadowRenderer.ShadowEffectsQuality.values()[Configuration.getInstance().getShadowEffects()];
    private float shadowDistance = 500.0f;

    public ShadowRenderer(GameObject gameObject) throws Exception {
        this.frameBuffer = new FrameBuffer("Shadow", Texture.Format.DEPTH24, FrameBuffer.Configuration.DEPTH_ATTACHMENT);
        this.gameObject = gameObject;
        setDepthByQuality();
        this.debugQuad = new Quad(512, 512, frameBuffer.getTexture());
        this.debugQuad.setScale(0.25f);
        this.debugQuad.setPos(new Vector2f(-0.5f, 0.5f));
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
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    /**
     * Call externally in game object
     */
    public void update() {
        shadowBox = ShadowBox.createOrUpdate(shadowDistance, gameObject.levelContainer.levelActors.mainCamera());
    }

    private void updateCamera(Vector3f lightSrcPos) {
        camera.setPos(new Vector3f(lightSrcPos));

        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();
        Vector3f lightDir = lightSrcPos.negate(temp1).normalize(temp2);
        float lightYaw = org.joml.Math.atan2(-lightDir.z, lightDir.x);
        float lightPitch = org.joml.Math.atan2(lightDir.y, org.joml.Math.sqrt(lightDir.x * lightDir.x + lightDir.z * lightDir.z));

        camera.lookAtAngle(-lightYaw, lightPitch);
//        DSLogger.reportInfo("yaw=" + org.joml.Math.toDegrees(lightYaw), null);
//        DSLogger.reportInfo("pitch=" + org.joml.Math.toDegrees(lightPitch), null);
    }

    private void capture(Vector3f lightSrcPos) {
        updateCamera(lightSrcPos);
        gameObject.levelContainer.render(camera, ShaderProgram.getShadowBaseShader(), ShaderProgram.getShadowVoxelShader(), 0);
    }

    /**
     * Render the shadows reflections to texture (frame buffer)
     */
    @Override
    public void render() {
        if (effectsQuality == ShadowRenderer.ShadowEffectsQuality.NONE) {
            return;
        }

        // PASS 1 .. render depth to texture
        frameBuffer.bind();
        prepare();
        for (LightSource ls : gameObject.levelContainer.lightSources.sourceList) {
            if (ls.pos != gameObject.levelContainer.levelActors.mainActor().getPos()) {
                capture(ls.pos);
            }
        }
        FrameBuffer.unbind(gameObject);
        // PASS 2 .. render the scene        

        if (!debugQuad.isBuffered()) {
            debugQuad.bufferAll(gameObject.intrface);
        }
        debugQuad.render(gameObject.intrface, ShaderProgram.getIntrfaceShader());
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
