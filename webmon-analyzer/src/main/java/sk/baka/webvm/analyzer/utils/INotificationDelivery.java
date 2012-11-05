package sk.baka.webvm.analyzer.utils;

import java.util.List;
import sk.baka.webvm.analyzer.ProblemReport;
import sk.baka.webvm.analyzer.config.Config;

/**
 *
 * @author Martin Vysny
 */
public interface INotificationDelivery extends IService {
    /**
     * Notifies the deliverer that the config has been changed.
     */
    void configChanged(Config newConfig);
    /**
     * Delivers given report asynchronously.
     * @param reports the reports to deliver, not null, not empty.
     */
    void deliverAsync(final List<ProblemReport> reports);
}
