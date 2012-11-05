/**
 * Copyright 2009 Martin Vysny.
 *
 * This file is part of WebMon.
 *
 * WebMon is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WebMon is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WebMon.  If not, see <http://www.gnu.org/licenses/>.
 */
package sk.baka.webvm.misc;

import java.util.Arrays;
import static org.junit.Assert.*;
import org.junit.Test;
/**
 * Tests the {@link DivGraph} class.
 * @author Martin Vysny
 */
public class DivGraphTest {

    /**
     * Tests a simple example consisting of a single value.
     */
    @Test
    public void testToPixelsSimple() {
        int[] result = DivGraph.toPixels(new int[]{1}, 10, 10);
        assertTrue(Arrays.equals(new int[]{1, 9}, result));
        result = DivGraph.toPixels(new int[]{1}, 10, 20);
        assertTrue(Arrays.equals(new int[]{2, 18}, result));
    }

    /**
     * Tests an example where a value exceeds the max. value.
     */
    @Test
    public void testToPixelsOverMax() {
        int[] result = DivGraph.toPixels(new int[]{20}, 10, 10);
        assertTrue(Arrays.equals(new int[]{10, 0}, result));
    }

    /**
     * Tests an example with three values.
     */
    @Test
    public void testToPixelsThreeValues() {
        int[] result = DivGraph.toPixels(new int[]{1, 2, 3}, 10, 10);
        assertTrue(Arrays.equals(new int[]{1, 1, 1, 7}, result));
    }
}
