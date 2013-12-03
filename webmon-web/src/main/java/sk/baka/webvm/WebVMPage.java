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
package sk.baka.webvm;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import sk.baka.webvm.analyzer.utils.MiscUtils;

/**
 * A superclass of all WebMon pages.
 *
 * @author Martin Vysny
 */
public class WebVMPage extends WebPage {

    private static final long serialVersionUID = 1L;
    /**
     * Each page is wrapped in this border.
     */
    protected final AppBorder border;

    /**
     * Creates new WebMon page.
     */
    public WebVMPage() {
        border = new AppBorder("appBorder");
        add(border);
    }

    /**
     * Shows given string unescaped.
     *
     * @param wid the wicket component
     * @param value the value to show
     */
    public final void unescaped(final String wid, final IModel<String> value) {
        final Label l = new Label(wid, value);
        l.setEscapeModelStrings(false);
        border.add(l);
    }

    public static File toFile(final URL url) {
        return MiscUtils.toLocalFile(url.toString());
    }
    
    private final List<IModel<?>> detach = new ArrayList<IModel<?>>();
    protected final <T> IModel<T> register(IModel<T> model) {
        detach.add(model);
        return model;
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        for (IModel<?> model: detach) {
            model.detach();
        }
    }
}
