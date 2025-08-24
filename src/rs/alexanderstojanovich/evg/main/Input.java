/*
 * Copyright (C) 2025 coas9
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
package rs.alexanderstojanovich.evg.main;

/**
 * Game Input (POCO) class.
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class Input {

    /**
     * Constant for numeric keys array.
     */
    public static int NUMERIC_KEYS_LENGTH = 10;

    /**
     * Direction keys used (all modes)
     */
    public final boolean[] directionKeys = new boolean[Game.Direction.values().length];
    /**
     * Move mouse
     */
    public boolean moveMouse = false;
    /**
     * Fly up (Editor Mode)
     */
    public boolean flyUp = false;
    /**
     * Fly down (Editor Mode)
     */
    public boolean flyDown = false;
    /**
     * Turn (camera) left (all modes)
     */
    public boolean turnLeft = false;
    /**
     * Turn (camera) right (all modes)
     */
    public boolean turnRight = false;
    /**
     * Numeric keys used
     */
    public final boolean[] numericKeys = new boolean[NUMERIC_KEYS_LENGTH];
    /**
     * Left shift (used)
     */
    public boolean leftShift = false;
    /**
     * Right shift (used)
     */
    public boolean rightShift = false;
    /**
     * Left control (used)
     */
    public boolean leftControl = false;
    /**
     * Right control (used)
     */
    public boolean rightControl = false;
    /**
     * Editor deselect (Editor only). Key 'F' pressed.
     */
    public boolean editorDeselect = false;
    /**
     * Mouse left used.
     */
    public boolean mouseLeftButton = false;
    /**
     * Mouse right used.
     */
    public boolean mouseRightButton = false;
    /**
     * Key N pressed (Editor new)
     */
    public boolean keyN_pressed = false;
    /**
     * Key R pressed (Editor remove)
     */
    public boolean keyR_pressed = false;

    /**
     * Clears all inputs
     */
    public void clear() {
        // Reset direction keys
        for (int i = 0; i < directionKeys.length; i++) {
            directionKeys[i] = false;
        }

        // Reset mouse movement
        moveMouse = false;

        // Reset fly controls
        flyUp = false;
        flyDown = false;

        // Reset camera turning
        turnLeft = false;
        turnRight = false;

        // Reset numeric keys
        for (int i = 0; i < numericKeys.length; i++) {
            numericKeys[i] = false;
        }

        // Reset modifier keys
        leftShift = false;
        rightShift = false;
        leftControl = false;
        rightControl = false;

        // Reset editor controls
        editorDeselect = false;
        keyN_pressed = false;
        keyR_pressed = false;

        // Reset mouse buttons
        mouseLeftButton = false;
        mouseRightButton = false;
    }
}
