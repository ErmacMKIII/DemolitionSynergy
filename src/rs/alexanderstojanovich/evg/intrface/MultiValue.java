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
import java.util.Arrays;
import java.util.List;
import rs.alexanderstojanovich.evg.shaders.ShaderProgram;

/**
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class MultiValue implements MenuValue { // customizable list of items (objects) which also has selected item

    private final List<Object> values = new ArrayList<>();
    protected final DynamicText valueText;
    private int selected = -1;
    protected Type type;
    private final Intrface intrface;

    public MultiValue(Intrface intrface, Object[] valueArray, Type type) throws Exception {
        this.intrface = intrface;
        this.type = type;
        this.valueText = new DynamicText(intrface.gameObject.GameAssets.FONT, "", intrface);
        values.addAll(Arrays.asList(valueArray));
    }

    public MultiValue(Intrface intrface, Object[] valueArray, Type type, int selected) throws Exception {
        this.intrface = intrface;
        this.type = type;
        this.selected = selected;
        this.valueText = new DynamicText(intrface.gameObject.GameAssets.FONT, String.valueOf(valueArray[selected]), intrface);
        values.addAll(Arrays.asList(valueArray));
    }

    public MultiValue(Intrface intrface, Object[] valueArray, Type type, Object currentValue) throws Exception {
        this.intrface = intrface;
        this.type = type;
        this.valueText = new DynamicText(intrface.gameObject.GameAssets.FONT, String.valueOf(currentValue), intrface);
        values.addAll(Arrays.asList(valueArray));
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
        selected = values.indexOf(object);
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
    public void render(Intrface intrface, ShaderProgram shaderProgram) {
        if (!valueText.isBuffered()) {
            valueText.bufferAll(intrface);
        }
        valueText.render(intrface, shaderProgram);
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Intrface getIntrface() {
        return intrface;
    }

}
