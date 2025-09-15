/*
 * Copyright (C) 2022 coas9
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
package rs.alexanderstojanovich.evg.intrface;

import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public interface MenuValue {

    /**
     * Type of the Menu Value
     */
    public static enum Type {
        BOOL, INT, LONG, FLOAT, DOUBLE, STRING
    };

    /**
     * Get Menu Value of the Options Menu
     *
     * @return menu value (Object)
     */
    public Object getCurrentValue();

    /**
     * Set Menu Value of the Options Menu
     *
     * @param object value to set
     */
    public void setCurrentValue(Object object);

    /**
     * Get Dynamic Text
     *
     * @return dynamic text
     */
    public DynamicText getValueText();

    /**
     * Render this component with shader program (interface).
     *
     * @param intrface intrface
     * @param shaderProgram shader program
     */
    public void render(Intrface intrface, ShaderProgram shaderProgram);

    /**
     * Get Type of the menu value
     *
     * @return type
     */
    public Type getType();

    /**
     * Get interface associated.
     *
     * @return intrface
     */
    public Intrface getIntrface();
}
