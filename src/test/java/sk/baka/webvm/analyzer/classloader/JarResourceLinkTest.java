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
import org.junit.Test;
import static org.junit.Assert.*;

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

    /**
     * Tests the search functionality.
     * @throws IOException on i/o error.
     */
    @Test
    public void testSearch() throws IOException {
        ResourceLink link = ResourceLink.newFor(file);
        List<ResourceLink> result = link.search("com");
        // note: JAR file itself is not a tree hierarchy - it is a list of names instead. Thus the only item should match: the com/sun/crypto/provider zip entry.
        assertEqualsLinks(new String[]{"provider"}, result);
        result = link.search("META");
        assertEqualsLinks(new String[]{"META-INF"}, result);
        result = link.search("c");
        // note: this test does not match a single "com" folder as there is no such entry in JAR.
        assertEqualsLinks(new String[]{"provider", "AESCipher.class"}, result);
        link = getSun();
        result = link.search("c");
        assertEqualsLinks(new String[]{"provider", "AESCipher.class"}, result);
        result = link.search("META");
        assertEqualsLinks(new String[]{}, result);
    }

    /**
     * Check that search does not return the root container even if it matches
     * @throws IOException on i/o error.
     */
    @Test
    public void testSearchDoesNotReturnRoot() throws IOException {
        ResourceLink link = ResourceLink.newFor(file);
        List<ResourceLink> result = link.search("provider");
        assertEqualsLinks(new String[]{"provider"}, result);
    }

    /**
     * @throws java.io.IOException if i/o error occurs.
     */
    @Test
    public void testFullName() throws IOException, IOException {
        ResourceLink link = getSun();
        assertEquals(file.getAbsolutePath() + "!/com/sun", link.getFullName());
    }
}
