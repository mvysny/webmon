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

import sk.baka.webvm.analyzer.ProblemReport;
import java.util.List;
import org.apache.wicket.PageParameters;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import sk.baka.webvm.analyzer.ProblemAnalyzer;

/**
 * Shows the "Problems" page and provides the problems analysis.
 * @author Martin Vysny
 */
public final class Problems extends WebPage {

    /**
     * Creates new instance
     * @param params page parameters
     */
    public Problems(PageParameters params) {
        final AppBorder border = new AppBorder("appBorder");
        add(border);
        final IModel<List<ProblemReport>> model = new LoadableDetachableModel<List<ProblemReport>>() {

            @Override
            protected List<ProblemReport> load() {
                return WicketApplication.getAnalyzer().getProblems(WicketApplication.getHistory().getVmstatHistory());
            }
        };
        border.add(new ListView<ProblemReport>("problemList", model) {

            @Override
            protected void populateItem(ListItem<ProblemReport> item) {
                final ProblemReport pr = item.getModelObject();
                item.add(new Label("problemClass", pr.pclass));
                final Label l = new Label("problemSeverity", pr.isProblem ? "WARN" : "OK");
                l.add(new SimpleAttributeModifier("bgcolor", pr.isProblem ? "#d24343" : "#28cb17"));
                item.add(l);
                final Label desc = new Label("problemDesc", pr.desc);
                desc.setEscapeModelStrings(false);
                item.add(desc);
                item.add(new Label("problemDiagnosis", pr.diagnosis));
            }
        });
        border.add(new Link<Void>("performGCLink") {

            @Override
            public void onClick() {
                System.gc();
                setResponsePage(Problems.class);
            }
        });
    }
}

