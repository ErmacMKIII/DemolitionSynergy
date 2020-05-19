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
package rs.alexanderstojanovich.evg.intrface;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Coa
 */
public class Combo { // customizable list of items (objects) which also has selected item

    private final List<Object> values = new ArrayList<>();
    private int selected = -1;

    public void fetchFromArray(Object[] valueArray, int selected) {
        this.selected = selected;
        for (Object object : valueArray) {
            values.add(object);
        }
    }

    public Object giveCurrent() {
        Object obj = null;
        if (!values.isEmpty() && selected != -1) {
            obj = values.get(selected);
        }
        return obj;
    }

    public void selectPrev() {
        if (values != null) {
            selected--;
            if (selected < 0) {
                selected = values.size() - 1;
            }
        }
    }

    public void selectNext() {
        if (values != null) {
            selected++;
            if (selected > values.size() - 1) {
                selected = 0;
            }
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

}
