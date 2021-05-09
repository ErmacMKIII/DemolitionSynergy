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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.magicwerk.brownies.collections.GapList;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;
import rs.alexanderstojanovich.evg.util.DSLogger;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Model implements Comparable<Model> {

    protected String modelFileName;

    protected final List<Vertex> vertices = new GapList<>();
    protected final List<Integer> indices = new ArrayList<>(); // refers which vertex we want to use when       
    protected String texName;
    protected Texture waterTexture;

    protected float width; // X axis dimension
    protected float height; // Y axis dimension
    protected float depth; // Z axis dimension

    protected int vbo = 0; // vertex buffer object
    protected int ibo = 0; // index buffer object        

    protected Vector3f pos = new Vector3f();
    protected float scale = 1.0f; // changing scale also changes width, height and depth

    protected float rX = 0.0f;
    protected float rY = 0.0f;
    protected float rZ = 0.0f;

    protected Vector3f primaryColor = new Vector3f(1.0f, 1.0f, 1.0f);

    protected Vector3f light = new Vector3f();

    protected boolean solid = true; // is movement through this model possible
    // fluid models are solid whilst solid ones aren't               

    protected boolean buffered = false; // is it buffered, it must be buffered before rendering otherwise FATAL ERROR
    protected Matrix4f modelMatrix = calcModelMatrix();
    private FloatBuffer fb;

    protected Model(String modelFileName, String texName) {
        this.modelFileName = modelFileName;
        this.texName = texName;
    }

    protected Model(String modelFileName, String texName, Vector3f pos, Vector3f primaryColor, boolean solid) {
        this.modelFileName = modelFileName;
        this.texName = texName;
        this.pos = pos;
        this.primaryColor = primaryColor;
        this.solid = solid;
    }

    public static Model readFromObjFile(String dirEntry, String fileName, String texName) {
        File extern = new File(dirEntry + fileName);
        File archive = new File(Game.DATA_ZIP);
        ZipFile zipFile = null;
        InputStream objInput = null;
        if (extern.exists()) {
            try {
                objInput = new FileInputStream(extern);
            } catch (FileNotFoundException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
        } else if (archive.exists()) {
            try {
                zipFile = new ZipFile(archive);
                for (ZipEntry zipEntry : Collections.list(zipFile.entries())) {
                    if (zipEntry.getName().equals(dirEntry + fileName)) {
                        objInput = zipFile.getInputStream(zipEntry);
                        break;
                    }
                }
            } catch (IOException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
        } else {
            DSLogger.reportError("Cannot find zip archive " + Game.DATA_ZIP + " or relevant ingame files!", null);
        }
        //----------------------------------------------------------------------
        if (objInput == null) {
            DSLogger.reportError("Cannot find resource " + dirEntry + fileName + "!", null);
            return null;
        }

        Model result = new Model(fileName, texName);
        BufferedReader br = new BufferedReader(new InputStreamReader(objInput));
        List<Vector2f> uvs = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        String line;
        try {
            while ((line = br.readLine()) != null) {
                String[] things = line.split(" ");
                if (things[0].equals("v")) {
                    Vector3f pos = new Vector3f(Float.parseFloat(things[1]), Float.parseFloat(things[2]), Float.parseFloat(things[3]));
                    Vertex vertex = new Vertex(pos);
                    result.vertices.add(vertex);
                } else if (things[0].equals("vt")) {
                    Vector2f uv = new Vector2f(Float.parseFloat(things[1]), 1.0f - Float.parseFloat(things[2]));
                    uvs.add(uv);
                } else if (things[0].equals("vn")) {
                    Vector3f normal = new Vector3f(Float.parseFloat(things[1]), Float.parseFloat(things[2]), Float.parseFloat(things[3]));
                    normals.add(normal);
                } else if (things[0].equals("f")) {
                    String[] subThings = {things[1], things[2], things[3]};
                    for (String subThing : subThings) {
                        String[] data = subThing.split("/");
                        int index = Integer.parseInt(data[0]) - 1;
                        result.indices.add(index);
                        if (!data[1].isEmpty()) {
                            result.vertices.get(index).setUv(uvs.get(Integer.parseInt(data[1]) - 1));
                        }
                        if (!data[2].isEmpty()) {
                            result.vertices.get(index).setNormal(normals.get(Integer.parseInt(data[2]) - 1));
                        }
                    }
                }
            }
        } catch (IOException ex) {
            DSLogger.reportFatalError(ex.getMessage(), ex);
        } finally {
            try {
                objInput.close();
            } catch (IOException ex) {
                DSLogger.reportFatalError(ex.getMessage(), ex);
            }
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException ex) {
                    DSLogger.reportFatalError(ex.getMessage(), ex);
                }
            }
        }

        return result;
    }

    public void bufferVertices() {
        // storing vertices and normals in the buffer
        fb = BufferUtils.createFloatBuffer(vertices.size() * Vertex.SIZE);
        for (Vertex vertex : vertices) {
            if (vertex.isEnabled()) {
                fb.put(vertex.getPos().x);
                fb.put(vertex.getPos().y);
                fb.put(vertex.getPos().z);

                fb.put(vertex.getNormal().x);
                fb.put(vertex.getNormal().y);
                fb.put(vertex.getNormal().z);

                fb.put(vertex.getUv().x);
                fb.put(vertex.getUv().y);
            }
        }
        fb.flip();
        // storing vertices and normals buffer on the graphics card
        if (vbo == 0) {
            vbo = GL15.glGenBuffers();
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, fb, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    public void updateVertices() {
        // storing vertices and normals in the buffer        
        for (Vertex vertex : vertices) {
            if (vertex.isEnabled()) {
                fb.put(vertex.getPos().x);
                fb.put(vertex.getPos().y);
                fb.put(vertex.getPos().z);

                fb.put(vertex.getNormal().x);
                fb.put(vertex.getNormal().y);
                fb.put(vertex.getNormal().z);

                fb.put(vertex.getUv().x);
                fb.put(vertex.getUv().y);
            }
        }
        fb.flip();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, fb);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    public void bufferIndices() {
        // storing indices in the buffer
        IntBuffer ib = BufferUtils.createIntBuffer(indices.size());
        for (Integer index : indices) {
            ib.put(index);
        }
        ib.flip();
        // storing indices buffer on the graphics card
        if (ibo == 0) {
            ibo = GL15.glGenBuffers();
        }
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, ib, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public void bufferAll() { // explicit call to buffer unbuffered before the rendering
        bufferVertices();
        bufferIndices();
        buffered = true;
    }

    public void calcNormals() {
        for (int i = 0; i < indices.size(); i += 3) {
            int i0 = indices.get(i);
            int i1 = indices.get(i + 1);
            int i2 = indices.get(i + 2);

            Vector3f v1 = vertices.get(i1).getPos().sub(vertices.get(i0).getPos());
            Vector3f v2 = vertices.get(i2).getPos().sub(vertices.get(i0).getPos());

            Vector3f normal = v1.cross(v2).normalize();
            vertices.get(i0).setNormal(vertices.get(i0).getNormal().add(normal));
            vertices.get(i1).setNormal(vertices.get(i1).getNormal().add(normal));
            vertices.get(i2).setNormal(vertices.get(i2).getNormal().add(normal));
        }

        for (int i = 0; i < vertices.size(); i++) {
            vertices.get(i).setNormal(vertices.get(i).getNormal().normalize());
        }
    }

    public void nullifyNormals() {
        for (int i = 0; i < vertices.size(); i++) {
            vertices.get(i).setNormal(new Vector3f());
        }
    }

    public void render(ShaderProgram shaderProgram) {
        if (!buffered) {
            return; // this is very critical!!
        }

        Texture.enable();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);

        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glEnableVertexAttribArray(2);

        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 0); // this is for pos
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 12); // this is for normal
        GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, Vertex.SIZE * 4, 24); // this is for uv

        if (shaderProgram != null) {
            shaderProgram.bind();
            transform(shaderProgram);
            useLight(shaderProgram);
            setAlpha(shaderProgram);

            Texture primaryTexture = Texture.TEX_MAP.getOrDefault(texName, Texture.QMARK);
            if (primaryTexture != null) { // this is primary texture
                primaryColor(shaderProgram);
                primaryTexture.bind(0, shaderProgram, "modelTexture0");
            }

            if (waterTexture != null) { // this is reflective texture
                secondaryColor(shaderProgram);
                waterTexture.bind(1, shaderProgram, "modelTexture1");
            }
        }
        GL11.glDrawElements(GL11.GL_TRIANGLES, indices.size(), GL11.GL_UNSIGNED_INT, 0);
        Texture.unbind(0);
        Texture.unbind(1);
        ShaderProgram.unbind();

        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL20.glDisableVertexAttribArray(2);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        Texture.disable();
    }

    @Deprecated
    public void release() {
        if (buffered) {
            GL15.glDeleteBuffers(vbo);
            GL15.glDeleteBuffers(ibo);
        }
        buffered = false;
    }

    public Matrix4f calcModelMatrix() {
        Matrix4f translationMatrix = new Matrix4f().setTranslation(pos);
        Matrix4f rotationMatrix = new Matrix4f().setRotationXYZ(rX, rY, rZ);
        Matrix4f scaleMatrix = new Matrix4f().scale(scale);

        Matrix4f temp = new Matrix4f();
        modelMatrix = translationMatrix.mul(rotationMatrix.mul(scaleMatrix, temp), temp);

        return modelMatrix;
    }

    protected void transform(ShaderProgram shaderProgram) {
        calcModelMatrix();
        shaderProgram.updateUniform(modelMatrix, "modelMatrix");
    }

    protected void primaryColor(ShaderProgram shaderProgram) {
        shaderProgram.updateUniform(primaryColor, "modelColor0");
    }

    protected void secondaryColor(ShaderProgram shaderProgram) {
        shaderProgram.updateUniform(new Vector3f(1.0f, 1.0f, 1.0f), "modelColor2");
    }

    protected void useLight(ShaderProgram shaderProgram) {
        shaderProgram.updateUniform(light, "modelLight");
    }

    protected void setAlpha(ShaderProgram shaderProgram) {
        shaderProgram.updateUniform(solid ? 1.0f : 0.5f, "modelAlpha");
    }

    private void calcDims() {
        Vector3f vect = vertices.get(0).getPos();
        float xMin = vect.x;
        float yMin = vect.y;
        float zMin = vect.z;

        float xMax = vect.x;
        float yMax = vect.y;
        float zMax = vect.z;

        for (int i = 1; i < vertices.size(); i++) {
            vect = vertices.get(i).getPos();
            xMin = Math.min(xMin, vect.x);
            yMin = Math.min(yMin, vect.y);
            zMin = Math.min(zMin, vect.z);

            xMax = Math.max(xMax, vect.x);
            yMax = Math.max(yMax, vect.y);
            zMax = Math.max(zMax, vect.z);
        }

        width = Math.abs(xMax - xMin) * scale;
        height = Math.abs(yMax - yMin) * scale;
        depth = Math.abs(zMax - zMin) * scale;
    }

    public boolean containsInsideExactly(Vector3f x) {
        boolean ints = false;
        boolean boolX = x.x > pos.x - width / 2.0f && x.x < pos.x + width / 2.0f;
        boolean boolY = x.y > pos.y - height / 2.0f && x.y < pos.y + height / 2.0f;
        boolean boolZ = x.z > pos.z - depth / 2.0f && x.z < pos.z + depth / 2.0f;
        ints = boolX && boolY && boolZ;
        return ints;
    }

    public static boolean containsInsideExactly(Vector3f modelPos, float modelWidth, float modelHeight, float modelDepth, Vector3f x) {
        boolean ints = false;
        boolean boolX = x.x > modelPos.x - modelWidth / 2.0f && x.x < modelPos.x + modelWidth / 2.0f;
        boolean boolY = x.y > modelPos.y - modelHeight / 2.0f && x.y < modelPos.y + modelHeight / 2.0f;
        boolean boolZ = x.z > modelPos.z - modelDepth / 2.0f && x.z < modelPos.z + modelDepth / 2.0f;
        ints = boolX && boolY && boolZ;
        return ints;
    }

    public static boolean containsInsideEqually(Vector3f modelPos, float modelWidth, float modelHeight, float modelDepth, Vector3f x) {
        boolean ints = false;
        boolean boolX = x.x >= modelPos.x - modelWidth / 2.0f && x.x <= modelPos.x + modelWidth / 2.0f;
        boolean boolY = x.y >= modelPos.y - modelHeight / 2.0f && x.y <= modelPos.y + modelHeight / 2.0f;
        boolean boolZ = x.z >= modelPos.z - modelDepth / 2.0f && x.z <= modelPos.z + modelDepth / 2.0f;
        ints = boolX && boolY && boolZ;
        return ints;
    }

    public boolean containsInsideEqually(Vector3f x) {
        boolean ints = false;
        boolean boolX = x.x >= pos.x - width / 2.0f && x.x <= pos.x + width / 2.0f;
        boolean boolY = x.y >= pos.y - height / 2.0f && x.y <= pos.y + height / 2.0f;
        boolean boolZ = x.z >= pos.z - depth / 2.0f && x.z <= pos.z + depth / 2.0f;
        ints = boolX && boolY && boolZ;
        return ints;
    }

    public boolean containsOnXZEqually(Vector3f x) {
        boolean ints = false;
        boolean boolX = x.x >= pos.x - height / 2.0f && x.x <= pos.x + height / 2.0f;
        boolean boolY = x.y >= pos.y - height / 2.0f && x.y <= pos.y + height / 2.0f;
        boolean boolZ = x.z >= pos.z - height / 2.0f && x.z <= pos.z + height / 2.0f;
        ints = boolX && boolY && boolZ;
        return ints;
    }

    public boolean containsOnXZExactly(Vector3f x) {
        boolean ints = false;
        boolean boolX = x.x > pos.x - height / 2.0f && x.x < pos.x + height / 2.0f;
        boolean boolY = x.y > pos.y - height / 2.0f && x.y < pos.y + height / 2.0f;
        boolean boolZ = x.z > pos.z - height / 2.0f && x.z < pos.z + height / 2.0f;
        ints = boolX && boolY && boolZ;
        return ints;
    }

    public static boolean containsOnXZExactly(Vector3f modelPos, float modelHeight, Vector3f x) {
        boolean ints = false;
        boolean boolX = x.x > modelPos.x - modelHeight / 2.0f && x.x < modelPos.x + modelHeight / 2.0f;
        boolean boolY = x.y > modelPos.y - modelHeight / 2.0f && x.y < modelPos.y + modelHeight / 2.0f;
        boolean boolZ = x.z > modelPos.z - modelHeight / 2.0f && x.z < modelPos.z + modelHeight / 2.0f;
        ints = boolX && boolY && boolZ;
        return ints;
    }

    public static boolean containsOnXZEqually(Vector3f modelPos, float modelHeight, Vector3f x) {
        boolean ints = false;
        boolean boolX = x.x >= modelPos.x - modelHeight / 2.0f && x.x <= modelPos.x + modelHeight / 2.0f;
        boolean boolY = x.y >= modelPos.y - modelHeight / 2.0f && x.y <= modelPos.y + modelHeight / 2.0f;
        boolean boolZ = x.z >= modelPos.z - modelHeight / 2.0f && x.z <= modelPos.z + modelHeight / 2.0f;
        ints = boolX && boolY && boolZ;
        return ints;
    }

    public boolean intersectsExactly(Model model) {
        boolean coll = false;
        boolean boolX = this.pos.x - this.width / 2.0f < model.pos.x + model.width / 2.0f
                && this.pos.x + this.width / 2.0f > model.pos.x - model.width / 2.0f;
        boolean boolY = this.pos.y - this.height / 2.0f < model.pos.y + model.height / 2.0f
                && this.pos.y + this.height / 2.0f > model.pos.y - model.height / 2.0f;
        boolean boolZ = this.pos.z - this.depth / 2.0f < model.pos.z + model.depth / 2.0f
                && this.pos.z + this.depth / 2.0f > model.pos.z - model.depth / 2.0f;
        coll = boolX && boolY && boolZ;
        return coll;
    }

    public boolean intersectsExactly(Vector3f modelPos, float modelWidth, float modelHeight, float modelDepth) {
        boolean coll = false;
        boolean boolX = this.pos.x - this.width / 2.0f < modelPos.x + modelWidth / 2.0f
                && this.pos.x + this.width / 2.0f > modelPos.x - modelWidth / 2.0f;
        boolean boolY = this.pos.y - this.height / 2.0f < modelPos.y + modelHeight / 2.0f
                && this.pos.y + this.height / 2.0f > modelPos.y - modelHeight / 2.0f;
        boolean boolZ = this.pos.z - this.depth / 2.0f < modelPos.z + modelDepth / 2.0f
                && this.pos.z + this.depth / 2.0f > modelPos.z - modelDepth / 2.0f;
        coll = boolX && boolY && boolZ;
        return coll;
    }

    public static boolean intersectsExactly(Vector3f modelAPos, float modelAWidth, float modelAHeight, float modelADepth,
            Vector3f modelBPos, float modelBWidth, float modelBHeight, float modelBDepth) {
        boolean coll = false;
        boolean boolX = modelAPos.x - modelAWidth / 2.0f < modelBPos.x + modelBWidth / 2.0f
                && modelAPos.x + modelAWidth / 2.0f > modelBPos.x - modelBWidth / 2.0f;
        boolean boolY = modelAPos.y - modelAHeight / 2.0f < modelBPos.y + modelBHeight / 2.0f
                && modelAPos.y + modelAHeight / 2.0f > modelBPos.y - modelBHeight / 2.0f;
        boolean boolZ = modelAPos.z - modelBDepth / 2.0f < modelBPos.z + modelBDepth / 2.0f
                && modelAPos.z + modelBDepth / 2.0f > modelBPos.z - modelBDepth / 2.0f;
        coll = boolX && boolY && boolZ;
        return coll;
    }

    public boolean intersectsEqually(Model model) {
        boolean coll = false;
        boolean boolX = this.pos.x - this.width / 2.0f <= model.pos.x + model.width / 2.0f
                && this.pos.x + this.width / 2.0f >= model.pos.x - model.width / 2.0f;
        boolean boolY = this.pos.y - this.height / 2.0f <= model.pos.y + model.height / 2.0f
                && this.pos.y + this.height / 2.0f >= model.pos.y - model.height / 2.0f;
        boolean boolZ = this.pos.z - this.depth / 2.0f <= model.pos.z + model.depth / 2.0f
                && this.pos.z + this.depth / 2.0f >= model.pos.z - model.depth / 2.0f;
        coll = boolX && boolY && boolZ;
        return coll;
    }

    public boolean intersectsEqually(Vector3f modelPos, float modelWidth, float modelHeight, float modelDepth) {
        boolean coll = false;
        boolean boolX = this.pos.x - this.width / 2.0f <= modelPos.x + modelWidth / 2.0f
                && this.pos.x + this.width / 2.0f >= modelPos.x - modelWidth / 2.0f;
        boolean boolY = this.pos.y - this.height / 2.0f <= modelPos.y + modelHeight / 2.0f
                && this.pos.y + this.height / 2.0f >= modelPos.y - modelHeight / 2.0f;
        boolean boolZ = this.pos.z - this.depth / 2.0f <= modelPos.z + modelDepth / 2.0f
                && this.pos.z + this.depth / 2.0f >= modelPos.z - modelDepth / 2.0f;
        coll = boolX && boolY && boolZ;
        return coll;
    }

    public static boolean intersectsEqually(Vector3f modelAPos, float modelAWidth, float modelAHeight, float modelADepth,
            Vector3f modelBPos, float modelBWidth, float modelBHeight, float modelBDepth) {
        boolean coll = false;
        boolean boolX = modelAPos.x - modelAWidth / 2.0f <= modelBPos.x + modelBWidth / 2.0f
                && modelAPos.x + modelAWidth / 2.0f >= modelBPos.x - modelBWidth / 2.0f;
        boolean boolY = modelAPos.y - modelAHeight / 2.0f <= modelBPos.y + modelBHeight / 2.0f
                && modelAPos.y + modelAHeight / 2.0f >= modelBPos.y - modelBHeight / 2.0f;
        boolean boolZ = modelAPos.z - modelADepth / 2.0f <= modelBPos.z + modelBDepth / 2.0f
                && modelAPos.z + modelADepth / 2.0f >= modelBPos.z - modelBDepth / 2.0f;
        coll = boolX && boolY && boolZ;
        return coll;
    }

    public boolean intersectsRay(Vector3f l, Vector3f l0) {
        boolean ints = false; // l is direction and l0 is the point
        for (Vertex vertex : vertices) {
            Vector3f temp = new Vector3f();
            Vector3f x0 = vertex.getPos().add(pos, temp); // point on the plane translated
            Vector3f n = vertex.getNormal(); // normal of the plane
            if (l.dot(n) != 0.0f) {
                float d = x0.sub(l0).dot(n) / l.dot(n);
                Vector3f x = l.mul(d, temp).add(l0, temp);
                if (containsInsideEqually(x)) {
                    ints = true;
                    break;
                }
            }
        }
        return ints;
    }

    @Override
    public String toString() {
        return "Model{" + "modelFileName=" + modelFileName + ", texName = " + texName + ", pos=" + pos + ", scale=" + scale + ", color=" + primaryColor + ", solid=" + solid + '}';
    }

    public void animate() {
        for (int i = 0; i < indices.size(); i += 3) {
            Vertex a = vertices.get(indices.get(i));
            Vertex b = vertices.get(indices.get(i + 1));
            Vertex c = vertices.get(indices.get(i + 2));
            Vector2f temp = c.getUv();
            c.setUv(b.getUv());
            b.setUv(a.getUv());
            a.setUv(temp);
        }
    }

    @Override
    public int compareTo(Model model) {
        return Float.compare(this.getPos().y, model.getPos().y);
    }

    public void calcDimsPub() {
        calcDims();
        calcModelMatrix();
    }

    public float getSurfaceY() {
        return (this.pos.y + this.height / 2.0f);
    }

    public static float getSurfaceY(Vector3f modelPos, float modelHeight) {
        return (modelPos.y + modelHeight / 2.0f);
    }

    public float getBottomY() {
        return (this.pos.y - this.height / 2.0f);
    }

    public static float getBottomY(Vector3f modelPos, float modelHeight) {
        return (modelPos.y - modelHeight / 2.0f);
    }

    public String getModelFileName() {
        return modelFileName;
    }

    public List<Vertex> getVertices() {
        return vertices;
    }

    public List<Integer> getIndices() {
        return indices;
    }

    public Texture getWaterTexture() {
        return waterTexture;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public float getDepth() {
        return depth;
    }

    public int getVbo() {
        return vbo;
    }

    public int getIbo() {
        return ibo;
    }

    public Vector3f getPos() {
        return pos;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
        this.width *= scale;
        this.height *= scale;
        this.depth *= scale;
    }

    public float getrX() {
        return rX;
    }

    public void setrX(float rX) {
        this.rX = rX;
    }

    public float getrY() {
        return rY;
    }

    public void setrY(float rY) {
        this.rY = rY;
    }

    public float getrZ() {
        return rZ;
    }

    public void setrZ(float rZ) {
        this.rZ = rZ;
    }

    public Vector3f getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(Vector3f primaryColor) {
        this.primaryColor = primaryColor;
    }

    public Vector3f getLight() {
        return light;
    }

    public void setLight(Vector3f light) {
        this.light = light;
    }

    public boolean isSolid() {
        return solid;
    }

    public void setSolid(boolean solid) {
        this.solid = solid;
    }

    public void setPos(Vector3f pos) {
        this.pos = pos;
    }

    public String getTexName() {
        return texName;
    }

    public void setTexName(String texName) {
        this.texName = texName;
    }

    public void setWaterTexture(Texture waterTexture) {
        this.waterTexture = waterTexture;
    }

    public boolean isBuffered() {
        return buffered;
    }

    public Matrix4f getModelMatrix() {
        return modelMatrix;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Model) {
            Model that = (Model) obj;
            return (this.vertices.equals(that.vertices)
                    && this.indices.equals(that.indices)
                    && this.pos.equals(that.pos)
                    && this.texName.equals(that.texName)
                    && this.primaryColor.equals(that.primaryColor)
                    && this.width == that.width
                    && this.height == that.height
                    && this.depth == that.depth
                    && this.solid == that.solid);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + Objects.hashCode(this.vertices);
        hash = 43 * hash + Objects.hashCode(this.indices);
        hash = 43 * hash + Objects.hashCode(this.texName);
        hash = 43 * hash + Float.floatToIntBits(this.width);
        hash = 43 * hash + Float.floatToIntBits(this.height);
        hash = 43 * hash + Float.floatToIntBits(this.depth);
        hash = 43 * hash + Objects.hashCode(this.pos);
        hash = 43 * hash + Objects.hashCode(this.primaryColor);
        hash = 43 * hash + (this.solid ? 1 : 0);
        return hash;
    }

}
