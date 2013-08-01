/**
 * Copyright 2009 Martin Vysny.
 *
 * This file is part of WebMon.
 *
 * WebMon is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * WebMon is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * WebMon. If not, see <http://www.gnu.org/licenses/>.
 */
package sk.baka.webvm.analyzer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.management.MemoryUsage;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests serialization and de-serialization of HistorySample.
 *
 * @author Martin Vysny
 */
public class HistorySampleTest {

    private HistorySample build() {
        final HistorySample.Builder b = new HistorySample.Builder().setClassesLoaded(5).setCpuIOUsage(10).setCpuJavaUsage(15).setCpuUsage(25).setGcCpuUsage(100);
        b.threads = ThreadMap.takeSnapshot();
        b.memPoolUsage = new MemoryUsage[1];
        b.memPoolUsage[0] = new MemoryUsage(-1, 25, 50, 10000);
        return b.build();
    }

    private void check(HistorySample hs) {
        assertEquals(5, hs.classesLoaded);
        assertEquals(10, hs.cpuIOUsage);
        assertEquals(15, hs.cpuJavaUsage);
        assertEquals(25, hs.cpuUsage);
        assertEquals(100, hs.gcCpuUsage);
        assertEquals(1, hs.memPoolUsage.length);
        assertEquals(-1L, hs.memPoolUsage[0].getInit());
        assertEquals(25L, hs.memPoolUsage[0].getUsed());
        assertEquals(50L, hs.memPoolUsage[0].getCommitted());
        assertEquals(10000L, hs.memPoolUsage[0].getMax());
    }

    @Test
    public void testCreation() {
        final HistorySample hs = build();
        check(hs);
        assertNotNull(hs.threads);
    }

    @Test
    public void testBuilderCopy() {
        final HistorySample hs = new HistorySample.Builder().copy(build()).build();
        check(hs);
        assertNotNull(hs.threads);
    }

    @Test
    public void testSerialization() throws Exception {
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final DataOutputStream dout = new DataOutputStream(bout);
        new HistorySample.Builder().copy(build()).writeTo(dout);
        dout.close();
        final HistorySample.Builder b = new HistorySample.Builder();
        b.readFrom(new DataInputStream(new ByteArrayInputStream(bout.toByteArray())));
        check(b.build());
    }
}
