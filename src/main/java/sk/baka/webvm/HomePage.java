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

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import org.apache.wicket.markup.html.basic.Label;

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
        border.add(new Label("os", System.getProperty("os.name") + " " + System.getProperty("os.version")));
        border.add(new Label("hw", System.getProperty("os.arch") + "; CPU#: " + Runtime.getRuntime().availableProcessors()));
        border.add(new Label("java", System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version") + " by " + System.getProperty("java.vm.vendor")));
    }
}
