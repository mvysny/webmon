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
package sk.baka.webvm;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.List;
import org.apache.wicket.markup.html.basic.Label;
import sk.baka.webvm.misc.MgmtUtils;

/**
 * Homepage
 * @author Martin Vysny
 */
public class HomePage extends WebVMPage {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor that is invoked when page is invoked without a session.
     */
    public HomePage() {
        // memory info
        final MemoryUsage usage = MgmtUtils.getInMB(MgmtUtils.getHeapFromRuntime());
        final long memFree = usage.getMax() - usage.getUsed();
        border.add(new Label("maxMem", Long.toString(usage.getMax())));
        border.add(new Label("heapSize", Long.toString(usage.getCommitted())));
        border.add(new Label("heapUsed", Long.toString(usage.getUsed())));
        border.add(new Label("heapUsedPerc", MgmtUtils.getUsagePerc(usage)));
        border.add(new Label("memFree", memFree < 0 ? "?" : Long.toString(memFree)));
        border.add(new Label("memFreePerc", memFree < 0 ? "?" : MgmtUtils.getFreePerc(usage)));
        // garbage collections
        int collectors = 0;
        long collections = 0;
        long collectTime = 0;
        final List<GarbageCollectorMXBean> beans = ManagementFactory.getGarbageCollectorMXBeans();
        if (beans != null) {
            for (final GarbageCollectorMXBean bean : beans) {
                if (bean.isValid()) {
                    collectors++;
                }
                if (bean.getCollectionCount() > 0) {
                    collections += bean.getCollectionCount();
                }
                if (bean.getCollectionTime() > 0) {
                    collectTime += bean.getCollectionTime();
                }
            }
        }
        border.add(new Label("gcCount", Long.toString(collectors)));
        border.add(new Label("gcAmount", Long.toString(collections)));
        border.add(new Label("gcTime", Long.toString(collectTime)));
        // system info
        border.add(new Label("os", System.getProperty("os.name") + " " + System.getProperty("os.version") + "; " + System.getProperty("os.arch") + "; CPU#: " + Runtime.getRuntime().availableProcessors()));
        border.add(new Label("java", System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version") + " by " + System.getProperty("java.vm.vendor")));
        // runtime info
        final ThreadMXBean tbean = ManagementFactory.getThreadMXBean();
        final int threadCount = tbean.getThreadCount();
        final int daemonThreads = tbean.getDaemonThreadCount();
        border.add(new Label("threads", Integer.toString(threadCount)));
        border.add(new Label("threadsNormal", Integer.toString(threadCount - daemonThreads)));
        border.add(new Label("threadsDaemon", Integer.toString(daemonThreads)));
        border.add(new Label("threadsStarted", Long.toString(tbean.getTotalStartedThreadCount())));
        final ClassLoadingMXBean clbean = ManagementFactory.getClassLoadingMXBean();
        border.add(new Label("classesLoaded", Integer.toString(clbean.getLoadedClassCount())));
        border.add(new Label("classesLoadedTotal", Long.toString(clbean.getTotalLoadedClassCount())));
        border.add(new Label("classesUnloaded", Long.toString(clbean.getUnloadedClassCount())));
        drawMemoryStatus(MgmtUtils.getInMB(MgmtUtils.getHeapFromRuntime()), "memStat", 400);
    }
}
