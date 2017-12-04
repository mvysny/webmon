package sk.baka.webvm.analyzer.dump;

import sk.baka.webvm.analyzer.ProblemAnalyzer;
import sk.baka.webvm.analyzer.utils.Threads;

import java.util.*;

/**
 * Dumps a VM state.
 *
 * @author Martin Vysny
 */
public class TextDump extends AbstractDump {

    @Override protected Table newTable(int numberOfColumns) {
        return new TextTable(numberOfColumns);
    }

    public static final class TextTable implements Table {

        public boolean horizontalHeaderSeparator = true;
        public boolean verticalHeaderSeparator = true;
        public boolean horizontalContentsSeparator = false;
        public boolean verticalContentsSeparator = true;

        public TextTable(int columnCount) {
            columnSizes = new int[columnCount];
        }
        private final int[] columnSizes;
        private final List<List<String>> contents = new ArrayList<List<String>>();
        private final List<List<Boolean>> rightAlign = new ArrayList<List<Boolean>>();

        @Override public void setHorizontalHeaderSeparator(boolean horizontalHeaderSeparator) {
            this.horizontalHeaderSeparator = horizontalHeaderSeparator;
        }

        @Override public void setVerticalHeaderSeparator(boolean verticalHeaderSeparator) {
            this.verticalHeaderSeparator = verticalHeaderSeparator;
        }

        @Override public void setHorizontalContentsSeparator(boolean horizontalContentsSeparator) {
            this.horizontalContentsSeparator = horizontalContentsSeparator;
        }

        @Override public void setVerticalContentsSeparator(boolean verticalContentsSeparator) {
            this.verticalContentsSeparator = verticalContentsSeparator;
        }

        public void add(List<String> values, List<Boolean> rightAlign) {
            if (values.size() < columnSizes.length) {
                throw new IllegalArgumentException("Parameter values: invalid value " + values + ": must contain at least " + columnSizes.length + " items");
            }
            if (rightAlign.size() < columnSizes.length) {
                throw new IllegalArgumentException("Parameter rightAlign: invalid value " + rightAlign + ": must contain at least " + columnSizes.length + " items");
            }
            contents.add(values);
            this.rightAlign.add(rightAlign);
            for (int i = 0; i < columnSizes.length; i++) {
                if (columnSizes[i] < values.get(i).length()) {
                    columnSizes[i] = values.get(i).length();
                }
            }
        }

        private void printContent(StringBuilder target, List<String> contents, List<Boolean> rightAlign) {
            for (int i = 0; i < contents.size(); i++) {
                final boolean ra = rightAlign == null ? false : rightAlign.get(i);
                if (!ra) {
                    target.append(contents.get(i));
                }
                for (int spacesToAdd = columnSizes[i] - contents.get(i).length(); spacesToAdd > 0; spacesToAdd--) {
                    target.append(' ');
                }
                if (ra) {
                    target.append(contents.get(i));
                }
                if (i < contents.size() - 1) {
                    target.append(printVerticalSeparator(i) ? " | " : " ");
                }
            }
            target.append('\n');
        }

        private boolean printVerticalSeparator(int index) {
            return (verticalHeaderSeparator && index == 0) || (verticalContentsSeparator && index > 0);
        }

        private boolean printHorizontalSeparator(int index) {
            return (horizontalHeaderSeparator && index == 0) || (horizontalContentsSeparator && index > 0);
        }

        private void printHorizontalSeparator(StringBuilder target) {
            for (int i = 0; i < columnSizes.length; i++) {
                for (int spacesToAdd = columnSizes[i]; spacesToAdd > 0; spacesToAdd--) {
                    target.append('-');
                }
                if (i < columnSizes.length - 1) {
                    target.append(printVerticalSeparator(i) ? "-+-" : "-");
                }
            }
            target.append('\n');
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < contents.size(); i++) {
                printContent(sb, contents.get(i), rightAlign.get(i));
                if (printHorizontalSeparator(i) && i < contents.size() - 1) {
                    printHorizontalSeparator(sb);
                }
            }
            return sb.toString();
        }
    }

    @Override
    protected void printProperties(StringBuilder sb, Map<?, ?> env) {
        final List<String> names = new ArrayList<String>(env.size());
        for (Object name : env.keySet()) {
            names.add(name.toString());
        }
        Collections.sort(names, new Comparator<String>() {
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        });
        for (String name : names) {
            sb.append(name).append('=').append(env.get(name)).append('\n');
        }
    }

    @Override
    protected void printThreadStacktraceDump(StringBuilder sb) {
        printHeader(sb, "Thread Stacktrace Dump");
        sb.append(new Threads.Dump());
        sb.append("\nThead Deadlock Analysis Result:\n");
        sb.append(ProblemAnalyzer.getDeadlockReport().toString());
        sb.append("\n\n");
    }

    @Override protected void newLine(StringBuilder sb) {
        sb.append('\n');
    }

    protected void printHeader(StringBuilder sb, String header) {
        sb.append("======================== ");
        sb.append(header);
        sb.append(" ========================\n\n");
    }
}
