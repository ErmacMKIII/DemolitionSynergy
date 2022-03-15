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

import java.util.Queue;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.magicwerk.brownies.collections.GapList;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.main.GameObject;
import rs.alexanderstojanovich.evg.models.Chunk;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class WaterRenderer {

    private final LevelContainer levelContainer;
    private final Queue<Float> waterHeights = new GapList<>();
    private final FrameBuffer frameBuffer = new FrameBuffer(GameObject.MY_WINDOW);
    private final Camera camera = new Camera();

//    private final Quad debugQuad = new Quad(512, 512, frameBuffer.getTexture());
    public WaterRenderer(LevelContainer levelContainer) {
        this.levelContainer = levelContainer;
//        this.debugQuad.setScale(0.25f);
    }

    private void refresh() { // call this in update (renderer) 
        if (!levelContainer.isWorking()) {
            Camera actCam = levelContainer.getLevelActors().mainCamera();
            Vector3f actCamPos = actCam.getPos();
            Vector3f catCamFront = actCam.getFront();

            Vector3f temp = new Vector3f();
            for (int id = 0; id < Chunk.CHUNK_NUM; id++) {
                Vector3f chunkPos = Chunk.invChunkFunc(id);
                float product = chunkPos.sub(actCamPos, temp).normalize(temp).dot(catCamFront);
                float distance = chunkPos.distance(actCamPos);
                if (chunkPos.distance(actCamPos) <= Chunk.VISION / 8.0f) {
                    Chunk fluidChunk = levelContainer.getFluidChunks().getChunk(id);
                    if (fluidChunk != null && distance <= Chunk.VISION && product >= 0.25f) {
                        float hMin = Math.round(chunkPos.y) & 0xFFFFFFFE;
                        float hMax = Math.round(actCamPos.y) & 0xFFFFFFFE;
                        float hStep = Math.round(hMax - hMin) << 4;

                        if (hMax > hMin && hStep > 0.0f) {
                            for (float h = hMin; h <= hMax; h += hStep) {
                                if (!waterHeights.contains(h)) {
                                    waterHeights.add(h);
                                }
                            }
                        }
                        fluidChunk.setWaterTexture(frameBuffer.getTexture());
                    }

                }
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

    public void render() {
        frameBuffer.bind();
        prepare();
        if (!levelContainer.isWorking()) {
            // refresh is called from the update (renderer)           
            Float height;
            while ((height = waterHeights.poll()) != null) {
                capture(height);
            }
            refresh();
        }
        frameBuffer.unbind();
//        if (!debugQuad.isBuffered()) {
//            debugQuad.buffer();
//        }
//        debugQuad.render();
    }

    public Queue<Float> getWaterHeights() {
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
