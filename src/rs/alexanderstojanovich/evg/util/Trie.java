/*
 * Copyright (C) 2021 Alexander Stojanovich <coas91@rocketmail.com>
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class Trie {

    protected final Map<Character, Trie> children = new HashMap<>();
    protected String content;
    protected boolean terminal = false;

    public Trie() {
        this(null);
    }

    private Trie(String content) {
        this.content = content;
    }

    protected void add(char character) {
        String s;
        if (this.content == null) {
            s = Character.toString(character);
        } else {
            s = this.content + character;
        }
        children.put(character, new Trie(s));
    }

    /**
     * Put candidate into the list of possible auto-completes.
     *
     * @param key candidate
     */
    public void insert(String key) {
        Trie node = this;
        for (char c : key.toCharArray()) {
            if (!node.children.containsKey(c)) {
                node.add(c);
            }
            node = node.children.get(c);
        }
        node.terminal = true;
    }

    public String find(String key) {
        Trie node = this;
        for (char c : key.toCharArray()) {
            if (!node.children.containsKey(c)) {
                return "";
            }
            node = node.children.get(c);
        }
        return node.content;
    }

    /**
     * Gives list of candidates for possible given prefix
     *
     * @param prefix given prefix
     * @return list of candidates (completes)
     */
    public List<String> autoComplete(String prefix) {
        Trie Trienode = this;
        for (char c : prefix.toCharArray()) {
            if (!Trienode.children.containsKey(c)) {
                return Collections.emptyList();
            }
            Trienode = Trienode.children.get(c);
        }
        return Trienode.allPrefixes();
    }

    protected List<String> allPrefixes() {
        List<String> result = new ArrayList<>();
        if (this.terminal) {
            result.add(this.content);
        }
        for (Map.Entry<Character, Trie> entry : children.entrySet()) {
            Trie child = entry.getValue();
            Collection<String> childPrefixes = child.allPrefixes();
            result.addAll(childPrefixes);
        }
        return result;
    }

}
