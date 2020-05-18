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
package rs.alexanderstojanovich.evg.texture;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 *
 * @author Coa
 */
public class Texture {

    private final Image image;
    private int textureID;
    private boolean buffered = false;

    public static final Texture LOGO = new Texture(Game.INTRFACE_ENTRY, "ds_title_gray.png");
    public static final Texture CROSSHAIR = new Texture(Game.INTRFACE_ENTRY, "crosshairUltimate.png");
    public static final Texture MINIGUN = new Texture(Game.INTRFACE_ENTRY, "minigun.png");
    public static final Texture FONT = new Texture(Game.INTRFACE_ENTRY, "font.png");
    public static final Texture DOOM0 = new Texture(Game.WORLD_ENTRY, "doom0.png");
    public static final Texture CRATE = new Texture(Game.WORLD_ENTRY, "crate.png");
    public static final Texture STONE = new Texture(Game.WORLD_ENTRY, "stone.png");
    public static final Texture WATER = new Texture(Game.WORLD_ENTRY, "water.png");
    public static final Texture NIGHT = new Texture(Game.WORLD_ENTRY, "night.png");
    public static final Texture MARBLE = new Texture(Game.WORLD_ENTRY, "marble.png");
    public static final Texture QMARK = new Texture(Game.WORLD_ENTRY, "qmark.png");
    public static final Texture DECAL = new Texture(Game.WORLD_ENTRY, "decal.png");

    public static final Texture PISTOL = new Texture(Game.PLAYER_ENTRY, "pistol.png");
    public static final Texture ASSAULT_RIFLE = new Texture(Game.PLAYER_ENTRY, "assault_rifle.png");
    public static final Texture SHOTGUN = new Texture(Game.PLAYER_ENTRY, "shotgun.png");
    public static final Texture SUB_MACHINE_GUN = new Texture(Game.PLAYER_ENTRY, "sub_machine_gun.png");
    public static final Texture MACHINE_GUN = new Texture(Game.PLAYER_ENTRY, "machine_gun.png");
    public static final Texture SNIPER_RIFLE = new Texture(Game.PLAYER_ENTRY, "sniper_rifle.png");

    public static final Texture CONSOLE = new Texture(Game.INTRFACE_ENTRY, "console.png");
    public static final Texture LIGHT_BULB = new Texture(Game.INTRFACE_ENTRY, "lbulb.png");
    
    public static final Map<String, Texture> TEX_MAP = new HashMap<>();

    static {
        // interface stuff
        TEX_MAP.put("logox", LOGO);
        TEX_MAP.put("xhair", CROSSHAIR);
        TEX_MAP.put("minigun", MINIGUN);
        TEX_MAP.put("font", FONT);
        TEX_MAP.put("console", CONSOLE);
        TEX_MAP.put("lbulb", LIGHT_BULB);
        // world stuff
        TEX_MAP.put("doom0", DOOM0);
        TEX_MAP.put("crate", CRATE);
        TEX_MAP.put("stone", STONE);
        TEX_MAP.put("water", WATER);
        TEX_MAP.put("night", NIGHT);
        TEX_MAP.put("marble", MARBLE);
        TEX_MAP.put("qmark", QMARK);
        TEX_MAP.put("decal", DECAL);
        // player stuff
        TEX_MAP.put("pistol", Texture.PISTOL);
        TEX_MAP.put("assrifle", Texture.ASSAULT_RIFLE);
        TEX_MAP.put("shotgun", Texture.SHOTGUN);
        TEX_MAP.put("smg", Texture.SUB_MACHINE_GUN);
        TEX_MAP.put("machgun", Texture.MACHINE_GUN);
        TEX_MAP.put("sniper", Texture.SNIPER_RIFLE);
    }

    public Texture(int width, int height) {
        this.image = new Image(width, height);
    }

    public Texture(String subDir, String fileName) {
        this.image = new Image(subDir, fileName);
    }

    public void bufferAll() {
        loadToGraphicCard();
        buffered = true;
    }

    public static void bufferAllTextures() {
        for (Texture texture : TEX_MAP.values()) {
            texture.bufferAll();
        }
    }

    private void loadToGraphicCard() {
        textureID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        // Set the texture wrapping parameters
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);// Set texture wrapping to GL_REPEAT (usually basic wrapping method)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        // Set texture filtering parameters
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, image.getWidth(), image.getHeight(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, image.getContent());
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    public void bind(ShaderProgram shaderProgram, String textureUniformName) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        int uniformLocation = GL20.glGetUniformLocation(shaderProgram.getProgram(), textureUniformName);
        GL20.glUniform1i(uniformLocation, 0);
    }

    public void bind(int textureUnitNum, ShaderProgram shaderProgram, String textureUniformName) {
        if (textureUnitNum >= 0 && textureUnitNum <= 7) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + textureUnitNum);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
            int uniformLocation = GL20.glGetUniformLocation(shaderProgram.getProgram(), textureUniformName);
            GL20.glUniform1i(uniformLocation, textureUnitNum);
        }
    }

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
        if (this.buffered != other.buffered) {
            return false;
        }
        if (!Objects.equals(this.image, other.image)) {
            return false;
        }
        return true;
    }

    public Image getImage() {
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

}
