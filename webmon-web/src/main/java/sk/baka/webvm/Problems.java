/**
 * Copyright 2009 Martin Vysny.
 *
 * This file is part of WebMon.
 *
 * WebMon is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WebMon is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WebMon.  If not, see <http://www.gnu.org/licenses/>.
 */
package sk.baka.webvm;

import com.google.inject.Inject;
import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import sk.baka.webvm.analyzer.IHistorySampler;
import sk.baka.webvm.analyzer.IProblemAnalyzer;
import sk.baka.webvm.analyzer.ProblemReport;

/**
 * Shows the "Problems" page and provides the problems analysis.
 * @author Martin Vysny
 */
public class Problems extends WebVMPage {

    private static final long serialVersionUID = 1L;

    /**
     * Creates new instance
     */
    public Problems() {
        final IModel<List<ProblemReport>> model = new ProblemsModel();
        final ListView<ProblemReport> list = new ProblemsListView("problemList", model);
        border.add(list);
    }

    @Inject
    private IProblemAnalyzer analyzer;
    @Inject
    private IHistorySampler history;

    /**
     * Provides a list of model. Stateless.
     */
    private class ProblemsModel extends LoadableDetachableModel<List<ProblemReport>> {

        private static final long serialVersionUID = 1L;

        @Override
        protected List<ProblemReport> load() {
            return analyzer.getProblems(history.getVmstatHistory());
        }
    }

    /**
     * Shows a list of problem reports.
     */
    private static class ProblemsListView extends ListView<ProblemReport> {

        public ProblemsListView(String id, IModel<? extends List<? extends ProblemReport>> model) {
            super(id, model);
        }
        private static final long serialVersionUID = 1L;

        @Override
        protected void populateItem(ListItem<ProblemReport> item) {
            final ProblemReport pr = item.getModelObject();
            item.add(new Label("problemClass", pr.pclass));
            final Label l = new Label("problemSeverity", pr.isProblem ? "WARN" : "OK");
            l.add(AttributeModifier.replace("bgcolor", pr.isProblem ? ProblemReport.LIGHT_RED : ProblemReport.DARK_GREEN));
            item.add(l);
            final Label desc = new Label("problemDesc", pr.desc);
            desc.setEscapeModelStrings(false);
            item.add(desc);
            item.add(new Label("problemDiagnosis", pr.diagnosis));
        }
    }
}

