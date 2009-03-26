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
        // system info
        border.add(new Label("os", System.getProperty("os.name") + " " + System.getProperty("os.version")));
        border.add(new Label("hw", System.getProperty("os.arch") + "; CPU#: " + Runtime.getRuntime().availableProcessors()));
        border.add(new Label("java", System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version") + " by " + System.getProperty("java.vm.vendor")));
    }
}
