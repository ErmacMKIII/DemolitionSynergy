/*
 * Copyright (C) 2019 Coa
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

import rs.alexanderstojanovich.evg.texture.Texture;
import java.util.ArrayList;
import java.util.List;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.models.Blocks;
import rs.alexanderstojanovich.evg.models.Chunk;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.util.Tuple;

/**
 *
 * @author Coa
 */
public class WaterRenderer {

    private final Window myWindow;
    private final LevelRenderer levelRenderer;
    private final List<Float> waterHeights = new ArrayList<>();
    private final FrameBuffer frameBuffer;
    private final Camera camera;

    public WaterRenderer(Window window, LevelRenderer levelRenderer) {
        this.myWindow = window;
        this.levelRenderer = levelRenderer;
        this.frameBuffer = new FrameBuffer(myWindow);
        this.camera = new Camera();
    }

    private void refresh() {
        Vector3f obsCameraPos = levelRenderer.getObserver().getCamera().getPos();
        Vector3f obsCameraFront = levelRenderer.getObserver().getCamera().getFront();
        float obsHeight = obsCameraPos.y;
        int currChunkId = Chunk.chunkFunc(obsCameraPos, obsCameraFront);
        Chunk currChunk = levelRenderer.getFluidChunks().getChunk(currChunkId);
        if (currChunk != null) {
            for (Tuple<Blocks, Integer, Integer, Texture, Integer> tuple : currChunk.getTupleList()) {
                for (Block fluidBlock : tuple.getA().getBlockList()) {
                    float waterHeight = fluidBlock.giveSurfacePos();
                    Integer topSolidBlockHashCode = fluidBlock.getAdjacentBlockMap().get(Block.TOP);
                    if (fluidBlock.getEnabledFaces()[Block.TOP] // it needs to have enabled top
                            && obsCameraPos.distance(fluidBlock.getPos()) <= Chunk.B
                            && topSolidBlockHashCode == null // it must be nothing on top of it
                            && waterHeight <= obsHeight) { // and it needs to be below the observer
                        fluidBlock.setTertiaryTexture(frameBuffer.getTexture()); // it's passed to level Renderer 
                        currChunk.setWaterTexture(frameBuffer.getTexture());
                        if (!waterHeights.contains(waterHeight)) {
                            waterHeights.add(waterHeight);
                        }
                    } else {
                        waterHeights.remove(waterHeight);
                        fluidBlock.setTertiaryTexture(null);
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
        camera.getPos().x = levelRenderer.getObserver().getCamera().getPos().x;
        camera.getPos().y = 2.0f * waterHeight - levelRenderer.getObserver().getCamera().getPos().y;
        camera.getPos().z = levelRenderer.getObserver().getCamera().getPos().z;
        camera.lookAt(levelRenderer.getObserver().getCamera().getYaw(), -levelRenderer.getObserver().getCamera().getPitch());
    }

    private void capture(float waterHeight) {
        updateClipPlane(waterHeight);
        updateCamera(waterHeight);
        levelRenderer.render(camera);
    }

    public void render() {
        GL11.glEnable(GL30.GL_CLIP_DISTANCE0);
        frameBuffer.bind();

        prepare();
        refresh();

        for (float height : waterHeights) {
            capture(height);
        }

        frameBuffer.unbind();
        GL11.glDisable(GL30.GL_CLIP_DISTANCE0);
    }

    public Window getMyWindow() {
        return myWindow;
    }

    public LevelRenderer getLevelRenderer() {
        return levelRenderer;
    }

    public List<Float> getWaterHeights() {
        return waterHeights;
    }

    public FrameBuffer getFrameBuffer() {
        return frameBuffer;
    }

    public Camera getCamera() {
        return camera;
    }

}
