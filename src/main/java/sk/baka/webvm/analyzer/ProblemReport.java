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
package sk.baka.webvm.analyzer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Describes a potential problem.
 * @author Martin Vysny
 */
public final class ProblemReport {

    private final boolean problem;

    /**
     * The description of the problem.
     * @return description of the problem, or something like OK if everything is okay. Never null.
     */
    public String getDesc() {
        return desc;
    }

    /**
     * Checks if this really is a problem.
     * @return true if this might be a serious problem, false if everything is OK or this is only a minor issue.
     */
    public boolean isProblem() {
        return problem;
    }
    private final String pclass;

    /**
     * The problem 'class'
     * @return problem class, not null.
     */
    public String getPclass() {
        return pclass;
    }
    private final String desc;

    /**
     * Creates new instance.
     * @param problem true if this might be a serious problem, false if everything is OK or this is only a minor issue.
     * @param desc description of the problem, or something like OK if everything is okay. not null
     * @param pclass the problem class, not null
     */
    public ProblemReport(final boolean problem, final String pclass, final String desc) {
        this.problem = problem;
        this.pclass = pclass;
        this.desc = desc;
    }
    
    /**
     * When this object was created.
     */
    public final long created = System.currentTimeMillis();

    /**
     * Checks if there is at least one real problem.
     * @param problems the list of problems
     * @return true if there is a problem, false otherwise.
     */
    public static boolean isProblem(final Collection<? extends ProblemReport> problems) {
        for (final ProblemReport p : problems) {
            if (p.isProblem()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Formats the problem reports in the form of CLASS: DESC\nCLASS: DESC ...
     * @param problems the problem reports to format.
     * @return all problem reports.
     * @param lineSeparator the new-line separator.
     */
    public static String toString(final Collection<? extends ProblemReport> problems, final String lineSeparator) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (final ProblemReport r : problems) {
            if (first) {
                first = false;
            } else {
                sb.append(lineSeparator);
            }
            sb.append(r.toString());
        }
        return sb.toString();
    }

    /**
     * Formats the problem reports in the form of a HTML table.
     * @param problems the problem reports to format.
     * @return all problem reports.
     */
    public static String toHtml(final Collection<? extends ProblemReport> problems) {
        final StringBuilder sb = new StringBuilder();
        sb.append("<table border=\"1\"><thead><tr><th>Problem type</th><th>Status</th><th>Description</th></tr></thead>\n");
        for (final ProblemReport r : problems) {
            sb.append("<tr><td>");
            sb.append(r.pclass);
            sb.append("</td><td bgcolor=\"#");
            sb.append(r.isProblem() ? "d24343" : "28cb17");
            sb.append("\">");
            sb.append(r.isProblem() ? "WARN" : "OK");
            sb.append("</td><td><pre>");
            sb.append(r.desc);
            sb.append("</pre></td></tr>\n");
        }
        return sb.toString();
    }

    /**
     * XML-escapes given text.
     * @param text the text to escape
     * @return text with &amp;amp;, &amp;lt; etc.
     */
    public static final String escape(final String text) {
        String result = text.replace("&", "&amp;");
        result = result.replace("<", "&lt;");
        result = result.replace(">", "&gt;");
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ProblemReport other = (ProblemReport) obj;
        if (this.problem != other.problem) {
            return false;
        }
        if ((this.pclass == null) ? (other.pclass != null) : !this.pclass.equals(other.pclass)) {
            return false;
        }
        if ((this.desc == null) ? (other.desc != null) : !this.desc.equals(other.desc)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + (this.problem ? 1 : 0);
        hash = 59 * hash + (this.pclass != null ? this.pclass.hashCode() : 0);
        hash = 59 * hash + (this.desc != null ? this.desc.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return pclass + ": " + desc;
    }

    private static Map<String, ProblemReport> toMap(final Iterable<? extends ProblemReport> reports, final boolean filterProblems) {
        final Map<String, ProblemReport> result = new HashMap<String, ProblemReport>();
        for (final ProblemReport r : reports) {
            if (filterProblems && !r.problem) {
                continue;
            }
            result.put(r.pclass, r);
        }
        return result;
    }

    /**
     * Checks if two problem reports are equal. Reports are equal when same classes are problematic and all problematic reports share the same description.
     * @param reports1 first report set.
     * @param reports2 second report set.
     * @return true if report sets are equal, false otherwise.
     */
    public static boolean equals(final Collection<? extends ProblemReport> reports1, final Collection<? extends ProblemReport> reports2) {
        final Map<String, ProblemReport> r1 = toMap(reports1, true);
        final Map<String, ProblemReport> r2 = toMap(reports2, true);
        if (!r1.keySet().equals(r2.keySet())) {
            return false;
        }
        for (final String pclass : r1.keySet()) {
            if (!r1.get(pclass).desc.equals(r2.get(pclass).desc)) {
                return false;
            }
        }
        return true;
    }
}
