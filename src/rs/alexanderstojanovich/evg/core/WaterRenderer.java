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

import java.util.HashSet;
import java.util.Set;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.models.Chunk;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class WaterRenderer {

    private final LevelContainer levelContainer;
    private final Set<Float> waterHeights = new HashSet<>();
    private final FrameBuffer frameBuffer = new FrameBuffer(GameObject.MY_WINDOW);
    private final Camera camera = new Camera();
//    private final Quad debugQuad = new Quad(512, 512, frameBuffer.getTexture());

    public WaterRenderer(LevelContainer levelContainer) {
        this.levelContainer = levelContainer;
//        this.debugQuad.setScale(0.25f);
    }

    public synchronized void refresh() { // call this in update (renderer) 
        waterHeights.clear();
        Vector3f playerPredictor = levelContainer.getLevelActors().getPlayer().getPredictor();
        if (!levelContainer.isWorking()) {
            Camera obsCamera = levelContainer.getLevelActors().getPlayer().getCamera();
            float obsHeight = obsCamera.getPos().y;
            Vector3f obsUp = obsCamera.getUp();
            for (Chunk fluidChunk : levelContainer.getFluidChunks().getChunkList()) {
                if (Chunk.invChunkFunc(fluidChunk.getId()).distance(playerPredictor) <= Chunk.VISION) {
                    for (Block fluidBlock : fluidChunk.getBlockList()) {
                        float waterHeight = fluidBlock.getSurfaceY();
                        if (fluidBlock.getEnabledFaces()[Block.TOP]
                                && waterHeight <= obsHeight
                                && fluidBlock.intersectsRay(obsCamera.getPos(), obsUp)) {
                            waterHeights.add(waterHeight);
                        }
                    }
                }
                fluidChunk.setWaterTexture(frameBuffer.getTexture());
            }
        }
    }

    private void prepare() {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    private void updateClipPlane(float waterHeight) {
        ShaderProgram.getWaterBaseShader().updateUniform(waterHeight, "waterHeight");
    }

    private void updateCamera(float waterHeight) {
        camera.getPos().x = levelContainer.getLevelActors().getPlayer().getCamera().getPos().x;
        camera.getPos().y = 2.0f * waterHeight - levelContainer.getLevelActors().getPlayer().getCamera().getPos().y;
        camera.getPos().z = levelContainer.getLevelActors().getPlayer().getCamera().getPos().z;
        camera.lookAt(levelContainer.getLevelActors().getPlayer().getCamera().getYaw(), -levelContainer.getLevelActors().getPlayer().getCamera().getPitch());
    }

    private void capture(float waterHeight) {
        GL11.glEnable(GL30.GL_CLIP_DISTANCE0);
        updateClipPlane(waterHeight);
        updateCamera(waterHeight);
        levelContainer.render(camera);
        GL11.glDisable(GL30.GL_CLIP_DISTANCE0);
    }

    public synchronized void render() {
        frameBuffer.bind();
        prepare();
        if (!waterHeights.isEmpty() && !levelContainer.isWorking()) {
            // refresh is called from the update (renderer)
            for (Float height : waterHeights) {
                capture(height);
            }
        }
        frameBuffer.unbind();

//        if (!debugQuad.isBuffered()) {
//            debugQuad.buffer();
//        }
//        debugQuad.render();
    }

    public Set<Float> getWaterHeights() {
        return waterHeights;
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
