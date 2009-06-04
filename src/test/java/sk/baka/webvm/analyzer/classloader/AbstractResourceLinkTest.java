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
public abstract class AbstractResourceLinkTest extends TestCase {

    /**
     * Returns the file to test on.
     * @return the file.
     */
    protected abstract File getFile();
    /**
     * The jar file or directory to test on.
     */
    protected File file = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        file = getFile();
        assertTrue(file.exists());
    }

    /**
     * @throws java.io.IOException if i/o error occurs.
     */
    public void testRoot() throws IOException {
        final ResourceLink link = ResourceLink.newFor(file);
        assertTrue(link.isRoot());
        assertTrue(link.isPackage());
        assertEquals(file, link.getContainer());
        assertEquals(-1, link.getLength());
    }

    /**
     * Tests the {@link ResourceLink#getName()} method.
     * @throws java.io.IOException
     */
    public void testName() throws IOException {
        ResourceLink link = ResourceLink.newFor(file);
        assertEquals(file.getAbsolutePath(), link.getName());
        link = getSun();
        assertEquals("sun", link.getName());
    }

    /**
     * @throws java.io.IOException if i/o error occurs.
     */
    public void testRootFullName() throws IOException {
        ResourceLink link = ResourceLink.newFor(file);
        assertEquals(file.getAbsolutePath(), link.getFullName());
    }

    /**
     * Tests contents of the root package.
     * @throws java.io.IOException if i/o error occurs.
     */
    public void testRootContents() throws IOException {
        final ResourceLink link = ResourceLink.newFor(file);
        final List<ResourceLink> children = link.list();
        assertEquals(new String[]{"META-INF", "com"}, children);
    }

    /**
     * Tests package content walking.
     * @throws java.io.IOException if i/o error occurs.
     */
    public void testContentsWalking() throws IOException {
        final ResourceLink link = ResourceLink.newFor(file);
        final ResourceLink comLink = ResourceLink.findFirstByName(link.list(), "com");
        assertFalse(comLink.isRoot());
        assertTrue(comLink.isPackage());
        final ResourceLink sunLink = ResourceLink.findFirstByName(comLink.list(), "sun");
        final ResourceLink cryptoLink = ResourceLink.findFirstByName(sunLink.list(), "crypto");
        final ResourceLink providerLink = ResourceLink.findFirstByName(cryptoLink.list(), "provider");
        final ResourceLink fileLink = ResourceLink.findFirstByName(providerLink.list(), "AESCipher.class");
        assertFalse(fileLink.isPackage());
    }

    /**
     * Returns link denoting the "com.sun" package.
     * @return the link to the "com.sun" package.
     * @throws java.io.IOException if i/o error occurs.
     */
    protected final ResourceLink getSun() throws IOException {
        final ResourceLink link = ResourceLink.newFor(file);
        final ResourceLink comLink = ResourceLink.findFirstByName(link.list(), "com");
        final ResourceLink sunLink = ResourceLink.findFirstByName(comLink.list(), "sun");
        return sunLink;
    }

    /**
     * Tests package name grouping.
     * @throws java.io.IOException if i/o error occurs.
     */
    public void testRootContentsGrouping() throws IOException {
        final ResourceLink link = ResourceLink.newFor(file);
        final List<ResourceLink> children = link.listAndGroup();
        assertEquals(new String[]{"META-INF", "com.sun.crypto.provider"}, children);
    }

    /**
     * Asserts that given collection does not contain equal items.
     * @param c the collection to check, not null
     */
    public static void assertDistinctItems(final Collection<?> c) {
        final Set<Object> set = new HashSet<Object>();
        for (Object o : c) {
            if (!set.add(o)) {
                fail("Object " + o + " is present multiple times in " + c);
            }
        }
    }

    /**
     * Checks that given resource list contains required elements.
     * @param expected the expected resource names.
     * @param got actual resource list. Must contain unique items.
     */
    public static void assertEquals(String[] expected, final List<ResourceLink> got) {
        final Set<String> expectedSet = new HashSet<String>(Arrays.asList(expected));
        final List<String> gotList = ResourceLink.getNames(got);
        assertDistinctItems(gotList);
        final Set<String> gotSet = new HashSet<String>(gotList);
        assertEqualsSet(expectedSet, gotSet);
    }

    /**
     * Checks that given resource list contains required elements.
     * @param expectedSet the expected resource names.
     * @param gotSet actual resource list
     */
    public static void assertEqualsSet(Collection<?> expectedSet, Collection<?> gotSet) {
        final Set<Object> expected = new HashSet<Object>(expectedSet);
        final Set<Object> got = new HashSet<Object>(gotSet);
        if (!expected.equals(got)) {
            got.removeAll(expected);
            expected.removeAll(gotSet);
            fail("Sets differs; excess items: " + got + "; missing items: " + expected);
        }
    }
}
