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

/**
 *
 * @author Coa
 */
public class Combo {

    private Object[] values;
    private int selected;

    public Combo(Object[] values, int selected) {
        this.values = values;
        this.selected = selected;
    }

    public Object giveCurrent() {
        Object obj = null;
        if (values != null) {
            obj = values[selected];
        }
        return obj;
    }

    public void selectPrev() {
        if (values != null) {
            selected--;
            if (selected < 0) {
                selected = values.length - 1;
            }
        }
    }

    public void selectNext() {
        if (values != null) {
            selected++;
            if (selected > values.length - 1) {
                selected = 0;
            }
        }
    }

    public Object[] getValues() {
        return values;
    }

    public void setValues(Object[] values) {
        this.values = values;
    }

    public int getSelected() {
        return selected;
    }

    public void setSelected(int selected) {
        this.selected = selected;
    }

}
