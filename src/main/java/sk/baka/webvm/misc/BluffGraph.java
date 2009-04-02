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
package sk.baka.webvm.misc;

/**
 * Renders a graph using the Bluff javascript rendering engine.
 * @author Martin Vysny
 */
public final class BluffGraph extends AbstractGraph {

    /**
     * Creates new graph.
     * @param max the maximum value, ignored.
     * @param style the style.
     */
    public BluffGraph(int max, GraphStyle style) {
        super(max, style);
    }

    @Override
    public void draw(StringBuilder sb) {
        final String id = Integer.toString(System.identityHashCode(this));
        sb.append("<canvas id=\"");
        sb.append(id);
        sb.append("\"");
        if (style.border != null) {
            sb.append(" style=\"border: 1px solid ");
            sb.append(style.border);
            sb.append("; \"");
        }
        sb.append("></canvas><script type=\"text/javascript\">\n");
        sb.append("  var g = new Bluff.");
        switch (style.style) {
            case StackedBar:
                sb.append("StackedArea");
                break;
            case Line:
                sb.append("Line");
                break;
        }
        sb.append("('");
        sb.append(id);
        sb.append("', '");
        sb.append(style.width);
        sb.append('x');
        sb.append(style.height);
        sb.append("');\n");
        if (style.caption == null) {
            sb.append("  g.hide_title = true;\n");
        } else {
            sb.append("  g.title='");
            sb.append(style.caption);
            sb.append("';\n");
        }
        if (!style.yLegend) {
            sb.append("  g.hide_line_numbers = true;");
        }
        sb.append("  g.hide_legend = true;g.set_margins(0);g.sort = false;g.theme_37signals();\n");
        for (int i = 0; i < style.colors.length; i++) {
            sb.append("  g.data(\"\", [");
            boolean first = true;
            // hack to suppress the "No Data" message - displayed when a bunch of zeroes is fed to the graph.
            // Just add a single "1" to the end of the graph :)
            boolean zeroesOnly = true;
            for (final int[] vals : this.values) {
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }
                int val = vals[i];
                if (style.style == GraphStyle.GraphStyleEnum.StackedBar) {
                    // elements are stacked on top of each other, not behind themselves. Recompute items values to fix this.
                    for (int j = 0; j < i; j++) {
                        val -= vals[j];
                    }
                }
                if (val != 0) {
                    zeroesOnly = false;
                }
                sb.append(val);
            }
            if (zeroesOnly) {
                sb.append('1');
            }
            sb.append("], '");
            sb.append(style.colors[i]);
            sb.append("');\n");
        }
        sb.append("  g.minimum_value=0;g.maximum_value=");
        sb.append(max);
        sb.append(";\n");
        sb.append("  g.draw();\n");
        sb.append("</script>\n");
    }
}
