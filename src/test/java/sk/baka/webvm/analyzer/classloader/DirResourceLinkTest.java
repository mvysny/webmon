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
 *
 * @author Martin Vysny
 */
public class DirResourceLinkTest extends AbstractResourceLinkTest {

    @Override
    protected File getFile() {
        return new File("src/test/files/sunjce_provider");
    }

    public void testSearch() throws IOException {
        ResourceLink link = ResourceLink.newFor(file);
        List<ResourceLink> result = link.search("com");
        assertEquals(new String[]{"com"}, result);
        result = link.search("META");
        assertEquals(new String[]{"META-INF"}, result);
        result = link.search("c");
        assertEquals(new String[]{"com", "com/crypto", "com/sun/crypto/provider/AESCipher.class"}, result);
        link = getSun();
        result = link.search("c");
        assertEquals(new String[]{"com/sun/crypto", "com/sun/crypto/provider/AESCipher.class"}, result);
    }

}
