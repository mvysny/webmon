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
package sk.baka.webvm.analyzer.config;

import sk.baka.webvm.analyzer.config.EncryptionEnum;
import sk.baka.webvm.analyzer.config.Config;
import sk.baka.webvm.analyzer.config.Binder;
import java.util.Map;
import java.util.Properties;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests the {@link Binder} class.
 * @author Martin Vysny
 */
public class BinderTest {

    /**
     * Test of bindBeanView method, of class Binder.
     */
    @Test
    public void testBindToBean() {
        Properties p = new Properties();
        p.put("minFreeDiskSpaceMb", "25");
        p.put("mail.smtp.encryption", "SSL");
        p.put("mail.to", "foo@bar.baz");
        Config c1 = new Config();
        final Map<String, String> warnings = Binder.bindBeanMap(c1, p, false, true);
        assertTrue(warnings.isEmpty());
        assertEquals(25, c1.minFreeDiskSpaceMb);
        assertEquals(EncryptionEnum.SSL, c1.mailSmtpEncryption);
        assertEquals("foo@bar.baz", c1.mailTo);
    }

    /**
     * Test of bindBeanView method, of class Binder.
     */
    @Test
    public void testBindToProperties() {
        Properties p = new Properties();
        final Map<String, String> warnings = Binder.bindBeanMap(new Config(), p, true, true);
        assertTrue(warnings.isEmpty());
        assertEquals("100", p.getProperty("minFreeDiskSpaceMb"));
        assertEquals("NONE", p.getProperty("mail.smtp.encryption"));
        assertNull(p.getProperty("mail.to"));
    }

    /**
     * Test of copy method, of class Binder.
     */
    @Test
    public void testCopy() {
        Config c1 = new Config();
        c1.gcCpuTreshold = 2000;
        c1.mailTo = "foo@bar.baz";
        c1.mailSmtpEncryption = EncryptionEnum.SSL;
        Config c2 = new Config();
        Binder.copy(c1, c2);
        assertEquals(EncryptionEnum.SSL, c2.mailSmtpEncryption);
        assertEquals("foo@bar.baz", c2.mailTo);
        assertEquals(2000, c2.gcCpuTreshold);
    }
}
