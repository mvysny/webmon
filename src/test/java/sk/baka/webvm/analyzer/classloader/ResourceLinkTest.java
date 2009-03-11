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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import junit.framework.TestCase;

/**
 * Tests the {@link ResourceLink} class.
 * @author Martin Vysny
 */
public class ResourceLinkTest extends TestCase {

    private File jarFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        jarFile = new File("src/test/files/sunjce_provider.jar");
        assertTrue(jarFile.exists());
    }

    public void testJarRoot() throws IOException, IOException {
        final ResourceLink link = ResourceLink.newFor(jarFile);
        assertTrue(link.isRoot());
        assertTrue(link.isPackage());
        assertEquals(jarFile, link.getContainer());
        assertEquals(-1, link.getLength());
        assertEquals(jarFile.getAbsolutePath(), link.getName());
    }

    public void testJarRootContents() throws IOException {
        final ResourceLink link = ResourceLink.newFor(jarFile);
        final List<ResourceLink> children = link.list();
        assertEquals(new String[]{"META-INF", "com"}, children);
    }

    public void testJarContents() throws IOException {
        final ResourceLink link = ResourceLink.newFor(jarFile);
        final ResourceLink comLink = ResourceLink.findFirstByName(link.list(), "com");
        assertFalse(comLink.isRoot());
        assertTrue(comLink.isPackage());
        final ResourceLink sunLink = ResourceLink.findFirstByName(comLink.list(), "sun");
        final ResourceLink cryptoLink = ResourceLink.findFirstByName(sunLink.list(), "crypto");
        final ResourceLink providerLink = ResourceLink.findFirstByName(cryptoLink.list(), "provider");
        final ResourceLink fileLink = ResourceLink.findFirstByName(providerLink.list(), "AESCipher.class");
        assertFalse(fileLink.isPackage());
    }

    public void testJarRootContentsGrouping() throws IOException {
        final ResourceLink link = ResourceLink.newFor(jarFile);
        final List<ResourceLink> children = link.listAndGroup();
        assertEquals(new String[]{"META-INF", "com.sun.crypto.provider"}, children);
    }

    private static void assertDistinctItems(final Collection<?> c) {
        final Set<Object> set = new HashSet<Object>();
        for (Object o : c) {
            if (!set.add(o)) {
                fail("Object " + o + " is present multiple times in " + c);
            }
        }
    }

    private static void assertEquals(String[] expected, final List<ResourceLink> got) {
        final Set<String> expectedSet = new HashSet<String>(Arrays.asList(expected));
        final List<String> gotList = ResourceLink.getNames(got);
        assertDistinctItems(gotList);
        final Set<String> gotSet = new HashSet<String>(gotList);
        assertEqualsSet(expectedSet, gotSet);
    }

    private static void assertEqualsSet(Collection<?> expectedSet, Collection<?> gotSet) {
        final Set<Object> expected = new HashSet<Object>(expectedSet);
        final Set<Object> got = new HashSet<Object>(gotSet);
        if (!expected.equals(got)) {
            got.removeAll(expected);
            expected.removeAll(gotSet);
            fail("Sets differs; excess items: " + got + "; missing items: " + expected);
        }
    }
}
