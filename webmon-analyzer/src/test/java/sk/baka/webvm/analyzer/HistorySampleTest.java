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
import sk.baka.webvm.analyzer.hostos.CPUUsage;
import sk.baka.webvm.analyzer.utils.MemoryUsage2;

import static org.junit.Assert.*;

/**
 * Tests serialization and de-serialization of HistorySample.
 *
 * @author Martin Vysny
 */
public class HistorySampleTest {

    private HistorySample build() {
        final HistorySample.Builder b = new HistorySample.Builder().setClassesLoaded(5).setCpuIOUsage(10).setCpuJavaUsage(15).setCpuUsage(CPUUsage.of(25)).setGcCpuUsage(100);
        b.threads = ThreadMap.takeSnapshot();
        b.memPoolUsage.put(HistorySample.MemoryPools.PhysMem, new MemoryUsage2(-1, 25, 50, 10000));
        return b.build();
    }

    private void check(HistorySample hs) {
        assertEquals(5, hs.classesLoaded);
        assertEquals(10, hs.cpuIOUsage);
        assertEquals(15, hs.cpuJavaUsage);
        assertEquals(CPUUsage.of(25), hs.cpuUsage);
        assertEquals(100, hs.gcCpuUsage);
        assertEquals(1, hs.memPoolUsage.size());
        final MemoryUsage2 physmem = hs.memPoolUsage.get(HistorySample.MemoryPools.PhysMem);
        assertEquals(-1L, physmem.getInit());
        assertEquals(25L, physmem.getUsed());
        assertEquals(50L, physmem.getCommitted());
        assertEquals(10000L, physmem.getMax());
    }

    @Test
    public void testCreation() {
        final HistorySample hs = build();
        check(hs);
        assertEquals(hs, hs);
        assertNotNull(hs.threads);
    }

    @Test
    public void testBuilderCopy() {
        final HistorySample hs1 = build();
        final HistorySample hs = new HistorySample.Builder().copy(hs1).build();
        check(hs);
        assertEquals(hs1, hs);
        assertNotNull(hs.threads);
    }
}
