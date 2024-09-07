/*
 * Copyright (C) 2023 Alexander Stojanovich <coas91@rocketmail.com>
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
package rs.alexanderstojanovich.evg.light;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.intrface.Intrface;
import rs.alexanderstojanovich.evg.intrface.Quad;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.GlobalColors;

/**
 * Project lights to the screen. Makes feel of light environment.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class LightOverlay extends Quad {

    public LightOverlay(int width, int height, Texture texture) {
        super(width, height, texture);
    }

    public LightOverlay(int width, int height, Texture texture, boolean ignoreFactor) {
        super(width, height, texture, ignoreFactor);
    }

    public void render(Intrface intrface, Camera camera, LevelContainer lc, ShaderProgram shaderProgram) {
        if (enabled && buffered) {
            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);

            shaderProgram.bind();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * 4, 0); // this is for intrface pos
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * 4, 8); // this is for intrface uv 
            shaderProgram.bindAttribute(0, "pos");
            shaderProgram.bindAttribute(1, "uv");
            lc.lightSources.updateLightsInShaderIfModified(shaderProgram);

            Matrix4f modelMatrix = calcModelMatrix(intrface);
            shaderProgram.updateUniform(modelMatrix, "modelMatrix");
            camera.updateCameraPosition(shaderProgram);
            camera.updateCameraFront(shaderProgram);
            shaderProgram.updateUniform(scale, "scale");

            if (LevelContainer.isActorInFluidChk(lc)) {
                texture.bind(shaderProgram, "ifcTexture");
                color = new Vector4f(GlobalColors.BLUE, 0.05f);
            } else {
                Texture.EMPTY.bind(shaderProgram, "ifcTexture");
                color = GlobalColors.TRANSPARENT;
            }
            shaderProgram.updateUniform(color, "color");

            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
            GL11.glDrawElements(GL11.GL_TRIANGLES, INDICES.length, GL11.GL_UNSIGNED_INT, 0);

            Texture.unbind(0);
            ShaderProgram.unbind();

            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
        }
    }

}
