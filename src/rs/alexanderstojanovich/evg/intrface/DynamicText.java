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
package rs.alexanderstojanovich.evg.intrface;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.List;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;
import rs.alexanderstojanovich.evg.core.Texture;
import rs.alexanderstojanovich.evg.core.Window;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.util.Pair;

/**
 *
 * @author Coa
 */
public class DynamicText extends Text {

    private int bigVbo; // vbo containing all the quads (characters)
    private int[] vboEntries;
    private final List<Pair<Float, Float>> pairList = new LinkedList<>(); // pairs xinc, ydec
    private static final Vector2f[] VERTICES = new Vector2f[4]; //            
    private static final int[] INDICES = {0, 1, 2, 3};
    private static final IntBuffer CONST_INT_BUFFER = BufferUtils.createIntBuffer(6);

    static {
        VERTICES[0] = new Vector2f(-1.0f, -1.0f);
        VERTICES[1] = new Vector2f(1.0f, -1.0f);
        VERTICES[2] = new Vector2f(1.0f, 1.0f);
        VERTICES[3] = new Vector2f(-1.0f, 1.0f);

        for (int i : INDICES) {
            CONST_INT_BUFFER.put(i);
        }
        CONST_INT_BUFFER.flip();
    }

    public DynamicText(Window window, Texture texture, String content) {
        super(window, texture, content);
        buffer();
    }

    public DynamicText(Window window, Texture texture, String content, Vector3f color, Vector2f pos) {
        super(window, texture, content, color, pos);
        buffer();
    }

    public DynamicText(Window window, Texture texture, String content, Vector2f pos, int charWidth, int charHeight) {
        super(window, texture, content, pos, charWidth, charHeight);
        buffer();
    }

    private void buffer() {
        pairList.clear();
        FloatBuffer bigFloatBuff = BufferUtils.createFloatBuffer(content.length() * Quad.VERTEX_COUNT * Quad.VERTEX_SIZE);
        String[] lines = content.split("\n");
        vboEntries = new int[1024];
        int e = 0;
        int offset = 0;
        for (int l = 0; l < lines.length; l++) {
            for (int i = 0; i < lines[l].length(); i++) {
                vboEntries[e++] = offset;
                int j = i % 64;
                int k = i / 64;
                int asciiCode = (int) (lines[l].charAt(i));

                float cellU = (int) (asciiCode % GRID_SIZE) * CELL_SIZE;
                float cellV = (int) (asciiCode / GRID_SIZE) * CELL_SIZE;

                float xinc = j;
                float ydec = k + l * LINE_SPACING;

                pairList.add(new Pair<>(xinc, ydec));

                Vector2f[] uvs = new Vector2f[4];

                for (int v = 0; v < 4; v++) {
                    uvs[v] = new Vector2f();
                }

                uvs[0].x = cellU;
                uvs[0].y = cellV + CELL_SIZE;

                uvs[1].x = cellU + CELL_SIZE;
                uvs[1].y = cellV + CELL_SIZE;

                uvs[2].x = cellU + CELL_SIZE;
                uvs[2].y = cellV;

                uvs[3].x = cellU;
                uvs[3].y = cellV;

                for (int v = 0; v < Quad.VERTEX_COUNT; v++) {
                    bigFloatBuff.put(VERTICES[v].x);
                    bigFloatBuff.put(VERTICES[v].y);
                    bigFloatBuff.put(uvs[v].x);
                    bigFloatBuff.put(uvs[v].y);
                    offset++;
                }

            }
        }
        bigFloatBuff.flip();
        bigVbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bigFloatBuff, GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    @Override
    public void render() {
        if (enabled) {
            float relWidth = quad.giveRelativeWidth();
            float relHeight = quad.giveRelativeHeight();
            Texture.enable();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bigVbo);
            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * 4, 0); // this is for intrface pos
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * 4, 8); // this is for intrface uv            
            for (int k = 0; k < content.length(); k++) {
                ShaderProgram.getIntrfaceShader().bind();
                ShaderProgram.getIntrfaceShader().updateUniform(quad.getPos(), "trans");
                ShaderProgram.getIntrfaceShader().updateUniform(relWidth, "width");
                ShaderProgram.getIntrfaceShader().updateUniform(relHeight, "height");
                ShaderProgram.getIntrfaceShader().updateUniform(quad.getScale(), "scale");
                ShaderProgram.getIntrfaceShader().updateUniform(quad.getColor(), "color");
                texture.bind(0, ShaderProgram.getIntrfaceShader(), "texture0");

                Pair<Float, Float> pair = pairList.get(vboEntries[k] >> 2);
                float xinc = (float) pair.getKey();
                float ydec = (float) pair.getValue();

                ShaderProgram.getIntrfaceShader().updateUniform(xinc, "xinc");
                ShaderProgram.getIntrfaceShader().updateUniform(ydec, "ydec");

                GL32.glDrawElementsBaseVertex(GL11.GL_QUADS, CONST_INT_BUFFER, vboEntries[k]);
            }
            Texture.unbind(0);
            ShaderProgram.unbind();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL20.glDisableVertexAttribArray(0);
            GL20.glDisableVertexAttribArray(1);
            Texture.disable();

        }
    }

    public int getBigVbo() {
        return bigVbo;
    }

    public List<Pair<Float, Float>> getPairList() {
        return pairList;
    }

    @Override
    public void setContent(String content) {
        super.setContent(content);
        buffer();
    }

}
