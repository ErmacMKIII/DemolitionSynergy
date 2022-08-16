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
package rs.alexanderstojanovich.evg.intrface;

import java.util.ArrayList;
import java.util.List;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;
import rs.alexanderstojanovich.evg.texture.Texture;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class MultiValue implements MenuValue { // customizable list of items (objects) which also has selected item

    private final List<Object> values = new ArrayList<>();
    protected final DynamicText valueText;
    private int selected = -1;
    protected Type type;

    public MultiValue(Object[] valueArray, Type type) {
        this.type = type;
        this.valueText = new DynamicText(Texture.FONT, "");
        for (Object object : valueArray) {
            values.add(object);
        }
    }

    public MultiValue(Object[] valueArray, Type type, int selected) {
        this.type = type;
        this.selected = selected;
        this.valueText = new DynamicText(Texture.FONT, String.valueOf(valueArray[selected]));
        for (Object object : valueArray) {
            values.add(object);
        }
    }

    public MultiValue(Object[] valueArray, Type type, Object currentValue) {
        this.type = type;
        this.valueText = new DynamicText(Texture.FONT, String.valueOf(currentValue));
        for (Object object : valueArray) {
            values.add(object);
        }
        this.selected = values.indexOf(currentValue);
    }

    @Override
    public Object getCurrentValue() {
        Object obj = null;
        if (!values.isEmpty() && selected != -1) {
            obj = values.get(selected);
        }
        return obj;
    }

    @Override
    public void setCurrentValue(Object object) {
        if (selected != -1) {
            switch (this.type) {
                case STRING:
                    values.set(selected, (String) object);
                    break;
                case INT:
                    values.set(selected, (Integer) object);
                    break;
                case LONG:
                    values.set(selected, (Long) object);
                    break;
                case FLOAT:
                    values.set(selected, (Float) object);
                    break;
                case DOUBLE:
                    values.set(selected, (Double) object);
                    break;
                case BOOL:
                    values.set(selected, (Boolean) object);
                    break;
            }

            valueText.setContent(String.valueOf(object));
            valueText.unbuffer();
        }
    }

    public void selectPrev() {
        if (values != null) {
            selected--;
            if (selected < 0) {
                selected = values.size() - 1;
            }
            setCurrentValue(values.get(selected));
        }
    }

    public void selectNext() {
        if (values != null) {
            selected++;
            if (selected > values.size() - 1) {
                selected = 0;
            }
            setCurrentValue(values.get(selected));
        }
    }

    public List<Object> getValues() {
        return values;
    }

    public int getSelected() {
        return selected;
    }

    public void setSelected(int selected) {
        this.selected = selected;
    }

    @Override
    public DynamicText getValueText() {
        return valueText;
    }

    @Override
    public void render(ShaderProgram shaderProgram) {
        if (!valueText.isBuffered()) {
            valueText.bufferAll();
        }
        valueText.render(shaderProgram);
    }

    @Override
    public Type getType() {
        return type;
    }

}
