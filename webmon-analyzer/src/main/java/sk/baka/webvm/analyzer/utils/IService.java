package sk.baka.webvm.analyzer.utils;

/**
 *
 * @author Martin Vysny
 */
public interface IService {
    /**
     * Starts the sampling process in a background thread.
     */
    void start();
    void stop();
}
