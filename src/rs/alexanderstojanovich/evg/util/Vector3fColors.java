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

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class Vector3fColors { // some of the defined colors

    public static enum ColorName {
        WHITE, RED, GREEN, BLUE, CYAN, MAGENTA, YELLOW, GRAY,
        DARK_RED, DARK_GREEN, DARK_BLUE, DARK_CYAN, DARK_MAGENTA, DARK_YELLOW;

        public static String[] names() {
            return Arrays.toString(ColorName.values()).replaceAll("^.|.$", "").split(", ");
        }
    }

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

    public static final Map<ColorName, Vector3f> NAME_TO_COLOR = new LinkedHashMap<>();

    static {
//        NAME_TO_COLOR.put(ColorName.BLACK, BLACK);
        NAME_TO_COLOR.put(ColorName.WHITE, WHITE);

        NAME_TO_COLOR.put(ColorName.RED, RED);
        NAME_TO_COLOR.put(ColorName.GREEN, GREEN);
        NAME_TO_COLOR.put(ColorName.BLUE, BLUE);

        NAME_TO_COLOR.put(ColorName.CYAN, CYAN);
        NAME_TO_COLOR.put(ColorName.MAGENTA, MAGENTA);
        NAME_TO_COLOR.put(ColorName.YELLOW, YELLOW);

        NAME_TO_COLOR.put(ColorName.GRAY, GRAY);

        NAME_TO_COLOR.put(ColorName.DARK_RED, DARK_RED);
        NAME_TO_COLOR.put(ColorName.DARK_GREEN, DARK_GREEN);
        NAME_TO_COLOR.put(ColorName.DARK_BLUE, DARK_BLUE);

        NAME_TO_COLOR.put(ColorName.DARK_CYAN, DARK_CYAN);
        NAME_TO_COLOR.put(ColorName.DARK_MAGENTA, DARK_MAGENTA);
        NAME_TO_COLOR.put(ColorName.DARK_YELLOW, DARK_YELLOW);
    }

    public static final Vector3f getColorOrDefault(String colorName) {
        return NAME_TO_COLOR.getOrDefault(ColorName.valueOf(colorName), WHITE);
    }

    public static final Vector3f getColorOrDefault(ColorName colorName) {
        return NAME_TO_COLOR.getOrDefault(colorName, WHITE);
    }
}
