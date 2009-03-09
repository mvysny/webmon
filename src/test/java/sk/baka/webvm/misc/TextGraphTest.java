/**
 *  Copyright 2009 vyzivus.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package sk.baka.webvm.misc;

import sk.baka.webvm.misc.TextGraph;
import java.io.IOException;
import junit.framework.TestCase;

/**
 * Tests the {@link TextGraph} class.
 * @author vyzivus
 */
public class TextGraphTest extends TestCase {

    /**
     * @throws IOException
     */
    public void testDrawEmptyGraph() throws IOException {
        assertEquals("", new TextGraph().draw(3));
        assertEquals("", new TextGraph().draw(10));
    }

    /**
     * @throws java.io.IOException
     */
    public void testDrawSingleValueGraph() throws IOException {
        TextGraph tg = new TextGraph();
        tg.addValue(3);
        tg.setRange(0, 3);
        assertEquals("2|#\n1|#\n0|#\n +-\n", tg.draw(3));
        assertEquals("3|#\n2|#\n1|#\n1|#\n0|#\n +-\n", tg.draw(5));
        tg.setRange(0, 6);
        assertEquals("5| \n3|x\n1|#\n +-\n", tg.draw(3));
        assertEquals("5| \n4| \n3| \n2|#\n1|#\n0|#\n +-\n", tg.draw(6));
    }
}
