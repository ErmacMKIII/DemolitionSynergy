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
package rs.alexanderstojanovich.evg.texture;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;
import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.IList;
import rs.alexanderstojanovich.evg.main.Configuration;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.util.DSLogger;
import rs.alexanderstojanovich.evg.util.ImageUtils;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Texture {

    private final BufferedImage image;
    private final String texName;
    private int textureID = 0;
    private boolean buffered = false;
    public static final int TEX_SIZE = Configuration.getInstance().getTextureSize();
    public static final Map<String, TexValue> TEX_STORE = new LinkedHashMap<>();

    public static final String[] TEX_WORLD = {"crate", "doom0", "stone", "water", "reflc"};
    public static final int GRID_SIZE_WORLD = 3;

    public static final Texture DECAL = new Texture(Game.WORLD_ENTRY, "decal.png");
    public static final Texture QMARK = new Texture(Game.WORLD_ENTRY, "qmark.png");
    public static final TexValue QMARK_TV = new TexValue(QMARK, -1);

    public static final Texture SUN = new Texture(Game.WORLD_ENTRY, "suntx.png");
    public static final Texture NIGHT = new Texture(Game.WORLD_ENTRY, "night.png");

    public static final Texture LOGO = new Texture(Game.INTRFACE_ENTRY, "ds_title_gray.png");
    public static final Texture CROSSHAIR = new Texture(Game.INTRFACE_ENTRY, "crosshairUltimate.png");
    public static final Texture MINIGUN = new Texture(Game.INTRFACE_ENTRY, "minigun.png");
    public static final Texture FONT = new Texture(Game.INTRFACE_ENTRY, "font.png");
    public static final Texture CONSOLE = new Texture(Game.INTRFACE_ENTRY, "console.png");
    public static final Texture LIGHT_BULB = new Texture(Game.INTRFACE_ENTRY, "lbulb.png");

    public static final Texture ALEX = new Texture(Game.CHARACTER_ENTRY, "alex.png");
    public static final Texture STEVE = new Texture(Game.CHARACTER_ENTRY, "steve.png");

    public static final String[] TEX_PLAYER_WEAPONS = {
        "W01M9", "W02M1", "W03DE", "W04UZ",
        "W05M5", "W06P9", "W07AK", "W08M4",
        "W09G3", "W10M6", "W11MS", "W12W2",
        "W13B9", "W14R7", "W15DR", "W16M8"
    };
    public static final int GRID_SIZE_PLAYER = 4;

    public static final Texture WORLD = Texture.buildTextureAtlas("WORLD", Game.WORLD_ENTRY, TEX_WORLD, GRID_SIZE_WORLD);
    public static final Texture PLAYER_WEAPONS = Texture.buildTextureAtlas("WEAPONS", Game.PLAYER_ENTRY, TEX_PLAYER_WEAPONS, GRID_SIZE_PLAYER);

    public static IList<String> LIGHT_TEX_LIST = new GapList<String>() {
        {
            add("suntx");
            add("reflc");
        }
    };

    /**
     * Creates blank Texture (TEXSIZE x TEXSIZE)
     *
     * @param texName texture name
     */
    public Texture(String texName) {
        this.image = new BufferedImage(TEX_SIZE, TEX_SIZE, BufferedImage.TYPE_INT_ARGB);
        this.texName = texName;
        Texture.TEX_STORE.put(texName, new TexValue(this, -1));
    }

    /**
     * Creates Texture from the zip entry (or extracted zip)
     *
     * @param subDir directory or entry where file is located
     * @param fileName filename of the image (future texture)
     */
    public Texture(String subDir, String fileName) {
        this.image = ImageUtils.loadImage(subDir, fileName);
        this.texName = fileName.substring(0, fileName.indexOf("."));
        Texture.TEX_STORE.put(texName, new TexValue(this, -1));
    }

    public void bufferAll() {
        loadTexture();
        buffered = true;
    }

    public static void bufferAllTextures() {
        // intrface
        LOGO.bufferAll();
        CROSSHAIR.bufferAll();
        MINIGUN.bufferAll();
        FONT.bufferAll();
        CONSOLE.bufferAll();
        LIGHT_BULB.bufferAll();
        // world        
        DECAL.bufferAll();
        SUN.bufferAll();
        QMARK.bufferAll();
        WORLD.bufferAll();
        NIGHT.bufferAll();
        // player
        ALEX.bufferAll();
        STEVE.bufferAll();
        PLAYER_WEAPONS.bufferAll();

        DSLogger.reportDebug("Textures loaded!", null);
    }

    private void loadTexture() {
        textureID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        // Set the texture wrapping parameters
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);// Set texture wrapping to GL_REPEAT (usually basic wrapping method)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        // Set texture filtering parameters
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        // get the content as ByteBuffer
        ByteBuffer imageDataBuffer = ImageUtils.getImageDataBuffer(image, Texture.TEX_SIZE);

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, TEX_SIZE, TEX_SIZE, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, imageDataBuffer);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        if (imageDataBuffer.capacity() != 0) {
            MemoryUtil.memFree(imageDataBuffer);
        }
    }

    /**
     * Binds this texture as active for use
     *
     * @param shaderProgram provided shader program
     * @param textureUniformName texture uniform name in the fragment shader
     */
    public void bind(ShaderProgram shaderProgram, String textureUniformName) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        int uniformLocation = GL20.glGetUniformLocation(shaderProgram.getProgram(), textureUniformName);
        GL20.glUniform1i(uniformLocation, 0);
    }

    /**
     * Binds this texture as active for use and specifying texture unit for it
     * (from 0 to 7)
     *
     * @param textureUnitNum texture unit number
     * @param shaderProgram provided shader program
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

    /**
     * Unbinds this texture as active from use
     */
    public static void unbind() {
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

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

    public void release() {
        GL11.glDeleteTextures(textureID);
    }

    public static void releaseAllTextures() {
        for (TexValue tv : TEX_STORE.values()) {
            tv.texture.release();
        }
        DSLogger.reportDebug("All textures deleted!", null);
    }

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
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Texture other = (Texture) obj;
        if (this.textureID != other.textureID) {
            return false;
        }
        if (!Objects.equals(this.image, other.image)) {
            return false;
        }
        return true;
    }

    /**
     * Build Texture Atlas from various textures.
     *
     * @param atlasName atlas (texture) name
     * @param subDir Subdirectory in dsynergy.zip
     * @param texNames texture names to build atlas from.
     * @param gridSize must be square root of number of textures.
     * @return Texture Atlas as one big Texture.
     */
    public static Texture buildTextureAtlas(String atlasName, String subDir, String[] texNames, int gridSize) {
        Texture result = new Texture(atlasName);
        Graphics2D g2d = result.image.createGraphics();
        final int texUnitSize = Math.round(TEX_SIZE / (float) gridSize);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setColor(new Color(0.0f, 0.0f, 0.0f, 0.0f));
        int index = 0;
        OUTER:
        for (String texName : texNames) {
            String fileName = texName;
            if (!fileName.toLowerCase().endsWith(".png")) {
                fileName += ".png";
            }
            BufferedImage image = ImageUtils.loadImage(subDir, fileName);

            int row = index / gridSize;
            int col = index % gridSize;

            int x = row * texUnitSize;
            int y = col * texUnitSize;

            g2d.drawImage(image, x, y, texUnitSize, texUnitSize, null);

            TexValue texValue = new TexValue(result, index);
            TEX_STORE.put(texName, texValue);
            index++;
        }

        return result;
    }

    /**
     * Get Texture from Texture Store with texture name.
     *
     * @param texName texture name (alias)
     * @return texture name
     */
    public static Texture getOrDefault(String texName) {
        return TEX_STORE.getOrDefault(texName, Texture.QMARK_TV).texture;
    }

    /**
     * Get Texture Index from Texture Store with texture name.
     *
     * @param texName texture name (alias)
     * @return texture index
     */
    public static int getOrDefaultIndex(String texName) {
        return TEX_STORE.getOrDefault(texName, Texture.QMARK_TV).value;
    }

    @Override
    public String toString() {
        return "Texture{" + "texName=" + texName + ", textureID=" + textureID + ", buffered=" + buffered + '}';
    }

    public BufferedImage getImage() {
        return image;
    }

    public int getTextureID() {
        return textureID;
    }

    public void setTextureID(int textureID) {
        this.textureID = textureID;
    }

    public boolean isBuffered() {
        return buffered;
    }

    public String getTexName() {
        return texName;
    }

    public static boolean isLightSource(String texName) {
        return LIGHT_TEX_LIST.contains(texName);
    }
}
