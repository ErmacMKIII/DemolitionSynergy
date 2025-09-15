/*
 * Copyright (C) 2024 Alexander Stojanovich <coas91@rocketmail.com>
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

/**
 * Generic renderer interface
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public interface CoreRenderer {

    /**
     * Prepare for render This usually means to clear the flag(s).
     *
     */
    public void prepare();

    /**
     * Render stuff to the screen (using OpenGL)
     */
    public void render();

    /**
     * Release resources
     */
    public void release();
}
