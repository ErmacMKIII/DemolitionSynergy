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

import de.matthiasmann.twl.utils.PNGDecoder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import rs.alexanderstojanovich.evg.main.Game;
import rs.alexanderstojanovich.evg.models.Model;

/**
 *
 * @author Coa
 */
public class Image {

    private String fileName;
    private int width;
    private int height;
    private ByteBuffer content;

    public Image(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Image(String subDir, String fileName) {
        this.fileName = fileName;
        loadImage(subDir, fileName);
    }

    private void loadImage(String subDir, String fileName) {
        InputStream in = getClass().getResourceAsStream(Game.RESOURCES_DIR + subDir + fileName);
        try {
            PNGDecoder decoder = new PNGDecoder(in);
            // Set the width and height of the image
            width = decoder.getWidth();
            height = decoder.getHeight();
            // Decode the PNG file in a ByteBuffer
            content = ByteBuffer.allocateDirect(4 * width * height);
            decoder.decode(content, width * 4, PNGDecoder.Format.RGBA);
            content.flip();
            in.close();
        } catch (IOException ex) {
            Logger.getLogger(Model.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public ByteBuffer getContent() {
        return content;
    }

    public void setContent(ByteBuffer content) {
        this.content = content;
    }

}
