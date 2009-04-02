/**
 * Copyright 2009 Martin Vysny.
 *
 * This file is part of WebVM.
 *
 * WebVM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WebVM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WebVM.  If not, see <http://www.gnu.org/licenses/>.
 */
package sk.baka.webvm.analyzer.classloader;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Runs the tests on a jar file.
 * @author Martin Vysny
 */
public class JarResourceLinkTest extends AbstractResourceLinkTest {

    @Override
    protected File getFile() {
        final File file = new File("src/test/files/sunjce_provider.jar");
        assertEquals(JarResourceLink.class, ResourceLink.newFor(file).getClass());
        return file;
    }

    public void testSearch() throws IOException {
        ResourceLink link = ResourceLink.newFor(file);
        List<ResourceLink> result = link.search("com");
        // note: JAR file itself is not a tree hierarchy - it is a list of names instead. Thus the only item should match: the com/sun/crypto/provider zip entry.
        assertEquals(new String[]{"provider"}, result);
        result = link.search("META");
        assertEquals(new String[]{"META-INF"}, result);
        result = link.search("c");
        // note: this test does not match a single "com" folder as there is no such entry in JAR.
        assertEquals(new String[]{"provider", "AESCipher.class"}, result);
        link = getSun();
        result = link.search("c");
        assertEquals(new String[]{"provider", "AESCipher.class"}, result);
        result = link.search("META");
        assertEquals(new String[]{}, result);
    }
}
