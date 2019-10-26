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
package rs.alexanderstojanovich.evg.util;

/**
 *
 * @author Coa
 */
public class Tuple<A, B, C, D, E> {

    private A a;
    private B b;
    private C c;
    private D d;
    private E e;

    public Tuple(A a, B b, C c, D d, E e) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.e = e;
    }

    public A getA() {
        return a;
    }

    public B getB() {
        return b;
    }

    public C getC() {
        return c;
    }

    public D getD() {
        return d;
    }

    public E getE() {
        return e;
    }

    public void setA(A a) {
        this.a = a;
    }

    public void setB(B b) {
        this.b = b;
    }

    public void setC(C c) {
        this.c = c;
    }

    public void setD(D d) {
        this.d = d;
    }

    public void setE(E e) {
        this.e = e;
    }

}
