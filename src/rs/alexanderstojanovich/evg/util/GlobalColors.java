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
package rs.alexanderstojanovich.evg.util;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class GlobalColors { // some of the defined colors

    public static enum ColorName {
        WHITE, RED, GREEN, BLUE, CYAN, MAGENTA, YELLOW, GRAY,
        DARK_RED, DARK_GREEN, DARK_BLUE, DARK_CYAN, DARK_MAGENTA, DARK_YELLOW, CUSTOM;

        public static String[] names() {
            return Arrays.toString(ColorName.values()).replaceAll("^.|.$", "").split(", ");
        }
    }

    public static final Vector4f TRANSPARENT = new Vector4f();
    public static final Vector3f BLACK = new Vector3f();
    public static final Vector3f WHITE = new Vector3f(1.0f, 1.0f, 1.0f);

    public static final Vector3f RED = new Vector3f(1.0f, 0.0f, 0.0f);
    public static final Vector3f GREEN = new Vector3f(0.0f, 1.0f, 0.0f);
    public static final Vector3f BLUE = new Vector3f(0.0f, 0.0f, 1.0f);

    public static final Vector3f CYAN = new Vector3f(0.0f, 1.0f, 1.0f);
    public static final Vector3f MAGENTA = new Vector3f(1.0f, 0.0f, 1.0f);
    public static final Vector3f YELLOW = new Vector3f(1.0f, 1.0f, 0.0f);

    public static final Vector3f GRAY = new Vector3f(0.5f, 0.5f, 0.5f);

    public static final Vector3f DARK_RED = new Vector3f(0.5f, 0.0f, 0.0f);
    public static final Vector3f DARK_GREEN = new Vector3f(0.0f, 0.5f, 0.0f);
    public static final Vector3f DARK_BLUE = new Vector3f(0.0f, 0.0f, 0.5f);

    public static final Vector3f DARK_CYAN = new Vector3f(0.0f, 0.5f, 0.5f);
    public static final Vector3f DARK_MAGENTA = new Vector3f(0.5f, 0.0f, 0.5f);
    public static final Vector3f DARK_YELLOW = new Vector3f(0.5f, 0.5f, 0.0f);

    public static final Vector4f BLACK_RGBA = new Vector4f(BLACK, 1.0f);
    public static final Vector4f WHITE_RGBA = new Vector4f(WHITE, 1.0f);

    public static final Vector4f RED_RGBA = new Vector4f(RED, 1.0f);
    public static final Vector4f GREEN_RGBA = new Vector4f(GREEN, 1.0f);
    public static final Vector4f BLUE_RGBA = new Vector4f(BLUE, 1.0f);

    public static final Vector4f CYAN_RGBA = new Vector4f(CYAN, 1.0f);
    public static final Vector4f MAGENTA_RGBA = new Vector4f(MAGENTA, 1.0f);
    public static final Vector4f YELLOW_RGBA = new Vector4f(YELLOW, 1.0f);

    public static final Vector4f GRAY_RGBA = new Vector4f(GRAY, 1.0f);

    public static final Vector4f DARK_RED_RGBA = new Vector4f(DARK_RED, 1.0f);
    public static final Vector4f DARK_GREEN_RGBA = new Vector4f(DARK_GREEN, 1.0f);
    public static final Vector4f DARK_BLUE_RGBA = new Vector4f(DARK_BLUE, 1.0f);

    public static final Vector4f DARK_CYAN_RGBA = new Vector4f(DARK_CYAN, 1.0f);
    public static final Vector4f DARK_MAGENTA_RGBA = new Vector4f(DARK_MAGENTA, 1.0f);
    public static final Vector4f DARK_YELLOW_RGBA = new Vector4f(DARK_YELLOW, 1.0f);

    public static final Map<ColorName, Vector3f> NAME_TO_COLOR_RGB = new LinkedHashMap<>();

    public static final Map<ColorName, Vector4f> NAME_TO_COLOR_RGBA = new LinkedHashMap<>();

    static {
//        NAME_TO_COLOR_RGB.put(ColorName.BLACK, BLACK);
        NAME_TO_COLOR_RGB.put(ColorName.WHITE, WHITE);

        NAME_TO_COLOR_RGB.put(ColorName.RED, RED);
        NAME_TO_COLOR_RGB.put(ColorName.GREEN, GREEN);
        NAME_TO_COLOR_RGB.put(ColorName.BLUE, BLUE);

        NAME_TO_COLOR_RGB.put(ColorName.CYAN, CYAN);
        NAME_TO_COLOR_RGB.put(ColorName.MAGENTA, MAGENTA);
        NAME_TO_COLOR_RGB.put(ColorName.YELLOW, YELLOW);

        NAME_TO_COLOR_RGB.put(ColorName.GRAY, GRAY);

        NAME_TO_COLOR_RGB.put(ColorName.DARK_RED, DARK_RED);
        NAME_TO_COLOR_RGB.put(ColorName.DARK_GREEN, DARK_GREEN);
        NAME_TO_COLOR_RGB.put(ColorName.DARK_BLUE, DARK_BLUE);

        NAME_TO_COLOR_RGB.put(ColorName.DARK_CYAN, DARK_CYAN);
        NAME_TO_COLOR_RGB.put(ColorName.DARK_MAGENTA, DARK_MAGENTA);
        NAME_TO_COLOR_RGB.put(ColorName.DARK_YELLOW, DARK_YELLOW);

        NAME_TO_COLOR_RGBA.put(ColorName.WHITE, WHITE_RGBA);

        NAME_TO_COLOR_RGBA.put(ColorName.RED, RED_RGBA);
        NAME_TO_COLOR_RGBA.put(ColorName.GREEN, GREEN_RGBA);
        NAME_TO_COLOR_RGBA.put(ColorName.BLUE, BLUE_RGBA);

        NAME_TO_COLOR_RGBA.put(ColorName.CYAN, CYAN_RGBA);
        NAME_TO_COLOR_RGBA.put(ColorName.MAGENTA, MAGENTA_RGBA);
        NAME_TO_COLOR_RGBA.put(ColorName.YELLOW, YELLOW_RGBA);

        NAME_TO_COLOR_RGBA.put(ColorName.GRAY, GRAY_RGBA);

        NAME_TO_COLOR_RGBA.put(ColorName.DARK_RED, DARK_RED_RGBA);
        NAME_TO_COLOR_RGBA.put(ColorName.DARK_GREEN, DARK_GREEN_RGBA);
        NAME_TO_COLOR_RGBA.put(ColorName.DARK_BLUE, DARK_BLUE_RGBA);

        NAME_TO_COLOR_RGBA.put(ColorName.DARK_CYAN, DARK_CYAN_RGBA);
        NAME_TO_COLOR_RGBA.put(ColorName.DARK_MAGENTA, DARK_MAGENTA_RGBA);
        NAME_TO_COLOR_RGBA.put(ColorName.DARK_YELLOW, DARK_YELLOW_RGBA);
    }

    public static final Vector3f getRGBColorOrDefault(String colorName) {
        return NAME_TO_COLOR_RGB.getOrDefault(ColorName.valueOf(colorName), WHITE);
    }

    public static final Vector3f getRGBColorOrDefault(ColorName colorName) {
        return NAME_TO_COLOR_RGB.getOrDefault(colorName, WHITE);
    }

    public static final Vector4f getRGBAColorOrDefault(String colorName) {
        return NAME_TO_COLOR_RGBA.getOrDefault(ColorName.valueOf(colorName), WHITE_RGBA);
    }

    public static final Vector4f getRGBAColorOrDefault(ColorName colorName) {
        return NAME_TO_COLOR_RGBA.getOrDefault(colorName, WHITE_RGBA);
    }

    public static final ColorName getColorName(Vector3f color) {
        for (Map.Entry<ColorName, Vector3f> entry : NAME_TO_COLOR_RGB.entrySet()) {
            if (entry.getValue().equals(color)) {
                return entry.getKey();
            }
        }

        return ColorName.CUSTOM;
    }
}
