package sk.baka.webvm.analyzer;

import java.util.List;
import sk.baka.webvm.analyzer.config.Config;
import sk.baka.webvm.analyzer.utils.IService;

/**
 *
 * @author Martin Vysny
 */
public interface IHistorySampler extends IService {
    /**
     * Default VMStat history.
     * <p></p>
     * POZOR sample delay ms musi byt nasobky sekund, inac niektore rutiny nebudu pocitat spravne.
     */
    public static final SamplerConfig HISTORY_VMSTAT = new SamplerConfig(150, 1000, 0);
    /**
     * Default Problems history.
     */
    public static final SamplerConfig HISTORY_PROBLEMS = new SamplerConfig(20, 10 * 1000, 500);
    public void configChanged(Config cfg);
    public List<HistorySample> getVmstatHistory();
    public List<List<ProblemReport>> getProblemHistory();
}
