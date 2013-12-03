package sk.baka.webvm.analyzer.utils;

/**
 *
 * @author Martin Vysny
 */
public class Checks {
    public static <T> T checkNotNull(String name, T value) {
        if (value == null) {
            throw new IllegalArgumentException("Parameter name: invalid value " + name + ": must not be null");
        }
        return value;
    }
}
