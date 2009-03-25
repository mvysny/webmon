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
        sb.append("window.onload = function() {\n");
        sb.append("  var g = new Bluff.StackedBar('");
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
        if (style.legend == null) {
            sb.append("  g.hide_legend = true;");
        }
        sb.append("  g.set_margins(0);\n");
        sb.append("  g.sort = false;\n");
        sb.append("  g.theme_37signals();\n");
        for (int i = 0; i < style.colors.length; i++) {
            sb.append("  g.data(\"");
            if ((style.legend != null) && (style.legend[i] != null)) {
                sb.append(style.legend[i]);
            }
            sb.append("\", [");
            boolean first = true;
            for (final int[] vals : this.values) {
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }
                sb.append(vals[i]);
            }
            sb.append("], '");
            sb.append(style.colors[i]);
            sb.append("');\n");
        }
        sb.append("  g.minimum_value=0;g.maximum_value=");
        sb.append(max);
        sb.append(";\n");
        sb.append("  g.draw();\n");
        sb.append("  };</script>\n");
    }
}
