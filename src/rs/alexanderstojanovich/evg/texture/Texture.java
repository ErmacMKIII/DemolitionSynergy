/*
 * Copyright (C) 2020 Aleksandar Stojanovic <coas91@rocketmail.com>
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
package rs.alexanderstojanovich.evg.texture;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.lwjgl.opengl.*;
import rs.alexanderstojanovich.evg.main.Configuration;
import rs.alexanderstojanovich.evg.resources.Assets;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.ImageUtils;

/**
 * Texture class responsible for loading and buffering textures.
 * Texture data is stored in the BufferedImage and can be buffered to OpenGL when needed.
 * Texture format and type are determined by the Format and Type enums.
 * Texture store is a static map of texture names (aliases) to their TexValue
 * (texture reference, index in atlas, grid size).
 * Texture atlas can be built from multiple textures and stored in the texture
 * store with an alias.
 * World textures are loaded into a GL_TEXTURE_2D_ARRAY for efficient sampling
 * in the shader.
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class Texture implements TextureIfc {

    /** Texture format (colorRGBA/depth) flag. */
    public static enum Format {
        NONE, RGB5_A1, RGBA8, DEPTH24
    }

    /** Texture data type for loading. */
    public static enum Type {
        UNSINGED_BYTE, FLOAT
    }

    /** If true, texture is loaded as GL_TEXTURE_2D_ARRAY. */
    public boolean isArray = false;

    /** Texture format flag. */
    protected Format texFmt = Format.NONE;

    /** Original image data for this texture. */
    private final BufferedImage image;

    /** Texture name (alias). */
    private final String texName;

    /**
     * Sub-directory used to locate texture files.
     * Required for array textures so {@code loadTexture} can load each layer.
     */
    private String texDir = "";

    /** OpenGL texture ID after buffering. */
    private int textureID = 0;
    private boolean buffered = false;

    /** Fixed tile size for every layer in the texture array. */
    public static final int ARRAY_TILE_SIZE = 256;

    /** Placeholder for Empty texture. */
    public static final Texture EMPTY = new Texture("EMPTY", Format.NONE);
    /** Placeholder for Empty value (from 'Empty' texture). */
    public static final TexValue EMPTY_VALUE = new TexValue(Texture.EMPTY, -1, 1);

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    /**
     * Creates blank Texture (TEXSIZE x TEXSIZE).
     *
     * @param texName texture name
     * @param texFmt  colorRGBA/depth format flag
     */
    public Texture(String texName, Format texFmt) {
        this.texFmt = texFmt;
        this.image = new BufferedImage(TEX_SIZE, TEX_SIZE, BufferedImage.TYPE_INT_ARGB);
        this.texName = texName;
        Texture.TEX_STORE.put(texName, new TexValue(this, -1, 1));
    }

    /**
     * Creates Texture from the zip entry (or extracted zip).
     *
     * @param subDir   directory or entry where file is located
     * @param fileName filename of the image
     * @param texFmt   colorRGBA/depth format flag
     */
    public Texture(String subDir, String fileName, Format texFmt) {
        this.texFmt = texFmt;
        this.texDir = subDir;
        this.image = ImageUtils.loadImage(subDir, fileName);
        this.texName = fileName.substring(0, fileName.indexOf("."));
        Texture.TEX_STORE.put(texName, new TexValue(this, -1, 1));
    }

    // -----------------------------------------------------------------------
    // bufferAll
    // -----------------------------------------------------------------------

    /**
     * Buffers texture data to OpenGL.
     * If image data is null texture is generated empty.
     */
    public void bufferAll() {
        ByteBuffer imgDatBuff = ImageUtils.getImageDataBuffer(image, TEX_SIZE);
        switch (texFmt) {
            case NONE:
                loadTexture(imgDatBuff, GL11.GL_RGBA, GL11.GL_RGBA, Type.UNSINGED_BYTE);
                break;
            case RGB5_A1:
                loadTexture(imgDatBuff, GL11.GL_RGB5_A1, GL11.GL_RGBA, Type.UNSINGED_BYTE);
                break;
            case RGBA8:
                loadTexture(imgDatBuff, GL11.GL_RGBA8, GL11.GL_RGBA, Type.UNSINGED_BYTE);
                break;
            case DEPTH24:
                loadTexture(imgDatBuff, GL14.GL_DEPTH_COMPONENT24, GL14.GL_DEPTH_COMPONENT, Type.FLOAT);
                break;
        }
        buffered = true;
    }

    /**
     * Buffers texture data to OpenGL using a pre-built buffer.
     *
     * @param texture    texture to load to
     * @param imgDatBuff (nullable) image data
     */
    public static void bufferAll(Texture texture, ByteBuffer imgDatBuff) {
        switch (texture.texFmt) {
            case NONE:
                texture.loadTexture(imgDatBuff, GL11.GL_RGBA, GL11.GL_RGBA, Type.UNSINGED_BYTE);
                break;
            case RGB5_A1:
                texture.loadTexture(imgDatBuff, GL11.GL_RGB5_A1, GL11.GL_RGBA, Type.UNSINGED_BYTE);
                break;
            case RGBA8:
                texture.loadTexture(imgDatBuff, GL11.GL_RGBA8, GL11.GL_RGBA, Type.UNSINGED_BYTE);
                break;
            case DEPTH24:
                texture.loadTexture(imgDatBuff, GL14.GL_DEPTH_COMPONENT24, GL14.GL_DEPTH_COMPONENT, Type.FLOAT);
                break;
        }
        texture.buffered = true;
    }

    /** Buffers the EMPTY texture (used for water etc.). */
    public static void bufferAllTextures() {
        Texture.bufferAll(EMPTY, null);
    }

    // -----------------------------------------------------------------------
    // loadTexture — merged with loadTextureArray
    // -----------------------------------------------------------------------

    /**
     * Internal method to load texture data into OpenGL.
     * For non-array textures loads as {@code GL_TEXTURE_2D}.
     * For array textures loads all {@link Assets#TEX_WORLD} layers into a
     * {@code GL_TEXTURE_2D_ARRAY}, ignoring {@code imgDatBuff} (each layer is
     * loaded individually from {@link #texDir}).
     *
     * @param imgDatBuff image data buffer (nullable; ignored for array textures)
     * @param internFmt  internal format for OpenGL
     * @param pixFmt     pixel format for OpenGL
     * @param type       data type for loading
     */
    private void loadTexture(ByteBuffer imgDatBuff, int internFmt, int pixFmt, Type type) {
        // Release any previously allocated texture
        if (textureID != 0) {
            GL11.glDeleteTextures(textureID);
            textureID = 0;
        }

        textureID = GL11.glGenTextures();

        // ----------------------------------------------------------------
        // Regular GL_TEXTURE_2D path
        // ----------------------------------------------------------------
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);

        switch (type) {
            case UNSINGED_BYTE:
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S,     GL11.GL_REPEAT);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T,     GL11.GL_REPEAT);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internFmt,
                        TEX_SIZE, TEX_SIZE, 0,
                        pixFmt, GL11.GL_UNSIGNED_BYTE, imgDatBuff);
                break;

            case FLOAT:
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S,     GL11.GL_CLAMP);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T,     GL11.GL_CLAMP);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internFmt,
                        TEX_SIZE, TEX_SIZE, 0,
                        GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, imgDatBuff);
                break;
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    // -----------------------------------------------------------------------
    // bind / unbind
    // -----------------------------------------------------------------------
    /**
     * Binds this texture as active for use.
     *
     * @param shaderProgram      provided shader program
     * @param textureUniformName texture uniform name in the fragment shader
     */
    public void bind(ShaderProgram shaderProgram, String textureUniformName) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        int uniformLocation = GL20.glGetUniformLocation(shaderProgram.getProgram(), textureUniformName);
        GL20.glUniform1i(uniformLocation, 0);
    }

    /**
     * Binds this texture to a specific texture unit (0–7).
     *
     * @param textureUnitNum     texture unit number
     * @param shaderProgram      provided shader program
     * @param textureUniformName texture uniform name in the fragment shader
     */
    public void bind(int textureUnitNum, ShaderProgram shaderProgram, String textureUniformName) {
        if (textureUnitNum >= 0 && textureUnitNum <= 7) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + textureUnitNum);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
            int uniformLocation = GL20.glGetUniformLocation(shaderProgram.getProgram(), textureUniformName);
            GL20.glUniform1i(uniformLocation, textureUnitNum);
        }
    }

    /** Unbinds any texture from unit 0. */
    public static void unbind() {
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    /**
     * Unbinds any texture from the given unit (0–7).
     *
     * @param textureUnitNum texture unit number
     */
    public static void unbind(int textureUnitNum) {
        if (textureUnitNum >= 0 && textureUnitNum <= 7) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + textureUnitNum);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        }
    }

    public static void enable() {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    public static void disable() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    // -----------------------------------------------------------------------
    // Release
    // -----------------------------------------------------------------------

    public void release() {
        GL11.glDeleteTextures(textureID);
    }

    public static void releaseAllTextures() {
        for (TexValue tv : TEX_STORE.values()) {
            if (tv != null && tv.texture != null) {
                tv.texture.release();
            }
        }
        TEX_STORE.clear();
        DSLogger.reportDebug("All textures deleted!", null);
    }

    // -----------------------------------------------------------------------
    // Atlas builder
    // -----------------------------------------------------------------------

    /**
     * Build Texture Atlas from various textures.
     *
     * @param atlasName atlas (texture) name
     * @param subDir    subdirectory in dsynergy.zip
     * @param texNames  texture names to build atlas from
     * @param gridSize  must be square root of number of textures
     * @param format    texture format
     * @return Texture Atlas as one big Texture
     */
    public static Texture buildTextureAtlas(String atlasName, String subDir,
                                            String[] texNames, int gridSize, Format format) {
        Texture result = new Texture(atlasName, format);
        Graphics2D g2d = result.image.createGraphics();
        final int texUnitSize = Math.round(TEX_SIZE / (float) gridSize);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING,     RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setColor(new Color(0.0f, 0.0f, 0.0f, 0.0f));

        int index = 0;
        OUTER:
        for (String texName : texNames) {
            String fileName = texName;
            if (!fileName.toLowerCase().endsWith(".png")) {
                fileName += ".png";
            }

            BufferedImage tile = ImageUtils.loadImage(subDir, fileName);
            if (tile == null) {
                DSLogger.reportWarning("Atlas tile not found: " + texName, null);
                index++;
                continue;
            }
            int col = index % gridSize;
            int row = index / gridSize;
            if (row >= gridSize) break OUTER;
            g2d.drawImage(tile, col * texUnitSize, row * texUnitSize,
                    texUnitSize, texUnitSize, null);
            TEX_STORE.put(texName, new TexValue(result, index, gridSize));
            index++;
        }
        g2d.dispose();
        return result;
    }

    // -----------------------------------------------------------------------
    // Object overrides & getters/setters
    // -----------------------------------------------------------------------

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + Objects.hashCode(this.image);
        hash = 73 * hash + this.textureID;
        hash = 73 * hash + (this.buffered ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final Texture other = (Texture) obj;
        if (this.textureID != other.textureID) return false;
        return Objects.equals(this.image, other.image);
    }

    @Override
    public String toString() {
        return "Texture{texName=" + texName
                + ", textureID=" + textureID
                + ", buffered=" + buffered + '}';
    }

    public BufferedImage getImage()             { return image; }
    public int           getTextureID()         { return textureID; }
    public void          setTextureID(int id)   { this.textureID = id; }
    public boolean       isBuffered()           { return buffered; }
    public String        getTexName()           { return texName; }
    public Format        getTexFmt()            { return texFmt; }
}
