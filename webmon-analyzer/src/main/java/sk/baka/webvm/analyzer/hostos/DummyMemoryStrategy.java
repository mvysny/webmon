package sk.baka.webvm.analyzer.hostos;

import java.lang.management.MemoryUsage;

/**
 * Always returns null.
 * @author Martin Vysny
 */
public class DummyMemoryStrategy implements IMemoryInfoProvider {

    public MemoryUsage getSwap() {
        return null;
    }

    public MemoryUsage getPhysicalMemory() {
        return null;
    }
}
