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
package rs.alexanderstojanovich.evg.models;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.joml.FrustumIntersection;
import org.joml.Intersectionf;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.core.Camera;
import rs.alexanderstojanovich.evg.level.LevelContainer;
import rs.alexanderstojanovich.evg.light.LightSources;
import rs.alexanderstojanovich.evg.location.TexByte;
import rs.alexanderstojanovich.evg.main.Game;
import static rs.alexanderstojanovich.evg.main.Game.Direction.BACKWARD;
import static rs.alexanderstojanovich.evg.main.Game.Direction.DOWN;
import static rs.alexanderstojanovich.evg.main.Game.Direction.FORWARD;
import static rs.alexanderstojanovich.evg.main.Game.Direction.LEFT;
import static rs.alexanderstojanovich.evg.main.Game.Direction.RIGHT;
import static rs.alexanderstojanovich.evg.main.Game.Direction.UP;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.BlockUtils;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.GlobalColors;
import rs.alexanderstojanovich.evg.util.MathUtils;
import rs.alexanderstojanovich.evg.util.ModelUtils;
import rs.alexanderstojanovich.evg.util.VectorFloatUtils;

/**
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class Block extends Model {

    public static final int Z_MASK = 0x20;
    public static final int ZNEG_MASK = 0x10;
    public static final int Y_MASK = 0x08;
    public static final int YNEG_MASK = 0x04;
    public static final int X_MASK = 0x02;
    public static final int XNEG_MASK = 0x01;

    public static final int NONE = -1;
    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int BOTTOM = 2;
    public static final int TOP = 3;
    public static final int BACK = 4;
    public static final int FRONT = 5;

    public static final int LEFT_BOTTOM = 6;        // -x-y
    public static final int RIGHT_BOTTOM = 7;      // +x-y
    public static final int LEFT_TOP = 8;         //  -x+y  
    public static final int RIGHT_TOP = 9;       //   +x+y

    public static final int BOTTOM_BACK = 10;    // -y-z
    public static final int BOTTOM_FRONT = 11;   // -y+z
    public static final int TOP_BACK = 12;       // +y+-z
    public static final int TOP_FRONT = 13;      // +y+z

    // which faces we enabled for rendering and which we disabled
    private final boolean[] enabledFaces = new boolean[6];

    private boolean verticesReversed = false;

    protected int id = 0;

    public static final Vector3f[] FACE_NORMALS = {
        new Vector3f(-1.0f, 0.0f, 0.0f),
        new Vector3f(1.0f, 0.0f, 0.0f),
        new Vector3f(0.0f, -1.0f, 0.0f),
        new Vector3f(0.0f, 1.0f, 0.0f),
        new Vector3f(0.0f, 0.0f, -1.0f),
        new Vector3f(0.0f, 0.0f, 1.0f)
    };

    public static final int VERTEX_COUNT = 24;
    public static final int INDICES_COUNT = 36;

    public static final List<Vertex> VERTICES = new GapList<>();
    public static final List<Integer> INDICES = new ArrayList<>();

    public static final Comparator<Block> UNIQUE_BLOCK_CMP = new Comparator<Block>() {
        @Override
        public int compare(Block o1, Block o2) {
            Integer a = o1.id;
            Integer b = o2.id;
            return a.compareTo(b);
        }
    };

    public static final Comparator<Block> Y_AXIS_COMP = new Comparator<Block>() {
        @Override
        public int compare(Block o1, Block o2) {
            if (o1.getPos().y > o2.getPos().y) {
                return 1;
            } else if (o1.getPos().y == o2.getPos().y) {
                return 0;
            } else {
                return -1;
            }
        }
    };

    static {
        BlockUtils.readFromTxtFileMK2("cubex.txt");
    }

    public Block(String texName) {
        super("cubex.txt", texName);
        this.solid = !texName.equals("water") && !texName.equals("cloud");
        Arrays.fill(enabledFaces, true);
        final Mesh mesh = new Mesh();
        deepCopyTo(mesh, texName);
        meshes.add(mesh);
        Material material = new Material(Texture.getOrDefault(texName));
        material.color = new Vector4f(GlobalColors.WHITE, solid ? 1.0f : 0.5f);
        materials.add(material);
        width = height = depth = 2.0f;
        id = genId();
    }

    public Block(String texName, Vector3f pos, Vector4f primaryRGBAColor, boolean solid) {
        super("cubex.txt", texName, pos, solid);
        Arrays.fill(enabledFaces, true);
        final Mesh mesh = new Mesh();
        deepCopyTo(mesh, texName);
        meshes.add(mesh);
        Material material = new Material(Texture.getOrDefault(texName));
        material.color = primaryRGBAColor;
        materials.add(material);
        this.solid = solid;
        width = height = depth = 2.0f;
        id = genId();
    }

    public Block(Model other) {
        super(other);
    }

    // cuz regular shallow copy doesn't work, for List of integers is applicable
    public static void deepCopyTo(IList<Vertex> vertices, String texName) {
        int texGridSize = Texture.getOrDefaultGridSize(texName);
        int texIndex = Texture.getOrDefaultIndex(texName);
        int row = texIndex / texGridSize;
        int col = texIndex % texGridSize;
        final float oneOver = 1.0f / (float) texGridSize;
        for (Vertex v : VERTICES) {
            vertices.add(new Vertex(
                    new Vector3f(v.getPos()),
                    new Vector3f(v.getNormal()),
                    (texIndex == -1)
                            ? new Vector2f(v.getUv().x, v.getUv().y)
                            : new Vector2f((v.getUv().x + row) * oneOver, (v.getUv().y + col) * oneOver))
            );
        }
    }

    // cuz regular shallow copy doesn't work, for List of integers is applicable
    public static void deepCopyTo(Mesh mesh, String texName) {
        int texGridSize = Texture.getOrDefaultGridSize(texName);
        int texIndex = Texture.getOrDefaultIndex(texName);
        int row = texIndex / texGridSize;
        int col = texIndex % texGridSize;
        final float oneOver = 1.0f / (float) texGridSize;

        mesh.vertices.clear();
        for (Vertex v : VERTICES) {
            mesh.vertices.add(new Vertex(
                    new Vector3f(v.getPos()),
                    new Vector3f(v.getNormal()),
                    (texIndex == -1)
                            ? new Vector2f(v.getUv().x, v.getUv().y)
                            : new Vector2f((v.getUv().x + row) * oneOver, (v.getUv().y + col) * oneOver))
            );
        }
        mesh.indices.clear();
        mesh.indices.addAll(INDICES);
        mesh.unbuffer();
    }

    /**
     * Render multiple blocks old fashion way.
     *
     * @param blocks models to render
     * @param texName texture name (uses map to find texture)
     * @param vbo common vbo
     * @param ibo common ibo
     * @param indicesNum num of indices (from the tuple)
     * @param lightSrc light source
     * @param shaderProgram shaderProgram for the models
     */
    public static void render(List<Block> blocks, String texName, int vbo, int ibo, int indicesNum, LightSources lightSrc, ShaderProgram shaderProgram) {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);

        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glEnableVertexAttribArray(2);

        if (shaderProgram != null) {
            shaderProgram.bind();

            shaderProgram.bindAttribute(0, "pos");
            shaderProgram.bindAttribute(1, "normal");
            shaderProgram.bindAttribute(2, "uv");

            Texture primaryTexture = Texture.getOrDefault(texName);
            if (primaryTexture != null) { // this is primary texture
                primaryTexture.bind(0, shaderProgram, "modelTexture0");
            }

            lightSrc.updateLightsInShaderIfModified(shaderProgram);

            for (Block block : blocks) {
                block.transform(shaderProgram);
                block.primaryColor(shaderProgram);

                GL11.glDrawElements(GL11.GL_TRIANGLES, indicesNum, GL11.GL_UNSIGNED_INT, 0);
            }
            Texture.unbind(0);
        }
        ShaderProgram.unbind();

        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL20.glDisableVertexAttribArray(2);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    /**
     * Render multiple blocks old fashion way.
     *
     * @param blocks models to render
     * @param texName texture name (uses map to find texture)
     * @param vbo common vbo
     * @param ibo common ibo
     * @param indicesNum num of indices (from the tuple)
     * @param lightSrc light source
     * @param shaderProgram shaderProgram for the models
     * @param predicate predicate which tells if block is visible or not
     */
    public static void renderIf(List<Block> blocks, String texName, int vbo, int ibo, int indicesNum, LightSources lightSrc, ShaderProgram shaderProgram, Predicate<Block> predicate) {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);

        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glEnableVertexAttribArray(2);

        if (shaderProgram != null) {
            shaderProgram.bind();

            shaderProgram.bindAttribute(0, "pos");
            shaderProgram.bindAttribute(1, "normal");
            shaderProgram.bindAttribute(2, "uv");

            Texture primaryTexture = Texture.TEX_STORE.get(texName).getTexture();
            if (primaryTexture != null) { // this is primary texture
                primaryTexture.bind(0, shaderProgram, "modelTexture0");
            }

            lightSrc.updateLightsInShaderIfModified(shaderProgram);

            for (Block block : blocks) {
                if (predicate.test(block)) {
                    block.transform(shaderProgram);
                    block.primaryColor(shaderProgram);

                    GL11.glDrawElements(GL11.GL_TRIANGLES, indicesNum, GL11.GL_UNSIGNED_INT, 0);
                }
            }
            Texture.unbind(0);
        }
        ShaderProgram.unbind();

        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL20.glDisableVertexAttribArray(2);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    @Override
    public void calcDims() {
        final Vector3f minv = new Vector3f(-1.0f, -1.0f, -1.0f);
        final Vector3f maxv = new Vector3f(1.0f, 1.0f, 1.0f);

        width = Math.abs(maxv.x - minv.x) * scale;
        height = Math.abs(maxv.y - minv.y) * scale;
        depth = Math.abs(maxv.z - minv.z) * scale;
    }

    public int faceAdjacentBy(Block block) { // which face of "this" is adjacent to compared "block"
        int faceNum = NONE;
        if (((this.pos.x - this.width / 2.0f) - (block.pos.x + block.width / 2.0f)) == 0.0f
                && (this.getPos().y - block.getPos().y) == 0.0f
                && (this.getPos().z - block.getPos().z) == 0.0f) {
            faceNum = LEFT;
        } else if (((this.pos.x + this.width / 2.0f) - (block.pos.x - block.width / 2.0f)) == 0.0f
                && (this.getPos().y - block.getPos().y) == 0.0f
                && (this.getPos().z - block.getPos().z) == 0.0f) {
            faceNum = RIGHT;
        } else if (((this.pos.y - this.height / 2.0f) - (block.pos.y + block.height / 2.0f)) == 0.0f
                && (this.getPos().z - block.getPos().z) == 0.0f
                && (this.getPos().x - block.getPos().x) == 0.0f) {
            faceNum = BOTTOM;
        } else if (((this.pos.y + this.height / 2.0f) - (block.pos.y - block.height / 2.0f)) == 0.0f
                && (this.getPos().z - block.getPos().z) == 0.0f
                && (this.getPos().x - block.getPos().x) == 0.0f) {
            faceNum = TOP;
        } else if (((this.pos.z - this.depth / 2.0f) - (block.pos.z + block.depth / 2.0f)) == 0.0f
                && (this.getPos().x - block.getPos().x) == 0.0f
                && (this.getPos().y - block.getPos().y) == 0.0f) {
            faceNum = BACK;
        } else if (((this.pos.z + this.depth / 2.0f) - (block.pos.z - block.depth / 2.0f)) == 0.0f
                && (this.getPos().x - block.getPos().x) == 0.0f
                && (this.getPos().y - block.getPos().y) == 0.0f) {
            faceNum = FRONT;
        }
        return faceNum;
    }

    public static List<Vertex> getFaceVertices(List<Vertex> vertices, int faceNum) {
        return vertices.subList(4 * faceNum, 4 * (faceNum + 1));
    }

    /**
     * Can block be seen by camera. It is assumed that block dimension is 2.1 x
     * 2.1 x 2.1
     *
     * @param camera (observer) camera
     * @param viewProjMatrix frustum matrix
     * @return intersection with this block
     */
    public boolean canBeSeenBy(Camera camera, Matrix4f viewProjMatrix) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();

        Vector3f min = pos.sub(1.05f, 1.05f, 1.05f, temp1);
        Vector3f max = pos.add(1.05f, 1.05f, 1.05f, temp2);

        FrustumIntersection frustumIntersection = new org.joml.FrustumIntersection(viewProjMatrix);

        return frustumIntersection.intersectAab(min, max) != FrustumIntersection.OUTSIDE;
    }

    /**
     * Can block be seen by camera.It is assumed that block dimension is 2.1 x
     * 2.1 x 2.1
     *
     * @param viewProjMatrix frustum matrix
     * @param blkPos block position
     * @return intersection with this block
     */
    public static boolean canBeSeenBy(Vector3f blkPos, Matrix4f viewProjMatrix) {
        Vector3f temp1 = new Vector3f();
        Vector3f temp2 = new Vector3f();

        Vector3f min = blkPos.sub(1.05f, 1.05f, 1.05f, temp1);
        Vector3f max = blkPos.add(1.05f, 1.05f, 1.05f, temp2);

        FrustumIntersection frustumIntersection = new org.joml.FrustumIntersection(viewProjMatrix);

        return frustumIntersection.intersectAab(min, max) != FrustumIntersection.OUTSIDE;
    }

    /**
     * Returns visible bits based on faces which can seen by camera front.
     *
     * @param camFront camera front (eye)
     * @return [LEFT, RIGHT, BOTTOM, TOP, BACK, FRONT] bits.
     */
    public static int getVisibleFaceBits(Vector3f camFront) {
        int result = 0;
        Vector3f temp = new Vector3f();
        for (int j = Block.LEFT; j <= Block.FRONT; j++) {
            Vector3f normal = FACE_NORMALS[j];
            float dotProduct = normal.dot(camFront.mul(-1.0f, temp));
            float angle = (float) org.joml.Math.toDegrees(MathUtils.acos(dotProduct));
            if (angle <= 177.0f) {
                int mask = 1 << j;
                result |= mask;
            }
        }
        return result;
    }

    /**
     * Returns visible bits based on faces which can seen by camera front.
     * Faster version of original. And performs on cosine 3 degrees.
     *
     * @param camFront camera front (eye)
     * @return [LEFT, RIGHT, BOTTOM, TOP, BACK, FRONT] bits.
     */
    public static int getVisibleFaceBitsFast(Vector3f camFront) {
        int result = 0;

        final float cos3 = 0.99863f; // cosine of 3 degrees
        Vector3f temp = new Vector3f();
        Vector3f camFrontNeg = camFront.negate(temp);

        int zPos = (camFrontNeg.z >= -cos3) ? Z_MASK : 0;
        int zNeg = (camFrontNeg.z <= cos3) ? ZNEG_MASK : 0;
        int yPos = (camFrontNeg.y >= -cos3) ? Y_MASK : 0;
        int yNeg = (camFrontNeg.y <= cos3) ? YNEG_MASK : 0;
        int xPos = (camFrontNeg.x >= -cos3) ? X_MASK : 0;
        int xNeg = (camFrontNeg.x <= cos3) ? XNEG_MASK : 0;

        result = zPos | zNeg | yPos | yNeg | xPos | xNeg;

        return result;
    }

    /**
     * Returns visible bits based on faces which can seen by camera front.
     * Faster version of original. And performs on cosine 3 degrees.
     *
     * @param camFront camera front (eye)
     * @param degrees angle degrees
     * @return [LEFT, RIGHT, BOTTOM, TOP, BACK, FRONT] bits.
     */
    public static int getVisibleFaceBitsFast(Vector3f camFront, float degrees) {
        final float cosine = org.joml.Math.cos(org.joml.Math.toRadians(degrees));
        Vector3f camFrontNeg = new Vector3f(-camFront.x, -camFront.y, -camFront.z);

        int result = 0;
        if (camFrontNeg.z >= -cosine) {
            result |= Z_MASK;
        }
        if (camFrontNeg.z <= cosine) {
            result |= ZNEG_MASK;
        }
        if (camFrontNeg.y >= -cosine) {
            result |= Y_MASK;
        }
        if (camFrontNeg.y <= cosine) {
            result |= YNEG_MASK;
        }
        if (camFrontNeg.x >= -cosine) {
            result |= X_MASK;
        }
        if (camFrontNeg.x <= cosine) {
            result |= XNEG_MASK;
        }

        return result;
    }

    /**
     * Returns single visible based on faces which can seen by camera front.
     * Faster version of original.
     *
     * @param camFront camera front (eye)
     * @param degrees angle degrees
     *
     * @return one of [LEFT, RIGHT, BOTTOM, TOP, BACK, FRONT] faces.
     */
    public static int getRayTraceSingleFaceFast(Vector3f camFront, float degrees) {
        final float cosine = org.joml.Math.cos(org.joml.Math.toRadians(degrees));
        Vector3f camFrontNeg = new Vector3f(-camFront.x, -camFront.y, -camFront.z);

        int someValue = 0;
        if (camFrontNeg.z >= -cosine) {
            someValue |= Z_MASK;
        }
        if (camFrontNeg.z <= cosine) {
            someValue |= ZNEG_MASK;
        }
        if (camFrontNeg.y >= -cosine) {
            someValue |= Y_MASK;
        }
        if (camFrontNeg.y <= cosine) {
            someValue |= YNEG_MASK;
        }
        if (camFrontNeg.x >= -cosine) {
            someValue |= X_MASK;
        }
        if (camFrontNeg.x <= cosine) {
            someValue |= XNEG_MASK;
        }

        for (int j = 0; j <= 5; j++) {
            if ((someValue & (1 << j)) != 0) {
                return j;
            }
        }

        return -1;
    }

    /**
     * Returns multi visible faces which can seen by camera front. Faster
     * version of original.
     *
     * @param camFront camera front (eye)
     * @param degrees angle degrees
     * @return one of [LEFT, RIGHT, BOTTOM, TOP, BACK, FRONT] faces.
     */
    public static IList<Integer> getRayTraceMultiFaceFast(Vector3f camFront, float degrees) {
        final IList<Integer> result = new GapList<>();
        final float cosine = org.joml.Math.cos(org.joml.Math.toRadians(degrees));
        Vector3f camFrontNeg = new Vector3f(-camFront.x, -camFront.y, -camFront.z);

        int someValue = 0;
        if (camFrontNeg.z >= -cosine) {
            someValue |= Z_MASK;
        }
        if (camFrontNeg.z <= cosine) {
            someValue |= ZNEG_MASK;
        }
        if (camFrontNeg.y >= -cosine) {
            someValue |= Y_MASK;
        }
        if (camFrontNeg.y <= cosine) {
            someValue |= YNEG_MASK;
        }
        if (camFrontNeg.x >= -cosine) {
            someValue |= X_MASK;
        }
        if (camFrontNeg.x <= cosine) {
            someValue |= XNEG_MASK;
        }

        for (int j = 0; j <= 5; j++) {
            if ((someValue & (1 << j)) != 0) {
                result.add(j);
            }
        }

        return result;
    }

    public void disableFace(int faceNum) {
        final IList<Vertex> vertices = meshes.getFirst().vertices;
        for (Vertex subVertex : getFaceVertices(vertices, faceNum)) {
            subVertex.setEnabled(false);
        }
        this.enabledFaces[faceNum] = false;
    }

    public void enableFace(int faceNum) {
        final IList<Vertex> vertices = meshes.getFirst().vertices;
        for (Vertex subVertex : getFaceVertices(vertices, faceNum)) {
            subVertex.setEnabled(true);
        }
        this.enabledFaces[faceNum] = true;
    }

    public void enableAllFaces() {
        final IList<Vertex> vertices = meshes.getFirst().vertices;
        for (Vertex vertex : vertices) {
            vertex.setEnabled(true);
        }
        Arrays.fill(enabledFaces, true);
    }

    public void disableAllFaces() {
        final IList<Vertex> vertices = meshes.getFirst().vertices;
        for (Vertex vertex : vertices) {
            vertex.setEnabled(false);
        }
        Arrays.fill(enabledFaces, false);
    }

    /**
     * Reverse faces but only for Top (Water)
     */
    public void reverseTopFaceVertexOrder() {
        final IList<Vertex> vertices = meshes.getFirst().vertices;
        Collections.reverse(getFaceVertices(vertices, Block.TOP));
        verticesReversed = !verticesReversed;
    }

    /**
     * Reverse face vertex order. All Faces. (Water)
     */
    public void reverseFaceVertexOrder() {
        final IList<Vertex> vertices = meshes.getFirst().vertices;
        for (int faceNum = Block.LEFT; faceNum <= Block.FRONT; faceNum++) {
            int start = 4 * faceNum;
            int end = start + 3;
            while (start < end) {
                Vertex temp = vertices.get(start);
                vertices.set(start, vertices.get(end));
                vertices.set(end, temp);
                start++;
                end--;
            }
        }
        verticesReversed = !verticesReversed;
    }

    /**
     * Reverse face vertex order. All Faces. (Water)
     *
     * @param camFront camera front (ray trace cap)
     * @param degrees angle degrees
     */
    public void reverseFaceVertexOrder(Vector3f camFront, float degrees) {
        IList<Integer> faces = getRayTraceMultiFaceFast(camFront, degrees);
        final IList<Vertex> vertices = meshes.getFirst().vertices;

        for (int faceNum : faces) {
            int start = 4 * faceNum;
            int end = start + 3;
            while (start < end) {
                Vertex temp = vertices.get(start);
                vertices.set(start, vertices.get(end));
                vertices.set(end, temp);
                start++;
                end--;
            }
        }
        verticesReversed = !verticesReversed;
    }

    /**
     * Reverse face vertex order. All Faces. (Water)
     *
     * Faces are provided before hand. Therefore potential for reducing overhead
     * (and improve performance).
     *
     * @param camFront camera front (ray trace cap)
     * @param degrees angle degrees
     * @param faces get enabled faces
     */
    public void reverseFaceVertexOrder(Vector3f camFront, float degrees, IList<Integer> faces) {
        final IList<Vertex> vertices = meshes.getFirst().vertices;

        for (int faceNum : faces) {
            int start = 4 * faceNum;
            int end = start + 3;
            while (start < end) {
                Vertex temp = vertices.get(start);
                vertices.set(start, vertices.get(end));
                vertices.set(end, temp);
                start++;
                end--;
            }
        }
        verticesReversed = !verticesReversed;
    }

    public void setFaceVertexOrder(boolean faceVertexOrderReversed) {
        final IList<Vertex> vertices = meshes.getFirst().vertices;
        vertices.clear();
        Block.deepCopyTo(vertices, texName);
        if (faceVertexOrderReversed) {
            for (int faceNum = 0; faceNum <= 5; faceNum++) {
                Collections.reverse(getFaceVertices(vertices, faceNum));
            }
        }
        verticesReversed = faceVertexOrderReversed;
    }

    public static void reverseFaceVertexOrder(List<Vertex> vertices) {
        for (int faceNum = 0; faceNum <= 5; faceNum++) {
            Collections.reverse(getFaceVertices(vertices, faceNum));
        }
    }

    public void setUVsForSkybox() {
        revertGroupsOfVertices();
        IList<Vertex> vertices = meshes.getFirst().vertices;
        // LEFT
        vertices.get(4 * LEFT).getUv().x = 0.5f;
        vertices.get(4 * LEFT).getUv().y = 1.0f / 3.0f;

        vertices.get(4 * LEFT + 1).getUv().x = 0.25f;
        vertices.get(4 * LEFT + 1).getUv().y = 1.0f / 3.0f;

        vertices.get(4 * LEFT + 2).getUv().x = 0.25f;
        vertices.get(4 * LEFT + 2).getUv().y = 2.0f / 3.0f;

        vertices.get(4 * LEFT + 3).getUv().x = 0.5f;
        vertices.get(4 * LEFT + 3).getUv().y = 2.0f / 3.0f;
        // BACK
        for (int i = 0; i < 4; i++) {
            vertices.get(4 * BACK + i).getUv().x = vertices.get(4 * LEFT + i).getUv().x - 0.25f;
            vertices.get(4 * BACK + i).getUv().y = vertices.get(4 * LEFT + i).getUv().y;
        }
        // FRONT
        for (int i = 0; i < 4; i++) {
            vertices.get(4 * FRONT + i).getUv().x = vertices.get(4 * LEFT + i).getUv().x + 0.25f;
            vertices.get(4 * FRONT + i).getUv().y = vertices.get(4 * LEFT + i).getUv().y;
        }
        // RIGHT
        for (int i = 0; i < 4; i++) {
            vertices.get(4 * RIGHT + i).getUv().x = vertices.get(4 * FRONT + i).getUv().x + 0.25f;
            vertices.get(4 * RIGHT + i).getUv().y = vertices.get(4 * FRONT + i).getUv().y;
        }
        // TOP
        for (int i = 0; i < 4; i++) {
            vertices.get(4 * TOP + i).getUv().x = vertices.get(4 * LEFT + i).getUv().x;
            vertices.get(4 * TOP + i).getUv().y = vertices.get(4 * LEFT + i).getUv().y - 1.0f / 3.0f;
        }
        // BOTTOM
        for (int i = 0; i < 4; i++) {
            vertices.get(4 * BOTTOM + i).getUv().x = vertices.get(4 * LEFT + i).getUv().x;
            vertices.get(4 * BOTTOM + i).getUv().y = vertices.get(4 * LEFT + i).getUv().y + 1.0f / 3.0f;
        }

    }

    public void nullifyNormalsForFace(int faceNum) {
        final IList<Vertex> vertices = meshes.getFirst().vertices;
        List<Vertex> faceVertices = Block.getFaceVertices(vertices, faceNum);
        for (Vertex fv : faceVertices) {
            fv.getNormal().zero();
        }
        meshes.getFirst().buffered = false;
    }

    private void revertGroupsOfVertices() {
        IList<Vertex> vertices = meshes.getFirst().vertices;
        Collections.reverse(vertices.subList(4 * LEFT, 4 * LEFT + 3));
        Collections.reverse(vertices.subList(4 * RIGHT, 4 * RIGHT + 3));
        Collections.reverse(vertices.subList(4 * BOTTOM, 4 * BOTTOM + 3));
        Collections.reverse(vertices.subList(4 * TOP, 4 * TOP + 3));
        Collections.reverse(vertices.subList(4 * BACK, 4 * BACK + 3));
        Collections.reverse(vertices.subList(4 * FRONT, 4 * FRONT + 3));
    }

    public boolean hasFaces() {
        boolean arg = false;
        for (Boolean bool : enabledFaces) {
            arg = arg || bool;
            if (arg) {
                break;
            }
        }
        return arg;
    }

    public int getNumOfEnabledFaces() {
        int num = 0;
        for (int i = 0; i <= 5; i++) {
            if (enabledFaces[i]) {
                num++;
            }
        }
        return num;
    }

    public int getNumOfEnabledVertices() {
        int num = 0;
        final IList<Vertex> vertices = meshes.getFirst().vertices;
        for (Vertex vertex : vertices) {
            if (vertex.isEnabled()) {
                num++;
            }
        }
        return num;
    }

    public boolean[] getEnabledFaces() {
        return enabledFaces;
    }

    public boolean isVerticesReversed() {
        return verticesReversed;
    }

    /**
     * Get enabled faces used in Tuple Series, representation is in 6-bit form
     * [LEFT, RIGHT, BOTTOM, TOP, BACK, FRONT]
     *
     * @return 6-bit face bits
     */
    public int getFaceBits() {
        int bits = 0;
        for (int j = 0; j <= 5; j++) {
            if (enabledFaces[j]) {
                int mask = 1 << j;
                bits |= mask;
            }
        }
        return bits;
    }

    // used in static Level container to get compressed positioned sets
    @Deprecated
    public static int getNeighborBits(Vector3f pos, Set<Vector3f> vectorSet) {
        int bits = 0;
        for (int j = 0; j <= 5; j++) { // j - face number
            Vector3f adjPos = Block.getAdjacentPos(pos, j);
            if (vectorSet.contains(adjPos)) {
                int mask = 1 << j;
                bits |= mask;
            }
        }
        return bits;
    }

    /**
     * Set facebits to block. Faces will be enabled/disabled on bit
     * representation. Bit 6 = don't care Bit 7 = Don't care Bit 5 = FRONT (+Z)
     * Bit 4 = BACK (-Z) Bit 3 = TOP (+Y) Bit 2 = BOTTOM (-Y) Bit 1 = RIGHT (+X)
     * Bit 0 = LEFT (-X)
     *
     * @param faceBits set facebits (0-63)
     * @return number of enabled faces (number of ones in face bit
     * representation)
     */
    public int setFaceBits(int faceBits) {
        int counter = 0;
        for (int j = 0; j <= 5; j++) {
            int mask = 1 << j;
            int bit = (faceBits & mask) >> j;
            if (bit == 1) {
                counter++;
                enableFace(j);
            } else {
                disableFace(j);
            }
        }

        this.id = genId();

        return counter;
    }

    @Deprecated
    public static void setFaceBits(List<Vertex> vertices, int faceBits) {
        for (int j = 0; j <= 5; j++) {
            int mask = 1 << j;
            int bit = (faceBits & mask) >> j;
            List<Vertex> subList = getFaceVertices(vertices, j);
            boolean en = (bit == 1);
            for (Vertex v : subList) {
                v.setEnabled(en);
            }
        }
    }

    /**
     * Make indices list base on bits form of enabled faces (used only for
     * blocks) Representation form is 6-bit [LEFT, RIGHT, BOTTOM, TOP, BACK,
     * FRONT]
     *
     * @param faceBits 6-bit form
     * @return indices list
     */
    public static List<Integer> createIndices(int faceBits) {
        // creating indices
        List<Integer> indices = new ArrayList<>();
        int j = 0; // is face number (which increments after the face is added)
        while (faceBits > 0) {
            int bit = faceBits & 1; // compare the rightmost bit with one and assign it to bit
            if (bit == 1) {
                indices.add(4 * j);
                indices.add(4 * j + 1);
                indices.add(4 * j + 2);

                indices.add(4 * j + 2);
                indices.add(4 * j + 3);
                indices.add(4 * j);

                j++;
            }
            faceBits >>= 1; // move bits to the right so they are compared again            
        }

        return indices;
    }

    /**
     * Make new index buffer base on bits form of enabled faces (used only for
     * blocks) Representation form is 6-bit [LEFT, RIGHT, BOTTOM, TOP, BACK,
     * FRONT]
     *
     * @param faceBits 6-bit form
     * @return index buffer
     */
    public static IntBuffer createIntBuffer(int faceBits) {
        // creating indices
        List<Integer> indices = new ArrayList<>();
        int j = 0; // is face number (which increments after the face is added)
        while (faceBits > 0) {
            int bit = faceBits & 1; // compare the rightmost bit with one and assign it to bit
            if (bit == 1) {
                indices.add(4 * j);
                indices.add(4 * j + 1);
                indices.add(4 * j + 2);

                indices.add(4 * j + 2);
                indices.add(4 * j + 3);
                indices.add(4 * j);

                j++;
            }
            faceBits >>= 1; // move bits to the right so they are compared again            
        }
        // storing indices in the buffer
        IntBuffer intBuff = MemoryUtil.memAllocInt(indices.size());
        if (intBuff.capacity() != 0 && MemoryUtil.memAddressSafe(intBuff) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            throw new RuntimeException("Could not allocate memory address!");
        }

        for (Integer index : indices) {
            intBuff.put(index);
        }

        if (intBuff.position() != 0) {
            intBuff.flip();
        }

        return intBuff;
    }

    /**
     * Make indices list base on bits form of enabled faces (used only for
     * blocks) Representation form is 6-bit [LEFT, RIGHT, BOTTOM, TOP, BACK,
     * FRONT]
     *
     * @param faceBits 6-bit form
     * @param baseConst const to add to all indices
     * @return index buffer
     */
    public static IList<Integer> createIndices(int faceBits, int baseConst) {
        // creating indices
        IList<Integer> indices = new GapList<>();
        int j = baseConst; // is face number (which increments after the face is added)
        while (faceBits > 0) {
            int bit = faceBits & 1; // compare the rightmost bit with one and assign it to bit
            if (bit == 1) {
                indices.add(4 * j);
                indices.add(4 * j + 1);
                indices.add(4 * j + 2);

                indices.add(4 * j + 2);
                indices.add(4 * j + 3);
                indices.add(4 * j);

                j++;
            }
            faceBits >>= 1; // move bits to the right so they are compared again            
        }

        return indices;
    }

    /**
     * Make new index buffer base on bits form of enabled faces (used only for
     * blocks) Representation form is 6-bit [LEFT, RIGHT, BOTTOM, TOP, BACK,
     * FRONT]
     *
     * @param faceBits 6-bit form
     * @param baseConst const to add to all indices
     * @return index buffer
     */
    public static IntBuffer createIntBuffer(int faceBits, int baseConst) {
        // creating indices
        List<Integer> indices = new ArrayList<>();
        int j = baseConst; // is face number (which increments after the face is added)
        while (faceBits > 0) {
            int bit = faceBits & 1; // compare the rightmost bit with one and assign it to bit
            if (bit == 1) {
                indices.add(4 * j);
                indices.add(4 * j + 1);
                indices.add(4 * j + 2);

                indices.add(4 * j + 2);
                indices.add(4 * j + 3);
                indices.add(4 * j);

                j++;
            }
            faceBits >>= 1; // move bits to the right so they are compared again            
        }
        // storing indices in the buffer
        IntBuffer intBuff = MemoryUtil.memAllocInt(indices.size());
        if (intBuff.capacity() != 0 && MemoryUtil.memAddressSafe(intBuff) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            throw new RuntimeException("Could not allocate memory address!");
        }

        for (Integer index : indices) {
            intBuff.put(index);
        }

        if (intBuff.position() != 0) {
            intBuff.flip();
        }

        return intBuff;
    }

    /**
     * Resize index buffer base on bits form of enabled faces (used only for
     * blocks) Representation form is 6-bit [LEFT, RIGHT, BOTTOM, TOP, BACK,
     * FRONT]
     *
     * @param intBuff supplied int buffer (to resize)
     * @param faceBits 6-bit form
     * @return index buffer
     */
    public static IntBuffer resizeIntBuffer(IntBuffer intBuff, int faceBits) {
        // creating indices
        List<Integer> indices = new ArrayList<>();
        int j = 0; // is face number (which increments after the face is added)
        while (faceBits > 0) {
            int bit = faceBits & 1; // compare the rightmost bit with one and assign it to bit
            if (bit == 1) {
                indices.add(4 * j);
                indices.add(4 * j + 1);
                indices.add(4 * j + 2);

                indices.add(4 * j + 2);
                indices.add(4 * j + 3);
                indices.add(4 * j);

                j++;
            }
            faceBits >>= 1; // move bits to the right so they are compared again            
        }
        // storing indices in the buffer
        intBuff = MemoryUtil.memRealloc(intBuff, indices.size());

        if (intBuff.capacity() != 0 && MemoryUtil.memAddressSafe(intBuff) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            throw new RuntimeException("Could not allocate memory address!");
        }

        for (Integer index : indices) {
            intBuff.put(index);
        }

        if (intBuff.position() != 0) {
            intBuff.flip();
        }

        return intBuff;
    }

    /**
     * Resize index buffer base on bits form of enabled faces (used only for
     * blocks) Representation form is 6-bit [LEFT, RIGHT, BOTTOM, TOP, BACK,
     * FRONT]
     *
     * @param intBuff supplied int buffer (to resize)
     * @param faceBits 6-bit form
     * @param baseConst const to add to all indices
     * @return index buffer
     */
    public static IntBuffer resizeIntBuffer(IntBuffer intBuff, int faceBits, int baseConst) {
        // creating indices
        List<Integer> indices = new ArrayList<>();
        int j = baseConst; // is face number (which increments after the face is added)
        while (faceBits > 0) {
            int bit = faceBits & 1; // compare the rightmost bit with one and assign it to bit
            if (bit == 1) {
                indices.add(4 * j);
                indices.add(4 * j + 1);
                indices.add(4 * j + 2);

                indices.add(4 * j + 2);
                indices.add(4 * j + 3);
                indices.add(4 * j);

                j++;
            }
            faceBits >>= 1; // move bits to the right so they are compared again            
        }
        // storing indices in the buffer
        intBuff = MemoryUtil.memRealloc(intBuff, indices.size());

        if (intBuff.capacity() != 0 && MemoryUtil.memAddressSafe(intBuff) == MemoryUtil.NULL) {
            DSLogger.reportError("Could not allocate memory address!", null);
            throw new RuntimeException("Could not allocate memory address!");
        }

        for (Integer index : indices) {
            intBuff.put(index);
        }

        if (intBuff.position() != 0) {
            intBuff.flip();
        }

        return intBuff;
    }

    // returns array of adjacent free face numbers (those faces without adjacent neighbor nearby)
    // used by Random Level Generator
    /**
     * Returns array of adjacent free face numbers (those faces without adjacent
     * neighbor nearby) used by Random Level Generator
     *
     * @return free face numbers (not block on that side
     */
    public List<Integer> getAdjacentFreeFaceNumbers() {
        List<Integer> result = new ArrayList<>();

        int sbits = 0;
        TexByte pair = LevelContainer.AllBlockMap.getLocation(pos);
        if (pair != null && pair.isSolid()) {
            sbits = pair.getByteValue();
        }

        int fbits = 0;

        if (pair != null && sbits == 0 && !pair.isSolid()) {
            fbits = pair.getByteValue();
        }

        int tbits = sbits | fbits;

        for (int j = 0; j <= 5; j++) {
            int mask = 1 << j;
            if ((tbits & mask) == 0) {
                result.add(j);
            }
        }

        return result;
    }

    /**
     * Returns array of adjacent free face numbers (those faces without adjacent
     * neighbor nearby) used by Random Level Generator
     *
     * @param pos block position
     * @return free face numbers (not block on that side
     */
    public static List<Integer> getAdjacentFreeFaceNumbers(Vector3f pos) {
        List<Integer> result = new ArrayList<>();

        int sbits = 0;
        TexByte pair = LevelContainer.AllBlockMap.getLocation(pos);
        if (pair != null && pair.isSolid()) {
            sbits = pair.getByteValue();
        }

        int fbits = 0;

        if (pair != null && sbits == 0 && !pair.isSolid()) {
            fbits = pair.getByteValue();
        }

        int tbits = sbits | fbits;

        for (int j = 0; j <= 5; j++) {
            int mask = 1 << j;
            if ((tbits & mask) == 0) {
                result.add(j);
            }
        }

        return result;
    }

    // assuming that blocks are the same scale
    public Vector3f getAdjacentPos(int faceNum) {
        Vector3f result = new Vector3f();
        result.x = pos.x;
        result.y = pos.y;
        result.z = pos.z;

        switch (faceNum) {
            case Block.LEFT:
                result.x -= 2.0f;
                break;
            case Block.RIGHT:
                result.x += 2.0f;
                break;
            case Block.BOTTOM:
                result.y -= 2.0f;
                break;
            case Block.TOP:
                result.y += 2.0f;
                break;
            case Block.BACK:
                result.z -= 2.0f;
                break;
            case Block.FRONT:
                result.z += 2.0f;
                break;
            case Block.LEFT_BOTTOM:
                result.x -= 2.0f;
                result.y -= 2.0f;
                break;
            case Block.RIGHT_BOTTOM:
                result.x += 2.0f;
                result.y -= 2.0f;
                break;
            case Block.LEFT_TOP:
                result.x -= 2.0f;
                result.y += 2.0f;
                break;
            case Block.RIGHT_TOP:
                result.x += 2.0f;
                result.y += 2.0f;
                break;
            case Block.BOTTOM_BACK:
                result.y -= 2.0f;
                result.z -= 2.0f;
                break;
            case Block.BOTTOM_FRONT:
                result.y -= 2.0f;
                result.z += 2.0f;
                break;
            case Block.TOP_BACK:
                result.y -= 2.0f;
                result.z += 2.0f;
                break;
            case Block.TOP_FRONT:
                result.y += 2.0f;
                result.z += 2.0f;
                break;
            default:
                break;
        }

        return result;
    }

    // assuming that blocks are the same scale
    public static Vector3f getAdjacentPos(Vector3f pos, int faceNum) {
        Vector3f result = new Vector3f();
        result.x = pos.x;
        result.y = pos.y;
        result.z = pos.z;

        switch (faceNum) {
            case Block.LEFT:
                result.x -= 2.0f;
                break;
            case Block.RIGHT:
                result.x += 2.0f;
                break;
            case Block.BOTTOM:
                result.y -= 2.0f;
                break;
            case Block.TOP:
                result.y += 2.0f;
                break;
            case Block.BACK:
                result.z -= 2.0f;
                break;
            case Block.FRONT:
                result.z += 2.0f;
                break;
            case Block.LEFT_BOTTOM:
                result.x -= 2.0f;
                result.y -= 2.0f;
                break;
            case Block.RIGHT_BOTTOM:
                result.x += 2.0f;
                result.y -= 2.0f;
                break;
            case Block.LEFT_TOP:
                result.x -= 2.0f;
                result.y += 2.0f;
                break;
            case Block.RIGHT_TOP:
                result.x += 2.0f;
                result.y += 2.0f;
                break;
            case Block.BOTTOM_BACK:
                result.y -= 2.0f;
                result.z -= 2.0f;
                break;
            case Block.BOTTOM_FRONT:
                result.y -= 2.0f;
                result.z += 2.0f;
                break;
            case Block.TOP_BACK:
                result.y -= 2.0f;
                result.z += 2.0f;
                break;
            case Block.TOP_FRONT:
                result.y += 2.0f;
                result.z += 2.0f;
                break;
            default:
                break;
        }

        return result;
    }

    // assuming that blocks are the same scale
    public static Vector3f getAdjacentPos(Vector3f pos, int faceNum, float amount) {
        Vector3f result = new Vector3f();
        result.x = pos.x;
        result.y = pos.y;
        result.z = pos.z;

        switch (faceNum) {
            case Block.LEFT:
                result.x -= amount;
                break;
            case Block.RIGHT:
                result.x += amount;
                break;
            case Block.BOTTOM:
                result.y -= amount;
                break;
            case Block.TOP:
                result.y += amount;
                break;
            case Block.BACK:
                result.z -= amount;
                break;
            case Block.FRONT:
                result.z += amount;
                break;
            case Block.LEFT_BOTTOM:
                result.x -= amount;
                result.y -= amount;
                break;
            case Block.RIGHT_BOTTOM:
                result.x += amount;
                result.y -= amount;
                break;
            case Block.LEFT_TOP:
                result.x -= amount;
                result.y += amount;
                break;
            case Block.RIGHT_TOP:
                result.x += amount;
                result.y += amount;
                break;
            case Block.BOTTOM_BACK:
                result.y -= amount;
                result.z -= amount;
                break;
            case Block.BOTTOM_FRONT:
                result.y -= amount;
                result.z += amount;
                break;
            case Block.TOP_BACK:
                result.y -= amount;
                result.z += amount;
                break;
            case Block.TOP_FRONT:
                result.y += amount;
                result.z += amount;
                break;
            default:
                break;
        }

        return result;
    }

    public static int faceAdjacentBy(Vector3f blkPosA, Vector3f blkPosB) { // which face of blk "A" is adjacent to compared blk "B"
        int faceNum = -1;
        if (Math.abs((blkPosA.x - 1.0f) - (blkPosB.x + 1.0f)) == 0.0f
                && Math.abs(blkPosA.y - blkPosB.y) <= 0.0f
                && Math.abs(blkPosA.z - blkPosB.z) <= 0.0f) {
            faceNum = LEFT;
        } else if (Math.abs((blkPosA.x + 1.0f) - (blkPosB.x - 1.0f)) == 0.0f
                && Math.abs(blkPosA.y - blkPosB.y) == 0.0f
                && Math.abs(blkPosA.z - blkPosB.z) == 0.0f) {
            faceNum = RIGHT;
        } else if (Math.abs((blkPosA.y - 1.0f) - (blkPosB.y + 1.0f)) == 0.0f
                && Math.abs(blkPosA.z - blkPosB.z) == 0.0f
                && Math.abs(blkPosA.x - blkPosB.x) == 0.0f) {
            faceNum = BOTTOM;
        } else if (Math.abs((blkPosA.y + 1.0f) - (blkPosB.y - 1.0f)) == 0.0f
                && Math.abs(blkPosA.z - blkPosB.z) == 0.0f
                && Math.abs(blkPosA.x - blkPosB.x) == 0.0f) {
            faceNum = TOP;
        } else if (Math.abs((blkPosA.z - 1.0f) - (blkPosB.z + 1.0f)) == 0.0f
                && Math.abs(blkPosA.x - blkPosB.x) == 0.0f
                && Math.abs(blkPosA.y - blkPosB.y) == 0.0f) {
            faceNum = BACK;
        } else if (Math.abs((blkPosA.z + 1.0f) - (blkPosB.z - 1.0f)) == 0.0f
                && Math.abs(blkPosA.x - blkPosB.x) == 0.0f
                && Math.abs(blkPosA.y - blkPosB.y) == 0.0f) {
            faceNum = FRONT;
        }
        return faceNum;
    }

    public static boolean intersectsRay(Vector3f blockPos, Vector3f dir, Vector3f origin) {
        boolean ints = false;
        Vector3f temp1 = new Vector3f();
        Vector3f min = blockPos.sub(1.0f, 1.0f, 1.0f, temp1);
        Vector3f temp2 = new Vector3f();
        Vector3f max = blockPos.add(1.0f, 1.0f, 1.0f, temp2);
        Vector2f result = new Vector2f();
        ints = Intersectionf.intersectRayAab(origin, dir, min, max, result);
        return ints;
    }

    public static Vector2f intersectsRay2(Vector3f blockPos, Vector3f dir, Vector3f origin) {
        Vector3f temp1 = new Vector3f();
        Vector3f min = blockPos.sub(1.0f, 1.0f, 1.0f, temp1);
        Vector3f temp2 = new Vector3f();
        Vector3f max = blockPos.add(1.0f, 1.0f, 1.0f, temp2);
        Vector2f result = new Vector2f();
        Intersectionf.intersectRayAab(origin, dir, min, max, result);

        return result;
    }

    public byte[] toByteArray() {
        byte[] byteArray = new byte[29];
        int offset = 0;
        byte[] texNameArr = texName.getBytes();
        System.arraycopy(texNameArr, 0, byteArray, offset, 5);
        offset += 5;
        byte[] posArr = VectorFloatUtils.vec3fToByteArray(pos);
        System.arraycopy(posArr, 0, byteArray, offset, posArr.length); // 12 B
        offset += posArr.length;
        Vector3f primaryRGBColor = getPrimaryRGBColor();
        byte[] colArr = VectorFloatUtils.vec3fToByteArray(primaryRGBColor);
        System.arraycopy(colArr, 0, byteArray, offset, colArr.length); // 12 B

        return byteArray;
    }

    public static byte[] toByteArray(Vector3f pos, TexByte tb) {
        byte[] byteArray = new byte[29];
        int offset = 0;
        byte[] texNameArr = tb.texName.getBytes();
        System.arraycopy(texNameArr, 0, byteArray, offset, 5);
        offset += 5;
        byte[] posArr = VectorFloatUtils.vec3fToByteArray(pos);
        System.arraycopy(posArr, 0, byteArray, offset, posArr.length); // 12 B
        offset += posArr.length;
        Vector3f primaryRGBColor = new Vector3f(tb.color.x, tb.color.y, tb.color.z);
        byte[] colArr = VectorFloatUtils.vec3fToByteArray(primaryRGBColor);
        System.arraycopy(colArr, 0, byteArray, offset, colArr.length); // 12 B

        return byteArray;
    }

    public byte[] toNewByteArray() {
        byte[] byteArray = new byte[35];
        int offset = 0;
        byte[] texNameArr = texName.getBytes();
        System.arraycopy(texNameArr, 0, byteArray, offset, 5); // 5B
        offset += 5;
        byte[] posArr = VectorFloatUtils.vec3fToByteArray(pos);
        System.arraycopy(posArr, 0, byteArray, offset, posArr.length); // 12 B
        offset += posArr.length;
        Vector4f primaryRGBAColor = getPrimaryRGBAColor();
        byte[] colArr = VectorFloatUtils.vec4fToByteArray(primaryRGBAColor);
        System.arraycopy(colArr, 0, byteArray, offset, colArr.length); // 16 B

        return byteArray;
    }

    public static Block fromByteArray(byte[] byteArray, boolean solid) {
        int offset = 0;
        char[] texNameArr = new char[5];
        for (int k = 0; k < texNameArr.length; k++) {
            texNameArr[k] = (char) byteArray[offset++];
        }
        String texName = String.valueOf(texNameArr);

        byte[] blockPosArr = new byte[12];
        System.arraycopy(byteArray, offset, blockPosArr, 0, blockPosArr.length);
        Vector3f blockPos = VectorFloatUtils.vec3fFromByteArray(blockPosArr);
        offset += blockPosArr.length;

        byte[] blockPosCol = new byte[12];
        System.arraycopy(byteArray, offset, blockPosCol, 0, blockPosCol.length);
        Vector3f blockCol = VectorFloatUtils.vec3fFromByteArray(blockPosCol);

        Block block = new Block(texName, blockPos, new Vector4f(blockCol, solid ? 1.0f : 0.5f), solid);

        return block;
    }

    public static Block fromNewByteArray(byte[] byteArray) {
        int offset = 0;
        char[] texNameArr = new char[5];
        for (int k = 0; k < texNameArr.length; k++) {
            texNameArr[k] = (char) byteArray[offset++];
        }
        String texName = String.valueOf(texNameArr);

        byte[] blockPosArr = new byte[12];
        System.arraycopy(byteArray, offset, blockPosArr, 0, blockPosArr.length);
        Vector3f blockPos = VectorFloatUtils.vec3fFromByteArray(blockPosArr);
        offset += blockPosArr.length;

        byte[] blockColArr = new byte[16];
        System.arraycopy(byteArray, offset, blockColArr, 0, blockColArr.length);
        Vector4f blockCol = VectorFloatUtils.vec4fFromByteArray(blockColArr);
        offset += blockColArr.length;

        boolean solid = byteArray[offset] != (byte) 0x00;
        Block block = new Block(texName, blockPos, blockCol, solid);

        return block;
    }

    /**
     * Returns the opposite side based on the given direction.
     *
     * @param direction The direction to get opposite side for
     * @return The opposite side constant, or NONE if direction is invalid
     */
    public static int getOppositeSide(Game.Direction direction) {
        if (direction == null) {
            return Block.NONE;
        }

        switch (direction) {
            case LEFT:
                return Block.RIGHT;  // Opposite of left is right
            case RIGHT:
                return Block.LEFT;   // Opposite of right is left
            case UP:
                return Block.BOTTOM; // Opposite of up is bottom
            case DOWN:
                return Block.TOP;    // Opposite of down is top
            case BACKWARD:
                return Block.FRONT;  // Opposite of backward is front
            case FORWARD:
                return Block.BACK;   // Opposite of forward is back
            default:
                return Block.NONE;   // Unknown direction
        }
    }

    /**
     * Returns an array of opposite sides based on the given direction. Includes
     * both the main opposite side and adjacent combinations.
     *
     * @param direction The direction to get opposite sides for
     * @return An array of opposite side constants, or {NONE} if direction is
     * invalid
     */
    public static int[] getOppositeSides(Game.Direction direction) {
        if (direction == null) {
            return new int[]{Block.NONE};
        }

        switch (direction) {
            case LEFT: // -x -> +x
                return new int[]{
                    Block.RIGHT,
                    Block.RIGHT_BOTTOM,
                    Block.RIGHT_TOP,
                    Block.BOTTOM_BACK,
                    Block.BOTTOM_FRONT
                };
            case RIGHT:
                return new int[]{
                    Block.LEFT,
                    Block.LEFT_BOTTOM,
                    Block.LEFT_TOP,
                    Block.TOP_BACK,
                    Block.TOP_FRONT
                };
            case UP:
                return new int[]{
                    Block.BOTTOM,
                    Block.BOTTOM_BACK,
                    Block.BOTTOM_FRONT,
                    Block.LEFT_BOTTOM,
                    Block.RIGHT_BOTTOM
                };
            case DOWN:
                return new int[]{
                    Block.TOP,
                    Block.TOP_BACK,
                    Block.TOP_FRONT,
                    Block.LEFT_TOP,
                    Block.RIGHT_TOP
                };
            case BACKWARD:
                return new int[]{
                    Block.FRONT,
                    Block.TOP_FRONT,
                    Block.BOTTOM_FRONT,
                    Block.LEFT_TOP, // Added more comprehensive front-facing sides
                    Block.RIGHT_TOP
                };
            case FORWARD:
                return new int[]{
                    Block.BACK,
                    Block.BOTTOM_BACK,
                    Block.TOP_BACK,
                    Block.LEFT_BOTTOM,
                    Block.RIGHT_BOTTOM
                };
            default:
                return new int[]{Block.NONE};
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append("Block{");
        sb.append("enabledFaces=").append(enabledFaces);
        sb.append(", verticesReversed=").append(verticesReversed);
        sb.append('}');
        return sb.toString();
    }

//    /**
//     * Get block light color (stored in AllBlockMap)
//     *
//     * @return block light color
//     */
//    @Override
//    public Vector4f getMapLightColor() {
//        // load light (color) information from somewhere
//        if (LevelContainer.AllBlockMap.isLocationPopulated(this.pos)) {
//            return LevelContainer.AllBlockMap.getLocation(this.pos).lightColor;
//        } else {
//            return super.getMapLightColor();
//        }
//
//    }
    /**
     * Convert block specs {solid, texName, VEC3} to unique int (computation).
     *
     * @return unique int
     */
    private int genId() {
        return ModelUtils.blockSpecsToUniqueInt(solid, texName, getFaceBits(), pos);
    }

    /**
     * Get unique id of this block
     *
     * @return unique (int) id
     */
    public int getId() {
        return id;
    }

    @Override
    public void setTexNameWithDeepCopy(String texName) {
        super.setTexNameWithDeepCopy(texName);
        id = genId();
    }

    @Override
    public void setTexName(String texName) {
        super.setTexName(texName);
        id = genId();
    }

    @Override
    public void setPos(Vector3f pos) {
        super.setPos(pos);
        id = genId();
    }

    @Override
    public void setSolid(boolean solid) {
        super.setSolid(solid);
        id = genId();
    }

}
