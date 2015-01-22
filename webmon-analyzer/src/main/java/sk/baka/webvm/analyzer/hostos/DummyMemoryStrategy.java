package sk.baka.webvm.analyzer.hostos;

import sk.baka.webvm.analyzer.utils.MemoryUsage2;

import java.lang.management.MemoryUsage;

/**
 * Always returns null.
 * @author Martin Vysny
 */
public class DummyMemoryStrategy implements IMemoryInfoProvider {

    public MemoryUsage2 getSwap() {
        return null;
    }

    public MemoryUsage2 getPhysicalMemory() {
        return null;
    }
}
