package sk.baka.webvm.analyzer;

import com.google.inject.ImplementedBy;
import java.util.List;

/**
 *
 * @author Martin Vysny
 */
@ImplementedBy(HistorySampler.class)
public interface IHistorySampler {
    /**
     * Default VMStat history.
     */
    public static final SamplerConfig HISTORY_VMSTAT = new SamplerConfig(150, 1000, 0);
    /**
     * Default Problems history.
     */
    public static final SamplerConfig HISTORY_PROBLEMS = new SamplerConfig(20, 10 * 1000, 500);
    public void stop();
    public void configChanged();
    public void start();
    public List<HistorySample> getVmstatHistory();

    public List<List<ProblemReport>> getProblemHistory();
}
