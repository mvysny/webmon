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
package sk.baka.webvm.analyzer.classloader;

import java.io.File;
import java.io.IOException;
import java.util.List;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Martin Vysny
 */
public class DirResourceLinkTest extends AbstractResourceLinkTest {

    @Override
    protected File getFile() {
        final File file = new File("src/test/files/sunjce_provider");
        assertEquals(DirResourceLink.class, ResourceLink.newFor(file).getClass());
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
        assertEqualsLinks(new String[]{"com"}, result);
        result = link.search("META");
        assertEqualsLinks(new String[]{"META-INF"}, result);
        result = link.search("c");
        assertEqualsLinks(new String[]{"com", "crypto", "AESCipher.class"}, result);
        link = getSun();
        result = link.search("c");
        assertEqualsLinks(new String[]{"crypto", "AESCipher.class"}, result);
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
        assertEquals(file.getAbsolutePath() + File.separator + "com" + File.separator + "sun", link.getFullName());
    }
}
