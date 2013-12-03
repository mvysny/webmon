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

import com.google.inject.Inject;
import java.lang.reflect.Constructor;
import java.text.DateFormat;
import java.util.Date;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.validation.validator.StringValidator;
import sk.baka.webvm.analyzer.IHistorySampler;
import sk.baka.webvm.analyzer.TextDump;

/**
 * Defines a border for the entire application.
 *
 * @author Martin Vysny
 */
public class AppBorder extends Border {

    private static final long serialVersionUID = 1L;

    /**
     * Creates new application border.
     *
     * @param componentName the component id
     */
    public AppBorder(final String componentName) {
        super(componentName);
        final DateFormat formatter = DateFormat.getTimeInstance(DateFormat.MEDIUM);
        addToBorder(new Label("currentTime", new LoadableDetachableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                return formatter.format(new Date());
            }
        }));
        addToBorder(new PerformGC("performGCLink"));
        addToBorder(new FormImpl("searchForm"));
        addToBorder(new BookmarkablePageLink<ThreadDump>("threadDump", ThreadDump.class));
        addToBorder(new Link<Void>("vmDump") {
            private static final long serialVersionUID = 1L;
            @Inject
            private IHistorySampler history;

            @Override
            public void onClick() {
                final String vmdump = TextDump.dump(history.getVmstatHistory());
                RequestCycle.get().scheduleRequestHandlerAfterCurrent(new ResourceStreamRequestHandler(new StringResourceStream(vmdump, "text/plain"), "vmdump.txt"));
            }
        });
    }

    /**
     * Performs a GC when clicked.
     */
    private static class PerformGC extends Link<Void> {

        public PerformGC(String id) {
            super(id);
        }
        private static final long serialVersionUID = 1L;

        @Override
        public void onClick() {
            System.gc();
            // force to reload the page if it does not have any parametrized constructor
            final Class<? extends Page> pclass = getPage().getClass();
            if (hasZeroArgConstructor(pclass)) {
                setResponsePage(pclass);
            }
        }
    }

    private static boolean hasZeroArgConstructor(final Class<?> clazz) {
        for (Constructor<?> c : clazz.getConstructors()) {
            if (c.getParameterTypes().length == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Implements the Search form.
     */
    private static class FormImpl extends Form<String> {

        public FormImpl(String id) {
            super(id);
            add(new FeedbackPanel("feedback"));
            add(new Button("submit"));
            final TextField<String> field = new TextField<String>("searchText", new PropertyModel<String>(this, "searchQuery"));
            field.add(new StringValidator.MinimumLengthValidator(3));
            add(field);
        }
        private static final long serialVersionUID = 1L;
        private String searchQuery = "";

        @Override
        protected void onSubmit() {
            setResponsePage(new SearchResults(searchQuery));
        }
    }
}
