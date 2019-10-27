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

import java.util.ArrayList;
import java.util.List;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import rs.alexanderstojanovich.evg.models.Block;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 *
 * @author Coa
 */
public class WaterRenderer {

    private final Window myWindow;
    private final LevelRenderer levelRenderer;
    private List<Float> waterHeights = new ArrayList<>();
    private FrameBuffer frameBuffer;
    private final Camera camera;
//    private Quad quad;

    public WaterRenderer(Window window, LevelRenderer levelRenderer) {
        this.myWindow = window;
        this.levelRenderer = levelRenderer;
        this.frameBuffer = new FrameBuffer(myWindow);
        PerspectiveRenderer.updatePerspective(myWindow.getWidth(), myWindow.getHeight(), ShaderProgram.getWaterBaseShader());
        this.camera = new Camera();
//        this.quad = new Quad(myWindow, 400, 300, frameBuffer.getTexture());
//        this.quad.setScale(0.25f);
//        this.quad.getPos().x = -0.85f * 0.95f;
//        this.quad.getPos().y = -0.7f;
//        quad.setEnabled(true);
    }

    private void refresh() {
        waterHeights.clear();
        if (FrameBuffer.getTexture() == null) {
            frameBuffer = new FrameBuffer(myWindow);
        }
        float obsHeight = levelRenderer.getObserver().getCamera().getPos().y;
        for (Block fluidBlock : levelRenderer.getFluidBlocks().getBlockList()) {
            float waterHeight = fluidBlock.giveSurfacePos();
            Block topSolidBlock = fluidBlock.getAdjacentBlockMap().get(Block.TOP);
            if (fluidBlock.getEnabledFaces()[Block.TOP] // it needs to have enabled top
                    && topSolidBlock == null // it must be nothing on top of it
                    && waterHeight <= obsHeight) { // and it needs to be below the observer
                fluidBlock.setTertiaryTexture(FrameBuffer.getTexture()); // it's passed to level Renderer
                if (!waterHeights.contains(waterHeight)) {
                    waterHeights.add(waterHeight);
                }
            } else {
                fluidBlock.setTertiaryTexture(null);
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
        GL11.glEnable(GL30.GL_CLIP_DISTANCE0);
        updateClipPlane(waterHeight);
        frameBuffer.bind();
        prepare();
        updateCamera(waterHeight);
        levelRenderer.render(camera);
        frameBuffer.unbind();
        GL11.glDisable(GL30.GL_CLIP_DISTANCE0);
    }

    public void render() {
        refresh();
        float minHeight = Float.NEGATIVE_INFINITY;
        for (float height : waterHeights) {
            if (minHeight < height) {
                minHeight = height;
            }
        }
        capture(minHeight);
    }

    public void removeEffects() {
        FrameBuffer.setTexture(null);
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

    public void setWaterHeights(ArrayList<Float> waterHeights) {
        this.waterHeights = waterHeights;
    }

    public FrameBuffer getFrameBuffer() {
        return frameBuffer;
    }

    public void setWaterHeights(List<Float> waterHeights) {
        this.waterHeights = waterHeights;
    }

    public void setFrameBuffer(FrameBuffer frameBuffer) {
        this.frameBuffer = frameBuffer;
    }

    public Camera getCamera() {
        return camera;
    }

//    public Quad getQuad() {
//        return quad;
//    }
//
//    public void setQuad(Quad quad) {
//        this.quad = quad;
//    }            
}
