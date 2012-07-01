package sk.baka.webvm.analyzer;

import com.google.inject.ImplementedBy;
import java.util.List;

/**
 *
 * @author Martin Vysny
 */
@ImplementedBy(HistorySampler.class)
public interface IHistorySampler {
    public void stop();
    public void configChanged();
    public void start();
    public List<HistorySample> getVmstatHistory();
}
