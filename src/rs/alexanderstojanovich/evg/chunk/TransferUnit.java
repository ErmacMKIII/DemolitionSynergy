/*
 * Copyright (C) 2024 coas9
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
package rs.alexanderstojanovich.evg.chunk;

import rs.alexanderstojanovich.evg.models.Block;

/**
 * One unit for updating after add/rem for chunks.
 *
 * @author Alexander Stojanovich <coas91@rocketmail.com>
 */
public class TransferUnit {

    public final Block block;
    public final int bitsBefore;
    public final int bitsAfter;

    /**
     * One transfer unit consisted of block (to transfer), bits before (src
     * tuple) and bits after (dst tuple).
     *
     * @param block trnasfer block
     * @param bitsBefore bits before
     * @param bitsAfter bits after
     */
    public TransferUnit(Block block, int bitsBefore, int bitsAfter) {
        this.block = block;
        this.bitsBefore = bitsBefore;
        this.bitsAfter = bitsAfter;
    }

    /**
     * Get block for transfer
     *
     * @return block for transfer
     */
    public Block getBlock() {
        return block;
    }

    /**
     * Get bits before transfer
     *
     * @return bits before transfer
     */
    public int getBitsBefore() {
        return bitsBefore;
    }

    /**
     * Get bits after transfer
     *
     * @return bits after transfer
     */
    public int getBitsAfter() {
        return bitsAfter;
    }

}
