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
import rs.alexanderstojanovich.evg.util.Vector3fUtils;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class WaterRenderer {

    private final LevelContainer levelContainer;
    private final Set<Float> waterHeights = new HashSet<>();
    private final FrameBuffer frameBuffer = new FrameBuffer(GameObject.MY_WINDOW);
    private final Camera camera = new Camera();

    public WaterRenderer(LevelContainer levelContainer) {
        this.levelContainer = levelContainer;
    }

    public void refresh() { // call this in update (renderer)        
        if (!levelContainer.isWorking()) {
            Vector3f obsCameraPos = levelContainer.getLevelActors().getPlayer().getCamera().getPos();
            float obsHeight = obsCameraPos.y;
            int currChunkId = Chunk.chunkFunc(obsCameraPos);
            Chunk currChunk = levelContainer.getFluidChunks().getChunk(currChunkId);
            if (currChunk != null) {
                for (Block fluidBlock : currChunk.getList()) {
                    float waterHeight = fluidBlock.getSurfaceY();
                    Vector3f topPos = fluidBlock.getAdjacentPos(Block.TOP);
                    if (fluidBlock.getEnabledFaces()[Block.TOP] // it needs to have enabled top
                            && obsCameraPos.distance(fluidBlock.getPos()) <= Chunk.VISION
                            && !LevelContainer.ALL_SOLID_MAP.containsKey(Vector3fUtils.hashCode(topPos)) // it must be nothing on top of it
                            && waterHeight <= obsHeight) { // and it needs to be below the observer
                        fluidBlock.setWaterTexture(frameBuffer.getTexture()); // it's passed to level Renderer 
                        currChunk.setWaterTexture(frameBuffer.getTexture());
                        waterHeights.add(waterHeight);
                    } else {
                        waterHeights.remove(waterHeight);
                        fluidBlock.setWaterTexture(null);
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
        updateClipPlane(waterHeight);
        updateCamera(waterHeight);
        levelContainer.render(camera);
    }

    public void render() {
        if (!waterHeights.isEmpty() && !levelContainer.isWorking()) {
            GL11.glEnable(GL30.GL_CLIP_DISTANCE0);
            frameBuffer.bind();
            prepare();
            // refresh is called from the update (renderer)
            for (Float height : waterHeights) {
                capture(height);
            }
            frameBuffer.unbind();
            GL11.glDisable(GL30.GL_CLIP_DISTANCE0);

            waterHeights.clear();
        }
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
