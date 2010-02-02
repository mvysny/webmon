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
package sk.baka.webvm.analyzer;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import sk.baka.webvm.Problems;

/**
 * Describes a potential problem.
 * @author Martin Vysny
 */
public final class ProblemReport implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * Checks if this really is a problem. true if this might be a serious problem, false if everything is OK or this is only a minor issue.
     */
    public final boolean isProblem;
    /**
     * description of the problem class, not null
     */
    public final String desc;
    /**
     * The problem 'class', not null.
     */
    public final String pclass;
    /**
     * The 'diagnosis' of the problem.
     */
    public final String diagnosis;

    /**
     * Creates new instance.
     * @param isProblem true if this might be a serious problem, false if everything is OK or this is only a minor issue.
     * @param diagnosis diagnosis of the problem, or something like OK if everything is okay. not null
     * @param pclass the problem class, not null
     * @param desc description of the problem class, not null
     */
    public ProblemReport(final boolean isProblem, final String pclass, final String diagnosis, final String desc) {
        this.isProblem = isProblem;
        this.pclass = pclass;
        this.diagnosis = diagnosis;
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
            if (p.isProblem) {
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
        sb.append("<table border=\"1\"><thead><tr><th>Problem type</th><th>Status</th><th>Diagnosis</th></tr></thead>\n");
        for (final ProblemReport r : problems) {
            sb.append("<tr><td>");
            sb.append(r.pclass);
            sb.append("</td><td bgcolor=\"#");
            sb.append(r.isProblem ? Problems.LIGHT_RED : Problems.DARK_GREEN);
            sb.append("\">");
            sb.append(r.isProblem ? "WARN" : "OK");
            sb.append("</td><td><pre>");
            sb.append(r.diagnosis);
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
        if (this.isProblem != other.isProblem) {
            return false;
        }
        if (!this.pclass.equals(other.pclass)) {
            return false;
        }
        if ((this.diagnosis == null) ? (other.diagnosis != null) : !this.diagnosis.equals(other.diagnosis)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + (this.isProblem ? 1 : 0);
        hash = 59 * hash + this.pclass.hashCode();
        hash = 59 * hash + (this.diagnosis != null ? this.diagnosis.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return (isProblem ? "WARN" : "OK  ") + ": " + pclass + ": " + diagnosis;
    }

    private static Map<String, ProblemReport> toMap(final Iterable<? extends ProblemReport> reports, final boolean filterProblems) {
        final Map<String, ProblemReport> result = new HashMap<String, ProblemReport>();
        for (final ProblemReport r : reports) {
            if (filterProblems && !r.isProblem) {
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
            if (!r1.get(pclass).diagnosis.equals(r2.get(pclass).diagnosis)) {
                return false;
            }
        }
        return true;
    }
}
