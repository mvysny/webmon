package sk.baka.webvm.analyzer.utils;

/**
 *
 * @author Martin Vysny
 */
public class Checks {
    public static void checkNotNull(String name, Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Parameter name: invalid value " + name + ": must not be null");
        }
    }
}
